package de.dkfz.roddy.knowledge.brawlworkflows

import de.dkfz.roddy.StringConstants
import de.dkfz.roddy.config.AnalysisConfiguration
import de.dkfz.roddy.config.Configuration
import de.dkfz.roddy.config.ContextConfiguration
import de.dkfz.roddy.config.ToolEntry;
import de.dkfz.roddy.core.*
import de.dkfz.roddy.knowledge.files.FileObject
import de.dkfz.roddy.knowledge.files.Tuple2
import de.dkfz.roddy.knowledge.methods.GenericMethod
import de.dkfz.roddy.plugins.LibrariesFactory
import de.dkfz.roddy.tools.LoggerWrapper
import de.dkfz.roddy.tools.RoddyConversionHelperMethods
import de.dkfz.roddy.tools.RoddyIOHelperMethods

import java.lang.reflect.Parameter

import static de.dkfz.roddy.Constants.ENV_LINESEPARATOR as NEWLINE

import java.lang.reflect.Method;

/**
 * Brawl is some sort of easy language to write Roddy workflows.
 *
 * A brawlworkflows workflow will be loaded and compiled to a Java workflow
 * Created by heinold on 31.08.15.
 */
@groovy.transform.CompileStatic
public class BrawlWorkflow extends Workflow {
    public static final LoggerWrapper logger = LoggerWrapper.getLogger(BrawlWorkflow.class.name);

    @Override
    public boolean execute(ExecutionContext context) {

        StringBuilder classBuilder = new StringBuilder();

        ContextConfiguration configuration = (ContextConfiguration) context.getConfiguration();
        AnalysisConfiguration aCfg = configuration.getAnalysisConfiguration();

        String brawlWorkflow = aCfg.getBrawlWorkflow();

        //Load the file and convert to a synthetic Java workfow
        File f = context.getConfiguration().getSourceBrawlWorkflow(brawlWorkflow);

        // Find all lines which are not a comment and not empty
        def lines = f.readLines();
        lines = lines.findAll { it.trim() != "" && !it.startsWith("#") }.collect { String s = it.split(StringConstants.SPLIT_HASH)[0].trim(); s.endsWith(";") ? s[0 .. -2] : s }

        // Find the header and get the proper base class
        // If there are lines before the header, the code will stop and return false
        int lineIndex = 0
        if (!lines[0].contains(":")) return false;
        // Extract the Workflow name and the base class
        // Find the base class.
        String[] header = lines[0].split("[:]");
        String workflowName = header[0].trim();
        String baseClass = header[1].trim();
        Class _baseClass = null;
        if (baseClass == Workflow.class.simpleName)
            _baseClass = Workflow.class;
        else {
            //Search all loaded classes...
            _baseClass = LibrariesFactory.getInstance().searchForClass(baseClass);
        }
        classBuilder << "import " << Configuration.class.name << NEWLINE
        classBuilder << NEWLINE << "@groovy.transform.CompileStatic" << NEWLINE
        classBuilder << "public class $workflowName extends ${_baseClass.name} { $NEWLINE $NEWLINE @Override $NEWLINE public boolean execute(";

        // The second line must be the execute line
        if (!lines[1] == "execute") return false;
        def parms = []
        // Follow up parameters for the execute code
        for (lineIndex = 2; lineIndex < lines.size(); lineIndex++) {
            if (!lines[lineIndex].startsWith("->"))
                break;
            parms << lines[lineIndex][3..-1].trim()
        }
        // Find the proper execute method in the base Workflow classes
        // Go for the parameter count first, then for the parameter names... types are difficult.
        Collection<Method> listOfMethods = _baseClass.declaredMethods.findAll { method -> method.parameterCount == parms.size() }
        if (listOfMethods.size() != 1) {
            // Find by parameter names...
            logger.severe("More than one execute method found for workflow class ${workflowName}");
            return false;
        }
        if (listOfMethods.size() == 0) {
            logger.severe("No execute method found for workflow class ${workflowName}");
            return false;
        }

        def parmArr = []
        Map<String, String> knownObjects = [:]
        for (int j = 0; j < parms.size(); j++) {
            String pName = parms[j]
            String pType = listOfMethods[0].parameters[j].type.name
            parmArr << "${pType} ${pName}";
            knownObjects[pName] = pType;
        }
        knownObjects["configuration"] = ContextConfiguration.class.name
        classBuilder << parmArr.join(", ") << ") { $NEWLINE"
        classBuilder << "Configuration configuration = context.getConfiguration(); $NEWLINE"

        // Find and convert run flags
        for (; lineIndex < lines.size(); lineIndex++) {
            if (!lines[lineIndex].startsWith("runflag")) break;
            String[] values = lines[lineIndex].split(StringConstants.SPLIT_WHITESPACE);
            if (values.size() < 2) {
                return false
            }
            String flagid = values[1];
            Boolean defaultValue = values.size() == 4 && values[2] == "default" ? RoddyConversionHelperMethods.toBoolean(values[3], false) : false;
            classBuilder << "boolean $flagid = configuration.getConfigurationValues().getBoolean(\"$flagid\", ${defaultValue}); $NEWLINE"
        }

        // Load the rest of the code
        Map<String, String> undefinedVariables = [:];
        // First, create a list of code blocks... also check, if there are if/fi mismatches.
        for (; lineIndex < lines.size(); lineIndex++) {

            String l = lines[lineIndex].replaceAll("=", " = ").replaceAll("\\s+", " ");
            String[] _l = l.split(StringConstants.SPLIT_WHITESPACE);

            if (l.startsWith("if ")) {
                if(!l.contains("="))
                    classBuilder << "if (${_l[1]}) {"
                else
                    classBuilder << "if (${_l[1]} == ${_l[3]}) {"
            } else if (l == "fi") {
                classBuilder << "}"
            } else if (l.startsWith("else if")) {
                classBuilder << "} else if (${_l[1]}) {"
            } else if (l.startsWith("else")) {
                classBuilder << "} else {"
            } else if (l.startsWith("return")) {
                if(_l.size() == 1)
                    classBuilder << "return true"
                else
                    classBuilder << "return " << RoddyConversionHelperMethods.toBoolean(_l[1], false);
            } else if (l.startsWith("call")) {
                int indexOfCallCmd = 0;
                int indexOfCallee = 1

                StringBuilder temp = new StringBuilder();

                assembleCall(_l, indexOfCallCmd, indexOfCallee, temp, configuration, context, knownObjects)

                classBuilder << temp.toString()[2..-1] << NEWLINE;
            } else if (l.startsWith("var ")) {
                int indexOfAssignment = 2;
                int indexOfCallCmd = 3;
                int indexOfCallee = 4

                StringBuilder temp = new StringBuilder();
                String varname = _l[1];
                String classOfFileObject = "def" //FileObject.class.name;

                if (_l.size() > indexOfAssignment && _l[indexOfAssignment] == "=") {
                    classOfFileObject = assembleCall(_l, indexOfCallCmd, indexOfCallee, temp, configuration, context, knownObjects)
                }

                knownObjects[varname] = classOfFileObject;
                String alternativeName = "#${knownObjects.size()}#"
                if (classOfFileObject == "def") {
                    undefinedVariables[varname] = alternativeName;
                }

                String result = (classOfFileObject == "def" ? alternativeName : classOfFileObject) + " " + varname + temp;
                classBuilder << result;

            } else {
                int indexOfAssignment = 1;
                int indexOfCallCmd = 2;
                int indexOfCallee = 3;

                StringBuilder temp = new StringBuilder();
                String varname = _l[0];
                String classOfFileObject = "def";

                if (_l.size() > indexOfAssignment && _l[indexOfAssignment] == "=") {
                    classOfFileObject = assembleCall(_l, indexOfCallCmd, indexOfCallee, temp, configuration, context, knownObjects)
                    classBuilder << varname << temp ;
                }

                if (knownObjects.containsKey(varname) && knownObjects[varname] == "def") {
                    knownObjects[varname] = classOfFileObject;
                }
            }
            classBuilder << NEWLINE;
        }

        classBuilder << "return true" << NEWLINE << "} $NEWLINE }"

        String text = classBuilder.toString();
        undefinedVariables.each { String k, String v ->
            text = text.replaceAll(v, knownObjects[k]);
        }

        logger.postAlwaysInfo(text);
        try {
            GroovyClassLoader groovyClassLoader = new GroovyClassLoader();
            Class theParsedWorkflow = groovyClassLoader.parseClass(text);
            ((Workflow) theParsedWorkflow.newInstance()).execute(context);
        } catch (Exception ex) {
            println(ex);
            return false;
        }
        return true;
    }

    private String assembleCall(String[] _l, int indexOfCallCmd, int indexOfCallee, StringBuilder temp, ContextConfiguration configuration, ExecutionContext context, LinkedHashMap<String, String> knownObjects) {
        try {
            return _assembleCall(_l, indexOfCallCmd, indexOfCallee, temp, configuration, context, knownObjects);
        } catch (Exception ex) {
            logger.severe("Can't assemble call for line ${_l}")
        }
    }

    private String _assembleCall(String[] _l, int indexOfCallCmd, int indexOfCallee, StringBuilder temp, ContextConfiguration configuration, ExecutionContext context, LinkedHashMap<String, String> knownObjects) {
        String classOfFileObject = "def" //FileObject.class.name;
        if (!_l[indexOfCallCmd] == "call") logger.severe("Something is wrong!");
        String call = _l[indexOfCallee];
        String methodParameters = (_l.size() > (indexOfCallee + 2) ? _l[(indexOfCallee + 2)..-1].join(", ").replaceAll("\\,+", ",") : "").replaceAll("\", =,", "=\" + ").replaceAll("new, ", "new ");

        if (call[0] == '"') { // We call a tool!
            //Find out via tool id
            List<ToolEntry.ToolParameter> outputParameters = configuration.getTools().getValue(call[1..-2]).getOutputParameters(context);
            if (outputParameters.size() == 1) {
                if (outputParameters[0] instanceof ToolEntry.ToolFileParameter)
                    classOfFileObject = ((ToolEntry.ToolFileParameter) outputParameters[0]).fileClass.name;
                if (outputParameters[0] instanceof ToolEntry.ToolFileGroupParameter)
                    classOfFileObject = ((ToolEntry.ToolFileGroupParameter) outputParameters[0]).groupClass.name;
                if (outputParameters[0] instanceof ToolEntry.ToolTupleParameter)
                    classOfFileObject = "de.dkfz.roddy.knowledge.files.Tuple" + ((ToolEntry.ToolTupleParameter) outputParameters[0]).files.size();
            }
            temp << " = (" << classOfFileObject << ") " << GenericMethod.class.name << ".callGenericTool(" << call << ", " << methodParameters << ");"
        } else {
            temp << " = " << _l[indexOfCallee] << "(" << methodParameters << ");"
            //Find out via method
            //Find class first, then the method
            String[] splitCall = call.split(StringConstants.SPLIT_STOP)
            String classOrObject = splitCall[0];
            String classOfCallingObject = "";
            if (!knownObjects.containsKey(classOrObject)) {
                classOfCallingObject = LibrariesFactory.getInstance().searchForClass(classOrObject).name;
                if (!classOfCallingObject) {
                    logger.severe("Could not find a class or method!")
                }
            } else
                classOfCallingObject = knownObjects[classOrObject];
            Class _classOfCallingObject = ClassLoader.getSystemClassLoader().loadClass(classOfCallingObject)
            classOfFileObject = findTypeOfMethodOrProperty(_classOfCallingObject, splitCall, 1);

        }
        classOfFileObject
    }

    private String findTypeOfMethodOrProperty(Class _classOfCallingObject, String[] splitCall, int indexInSplit) {
        Method method = _classOfCallingObject.methods.find { Method m -> m.name == splitCall[indexInSplit].replaceAll("[()]", "") }
        Class classOfFileObject = method.returnType.name;
        if (splitCall.size() == indexInSplit + 1)
            return classOfFileObject.name;
        else
            return findTypeOfMethodOrProperty(classOfFileObject, splitCall, indexInSplit + 1);
    }
}
