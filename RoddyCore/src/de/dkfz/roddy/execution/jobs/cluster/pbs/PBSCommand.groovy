package de.dkfz.roddy.execution.jobs.cluster.pbs

import de.dkfz.roddy.AvailableFeatureToggles
import de.dkfz.roddy.Roddy
import de.dkfz.roddy.StringConstants
import de.dkfz.roddy.config.Configuration
import de.dkfz.roddy.core.ExecutionContext
import de.dkfz.roddy.execution.io.ExecutionService
import de.dkfz.roddy.execution.io.fs.FileSystemInfoProvider
import de.dkfz.roddy.execution.jobs.Command
import de.dkfz.roddy.execution.jobs.Job
import de.dkfz.roddy.execution.jobs.ProcessingCommands

import java.util.logging.Level
import java.util.logging.Logger

import static de.dkfz.roddy.StringConstants.*

/**
 * This class is used to create and execute qsub commands
 *
 * @author michael
 */
@groovy.transform.CompileStatic
public class PBSCommand extends Command implements Serializable {

    private static final de.dkfz.roddy.tools.LoggerWrapper logger = de.dkfz.roddy.tools.LoggerWrapper.getLogger(PBSCommand.class.name);
    public static final String NONE = "none"
    public static final String NONE_ARR = "none[]"
    public static final String AFTEROK = "afterok"
    public static final String AFTEROK_ARR = "afterokarray"
    public static final String QSUB = "qsub"
    public static final String PARM_ACCOUNT = " -A "
    public static final String PARM_JOBNAME = " -N "
    public static final String PARM_JOINLOGS = " -j oe"
    public static final String PARM_LOGPATH = " -o "
    public static final String PARM_MAIL = " -M "
    public static final String PARM_GROUPLIST = " -W group_list="
    public static final String PARM_UMASK = " -W umask="
    public static final String PARM_DEPENDS = " -W depend="
    public static final String PARM_VARIABLES = " -v "
    public static final String PARM_WRAPPED_SCRIPT = "WRAPPED_SCRIPT="

    private transient Configuration configuration;

    /**
     * The qsub log directoy where all output is put
     */
    private File loggingDirectory;
    /**
     * Parameters for the qsub command
     */
    private transient Map<String, String> parameters;
    /**
     * The command which should be called
     */
    private String command;
    /**
     * The job id for the qsub system
     */
    private String id;
    /**
     * Provide a lower and upper array index to make this qsub job an array job
     */
    private List<String> arrayIndices;

    private List<String> dependencyIDs;
    private final List<ProcessingCommands> processingCommands
    /**
     *
     * @param id
     * @param parameters
     * @param command
     * @param filesToCheck
     */
    //    @groovy.transform.CompileStatic
//    public PBSCommand(ExecutionContext context, ExecutionService executionService, String id, Map<String, String> parameters, List<String> dependencyIDs, String command) {
//        this(context, executionService, id, parameters, null, dependencyIDs, command);
//    }

    /**
     *
     * @param id
     * @param parameters
     * @param arrayIndices
     * @param command
     * @param filesToCheck
     */
    public PBSCommand(Job job, ExecutionContext run, ExecutionService executionService, String id, List<ProcessingCommands> processingCommands, Map<String, String> parameters, List<String> arrayIndices, List<String> dependencyIDs, String command) {
        super(job, run, String.format("%s_%08d", run.getDataSet(), Command.getNextIDCountValue()));
        this.processingCommands = processingCommands;
        this.parameters = parameters ?: new LinkedHashMap<String, String>();
        this.command = command;
        this.id = id;
        this.configuration = run.getConfiguration()
        this.loggingDirectory = new File(run.getLoggingDirectory().getAbsolutePath())
        this.arrayIndices = arrayIndices ?: new LinkedList<String>();
        this.dependencyIDs = dependencyIDs ?: new LinkedList<String>();
    }

    private synchronized void writeObject(java.io.ObjectOutputStream s) throws IOException {
        try {
            s.defaultWriteObject();
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    private synchronized void readObject(java.io.ObjectInputStream s) throws IOException, ClassNotFoundException {
        try {
            s.defaultReadObject();
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (ClassNotFoundException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    public boolean getIsArray() {
        return arrayIndices != null && arrayIndices.size() > 0;
    }

    String getJoinLogParameter() {
        return PARM_JOINLOGS;
    }

    String getEmailParameter(String address) {
        return PARM_MAIL + address;
    }

    String getGroupListString(String groupList) {
        return PARM_GROUPLIST + groupList;
    }

    String getUmaskString(String umask) {
        return PARM_UMASK + umask;
    }

    String getDependencyParameterName() {
        return AFTEROK;
    }

    /**
     * In this case i.e. afterokarray:...,afterok:
     * A comma
     * @return
     */
    protected String getDependencyTypesSeparator() {

    }

    protected String getDependencyOptionSeparator() {
        return COLON;
    }

    protected String getDependencyIDSeparator() {
        return COLON;
    }

    protected String getArrayDependencyParameterName() {
        return AFTEROK_ARR;
    }

    protected String getAdditionalCommandParameters() {
        return "";
    }

    // I.e. -W depend=
    protected String getDependsSuperParameter() {
        PARM_DEPENDS
    }
    //    /**
    //     * Executes the command and returns the job id
    //     * Optionial: SSHstring to execute the call remotely
    //     * @return
    //     */
    //    public def executeIndependent() {
    //        return executeDependent(new LinkedList<String>())
    //    }
    //
    //    public String executeDependent(List<String> dependencies = []) {
    //
    //
    //    }
    @Override
    public String toString() {

        String email = configuration.getConfigurationValues().getString("email");
        String groupList = configuration.getConfigurationValues().getString("outputFileGroup", null);
        String accountName = configuration.getConfigurationValues().getString("PBS_AccountName", "");


        StringBuilder qsubCall = new StringBuilder(EMPTY);

        qsubCall << QSUB << PARM_JOBNAME << id

        if (accountName) qsubCall << PARM_ACCOUNT << accountName << " "

        qsubCall << getAdditionalCommandParameters();

        if (loggingDirectory) qsubCall << PARM_LOGPATH << loggingDirectory

        qsubCall << getJoinLogParameter();

        if (isArray) {
            qsubCall << " -t ";
            StringBuilder sbArrayIndices = new StringBuilder("");
            //TODO Make a second list of array indices, which is valid for job submission. The current translation with the help of counting is not optimal!
            int i = 1; //TODO Think if pbs arrays should always start with one?
            for (String ai in arrayIndices) {
                if (ai.isNumber())
                    sbArrayIndices << ai.toInteger();
                else
                    sbArrayIndices << i;
                sbArrayIndices << COMMA;
                i++;
            }
            qsubCall << sbArrayIndices.toString()[0..-2]
        }
        if (email) qsubCall << getEmailParameter(email);

        LinkedList<String> tempDependencies = new LinkedList<String>();
        LinkedList<String> tempDependenciesArrays = new LinkedList<String>();
        for (String d in dependencyIDs) {
            if (d != "" && d != NONE && d != "-1") {
                if (d.contains("[].")) {
                    tempDependenciesArrays << d.toString()
                } else {
                    tempDependencies << d.toString()
                }
            }
        }

        if (groupList != EMPTY && groupList != "UNDEFINED") {
            qsubCall << getGroupListString(groupList);
        }
        String outputUMask = configuration.getConfigurationValues().getString("outputUMask", "007");
        qsubCall << getUmaskString(outputUMask);

        // Append the epilogue script for job state tracking.
//        qsubCall << " -l epilogue=" << configuration.getProcessingToolPath(executionContext, "jobEpilogue");

        for (ProcessingCommands pcmd in job.getListOfProcessingCommand()) {
            if (!(pcmd instanceof PBSResourceProcessingCommand)) continue;
            PBSResourceProcessingCommand command = (PBSResourceProcessingCommand) pcmd;
            if (command == null)
                continue;
            qsubCall << WHITESPACE << command.getProcessingString();
        }

        if (tempDependencies.size() > 0 || tempDependenciesArrays.size() > 0) {
            StringBuilder depStrBld = new StringBuilder()
            try {
                depStrBld << EMPTY //Prevent the string to be null!
                //qsub wants the afterokarray before afterok. Don't swap this
                if (tempDependenciesArrays.size() > 0) {
                    depStrBld << getArrayDependencyParameterName() << getDependencyOptionSeparator()
                    for (String d in tempDependenciesArrays) {
                        depStrBld << (d != NONE && d != NONE_ARR && d != "-1" ? d + getDependencyIDSeparator() : EMPTY)
                    }
                    String tmp = depStrBld.toString()[0..-2];
                    depStrBld = new StringBuilder();
                    depStrBld << tmp;
                }
                if (tempDependencies.size() > 0) {
                    if (tempDependenciesArrays.size() > 0) {
                        depStrBld << getDependencyTypesSeparator();
                    }

                    String dependencyType = getDependencyParameterName();
                    for (ProcessingCommands pcmd : job.getListOfProcessingCommand()) {
                        if (!(pcmd instanceof ChangedProcessDependencyProcessingCommand))
                            continue;
                        ChangedProcessDependencyProcessingCommand dpc = pcmd as ChangedProcessDependencyProcessingCommand;
                        if (dpc == null) continue;
                        dependencyType = dpc.getProcessDependency().name();
////                        dependencyType = dpc.dependencyOptions.name();
//                        DependencyGroup group = DependencyGroup.getGroup(dpc.dependencyGroupID);
//                        if(job == group.referenceJob) {
//                            continue;
//                        } else {
//                            tempDependencies << group.referenceJob.getJobResult().getJobID().shortID;
//                        }
                    }

                    depStrBld << dependencyType << getDependencyOptionSeparator();
                    for (String d in tempDependencies) {
                        depStrBld << (d != NONE && d != NONE_ARR && d != "-1" ? d + getDependencyIDSeparator() : EMPTY)
                    }
                    String tmp = depStrBld.toString()[0..-2];
                    depStrBld = new StringBuilder();
                    depStrBld << tmp;
                }

                String depStr = depStrBld.toString()
                if (depStr.length() > 1) {
                    qsubCall << getDependsSuperParameter() << depStr
                }
            } catch (Exception ex) {
                logger.log(Level.SEVERE, ex.toString());
            }
        }

        qsubCall << PARM_VARIABLES + PARM_WRAPPED_SCRIPT << command;
        if (parameters.size() > 0) {
            List<String> allParms = [];
            StringBuilder parameterSB = new StringBuilder();
            for (String parm : parameters.keySet()) {
                String val = parameters.get(parm);
                if (val.contains("\${") && val.contains(BRACE_RIGHT)) {
                    val = val.replace("\${", "#{"); // Replace variable names so they can be passed to qsub.
                }
                String key = parm;
                if (val.startsWith("parameterArray=")) {
                    val = "'" + val.replace("parameterArray=", EMPTY) + SINGLE_QUOTE;
                }
                allParms << "${key}${EQUALS}${val}".toString();
//                parameterSB << COMMA << key << EQUALS << val
            }
            if (Roddy.getFeatureToggleValue(AvailableFeatureToggles.ModifiedVariablePassing)) {
                File parmFile = executionContext.getParameterFilename(this);
                qsubCall << ",PARAMETER_FILE=" << parmFile;
                StringBuilder allLines = new StringBuilder();
                allParms.each {
                    String line ->
                    //TODO export is Bash dependent!
                    allLines << "export " << line << "\n";
                }
                FileSystemInfoProvider.getInstance().writeTextFile(parmFile, allLines.toString(), executionContext);
            } else {
                qsubCall << StringConstants.COMMA << allParms.join(StringConstants.COMMA);
            }
        }
        qsubCall << " " << configuration.getProcessingToolPath(executionContext, "wrapinScript").getAbsolutePath();
        return qsubCall
    }
}
