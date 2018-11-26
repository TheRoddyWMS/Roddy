/*
 * Copyright (c) 2016 German Cancer Research Center (Deutsches Krebsforschungszentrum, DKFZ).
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.config;

import de.dkfz.roddy.knowledge.files.BaseFile;

/**
 * Created by heinold on 14.01.16.
 */
public class DerivedFromFilenamePattern extends FilenamePattern {
    protected final Class<BaseFile> derivedFromCls;

    public DerivedFromFilenamePattern(Class<BaseFile> cls, Class<BaseFile> derivedFromCls, String pattern, String selectionTag) {
        this(cls, derivedFromCls, pattern, selectionTag, false, -1);
    }

    public DerivedFromFilenamePattern(Class<BaseFile> cls, Class<BaseFile> derivedFromCls, String pattern, String selectionTag, boolean acceptsFileArrays, int enforcedArraySize) {
        super(cls, pattern, selectionTag);
        this.derivedFromCls = derivedFromCls;
        this.acceptsFileArrays = acceptsFileArrays;
        this.enforcedArraySize = enforcedArraySize;
    }

    @Override
    public String getID() {
        return String.format("%s::c_%s[%s]", cls.getName(), derivedFromCls.getName(), selectionTag);
    }

    @Override
    public FilenamePatternDependency getFilenamePatternDependency() {
        return FilenamePatternDependency.derivedFrom;
    }

    @Override
    protected BaseFile getSourceFile(BaseFile[] baseFiles) {
        return (BaseFile) baseFiles[0].getParentFiles().get(0);
    }

    public Class<BaseFile> getDerivedFromCls() {
        return derivedFromCls;
    }

    public String toString() {
        return super.toString() + ", derivedFromClass=" + derivedFromCls.getCanonicalName();
    }
}
