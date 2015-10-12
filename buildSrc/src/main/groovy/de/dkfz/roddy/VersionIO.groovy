import Version


class VersionIO {

    static String toBuildInfo (Version version) {
        return "${version.major}.${version.minor}" + System.getProperty("line.separator") + version.build + System.getProperty("line.separator")
    }

    static Version fromBuildInfo (List<String> buildInfo) {
        assert buildInfo.size() == 2
        return new Version (buildInfo[0].split("\\.")[0].toInteger(),
                buildInfo[0].split("\\.")[1].toInteger(),
                buildInfo[1].toInteger())
    }

    static Version readVersion(File buildInfoFile) {
        if (!buildInfoFile.canRead()) {
            throw new RuntimeException("Cannot read '${buildInfoFile}'")
        } else {
            return fromBuildInfo(buildInfoFile.readLines())
        }
    }

    static Version readCoreVersion(File projectRoot) {
        return readVersion(new File(projectRoot, "RoddyCore/rbuildversions.txt"))
    }

    static Version readBasePluginVersion(File projectRoot) {
        return readVersion(new File(projectRoot, "dist/plugins/PluginBase/buildversion.txt"))
    }

    static Version readDefaultPluginVersion(File projectRoot) {
        return readVersion(new File(projectRoot, "dist/plugins/DefaultPlugin/buildversion.txt"))
    }

    /*
     * Gets the version name from the latest Git tag
     * http://ryanharter.com/blog/2013/07/30/automatic-versioning-with-git-and-gradle/
     */
    static String getVersionName () {
        def stdout = new ByteArrayOutputStream()
        exec {
            commandLine 'git', 'describe', '--tags'
            standardOutput = stdout
        }
        return stdout.toString().trim()
    }

}