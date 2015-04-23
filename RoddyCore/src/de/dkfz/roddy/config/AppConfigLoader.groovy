package de.dkfz.roddy.config

import de.dkfz.roddy.StringConstants;

/**
 * Java basically provides an ini file loading class. When using this class, there were several problems:
 * - The entries order in ini files is not permanent. Upon load and write, the order changes.
 * - Comments cannot be stored in those ini files
 */
@groovy.transform.CompileStatic
public class AppConfigLoader {

    /**
     * A copy of the entries in the ini file.
     */
    private final Map<String, String> entries = [:];

    public AppConfigLoader(String file) {
        this(new File(file));
    }

    public AppConfigLoader(File file) {
        file.eachLine {
            String line ->
                // Split away comments and trim the line
                String[] splitline = line.split(StringConstants.SPLIT_HASH)[0].trim().split(StringConstants.SPLIT_EQUALS, 2)
                String key = splitline[0];
                String value = splitline.size() > 1 ? splitline[1];
        };
    }
}
