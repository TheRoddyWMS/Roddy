package de.dkfz.roddy.tools

import de.dkfz.roddy.Constants
import de.dkfz.roddy.StringConstants
import de.dkfz.roddy.execution.io.ExecutionHelper
import de.dkfz.roddy.execution.io.ExecutionService
import groovy.io.FileType
import org.apache.commons.codec.digest.DigestUtils

import java.util.logging.Logger

//import de.dkfz.roddy.core.RunService
/**
 * Contains methods which print out text on the console, like listworkflows.
 * This is a bit easier in groovy so we'll use it for these tasks.
 *
 * User: michael
 * Date: 27.11.12
 * Time: 09:09
 */
@groovy.transform.CompileStatic
class RoddyIOHelperMethods {


    public static abstract class Compressor {
        public void compress(File from, File to, File workingDirectory) {
            if (from.isFile())
                compressFile(from, to, workingDirectory);

            else if (from.isDirectory())
                compressDirectory(from, to, workingDirectory);
        }

        public void compress(File from, File to) {
            compress(from, to, null);
        }

        public abstract void decompress(File from, File to);

        public abstract void decompress(File from, File to, File workingDirectory);

        public abstract void compressFile(File from, File to);

        public abstract void compressFile(File from, File to, File workingDirectory);

        public abstract void compressDirectory(File from, File to);

        public abstract void compressDirectory(File from, File to, File workingDirectory);

        public abstract GString getCompressionString(File from, File to);

        public abstract GString getDecompressionString(File from, File to);

        public abstract GString getCompressionString(File from, File to, File workingDirectory);

        public abstract GString getDecompressionString(File from, File to, File workingDirectory);
    }

    /**
     * Produces md5 compatible zipped archives of the input file / folder
     * Also produces a file named [zip.gz].md5
     */
    public static class NativeLinuxZipCompressor extends Compressor {

        @Override
        void compressFile(File from, File to, File workingDirectory = null) {
            compressDirectory(from, to, workingDirectory);
        }

        @Override
        void compressDirectory(File from, File to, File workingDirectory = null) {

            String result = ExecutionHelper.executeSingleCommand(getCompressionString(from, to, workingDirectory).toString());
//            println(result);
        }

        @Override
        void decompress(File from, File to, File workingDirectory = null) {
            String result = ExecutionHelper.executeSingleCommand(getDecompressionString(from, to, workingDirectory));
//            println(result);
        }

        private String getWorkingDirectory(File from, File workingDirectory = null) {
            if (!workingDirectory) {
                workingDirectory = from.getParentFile().getAbsoluteFile();
            }
            String wdPath = workingDirectory ? "cd ${workingDirectory.getAbsolutePath()} &&" : "";
            return wdPath;
        }

        @Override
        GString getCompressionString(File from, File to, File workingDirectory = null) {
            String wdPath = getWorkingDirectory(from, workingDirectory);

            String _to = to.getAbsolutePath()
            GString zipTarget = "${_to} ${from.getName()} -x '*.svn*'"

//            GString gString = "${wdPath} [[ -e ${_to} ]] && zip -dr9 ${zipTarget} || zip -r9 ${zipTarget}";
            GString gString = "[[ -f \"${_to}\" ]] && rm ${_to}; ${wdPath} zip -r9 ${zipTarget} > /dev/null && md5sum ${_to}";
            return gString;
        }

        @Override
        GString getDecompressionString(File from, File to, File workingDirectory = null) {
            String wdPath = getWorkingDirectory(to, workingDirectory);
            GString gString = "${wdPath} unzip -o ${from} > /dev/null";
            return gString;
        }
    }

    private static Logger logger = Logger.getLogger(RoddyIOHelperMethods.class.name);

    private static Compressor compressor = new NativeLinuxZipCompressor();

    public static Compressor getCompressor() { return compressor; }

    public static String[] loadTextFile(File f) {
        try {
            return f.readLines().toArray(new String[0]);
        } catch (Exception ex) {
            return new String[0];
        }
    }

    public static String loadTextFileEnblock(File f) {
        try {
            return f.text;
        } catch (Exception ex) {
            return StringConstants.EMPTY;
        }
    }

    public static void writeTextFile(String path, String text) {
        File f = new File(path);
        writeTextFile(f, text);
    }

    public static void writeTextFile(File file, String text) {
        try {
            file.write(text);
        } catch (Exception ex) {
            logger.severe(ex.toString());
        }
    }

    public static void compressFile(File file, File targetFile, File workingDirectory = null) {
        compressor.compressFile(file, targetFile, workingDirectory)
    }

    public static void compressDirectory(File file, File targetFile, File workingDirectory = null) {
        compressor.compressDirectory(file, targetFile, workingDirectory)
    }

    public static String getStackTraceAsString(Exception exception) {
        try {
            StackTraceElement[] stackTrace = exception.getStackTrace();
            if (stackTrace != null)
                return joinArray(stackTrace, Constants.ENV_LINESEPARATOR);
        } catch (Exception ex) {
            logger.info("No stacktrace could be printed for an exception.")
            return "";
        }
    }

    public static String joinArray(Object[] array, String separator) {
        return array.collect { it -> it.toString() }.join(separator);
    }

    public static String joinArray(String[] array, String separator) {
        return array.join(separator);
    }

    public static String joinTextFileContent(String[] array) {
        return joinArray(array, System.lineSeparator());
    }

    public static String getMD5OfText(String text) {
        return DigestUtils.md5Hex(text);
    }

    public static String getMD5OfFile(File f) {
        try {
            DigestUtils.md5Hex(new FileInputStream(f));
        } catch (Exception ex) {
            logger.warning("Could not md5 file ${f.absolutePath} " + ex.toString());
            return "";
        }
    }

    /**
     * The method finds all files in a directory, creates an md5sum of each file and md5'es the result text.
     * This is i.e. useful when the folder has to be archived and the archives content should be comparable.
     * @param file
     * @return
     */
    static String getSingleMD5OfFilesInDirectory(File file) {
        List<File> list = []
        List<String> md5s = []
        file.eachFileRecurse (FileType.FILES) { File aFile -> list << aFile }
        list.sort()
        list.each { File line -> md5s << getMD5OfFile(line) }
        return getMD5OfText(md5s.join(Constants.ENV_LINESEPARATOR));
    }

    public static synchronized void appendLineToFile(File file, String line) {
        file.append(line + Constants.ENV_LINESEPARATOR);
    }

    public static File assembleLocalPath(String rootPath, String... structure) {
        return assembleLocalPath(new File(rootPath), structure);
    }

    public static File assembleLocalPath(File rootPath, String... structure) {
        if(!structure)
            return rootPath;
        File result = new File(rootPath, structure[0]);
        for (int i = 1; i < structure.length; i++) {
            result = new File(result, structure[i]);
        }
        return result;
    }
}
