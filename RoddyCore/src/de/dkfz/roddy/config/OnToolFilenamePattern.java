package de.dkfz.roddy.config;

import de.dkfz.roddy.knowledge.files.BaseFile;

/**
 * Created by heinold on 14.01.16.
 */
public class OnToolFilenamePattern extends FilenamePattern {
    private final String calledScriptID;

    public OnToolFilenamePattern(Class<BaseFile> cls, String script, String pattern, String selectionTag) {
        super(cls, pattern, selectionTag);
        this.calledScriptID = script;
    }

    @Override
    public String getID() {
        return String.format("%s::onS_%s[%s]", cls.getName(), calledScriptID, selectionTag);
    }

    public String getCalledScriptID() { return calledScriptID; }

    @Override
    public FilenamePatternDependency getFilenamePatternDependency() {
        return FilenamePatternDependency.onTool;
    }
}
