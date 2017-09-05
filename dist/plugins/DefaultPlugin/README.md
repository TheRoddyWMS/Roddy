# Default Roddy Plugin

The root of all Roddy plugins, including the PluginBase plugin.

All top-level tools or scripts that are supposed to be started on the cluster by Roddy are actually not directly
started, but are wrapped by the `resources/roddyTools/wrapInScript.sh` contained in this plugin.

## Environment Setup Support

The wrap in script checks whether you have a dedicated environment script defined for the whole workflow or the specific
cluster job. These variables are usually defined in one of the configuration XMLs or on the commandline via the 
`--cvalues` parameter.

To define a plugin-level environment, you can add lines like the following to your XMLs:

```xml
<cvalue name="workflowEnvironmentScript" value="${TOOL_WORKFLOW_ENVIRONMENT_CONDA}" type="string"
              description="Use ${TOOL_WORKFLOW_ENVIRONMENT_CONDA} for a generic Conda environment."/>
<processingTools>
   <tool name="workflowEnvironment_conda" value="conda.sh" basepath="workflowName/environments"/>
</processingTools>
```

This will declare that the file `resources/workflowName/environments/conda.sh` is to be used as workflow setup
script for all jobs. Like all "tools" the environment scripts needs to be made executable. 

Notice the reference to a tool variable in the `cvalue`. Each environment script is represented in Roddy as
a "tool" that has a name, e.g. "myProcessingStepEnv". All tool names, which are conventionally in "camel-case", are exposed 
to the cluster job environment in a translated form. The tool name is translated in 3 steps by (1) inserting an 
underscore '\_' before all capitals, (2) changing them to all upper-case, and (3) prepending "TOOL\_" before the name.
Thus "myProcessingStep" becomes "TOOL_MY_PROCESSING_STEP_ENV". The "workflowEnvirontment_conda" tool from the previous
example is translated to "TOOL_WORKFLOW_ENVIRONMENT_CONDA" and points to the `workflowName/environments/conda.sh` _as
it is available for the cluster job on the remote system_. Therefore in the XML the tool is only specified with a 
`basepath` relative to the `resources` directory in the plugin.

As the environment script is simple `source`'d you can access any variables from within that script. For instance, 
you may want to also specify the conda environment name in the XML:

```xml
<cvalue name="condaEnvironmentName" value="myWorkflow" type="string"
        description="Name of the Conda environment on the execution hosts. Used by the environment setup script conda.sh defined as tool below."/>
```

Then your `conda.sh` may look like this:

```bash
source activate "$condaEnvironmentName"
```

Additionally, you can specify dedicated scripts for cluster jobs. For instance, the following defines a tool as
environment script for the `correctGcBias` cluster job (which is also defined as tool).

```xml
<cvalue name="correctGcBiasEnvironmentScript" value="${TOOL_CORRECT_GC_BIAS_ENVIRONMENT_CONDA}" type="string"/>
<processingTools>
  <tool name="correctGcBiasEnvironment_conda" value="conda-correctGcBias.sh" basepath="workflowName/environments"/>
</processingTools>
``` 

Cluster-job specific environments take precedence over plugin-level environments. Thus you can define a default for your
plugin and a modified environment for a specific job.

### Conventions

The following conventions are nothing more than that and are currently not enforced by Roddy:

* use camel-case tool names starting with small letters (e.g. "correctGcBias")
* append the arbitrary environment name that you want to use to the tool name to get the name of the environment variable
* describe the environment in the `description` attribute of the `cvalue` tag
* the environment setup scripts should be located in the "environments" subdirectory of the workflow directory in the plugin

