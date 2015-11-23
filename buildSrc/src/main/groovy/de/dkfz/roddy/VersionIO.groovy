package de.dkfz.roddy

class VersionIO {

    public static Version fromBuildVersion(List<String> buildVersion) {
        if (buildVersion.size() == 2) {
            return new Version(
                    buildVersion[0].split("\\.")[0].toInteger(),
                    buildVersion[0].split("\\.")[1].toInteger(),
                    buildVersion[1].toInteger(),
            )
        } else {
             throw new RuntimeException("Could not parse build information from '${buildVersion}'")
        }
    }

    public static String toBuildVersion(Version version) {
        List<String> buildVersion = ["${version.major}.${version.minor}",
                                  version.patch]
        return buildVersion.join("\n")
    }

    public static Version readBuildVersion(File buildVersionFile) {
        if (!buildVersionFile.canRead()) {
            throw new RuntimeException("Cannot read '${buildVersionFile}'")
        } else {
            Version version = fromBuildVersion(buildVersionFile.readLines())
            version.buildVersionFile = buildVersionFile
            return version
        }
    }

    public static boolean writeBuildVersion(Version version) {
        if (version.buildVersionFile == null) {
            throw new RuntimeException("Version object (${version}) has empty buildVersionFile property")
        }
        if (!version.buildVersionFile.canWrite()) {
            throw new RuntimeException("Cannot write '${version.buildVersionFile}'")
        }
        version.buildVersionFile.write(String.join(System.getProperty("line.separator"), toBuildVersion(version)))
    }

}