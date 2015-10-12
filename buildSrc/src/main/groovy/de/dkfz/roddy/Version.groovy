package de.dkfz.roddy

class Version {

    public Integer major
    public Integer minor
    public Integer patch

    public Version (Integer major, Integer minor, Integer patch) {
        this.major = major
        this.minor = minor
        this.patch = patch
    }

    public Version increaseMajor() {
        ++major
        minor = 0
        patch = 0
        return this
    }

    public Version increaseMinor() {
        ++minor
        patch = 0
        return this
    }

    public Version increasePatch() {
        ++patch
        return this
    }

    public String toString() {
        return "${major}.${minor}.${patch}"

    }

    public static Version fromString (String versionString) {
        versionString.split("\\.").each { it.toInteger() }.with {
            major = getAt(0)
            minor = getAt(1)
            patch = getAt(2)
        }
    }

    public getAt (int idx) {
        if (idx == 0) major
        else if (idx == 1) minor
        else if (idx == 2) patch
        else throw new IndexOutOfBoundsException("major.minor.patch")
    }
}

