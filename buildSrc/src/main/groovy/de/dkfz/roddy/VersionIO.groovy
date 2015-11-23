package de.dkfz.roddy

class VersionIO {

    public static Version fromBuildInfo (List<String> buildInfo) {
        if (buildInfo.size() == 2) {
            return new Version(
                    buildInfo[0].split("\\.")[0].toInteger(),
                    buildInfo[0].split("\\.")[1].toInteger(),
                    buildInfo[1].toInteger(),
            )
        } else {
             throw new RuntimeException("Could not parse build information from '${buildInfo}'")
        }
    }

    public static String toBuildVersion(Version version) {
        List<String> buildVersion = ["${version.major}.${version.minor}",
                                  version.patch]
        return buildVersion.join("\n")
    }

    public static Version readBuildVersion(File buildInfoFile) {
        if (!buildInfoFile.canRead()) {
            throw new RuntimeException("Cannot read '${buildInfoFile}'")
        } else {
            Version version = fromBuildInfo(buildInfoFile.readLines())
            version.buildVersionFile = buildInfoFile
            return version
        }
    }

    public static boolean writeBuildVersion(Version version) {
        if (version.buildInfoFile == null) {
            throw new RuntimeException("Version object (${version}) has empty buildInfoFile property")
        }
        if (!version.buildInfoFile.canWrite()) {
            throw new RuntimeException("Cannot write '${version.buildInfoFile}'")
        }
        version.buildVersionFile.write(String.join(System.getProperty("line.separator"), toBuildVersion(version)))
    }

}