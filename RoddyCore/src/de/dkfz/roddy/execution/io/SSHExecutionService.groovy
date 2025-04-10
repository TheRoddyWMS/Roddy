/*
 * Copyright (c) 2016 German Cancer Research Center (Deutsches Krebsforschungszentrum, DKFZ).
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/TheRoddyWMS/Roddy/LICENSE.txt).
 */
package de.dkfz.roddy.execution.io

import com.jcraft.jsch.agentproxy.AgentProxy
import com.jcraft.jsch.agentproxy.Connector
import com.jcraft.jsch.agentproxy.ConnectorFactory
import com.jcraft.jsch.agentproxy.Identity
import com.jcraft.jsch.agentproxy.sshj.AuthAgent
import de.dkfz.roddy.Constants
import de.dkfz.roddy.ExitReasons
import de.dkfz.roddy.Roddy
import de.dkfz.roddy.SystemProperties
import de.dkfz.roddy.config.RoddyAppConfig
import de.dkfz.roddy.execution.io.fs.FileSystemAccessProvider
import de.dkfz.roddy.tools.LoggerWrapper
import de.dkfz.roddy.tools.RoddyConversionHelperMethods
import de.dkfz.roddy.tools.RoddyIOHelperMethods
import de.dkfz.roddy.tools.Tuple3
import groovy.transform.CompileStatic
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.connection.channel.OpenFailException
import net.schmizz.sshj.connection.channel.direct.Session
import net.schmizz.sshj.sftp.*
import net.schmizz.sshj.transport.verification.PromiscuousVerifier
import net.schmizz.sshj.userauth.UserAuthException
import net.schmizz.sshj.userauth.method.AuthMethod
import net.schmizz.sshj.xfer.scp.SCPDownloadClient
import net.schmizz.sshj.xfer.scp.SCPFileTransfer
import org.apache.commons.io.filefilter.WildcardFileFilter
import sun.reflect.generics.reflectiveObjects.NotImplementedException

import java.time.Duration
import java.time.temporal.TemporalUnit
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import java.util.function.Supplier

import static de.dkfz.roddy.StringConstants.SPLIT_COMMA

/**
 *
 * @author michael
 */
@CompileStatic
class SSHExecutionService extends RemoteExecutionService {

    public static final LoggerWrapper logger = LoggerWrapper.getLogger(SSHExecutionService.class.name)

    static class SSHPoolConnectionSet {
        public Session session

        public SSHClient client

        public SFTPClient sftpClient

        public SCPDownloadClient scpDownloadClient

        public SCPFileTransfer scpFileTransfer

        public final String user

        public final String host

        public final String method

        private final Semaphore sshSemaphore = new Semaphore(8)

        private final int id

        SSHPoolConnectionSet(int id, String user, String host, String method) {
            this.user = user
            this.host = host
            this.method = method
            this.id = id
        }

        boolean check() {
            if (client == null)
                return false

            if (!client.connected)
                return false

            if (!client.authenticated)
                return false

            return true
        }

        void close() {

            try {
                if (sftpClient != null) {
                    sftpClient.close()
                }
            } catch (Exception ex) {
                logger.rare("Could not close SFTP client object." + ex)
            }

            try {
                if (client != null) {
                    client.disconnect()
                    client.close()
                }
            } catch (Exception ex) {
                logger.rare("Could not close SSH client object." + ex)
            }

        }

        boolean initialize() {
            long t1 = System.nanoTime()
            close()

            SSHClient c = new SSHClient()
            long t2 = System.nanoTime()

            logger.sometimes(RoddyIOHelperMethods.printTimingInfo("create ssh client", t1, t2))
            try {
                c.setConnectTimeout(1000)
                c.addHostKeyVerifier(new PromiscuousVerifier())
                c.connect(host)
                t1 = System.nanoTime()
                logger.sometimes(RoddyIOHelperMethods.printTimingInfo("connect ssh client", t2, t1))

                if (method == Constants.APP_PROPERTY_EXECUTION_SERVICE_AUTH_METHOD_PWD) {
                    logger.always("Try setup the SSH connection ${user}@${host} using password authentification.")
                    c.authPassword(user, Roddy.applicationConfiguration.getOrSetApplicationProperty(Roddy.getRunMode(), Constants.APP_PROPERTY_EXECUTION_SERVICE_AUTH_PWD))
                } else if (method == Constants.APP_PROPERTY_EXECUTION_SERVICE_AUTH_METHOD_SSHAGENT) {
                    logger.always("Try setup the SSH connection ${user}@${host} using an ssh agent.")
                    c.auth(user, getAuthMethods(getAgentProxy()))
                } else if (method == Constants.APP_PROPERTY_EXECUTION_SERVICE_AUTH_METHOD_KEYFILE) {
                    logger.always("Try setup the SSH connection ${user}@${host} using a password less keyfile.")
                    String customKeyfile = Roddy.applicationConfiguration.getOrSetApplicationProperty(Roddy.getRunMode(), Constants.APP_PROPERTY_EXECUTION_SERVICE_AUTH_METHOD_KEYFILE_LOCATION, "")
                    if (customKeyfile) {
                        File _f = new File(customKeyfile)
                        if (!_f.canRead()) {
                            throw new UserAuthException("Cannot read the set keyfile: ${customKeyfile}")
                        }
                        def keys = c.loadKeys(customKeyfile)
                        c.authPublickey(user, keys)
                    } else {
                        c.authPublickey(user)
                    }
                } else {
                    logger.always("")
                }

                //At least for the moment compression is either not supported or maybe jzlib is not recognized
                //Finally compression does not work now with v. 0.9.2 of sshj
                if (RoddyConversionHelperMethods.toBoolean(Roddy.applicationConfiguration.getOrSetApplicationProperty(Roddy.getRunMode(), Constants.APP_PROPERTY_EXECUTION_SERVICE_USE_COMPRESSION, Boolean.FALSE.toString()), false))
                    c.useCompression()

                c.startSession()
                t2 = System.nanoTime()
                logger.sometimes(RoddyIOHelperMethods.printTimingInfo("start ssh client session", t1, t2))
            } catch (UnknownHostException ex) {
                Roddy.exitWithMessage(ExitReasons.unknownSSHHost)
            } catch (UserAuthException ex) {
                logger.severe(
                        [
                                "Could not setup SSH access with your configuration:",
                                "- Is the user and password combination right?",
                                "- Did you store the password and is it right?",
                                "- Did you use keyfile authentification and are there any keyfiles available and accessible?",
                                "- Did you set the custom keyfile location and is this file accessible?",
                                "- Did you try to use an ssh agent and is it setup properly? Was Roddy called properly?",
                                "- Did you check if the authentification method is right (keyfile, password, sshagent)?"
                        ].join("\n\t")
                )

                Roddy.exit(ExitReasons.invalidSSHConfig.code)
            } catch (Exception ex) {
                logger.severe("Fatal and unknown error during initialization of SSHExecutionService. Message: \"${ex.message}\".")
                Roddy.exit(ExitReasons.fatalSSHError.code)
            }
            client = c
            sftpClient = client.newSFTPClient()
            scpFileTransfer = client.newSCPFileTransfer()
            scpDownloadClient = scpFileTransfer.newSCPDownloadClient()
            t1 = System.nanoTime()
            logger.sometimes(RoddyIOHelperMethods.printTimingInfo("create additional ssh services", t2, t1))

        }

        AgentProxy getAgentProxy() {
            Connector connector = ConnectorFactory.default.createConnector()
            if (connector != null)
                return new AgentProxy(connector)
            return null
        }

        List<AuthMethod> getAuthMethods(AgentProxy agent) throws Exception {
            return agent.identities.collect { Identity it -> new AuthAgent(agent, it) } as List<AuthMethod>
        }

        void acquire() {
            sshSemaphore.acquire()
        }

        void release() {
            sshSemaphore.release()
        }
    }

    private static final class ConnectionPool {

        private final List<SSHPoolConnectionSet> poolEntries = new LinkedList<>()

        private Semaphore sshSemaphore = new Semaphore(8)

        private void _initialize() {
            RoddyAppConfig appConf = Roddy.applicationConfiguration
            String sshUser = appConf.getOrSetApplicationProperty(Roddy.getRunMode(), Constants.APP_PROPERTY_EXECUTION_SERVICE_USER, SystemProperties.getUserName())
            if (sshUser == Constants.USERNAME) sshUser = SystemProperties.getUserName() //Get the local name if USERNAME is set
            String sshMethod = appConf.getOrSetApplicationProperty(Roddy.getRunMode(), Constants.APP_PROPERTY_EXECUTION_SERVICE_AUTH_METHOD, Constants.APP_PROPERTY_EXECUTION_SERVICE_AUTH_METHOD_PWD)

            if (![Constants.APP_PROPERTY_EXECUTION_SERVICE_AUTH_METHOD_PWD, Constants.APP_PROPERTY_EXECUTION_SERVICE_AUTH_METHOD_KEYFILE, Constants.APP_PROPERTY_EXECUTION_SERVICE_AUTH_METHOD_SSHAGENT].contains(sshMethod))
                sshMethod = Constants.APP_PROPERTY_EXECUTION_SERVICE_AUTH_METHOD_PWD


            List<SSHPoolConnectionSet> tempEntries = new LinkedList<>()
            String[] sshHosts = appConf.getOrSetApplicationProperty(Roddy.getRunMode(), Constants.APP_PROPERTY_EXECUTION_SERVICE_HOSTS).split(SPLIT_COMMA)
            int i = 0
            for (String host : sshHosts) {
                logger.always("Opening SSH connection: $sshUser@$host via $sshMethod")
                SSHPoolConnectionSet cs = new SSHPoolConnectionSet(i++, sshUser, host, sshMethod)
                cs.initialize()
                if (cs.check())
                    tempEntries << cs
            }

            synchronized (poolEntries) {
                poolEntries.clear()
                poolEntries.addAll(tempEntries)
            }
        }

        boolean check() {
            if (poolEntries.size() == 0) return false

            boolean valid = true
            for (SSHPoolConnectionSet it in poolEntries) {
                valid &= it.check()
            }

            return valid
        }

        boolean initialize() {
            _initialize()
            return check()
        }

        void acquire() {
            sshSemaphore.acquire()
        }

        void release() {
            sshSemaphore.release()
        }

        private int roundTrip = -1

        synchronized SSHPoolConnectionSet get() {
            if (!check())
                initialize()
            try {
                if (poolEntries.size() == 1)
                    return poolEntries.get(0)
                roundTrip++
                roundTrip %= poolEntries.size()
                return poolEntries.get(roundTrip)
            } catch (any) {
                return poolEntries.get(0)
            }
        }

        void close() {
            synchronized (poolEntries) {
                for (SSHPoolConnectionSet it in poolEntries) {
                    try {
                        it.close()
                    } catch (Exception ex) {
                        logger.sometimes("There were some problems when Roddy tried to close an SSH connection object.")
                        logger.rare(ex.toString())
                    }
                }
            }
        }
    }

    private static Lock lock = new ReentrantLock()

    private static final ConnectionPool connectionPool = new ConnectionPool()

    /**
     * Stores a map of temporary files with the path to their remote sources.
     */
    private static Map<File, File> _fileToTempFileMap = new LinkedHashMap<>()

    @Override
    boolean doesKnowTheUsername() {
        return true
    }

    @Override
    String getUsername() {
        String userName = Roddy.applicationConfiguration.getOrSetApplicationProperty(Roddy.getRunMode(), Constants.APP_PROPERTY_EXECUTION_SERVICE_USER)
        if (userName == Constants.USERNAME) //Get the local username.
            userName = SystemProperties.userName
        return userName
    }

    @Override
    boolean initialize() {
        return connectionPool.initialize()
    }

    @Override
    void destroy() {
        synchronized (connectionPool) {
            connectionPool.close()
        }
    }

    @Override
    boolean testConnection() {
        return connectionPool.check()
    }

    SSHExecutionService() {
    }

    private SSHPoolConnectionSet waitForService() {
        if (!connectionPool.check())
            connectionPool.initialize()
        return connectionPool.get()
    }

    String readStream(InputStream inputStream) {
        ByteArrayOutputStream result = new ByteArrayOutputStream()
        byte[] buffer = new byte[1024]
        int length
        while ((length = inputStream.read(buffer)) != -1)
            result.write(buffer, 0, length)
        return result.toString("UTF-8")
    }

    private static final ExecutorService executorService = Executors.newCachedThreadPool()

    @Override
    ExecutionResult _execute(String command,
                             boolean waitFor = true,
                             Duration timeout = Duration.ZERO,
                             OutputStream outputStream = null) {
        _execute(command, waitFor, timeout, outputStream, executorService)
    }

    /**
     * An com.hierynomus:sshj:0.23.0 and JSCH Agent 0.0.9 (-> net.schmizz:sshj:0.10.0) compatibility
     * problem prevents the usage of Command.join(long, TimeUnit) from working. The best solution is
     * probably to get rid of the whole SSHJ/JSCH stack and instead use mina-sshd, which is not
     * completely outdated and actively maintained (and can use the SSH-agent and execute with timeouts.
     *
     * The solution taken now uses the Linux command `timeout`.
     *
     * @param command
     * @param waitFor
     * @param timeout
     * @param outputStream
     * @param executorService
     * @return
     */
    ExecutionResult _execute(String command,
                             boolean waitFor = true,
                             Duration timeout = Duration.ZERO,
                             OutputStream outputStream = null,
                             ExecutorService executorService) {

        CompletableFuture<Tuple3<Integer,List<String>, List<String>>> processF = CompletableFuture.supplyAsync({
            SSHPoolConnectionSet connectionSet = waitForService()
            SSHClient sshClient = connectionSet.client
            connectionSet.acquire()
            Session session = sshClient.startSession()
            List<String> stdout
            List<String> stderr
            Integer exitCode
            try {
                Session.Command executingCommand
                if (timeout != Duration.ZERO) {
                    // TODO Remove this workaround. Try switching to mina-sshd.
                    Long seconds = Math.round(Math.ceil(timeout.toNanos() / 1000000000.0d))
                    Long killSeconds = 60
                    String timeoutCommand = "timeout --preserve-status -s TERM -k ${killSeconds}s ${seconds}s"
                    executingCommand = session.exec("${timeoutCommand} ${command}")
                } else {
                    executingCommand = session.exec(command)
                }
                if (outputStream) {
                    stdout = LocalExecutionHelper.readStringStream(executingCommand.inputStream, outputStream)
                } else {
                    stdout = LocalExecutionHelper.readStringStream(executingCommand.inputStream)
                }
                stderr = LocalExecutionHelper.readStringStream(executingCommand.errorStream)

                executingCommand.join()
                exitCode = executingCommand.exitStatus
            } finally {
                connectionSet.release()
                session.close()
            }

            new Tuple3<Integer,List<String>, List<String>>(exitCode, stdout, stderr)
        } as Supplier<Tuple3<Integer,List<String>, List<String>>>, executorService)

        AsyncExecutionResult result = new AsyncExecutionResult([command], null as Integer,
            processF.thenApply { it.x as Integer },
            processF.thenApply { it.y as List<String> },
            processF.thenApply { it.z as List<String> })

        if (waitFor) {
            return result.asSynchronousExecutionResult()
        } else {
            return result
        }
    }

//    @Override
//    public ExecutionResult execute(String string, boolean waitFor = true) {
//        ExecutionResult result = _execute(string, waitFor);
//        int returnCode = result.size() > 0 && result[0] != "null" ? result.exitValue : 256;
//        result.remove(0)
//        ExecutionResult er = new ExecutionResult(returnCode == 0, returnCode, result, "");
//        return _execute(string, waitFor)
//    }

    @Override
    boolean copyFile(File _in, File _out) {
        return copyFile(_in, _out, 0)
    }

    boolean copyFile(File _in, File _out, int retries) {
        boolean retry = false
        boolean fileCopy = _in.isFile()
        String copyType = fileCopy ? "file" : "directory"

//        long id = fireExecutionStartedEvent("")
        SSHPoolConnectionSet service = waitForAndAcquireService()
        boolean result
        try {
            service.sftpClient.fileTransfer.upload(_in.absolutePath, _out.absolutePath)
            result = true
        } catch (SFTPException ex) {
            if (retries < 3) {
                retry = true
            } else if (retries >= 3) {
                logger.severe("Could not copy ${copyType} ${_in.absolutePath} to ${_out.absolutePath}")
                throw ex
            }
        } finally {
            service.release()
//            measureStop(id, "${copyType} copy [sshclient:${service.id}]");
//            fireExecutionStoppedEvent(id, "");
        }
        if (retry) {
            logger.warning("Caught no such file exception, attempting to retry copyFile ${_in.absolutePath} to ${_out.absolutePath}")
            result = copyFile(_in, _out, retries + 1)
        }
        return result
    }

    @Override
    boolean copyDirectory(File _in, File _out) {
        return copyDirectory(_in, _out, 0)
    }

    boolean copyDirectory(File _in, File _out, int retries) {
        File tempZip = File.createTempFile("roddy_", ".zip")
        tempZip.deleteOnExit()
        tempZip.delete()
        File roddyPath = _in.parentFile.absoluteFile

        // TODO Get the following from the CommandSet
        GString gString = "tar -C ${roddyPath.absolutePath} -zcvf ${tempZip.absolutePath} ${_in.name}"
        Process process = gString.execute()
        String outPath = "${_out.absolutePath}/${tempZip.name}"
        boolean result = process.waitFor() &&
                copyFile(tempZip, _out, retries) &&
                execute("tar -C ${_out.absolutePath} -xzvf ${outPath} && rm ${outPath}",
                        true)
        tempZip.delete()
        return result
    }

    @Override
    boolean modifyAccessRights(File file, String rightsStr, String groupID) {
        waitForService()
//        long id = fireExecutionStartedEvent("")

        SSHPoolConnectionSet service = waitForAndAcquireService()
        boolean result = true
        try {
            FileSystemAccessProvider fp = FileSystemAccessProvider.instance
            if (rightsStr) service.sftpClient.chmod(file.absolutePath, RoddyIOHelperMethods.symbolicToIntegerAccessRights(rightsStr, FileSystemAccessProvider.instance.getDefaultUserMask()))
            if (groupID) service.sftpClient.chgrp(file.absolutePath, fp.getGroupID(groupID))
        } catch (Exception ex) {
            logger.severe("Could not set access attributes for ${file.absolutePath}")
            result = false
        } finally {
            service.release()
//            measureStop(id, "chmod [sshclient:${service.id}]");
//            fireExecutionStoppedEvent(id, "");
        }
        return result
    }

    @Override
    boolean createFileWithRights(boolean atomic, File file, String accessRights, String groupID, boolean blocking) {
//        long id = fireExecutionStartedEvent("")
        SSHPoolConnectionSet service = waitForAndAcquireService()
        try {
            Set<OpenMode> set = new HashSet<>()
            set.add(OpenMode.CREAT)
            set.add(OpenMode.WRITE)
            final RemoteFile f = service.sftpClient.open(file.absolutePath, set)
            f.close()
            FileSystemAccessProvider fp = FileSystemAccessProvider.instance
            if (accessRights)
                service.sftpClient.chmod(file.absolutePath, RoddyIOHelperMethods.symbolicToIntegerAccessRights(accessRights, FileSystemAccessProvider.instance.getDefaultUserMask()))
            if (groupID)
                service.sftpClient.chgrp(file.absolutePath, fp.getGroupID(groupID))
        } finally {
            service.release()
//            measureStop(id, "touch [sshclient:${service.id}]");
//            fireExecutionStoppedEvent(id, "");
        }
        return modifyAccessRights(file, accessRights, groupID)
    }

    @Override
    boolean removeDirectory(File directory) {
        SSHPoolConnectionSet service = waitForAndAcquireService()
        boolean result = true
        try {
            waitForService().sftpClient.rmdir(directory.absolutePath)
        } catch (Exception ex) {
            logger.warning("Could not remove directory ${directory.absolutePath}")
            result = false
        } finally {
            service.release()
        }
        return result
    }

    @Override
    boolean removeFile(File file) {
        SSHPoolConnectionSet service = waitForAndAcquireService()
        boolean result = true
        try {
            waitForService().sftpClient.rm(file.absolutePath)
            /* Philip: Not removing a file with e.g. patient data is more severe than not removing a directory. Therefore,
             *         propagate the error. */
        } finally {
            service.release()
        }
        return result
    }

    @Override
    boolean appendLinesToFile(boolean atomic, File file, List<String> lines, boolean blocking) {
        String text = lines.join(FileSystemAccessProvider.instance.getNewLineString())
        appendLineToFile(atomic, file, text, blocking)
    }

    @Override
    boolean appendLineToFile(boolean atomic, File file, String line, boolean blocking) {
//        long id = fireExecutionStartedEvent("")
        SSHPoolConnectionSet service = waitForAndAcquireService()
        boolean result
        try {
            Set<OpenMode> set = new HashSet<>()
            set.add(OpenMode.APPEND)
            set.add(OpenMode.CREAT)
            set.add(OpenMode.WRITE)
            String sep = FileSystemAccessProvider.instance.getNewLineString()
            String lineNew = line + (!line.endsWith(sep) ? sep : "")
            final RemoteFile f = service.sftpClient.open(file.absolutePath, set)
            f.write(f.length(), lineNew.getBytes(), 0, lineNew.length())
            f.close()
        } finally {
            service.release()
//            measureStop(id, "append to file [sshclient:${service.id}]");
//            fireExecutionStoppedEvent(id, "");
        }
        return result
    }

    private SSHPoolConnectionSet waitForAndAcquireService() {
        final service = waitForService()
        service.acquire()
        return service
    }

    @Override
    boolean writeTextFile(File file, String text) {
        return copyFile(FileSystemAccessProvider.writeTextToTempFile(text), file)
    }

    @Override
    boolean writeBinaryFile(File file, Serializable serializable) {
        boolean result
        try {
            result = copyFile(FileSystemAccessProvider.serializeObjectToTempFile(serializable), file)
        } catch (Exception ex) {
            logger.warning("Could not write or serialize object ${serializable.toString()} to file ${file.absolutePath}. " + ex.toString())
        }
        return result
    }

    private final Map<File, File> tempFileByFile = new LinkedHashMap<>()
/**
 * Transfer a file from a remote location to a local temporary location.
 * Store the file and the temp file locations in a hashmap.
 * @param f
 * @param tempPre
 * @param tempPost
 */
    private File transferFileFromRemoteToLocal(File file, String tempPre, String tempPost) {
//        long id = fireExecutionStartedEvent("")
//        boolean acquired = false;
        try {
            synchronized (file) {
                if (_fileToTempFileMap.containsKey(file)) {
                    logger.info("File ${file} was already transferred!")
                    return _fileToTempFileMap[file]
                } else {
                    File tempFile = File.createTempFile(tempPre, tempPost)
                    tempFile.deleteOnExit()

                    long t = measureStart()
                    SSHPoolConnectionSet service = waitForAndAcquireService()
                    try {
                        if (service.sftpClient.statExistence(file.absolutePath))
                            service.sftpClient.get(file.absolutePath, tempFile.absolutePath)
                    } finally {
                        service.release()
                    }
                    measureStop(t, "transfer file [sshclient:${service.id}] ${file.absolutePath} from remote")
                    lock.lock()
                    try {
                        if (FileSystemAccessProvider.instance.isCachingAllowed(file))
                            _fileToTempFileMap.put(file, tempFile)
                    } finally {
                        lock.unlock()
                    }
                    return tempFile
                }
            }
        } catch (OpenFailException ofe) { //Reduce semaphore count
//            sshSemaphore.decrementPermits();
            throw ofe
        } finally {
//            if (acquired)
//                sshSemaphore.release();
//            fireExecutionStoppedEvent(id, "");
            null
        }
    }

    @Override
    Object loadBinaryFile(File file) {
        try {
            return FileSystemAccessProvider.deserializeObjectFromFile(transferFileFromRemoteToLocal(file, "roddy_sshserver_down", ".tmp"))
        } catch (Exception ex) {
            logger.warning("Could not read file ${file.absolutePath}")
            return null
        }
    }

    @Override
    File getTemporaryFileForFile(File file) {
        lock.lock()
        try {
            return _fileToTempFileMap.get(file)
        } finally {
            lock.unlock()
        }
    }

    @Override
    boolean isAvailable() {
        return connectionPool.check()
    }

    @Override
    File queryWorkingDirectory() {
        def rs = _execute("pwd")
        if (rs.successful)
            return new File(rs.stdout[1])
        return null
    }
    private static Random random = new Random()

    @Override
    String[] loadTextFile(File file) {
        try {
            File tempFile = transferFileFromRemoteToLocal(file, "roddy_sshserver_down", ".tmp")
            return tempFile.readLines().toArray(new String[0])
        } catch (Exception ex) {
            if (file == null) {
                logger.severe("Could not read file, variable file == null.")
            } else {
                if (ex instanceof OpenFailException) {
                    logger.warning("Could not read file ${file.absolutePath}, wait and retry.")
                    Thread.sleep(random.nextInt(70) + 5)
                    return loadTextFile(file)
                }
                logger.warning("Could not read file ${file.absolutePath}")
            }
            return new String[0]
        }
    }

    @Override
    List<File> listFiles(File file, List<String> filters) {
        try {
//            long id = fireExecutionStartedEvent("")
            final service = waitForService()
            final List<RemoteResourceInfo> ls
            service.acquire()
            try {
                File parentDir = file.parentFile
                if (directoryExists(parentDir) &&
                        isFileReadable(parentDir) &&
                        isFileExecutable(parentDir)) {
                    ls = service.sftpClient.ls(file.absolutePath)
                } else {
                    throw new RuntimeException("Path '" + parentDir.absolutePath + " cannot be accessed.")
                }
            }
            finally {
                service.release()
            }

            WildcardFileFilter wff = null
            if (filters)
                wff = new WildcardFileFilter(filters)

            List<File> result = []
            for (RemoteResourceInfo rinfo in ls) {
                if (!wff || (wff && wff.accept(new File(rinfo.path))))
                    result.add(new File(rinfo.path))
            }

//            measureStop(id, "list files [sshclient:${service.id}:${id}] ${file.absolutePath}");
//            fireExecutionStoppedEvent(id, "");
            return result
        }

        catch (any) {
            []
        }
    }

    @Override
    List<File> listFiles(List<File> files, List<String> filters) {
        List<File> allfiles = []
        for (File f in files)
            allfiles.addAll(listFiles(f, filters))
        return allfiles
    }

    @Override
    boolean fileExists(File file) {
        return checkExistence(file, FileMode.Type.REGULAR)
    }

    @Override
    boolean directoryExists(File file) {
        return checkExistence(file, FileMode.Type.DIRECTORY)
    }

    private boolean checkExistence(File file, FileMode.Type typeToCheck) {
        SSHPoolConnectionSet service = waitForAndAcquireService()
        boolean typecheck = false
        try {
            net.schmizz.sshj.sftp.FileAttributes attributes = service.sftpClient.statExistence(file.absolutePath)

            if (attributes && attributes.getType() == typeToCheck) typecheck = true
        }
        finally {
            service.release()
        }
        return typecheck
    }

    @Override
    boolean isFileReadable(File f) {
        FileAttributes attributes = queryFileAttributes(f)
        if (attributes == null)
            return false
        return attributes.userCanRead
    }

    @Override
    boolean isFileWriteable(File f) {
        FileAttributes attributes = queryFileAttributes(f)
        if (attributes == null)
            return false
        return attributes.userCanWrite
    }

    @Override
    boolean isFileExecutable(File f) {
        FileAttributes attributes = queryFileAttributes(f)
        if (attributes == null)
            return false
        return attributes.userCanRead && attributes.userCanExecute
    }

    @Override
    FileAttributes queryFileAttributes(File file) {
        try {
//            long id = fireExecutionStartedEvent("")
            net.schmizz.sshj.sftp.FileAttributes attributes
            SSHPoolConnectionSet service = waitForAndAcquireService()
            try {
                attributes = service.sftpClient.lstat(file.absolutePath)
            }
            finally {
                service.release()
            }

//            measureStop(id, "query attributes [sshclient:${service.id}] of ${file.absolutePath}");
//            fireExecutionStoppedEvent(id, "");
            FileAttributes newAttributes = new FileAttributes("" + attributes.UID, "" + attributes.GID)
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
            )
            return newAttributes
        } catch (any) {
            null
        }
    }

    @Override
    boolean canCopyFiles() {
        return true
    }

    @Override
    boolean canReadFiles() {
        return true
    }

    @Override
    boolean canWriteFiles() {
        return true
    }

    @Override
    boolean canDeleteFiles() {
        return false
    }

    @Override
    boolean canListFiles() { return true }

    @Override
    boolean canModifyAccessRights() { return true }

    @Override
    boolean canQueryFileAttributes() { return true }

}
