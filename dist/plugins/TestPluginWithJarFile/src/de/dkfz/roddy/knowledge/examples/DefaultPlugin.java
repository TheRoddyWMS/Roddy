import de.dkfz.roddy.plugins.BasePlugin;

/**
 * This is a template class for roddy plugins.
 * It shows you, how a plugin declaration should look like, especially if you want to incorporate version strings.
 */

public class DefaultPlugin extends BasePlugin {

    public static final String CURRENT_VERSION_STRING = "1.0.35";
    public static final String CURRENT_VERSION_BUILD_DATE = "Tue Feb 23 14:00:10 CET 2016";

    @Override
    public String getVersionInfo() {
        return "Roddy plugin: " + this.getClass().getName() + ", V " + CURRENT_VERSION_STRING + " built at " + CURRENT_VERSION_BUILD_DATE;
    }
}
