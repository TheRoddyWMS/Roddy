package de.dkfz.roddy.plugins;

import de.dkfz.roddy.StringConstants;
import de.dkfz.roddy.tools.RoddyConversionHelperMethods;
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

    public enum PluginInfoConnection {
        /**
         * Revisions mark plugins which contain smaller bugfixes but not extensions. In overall, the revision
         * does not introduce extensions or functional changes.
         */
        REVISION,

        /**
         * Extension plugins are plugins, which add functionality or bugfixes to an existing plugin.
         * They are considered as compatible to their precursors and plugins using the precursors. However
         * plugins referencing these plugins are not allowed to use older versions of the plugin.
         */
        EXTENSION,

        /**
         * Plugin versions which are marked incompatible to their precursor will not be considered for
         * auto version select by Roddy.
         */
        INCOMPATIBLE
    }

    private String name;
    private File directory;
    private File developmentDirectory;
    private String prodVersion;
    private Map<String, String> dependencies;
    private final File zipFile;

    /**
     * Stores the next entry in the plugin chain or null if there is nor further plugin available.
     */
    private PluginInfo nextInChain = null;
    /**
     * Stores the previous entry in the plugin chain or null if there is no precursor.
     */
    private PluginInfo previousInChain = null;
    /**
     * Stores the connection type of this PI object relative to its precursor.
     */
    private PluginInfoConnection previousInChainConnectionType = PluginInfoConnection.INCOMPATIBLE;

    private boolean isBetaPlugin = false;

    private Map<String, File> listOfToolDirectories = new LinkedHashMap<>();

    public PluginInfo(String name, File zipFile, File directory, File developmentDirectory, String prodVersion, Map<String, String> dependencies) {
        this.name = name;
        this.directory = directory;
        this.developmentDirectory = developmentDirectory;
        this.prodVersion = prodVersion;
        this.dependencies = dependencies;
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

            if (toolsBaseDir != null && toolsBaseDir.exists() && toolsBaseDir.isDirectory()) { //Search through the default folders, if possible.
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
        }
    }

    public File getBrawlWorkflowDirectory() {
        return new File(new File(directory, "resources"), "brawlworkflows");
    }

    public String getName() {
        return name;
    }

    public File getDirectory() {
        return directory;
    }

    public String getProdVersion() {
        return prodVersion;
    }

    public Map<String, File> getToolsDirectories() {
        return listOfToolDirectories;
    }

    public Map<String, String> getDependencies() {
        return dependencies;
    }

    public PluginInfo getNextInChain() {
        return nextInChain;
    }

    public PluginInfo getNextCompatibleInChain() {
        if (nextInChain != null && nextInChain.previousInChainConnectionType != PluginInfoConnection.INCOMPATIBLE) {
            return nextInChain;
        } else {
            return null;
        }
    }

    public PluginInfo getPreviousInChain() {
        return previousInChain;
    }

    public PluginInfoConnection getPreviousInChainConnectionType() {
        return previousInChainConnectionType;
    }

    public String getFullID() {
        return name + ":" + prodVersion;
    }

    public String getMajorAndMinor() {
        return prodVersion.split("[-]")[0];
    }

    public int getRevision() {
        int result = 0;
        try {
            String[] split = prodVersion.split("[-]");
            if (split.length == 1)
                return 0;
            else
                result = RoddyConversionHelperMethods.toInt(split[1], 0);
        } catch (Exception e) {
            System.out.println("Error for revision fetch of " + this.name + " in " + this.directory);
//            e.printStackTrace();
        }
        return result;
    }

    @Override
    public String toString() {
        return getFullID();
    }
}