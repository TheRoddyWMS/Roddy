/*
 * Copyright (c) 2016 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.execution.io.fs

import org.junit.Test

/**
 */
@groovy.transform.CompileStatic
class BashCommandSetTest {
    @Test
    void testGetReadabilityTestCommand() throws Exception {
        assert new BashCommandSet().getReadabilityTestCommand(new File("/tmp/test.file")).equals("[[ -e /tmp/test.file && -r /tmp/test.file ]] && echo TRUE || echo FALSE")
    }

    @Test
    void testGetCheckForInteractiveConsoleCommand() {
        String separator = "\n"
        assert new BashCommandSet().getCheckForInteractiveConsoleCommand() == 'if [[ -z "${PS1-}" ]]; then' + separator + "\t echo \"non interactive process!\" >> /dev/stderr" + separator + "else" + separator + "\t echo \"interactive process\" >> /dev/stderr"
    }

    @Test
    void testGetFindFilesUsingWildcardsCommand(File baseFolder, String wildcards) {
        assert new BashCommandSet().getFindFilesUsingWildcardsCommand(new File("/tmp"), '*.png') == 'ls "/tmp/*.png"'

    }

    @Test
    void getFindFilesUsingRegexCommand(File baseFolder, String regex) {
        def command = new BashCommandSet().getFindFilesUsingRegexCommand(new File("/tmp"), '"temp[/]vpn[0-9]\\.png"')
        assert command == 'find "/tmp" -regextype posix-extended -regex "temp[/]vpn[0-9]\\.png" | sort'
    }

    @Test
    void testSingleQuote() {
        assert new BashCommandSet().singleQuote("Text") == "'Text'"
    }

    @Test
    void testDoubleQuote() {
        assert new BashCommandSet().doubleQuote("Text") == '"Text"'
    }

    @Test
    void testGetShellExecuteCommand() {
        assert new BashCommandSet().getShellExecuteCommand("env") == ["bash", "-c", "env"]
        assert new BashCommandSet().getShellExecuteCommand("echo a;", "echo b;") == ["bash", "-c", "echo a;", "echo b;"]
    }

    @Test
    void testValidate() {
        // This method can only be tested if /bin/bash is really avaible. The test makes no sense in my oppinion

        def file = new File("/bin/bash")
        assert file.canRead() && file.canExecute() == new BashCommandSet().validate()
    }
}
