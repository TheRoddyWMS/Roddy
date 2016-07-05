package de.dkfz.roddy.execution.io.fs;

import org.junit.Test

/**
 */
@groovy.transform.CompileStatic
public class BashCommandSetTest {
    @Test
    public void testGetReadabilityTestCommand() throws Exception {
        assert new BashCommandSet().getReadabilityTestCommand(new File("/tmp/test.file")).equals("[[ -e /tmp/test.file && -r /tmp/test.file ]] && echo TRUE || echo FALSE");
    }

    @Test
    public void testGetCheckForInteractiveConsoleCommand() {
        String separator = "\n"
        assert new BashCommandSet().getCheckForInteractiveConsoleCommand() == 'if [ -z "${PS1-}" ]; then' + separator + "\t echo non interactive process!" + separator + "else" + separator + "\t echo interactive process"
    }

    @Test
    public void testSingleQuote() {
        assert new BashCommandSet().singleQuote("Text") == "'Text'"
    }

    @Test
    public void testDoubleQuote() {
        assert new BashCommandSet().doubleQuote("Text") == '"Text"'
    }
}
