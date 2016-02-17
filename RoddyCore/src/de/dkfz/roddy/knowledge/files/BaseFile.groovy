package de.dkfz.roddy.knowledge.files;

import de.dkfz.roddy.AvailableFeatureToggles;
import de.dkfz.roddy.Roddy;
import de.dkfz.roddy.config.Configuration
import de.dkfz.roddy.config.DerivedFromFilenamePattern
import de.dkfz.roddy.config.FileStageFilenamePattern;
import de.dkfz.roddy.config.FilenamePattern
import de.dkfz.roddy.config.FilenamePatternDependency
import de.dkfz.roddy.config.OnMethodFilenamePattern
import de.dkfz.roddy.config.OnToolFilenamePattern
import de.dkfz.roddy.config.ToolEntry;
import de.dkfz.roddy.core.ExecutionContext
import de.dkfz.roddy.core.ExecutionContextLevel;
import de.dkfz.roddy.core.Workflow;
import de.dkfz.roddy.execution.io.fs.FileSystemAccessProvider;
import de.dkfz.roddy.execution.jobs.Job;
import de.dkfz.roddy.execution.jobs.JobResult
import de.dkfz.roddy.plugins.LibrariesFactory
import de.dkfz.roddy.tools.Tuple2

import java.util.*;

/**
 * Basic class for all processed files. Contains information about the storage
 * location of a file and the associated project. Might contain a list of files
 * which led to the creation of this file. Might also contain a list of files
 * which were created with this file. With this double-list implementation, you
 * can span up a file-creation-tree.
 * <p>
 * Remark: This class cannot be converted to groovy, because groovy will have
 * problems with the constructors. When a stub class is generated, several
 * constructors will match with the generated "null"-calls.
 *
 * @author michael
 */
@groovy.transform.CompileStatic
public abstract class BaseFile<FS extends FileStageSettings> extends FileObject {

    public static abstract class ConstructionHelperForBaseFiles {
        public final ExecutionContext context;
        public final FileStageSettings fileStageSettings;
        public final String selectionTag
        public final JobResult jobResult

        protected ConstructionHelperForBaseFiles(ExecutionContext context, FileStageSettings fileStageSettings, String selectionTag, JobResult jobResult) {
            this.context = context
            this.fileStageSettings = fileStageSettings
            this.selectionTag = selectionTag
            this.jobResult = jobResult
        }
    }

    public static class ConstructionHelperForSourceFiles extends ConstructionHelperForBaseFiles {

        public final File path

        public ConstructionHelperForSourceFiles(File path, ExecutionContext context, FileStageSettings fileStageSettings, JobResult jobResult) {
            super(context, fileStageSettings, null, jobResult)
            this.path = path
        }

        File getPath() {
            return path
        }
    }

    /**
     * A helper class specifically for GenericMethod based file creation
     */
    public static class ConstructionHelperForGenericCreation<T extends ConstructionHelperForGenericCreation> extends ConstructionHelperForBaseFiles {

        public final FileObject parentObject
        public final ToolEntry creatingTool
        public final String toolID
        public final String slotID
        public final List<FileObject> parentFiles

        public ConstructionHelperForGenericCreation(FileObject parentObject, List<FileObject> parentFiles, ToolEntry creatingTool, String toolID, String slotID, String selectionTag, FileStageSettings fileStageSettings, JobResult jobResult) {
            super(parentObject?.getExecutionContext(), fileStageSettings, selectionTag, jobResult)
            this.parentFiles = parentFiles
            this.slotID = slotID
            this.toolID = toolID
            this.creatingTool = creatingTool
            this.parentObject = parentObject
        }

        public ConstructionHelperForGenericCreation(ExecutionContext context, ToolEntry creatingTool, String toolID, String slotID, String selectionTag, FileStageSettings fileStageSettings, JobResult jobResult) {
            super(context, fileStageSettings, selectionTag, jobResult)
            this.slotID = slotID
            this.toolID = toolID
            this.creatingTool = creatingTool
            this.parentObject = parentObject
        }
    }

    public static class ConstructionHelperForManualCreation extends ConstructionHelperForGenericCreation {
        public ConstructionHelperForManualCreation(FileObject parentObject, List<FileObject> parentFiles, ToolEntry creatingTool, String toolID, String slotID, String selectionTag, FileStageSettings fileStageSettings, JobResult jobResult) {
            super(parentObject, parentFiles, creatingTool, toolID, slotID, selectionTag, fileStageSettings, jobResult);
        }

        public ConstructionHelperForManualCreation(ExecutionContext context, ToolEntry creatingTool, String toolID, String slotID, String selectionTag, FileStageSettings fileStageSettings, JobResult jobResult) {
            super(context, creatingTool, toolID, slotID, selectionTag, fileStageSettings, jobResult);
        }
    }


    public static BaseFile constructSourceFile(Class<? extends BaseFile> classToConstruct, File path, ExecutionContext context, FileStageSettings fileStageSettings = null, JobResult jobResult = null) {
        return classToConstruct.newInstance(new ConstructionHelperForSourceFiles(path, context, fileStageSettings, jobResult));
    }

    public
    static BaseFile constructGeneric(Class<? extends BaseFile> classToConstruct, FileObject parentObject, List<FileObject> parentFiles, ToolEntry creatingTool, String toolID, String slotID, String selectionTag, FileStageSettings fileStageSettings, JobResult jobResult) {
        return classToConstruct.newInstance(new ConstructionHelperForGenericCreation(parentObject, parentFiles, creatingTool, toolID, slotID, selectionTag, fileStageSettings, jobResult));
    }

    public
    static BaseFile constructGeneric(Class<? extends BaseFile> classToConstruct, ExecutionContext context, ToolEntry creatingTool, String toolID, String slotID, String selectionTag, FileStageSettings fileStageSettings, JobResult jobResult) {
        return classToConstruct.newInstance(new ConstructionHelperForGenericCreation(context, creatingTool, toolID, slotID, selectionTag, fileStageSettings, jobResult));
    }

    public
    static BaseFile constructManual(Class<? extends BaseFile> classToConstruct, FileObject parentObject, List<FileObject> parentFiles, ToolEntry creatingTool, String toolID, String slotID, String selectionTag, FileStageSettings fileStageSettings, JobResult jobResult) {
        return classToConstruct.newInstance(new ConstructionHelperForManualCreation(parentObject, parentFiles, creatingTool, toolID, slotID, selectionTag, fileStageSettings, jobResult));
    }

    public
    static BaseFile constructManual(Class<? extends BaseFile> classToConstruct, ExecutionContext context, ToolEntry creatingTool, String toolID, String slotID, String selectionTag, FileStageSettings fileStageSettings, JobResult jobResult) {
        return classToConstruct.newInstance(new ConstructionHelperForManualCreation(context, creatingTool, toolID, slotID, selectionTag, fileStageSettings, jobResult));
    }

    public static BaseFile constructManual(Class<? extends BaseFile> classToConstruct, FileObject parentFile) {
        return constructManual(classToConstruct, parentFile, null, null, null, null, null, null, null);
    }


    protected File path;

    protected final List<BaseFile> parentFiles = new LinkedList<>();

    /**
     * A BaseFile object can be grouped into one or more groups.
     */
    protected final List<FileGroup> fileGroups = new LinkedList<FileGroup>();

    protected FS fileStageSettings;

    private boolean valid;

    /**
     * This list keeps a reference to all jobs which tried to create this file.
     * A file can exist only once but the attempt to create it could be done in multiple previous jobs!
     */
    private final List<Job> listOfParentJobs = new LinkedList<Job>();

    /**
     * This list keeps a reference to all processes which tried to create this file.
     * A file can exist only once but the attempt to create it could be done in multiple previous processes!
     */
    private transient final List<ExecutionContext> listOfParentProcesses = new LinkedList<ExecutionContext>();
    /**
     * Upon execution a file may be marked as temporary. This means that a file
     * might be deleted automatically by Roddy with some sort of cleanup job or
     * cleanup process and that it will not be recreated upon rerun.
     * <p>
     * TODO Upon rerun and a change to the pipeline the file might be necessary
     * for new jobs. In this case it would currently be recreated but also
     * all the existing files! One solution could be to store information
     * about created files in a logfile (size, name, md5?, source and
     * targetfile) and to evaluate this file on rerun. But this can also
     * lead to problems.
     */
    private boolean isTemporaryFile;

    private Boolean isReadable = null;
    private boolean isSourceFile = false;

    private FilenamePattern appliedFilenamePattern = null;

    protected BaseFile(ConstructionHelperForBaseFiles helper) {
        super(helper.context);
        executionContext.addFile(this);

        if (helper instanceof ConstructionHelperForGenericCreation) { //Manual creation is currently intrinsic.
            ConstructionHelperForGenericCreation _helper = helper as ConstructionHelperForGenericCreation;

            if (_helper.parentObject instanceof FileGroup) {
                parentFiles.addAll((_helper.parentObject as FileGroup).getFilesInGroup());
                this.fileStageSettings = _helper.fileStageSettings
            } else if (_helper.parentObject instanceof BaseFile) {
                parentFiles.add(_helper.parentObject as BaseFile);
                this.fileStageSettings = _helper.fileStageSettings ?: (FS) (_helper.parentObject as BaseFile)?.fileStageSettings ?: null;
            }
            Tuple2<File, FilenamePattern> fnresult = getFilename(this, _helper.selectionTag);
            if (fnresult) {
                this.path = fnresult.x;
                this.appliedFilenamePattern = fnresult.y;
            }
        } else if (helper instanceof ConstructionHelperForSourceFiles) {
            ConstructionHelperForSourceFiles _helper = helper as ConstructionHelperForSourceFiles;

            this.fileStageSettings = _helper.fileStageSettings;
            this.path = _helper.getPath();
            setAsSourceFile();
        } else {
            //Do not allow custom classes.
            throw new RuntimeException("It is not allowed to use custom Construction Helper classes for BaseFile objects");
        }

        this.setCreatingJobsResult(helper.jobResult);
        determineFileValidityBasedOnContextLevel(executionContext);
    }

    /** A copy constructor for something like "extending" classes **/
    protected BaseFile(BaseFile parent) {
        super(parent.executionContext);
        this.fileStageSettings = parent.fileStageSettings;
        this.path = parent.path;
        this.parentFiles += parent.parentFiles;
        this.fileGroups += parent.fileGroups;
//        parent.fileGroups.each { FileGroup fg -> fg.remove(parent); }
        this.valid = parent.valid;
        this.listOfParentJobs += parent.listOfParentJobs;
        this.listOfParentProcesses += parent.listOfParentProcesses;
        this.isTemporaryFile = parent.isTemporaryFile;
        this.isReadable = parent.isReadable;
        this.isSourceFile = parent.isSourceFile;
        this.appliedFilenamePattern = parent.appliedFilenamePattern;
    }

    private void determineFileValidityBasedOnContextLevel(ExecutionContext executionContext) {
        switch (executionContext.getExecutionContextLevel()) {
            case ExecutionContextLevel.QUERY_STATUS:
                valid = !checkFileValidity();
                break;
            case ExecutionContextLevel.RERUN:
                valid = !checkFileValidity();
                break;
            case ExecutionContextLevel.TESTRERUN:
                valid = !checkFileValidity();
                break;
            case ExecutionContextLevel.RUN:
                valid = true;
                break;
        }
    }

    public final void addFileGroup(FileGroup fg) {
        this.fileGroups.add(fg);
    }

    public final List<FileGroup> getFileGroups() {
        return this.fileGroups;
    }

    public final List<BaseFile> getNeighbourFiles() {
        List<BaseFile> neighbourFiles = new LinkedList<BaseFile>();
        for (FileGroup fg : fileGroups) {
            neighbourFiles.addAll(fg.getFilesInGroup());
        }
        return neighbourFiles;
    }

    /**
     * Run the default operations for a file.
     */
    @Override
    public void runDefaultOperations() {
    }

    /**
     * This method can be overridden and is called on each isValid check
     *
     * @return
     */
    public boolean checkFileValidity() {
        return true;
    }

    public boolean isSourceFile() {
        return isSourceFile;
    }

    public void setAsSourceFile() {
        isSourceFile = true;
    }

    /**
     * Check if the file exists on disk. The query might be buffered, so don't
     * delete the files after the check!
     *
     * @return
     */
    public final boolean isFileReadable() {
        if (isReadable == null)
            isReadable = FileSystemAccessProvider.getInstance().isReadable(this);
        return isReadable;
    }

    public final void isFileReadable(boolean readable) {
        isReadable = readable;
    }

    /**
     * As the isFileValid method can take a while the value is cached.
     * <p>
     * The value will not change during one run, so caching is ok.
     */
    private Boolean _cacheIsFileValid = null;

    /**
     * Combines isFileReadable and isFileValid.
     * <p>
     * A file is valid if it exists, if it was not created recently (within this
     * run) and if it could be validated (i.e. by its size or content, no
     * lengthy operation!).
     *
     * @return
     */
    public final boolean isFileValid() {
        //TODO Check for file sizes < 200Byte!
        if (_cacheIsFileValid == null) {
            _cacheIsFileValid = getExecutionContext().getRuntimeService().isFileValid(this);
        }
        return _cacheIsFileValid;
    }

    public final void setFileIsValid() {
        _cacheIsFileValid = true;
    }

    public FS getFileStage() {
        return fileStageSettings;
    }

    public File getPath() {
        return path;
    }

    /**
     * (Re-)Sets the path for this file object.
     *
     * @param path
     */
    public void setPath(File path) {
        this.path = path;
    }

    public String getAbsolutePath() {
        return path.getAbsolutePath();
    }

    public String getContainingFolder() {
        return path.getParent();
    }

    /**
     * Returns a copy of the list of parent files.
     *
     * @return
     */
    public List<BaseFile> getParentFiles() {
        return new LinkedList<BaseFile>(parentFiles);
    }

    public void setParentFiles(List<BaseFile> parentfiles) {
        setParentFiles(parentfiles, false);
    }

    public void setParentFiles(List<BaseFile> parentfiles, boolean resetFilename) {
        this.parentFiles.clear();
        this.parentFiles.addAll(parentfiles);
        if (resetFilename) {
            File temp = path;
            Tuple2<File, FilenamePattern> fnresult = getFilename(this);
            this.path = fnresult?.x;
            this.appliedFilenamePattern = fnresult?.y;
            if (path == null) {
                //TODO Also this should be handled somehow else. It is occurring much too often.
//                getExecutionContext().addErrorEntry(ExecutionContextError.EXECUTION_FILECREATION_PATH_NOTSET.expand("Setting a new filename with parent files returned null for " + this.getClass().getName() + " returning to " + (temp != null ? temp.getAbsolutePath() : "null"), Level.WARNING));
                path = temp;
            }
        }
    }

    private BaseFile findAncestorOfType(Class clzType) {
        for (BaseFile bf : parentFiles) {
            if (bf.getClass() == clzType) {
                return bf;
            } else {
                return bf.findAncestorOfType(clzType);
            }
        }
        return null;
    }

    /**
     * Tell Roddy that this file is temporary and can be deleted if dependent files are valid.
     */
    public void setAsTemporaryFile() {
        this.isTemporaryFile = true;
    }

    /**
     * Query if this file is temporary
     *
     * @return
     */
    public boolean isTemporaryFile() {
        return isTemporaryFile;
    }

    /**
     * Call this method after you created a basefile to reset the path with the passed selectionTag.
     * getFilename() will be rerun
     *
     * @param selectionTag
     */
    public void overrideFilenameUsingSelectionTag(String selectionTag) {
        Tuple2<File, FilenamePattern> fnresult = getFilename(this, selectionTag);
        File _path = fnresult.x;
        this.appliedFilenamePattern = fnresult.y;
        this.setPath(_path);
    }

    /**
     * Convenience method which calls getFilename with the default selection tag.
     *
     * @param baseFile
     * @return
     */
    public static Tuple2<File, FilenamePattern> getFilename(BaseFile baseFile) {
        return getFilename(baseFile, FilenamePattern.DEFAULT_SELECTION_TAG);
    }

    private static Map<Class, LinkedHashMap<FilenamePatternDependency, LinkedList<FilenamePattern>>> _classPatternsCache = new LinkedHashMap<>();

    private static LinkedHashMap<FilenamePatternDependency, LinkedList<FilenamePattern>> loadAVailableFilenamePatterns(BaseFile baseFile, ExecutionContext context) {
        Configuration cfg = context.getConfiguration();
        LinkedHashMap<FilenamePatternDependency, LinkedList<FilenamePattern>> availablePatterns = new LinkedHashMap<>();
        if (!_classPatternsCache.containsKey(baseFile.getClass())) {
            availablePatterns = new LinkedHashMap<>();
            _classPatternsCache.put(baseFile.getClass(), availablePatterns);
            FilenamePatternDependency.values().each { FilenamePatternDependency it -> availablePatterns[it] = new LinkedList<FilenamePattern>() }
            for (FilenamePattern fp : cfg.getFilenamePatterns().getAllValuesAsList()) {
                if (fp.getCls() == baseFile.getClass()) {
                    availablePatterns[fp.getFilenamePatternDependency()] << fp;
                }
            }
        } else {
            availablePatterns = _classPatternsCache.get(baseFile.getClass());
        }
        return availablePatterns;
    }

    /**
     * Looks for the right filename pattern for the new file.
     * <p>
     * Find the correct pattern:
     * Look if filename patterns for this class are available
     * If not throw an exception
     * Look if there is only one available: Easy, use this
     * Else priority is onMethod, sourcefile, filestage
     *
     * @param baseFile
     * @return
     */

    public static Tuple2<File, FilenamePattern> getFilename(BaseFile baseFile, String selectionTag) {
        if (!selectionTag)
            selectionTag = "default";
        Tuple2<File, FilenamePattern> patternResult = new Tuple2<>(null, null);
        try {

            //Find the correct pattern:
            // Look if filename patterns for this class are available
            // If not throw an exception
            // Look if there is only one available: Easy, use this
            // onMethod, sourcefile, filestage
            ExecutionContext context = baseFile.getExecutionContext();
            LinkedHashMap<FilenamePatternDependency, LinkedList<FilenamePattern>> availablePatterns = loadAVailableFilenamePatterns(baseFile, context);

            patternResult = findFilenameFromOnMethodPatterns(baseFile, availablePatterns[FilenamePatternDependency.onMethod], selectionTag) ?:
                    findFilenameFromOnToolIDPatterns(baseFile, availablePatterns[FilenamePatternDependency.onTool], selectionTag) ?:
                            findFilenameFromSourcefilePatterns(baseFile, availablePatterns[FilenamePatternDependency.derivedFrom], selectionTag) ?:
                                    findFilenameFromGenericPatterns(baseFile, availablePatterns[FilenamePatternDependency.FileStage], selectionTag);

            // Do some further checks for selection tags.
            if (selectionTag.equals("default")) {
                if (!patternResult || patternResult.x == null) {
                    throw new RuntimeException("There is no valid filename pattern for this file: " + baseFile);
                } else {
                    //Check if the path exists and create it if necessary.
                    if (context.getExecutionContextLevel().isOrWasAllowedToSubmitJobs && !FileSystemAccessProvider.getInstance().checkDirectory(patternResult.x.getParentFile(), context, true)) {
                        throw new RuntimeException("Output path could not be created for file: " + baseFile);
                    }
                }
            } else {

                if (patternResult.x == null) {
                    throw new RuntimeException("There is no valid filename pattern for this file: " + baseFile);
                }

                //Check if the path exists and create it if necessary.
                if (context.getExecutionContextLevel().isOrWasAllowedToSubmitJobs && !FileSystemAccessProvider.getInstance().checkDirectory(patternResult.x.getParentFile(), context, true)) {
                    throw new RuntimeException("Output path could not be created for file: " + baseFile);
                }
            }
        } catch (Exception ex) {
            if (Roddy.getFeatureToggleValue(AvailableFeatureToggles.AutoFilenames)) {
                // In case that we did not find a pattern and therefore could not create a filename, we will now apply an automatic filename.

                // This might work for GenericMethod calls! But surely not for other ones...

                // The problem is, that we need to get some sort of jobname and / or an internal job id. But! Jobs normally get created after the files.
                // Then files get assigned to the job on its creation. However, GenericMethod calls, at least have some info about the running script.
                // Manual calls might miss this information and it will possibly get ugly to extract info on this.
            } else {
                throw ex;
            }
        } finally {
            return patternResult;
        }
    }

    private static Tuple2<File, FilenamePattern> findFilenameFromGenericPatterns(BaseFile baseFile, LinkedList<FilenamePattern> availablePatterns, String selectionTag) {
        Tuple2<File, FilenamePattern> result = null;
        File filename = null;
        FilenamePattern appliedPattern = null;

        if (!availablePatterns) return result;

        for (FilenamePattern _fp : availablePatterns) {
            FileStageFilenamePattern fp = _fp as FileStageFilenamePattern;
            if ((fp.getFileStage() == FileStage.GENERIC || fp.getFileStage() == baseFile.getFileStage().getStage()) && fp.getSelectionTag().equals(selectionTag)) {
                filename = new File(fp.apply(baseFile));
                appliedPattern = fp;
                break;
            }
        }

        if (!filename || !appliedPattern) return null;
        return new Tuple2<>(filename, appliedPattern);
    }

    private static Tuple2<File, FilenamePattern> findFilenameFromSourcefilePatterns(BaseFile baseFile, LinkedList<FilenamePattern> availablePatterns, String selectionTag) {
        Tuple2<File, FilenamePattern> result = null;
        File filename = null;
        FilenamePattern appliedPattern = null;

        if (availablePatterns && baseFile.parentFiles.size() > 0) {
            for (FilenamePattern fp : availablePatterns) {
                for (BaseFile bf : (List<BaseFile>) baseFile.parentFiles) {
                    if (bf.getClass() == ((DerivedFromFilenamePattern) fp).getDerivedFromCls() && fp.getSelectionTag().equals(selectionTag)) {
                        if (fp.doesAcceptFileArrays()) {
                            filename = new File(fp.apply((BaseFile[]) baseFile.parentFiles.toArray(new BaseFile[0])));
                        } else {
                            filename = new File(fp.apply((BaseFile) baseFile.parentFiles.get(0)));
                        }
                        appliedPattern = fp;
                        break;
                    }
                }
            }
        }
        if (!filename || !appliedPattern) return null;
        return new Tuple2<>(filename, appliedPattern);
    }

    /**
     * Tries to find a filename from onMethod patterns.
     * <p>
     * If none was found, null will be returned.
     *
     * @param baseFile
     * @param availablePatterns
     * @param selectionTag
     * @return
     */
    private static Tuple2<File, FilenamePattern> findFilenameFromOnMethodPatterns(BaseFile baseFile, LinkedList<FilenamePattern> availablePatterns, String selectionTag) {
        Tuple2<File, FilenamePattern> result = null;
        File filename = null;
        FilenamePattern appliedPattern = null;

        //Find the called basefile method, if on_method patterns are available.
        if (!availablePatterns) return result;

        List<StackTraceElement> steByMethod = getAndFilterStackElementsToWorkflowInstance();

        traceLoop:
        for (StackTraceElement ste : steByMethod) {
            for (FilenamePattern _fp : availablePatterns) {
                OnMethodFilenamePattern fp = _fp as OnMethodFilenamePattern;

                String cmName = fp.getCalledMethodsName().getName();
                String cmClass = fp.getCalledMethodsClass().getName();
                if (cmName.equals(ste.getMethodName())
                        && cmClass.equals(ste.getClassName())
                        && fp.getSelectionTag().equals(selectionTag)) {
                    //OOOOOH LOOK try this!
                    File tempFN = new File(fp.apply(baseFile));
                    appliedPattern = fp;
                    filename = tempFN;
                    break traceLoop;
                }
            }
        }
        if (!filename || !appliedPattern) return null;
        return new Tuple2<>(filename, appliedPattern);
    }

    /**
     * TODO The current premise for this method is, that it will only work if GenericMethod was used to run it. The current process / context / run needs an extension, so that we have something like
     * a state machine where the developer can always query the current context and store context specific things. Maybe this can be bound to the current Thread or so... In this case, a Thread could also
     * have a link to several context objects (which are called one after another.). The state machine will always know the current active context, the current active job and so on. It might be complicated
     * but maybe worth it. For now, let's just get the job id from the Generic Method call.
     *
     * @param baseFile
     * @param availablePatterns
     * @param selectionTag
     * @return
     */
    private static Tuple2<File, FilenamePattern> findFilenameFromOnToolIDPatterns(BaseFile baseFile, LinkedList<FilenamePattern> availablePatterns, String selectionTag) {
        Tuple2<File, FilenamePattern> result = null;
        File filename = null;
        FilenamePattern appliedPattern = null;

        //Find the called basefile method, if on_method patterns are available.
        if (!availablePatterns) return result;

        String id = baseFile.getExecutionContext().getCurrentExecutedTool().getID();
        for (FilenamePattern _fp : availablePatterns) {
            OnToolFilenamePattern fp = _fp as OnToolFilenamePattern;
            if (fp.getCalledScriptID().equals(id)) {
                appliedPattern = fp;
                filename = new File(fp.apply(baseFile));
                break;
            }
        }
        if (!filename || !appliedPattern) return null;
        return new Tuple2<>(filename, appliedPattern);
    }

    /**
     * As the method name states, the method fetches a filtered list of all stack trace elements until the workflows execute method.
     * @return
     */
    private static List<StackTraceElement> getAndFilterStackElementsToWorkflowInstance() {
        String calledBaseFileMethodName = null;
        //Walk through the stack to get the method.
        List<StackTraceElement> steByMethod = new LinkedList<>();
        boolean constructorPassed = false;
        for (StackTraceElement ste : Thread.currentThread().getStackTrace()) {
            try {
                //Skip several methods

                String methodName = ste.getMethodName();
                if (methodName.equals("<init>")
                        || methodName.endsWith("getFilename")
                        || methodName.endsWith("getStackTrace")
                ) {
                    continue;
                }
                //Abort when the workflows execute method is called.
                if ((ste.getClassName().equals(ExecutionContext.class.getName())
                        || Workflow.class.isAssignableFrom(LibrariesFactory.getGroovyClassLoader().loadClass(ste.getClassName())))
                        && methodName.equals("execute"))
                    break;

                //In all other cases add the method.
//                    if (constructorPassed)
                steByMethod.add(ste);
            } catch (Exception ex) {

            }
        }
        return steByMethod;
    }

    /**
     * Adds a job to the list of the parent jobs.
     *
     * @param job
     */
    public void addCreatingJob(Job job) {
        listOfParentJobs.add(job);
    }

    /**
     * Adds a process to the end of the list of parent processes.
     *
     * @param process
     */
    public void addCreatingProcess(ExecutionContext process) {
        listOfParentProcesses.add(process);
    }

    /**
     * Returns a copy of the list of parent jobs.
     * last entry is the most current entry.
     *
     * @return
     */
    public List<Job> getListOfParentJobs() {
        return new LinkedList<Job>(listOfParentJobs);
    }

    /**
     * Returns a copy of the list of parent processes.
     * last entry is the most current entry.
     *
     * @return
     */
    public List<ExecutionContext> getListOfParentProcesses() {
        return new LinkedList<ExecutionContext>(listOfParentProcesses);
    }

    @Override
    public String toString() {
        return "BaseFile of type " + getClass().getName() + " with path " + path;
    }
}
