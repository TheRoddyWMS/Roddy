package de.dkfz.roddy

import spock.lang.*


class VersionSpec extends Specification {

    def "increasing the major number" () {
        when:
        def version = new Version (1, 2, 3, 4)
        version.increaseMajor()
        then:
        version.major == 2
        version.minor == 0
        version.patch == 0
        version.revision == 0
    }

    def "increasing the minor number" () {
        when:
        def version = new Version (4, 3, 2, 1)
        version.increaseMinor()
        then:
        version.major == 4
        version.minor == 4
        version.patch == 0
        version.revision == 0
    }

    def "increasing the patch" () {
        when:
        def version = new Version (3, 1, 4, 9)
        version.increasePatch()
        then:
        version.major == 3
        version.minor == 1
        version.patch == 5
        version.revision == 0
    }

    def "increasing the revision" () {
        when:
        def version = new Version (1, 2, 3, 4)
        version.increaseRevision()
        then:
        version.major == 1
        version.minor == 2
        version.patch == 3
        version.revision == 5
    }

    def "getAt and destructuring binding" () {
        when:
        def version = new Version (12, 11, 10, 9)
        then:
        version[0] == 12
        version[1] == 11
        version[2] == 10
        version[3] == 9
        def (major, minor, patch, revision) = version
        major == 12
        minor == 11
        patch == 10
        revision == 9
    }

}