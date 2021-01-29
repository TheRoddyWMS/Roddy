/*
 * Copyright (c) 2021 German Cancer Research Center (Deutsches Krebsforschungszentrum, DKFZ).
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/TheRoddyWMS/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.config

import groovy.transform.CompileStatic
import groovy.util.slurpersupport.NodeChild

/**
 * This class serves as an intermediate parsing result of the XML towards the final Configuration object.
 *
 * The ConfigurationFactory reads in all XMLs (directly using the Groovy XML slurper), only extracts the configuration dependencies (such as
 * analyses and imports), such that only the minimal amount of parsing has to be done. From the parsed information the
 * PreloadedConfiguration is produced. All preloaded configuration objects are stored as value in an map and indexed by their 'id' field.
 *
 * As soon as it is know, which configurations are required, these Configurations objects are produced using the information in the preloaded
 * configuration and the remaining XMLs.
 */
@CompileStatic
class PreloadedConfiguration {
    public final PreloadedConfiguration parent
    public final Configuration.ConfigurationType type
    public final String name
    public final String description
    public final String className
    public final NodeChild configurationNode
    public final String imports
    public final File file
    public final String text
    public final String id

    private File readmeFile

    private final List<PreloadedConfiguration> subConf
    private final List<String> analyses = []
    public final ResourceSetSize usedresourcessize

    PreloadedConfiguration(PreloadedConfiguration parent, Configuration.ConfigurationType type, String name, String description,
                           String className, NodeChild configurationNode, String imports, ResourceSetSize usedresourcessize,
                           List<String> analyses, List<PreloadedConfiguration> subContent, File file, String text) {
        this(parent, type, name, description, className, configurationNode, imports, subContent,
                file, text, usedresourcessize)
        if (analyses != null)
            this.analyses.addAll(analyses)
    }

    PreloadedConfiguration(PreloadedConfiguration parent, Configuration.ConfigurationType type, String name, String description,
                           String className, NodeChild configurationNode, String imports,
                           List<PreloadedConfiguration> subContent, File file, String text,
                           ResourceSetSize usedresourcessize = null) {
        this.type = type
        this.name = name
        this.className = className
        this.configurationNode = configurationNode
        this.description = description
        this.usedresourcessize = usedresourcessize
        if (parent == null && type == Configuration.ConfigurationType.PROJECT) {
            if (imports) {
                this.imports = "default,${imports}"
            } else {
                this.imports = 'default'
            }
        } else {
            this.imports = imports
        }
        this.file = file
        this.parent = parent
        if (parent != null)
            id = "${parent.id}.${name}"
        else
            id = name
        if (subContent != null)
            this.subConf = subContent
        else
            this.subConf = new LinkedList<PreloadedConfiguration>()
        this.text = text
    }

    /**
     * Returns a shallow copy of the list of analysesByID.
     * Each analysis has an id (like a name for a project) and the configuration name on which it is based.
     * i.e. exome::exomeAnalysis
     * @return
     */
    List<String> getListOfAnalyses() {
        return new LinkedList<String>(analyses)
    }

    PreloadedConfiguration getParent() {
        return parent
    }

    List<PreloadedConfiguration> getSubContent() {
        return new LinkedList<PreloadedConfiguration>(subConf)
    }

    List<PreloadedConfiguration> getAllSubContent() {
        List<PreloadedConfiguration> sc = []
        sc.addAll(subConf)
        subConf.each { PreloadedConfiguration it -> sc.addAll(it.getAllSubContent()) }
        return sc
    }

    @Override
    String toString() {
        return "Config: ${name} of type ${type} in file ${file.absolutePath}"
    }

    void setReadmeFile(File file) {
        this.readmeFile = file
    }

    File getReadmeFile() {
        return readmeFile
    }
}
