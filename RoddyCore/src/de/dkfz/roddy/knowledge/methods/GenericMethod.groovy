package de.dkfz.roddy.knowledge.methods

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
 */
@groovy.transform.CompileStatic
class GenericMethod {

    private static final LoggerWrapper logger = LoggerWrapper.getLogger(GenericMethod.class.getName());

    public static <F extends FileObject> F callGenericTool(String toolName, BaseFile input, Object... additionalInput) {
        F result = callGenericToolArray(toolName, null, input, additionalInput);
        if (result == null) {
            def errMsg = "The job callGenericTool(${toolName}, ...) returned null."
            logger.warning(errMsg);
            input.getExecutionContext().addErrorEntry(ExecutionContextError.EXECUTION_JOBFAILED.expand(errMsg))
        }
        return result;
    }

    public static <F extends FileObject> F callGenericToolArray(String toolName, List<String> arrayIndices, BaseFile input, Object... additionalInput) {
        ExecutionContext context = input.getExecutionContext();
        Configuration cfg = context.getConfiguration();
        ToolEntry te = cfg.getTools().getValue(toolName);
        if (!te.isToolGeneric()) {
            logger.severe("Tried to call a non generic tool via the generic call method");
            throw new RuntimeException("Not able to context tool " + toolName);
        }
        Map<String, Object> parameters = [:];

        if (toolName) {
            parameters[PRM_TOOLS_DIR] = cfg.getProcessingToolPath(context, toolName).getParent();
            parameters[PRM_TOOL_ID] = toolName;
        }

        List<FileObject> allInput = new LinkedList<FileObject>();
        allInput << input;
        for (Object entry in additionalInput) {
            if (entry instanceof BaseFile)
                allInput << (BaseFile) entry;
            else if (entry instanceof FileGroup) {
                //Take a group and store all files in that group.
                allInput << (FileGroup) entry;

            } else {
                String[] split = entry.toString().split("=");
                if (split.length != 2)
                    throw new RuntimeException("Not able to convert entry ${entry.toString()} to parameter.")
                parameters[split[0]] = split[1];
            }
        }

        List<BaseFile> pFiles = [];
        for (FileObject fo in allInput) {
            if (fo instanceof BaseFile)
                pFiles << (BaseFile) fo;
            else if (fo instanceof FileGroup)
                pFiles.addAll(((FileGroup) fo).getFilesInGroup());
        }

        for (int i = 0; i < te.getInputParameters(context).size(); i++) {
            ToolEntry.ToolParameter toolParameter = te.getInputParameters(context)[i];
            if (toolParameter instanceof ToolEntry.ToolFileParameter) {
                ToolEntry.ToolFileParameter _tp = (ToolEntry.ToolFileParameter) toolParameter;
                //TODO Check if input and output parameters match and also check for array indices and item count. Throw a proper error message.
                if (!allInput[i].class == _tp.fileClass)
                    logger.severe("Class mismatch for " + allInput[i] + " should be of class " + _tp.fileClass);
                if (_tp.scriptParameterName) {
                    String path = ((BaseFile) allInput[i])?.path?.absolutePath;
                    if (!path) {
                        context.addErrorEntry(ExecutionContextError.EXECUTION_PARAMETER_ISNULL_NOTUSABLE.expand("The parameter ${_tp.scriptParameterName} has no valid value and will be set to <NO_VALUE>."));
                        path = NO_VALUE;
                    }
                    parameters[_tp.scriptParameterName] = path;

                }
                _tp.constraints.each {
                    ToolEntry.ToolConstraint constraint ->
                        constraint.apply(input);
                }
            } else if (toolParameter instanceof ToolEntry.ToolTupleParameter) {
                ToolEntry.ToolTupleParameter _tp = (ToolEntry.ToolTupleParameter) toolParameter;
                logger.severe("Tuples must not be used as an input parameter for tool ${toolName}.l");
            } else if (toolParameter instanceof ToolEntry.ToolFileGroupParameter) {
                ToolEntry.ToolFileGroupParameter _tp = (ToolEntry.ToolFileGroupParameter) toolParameter;
                if (!allInput[i].class == _tp.groupClass)
                    logger.severe("Class mismatch for ${allInput[i]} should be of class ${_tp.groupClass}");
                if (_tp.passOptions == ToolEntry.ToolFileGroupParameter.PassOptions.parameters) {
                    int cnt = 0;
                    for (BaseFile bf in (List<BaseFile>) ((FileGroup) allInput[i]).getFilesInGroup()) {
                        parameters[_tp.scriptParameterName + "_" + cnt] = bf.getAbsolutePath();
                        cnt++;
                    }
                } else { //Arrays
                    int cnt = 0;
                    parameters[_tp.scriptParameterName] = ((FileGroup) allInput[i]).getFilesInGroup();//paths;
                }
            }
        }

        for (int i = 0; i < te.getOutputParameters(context).size(); i++) {
            ToolEntry.ToolParameter toolParameter = te.getOutputParameters(context)[i];
            if (toolParameter instanceof ToolEntry.ToolFileParameter) {
                ToolEntry.ToolFileParameter _tp = (ToolEntry.ToolFileParameter) toolParameter;
                for (ToolEntry.ToolConstraint constraint in _tp.constraints) {
                    constraint.apply(input);
                }
            }
        }

        F outputObject = null;
        List<FileObject> allCreatedObjects = [];

        outputObject = createOutputObject(te, input, pFiles, parameters, allCreatedObjects);

        allCreatedObjects.add(outputObject);
        JobResult jobResult = null;
        //Verifiable output files:
        List<BaseFile> filesToVerify = [];
        allCreatedObjects.each { FileObject fo -> if (fo instanceof BaseFile && ! ((BaseFile)fo).isTemporaryFile()) filesToVerify << (BaseFile) fo; }

        if (arrayIndices != null) {
            jobResult = new Job(context, context.createJobName(input, toolName), toolName, arrayIndices, parameters, pFiles, filesToVerify).run();

            Map<String, FileObject> outputObjectsByArrayIndex = [:];
            IndexedFileObjects indexedFileObjects = new IndexedFileObjects(arrayIndices, outputObjectsByArrayIndex, context);
            // Run array job and afterwards create output files for all sub jobs. The values in filesToVerify will be used and the path names will be corrected.
//            allCreatedObjects.removeAll(filesToVerify);
            int i = 1;
            for (String arrayIndex in arrayIndices) {
                List<FileObject> newObjects = [];
                outputObjectsByArrayIndex[arrayIndex] = createOutputObject(te, input, pFiles, parameters, newObjects, arrayIndex);
                JobResult jr = CommandFactory.getInstance().convertToArrayResult(jobResult.job, jobResult, i++);
                for (FileObject fo : newObjects) {
                    fo.setCreatingJobsResult(jr);
                }
            }
            return indexedFileObjects;
        }
        jobResult = new Job(context, context.createJobName(input, toolName), toolName, parameters, pFiles, filesToVerify).run();

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

    private
    static <F extends FileObject> F createOutputObject(ToolEntry te, BaseFile input, List<BaseFile> allInput, Map<String, Object> parameters, List<FileObject> allCreatedObjects, String arrayIndex = null) {
        F outputObject = null;
        if (te.getOutputParameters(input.getExecutionContext()).size() == 1) {
            ToolEntry.ToolParameter tparm = te.getOutputParameters(input.getExecutionContext())[0];
            if (tparm instanceof ToolEntry.ToolFileParameter) {
                ToolEntry.ToolFileParameter fileParameter = tparm as ToolEntry.ToolFileParameter;
                BaseFile bf = toolFileParameterToBaseFile(fileParameter, input, allInput, parameters)
                for(ToolEntry.ToolFileParameter childFileParameter in fileParameter.getChildFiles()) {
                    if(childFileParameter.parentVariable == null) {
                        continue;
                    }
                    java.lang.reflect.Field _field = null;
                    java.lang.reflect.Method _method = null;
                    try {
                        _field =  bf.getClass().getField(childFileParameter.parentVariable);
                    } catch (Exception ex) { }
                    try {
                        String setterMethod = "set" + childFileParameter.parentVariable[0].toUpperCase() + childFileParameter.parentVariable[1..-1];
                        _method = bf.getClass().getMethod(setterMethod, childFileParameter.fileClass);
                    } catch(Exception ex) { }
                    if(_field == null && _method == null) {
                        try {
                            input.getExecutionContext().addErrorEntry(ExecutionContextError.EXECUTION_FILECREATION_FIELDINACCESSIBLE.expand("Class ${bf.getClass().getName()} field ${childFileParameter.parentVariable}"));
                        } catch (Exception ex) {
                        }
                        continue;
                    }
                    BaseFile childFile = toolFileParameterToBaseFile(childFileParameter, bf, [bf], parameters);
                    allCreatedObjects << childFile;
                    if(_field)
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
                    BaseFile bf = toolFileParameterToBaseFile(fileParameter, input, allInput, parameters);
                    filesInTuple << bf;
                    allCreatedObjects << bf;
                }
                outputObject = FileObjectTupleFactory.createTuple(filesInTuple) as F;

            } else if (tparm instanceof ToolEntry.ToolFileGroupParameter) {
                ToolEntry.ToolFileGroupParameter tfg = tparm as ToolEntry.ToolFileGroupParameter;
                List<BaseFile> filesInGroup = [];

                for (ToolEntry.ToolFileParameter fileParameter in tfg.files) {
                    BaseFile bf = toolFileParameterToBaseFile(fileParameter, input, allInput, parameters)
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

    private
    static BaseFile toolFileParameterToBaseFile(ToolEntry.ToolFileParameter fileParameter, BaseFile input, List<BaseFile> allInput, Map<String, Object> parameters) {
        Constructor c = getConstructorForOneOf(fileParameter.fileClass, input.class, BaseFile.class);
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

        if(!fileParameter.checkFile)
            bf.setAsTemporaryFile();

        if (allInput.size() > 1)
            bf.setParentFiles(allInput, true);

        File path = replaceParametersInFilePath(bf, parameters)
        if (path == null) {
            bf.getExecutionContext().addErrorEntry(ExecutionContextError.EXECUTION_FILECREATION_PATH_NOTSET.expand("File object of type ${bf.class.name} with input ${input.class}.."));
            path = new File("");
        }

        if (fileParameter.scriptParameterName) {
            parameters[fileParameter.scriptParameterName] = path;
        }
        bf
    }

    private static Constructor<BaseFile> getConstructorForOneOf(Class forClass, Class... inputClass) {
        Constructor c;
        for (Class ic in inputClass) {
            try {
                c = forClass.getConstructor(ic);
            } catch (NoSuchMethodException ex) {
            }
            if (c != null)
                break;
        }
        return c;
    }

    private static File replaceParametersInFilePath(BaseFile bf, Map<String, Object> parameters) {
//TODO: It can occur that the regeneration of the filename is not valid!

        // Replace $_JOBPARAMETER items in path with proper values.
        //TODO: Think how to best place this with parameters into the FilenamePattern class.
        File path = bf.getPath()
        if (path == null) {
            return null;
        }

        String absolutePath = path.getAbsolutePath()
        if (absolutePath.contains($_JOBPARAMETER)) {
            FilenamePattern.Command command = FilenamePattern.extractCommand($_JOBPARAMETER, absolutePath);
            FilenamePattern.CommandAttribute name = command.attributes.get("name");
//                    FilenamePattern.CommandAttribute defValue = command.attributes.get("default");
            if (name != null) {
                String val = parameters[name.value];
                if (val == null) {
                    val = NO_VALUE;
                    bf.getExecutionContext().addErrorEntry(ExecutionContextError.EXECUTION_PARAMETER_ISNULL_NOTUSABLE.expand("A value named " + name.value + " cannot be found in the jobs parameter list for a file of ${bf.class.name}. The value is set to <NO_VALUE>"));
                }
                absolutePath = absolutePath.replace(command.name, val);
            }
            path = new File(absolutePath);
            bf.setPath(path);
        }
        path
    }
}
