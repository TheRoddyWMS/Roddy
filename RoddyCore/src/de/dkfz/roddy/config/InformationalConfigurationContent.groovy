package de.dkfz.roddy.config

import groovy.util.slurpersupport.NodeChild

/**
 */
@groovy.transform.CompileStatic
class InformationalConfigurationContent {
    public final InformationalConfigurationContent parent;
    public final Configuration.ConfigurationType type;
    public final String name;
    public final String description;
    public final String className;
    public final NodeChild configurationNode;
    public final String imports;
    public final File file;
    public final String text;
    public final String id;

    private File readmeFile;

    private final List<InformationalConfigurationContent> subConf;
    private final List<String> analyses;
    public final ToolEntry.ResourceSetSize usedresourcessize;

    InformationalConfigurationContent(InformationalConfigurationContent parent, Configuration.ConfigurationType type, String name, String description, String className, NodeChild configurationNode, String imports, ToolEntry.ResourceSetSize usedresourcessize, List<String> analyses, List<InformationalConfigurationContent> subContent, File file, String text) {
        this(parent, type, name, description, className, configurationNode, imports, subContent, file, text, usedresourcessize);
        if (analyses != null)
            this.analyses = analyses;
        else
            this.analyses = [];
    }

    InformationalConfigurationContent(InformationalConfigurationContent parent, Configuration.ConfigurationType type, String name, String description, String className, NodeChild configurationNode, String imports, List<InformationalConfigurationContent> subContent, File file, String text, ToolEntry.ResourceSetSize usedresourcessize = null) {
        this.type = type
        this.name = name
        this.className = className
        this.configurationNode = configurationNode
        this.description = description
        this.usedresourcessize = usedresourcessize;
        if (parent == null && type == Configuration.ConfigurationType.PROJECT) {
            if (imports) {
                this.imports = "default,${imports}"
            } else {
                this.imports = "default"
            }
        } else {
            this.imports = imports;
        }
        this.file = file
        this.parent = parent
        if (parent != null)
            id = "${parent.id}.${name}";
        else
            id = name;
        if (subContent != null)
            this.subConf = subContent;
        else
            this.subConf = new LinkedList<InformationalConfigurationContent>();
        this.text = text;
    }

    /**
     * Returns a shallow copy of the list of analysesByID.
     * Each analysis has an id (like a name for a project) and the configuration name on which it is based.
     * i.e. exome::exomeAnalysis
     * @return
     */
    List<String> getListOfAnalyses() {
        return new LinkedList<String>(analyses);
    }

    InformationalConfigurationContent getParent() {
        return parent
    }

    List<InformationalConfigurationContent> getSubContent() {
        return new LinkedList<InformationalConfigurationContent>(subConf);
    }

    List<InformationalConfigurationContent> getAllSubContent() {
        List<InformationalConfigurationContent> sc = [];
        sc.addAll(subConf);
        subConf.each { InformationalConfigurationContent it -> sc.addAll(it.getAllSubContent()); }
        return sc;
    }

    @Override
    public String toString() {
        return "Config: ${name} of type ${type} in file ${file.absolutePath}";
    }

    public void setReadmeFile(File file) {
        this.readmeFile = file;
    }

    public File getReadmeFile() {
        return readmeFile;
    }
}
