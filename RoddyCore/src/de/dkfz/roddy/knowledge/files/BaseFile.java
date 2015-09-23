package de.dkfz.roddy.knowledge.files;

import de.dkfz.roddy.AvailableFeatureToggles;
import de.dkfz.roddy.Roddy;
import de.dkfz.roddy.config.Configuration;
import de.dkfz.roddy.config.FilenamePattern;
import de.dkfz.roddy.core.ExecutionContext;
import de.dkfz.roddy.core.ExecutionContextLevel;
import de.dkfz.roddy.core.Workflow;
import de.dkfz.roddy.execution.io.fs.FileSystemInfoProvider;
import de.dkfz.roddy.execution.jobs.Job;
import de.dkfz.roddy.execution.jobs.JobResult;
import de.dkfz.roddy.plugins.LibrariesFactory;
import org.reflections.Reflections;
import de.dkfz.roddy.tools.Tuple2;
import de.dkfz.roddy.tools.Tuple3;

import java.io.File;
import java.util.*;
import java.util.logging.Level;

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
public abstract class BaseFile<FS extends FileStageSettings> extends FileObject {

    public static final String ON_METHOD = "onMethod";
    public static final String SOURCEFILE = "sourcefile";
    public static final String FILESTAGE = "filestage";

    protected File path;

    protected final List<BaseFile> parentFiles = new LinkedList<>();

    /**
     * A BaseFile object can be grouped into one or more groups.
     */
    protected final List<FileGroup> fileGroups = new LinkedList<FileGroup>();

    protected final FS fileStageSettings;

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

    private BaseFile(ExecutionContext executionContext, FS settings) {
        super(executionContext);
        this.fileStageSettings = settings;
        executionContext.addFile(this);
        switch (executionContext.getExecutionContextLevel()) {
            case QUERY_STATUS:
                valid = !checkFileValidity();
                break;
            case RERUN:
                valid = !checkFileValidity();
                break;
            case TESTRERUN:
                valid = !checkFileValidity();
                break;
            case RUN:
                valid = true;
                break;
        }
    }

    public BaseFile(FileGroup parentFileGroup, FS settings, JobResult jobResult) {
        this(parentFileGroup, settings);
        this.setCreatingJobsResult(jobResult);
    }

    /**
     * Used i.e. for rerunning a pipeline.
     * <p>
     * Mark a file as dirty so that the program knows, that it was created
     * freshly or modified. Following files can use this to see that they should
     * be recreated as well, i.e. after merging.
     */
    public BaseFile(FileGroup parentFileGroup, FS settings) {
        this(parentFileGroup.getExecutionContext(), settings);
        parentFiles.addAll(parentFileGroup.getFilesInGroup());
        Tuple2<File, FilenamePattern> fnresult = getFilename(this);
        this.path = fnresult.x;
        this.appliedFilenamePattern = fnresult.y;
    }

    public BaseFile(BaseFile parentFile, FS fileStage, JobResult jobResult) {
        this(parentFile, fileStage);
        this.setCreatingJobsResult(jobResult);
    }

    /**
     * Used i.e. for rerunning a pipeline.
     * <p>
     * Mark a file as dirty so that the program knows, that it was created
     * freshly or modified. Following files can use this to see that they should
     * be recreated as well, i.e. after merging.
     */
    public BaseFile(BaseFile parentFile) {
        this(parentFile.getExecutionContext(), (FS) parentFile.fileStageSettings);
        parentFiles.add(parentFile);
        Tuple2<File, FilenamePattern> fnresult = getFilename(this);
        this.path = fnresult.x;
        this.appliedFilenamePattern = fnresult.y;
    }


    public BaseFile(BaseFile parentFile, FS settings) {
        this(parentFile.getExecutionContext(), settings);
        parentFiles.add(parentFile);
        Tuple2<File, FilenamePattern> fnresult = getFilename(this);
        this.path = fnresult.x;
        this.appliedFilenamePattern = fnresult.y;
    }

    public BaseFile(File path, ExecutionContext executionContext, JobResult jobResult, List<BaseFile> parentFiles, FS settings) {
        this(executionContext, settings);
        this.path = path;
        this.setCreatingJobsResult(jobResult);
        if (parentFiles != null) {
            this.parentFiles.addAll(parentFiles);
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
            isReadable = FileSystemInfoProvider.getInstance().isReadable(this);
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
            this.path = fnresult.x;
            this.appliedFilenamePattern = fnresult.y;
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

    private static Map<String, Reflections> _reflectionsCache = new LinkedHashMap<>();
    private static Map<Class, LinkedHashMap<String, LinkedList<FilenamePattern>>> _classPatternsCache = new LinkedHashMap<>();

    private static LinkedHashMap<String, LinkedList<FilenamePattern>> loadAVailableFilenamePatterns(BaseFile baseFile, ExecutionContext context) {
        Configuration cfg = context.getConfiguration();
        LinkedHashMap<String, LinkedList<FilenamePattern>> availablePatterns = new LinkedHashMap<>();
        if (!_classPatternsCache.containsKey(baseFile.getClass())) {
            availablePatterns = new LinkedHashMap<>();
            _classPatternsCache.put(baseFile.getClass(), availablePatterns);
            availablePatterns.put(ON_METHOD, new LinkedList<>());
            availablePatterns.put(SOURCEFILE, new LinkedList<>());
            availablePatterns.put(FILESTAGE, new LinkedList<>());
            for (FilenamePattern fp : cfg.getFilenamePatterns().getAllValuesAsList()) {
                if (fp.getCls() == baseFile.getClass()) {
                    if (fp.getFilenamePatternDependency() == FilenamePattern.FilenamePatternDependency.MethodName)
                        availablePatterns.get(ON_METHOD).add(fp);
                    else if (fp.getFilenamePatternDependency() == FilenamePattern.FilenamePatternDependency.SourceClass)
                        availablePatterns.get(SOURCEFILE).add(fp);
                    else if (fp.getFilenamePatternDependency() == FilenamePattern.FilenamePatternDependency.FileStage)
                        availablePatterns.get(FILESTAGE).add(fp);
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
        Tuple2<File, FilenamePattern> patternResult = new Tuple2<>(null, null);
        try {

            //Find the correct pattern:
            // Look if filename patterns for this class are available
            // If not throw an exception
            // Look if there is only one available: Easy, use this
            // onMethod, sourcefile, filestage
            ExecutionContext context = baseFile.getExecutionContext();
            LinkedHashMap<String, LinkedList<FilenamePattern>> availablePatterns = loadAVailableFilenamePatterns(baseFile, context);

            patternResult = findFilenameFromOnMethodPatterns(baseFile, availablePatterns, selectionTag);

            if (patternResult.x == null)
                patternResult = findFilenameFromSourcefilePatterns(baseFile, availablePatterns, selectionTag);


            if (patternResult.x == null)
                patternResult = findFilenameFromGenericPatterns(baseFile, availablePatterns, selectionTag);

            // Do some further checks for selection tags.
            if (selectionTag.equals("default")) {
                if (patternResult.x == null) {
                    throw new RuntimeException("There is no valid filename pattern for this file: " + baseFile);
                } else {
                    //Check if the path exists and create it if necessary.
                    if (context.getExecutionContextLevel().isOrWasAllowedToSubmitJobs && !FileSystemInfoProvider.getInstance().checkDirectory(patternResult.x.getParentFile(), context, true)) {
                        throw new RuntimeException("Output path could not be created for file: " + baseFile);
                    }
                }
            } else {

                if (patternResult.x == null) {
                    throw new RuntimeException("There is no valid filename pattern for this file: " + baseFile);
                }

                //Check if the path exists and create it if necessary.
                if (context.getExecutionContextLevel().isOrWasAllowedToSubmitJobs && !FileSystemInfoProvider.getInstance().checkDirectory(patternResult.x.getParentFile(), context, true)) {
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

    private static Tuple2<File, FilenamePattern> findFilenameFromGenericPatterns(BaseFile baseFile, LinkedHashMap<String, LinkedList<FilenamePattern>> availablePatterns, String selectionTag) {
        Tuple2<File, FilenamePattern> result = new Tuple2<>(null,null);
        File filename = null;
        FilenamePattern appliedPattern = null;

        if (availablePatterns.get(FILESTAGE).size() > 0) {
            for (FilenamePattern fp : availablePatterns.get(FILESTAGE)) {
                if ((fp.getFileStage() == FileStage.GENERIC || fp.getFileStage() == baseFile.getFileStage().getStage()) && fp.getSelectionTag().equals(selectionTag)) {
                    filename = new File(fp.apply(baseFile));
                    appliedPattern = fp;
                    break;
                }
            }
        }
        result = new Tuple2<>(filename, appliedPattern);
        return result;
    }

    private static Tuple2<File, FilenamePattern> findFilenameFromSourcefilePatterns(BaseFile baseFile, LinkedHashMap<String, LinkedList<FilenamePattern>> availablePatterns, String selectionTag) {
        Tuple2<File, FilenamePattern> result = new Tuple2<>(null,null);
        File filename = null;
        FilenamePattern appliedPattern = null;

        if (availablePatterns.get(SOURCEFILE).size() > 0 && baseFile.parentFiles.size() > 0) {
            for (FilenamePattern fp : availablePatterns.get(SOURCEFILE)) {
                for (BaseFile bf : (List<BaseFile>) baseFile.parentFiles) {
                    if (bf.getClass() == fp.getDerivedFromCls() && fp.getSelectionTag().equals(selectionTag)) {
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
        result = new Tuple2<>(filename, appliedPattern);
        return result;
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
    private static Tuple2<File, FilenamePattern> findFilenameFromOnMethodPatterns(BaseFile baseFile, LinkedHashMap<String, LinkedList<FilenamePattern>> availablePatterns, String selectionTag) {
        Tuple2<File, FilenamePattern> result = new Tuple2<>(null,null);
        File filename = null;
        FilenamePattern appliedPattern = null;

        //Find the called basefile method, if on_method patterns are available.
        if (availablePatterns.get(ON_METHOD).size() <= 0) {
            return result;
        }
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

        traceLoop:
        for (StackTraceElement ste : steByMethod) {
            for (FilenamePattern fp : availablePatterns.get(ON_METHOD)) {
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
        result = new Tuple2<>(filename, appliedPattern);
        return result;
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
