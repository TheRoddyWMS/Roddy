package de.dkfz.roddy

class GitRepo {

    final public File repoDir

    public boolean debug = false

    GitRepo(File repoDir) {
        this.repoDir = repoDir
    }

    @Override
    String toString () {
        return String.format("<GitRepo url='%s'>", repoDir)
     }

    String[] gitCommand (String... command) {
        return ["git", "--no-pager", "-C", "${repoDir}/"] + command.toList() as String[]
    }

    private LinkedList<String> execute (String... command) {
        Process proc = new ProcessBuilder(command).start()
        StringBuilder errStrm = new StringBuilder()
        StringBuilder outStrm = new StringBuilder()
        proc.consumeProcessOutput(outStrm, errStrm)
        if (proc.waitFor() != 0) {
            throw new RuntimeException("Error executing '${command}':\nout='${outStrm}',\nerr='${errStrm}'")
        } else {
            if (debug) {
                System.err.print(outStrm.toString())
                System.err.print(errStrm.toString())
            }
            return outStrm.toString().split("\n")
        }
    }

    GitRepo initialize() {
        execute("git", "init", repoDir.toString())
        return this
    }

    List<String> add(Collection<File> files) {
        execute(gitCommand(["add"] + files.each { it.toString() } as String[]))
    }

    List<String> commit(String message) {
        execute(gitCommand("commit", "-m '${message}'"))
    }

    String activeBranch () {
        LinkedList<String> matches = execute(gitCommand("branch")).findAll({ it.matches(~/^\\*\\s/) })
        if (matches.size() != 1) {
            throw new RuntimeException("Expected unique match of branch: ${matches}")
        }
        return matches[0].replaceAll(~/^\\*\\s/, "")
    }

    String lastCommitHash (boolean allowDirty=false, boolean shortHash=false) {
        assert allowDirty || !isDirty()
        LinkedList<String> result
        if (shortHash) {
            result = execute(gitCommand("log", "-1", "--pretty=%h%n")).findAll( { it.matches(~ /\S+/)})
        } else {
            result = execute(gitCommand("log", "-1", "--pretty=%H%n")).findAll( { it.matches(~ /\S+/)})
        }
        if (result.size() != 1) {
            throw new RuntimeException("Couldn't get last commit for ${this}.")
        }
        return(result[0]);
    }

    String lastCommitDate (boolean allowDirty=false) {
        assert allowDirty || !isDirty()
        return execute(gitCommand("log", "-1")).findAll {
            (it =~/^Date:\s+/) as Boolean
        }.collect {
            it.replaceAll(~/^Date:\s+/, "")
        }.last()
    }

    LinkedList<String> modifiedObjects () {
        return execute(gitCommand("status", "--short")).findAll {
            (it =~ /^\s*[DM]\s+/) as Boolean
        }.collect {
            new File(repoDir, it.replaceAll(~/^\s*[DAMCUR]\s+/, "")).toString()
        }
    }

    LinkedList<String> stagedObjects () {
        return execute(gitCommand("status", "--short")).findAll {
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
        execute(gitCommand("tag", "-a", "-m '${tagMessage}'", tagName))
    }

    void commit (Collection<File> files, String message) {
        execute(gitCommand(["commit", "-m '${message}'", "--"] + files.each { "'${it.absolutePath}'"} as String[]))
    }

}
