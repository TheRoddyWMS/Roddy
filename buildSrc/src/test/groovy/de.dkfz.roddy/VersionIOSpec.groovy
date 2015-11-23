package de.dkfz.roddy

import spock.lang.*


class VersionIOSpec extends Specification {

    def "convert to buildinfo.txt" () {
        when:
        def version = new Version (1, 2, 3, 4)
        then:
        VersionIO.toBuildVersion(version) == "1.2\n3"
    }
}