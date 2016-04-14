package de.dkfz.roddy.execution.io;

import de.dkfz.roddy.plugins.LibrariesFactory
import groovy.transform.CompileStatic
import org.junit.Test;

/**
 * Created by heinold on 13.04.16.
 */
@CompileStatic
public class BaseMetadataTableTest {

    class MetadataTableTest extends BaseMetadataTable<MetadataTableTest> {
        MetadataTableTest(Map<String, Integer> headerMap, List<Map<String, String>> records) {
            super(headerMap, records)
        }
    }

    public File resourceDir = new File("test/resources");
    public String correctTable = "InputTableTest_CorrectTable1.tsv"
    public String damagedTable = "InputTableTest_DamagedTable1.tsv"

    private BaseMetadataTable readTable(String table) {
        String testFileName = LibrariesFactory.groovyClassLoader.getResource(table).file
        BaseMetadataTable inputTable = BaseMetadataTable.readTSVTable(testFileName)
        return inputTable;
    }

    @Test
    public void testReadTable_correctTable() throws Exception {
        BaseMetadataTable table = readTable(correctTable)
        assert table != null
        assert table.getHeaderMap().size() == 2
        assert table.size() == 8
    }

    @Test(expected = RuntimeException)
    public void testReadTable_damagedTable() {
        BaseMetadataTable table = readTable(damagedTable)
        assert table.assertValidTable()
    }

    @Test
    public void testExtendingClass() {
        def table = MetadataTableTest.readTSVTable(LibrariesFactory.groovyClassLoader.getResource(correctTable).file)
        assert table instanceof MetadataTableTest;
    }

    @Test
    public void testAssertValidTable() throws Exception {
        BaseMetadataTable table = readTable(correctTable)
        table.assertValidTable();
    }

    @Test
    public void testGetHeader() throws Exception {
        BaseMetadataTable table = readTable(correctTable)

        def keys = table.getHeaderMap().keySet()
        assert keys.size() == 2
        assert keys.containsAll(["PID", "File"])
    }

    @Test
    public void testSubsetByColumn() throws Exception {
        BaseMetadataTable table = readTable(correctTable)
        def column = table.subsetByColumn("PID", "b")
        assert column.size() == 2
        assert table.subsetByColumn("PID", "a").size() == 4
    }

}