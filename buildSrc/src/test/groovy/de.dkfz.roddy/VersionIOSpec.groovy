package de.dkfz.roddy

import spock.lang.*


class VersionIOSpec extends Specification {

    def "convert to buildversion.txt" () {
        when:
        def version = new Version (1, 2, 3, 4)
        then:
        VersionIO.toBuildVersion(version) == "1.2\n3"
    }

    def "from list of strings" () {
        when:
        def versionStrings = ["1.4", "5"] as List<String>
        then:
        VersionIO.fromBuildVersion(versionStrings) == new Version (1, 4, 5)
    }

}