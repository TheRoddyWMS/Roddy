/*
 * Copyright (c) 2016 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.config;

import de.dkfz.roddy.Roddy;
import de.dkfz.roddy.core.ExecutionContext;
import de.dkfz.roddy.knowledge.files.BaseFile;
import de.dkfz.roddy.knowledge.files.FileStageSettings;

import java.io.File;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import static de.dkfz.roddy.StringConstants.*;

/**
 * Filename patterns are stored in a configuration file. They are project specific and should be fully configurable.
 */
public abstract class FilenamePattern implements RecursiveOverridableMapContainer.Identifiable {

    private static final de.dkfz.roddy.tools.LoggerWrapper logger = de.dkfz.roddy.tools.LoggerWrapper.getLogger(FilenamePattern.class.getSimpleName());
    public static final String PLACEHOLDER_SOURCEFILE_ATOMIC_PREFIX = "${sourcefileAtomicPrefix";
    public static final String PLACEHOLDER_SOURCEFILE_PROPERTY = "${sourcefileProperty";
    public static final String PLACEHOLDER_CVALUE = "${cvalue";
    public static final String PLACEHOLDER_JOBPARAMETER = "${jobParameter";
    public static final String DEFAULT_SELECTION_TAG = "default";

    protected final Class<BaseFile> cls;
    protected final String pattern;
    protected final String selectionTag;
    protected boolean acceptsFileArrays;
    protected int enforcedArraySize;

    public FilenamePattern(Class<BaseFile> cls, String pattern, String selectionTag) {
        this.cls = cls;
        this.pattern = pattern;
        this.selectionTag = selectionTag != null ? selectionTag : DEFAULT_SELECTION_TAG;
    }

    public Class<BaseFile> getCls() {
        return cls;
    }

    public String getPattern() {
        return pattern;
    }

    public boolean hasSelectionTag() {
        return !selectionTag.equals(DEFAULT_SELECTION_TAG);
    }

    public String getSelectionTag() {
        return selectionTag;
    }

    public boolean doesAcceptFileArrays() {
        return acceptsFileArrays;
    }

    public boolean hasEnforcedArraySize() {
        return enforcedArraySize > 0;
    }

    public int getEnforcedArraySize() {
        return enforcedArraySize;
    }

    public abstract FilenamePatternDependency getFilenamePatternDependency();

    protected String fillVariables(BaseFile baseFile, String temp) {
        ExecutionContext context = baseFile.getExecutionContext();
        Configuration cfg = context.getConfiguration();
        //Different output folder
        String oPath = context.getOutputDirectory().getAbsolutePath();
//        temp = temp.replace("${outputbasepath}", oPath);
        temp = temp.replace("${outputAnalysisBaseDirectory}", oPath);
        //Replace output directories containing OutputDirectory
        final String odComp = "OutputDirectory";
        if (temp.contains(odComp)) {
            String[] split = temp.split(File.separator);
            for (String s : split) {
                if (s.contains(odComp)) {
                    String cvalID = s.substring(2, s.length() - 1);
                    ConfigurationValue cval = cfg.getConfigurationValues().get(cvalID);

                    String pathSup = cval.getType().equals("path") ? cval.toFile(context).getAbsolutePath() : cval.toString();
                    pathSup = pathSup.replace(Roddy.getApplicationDirectory().getAbsolutePath() + "/", ""); //Remove Roddy application folder from path...
                    temp = temp.replace(s, pathSup);
                }
            }
        }

        while (temp.contains(PLACEHOLDER_CVALUE)) {
            Command command = extractCommand(PLACEHOLDER_CVALUE, temp);
            CommandAttribute name = command.attributes.get("name");
            CommandAttribute def = command.attributes.get("default");
            if (name != null) {
                ConfigurationValue cv = null;

                if (def != null)
                    cv = cfg.getConfigurationValues().get(name.value, def.value);
                else
                    cv = cfg.getConfigurationValues().get(name.value);

                if (cv != null) {
                    temp = temp.replace(command.name, cv.toString());
                }
            }
        }
        return temp;
    }

    protected String fillVariablesFromSourceFileValues(BaseFile baseFile, String temp) {
        FileStageSettings fs = baseFile.getFileStage();
        String _temp = temp;
        temp = fs.fillStringContent(temp);
        if (temp == null)
            temp = _temp; //TODO Look why temp gets null. This should not be the case.
        //pid, sample, run...
        temp = temp.replace("${fileStageID}", fs.getIDString());
        temp = temp.replace("${pid}", baseFile.getDataSet().toString()); // TODO: Move to plugin.
        temp = temp.replace("${dataSet}", baseFile.getDataSet().toString());
        return temp;
    }

    protected String fillVariablesFromSourceFileArrayValues(BaseFile baseFile, String temp, int index) {
        FileStageSettings fs = baseFile.getFileStage();
        //pid, sample, run...
        temp = temp.replace(String.format("${fileStageID[%d]}", index), fs.getIDString());
        temp = temp.replace(String.format("${pid[%d]}", index), baseFile.getDataSet().toString()); // TODO: Move to plugin.
        temp = temp.replace(String.format("${dataSet[%d]}", index), baseFile.getDataSet().toString());
        temp = fs.fillStringContentWithArrayValues(index, temp);
        return temp;
    }

    protected BaseFile getSourceFile(BaseFile[] baseFiles) {
        return null;
    }

    /**
     * Applies the pattern to a filename.
     * If the pattern derives the name from a file, put in the sourcefile.
     * If the pattern uses the filestage, provide the targetfile.
     *
     * @param baseFile Either the source or the target file, depending on the patterns writeConfigurationFile.
     * @return
     */
    public String apply(BaseFile baseFile) {
        return apply(new BaseFile[]{baseFile});
    }

    /**
     * Applies this pattern to several filenames. Only derivedFrom is supported as dependency!
     *
     * @param baseFiles
     * @return
     */
    public String apply(BaseFile[] baseFiles) {

        String temp = null;
        try {
            temp = null;
            BaseFile baseFile = baseFiles[0];
            BaseFile sourceFile = getSourceFile(baseFiles);
            temp = pattern;

            // There is one source file existing, so source file based options can be applied.
            if (sourceFile != null) {
                File sourcepath = sourceFile.getPath();
                temp = temp.replace("${sourcefile}", sourcepath.getAbsolutePath());
                temp = temp.replace("${sourcefileAtomic}", sourcepath.getName());
                if (temp.contains(PLACEHOLDER_SOURCEFILE_PROPERTY)) { //Replace the string with a property value
                    Command command = extractCommand(PLACEHOLDER_SOURCEFILE_PROPERTY, temp);
                    String pName = command.attributes.keySet().toArray()[0].toString();

                    String accessorName = "get" + pName.substring(0, 1).toUpperCase() + pName.substring(1);
                    Method accessorMethod = sourceFile.getClass().getMethod(accessorName);
                    String value = accessorMethod.invoke(sourceFile).toString();
                    temp = temp.replace(command.name, value);
                }
                if (temp.contains(PLACEHOLDER_SOURCEFILE_ATOMIC_PREFIX)) {
                    Command command = extractCommand(PLACEHOLDER_SOURCEFILE_ATOMIC_PREFIX, temp);
                    CommandAttribute att = command.attributes.get("delimiter");
                    if (att != null) {
                        String sourcename = sourcepath.getName();
                        temp = temp.replace(command.name, sourcename.substring(0, sourcename.lastIndexOf(att.value)));
                    }
                }

                temp = temp.replace("${sourcepath}", sourcepath.getParent());
                if (acceptsFileArrays && (enforcedArraySize == -1 || (enforcedArraySize != -1 && enforcedArraySize == baseFiles.length)))
                    for (int i = 0; i < baseFiles.length; i++) {
                        temp = fillVariablesFromSourceFileArrayValues(baseFiles[i], temp, i);
                    }

            }

            temp = fillVariables(baseFile, temp);
            temp = fillVariablesFromSourceFileValues(baseFile, temp);
        } catch (Exception e) {
            logger.severe("Could not apply filename pattern " + pattern + " for file " + baseFiles[0]);
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        return temp;
    }

    public static class Command {
        public final String name;
        public final Map<String, CommandAttribute> attributes = new HashMap<String, CommandAttribute>();

        private Command(String name, Map<String, CommandAttribute> attributes) {
            this.name = name;
            if (attributes != null)
                this.attributes.putAll(attributes);
        }
    }

    public static class CommandAttribute {
        public final String name;
        public final String value;

        private CommandAttribute(String name, String value) {
            this.name = name;
            this.value = value;
        }
    }

    public static Command extractCommand(String commandID, String temp) {
        int startIndex = temp.indexOf(commandID);
        int endIndex = temp.indexOf(BRACE_RIGHT, startIndex);
        String command = temp.substring(startIndex, endIndex + 1);

        Map<String, CommandAttribute> attributes = new HashMap<String, CommandAttribute>();

        String[] split = command.split(SPLIT_COMMA);
        for (int i = 1; i < split.length; i++) { //Start with the first option.
            String _split = split[i].replaceAll("[^0-9a-zA-Z=.-_+#]", EMPTY);
            String[] attributeSplit = _split.split(SPLIT_EQUALS);
            String name = attributeSplit[0];
            String value = EMPTY;
            if (attributeSplit.length == 2)
                value = attributeSplit[1];
            attributes.put(name, new CommandAttribute(name, value));
        }

        return new Command(command, attributes);
    }
}
