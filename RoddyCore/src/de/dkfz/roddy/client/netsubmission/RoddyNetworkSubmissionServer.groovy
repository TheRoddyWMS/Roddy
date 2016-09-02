/*
 * Copyright (c) 2016 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy

//import de.dkfz.memstreamer.MemoryStreamerInstance
import de.dkfz.roddy.execution.io.LocalExecutionService
import de.dkfz.roddy.tools.RoddyConversionHelperMethods

/**
 * Server class for fast network service.
 * This is an unsecured class. A simple form of encryption might be used but it is supposed to run in secured networks.
 */
@groovy.transform.CompileStatic
public class RoddyNetworkSubmissionServer {

    private static final LocalExecutionService service = new LocalExecutionService();

    /**
     * Starts up the server enabling the network service.
     * @param args
     */
    public static void main(String[] args) {
        try {
            //Start the server and listen for connections.
            ServerSocket serverSocket = new ServerSocket(44900);

            while (true) {
                final Socket socket = serverSocket.accept();
                Thread.start {
                    println("Accept new connection.");
                    final def inputStream = socket.getInputStream();
                    final def outputStream = socket.getOutputStream();
                    handleSocket(inputStream, outputStream);
//                    socket.close();
                }
            }

        } catch (Exception ex) {
            System.out.println(ex);
            System.exit(1);
        }

        //Just exit without an error
        System.exit(0);
    }

    static volatile int sequence = 0;

    private static void handleSocket(InputStream input, OutputStream output) {
//        int localSequence;
//        synchronized (RoddyNetworkSubmissionServer.class) {
//            localSequence = sequence;
//            sequence++;
//        }
//        String cmd = MemoryStreamerInstance.readStringFromSocketStream(input);
//        println("${localSequence} : ${cmd}");
//        List<String> lines = [];
//        if (cmd.startsWith(RoddyNetworkExecutionService.LIST_FILES_IN_DIR)) {
//            lines = listFilesInDirectory(cmd);
//        } else if (cmd.startsWith(RoddyNetworkExecutionService.LIST_FILES_IN_DIRECTORIES)) {
//            lines = listFilesInDirectories(cmd)
//        } else {
//            def result = service.execute(cmd, true);
//            lines = new LinkedList<>([ "" + result.errorNumber, result.processID ] + result.resultLines);
//        }
//
//        MemoryStreamerInstance.writeIntToStream(output, RoddyConversionHelperMethods.toInt(lines[0]));
//        MemoryStreamerInstance.writeStringToStream(output, lines[1]);
//        def numberOfLines = lines.size()
//        MemoryStreamerInstance.writeIntToStream(output, numberOfLines);
//        for (int i = 2; i < lines.size(); i++) {
//            MemoryStreamerInstance.writeStringToStream(output, lines[i]);
//        }
//        println("${localSequence} :  # ${numberOfLines} lines");
//        output.flush();
    }

    public static List<String> listFilesInDirectory(String cmd) {
        String[] split = cmd.split(",");
        File dir = new File(split[1]);
        List<String> filters = [];
        if (RoddyConversionHelperMethods.toInt(split[2]) > 0) {
            filters += split[3].split(":").toList();
        }

        return new LinkedList<>([ "0", "0" ] + service.listFiles(dir, filters).collect { File f -> return f.getAbsolutePath(); });
    }

    public static List<String> listFilesInDirectories(String cmd) {
        String[] split = cmd.split(",");
        List<File> files = split[2].split(":").collect { String it -> new File(it); }
        List<String> filters = [];
        if (RoddyConversionHelperMethods.toInt(split[3]) > 0) {
            filters += split[4].split(":").toList();
        }

        return new LinkedList<>([ "0", "0" ] + service.listFiles(files, filters).collect { File f -> return f.getAbsolutePath(); });
    }
}
