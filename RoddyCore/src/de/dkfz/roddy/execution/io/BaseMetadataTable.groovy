package de.dkfz.roddy.execution.io

import de.dkfz.roddy.plugins.LibrariesFactory
import groovy.transform.CompileStatic
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser

/**
 * The basic input table class for data input in table format instead of files.
 * Initially the class was created by Philip Kensche and created for our workflows.
 * But it turned out to be useful in general. So to get the full power of the class,
 * create a custom class in your workflow extends this one and add all the stuff you
 * need.
 *
 * Created by heinold on 13.04.16.
 */
@CompileStatic
public class BaseMetadataTable<T extends BaseMetadataTable> {

    /**
     * Type of input table. Can be a file or a database (but this is not supported yet)
     */
    public static enum InputTableType {
        File,
        Database,
    }

    private Map<String, Integer> headerMap
    private List<Map<String, String>> records
    public static final String INPUT_TABLE_DATASET = "PID";
    public static final String INPUT_TABLE_FILE = "File";
    public static final String CVALUE_INPUT_TABLE = "inputTable";
    public static final String CVALUE_INPUT_TABLE_FORMAT = "inputTableFormat";


    BaseMetadataTable(Map<String, Integer> headerMap, List<Map<String, String>> records) {
        // The following code is much more complex than it should be. Theoretically, it should be possible to output everything
        // in one line.
        def collect = records.collect {
            Map<String, String> record ->
                def clone = [:]
                clone += record;
                clone as Map<String, String>
        };
        def list = collect as List<Map<String, String>>;

        this.headerMap = headerMap
        this.records = list
    }

    public static BaseMetadataTable readTSVTable(String file) {
        return readTable(new FileReader(new File(file)), "tsv")
    }

    public static BaseMetadataTable readTable(File file, String format) {
        Reader instream = new FileReader(file)
        BaseMetadataTable inputTable = readTable(instream, format)
        instream.close()
        return inputTable
    }

    public static BaseMetadataTable readTable(InputStream stream, String format) {
        return readTable(new InputStreamReader(stream), format)
    }

    public static BaseMetadataTable readTable(Reader reader, String format) {
        CSVFormat tableFormat = convertFormat(format)
        tableFormat = tableFormat.withCommentMarker('#' as char)
                .withIgnoreEmptyLines()
                .withHeader();
        CSVParser parser = tableFormat.parse(reader)
        def map = parser.headerMap as Map<String, Integer>
        def collect = parser.records.collect { it.toMap() }
        def inputTable = new BaseMetadataTable<T>(map, collect)
        return inputTable
    }

    private static CSVFormat convertFormat(String format) {
        if (format == null || format == "") format = "tsv";
        CSVFormat tableFormat
        switch (format.toLowerCase()) {
            case "tsv":
                tableFormat = CSVFormat.TDF
                break
            case "excel":
                tableFormat = CSVFormat.EXCEL
                break
            case "csv":
                tableFormat = CSVFormat.RFC4180
                break
            default:
                throw new IllegalArgumentException("Value '${format}' is not a valid for ${CVALUE_INPUT_TABLE_FORMAT}. Use 'tsv', 'csv' or 'excel' (case-insensitive)!")
        }
        tableFormat
    }

    public List<String> getMandatoryColumnNames() {
        def list = [INPUT_TABLE_DATASET,         // individual ID, cohort name, often pseudonym of patient
                    INPUT_TABLE_FILE];
        list += _getAdditionalMandatoryColumnNames()
        return list as List<String>;
    }

    /**
     * Can be overriden
     * @return
     */
    protected List<String> _getAdditionalMandatoryColumnNames() {
        []
    }

    public List<String> getOptionalColumnNames() {
        return [] as List<String>
    }

    public List<String> getRelevantColumnNames() {
        return mandatoryColumnNames + optionalColumnNames
    }

    private void assertValidRecord(Map<String, String> record) {
        if (!record.keySet().equals(headerMap.keySet())) {
            throw new RuntimeException("Record has columns inconsistent with header: ${record}")
        }
        mandatoryColumnNames.each {
            if (!record.containsKey(it) && record.get(it) != "") {
                throw new RuntimeException("Field '${it}' is not set for record: ${record}")
            }
        }
    }

    private void assertHeader() {
        mandatoryColumnNames.each {
            if (!headerMap.containsKey(it)) {
                throw new RuntimeException("Field '${it}' is missing")
            }
        }
    }

    public void assertValidTable() {
        assertHeader()
        records.each { assertValidRecord(it) }
    }

    protected void _assertCustom() {

    }

    public Map<String, Integer> getHeaderMap() {
        return headerMap as Map<String, Integer>
    }

    /**
     * @return The header names in the order defined by the headerMap
     */
    public List<String> getHeader() {
        return headerMap.entrySet().collect { it.key } as List<String>
    }

    public List<Map<String, String>> getTable() {
        return records.collect { it.clone() } as List<Map<String, String>>
    }

    public BaseMetadataTable subsetByColumn(String columnName, String value) {

        // Look into internal mapping table for headernames to varnames

        return new BaseMetadataTable(headerMap, records.findAll { Map<String, String> row ->
            row.get(columnName) == value
        })
    }

    public BaseMetadataTable subsetByDataset(String datasetId) {
        return subsetByColumn(INPUT_TABLE_DATASET, datasetId)
    }


    public Integer size() {
        return records.size()
    }

    public List<String> listColumn(String columnName) {
        return records.collect { Map<String, String> record ->
            record.get(columnName)
        }
    }

    public List<String> listDatasets() {
        return listColumn(INPUT_TABLE_DATASET).unique();
    }

    public List<File> listFiles() {
        return listColumn(INPUT_TABLE_FILE).unique().collect { new File(it) }
    }

    public List<Map<String, String>> getRecords() {
        return records;
    }
}
