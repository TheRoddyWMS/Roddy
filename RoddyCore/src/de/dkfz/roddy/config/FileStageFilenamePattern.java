/*
 * Copyright (c) 2016 German Cancer Research Center (Deutsches Krebsforschungszentrum, DKFZ).
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/TheRoddyWMS/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.config;

import de.dkfz.roddy.knowledge.files.BaseFile;
import de.dkfz.roddy.knowledge.files.FileStage;

/**
 * Created by heinold on 14.01.16.
 */
public class FileStageFilenamePattern extends FilenamePattern {

    private final FileStage fileStage;
    public FileStageFilenamePattern(Class<BaseFile> cls, FileStage fileStage, String pattern, String selectionTag) {
        super(cls, pattern, selectionTag);
        this.fileStage = fileStage;
    }

    @Override
    public String getID() {
        return String.format("%s::#_%s[%s]", cls.getName(), fileStage.toString(), selectionTag);
    }

    public FileStage getFileStage() {
        return fileStage;
    }

    @Override
    public FilenamePatternDependency getFilenamePatternDependency() {
        return FilenamePatternDependency.FileStage;
    }

    @Override
    protected BaseFile getSourceFile(BaseFile[] baseFiles) {
        return null;
    }

    public String toString() {
        return super.toString() + " fileStage=" + fileStage;
    }

}
