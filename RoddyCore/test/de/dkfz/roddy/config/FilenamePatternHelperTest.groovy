/*
 * Copyright (c) 2017 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.config;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Created by heinold on 10.01.17.
 */
public class FilenamePatternHelperTest {

    @Test
    public void extractCommands() throws Exception {
        String commandID = '\${jobParameter';
        String temp = '/a/real/string/${jobParameter,name="test",default="abc"}/${jobParameter,name="test2"}/a_filename.txt'
        def result = FilenamePatternHelper.extractCommands(commandID, temp)
        assert result
        assert result.size() == 2
        assert result[0].attributes["name"].value == "test"
        assert result[0].attributes["default"].value == "abc"
        assert result[1].attributes["name"].value == "test2"
    }

    @Test
    public void testExtractCommandsWithUnclosedLiterals() {
        String commandID = '\${jobParameter';
        String temp = '/a/real/string/${jobParameter,name="test"}/${jobParameter,name="test2"/a_filename.txt'
        assert false
    }

}