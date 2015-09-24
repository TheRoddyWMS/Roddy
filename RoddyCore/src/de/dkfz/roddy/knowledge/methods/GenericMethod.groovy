package de.dkfz.roddy.knowledge.methods

import de.dkfz.roddy.AvailableFeatureToggles
import de.dkfz.roddy.config.*;
import de.dkfz.roddy.Roddy
import de.dkfz.roddy.execution.jobs.CommandFactory
import de.dkfz.roddy.tools.*
import de.dkfz.roddy.config.Configuration
import de.dkfz.roddy.config.ConfigurationConstants
import de.dkfz.roddy.config.ToolEntry
import de.dkfz.roddy.core.ExecutionContext
import de.dkfz.roddy.core.ExecutionContextError
import de.dkfz.roddy.execution.jobs.Job
import de.dkfz.roddy.execution.jobs.JobResult
import de.dkfz.roddy.knowledge.files.AbstractFileObjectTuple
import de.dkfz.roddy.knowledge.files.BaseFile
import de.dkfz.roddy.knowledge.files.FileGroup
import de.dkfz.roddy.knowledge.files.FileObject
import de.dkfz.roddy.knowledge.files.IndexedFileObjects
import de.dkfz.roddy.knowledge.files.FileObjectTupleFactory

import java.lang.reflect.Constructor
import java.util.logging.Level
import java.util.logging.Logger

import static de.dkfz.roddy.config.FilenamePattern.$_JOBPARAMETER
import static de.dkfz.roddy.execution.jobs.JobConstants.*
import static de.dkfz.roddy.Constants.NO_VALUE;

/**
 * Class for generic, configurable methods
 * This class is very magic and does quite a lot of stuff.
 * Unfortunately it is also very heavy and bulky... So be advised to take your time when digging through this.
 * We have a
 * TODO to make this class more readable and usable
 * open
 *
 * The class is externally called via two static methods. Internally, for convenience, an object is created
 * and used for the furhter process.
 */
@groovy.transform.CompileStatic
class GenericMethod {

    private static final LoggerWrapper logger = LoggerWrapper.getLogger(GenericMethod.class.getName());

    /**
     * This method is basically a wrapper around callGenericToolArray.
     * Both methods were so similar, that it made no sense to keep both.
     * callGenericToolArray can handle single job calls very well.
     * @param toolName The tool id to call
     * @param input The basic input object TODO should be extended or changed to handle FileGroups as well.
     * @param additionalInput Any additional input to the job. The input must fit the tool i/o specs in your xml file.
     * @return
     */
    public static <F extends FileObject> F callGenericTool(String toolName, BaseFile input, Object... additionalInput) {
        F result = callGenericToolArray(toolName, null, input, additionalInput);
        if (result == null) {
            def errMsg = "The job callGenericTool(${toolName}, ...) returned null."
            logger.warning(errMsg);
            input.getExecutionContext().addErrorEntry(ExecutionContextError.EXECUTION_JOBFAILED.expand(errMsg))
        }
        return result;
    }

    /**
     * This is Roddys most magic method!
     * It uses all the input parameters and the tool description and magically assembles a full job object.
     * The generated tool object is run() at the end.
     *
     * @param toolName The tool id to call
     * @param arrayIndices If it is an array, we need indices
     * @param input The basic input object TODO should be extended or changed to handle FileGroups as well.
     * @param additionalInput Any additional input to the job. The input must fit the tool i/o specs in your xml file.
     * @return
     */
    public static <F extends FileObject> F callGenericToolArray(String toolName, List<String> arrayIndices, BaseFile input, Object... additionalInput) {
        new GenericMethod(toolName, arrayIndices, input, additionalInput)._callGenericToolArray();
    }


    private final ExecutionContext context
    private final Configuration configuration
    private final String toolName
    private final ToolEntry calledTool
    private final List<String> arrayIndices
    private final BaseFile inputFile
    private final Object[] additionalInput
    private final List<FileObject> allInputValues = []
    private final List<BaseFile> allInputFiles = [];
    private final Map<String, Object> parameters = [:]
    private final List<FileObject> allCreatedObjects = [];

    private GenericMethod(String toolName, List<String> arrayIndices, BaseFile inputFile, Object... additionalInput) {
        this.additionalInput = additionalInput
        this.inputFile = inputFile
        this.allInputValues << inputFile;
        this.arrayIndices = arrayIndices
        this.toolName = toolName
        this.context = inputFile.getExecutionContext();
        this.configuration = context.getConfiguration();
        this.calledTool = configuration.getTools().getValue(toolName);
    }

    private <F extends FileObject> F _callGenericToolArray() {

        // Check if method may be executed
        if (!calledTool.isToolGeneric()) {
            logger.severe("Tried to call a non generic tool via the generic call method");
            throw new RuntimeException("Not able to context tool " + toolName);
        }

        // Assemble initial parameters
        if (toolName) {
            parameters[PRM_TOOLS_DIR] = configuration.getProcessingToolPath(context, toolName).getParent();
            parameters[PRM_TOOL_ID] = toolName;
        }

        for (Object entry in additionalInput) {
            if (entry instanceof BaseFile)
                allInputValues << (BaseFile) entry;
            else if (entry instanceof FileGroup) {
                //Take a group and store all files in that group.
                allInputValues << (FileGroup) entry;

            } else {
                String[] split = entry.toString().split("=");
                if (split.length != 2)
                    throw new RuntimeException("Not able to convert entry ${entry.toString()} to parameter.")
                parameters[split[0]] = split[1];
            }
        }

        for (FileObject fo in allInputValues) {
            if (fo instanceof BaseFile)
                allInputFiles << (BaseFile) fo;
            else if (fo instanceof FileGroup)
                allInputFiles.addAll(((FileGroup) fo).getFilesInGroup());
        }

        for (int i = 0; i < calledTool.getInputParameters(context).size(); i++) {
            ToolEntry.ToolParameter toolParameter = calledTool.getInputParameters(context)[i];
            if (toolParameter instanceof ToolEntry.ToolFileParameter) {
                ToolEntry.ToolFileParameter _tp = (ToolEntry.ToolFileParameter) toolParameter;
                //TODO Check if input and output parameters match and also check for array indices and item count. Throw a proper error message.
                if (!allInputValues[i].class == _tp.fileClass)
                    logger.severe("Class mismatch for " + allInputValues[i] + " should be of class " + _tp.fileClass);
                if (_tp.scriptParameterName) {
//                    String path = ((BaseFile) allInputValues[i])?.path?.absolutePath;
//                    if (!path) {
//                        context.addErrorEntry(ExecutionContextError.EXECUTION_PARAMETER_ISNULL_NOTUSABLE.expand("The parameter ${_tp.scriptParameterName} has no valid value and will be set to <NO_VALUE>."));
//                        path = NO_VALUE;
//                    }
                    parameters[_tp.scriptParameterName] = ((BaseFile) allInputValues[i]);

                }
                _tp.constraints.each {
                    ToolEntry.ToolConstraint constraint ->
                        constraint.apply(inputFile);
                }
            } else if (toolParameter instanceof ToolEntry.ToolTupleParameter) {
                ToolEntry.ToolTupleParameter _tp = (ToolEntry.ToolTupleParameter) toolParameter;
                logger.severe("Tuples must not be used as an input parameter for tool ${toolName}.l");
            } else if (toolParameter instanceof ToolEntry.ToolFileGroupParameter) {
                ToolEntry.ToolFileGroupParameter _tp = (ToolEntry.ToolFileGroupParameter) toolParameter;
                if (!allInputValues[i].class == _tp.groupClass)
                    logger.severe("Class mismatch for ${allInputValues[i]} should be of class ${_tp.groupClass}");
                if (_tp.passOptions == ToolEntry.ToolFileGroupParameter.PassOptions.parameters) {
                    int cnt = 0;
                    for (BaseFile bf in (List<BaseFile>) ((FileGroup) allInputValues[i]).getFilesInGroup()) {
                        parameters[_tp.scriptParameterName + "_" + cnt] = bf;
                        cnt++;
                    }
                } else { //Arrays
                    int cnt = 0;
                    parameters[_tp.scriptParameterName] = ((FileGroup) allInputValues[i]).getFilesInGroup();//paths;
                }
            }
        }

        for (int i = 0; i < calledTool.getOutputParameters(context).size(); i++) {
            ToolEntry.ToolParameter toolParameter = calledTool.getOutputParameters(context)[i];
            if (toolParameter instanceof ToolEntry.ToolFileParameter) {
                ToolEntry.ToolFileParameter _tp = (ToolEntry.ToolFileParameter) toolParameter;
                for (ToolEntry.ToolConstraint constraint in _tp.constraints) {
                    constraint.apply(inputFile);
                }
            }
        }

        F outputObject = null;

        outputObject = createOutputObject();
        allCreatedObjects << outputObject;

        JobResult jobResult = null;
        //Verifiable output files:
        List<BaseFile> filesToVerify = [];
        allCreatedObjects.each { FileObject fo -> if (fo instanceof BaseFile && !((BaseFile) fo).isTemporaryFile()) filesToVerify << (BaseFile) fo; }

        if (arrayIndices != null) {
            jobResult = new Job(context, context.createJobName(inputFile, toolName), toolName, arrayIndices, parameters, allInputFiles, filesToVerify).run();

            Map<String, FileObject> outputObjectsByArrayIndex = [:];
            IndexedFileObjects indexedFileObjects = new IndexedFileObjects(arrayIndices, outputObjectsByArrayIndex, context);
            // Run array job and afterwards create output files for all sub jobs. The values in filesToVerify will be used and the path names will be corrected.
//            allCreatedObjects.removeAll(filesToVerify);
            int i = 1;
            for (String arrayIndex in arrayIndices) {
                List<FileObject> newObjects = [];
                outputObjectsByArrayIndex[arrayIndex] = createOutputObject(arrayIndex);
                JobResult jr = CommandFactory.getInstance().convertToArrayResult(jobResult.job, jobResult, i++);
                for (FileObject fo : newObjects) {
                    fo.setCreatingJobsResult(jr);
                }
            }
            return indexedFileObjects;
        }
        jobResult = new Job(context, context.createJobName(inputFile, toolName), toolName, parameters, allInputFiles, filesToVerify).run();

        try {
            if (allCreatedObjects) {
                for (FileObject fo in allCreatedObjects) {
                    if (fo == null)
                        continue;
                    fo.setCreatingJobsResult(jobResult);
                }
            }
        } catch (Exception ex) {
            logger.severe(ex.toString());
            logger.severe(RoddyIOHelperMethods.getStackTraceAsString(ex));
        }
        return outputObject;
    }

    private <F extends FileObject> F createOutputObject(String arrayIndex = null) {
        F outputObject = null;
        if (calledTool.getOutputParameters(inputFile.getExecutionContext()).size() == 1) {
            ToolEntry.ToolParameter tparm = calledTool.getOutputParameters(inputFile.getExecutionContext())[0];
            if (tparm instanceof ToolEntry.ToolFileParameter) {
                ToolEntry.ToolFileParameter fileParameter = tparm as ToolEntry.ToolFileParameter;
                BaseFile bf = toolFileParameterToBaseFile(fileParameter, inputFile, allInputFiles)
                for (ToolEntry.ToolFileParameter childFileParameter in fileParameter.getChildFiles()) {
                    if (childFileParameter.parentVariable == null) {
                        continue;
                    }
                    java.lang.reflect.Field _field = null;
                    java.lang.reflect.Method _method = null;
                    try {
                        _field = bf.getClass().getField(childFileParameter.parentVariable);
                    } catch (Exception ex) {
                    }
                    try {
                        String setterMethod = "set" + childFileParameter.parentVariable[0].toUpperCase() + childFileParameter.parentVariable[1..-1];
                        _method = bf.getClass().getMethod(setterMethod, childFileParameter.fileClass);
                    } catch (Exception ex) {
                    }
                    if (_field == null && _method == null) {
                        try {
                            inputFile.getExecutionContext().addErrorEntry(ExecutionContextError.EXECUTION_FILECREATION_FIELDINACCESSIBLE.expand("Class ${bf.getClass().getName()} field ${childFileParameter.parentVariable}"));
                        } catch (Exception ex) {
                        }
                        continue;
                    }
                    BaseFile childFile = toolFileParameterToBaseFile(childFileParameter, bf, [bf]);
                    allCreatedObjects << childFile;
                    if (_field)
                        _field.set(bf, childFile);
                    else
                        _method.invoke(bf, childFile);
                }
                outputObject = fileParameter.fileClass.cast(bf) as F;
            } else if (tparm instanceof ToolEntry.ToolTupleParameter) {
                //TODO Auto recognize tuples?
                ToolEntry.ToolTupleParameter tfg = tparm as ToolEntry.ToolTupleParameter;
                List<FileObject> filesInTuple = [];
                for (ToolEntry.ToolFileParameter fileParameter in tfg.files) {
                    BaseFile bf = toolFileParameterToBaseFile(fileParameter, inputFile, allInputFiles);
                    filesInTuple << bf;
                    allCreatedObjects << bf;
                }
                outputObject = FileObjectTupleFactory.createTuple(filesInTuple) as F;

            } else if (tparm instanceof ToolEntry.ToolFileGroupParameter) {
                ToolEntry.ToolFileGroupParameter tfg = tparm as ToolEntry.ToolFileGroupParameter;
                List<BaseFile> filesInGroup = [];

                for (ToolEntry.ToolFileParameter fileParameter in tfg.files) {
                    BaseFile bf = toolFileParameterToBaseFile(fileParameter, inputFile, allInputFiles)
                    filesInGroup << bf;
                    allCreatedObjects << bf;
                }

                Constructor cGroup = tfg.groupClass.getConstructor(List.class);
                FileGroup bf = cGroup.newInstance(filesInGroup);
                outputObject = tfg.groupClass.cast(bf) as F;
            }
        }

        if (arrayIndex == null)
            return outputObject;

        for (FileObject fo in allCreatedObjects) {
            if (!(fo instanceof BaseFile))
                continue;

            BaseFile bf = (BaseFile) fo;
            String newPath = bf.getAbsolutePath().replace(ConfigurationConstants.CVALUE_PLACEHOLDER_RODDY_JOBARRAYINDEX, arrayIndex);
            bf.setPath(new File(newPath));
        }
        return outputObject;
    }

    private BaseFile toolFileParameterToBaseFile(ToolEntry.ToolFileParameter fileParameter, BaseFile input, List<BaseFile> allInput) {
        Constructor c = searchConstructorForOneOf(fileParameter.fileClass, input.class, BaseFile.class);
        BaseFile bf = null;
        try {
            if (c == null) {
                input.getExecutionContext().addErrorEntry(ExecutionContextError.EXECUTION_FILECREATION_NOCONSTRUCTOR.expand("File object of type ${fileParameter?.fileClass} with input ${input?.class}."));
                throw new RuntimeException("Could not find valid constructor for type  ${fileParameter?.fileClass} with input ${input?.class}.");
            } else {
                bf = c.newInstance(input);
            }
        } catch (Exception ex) {
            input.getExecutionContext().addErrorEntry(ExecutionContextError.EXECUTION_FILECREATION_NOCONSTRUCTOR.expand("Error during constructor call."));

//                    logger.severe("Constructor for class ${c.name} with input class ${input.class.name} did not work!")
//                    bf = c.newInstance(input); // Try it again for debugging :)...
//                    for (Object o in ex.getStackTrace())
//                        logger.info(o.toString());
            throw (ex);
        }

        if (fileParameter.hasSelectionTag())
            bf.overrideFilenameUsingSelectionTag(fileParameter.filenamePatternSelectionTag);

        if (!fileParameter.checkFile)
            bf.setAsTemporaryFile();

        if (allInput.size() > 1)
            bf.setParentFiles(allInput, true);

//        File path = replaceParametersInFilePath(bf, parameters)
//        if (path == null && !Roddy.getFeatureToggleValue(AvailableFeatureToggles.AutoFilenames)) {
//            bf.getExecutionContext().addErrorEntry(ExecutionContextError.EXECUTION_FILECREATION_PATH_NOTSET.expand("File object of type ${bf.class.name} with input ${input.class}.."));
//            //TODO
//            path = new File("");
//        }

        if (fileParameter.scriptParameterName) {
            parameters[fileParameter.scriptParameterName] = bf;
        }
        bf
    }

    /**
     * Searches a constructor for classToSearch which fits to either one of classesToFind
     * If no constructor is found, null is returned.
     * @param classToSearch
     * @param classesToFind
     * @return
     */
    private Constructor<BaseFile> searchConstructorForOneOf(Class classToSearch, Class... classesToFind) {
        Constructor c;
        for (Class classToFind in classesToFind) {
            try {
                c = classToSearch.getConstructor(classToFind);
            } catch (NoSuchMethodException ex) {
            }
            if (c != null)
                break;
        }
        return c;
    }
}
