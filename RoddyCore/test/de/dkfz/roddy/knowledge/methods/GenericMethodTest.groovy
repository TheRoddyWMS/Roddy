package de.dkfz.roddy.knowledge.methods;

import de.dkfz.roddy.knowledge.files.BaseFile;
import de.dkfz.roddy.knowledge.files.FileObject;
import org.junit.Test

import java.lang.reflect.Constructor
import java.lang.reflect.Method;

import static org.junit.Assert.*;

/**
 * Created by heinold on 19.01.16.
 */
public class GenericMethodTest {

    public static class FileTestClass3 extends FileTestClass2 {
        public FileTestClass3(FileObject parent) {
            super(parent);
        }
    }

    public static class FileTestClass2 extends BaseFile {
        public FileTestClass2(FileObject parent) {
            super(parent);
        }
    }

    public static class FileTestClass extends BaseFile {
        public FileTestClass(FileTestClass2 parent) {
            super(parent);
        }

        public FileTestClass(FileTestClass2 parent, String tag) {
            super(parent);
        }

        public FileTestClass(String tag) {
            super(null);
        }
    }

    @Test
    public void testSearchConstructorWithSelectionTag() {
        Constructor constructor = GenericMethod.searchConstructorForOneOf(FileTestClass, "otherwise", FileTestClass2, BaseFile);
        assert constructor?.parameterTypes[0] == FileTestClass2;
    }

    @Test
    public void testSearchConstructorWithoutSelectionTag() {
        Constructor constructor = GenericMethod.searchConstructorForOneOf(FileTestClass, null, FileTestClass2, BaseFile);
        assert constructor && constructor.parameterTypes[0] == FileTestClass2;
    }

    @Test
    public void testSearchConstructorWithBaseClass() {
        Constructor constructor = GenericMethod.searchConstructorForOneOf(FileTestClass, null, FileTestClass3); // Should fall back to FileTestClass2!
        assert constructor && constructor.parameterTypes[0] == FileTestClass2
    }

}