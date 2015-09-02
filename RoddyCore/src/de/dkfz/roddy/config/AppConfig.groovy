package de.dkfz.roddy.config

import de.dkfz.roddy.StringConstants


/**
 * Java basically provides an ini file loading class. When using this class, there were several problems:
 * - The entries order in ini files is not permanent. Upon load and write, the order changes.
 * - Comments cannot be stored in those ini files
 */
@groovy.transform.CompileStatic
public class AppConfig {

    private class Entry {

        private String line;

        private int lineNo;

        private String key;

        private List<String> values;

        private String value;

        private String comment;

        public Entry(int lineNumber, String line) {
            lineNo = lineNumber;
            this.line = line;

            if(!isContent())
                return;

            setLine(line);
        }

        public boolean isEmpty() {
            return line.trim().size() == 0 || isComment();
        }

        public boolean isHeader() {
            return line.trim().startsWith(StringConstants.SBRACKET_LEFT) && line.trim().endsWith(StringConstants.SBRACKET_RIGHT);
        }

        public boolean isComment() {
            return line.trim().startsWith(StringConstants.HASH);
        }

        public boolean isContent() {
            return !(isEmpty() || isHeader() || isComment());
        }

        public String getType() {
            return isContent() ? "CONTENT" : isHeader() ? "HEADER" : isComment() ? "COMMENT" : isEmpty() ? "EMPTY" : "UNKNOWN";
        }

        void setLine(String s) {
            this.line = s;
            // Split away comments and trim the line
            // TODO Append comments again, if there was one stored.
            def splitLineByHash = line.split(StringConstants.SPLIT_HASH)
            String[] splitline = splitLineByHash[0].trim().split(StringConstants.SPLIT_EQUALS, 2)
            if(splitLineByHash.size() > 1)
                comment = splitLineByHash[1];
            key = splitline[0];

            if(splitline.size() > 1) {
                values = splitline[1].split("[,:]") as List;
                value = splitline[1];
            } else {
                values = [ "" ]
                value = ""
            }

        }
    }

    private File appIniFile;

    /**
     * A copy of the entries in the ini file.
     */
    private final Map<String, Entry> entriesByKey = [:];

    private List<Entry> allEntries = [];

    public AppConfig(String file) {
        this(new File(file));
    }

    public AppConfig(File file) {
        this.appIniFile = file;
        String[] lines = file.readLines()

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            Entry e = new Entry(i, line);
            allEntries << e;
            if(e.isContent())
                entriesByKey[e.key] = e;
        }
    }

    public AppConfig() {
        appIniFile = null;
    }

    public String toString() {
        println(appIniFile);

        for (def entry in allEntries) {
            println entry.getType() + "\t" + entry.line;
        }
    }

    public boolean containsKey(String key) {
        return entriesByKey.containsKey(key);
    }

    public void setProperty(String key, String value) {
        if(!containsKey(key))
            entriesByKey[key] = new Entry(-1, key + "=" + value);
        else
            entriesByKey[key].setLine(key + "=" + value);
    }

    public String getProperty(String key, String defaultvalue) {
        if(!containsKey(key))
            return defaultvalue;
        return entriesByKey[key].value
    }
}
