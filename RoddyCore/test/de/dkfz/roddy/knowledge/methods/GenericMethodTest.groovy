/*
 * Copyright (c) 2016 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.knowledge.methods;

import de.dkfz.roddy.knowledge.files.BaseFile;
import de.dkfz.roddy.knowledge.files.FileObject;
import de.dkfz.roddy.knowledge.files.BaseFile.ConstructionHelperForBaseFiles;
import org.junit.Test

import java.lang.reflect.Constructor

/**
 * Created by heinold on 19.01.16.
 */
public class GenericMethodTest {

    public static class FileTestClass extends BaseFile {
        public FileTestClass(ConstructionHelperForBaseFiles helper) {
            super(helper);
        }
    }

    @Test
    public void testSearchConstructor() {
        Constructor constructor = GenericMethod.searchBaseFileConstructorForConstructionHelperObject(FileTestClass);
        assert constructor != null;
    }

}