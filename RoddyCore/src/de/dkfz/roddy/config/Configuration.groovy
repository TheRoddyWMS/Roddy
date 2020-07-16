/*
 * Copyright (c) 2016 German Cancer Research Center (Deutsches Krebsforschungszentrum, DKFZ).
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/TheRoddyWMS/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.config

import static de.dkfz.roddy.StringConstants.SPLIT_COMMA

import de.dkfz.roddy.config.loader.ConfigurationFactory
import de.dkfz.roddy.config.loader.ConfigurationLoadError
import de.dkfz.roddy.config.validation.ConfigurationValidationError
import de.dkfz.roddy.core.ExecutionContext
import de.dkfz.roddy.core.RuntimeService
import de.dkfz.roddy.plugins.LibrariesFactory
import de.dkfz.roddy.plugins.PluginInfo
import de.dkfz.roddy.tools.LoggerWrapper
import de.dkfz.roddy.tools.RoddyIOHelperMethods
import groovy.transform.CompileStatic
import org.apache.commons.io.filefilter.WildcardFileFilter

/**
 * A configuration stores maps of different types:
 * - configuration values / value bundles
 * - basepaths
 * - tool entries
 * - enumerations
 * - filename patterns
 * <p/>
 * A configuration can import from other configurations (only first configurationType of file)
 * A configuration can inherit from other configurations
 *
 * @author michael
 */
@CompileStatic
class Configuration implements ContainerParent<Configuration>, ConfigurationIssue.IConfigurationIssueContainer {

    /**
     * Several levels of configurations.
     * Do not change the order of this! It is queried and compared several times.
     */
    enum ConfigurationType {
        /**
         * Unknown / Unset
         */
        UNSET,
        /**
         * Other configurations (i.e. definition of filenames, tools)
         */
        OTHER,
        /**
         * For the definition of workflows.
         */
        ANALYSIS,
        /**
         * For the definition of projects
         */
        PROJECT
    }

    private static final LoggerWrapper logger = LoggerWrapper.getLogger(Configuration.simpleName)
    /**
     * The prototype with basic information about this configuration
     */
    protected final PreloadedConfiguration preloadedConfiguration

    /**
     * A list of parent configuration objects. Order matters! Configurations are stored with
     * increasing priority, so parents[0] has the lowest and parents[n -1] has the highest priority
     */
    private final List<Configuration> parents = []

    private final Map<String, Configuration> subConfigurations = [:]

    private final List<ConfigurationValidationError> listOfValidationErrors = []

    private final List<ConfigurationLoadError> listOfLoadErrors = []

    private final List<ConfigurationIssue> warnings = []

    private final List<ConfigurationIssue> errors = []

    private final RecursiveOverridableMapContainerForConfigurationValues configurationValues =
            new RecursiveOverridableMapContainerForConfigurationValues(this, "configurationValues")

    /**
     * Bundles store values with the same name for the same configuration.
     * This can sometimes be necessary. So you do not need a sub configuration for each different set of
     * values.
     */
    private final RecursiveOverridableMapContainer<String, ConfigurationValueBundle, Configuration> configurationValueBundles =
            new RecursiveOverridableMapContainer<>(this, "configurationValueBundles")

    private final RecursiveOverridableMapContainer<String, ToolEntry, Configuration> tools =
            new RecursiveOverridableMapContainer<>(this, "tools")

    private final RecursiveOverridableMapContainer<String, Enumeration, Configuration> enumerations =
            new RecursiveOverridableMapContainer<>(this, "enumerations")

    private RecursiveOverridableMapContainer<String, FilenamePattern, Configuration> filenamePatterns =
            new RecursiveOverridableMapContainer<>(this, "filenamePatterns")

    Configuration() {
        preloadedConfiguration = null
    }

    /**
     * Creates a new configuration that can be filled by filling the containers.
     */
    Configuration(PreloadedConfiguration icc) {
        this.preloadedConfiguration = icc
    }

    /**
     * For main configurations
     * Read reversely
     * Remember to set the parent config afterwards.
     * With this configuration no dependency tree is created!
     */
    Configuration(PreloadedConfiguration preloadedConfiguration, Configuration parentConfig) {
        this.preloadedConfiguration = preloadedConfiguration
        this.addParent(parentConfig)
    }

    /**
     * @param preloadedConfiguration
     * @param parentConfigurations A list of parent configuration objects.
     *                               Order matters! Configurations are stored with
     *                               increasing priority, so pcs[0] has the lowest
     *                               and pcs[n -1] has the highest priority
     */
    Configuration(PreloadedConfiguration preloadedConfiguration, List<Configuration> parentConfigurations) {
        this.preloadedConfiguration = preloadedConfiguration
        for (Configuration parentConfiguration : parentConfigurations) {
            addParent(parentConfiguration)
        }
    }

    /**
     * For main configurations
     */
    Configuration(PreloadedConfiguration preloadedConfiguration, Map<String, Configuration> subConfigurations) {
        this.preloadedConfiguration = preloadedConfiguration
        if (subConfigurations != null) {
            this.subConfigurations.putAll(subConfigurations)
        }
    }

    PreloadedConfiguration getPreloadedConfiguration() {
        return preloadedConfiguration
    }

    List<String> getImportConfigurations() {
        if (preloadedConfiguration.imports.trim().length() == 0) return new LinkedList<String>()
        return Arrays.asList(preloadedConfiguration.imports.trim().split(SPLIT_COMMA))
    }

    ConfigurationType getConfigurationLevel() {
        return preloadedConfiguration.type
    }

    void removeFilenamePatternsRecursively() {
        this.filenamePatterns = new RecursiveOverridableMapContainer<>(this, "filenamePatterns")
        for (Configuration parent : parents) {
            parent.removeFilenamePatternsRecursively()
        }
    }

    RecursiveOverridableMapContainer<String, FilenamePattern, Configuration> getFilenamePatterns() {
        filenamePatterns
    }

    RecursiveOverridableMapContainer<String, ToolEntry, Configuration> getTools() {
        tools
    }

    RecursiveOverridableMapContainer<String, Enumeration, Configuration> getEnumerations() {
        enumerations
    }

    RecursiveOverridableMapContainerForConfigurationValues getConfigurationValues() {
        configurationValues
    }

    RecursiveOverridableMapContainer<String, ConfigurationValueBundle, Configuration> getConfigurationValueBundles() {
        configurationValueBundles
    }

    /**
     * Returns the name of this configuration
     *
     * @return
     */
    String getName() {
        preloadedConfiguration.name
    }

    /**
     * Returns the id / path of this configuration like prostate.subproject
     *
     * @return
     */
    @Override
    String getID() {
        if (preloadedConfiguration != null)
            return preloadedConfiguration.id
        else
            return "'Unnamed Configuration'"
    }

    String getDescription() {
        preloadedConfiguration.description
    }

    String getConfiguredClass() {
        preloadedConfiguration.className
    }

    ResourceSetSize getResourcesSize() {
        if (configurationValues.hasValue(ConfigurationConstants.CFG_USED_RESOURCES_SIZE)) {
            try {
                return ResourceSetSize.valueOf(configurationValues.getValue(ConfigurationConstants.CFG_USED_RESOURCES_SIZE).toString())
            } catch (ConfigurationError e) {
                throw new RuntimeException("Unrecoverable error", e)
            }
        }
        return preloadedConfiguration.usedresourcessize
    }

    /**
     * If the configuration is a project configuration then the name of the project will be returned.
     * Variants do not alter the project name and use the name of the first project found in their parents.
     * If the project name is not set this returns the configurations simple name (i.e. project, but not qcpipeline.project)
     * TODO: Implement and verify rules: A project can extend a project and a workflow. A variant can extend a project or a variant.
     *
     * @return The projects name or null.
     */
    String getProjectName() {
        //Search the configuration from which to take the name.
        String projectName = null
        if (this.getConfigurationLevel() == ConfigurationType.PROJECT) {
            projectName = configurationValues.get('projectName', name).toString()
        } else if (this.preloadedConfiguration.type.ordinal() < ConfigurationType.PROJECT.ordinal()) {
            //This is not a project configuration and not a variant.
        } else if (this.preloadedConfiguration.type.ordinal() > ConfigurationType.PROJECT.ordinal()) {
            //Return the parents getProjectName(). This is recursive and should lead to the project configuration.
            String tempName = null
            for (Configuration parent : parents) {
                String tName = parent.projectName
                if (tName == null) {
                    continue
                }
                tempName = tName
                break
            }

            projectName = tempName
        }

        // Return the value and if not set the name of the config.
        return projectName
    }

    List<Configuration> getParents() {
        return parents
    }

    @Override
    RecursiveOverridableMapContainer getContainer(String id) {
        if (configurationValues.is(id)) {
            return configurationValues
        } else if (configurationValueBundles.is(id)) {
            return configurationValueBundles
        } else if (tools.is(id)) {
            return tools
        } else if (enumerations.is(id)) {
            return enumerations
        } else if (filenamePatterns.is(id)) {
            return filenamePatterns
        }
        return null
    }

    /**
     * Clears the list of parent configuration objects and sets c as the single parent.
     *
     * @param c
     */
    void setParent(Configuration c) {
        parents.clear()
        parents.add(c)
    }

    /**
     * Add a parent to the parents list. Note, that the added configuration has a higher priority
     * than the ones already in the list.
     *
     * @param p
     */
    void addParent(Configuration p) {
        if (p == null) return
        if (!parents.contains(p))
            parents.add(p)
    }

    Map<String, Configuration> getSubConfigurations() {
        return subConfigurations
    }

     List<Configuration> getListOfSubConfigurations() {
        return new LinkedList<Configuration>(subConfigurations.values())
    }

    File getBrawlWorkflowSourceFile(String brawlName) {
        // Brawl workflows can have the ending .brawl OR .groovy (better for e.g. Idea)
        File wf = getBrawlWorkflowFile(brawlName, Arrays.asList(".brawl", ".groovy"))
        return wf
    }

    File getJBrawlWorkflowSourceFile(String brawlName) {
        getBrawlWorkflowFile(brawlName, Arrays.asList(".jbrawl"))
    }

    private File getBrawlWorkflowFile(String brawlName, List<String> suffix) {
        List<PluginInfo> pluginInfos = LibrariesFactory.getInstance().getLoadedPlugins()
        Map<String, File> availableBasePaths = new LinkedHashMap<>()
        List<File> allFiles = []
        List<String> filenames = []
        for (String s : suffix)
            filenames.add(brawlName + s)
        FileFilter filter = (FileFilter) new WildcardFileFilter(filenames)
        for (PluginInfo pluginInfo : pluginInfos) {
            File[] files = pluginInfo.getBrawlWorkflowDirectory().listFiles(filter)
            if (files != null && files.length > 0)
                allFiles.addAll(Arrays.asList(files))
        }
        if (allFiles.size() == 1) return allFiles.get(0)
        else if (allFiles.size() == 0)
            logger.severe("No Brawl workflow '" + brawlName + "' could be found")
        else if (allFiles.size() > 1)
            logger.severe("Too many Brawl workflows called " + brawlName)
        return null;
    }

    File getSourceToolPath(String tool) throws ConfigurationError {
        List<PluginInfo> pluginInfos = LibrariesFactory.getInstance().getLoadedPlugins()
        LinkedHashMap<String, File> availableBasePaths = [:]
        for (PluginInfo pluginInfo : pluginInfos) {
            availableBasePaths.putAll(pluginInfo.toolsDirectories)
        }

        ToolEntry te = null
        try {
            te = tools.getValue(tool)
        } catch (ConfigurationError e) {
            throw new ConfigurationError('Unknown tool ID', tool, e)
        }
        if (te.basePathId.length() > 0 && !availableBasePaths.containsKey(te.basePathId)) {
            throw new ConfigurationError('Base path for tool is not configured', tool)
        }
        File bPath = availableBasePaths.get(te.basePathId)

        LinkedHashMap<String, String> localPath = [:]
        localPath.put(ConfigurationConstants.CVALUE_PLACEHOLDER_EXECUTION_DIRECTORY, '.')
        return new File(bPath.absolutePath, te.path)
    }

    /**
     * The actual path to the copy of the tool on the execution host (which can be local or remote).
     */
    File getProcessingToolPath(ExecutionContext context, String tool) throws ConfigurationError {
        ToolEntry te
        try {
            te = tools.getValue(tool);
        } catch (ConfigurationError e) {
            throw new ConfigurationError("Unknown tool ID", tool, e)
        }
        File toolPath = new File(new File(new File(context.executionDirectory, RuntimeService.DIRNAME_ANALYSIS_TOOLS), te.basePathId),
                                 te.path)
        return toolPath
    }

    String getProcessingToolMD5(String tool) throws ConfigurationError {
        if (tool == null || tool == '') {
            logger.warning('Tool id not correctly specified for md5 query.');
            throw new ConfigurationError('Tool ID not correctly specified for md5 query', tool)
        }
        File sourceToolPath = getSourceToolPath(tool)
        return RoddyIOHelperMethods.getMD5OfFile(sourceToolPath)
    }

    String getSSHExecutionUser() {
        configurationValues.get(ConfigurationFactory.XMLTAG_EXECUTIONSERVICE_SSHUSER).toString()
    }

    boolean getShowSSHCalls() {
        configurationValues.getBoolean(ConfigurationFactory.XMLTAG_EXECUTIONSERVICE_SHOW_SSHCALLS)
    }

    @Override
    String toString() {
        String.format('Configuration %s / %s of type %s', name, ID, getClass().name)
    }

    void addValidationError(ConfigurationValidationError error) {
        this.listOfValidationErrors.add(error)
    }

    void addLoadError(ConfigurationLoadError error) {
        this.listOfLoadErrors.add(error)
    }

    void addLoadErrors(Collection<ConfigurationLoadError> errors) {
        this.listOfLoadErrors.addAll(errors)
    }

    List<ConfigurationLoadError> getListOfLoadErrors() {
        def loadErrors = parents.collect { it.getListOfLoadErrors() }.flatten()
        loadErrors += listOfLoadErrors
        return loadErrors as List<ConfigurationLoadError>;
    }

    boolean hasLoadErrors() {
        if (listOfLoadErrors) return true
        for (Configuration parent : parents) {
            if (parent.hasLoadErrors()) return true
        }
        return false
    }

    void addError(ConfigurationIssue error) {
        if (error == null) return
        errors << error
    }

    List<ConfigurationIssue> getErrors(boolean ignoreCValues = false) {
        def errors = parents.collect { it.getErrors(true) }.flatten()
        if (!ignoreCValues)
            errors += configurationValues.allValuesAsList.collectNested { ConfigurationValue val -> val.errors }.flatten()
        return errors as List<ConfigurationIssue>
    }

    boolean hasErrors() {
        if (errors.size() > 0) return true
        for (Configuration parent : parents) { // Could be made shorter, but this is rather efficient as it breaks as soon as an error was found.
            if (parent.hasErrors()) return true
        }
        return configurationValues.allValuesAsList.any { it.hasErrors() }
    }

    void addWarning(ConfigurationIssue warning) {
        if (warning == null) return;
        warnings.add(warning);
    }

    List<ConfigurationIssue> getWarnings(boolean ignoreCValues = false) {
        List<ConfigurationIssue> warnings = parents.collectMany { it.getWarnings(true) }
        if (!ignoreCValues) {
            warnings += configurationValues.allValuesAsList.collectNested {
                ConfigurationValue val -> val.warnings
            }.flatten() as List<ConfigurationIssue>
        }
        warnings
    }

    boolean hasWarnings() {
        if (warnings.size() > 0) return true
        for (Configuration parent : parents) { // Could be made shorter, but this is rather efficient as it breaks as soon as an error was found.
            if (parent.hasWarnings()) return true
        }
        return configurationValues.allValuesAsList.any { it.hasWarnings() }
    }

    boolean isInvalid() {
        this.listOfValidationErrors.size() > 0
    }

    File getFile() {
        preloadedConfiguration?.file
    }
}
