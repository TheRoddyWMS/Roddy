package de.dkfz.roddy.knowledge.examples

import de.dkfz.roddy.config.Configuration
import de.dkfz.roddy.core.ExecutionContext
import de.dkfz.roddy.core.RuntimeService
import de.dkfz.roddy.execution.io.fs.FileSystemAccessProvider
import de.dkfz.roddy.execution.jobs.CommandFactory
import de.dkfz.roddy.knowledge.files.BaseFile

/**
 */
@groovy.transform.CompileStatic
public class SimpleRuntimeService extends RuntimeService {
    @Override
    public Map<String, Object> getDefaultJobParameters(ExecutionContext context, String toolID) {
        //File cf = fs..createTemporaryConfigurationFile(executionContext);
        Configuration cfg = context.getConfiguration();
        String pid = context.getDataSet().toString();
        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("pid", (Object) pid);
        parameters.put("PID", (Object) pid);
        parameters.put("CONFIG_FILE", getNameOfConfigurationFile(context).getAbsolutePath());
        parameters.put("ANALYSIS_DIR", context.getOutputDirectory().getParentFile().getParent());
        if (toolID != null && toolID.length() > 0) {
            parameters.put("PRM_TOOLS_DIR", cfg.getProcessingToolPath(context, toolID).getParent());
        }
        return parameters;
    }

    @Override
    public String createJobName(ExecutionContext executionContext, BaseFile file, String TOOLID, boolean reduceLevel) {
        return CommandFactory.getInstance().createJobName(file, TOOLID, reduceLevel);
    }

    @Override
    public boolean isFileValid(BaseFile baseFile) {
        //Parents valid?
        boolean parentsValid = true;
        for (BaseFile bf in baseFile.parentFiles) {
            if (bf.isTemporaryFile()) continue; //We do not check the existence of parent files which are temporary.
            if (bf.isSourceFile()) continue; //We do not check source files.
            if (!bf.isFileValid()) {
                return false;
            }
        }

        boolean result = true;

        //Source files should be marked as such and checked in a different way. They are assumed to be valid.
        if(baseFile.isSourceFile())
            return true;

        //Temporary files are also considered as valid.
        if(baseFile.isTemporaryFile())
            return true;

        try {
            //Was freshly created?
            if (baseFile.creatingJobsResult != null && baseFile.creatingJobsResult.wasExecuted) {
                result = false;
            }
        } catch (Exception ex) {
            result = false;
        }

        try {
            //Does it exist and is it readable?
            if (result && !baseFile.isFileReadable()) {
                result = false;
            }
        } catch (Exception ex) {
            result = false;
        }

        try {
            //Can it be validated?
            //TODO basefiles are always validated!
            if (result && !baseFile.checkFileValidity()) {
                result = false;
            }
        } catch (Exception ex) {
            result = false;
        }

// If the file is not valid then also temporary parent files should be invalidated! Or at least checked.
        if (!result) { }

        return result;
    }

    @Override
    public void releaseCache() {

    }

    @Override
    public boolean initialize() {

    }

    @Override
    public void destroy() {

    }

    public SimpleTestTextFile createInitialTextFile(ExecutionContext ec) {
        SimpleTestTextFile tf = new SimpleTestTextFile(new File(getOutputFolderForDataSetAndAnalysis(ec.getDataSet(), ec.getAnalysis()).getAbsolutePath() + "/textBase.txt"), ec, null, null, new SimpleFileStageSettings(ec.getDataSet(), "100", "R001"));
        tf.setAsSourceFile();
        if (!FileSystemAccessProvider.getInstance().checkFile(tf.getPath()))
            FileSystemAccessProvider.getInstance().createFileWithDefaultAccessRights(true, tf.getPath(), ec, true);
        return tf;
    }
}
