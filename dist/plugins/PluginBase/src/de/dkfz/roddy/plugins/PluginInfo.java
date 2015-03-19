package de.dkfz.roddy.plugins;

import de.dkfz.roddy.StringConstants;
import de.dkfz.roddy.tools.RoddyIOHelperMethods;

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;


/**
 * An informational class for loaded plugins.
 */
public class PluginInfo {

    private String name;
    private File directory;
    private File developmentDirectory;
    private String prodVersion;
    private String devVersion;
    private final File zipFile;
    private Map<String, File> listOfToolDirectories = new LinkedHashMap<>();

    public PluginInfo(String name, File zipFile, File directory, File developmentDirectory, String prodVersion, String devVersion) {
        this.name = name;
        this.directory = directory;
        this.developmentDirectory = developmentDirectory;
        this.prodVersion = prodVersion;
        this.devVersion = devVersion;
        this.zipFile = zipFile;
        fillListOfToolDirectories();
    }

    private void fillListOfToolDirectories() {
        File toolsBaseDir = null;
        try {
            if (developmentDirectory != null && developmentDirectory.exists()) {
                toolsBaseDir = new File(new File(developmentDirectory, "resources"), "analysisTools");
            } else if (directory != null && directory.exists()) {
                toolsBaseDir = new File(new File(directory, "resources"), "analysisTools");
            }

            if (toolsBaseDir != null) { //Search through the default folders, if possible.
                for (File file : toolsBaseDir.listFiles()) {
                    String toolsDir = file.getName();
                    listOfToolDirectories.put(toolsDir, file);
                }
            } else { //Otherwise, finally look into the zip file. (Which must be existing at this point!)
                directory = RoddyIOHelperMethods.assembleLocalPath(zipFile.getParent(), zipFile.getName().split(".zip")[0]);
                toolsBaseDir = RoddyIOHelperMethods.assembleLocalPath(directory, "resources", "analysisTools");
                ZipFile zFile = new ZipFile(zipFile);
                try {
                    Enumeration<? extends ZipEntry> e = zFile.entries();
                    while (e.hasMoreElements()) {
                        String entry = e.nextElement().getName();
                        if (entry.endsWith("/") && !entry.endsWith("analysisTools/") && entry.contains("resources/analysisTools")) {
                            String name = entry.split("resources/analysisTools/")[1].split(StringConstants.SPLIT_SLASH)[0];
                            listOfToolDirectories.put(name, RoddyIOHelperMethods.assembleLocalPath(toolsBaseDir, name));
                        }
                    }
                } finally {
                    try {
                        if (zFile != null)
                            zFile.close();
                    } catch (IOException ioe) {
                        System.out.println("Error while closing zip file" + ioe);
                    }
                }
            }
        } catch (Exception ex) {
            System.out.println("The plugin " + name + " in directory " + directory + " could not be loaded properly. listFiles() in " + toolsBaseDir + " did not work.");
        }
    }

    public String getName() {
        return name;
    }

    public File getDirectory() {
        return directory;
    }

    public File getDevelopmentDirectory() {
        return developmentDirectory;
    }

    public String getProdVersion() {
        return prodVersion;
    }

    public String getDevVersion() {
        return devVersion;
    }

    public Map<String, File> getToolsDirectories() {
        return listOfToolDirectories;
    }

}