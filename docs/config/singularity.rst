Singularity Job Containers
==========================

Since version 3.8.0 Roddy can execute jobs in Singularity containers. There are a number of configuration values that need to be set to get this feature working:

  * `jobExecutionEnvironment` needs to be set to `singularity` or `apptainer`
  * `containerEnginePath` needs to be set to the path of the container engine. By default this is the name of the engine as provided in `jobExecutionEnvironment`.
  * `containerImage` needs to be set to the path of the container image.
  * `containerMounts` needs to be set to a list of paths to mount into the container. This should be of the format `(mount1 mount2 mount3)`. Note that you don't have to add the `inputBaseDirectory` and `outputBaseDirectory`, because these are added automatically. Be careful with symlinks, as Roddy does not resolve them (neither locally nor remotely).
  * `apptainerParameters` can be used to pass global parameters to `apptainer exec`. For instance `--contain`, which is advisable to unsure a proper separation between the compute nodes' environment and the analysis environment.

Singularity containers work with `outputFileGroup`. Furthermore, all variables that you can use in your bioinformatic job's top-level scripts that you use without Singularity containers, also are available in Singularity containers. By contrast, only a very limited number of environment variables on the executing environment are available.

Note that Singularity does not resolve symlinks, and Roddy does not help you to automatically resolve symlinks as mount points. This means, if your need to access files you should manually check that paths containing symlinks will be properly mounted into the container at the positions they are expected by your workflow.

.. code-block:: XML

    <configurationvalues>
        <cvalue name="jobExecutionEnvironment" value="apptainer" type="string"
                description="Set this to 'bash' for native execution, 'apptainer', or 'singularity'. Default is 'bash'."/>
        <cvalue name="containerEnginePath" value="/usr/bin/apptainer" type="path"
                description="The path to the container engine. By default this is the name of the engine as provided in jobExecutionEnvironment."/>
        <cvalue name="containerImage" value="/path/to/job-containers/dkfz_minimal.sif" type="path"
                description="Path to a singularity/apptainer container."/>
        <cvalue name="containerMounts" value="( /your/software/ /software /your/virtualenvs/ /software/modules/3.2.10 /your/miniconda3 /your/annotation/data /your/reference/genome /true/symlinked/path )" type="bashArray"
                description="List of paths to mount into the container. Can be comma-separated list (type='string') or a type='bashArray'. All these paths are mounted read-only. This should be of the format '(mount1 mount2 mount3)'. Note that you dont have to add the inputBaseDirectory and outputBaseDirectory, because these are added automatically. Be careful with symlinks, as Roddy does not resolve them (neither locally nor remotely)."/>
        <cvalue name="apptainerParameters" value="--contain" type="string"
                description="Global parameters for `apptainer exec`. For instance '--contain'. Can be comma-separated list (type='string') or a type='bashArray'."/>
    </configurationvalues>
