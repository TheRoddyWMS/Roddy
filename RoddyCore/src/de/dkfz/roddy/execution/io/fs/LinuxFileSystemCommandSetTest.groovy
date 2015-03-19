package de.dkfz.roddy.execution.io.fs;

import org.junit.Test;

import java.io.File;

/**
 */
@groovy.transform.CompileStatic
public class LinuxFileSystemCommandSetTest {
    @Test
    public void testGetReadabilityTestCommand() throws Exception {
        assert new LinuxFileSystemCommandSet().getReadabilityTestCommand(new File("/tmp/test.file")).equals("[[ -e /tmp/test.file && -r /tmp/test.file ]] && echo TRUE");
    }


}
