/*
 * Copyright (c) 2016 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

/**
 * This is a very generic helper script which just converts a bash cfg to xml.
 * Not everything is covered, it's not built in yet and it needs a lot of improvement.
 * But for now, it works.
 * To run it faster from within IDEA, copy the content to a groovy console and start it.
 */
def lines = []
File f = new File("/data/michael/Projekte/RoddyWorkflows/Plugins/rareGAPWorkflow/pedbrain_PA_platypusConfigFile.JointCall.UPDATE.sh")
String previousLine = ""
f.readLines().each {
    String[] s = it.split("[=]")
    if (!it) return;
    if (!s) return;

    if (isPipe(it)) {

    } else if (isComment(it)) {
        if (isComment(previousLine)) {
            lines << ("""     ${it}""")
        } else {
            lines << ("""<!-- ${it}""")
        }
    } else {

        if (isComment(previousLine)) {
            lines[-1] += ("""-->""")
        }

        if (s.size() == 2)
            lines << ("""<cvalue name='${s[0]}' value='${s[1]}' type='string' />""")
        else if (s.size() == 1)
            lines << ("""<cvalue name='${s[0]}' value='' type='string' />""")
        else
            lines << ("""<cvalue name='${s[0]}' value='${s[1..-1].join("=")}' type='string' />""")
    }
    previousLine = it;
}

println lines.join("\n")

private boolean isComment(it) {
    it.trim().startsWith("#")
}

private boolean isPipe(it) {
    it.trim().startsWith("###")
}