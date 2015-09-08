package de.dkfz.roddy.config;

import de.dkfz.roddy.core.ExecutionContext;
import de.dkfz.roddy.knowledge.files.BaseFile;
import de.dkfz.roddy.knowledge.files.FileObject;
import de.dkfz.roddy.knowledge.files.FileStage;
import de.dkfz.roddy.knowledge.files.FileStageSettings;

import java.io.File;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import static de.dkfz.roddy.StringConstants.*;

/**
 * Filename patterns are stored in a configuration file. They are project specific and should be fully configurable.
 */
public class FilenamePattern implements RecursiveOverridableMapContainer.Identifiable {
    private static final de.dkfz.roddy.tools.LoggerWrapper logger = de.dkfz.roddy.tools.LoggerWrapper.getLogger(FilenamePattern.class.getName());
    public static final String $_SOURCEFILE_ATOMIC_PREFIX = "${sourcefileAtomicPrefix";
    public static final String $_SOURCEFILE_PROPERTY = "${sourcefileProperty";
    public static final String $_CVALUE = "${cvalue";
    public static final String $_JOBPARAMETER = "${jobParameter";
    public static final String DEFAULT_SELECTION_TAG = "default";

    private boolean acceptsFileArrays;


    public enum FilenamePatternDependency {
        SourceClass,
        FileStage,
        MethodName,
        ScriptID;
    }

    private final Class<BaseFile> cls;
    private final Class<BaseFile> derivedFromCls;
    private int enforcedArraySize;
    private final FileStage fileStage;
    private final Class calledMethodsClass;
    private final Method calledMethodsName;
    private final String calledScriptID;
    private final String pattern;
    private final String selectionTag;
    private final FilenamePatternDependency filenamePatternDependency;

    public FilenamePattern(Class<BaseFile> cls, Class<BaseFile> derivedFromCls, String pattern, String selectionTag) {
        this(cls, derivedFromCls, pattern, selectionTag, false, -1);
    }

    public FilenamePattern(Class<BaseFile> cls, Class<BaseFile> derivedFromCls, String pattern, String selectionTag, boolean acceptsFileArrays, int enforcedArraySize) {
        this.cls = cls;
        this.derivedFromCls = derivedFromCls;
        this.acceptsFileArrays = acceptsFileArrays;
        this.enforcedArraySize = enforcedArraySize;
        this.fileStage = null;
        calledMethodsName = null;
        calledMethodsClass = null;
        this.calledScriptID = null;
        this.pattern = pattern;
        this.filenamePatternDependency = FilenamePatternDependency.SourceClass;
        this.selectionTag = selectionTag != null ? selectionTag : DEFAULT_SELECTION_TAG;
    }

    public FilenamePattern(Class<BaseFile> cls, FileStage fileStage, String pattern, String selectionTag) {
        this.cls = cls;
        this.fileStage = fileStage;
        this.derivedFromCls = null;
        calledMethodsName = null;
        calledMethodsClass = null;
        this.calledScriptID = null;
        this.pattern = pattern;
        this.filenamePatternDependency = FilenamePatternDependency.FileStage;
        this.selectionTag = selectionTag != null ? selectionTag : DEFAULT_SELECTION_TAG;
    }

    /**
     * OnScript
     * @param cls
     * @param pattern
     * @param selectionTag
     */
    public FilenamePattern(Class<BaseFile> cls, String script, String pattern, String selectionTag) {
        this.cls = cls;
        this.calledScriptID = script;
        this.pattern = pattern;
        this.filenamePatternDependency = FilenamePatternDependency.ScriptID;
        this.selectionTag = selectionTag != null ? selectionTag : DEFAULT_SELECTION_TAG;
        fileStage = null;
        derivedFromCls = null;
        calledMethodsName = null;
        calledMethodsClass = null;
    }

    /**
     * OnMethod
     * @param cls
     * @param calledClass
     * @param method
     * @param pattern
     * @param selectionTag
     */
    public FilenamePattern(Class<BaseFile> cls, Class calledClass, Method method, String pattern, String selectionTag) {
        this.cls = cls;
        this.fileStage = null;
        this.derivedFromCls = null;
        this.calledScriptID = null;
        this.calledMethodsClass = calledClass;
        this.calledMethodsName = method;
        this.pattern = pattern;
        this.filenamePatternDependency = FilenamePatternDependency.MethodName;
        this.selectionTag = selectionTag != null ? selectionTag : DEFAULT_SELECTION_TAG;
    }

    public static String assembleID(Class<BaseFile> cls, Class<BaseFile> derivedFromCls, String selectionTag) {
        return  String.format("%s::c_%s[%s]", cls.getName(), derivedFromCls.getName(), selectionTag);
    }

    public static String assembleID(Class<BaseFile> cls, FileStage stage, String selectionTag) {
        return String.format("%s::#_%s[%s]", cls.getName(), stage.toString(), selectionTag);
    }

    public static String assembleID(Class<FileObject> cls, Method calledMethodsName, Class createdClass, String selectionTag) {
        return String.format("%s::m_%s/r_%s[%s]", cls.getName(), calledMethodsName.getName(), createdClass.getName(), selectionTag);
    }

    public static String assembleID(Class<BaseFile> cls, String scriptID, String selectionTag) {
        return String.format("%s::onS_%s[%s]", cls.getName(), scriptID, selectionTag);
    }

    @Override
    public String getID() {
        if (filenamePatternDependency == FilenamePatternDependency.FileStage)
            return assembleID(cls, fileStage, selectionTag);
        if (filenamePatternDependency == FilenamePatternDependency.SourceClass)
            return assembleID(cls, derivedFromCls, selectionTag);
        if (filenamePatternDependency == FilenamePatternDependency.ScriptID)
            return assembleID(cls, calledScriptID, selectionTag);
        if (filenamePatternDependency == FilenamePatternDependency.MethodName)
            return assembleID(calledMethodsClass, calledMethodsName, cls, selectionTag);
        return null;
    }

    public Class<BaseFile> getCls() {
        return cls;
    }

    public Class<BaseFile> getDerivedFromCls() {
        return derivedFromCls;
    }

    public Method getCalledMethodsName() {
        return calledMethodsName;
    }

    public Class getCalledMethodsClass() { return calledMethodsClass; }

    public String getPattern() {
        return pattern;
    }

    public FileStage getFileStage() {
        return fileStage;
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

    public FilenamePatternDependency getFilenamePatternDependency() {
        return filenamePatternDependency;
    }

    public boolean hasSelectionTag() {
        return !selectionTag.equals(DEFAULT_SELECTION_TAG);
    }

    public String getSelectionTag() {
        return selectionTag;
    }

    private String fillVariables(BaseFile baseFile, String temp) {
        ExecutionContext run = baseFile.getExecutionContext();
        Configuration cfg = run.getConfiguration();
        //Different output folder
        String oPath = run.getOutputDirectory().getAbsolutePath();
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

                    String pathSup = cval.getType().equals("path") ? cval.toFile(run).getAbsolutePath() : cval.toString();
                    temp = temp.replace(s, pathSup);
                }
            }
        }

        while (temp.contains($_CVALUE)) {
            Command command = extractCommand($_CVALUE, temp);
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

    private String fillVariablesFromSourceFileValues(BaseFile baseFile, String temp) {
        FileStageSettings fs = baseFile.getFileStage();
        String _temp = temp;
        temp = fs.fillStringContent(temp);
        if(temp == null)
            temp = _temp; //TODO Look why temp gets null. This should not be the case.
        //pid, sample, run...
        temp = temp.replace("${fileStageID}", fs.getIDString());
        temp = temp.replace("${pid}", baseFile.getPid().toString());
        return temp;
    }

    private String fillVariablesFromSourceFileArrayValues(BaseFile baseFile, String temp, int index) {
        FileStageSettings fs = baseFile.getFileStage();
        //pid, sample, run...
        temp = temp.replace(String.format("${fileStageID[%d]}", index), fs.getIDString());
        temp = temp.replace(String.format("${pid[%d]}", index), baseFile.getPid().toString());
        temp = fs.fillStringContentWithArrayValues(index, temp);
        return temp;
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
            BaseFile sourceFile = null;
            temp = pattern;

            if (filenamePatternDependency == FilenamePatternDependency.SourceClass) {
                //Filename pattern dependency is always derived from
                sourceFile = baseFile; //In this case sourcefile is the basefile.
            } else if (filenamePatternDependency == FilenamePatternDependency.FileStage) {
                //            BaseFile.FileStage tgtFileStage = baseFile.getFileStage().getStage();
            } else if (filenamePatternDependency == FilenamePatternDependency.MethodName) {
                if (baseFile.getParentFiles() != null && baseFile.getParentFiles().size() > 0)
                    sourceFile = (BaseFile)baseFile.getParentFiles().get(0); //In this case sourcefile is the first of the base files, if at least one basefile is available.
            }

            // There is one source file existing, so source file based options can be applied.
            if (sourceFile != null) {
                File sourcepath = sourceFile.getPath();
                temp = temp.replace("${sourcefile}", sourcepath.getAbsolutePath());
                temp = temp.replace("${sourcefileAtomic}", sourcepath.getName());
                if (temp.contains($_SOURCEFILE_PROPERTY)) { //Replace the string with a property value
                    Command command = extractCommand($_SOURCEFILE_PROPERTY, temp);
                    String pName = command.attributes.keySet().toArray()[0].toString();

                    String accessorName = "get" + pName.substring(0, 1).toUpperCase() + pName.substring(1);
                    Method accessorMethod = sourceFile.getClass().getMethod(accessorName);
                    String value = accessorMethod.invoke(sourceFile).toString();
                    temp = temp.replace(command.name, value);
                }
                if (temp.contains($_SOURCEFILE_ATOMIC_PREFIX)) {
                    Command command = extractCommand($_SOURCEFILE_ATOMIC_PREFIX, temp);
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
            if(attributeSplit.length == 2)
                value = attributeSplit[1];
            attributes.put(name, new CommandAttribute(name, value));
        }

        return new Command(command, attributes);
    }
}
