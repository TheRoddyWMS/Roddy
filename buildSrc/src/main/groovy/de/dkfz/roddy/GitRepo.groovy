package de.dkfz.roddy

import org.gradle.api.file.FileCollection


class GitRepo {

    final public File repoDir

    GitRepo(File repoDir) {
        this.repoDir = repoDir
    }

    @Override
    String toString () {
        return String.format("<GitRepo url='%s'>", repoDir)
    }

    String gitCommand (String gitCommand) {
        return String.format("git --no-pager -C %s/ %s", repoDir, gitCommand)
    }

    private static LinkedList<String> execute (String command) {
        Process proc = command.execute()
        StringBuilder errStrm = new StringBuilder()
        StringBuilder outStrm = new StringBuilder()
        proc.consumeProcessOutput(outStrm, errStrm)
        if (proc.waitFor() != 0) {
            throw new RuntimeException("Error executing '${command}':\nout='${outStrm}',\nerr='${errStrm}'")
        } else {
            return outStrm.toString().split("\n")
        }
    }

    static GitRepo initialize(File targetDir) {
        execute("git init ${targetDir}")
        return new GitRepo(targetDir)
    }

    List<String> add(Collection<File> files) {
        execute(gitCommand("add ${files.asList().join(" ")}"))
    }

    List<String> commit(String message) {
        execute(gitCommand("commit -m '${message}'"))
    }

    String activeBranch () {
        LinkedList<String> matches = execute(gitCommand("branch")).findAll({ it.matches(~/^\\*\\s/) })
        if (matches.size() != 1) {
            throw new RuntimeException("Expected unique match of branch: ${matches}")
        }
        return matches[0].replaceAll(~/^\\*\\s/, "")
    }

    String currentCommit (boolean allowDirty=false, boolean shortHash=false) {
        assert allowDirty || !isDirty()
        LinkedList<String> result
        if (shortHash) {
             result = execute(gitCommand("log -1 --pretty=%h%n")).findAll( { it.matches(~ /\S+/)})
        } else {
              result = execute(gitCommand("log -1 --pretty=%H%n")).findAll( { it.matches(~ /\S+/)})
        }
        if (result.size() != 1) {
            throw new RuntimeException("Couldn't get current commit for ${this}.")
        }
        return(result[0]);
    }

    String currentCommitDate (boolean allowDirty=false) {
        assert allowDirty || !isDirty()
        return execute(gitCommand("log -1")).findAll {
            (it =~/^Date:\s+/) as Boolean
        }.collect {
            it.replaceAll(~/^Date:\s+/, "")
        }.last()
    }

    LinkedList<String> modifiedObjects () {
        return execute(gitCommand("status --short")).findAll {
            (it =~ /^\s*[DM]\s+/) as Boolean
        }.collect {
            new File(repoDir, it.replaceAll(~/^\s*[DAMCUR]\s+/, "")).toString()
        }
    }

    LinkedList<String> stagedObjects () {
        return execute(gitCommand("status --short")).findAll {
            (it =~ /^\s*A\s+/) as Boolean
        }.collect {
            new File(repoDir, it.replaceAll(~/^\s*A\s+/, "")).toString()
        }
    }

    boolean isDirty () {
        return modifiedObjects().size() != 0 || stagedObjects().size() != 0
    }

    void tag (String tagName, String tagMessage, boolean allowDirty=false) {
        assert allowDirty || !isDirty()
        execute(gitCommand("tag -m '${tagMessage}' '${tagName}'"))
    }

    void commit (Collection<File> files, String message) {
        execute(gitCommand("commit -m '${message}' -- ${files.each { "'${it.absolutePath}'"}.join(" ")}"))
    }

}
