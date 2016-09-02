package de.dkfz.roddy.execution.io

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
public class BaseMetadataTable {

    /**
     * Type of input table. Can be a file or a database (but this is not supported yet)
     */
    public static enum InputTableType {
        File,
        Database,
    }

    // A map translating "external" table column ids to "internal" ones.
    // The mapping is via standard values from some xml files.
    protected Map<String, String> internal2CustomIDMap = [:]
    protected Map<String, String> custom2InternalIDMap = [:]

    // The values of mandatoryColumns need to be internal column names ("datasetCol", "fileCol", etc.).
    protected List<String> mandatoryColumns = [];

    // A map which links column id and column position.
    // The table uses internal column ids
    protected Map<String, Integer> headerMap = [:]
    protected List<Map<String, String>> records = []

    public static final String INPUT_TABLE_DATASET = "datasetCol";
    public static final String INPUT_TABLE_FILE = "fileCol";

    /**
     *  Copy constructor for subclasses
     */
    public BaseMetadataTable(BaseMetadataTable origin) {
        this.internal2CustomIDMap += origin.internal2CustomIDMap
        this.custom2InternalIDMap += origin.custom2InternalIDMap
        this.mandatoryColumns += origin.mandatoryColumns
        this.headerMap += origin.headerMap
        this.records += origin.records
    }

    /**
     * Copy construct copies with subsetByColumn. This won't work with the other constructors.
     */
    protected BaseMetadataTable(BaseMetadataTable origin, List<Map<String, String>> records) {
        this.internal2CustomIDMap += origin.internal2CustomIDMap
        this.custom2InternalIDMap += origin.custom2InternalIDMap
        this.mandatoryColumns += origin.mandatoryColumns
        this.headerMap += origin.headerMap
        this.records += records;
    }

    BaseMetadataTable(Map<String, Integer> headerMap, Map<String, String> internal2CustomIDMap, List<String> mandatoryColumns, List<Map<String, String>> records) {
        this.internal2CustomIDMap = internal2CustomIDMap as LinkedHashMap<String,String>;
        this.internal2CustomIDMap.each {
            String key, String val -> custom2InternalIDMap[val] = key;
        }
        this.mandatoryColumns = mandatoryColumns;
        def collect = records.collect {
            Map<String, String> record ->
                Map<String, String> clone = [:] as Map<String, String>
                for (String header in internal2CustomIDMap.keySet())
                    clone[header] = (String) null;
                for (String key in record.keySet()) {
                    String val = record[key];

                    def internalKey = custom2InternalIDMap[key]
                    if (internalKey == null)
                        throw new RuntimeException("The metadata table key '${key}' could not be mapped to an internal key!")

                    clone[internalKey] = val;
                }
                return clone;
        };
        def list = collect as List<Map<String, String>>;

        this.headerMap = headerMap
        this.records = list
    }

    public List<String> getMandatoryColumnNames() {
        return new LinkedList<String>(mandatoryColumns);
    }

    public List<String> getOptionalColumnNames() {
        return internal2CustomIDMap.keySet() - mandatoryColumns as List<String>;
    }

    private void assertValidRecord(Map<String, String> record) {
        if (!record.keySet().equals(internal2CustomIDMap.keySet())) {
            throw new RuntimeException("Record has columns inconsistent with header: ${record}")
        }
        if (record.size() != headerMap.size()) {
            throw new RuntimeException("Record has the wrong size: ${record}")
        }
        mandatoryColumnNames.each {
            if (!record.containsKey(it) && record.get(it) != "") {
                throw new RuntimeException("Field '${it}' is not set for record: ${record}")
            }
        }
    }

    private void assertHeader() {
        mandatoryColumnNames.each {
            if (!headerMap.containsKey(internal2CustomIDMap[it])) {
                throw new RuntimeException("Field '${it}' is missing")
            }
        }
    }

    public void assertValidTable() {
        assertHeader()
        records.each { assertValidRecord(it) }
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

    public Map<String, String> getColumnIDMappingMap() {
//        return internal2CustomIDMap.collectEntries { String key, String val -> ["${key}".toString(): val] } as Map<String, String>
        Map<String, String> collect = internal2CustomIDMap.collectEntries {
            String key, String val ->
                def clone = [:]
                clone[key] = val;
                clone as Map<String, String>
        } as Map<String, String>;
        return collect
    }


    public List<Map<String, String>> getTable() {
        return records.collect { it.clone() } as List<Map<String, String>>
    }

    public BaseMetadataTable unsafeSubsetByColumn(String columnName, String value) {

        // Look into internal mapping table for headernames to varnames
        return new BaseMetadataTable(
                this,
                records.findAll { Map<String, String> row ->
                    row.get(columnName) == value
                }
        )
    }

    /** Get a subset of rows by unique values in a specified column (internal column namespace).
     *  If mandatory column is selected, it is checked, whether the higher priority column, which
     *  are before the selected columns in the mandatory columns, are unique. This ensures that
     *  for instance not the same file is assigned to two different datasets.
     * @param columnName internal column name (e.g. "datasetCol")
     * @param value
     * @param check
     * @return
     */
    public BaseMetadataTable subsetByColumn(String columnName, String value, boolean check = true) {
        BaseMetadataTable subtable = unsafeSubsetByColumn(columnName, value)
        if (check && mandatoryColumnNames.contains(columnName)) {
            for(String colToCheck : mandatoryColumnNames) {
                if (colToCheck.equals(columnName)) {
                    break;
                } else {
                    if (subtable.listColumn(colToCheck).unique().size() != 1) {
                        throw new RuntimeException("Subsetting metadata table column '${columnName}' with value '${value}' results in non-unique higher-priority column values for '${colToCheck}")
                    }
                }
            }
        }
        return subtable;
    }

    public BaseMetadataTable subsetByDataset(String datasetId, boolean check = true) {
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
