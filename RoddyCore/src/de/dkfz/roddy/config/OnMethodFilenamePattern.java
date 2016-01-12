package de.dkfz.roddy.config;

import de.dkfz.roddy.knowledge.files.BaseFile;

import java.lang.reflect.Method;

/**
 * Created by heinold on 14.01.16.
 */
public class OnMethodFilenamePattern extends FilenamePattern {

    private final Class calledMethodsClass;
    private final Method calledMethodsName;

    public OnMethodFilenamePattern(Class<BaseFile> cls, Class calledClass, Method method, String pattern, String selectionTag) {
        super(cls, pattern, selectionTag);
        this.calledMethodsClass = calledClass;
        this.calledMethodsName = method;
    }

    @Override
    public FilenamePatternDependency getFilenamePatternDependency() {
        return FilenamePatternDependency.onMethod;
    }

    @Override
    protected BaseFile getSourceFile(BaseFile[] baseFiles) {
        BaseFile baseFile = baseFiles[0];
        if (baseFile.getParentFiles() != null && baseFile.getParentFiles().size() > 0)
            return (BaseFile) baseFile.getParentFiles().get(0); //In this case sourcefile is the first of the base files, if at least one basefile is available.
        return null;
    }

    @Override
    public String getID() {
        return String.format("%s::m_%s/r_%s[%s]", cls.getName(), calledMethodsName.getName(), cls.getName(), selectionTag);
    }

    public Method getCalledMethodsName() {
        return calledMethodsName;
    }

    public Class getCalledMethodsClass() { return calledMethodsClass; }
}
