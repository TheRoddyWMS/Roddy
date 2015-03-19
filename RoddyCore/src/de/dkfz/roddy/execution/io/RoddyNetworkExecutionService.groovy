package de.dkfz.roddy.execution.io

//import de.dkfz.memstreamer.MemoryStreamerInstance
import de.dkfz.roddy.Constants
import de.dkfz.roddy.Roddy

import static de.dkfz.roddy.StringConstants.SPLIT_COMMA

/**
 * Fast network execution service for secured networks (VPN etc.).
 *
 * The service needs to be available on the target machine.
 *
 * TODO For later versions: Make Roddy autostart the server.
 * Created by michael on 16.07.14.
 */
public class RoddyNetworkExecutionService extends RemoteExecutionService {
    public static final String LIST_FILES_IN_DIR = "listFiles_S"
    public static final String LIST_FILES_IN_DIRECTORIES = "listFiles_M"
    private Socket socketToServer;

    @Override
    protected
    synchronized List<String> _execute(String string, boolean waitFor, boolean ignoreErrors, OutputStream outputStream) {
        if (socketToServer == null || socketToServer.isClosed()) {
            String[] sshHosts = Roddy.getApplicationProperty(Roddy.getRunMode(), Constants.APP_PROPERTY_EXECUTION_SERVICE_HOSTS).split(SPLIT_COMMA);
            socketToServer = new Socket(sshHosts[0], 44900);
        }

        def networkOutputStream = socketToServer.getOutputStream();
        MemoryStreamerInstance.writeStringToStream(networkOutputStream, string);
//        def bytes = string.getBytes()
//        networkOutputStream.write(bytes);
//        networkOutputStream.flush();
//        networkOutputStream.close();

        def inputStream = socketToServer.getInputStream();
//        BufferedReader br = new BufferedReader(new InputStreamReader(socketToServer.getInputStream()));
//        String line = null;
//
        List<String> resultLines = [];
//        while ((line = br.readLine()) != null) {
//            resultLines << (line);
//        }
        int result = MemoryStreamerInstance.readIntFromStream(inputStream);
        resultLines << "${result}";
        resultLines << MemoryStreamerInstance.readStringFromSocketStream(inputStream);
        int count = MemoryStreamerInstance.readIntFromStream(inputStream);
        for (int i = 0; i < count; i++) {
            resultLines << MemoryStreamerInstance.readStringFromSocketStream(inputStream);
        }

//        def reader = ;
//        String line;
//        while(line = reader.readLines() != null) {
//            resultLines << line;
//        }
        inputStream.close();
//        socketToServer.close();

        return resultLines;
    }

    @Override
    boolean isAvailable() {
        return true
    }

    @Override
    void releaseCache() {

    }

    @Override
    boolean canListFiles() {
        return false;
    }

    @Override
    List<File> listFiles(File file, List<String> filters) {
        StringBuffer cmd = new StringBuffer(LIST_FILES_IN_DIR);
        cmd << "," << file.absolutePath << ","
        if(filters) {
            cmd << filters.size() << "," << filters.join(":");
        } else {
            cmd << 0;
        }
        _execute(cmd.toString(), true, true, null);
    }

    @Override
    List<File> listFiles(List<File> file, List<String> filters) {
        StringBuffer cmd = new StringBuffer(LIST_FILES_IN_DIRECTORIES);
        cmd << "," << file.size() << "," << file.join(":") << ",";
        if(filters) {
            cmd << filters.size() << "," << filters.join(":");
        } else {
            cmd << 0;
        }
        _execute(cmd.toString(), true, true, null);
    }
}
