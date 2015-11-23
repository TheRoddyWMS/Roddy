package de.dkfz.roddy

import spock.lang.*
import org.gradle.api.file.*

class GitRepoSpec extends Specification {

    File tmpDir
    File tmpFile

    def setup () {
        tmpDir = File.createTempDir("/tmp/testRepoDir-", "")
        tmpFile = File.createTempFile("testFile", "", tmpDir)
        tmpDir.deleteOnExit()
    }

    def "check initializing new repo" () {
        when:
        GitRepo repo = GitRepo.initialize(tmpDir)
        then:
        repo.repoDir.exists() && new File (repo.repoDir, ".git").exists()
    }

    def "adding a file" () {
        when:
        GitRepo repo = GitRepo.initialize(tmpDir)
        then:
        repo.add([tmpFile])
    }

    def "committing a file" () {
        when:
        GitRepo repo = GitRepo.initialize(tmpDir)
        repo.add([tmpFile])
        then:
        repo.commit("testfile")
    }

    def "checking current commit" () {
        when:
        GitRepo repo = GitRepo.initialize(tmpDir)
        repo.add([tmpFile])
        repo.commit("testfile")
        then:
        repo.currentCommit(true).matches(~ /^[0-9a-f]+$/)
    }

    def "listing modified files" () {
        when:
        GitRepo repo = GitRepo.initialize(tmpDir)
        repo.add([tmpFile])
        repo.commit("testfile")
        tmpFile.write("hallo")
        then:
        repo.modifiedObjects() == [tmpFile.toString()]
    }

    def "checking current commit date" () {
        when:
        GitRepo repo = GitRepo.initialize(tmpDir)
        repo.add([tmpFile])
        repo.commit("testfile")
        then:
        (repo.currentCommitDate(true) =~ /^\S{3}\s\S{3}\s\d{1,2}\s\d{2}:\d{2}:\d{2}\s\d{4}\s[+-]\d{4}$/) as Boolean
    }

    def "recognizing dirty repo" () {
        when:
        GitRepo repo = GitRepo.initialize(tmpDir)
        repo.add([tmpFile])
        then:
        repo.isDirty()
        when:
        repo.commit("testmessage")
        then:
        !repo.isDirty()
    }

}
