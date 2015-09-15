/*
* To change this template, choose Tools | Templates
* and open the template in the editor.
*/
package de.dkfz.roddy.execution.io

import de.dkfz.roddy.Constants
import de.dkfz.roddy.Roddy
import de.dkfz.roddy.core.ExecutionContextLevel
import de.dkfz.roddy.execution.jobs.CommandFactory
import de.dkfz.roddy.tools.LoggerWrapper
import de.dkfz.roddy.tools.RoddyConversionHelperMethods
import de.dkfz.roddy.StringConstants
import de.dkfz.roddy.core.ExecutionContext
import de.dkfz.roddy.execution.io.fs.FileSystemInfoProvider
import de.dkfz.roddy.execution.jobs.Command
import de.dkfz.roddy.tools.RoddyIOHelperMethods
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.common.IOUtils
import net.schmizz.sshj.connection.channel.OpenFailException
import net.schmizz.sshj.connection.channel.direct.Session
import net.schmizz.sshj.sftp.*
import net.schmizz.sshj.transport.verification.PromiscuousVerifier
import net.schmizz.sshj.xfer.scp.SCPDownloadClient
import net.schmizz.sshj.xfer.scp.SCPFileTransfer
import org.apache.commons.io.filefilter.WildcardFileFilter

import java.util.concurrent.Semaphore
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import java.util.logging.Level

import static de.dkfz.roddy.StringConstants.SPLIT_COMMA

/**
 *
 * @author michael
 */
@groovy.transform.CompileStatic
class SSHExecutionService extends RemoteExecutionService {

    public static final LoggerWrapper logger = LoggerWrapper.getLogger(SSHExecutionService.class.name);

    public static class SSHPoolConnectionSet {
        public SSHClient client;

        public SFTPClient sftpClient;

        public SCPDownloadClient scpDownloadClient;

        public SCPFileTransfer scpFileTransfer;

        public final String user;

        public final String host;

        public final String method;

        private final Semaphore sshSemaphore = new Semaphore(8);

        private final int id;

        public SSHPoolConnectionSet(int id, String user, String host, String method) {
            this.user = user
            this.host = host
            this.method = method
            this.id = id;
        }

        public boolean check() {
            if (client == null)
                return false;

            if (client.isConnected() == false)
                return false;

            if (client.isAuthenticated() == false)
                return false;

            return true;
        }

        public void close() {

            try {
                if (sftpClient != null) {
                    sftpClient.close();
                }
            } catch (Exception ex) {
                logger.postRareInfo("Could not close SFTP client object." + ex);
            }

            try {
                if (client != null) {
                    client.disconnect();
                    client.close();
                }
            } catch (Exception ex) {
                logger.postRareInfo("Could not close SSH client object." + ex);
            }

        }

        public boolean initialize() {
            close();

            SSHClient c = new SSHClient();

            try {
                c.setConnectTimeout(1000);
                c.addHostKeyVerifier(new PromiscuousVerifier());
                c.connect(host);

                if (method == Constants.APP_PROPERTY_EXECUTION_SERVICE_AUTH_METHOD_PWD) {
                    c.authPassword(user, Roddy.getApplicationProperty(Roddy.getRunMode(), Constants.APP_PROPERTY_EXECUTION_SERVICE_AUTH_PWD));
                } else {
                    c.authPublickey(user);
                }

                //At least for the moment compression is either not supported or maybe jzlib is not recognized
                //Finally compression does not work now with v. 0.9.2 of sshj
                if (RoddyConversionHelperMethods.toBoolean(Roddy.getApplicationProperty(Roddy.getRunMode(), Constants.APP_PROPERTY_EXECUTION_SERVICE_USE_COMPRESSION, Boolean.FALSE.toString()), false))
                    c.useCompression();

                c.startSession();

            } catch (Exception ex) {
            }
            client = c;
            sftpClient = client.newSFTPClient();
            scpFileTransfer = client.newSCPFileTransfer();
            scpDownloadClient = scpFileTransfer.newSCPDownloadClient();
        }

        public void acquire() {
            sshSemaphore.acquire();
        }

        public void release() {
            sshSemaphore.release();
        }
    }

    private static final class ConnectionPool {

        private final List<SSHPoolConnectionSet> poolEntries = new LinkedList<>();

        private Semaphore sshSemaphore = new Semaphore(8);

        private void _initialize() {
            String sshUser = Roddy.getApplicationProperty(Roddy.getRunMode(), Constants.APP_PROPERTY_EXECUTION_SERVICE_USER, System.getProperty("user.name"));
            if (sshUser == "USERNAME") sshUser = System.getProperty("user.name"); //Get the local name if USERNAME is set
            String sshMethod = Roddy.getApplicationProperty(Roddy.getRunMode(), Constants.APP_PROPERTY_EXECUTION_SERVICE_AUTH_METHOD, Constants.APP_PROPERTY_EXECUTION_SERVICE_AUTH_METHOD_PWD);

            if (sshMethod != Constants.APP_PROPERTY_EXECUTION_SERVICE_AUTH_METHOD_PWD && sshMethod != Constants.APP_PROPERTY_EXECUTION_SERVICE_AUTH_METHOD_KEYFILE)
                sshMethod = Constants.APP_PROPERTY_EXECUTION_SERVICE_AUTH_METHOD_PWD

            List<SSHPoolConnectionSet> tempEntries = new LinkedList<>();
            String[] sshHosts = Roddy.getApplicationProperty(Roddy.getRunMode(), Constants.APP_PROPERTY_EXECUTION_SERVICE_HOSTS).split(SPLIT_COMMA);
            int i = 0;
            for (String host : sshHosts) {
                SSHPoolConnectionSet cs = new SSHPoolConnectionSet(i++, sshUser, host, sshMethod);
                cs.initialize();
                if (cs.check())
                    tempEntries << cs;
            }

            synchronized (poolEntries) {
                poolEntries.clear();
                poolEntries.addAll(tempEntries);
            }
        }

        public boolean check() {
            if (poolEntries.size() == 0) return false;

            boolean valid = true;
            for (SSHPoolConnectionSet it in poolEntries) {
                valid &= it.check();
            }

            return valid;
        }

        public boolean initialize() {
            _initialize();
            return check();
        }

        public void acquire() {
            sshSemaphore.acquire();
        }

        public void release() {
            sshSemaphore.release();
        }

        private int roundTrip = -1;

//        if (!check())
//        initialize();
//        int[] queueLen = new int[poolEntries.size()];
//        poolEntries.sort( new Comparator<SSHPoolConnectionSet>() {
//            @Override
//            int compare(SSHPoolConnectionSet o1, SSHPoolConnectionSet o2) {
//                return o2.sshSemaphore.getQueueLength().compareTo(o1.sshSemaphore.getQueueLength());
//            }
//        })
//        String pe = new String();
//        poolEntries.each {SSHPoolConnectionSet it -> pe +="" + it.id + ":" + it.sshSemaphore.getQueueLength()+ " "; }
//        println(pe);
////            roundTrip++;
////            roundTrip %= poolEntries.size();
//        return poolEntries.get(0);
        public synchronized SSHPoolConnectionSet get() {
            if (!check())
                initialize();
            try {
                if (poolEntries.size() == 1)
                    return poolEntries.get(0);
                roundTrip++;
                roundTrip %= poolEntries.size();
                return poolEntries.get(roundTrip);
            } catch (any) {
                return poolEntries.get(0);
            }
        }

        public void close() {
            synchronized (poolEntries) {
                for (SSHPoolConnectionSet it in poolEntries) {
                    try {
                        it.close();
                    } catch (Exception ex) {
                        logger.postSometimesInfo("There were some problems when Roddy tried to close an SSH connection object.");
                        logger.postRareInfo(ex.toString());
                    }
                }
            }
        }
    }

    private static Lock lock = new ReentrantLock();

    private static final ConnectionPool connectionPool = new ConnectionPool();

    /**
     * Stores a map of temporary files with the path to their remote sources.
     */
    private static Map<File, File> _fileToTempFileMap = new LinkedHashMap<>();

    @Override
    boolean doesKnowTheUsername() {
        return true;
    }

    @Override
    String getUsername() {
        String userName = Roddy.getApplicationProperty(Roddy.getRunMode(), Constants.APP_PROPERTY_EXECUTION_SERVICE_USER);
        if (userName == "USERNAME") //Get the local username.
            userName = System.getProperty("user.name")
        return userName;
    }

    @Override
    public boolean initialize() {
        return initialize(false);
    }

    @Override
    boolean initialize(boolean waitFor) {
        if (!waitFor) {
            Thread.start { connectionPool.initialize() };
            return true;
        }

        return connectionPool.initialize();
    }

    @Override
    public void destroy() {
        synchronized (connectionPool) {
            connectionPool.close();
        }
    }

    @Override
    public boolean testConnection() {
        return connectionPool.check();
    }

    public SSHExecutionService() {
    }

    @Override
    boolean isLocalService() {
        return false;
    }

    private SSHPoolConnectionSet waitForService() {
        if (!connectionPool.check())
            connectionPool.initialize();
        return connectionPool.get();

//        SSHClient sshClient = set.client;
//
//            if (sshClient != null && !sshClient.isConnected()) {
//                fireExecutionServiceStateChange(TriState.FALSE);
//                logger.severe("Reconnecting!");
//                initIsInProgress = true;
//                sshClient = null;
//                initialize();
//                if (sshClient != null)
//                    logger.info("SSH client is connected: " + sshClient.isConnected());
//                else
//                    logger.info("SSH client is null and not connected.");
//            }
    }

//    @Override
    public
    synchronized List<String> _execute(String command, boolean waitFor = true, boolean ignoreError = false, OutputStream outputStream = null) {
        SSHPoolConnectionSet set = waitForService()
        SSHClient sshClient = set.client;

        if (waitFor) {
            long id = fireExecutionStartedEvent(command)

            set.acquire();
            Session session = sshClient.startSession();
            Session.Command cmd;
            try {
                cmd = session.exec(command)
            } finally {
                set.release();
            }
            String content = IOUtils.readFully(cmd.getInputStream()).toString();
            session.close();

            cmd.join();
            session.close();

            // Get the exit status of the process. In case of things like caught signals (SEGV-Segmentation fault), the value is null and will be set to 256.
            Integer exitStatus = cmd.getExitStatus();
            if (exitStatus == null) exitStatus = 256;

            List<String> output = new LinkedList<String>()
            output << "" + exitStatus;
            measureStop(id, "blocking command [sshclient:${set.id}] '" + RoddyIOHelperMethods.truncateCommand(command, 20) + "'");
            fireExecutionStoppedEvent(id, command);

            if (exitStatus > 0) {
                if (ignoreError) {
                    logger.severe("Command not executed correctly, return code: " + exitStatus + ", error was ignored on purpose.");
                    content.readLines().each { String line -> output << "" + line }
                } else {
                    logger.severe("Command not executed correctly, return code: " + exitStatus +
                                    (cmd.getExitSignal()
                                            ? " Caught signal is " + cmd.getExitSignal().name()
                                            : "\n\tCommand Str. " + RoddyIOHelperMethods.truncateCommand(command)));
                }
            } else {
                content.readLines().each { String line -> output << "" + line }
            }

            return output;
        }

        Runnable runnable = new Runnable() {

            @Override
            void run() {
                long id = fireExecutionStartedEvent(command)
                //Append a newgrp to each command, so that all command context in the proper group context.
                set.acquire();
                Session session = sshClient.startSession();
                Session.Command cmd;
                try {
                    cmd = session.exec(command)
                } finally {
                    set.release();
                }
                String content = IOUtils.readFully(cmd.getInputStream()).toString();
                session.close();
                measureStop(id, "async command  [sshclient:${set.id}] '" + RoddyIOHelperMethods.truncateCommand(command, 20) + "'");
                fireExecutionStoppedEvent(id, command)
            }
        }
        Thread thread = new Thread(runnable)
        thread.setName("SSHExecutionService::_execute()")
        thread.start();
        return ["0"];
    }


    @Override
    public ExecutionResult execute(String string, boolean waitFor = true) {
        List<String> result = _execute(string, waitFor);
        String returnCodeStr = result[0];
        int returnCode = result.size() > 0 && result[0] != "null" ? Integer.parseInt(returnCodeStr) : 256;
        result.remove(0);
        ExecutionResult er = new ExecutionResult(returnCode == 0, returnCode, result, "");
        fireStringExecutedEvent(string, er);
        return er;
    }

    @Override
    public void execute(Command command, boolean waitFor = true) {
        ExecutionContext run = command.getExecutionContext();
        //String commandString = command.toString();
        boolean preventCalls = run.getConfiguration().getPreventJobExecution();
        boolean pidIsBlocked = blockedPIDsForJobExecution.contains(command.getExecutionContext().getDataSet());
        boolean showCalls = run.getConfiguration().getShowSSHCalls();
        boolean isDummyCommand = Command instanceof Command.DummyCommand;
        String cmdString;
        if (!allJobsBlocked && !pidIsBlocked && !preventCalls && !isDummyCommand) {
            try {
                cmdString = command.toString();
                ExecutionResult res = null;
                if (run.getExecutionContextLevel() == ExecutionContextLevel.TESTRERUN) {
                    String pid = String.format("0x%08X", System.nanoTime());
                    res = new ExecutionResult(true, 0, [pid], pid);
                } else
                    res = execute(cmdString, waitFor);
                String exID = "none";
                if (res.successful) {
                    exID = CommandFactory.getInstance().parseJobID(res.resultLines[0]);
                    command.setExecutionID(CommandFactory.getInstance().createJobDependencyID(command.getJob(), exID));
                }
                logger.postSometimesInfo("${exID}, ${cmdString}");
                command.getExecutionContext().addCalledCommand(command);
            } catch (Exception ex) {
                logger.log(Level.SEVERE, ex.toString());
            }
        } else {
            String reason = "";
            if (preventCalls) {
                reason += "Configuration does not allow the execution of commands. "
            }
            if (pidIsBlocked) {
                reason += "The execution of jobs for this DataSet is stopped. "
            }
            if (allJobsBlocked) {
                reason += "The execution service is no longer allowed to execute commands. "
            }
            logger.log(Level.INFO, "Skipping command " + command + " for reason: " + reason);
        }
        fireCommandExecutedEvent(command);
    }

    @Override
    void copyFile(File _in, File _out) {
        copyFile(_in, _out, 0);
    }

    void copyFile(File _in, File _out, int retries) {
        boolean retry = false;
        boolean fileCopy = _in.isFile();
        String copyType = fileCopy ? "file" : "directory";

        long id = fireExecutionStartedEvent("")
        SSHPoolConnectionSet service = waitForAndAcquireService()
        try {
            service.sftpClient.getFileTransfer().upload(_in.absolutePath, _out.absolutePath);
        } catch (SFTPException ex) {
            if (retries < 3) {
                retry = true;
//                logger.warning("Catched no such file exception, attempting to retry copyFile ${_in.absolutePath} to ${_out.absolutePath}")
//                copyFile(_in, _out, retries + 1);
            } else if (retries >= 3) {
                logger.severe("Could not copy ${copyType} ${_in.absolutePath} to ${_out.absolutePath}");
                throw ex;
            }
        } finally {
            service.release();
            measureStop(id, "${copyType} copy [sshclient:${service.id}]");
            fireExecutionStoppedEvent(id, "");
        }
        if (retry) {
            logger.warning("Catched no such file exception, attempting to retry copyFile ${_in.absolutePath} to ${_out.absolutePath}")
            copyFile(_in, _out, retries + 1);
        }
    }

    @Override
    void copyDirectory(File _in, File _out) {
        copyDirectory(_in, _out, 0);
    }

    void copyDirectory(File _in, File _out, int retries) {
        File tempZip = File.createTempFile("roddy_", ".zip");
        tempZip.deleteOnExit();
        tempZip.delete();
        File roddyPath = _in.getParentFile().getAbsoluteFile();

        GString gString = "tar -C ${roddyPath.getAbsolutePath()} -zcvf ${tempZip.getAbsolutePath()} ${_in.getName()}"
        Process process = gString.execute()
        process.waitFor();

        copyFile(tempZip, _out, retries);
        String outPath = "${_out.getAbsolutePath()}/${tempZip.getName()}";
        execute("tar -C ${_out.getAbsolutePath()} -xzvf ${outPath} && rm ${outPath}", true);

        tempZip.delete();
    }

    @Override
    boolean modifyAccessRights(File file, String rightsStr, String groupID) {
        waitForService();
        long id = fireExecutionStartedEvent("")

        SSHPoolConnectionSet service = waitForAndAcquireService()
        boolean result = true;
        try {
            FileSystemInfoProvider fp = FileSystemInfoProvider.getInstance();
            if(rightsStr) service.sftpClient.chmod(file.getAbsolutePath(), convertToAccessRights(rightsStr));
            if(groupID) service.sftpClient.chgrp(file.getAbsolutePath(), fp.getGroupID(groupID));
        } catch (Exception ex) {
            logger.severe("Could not set access attributes for ${file.absolutePath}")
            result = false;
        } finally {
            service.release();
            measureStop(id, "chmod [sshclient:${service.id}]");
            fireExecutionStoppedEvent(id, "");
        }
        return result;
    }

    private int convertToAccessRights(String rightsStr) {
        Map<String, Integer> rights = [u: 07, g: 00, o: 00];
        String[] split = rightsStr.split(SPLIT_COMMA);
        for (String s in split) {
            if (s.contains(StringConstants.PLUS)) {
                String[] _s = s.split(StringConstants.SPLIT_PLUS);
                int number = _s[1].contains("r") ? 04 : 0;
                number += _s[1].contains("w") ? 02 : 0;
                number += _s[1].contains("x") ? 01 : 0;
                rights[_s[0]] = number;
            } else {
                //Possibly contains a -
                String[] _s = s.split(StringConstants.SPLIT_MINUS);
                int number = 07 - (_s[1].contains("r") ? 04 : 0);
                number -= _s[1].contains("w") ? 02 : 0;
                number -= _s[1].contains("x") ? 01 : 0;
                rights[_s[0]] = number;
            }
        }

        int rightsNo = rights["u"] * 0100 + rights["g"] * 010 + rights["o"];
        rightsNo
    }

    @Override
    void createFileWithRights(boolean atomic, File file, String accessRights, String groupID, boolean blocking) {
        long id = fireExecutionStartedEvent("")
        SSHPoolConnectionSet service = waitForAndAcquireService()
        try {
            Set<OpenMode> set = new HashSet<>();
            set.add(OpenMode.CREAT);
            set.add(OpenMode.WRITE);
            final RemoteFile f = service.sftpClient.open(file.getAbsolutePath(), set);
            f.close();
            FileSystemInfoProvider fp = FileSystemInfoProvider.getInstance();
            if(accessRights)
                service.sftpClient.chmod(file.getAbsolutePath(), convertToAccessRights(accessRights));
            if(groupID)
                service.sftpClient.chgrp(file.getAbsolutePath(), fp.getGroupID(groupID));
        } finally {
            service.release();
            measureStop(id, "touch [sshclient:${service.id}]");
            fireExecutionStoppedEvent(id, "");
        }
        modifyAccessRights(file, accessRights, groupID);
    }

    @Override
    void removeDirectory(File directory) {
        SSHPoolConnectionSet service = waitForAndAcquireService()
        try {
            waitForService().sftpClient.rmdir(directory.absolutePath)
        } catch (Exception ex) {
            println(ex);
        } finally {
            service.release();
        }
    }

    @Override
    void removeFile(File file) {
        SSHPoolConnectionSet service = waitForAndAcquireService()
        try {
            waitForService().sftpClient.rm(file.absolutePath)
        } finally {
            service.release();
        }
    }

    @Override
    void appendLinesToFile(boolean atomic, File file, List<String> lines, boolean blocking) {
        String text = lines.join(FileSystemInfoProvider.getInstance().getNewLineString());
        appendLineToFile(atomic, file, text, blocking);
    }

    @Override
    void appendLineToFile(boolean atomic, File file, String line, boolean blocking) {
        long id = fireExecutionStartedEvent("")
        SSHPoolConnectionSet service = waitForAndAcquireService()
        try {
            Set<OpenMode> set = new HashSet<>();
            set.add(OpenMode.APPEND);
            set.add(OpenMode.CREAT);
            set.add(OpenMode.WRITE);
            String sep = FileSystemInfoProvider.getInstance().getNewLineString();
            String lineNew = line + (!line.endsWith(sep) ? sep : "");
            final RemoteFile f = service.sftpClient.open(file.getAbsolutePath(), set);
            f.write(f.length(), lineNew.getBytes(), 0, lineNew.length());
            f.close();
        } finally {
            service.release();
            measureStop(id, "append to file [sshclient:${service.id}]");
            fireExecutionStoppedEvent(id, "");
        }
    }

    private SSHPoolConnectionSet waitForAndAcquireService() {
        final service = waitForService();
        service.acquire();
        service
    }

    @Override
    void writeTextFile(File file, String text) {
        copyFile(FileSystemInfoProvider.writeTextToTempFile(text), file);
    }

    @Override
    void writeBinaryFile(File file, Serializable serializable) {
        try {
            copyFile(FileSystemInfoProvider.serializeObjectToTempFile(serializable), file);
        } catch (Exception ex) {
            logger.warning("Could not write or serialize object ${serializable.toString()} to file ${file.absolutePath}. " + ex.toString());
        }
    }

    private final Map<File, File> tempFileByFile = new LinkedHashMap<>();
/**
 * Transfer a file from a remote location to a local temporary location.
 * Store the file and the temp file locations in a hashmap.
 * @param f
 * @param tempPre
 * @param tempPost
 */
    private File transferFileFromRemoteToLocal(File file, String tempPre, String tempPost) {
        long id = fireExecutionStartedEvent("")
//        boolean acquired = false;
        try {
            synchronized (file) {
                if (_fileToTempFileMap.containsKey(file)) {
                    logger.info("File ${file} was already transferred!")
                    return _fileToTempFileMap[file];
                } else {
                    File tempFile = File.createTempFile(tempPre, tempPost);
                    tempFile.deleteOnExit();

                    long t = measureStart();
                    SSHPoolConnectionSet service = waitForAndAcquireService()
                    try {
                        if (service.sftpClient.statExistence(file.getAbsolutePath()))
                            service.sftpClient.get(file.getAbsolutePath(), tempFile.getAbsolutePath());
                    } finally {
                        service.release();
                    }
                    measureStop(t, "transfer file [sshclient:${service.id}] ${file.getAbsolutePath()} from remote");
                    lock.lock();
                    try {
                        if (FileSystemInfoProvider.getInstance().isCachingAllowed(file))
                            _fileToTempFileMap.put(file, tempFile);
                    } finally {
                        lock.unlock();
                    }
                    return tempFile;
                }
            }
        } catch (OpenFailException ofe) { //Reduce semaphore count
//            sshSemaphore.decrementPermits();
            throw ofe;
        } finally {
//            if (acquired)
//                sshSemaphore.release();
            fireExecutionStoppedEvent(id, "");
            null;
        }
    }

    @Override
    Object loadBinaryFile(File file) {
        try {
            return FileSystemInfoProvider.deserializeObjectFromFile(transferFileFromRemoteToLocal(file, "roddy_sshserver_down", ".tmp"));
        } catch (Exception ex) {
            logger.warning("Could not read file ${file.absolutePath}");
            return null;
        }
    }

    @Override
    File getTemporaryFileForFile(File file) {
        lock.lock();
        try {
            return _fileToTempFileMap.get(file);
        } finally {
            lock.unlock();
        }
    }

    @Override
    boolean isAvailable() {
        return connectionPool.check();
    }

    private static Random random = new Random();

    @Override
    String[] loadTextFile(File file) {
        try {
            File tempFile = transferFileFromRemoteToLocal(file, "roddy_sshserver_down", ".tmp")
            return tempFile.readLines().toArray(new String[0]);
        } catch (Exception ex) {
            if (file == null) {
                logger.severe("Could not read file, variable file == null.");
            } else {
                if (ex instanceof OpenFailException) {
                    logger.warning("Could not read file ${file.absolutePath}, wait and retry.");
                    Thread.sleep(random.nextInt(70) + 5);
                    return loadTextFile(file);
                }
                logger.warning("Could not read file ${file.absolutePath}");
            }
            return new String[0];
        }
    }

    @Override
    List<File> listFiles(File file, List<String> filters) {
        try {
            long id = fireExecutionStartedEvent("")
            final service = waitForService();
            final List<RemoteResourceInfo> ls;
            service.acquire();
            try {
                // TODO Check first, if the directory is readable and accessible. If not, throw an error.
                ls = service.sftpClient.ls(file.absolutePath);
            }
            finally {
                service.release();
            }

            WildcardFileFilter wff = null;
            if (filters)
                wff = new WildcardFileFilter(filters);

            List<File> result = [];
            for (RemoteResourceInfo rinfo in ls) {
                if (!wff || (wff && wff.accept(new File(rinfo.path))))
                    result.add(new File(rinfo.path));
            }

            measureStop(id, "list files [sshclient:${service.id}:${id}] ${file.absolutePath}");
            fireExecutionStoppedEvent(id, "");
            return result;
        }

        catch (any) {
            []
        }
    }

    @Override
    List<File> listFiles(List<File> files, List<String> filters) {
        List<File> allfiles = [];
        for (File f in files)
            allfiles.addAll(listFiles(f, filters));
        return allfiles;
    }

    @Override
    public boolean fileExists(File file) {
        return checkExistence(file, FileMode.Type.REGULAR)
    }

    @Override
    public boolean directoryExists(File file) {
        return checkExistence(file, FileMode.Type.DIRECTORY)
    }

    private boolean checkExistence(File file, FileMode.Type typeToCheck) {
        SSHPoolConnectionSet service = waitForAndAcquireService()
        boolean typecheck = false;
        try {
            net.schmizz.sshj.sftp.FileAttributes attributes = service.sftpClient.statExistence(file.absolutePath);

            if (attributes && attributes.getType() == typeToCheck) typecheck = true;
        }
        finally {
            service.release();
        }
        return typecheck
    }

    @Override
    boolean isFileReadable(File f) {
        FileAttributes attributes = queryFileAttributes(f);
        if (attributes == null)
            return false;
        return attributes.userCanRead;
    }

    @Override
    boolean isFileWriteable(File f) {
        FileAttributes attributes = queryFileAttributes(f);
        if (attributes == null)
            return false;
        return attributes.userCanWrite;
    }

    @Override
    boolean isFileExecutable(File f) {
        FileAttributes attributes = queryFileAttributes(f);
        if (attributes == null)
            return false;
        return attributes.userCanRead && attributes.getUserCanExecute();
    }

    @Override
    FileAttributes queryFileAttributes(File file) {
        try {
            long id = fireExecutionStartedEvent("")
            net.schmizz.sshj.sftp.FileAttributes attributes
            SSHPoolConnectionSet service = waitForAndAcquireService()
            try {
                attributes = service.sftpClient.lstat(file.absolutePath);
            }
            finally {
                service.release();
            }

            measureStop(id, "query attributes [sshclient:${service.id}] of ${file.absolutePath}");
            fireExecutionStoppedEvent(id, "");
            FileAttributes newAttributes = new FileAttributes("" + attributes.getUID(), "" + attributes.getGID());
            newAttributes.setPermissions(
                    attributes.permissions.contains(net.schmizz.sshj.xfer.FilePermission.USR_R),
                    attributes.permissions.contains(net.schmizz.sshj.xfer.FilePermission.USR_W),
                    attributes.permissions.contains(net.schmizz.sshj.xfer.FilePermission.USR_X),
                    attributes.permissions.contains(net.schmizz.sshj.xfer.FilePermission.GRP_R),
                    attributes.permissions.contains(net.schmizz.sshj.xfer.FilePermission.GRP_W),
                    attributes.permissions.contains(net.schmizz.sshj.xfer.FilePermission.GRP_X),
                    attributes.permissions.contains(net.schmizz.sshj.xfer.FilePermission.OTH_R),
                    attributes.permissions.contains(net.schmizz.sshj.xfer.FilePermission.OTH_W),
                    attributes.permissions.contains(net.schmizz.sshj.xfer.FilePermission.OTH_X)
            );
            return newAttributes;
        } catch (any) {
            null
        }
    }

    @Override
    public boolean canCopyFiles() {
        return true;
    }

    @Override
    public boolean canReadFiles() {
        return true;
    }

    @Override
    public boolean canWriteFiles() {
        return true;
    }

    @Override
    boolean canDeleteFiles() {
        return false;
    }

    @Override
    public boolean canListFiles() { return true; }

    @Override
    public boolean canModifyAccessRights() { return true; }

    @Override
    boolean canQueryFileAttributes() { return true; }

    @Override
    void releaseCache() {

    }

}
