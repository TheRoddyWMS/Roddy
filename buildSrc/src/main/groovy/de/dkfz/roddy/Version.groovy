package de.dkfz.roddy

import java.util.regex.Pattern
import java.util.regex.Matcher

class Version {

    public Integer major
    public Integer minor
    public Integer patch
    public Integer revision

    public File buildVersionFile = null

    public Version (Integer major, Integer minor, Integer patch, Integer revision = 0) {
        this.major = major
        this.minor = minor
        this.patch = patch
        this.revision = revision
    }

    public Version increaseMajor() {
        ++major
        minor = 0
        patch = 0
        revision = 0
        return this
    }

    public Version increaseMinor() {
        ++minor
        patch = 0
        revision = 0
        return this
    }

    public Version increasePatch() {
        ++patch
        revision = 0
        return this
    }

    public Version increaseRevision() {
        ++revision
        return this
    }

    public String toString() {
        if (revision == 0) {
            return "${major}.${minor}.${patch}"
        } else {
            return "${major}.${minor}.${patch}-${revision}"
        }
    }

    private static final versionPattern = Pattern.compile(/^(\d+)\.(\d+)\.(\d+)(-(\d+))?$/)

    public static Version fromString (String versionString) {
        Matcher matcher = versionPattern.matcher(versionString)
        if (matcher.find()) {
            return new Version (
                    matcher.group(1),
                    matcher.group(2),
                    matcher.group(3),
                    matcher.group(5),
            )
        } else {
            return null
        }
    }

    public getAt (int idx) {
        if (idx == 0) major
        else if (idx == 1) minor
        else if (idx == 2) patch
        else if (idx == 3) revision
        else throw new IndexOutOfBoundsException("1.2.3-4")
    }

    public equals (Version other) {
        return this.major == other.major && this.minor == other.minor && this.patch == other.patch
    }

}

