package de.dkfz.roddy.execution.io;

import com.sun.xml.internal.ws.api.addressing.WSEndpointReference;
import de.dkfz.roddy.Roddy;
import de.dkfz.roddy.StringConstants;
import de.dkfz.roddy.client.RoddyStartupOptions;
import de.dkfz.roddy.tools.RoddyConversionHelperMethods;

import java.io.File;

/**
 * A factory to construct Roddys metadata table instance.
 * The metadata table instance is per start, so it will be provided and accessible via the Roddy class.
 * <p>
 * Let's see, how this will work out with the GUI.
 * <p>
 * Created by heinold on 14.04.16.
 */
public final class MetadataTableFactory {

    private static BaseMetadataTable _cachedTable;

    private MetadataTableFactory() {
    }

    /**
     * This method constructs the Metadata table valid for the current Roddy execution!
     * It will lookup implementataion
     */
    public static BaseMetadataTable getTable() {
        if (Roddy.isMetadataCLOptionSet()) {

            // Create a metadata table from a file

            if (Roddy.isMetadataCLOptionSet()) {
                if (!_cachedTable) {
                    String[] split = Roddy.getCommandLineCall().getOptionValue(RoddyStartupOptions.usemetadatatable).split(StringConstants.SPLIT_COMMA);
                    String file = split[0];
                    String format = split.length == 2 && !RoddyConversionHelperMethods.isNullOrEmpty(split[1]) ? split[1] : null;
                    _cachedTable = BaseMetadataTable.readTable(new File(file), format);
                }
                return _cachedTable;
            }

/**         Leave it for later?
 // Search for Metadata implementations in any plugin
 // If too many were found, select via analysis xml file.
 // If none possible, select metadata table?
 **/


        } else {
            // Create the file based input table
        }
        return null;
    }
}
