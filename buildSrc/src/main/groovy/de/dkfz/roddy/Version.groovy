class Version {

    public Integer major
    public Integer minor
    public Integer build

    public Version (major, minor, build) {
        this.major = major
        this.minor = minor
        this.build = build
    }

    public Version increaseMajor() {
        ++major
        minor = 0
        build = 0
        return this
    }

    public Version increaseMinor() {
        ++minor
        build = 0
        return this
    }

    public Version increaseBuild() {
        ++build
        return this
    }

    public String toString() {
        return "${major}.${minor}.${build}"

    }

    public static Version fromString (String versionString) {
        versionString.split("\\.").each { it.toInteger() }.with {
            major = getAt(0)
            minor = getAt(1)
            build = getAt(2)
        }
    }

    public getAt (int idx) {
        if (idx == 0) major
        else if (idx == 1) minor
        else if (idx == 2) build
        else throw new IndexOutOfBoundsException("major.minor.build")
    }
}

