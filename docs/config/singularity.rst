Singularity Job Containers
==========================

Since version 3.8.0 Roddy can execute jobs in Singularity containers.

When submitting a task to the cluster (or executing it locally) with Singularity, the ``wrapInScript.sh`` is not executed natively, but instead the call is wrapped in a Singularity call.

The only differences to the execution without Singularity are

1. The ``wrapInScript.sh`` is not executed natively, but instead the call is wrapped in a Singularity call.
2. Input and output directories have to be mounted into the container.

Configuration
-------------

There are a number of configuration values that need to be set to get this feature working:

  * `jobExecutionEnvironment` needs to be set to `singularity` or `apptainer`.
  * `containerEnginePath` needs to be set to the path of the container engine. By default this is the name of the engine as provided in `jobExecutionEnvironment`, e.g. "singularity". Thus it is assumed that the "singularity" executable is available in the environment (e.g. of `bsub`).
  * `containerImage` needs to be set to the path of the container image.
  * `containerMounts` needs to be set to a list of paths to mount into the container. This should be of the format `(mount1 mount2 mount3)`. Note that you don't have to add the `inputBaseDirectory` and `outputBaseDirectory`, because these are added automatically. Be careful with symlinks, as Roddy does not resolve them (neither locally nor remotely).
  * `apptainerArguments` can be used to pass global parameters to `apptainer exec`. For instance `--contain`, which is advisable to unsure a proper separation between the compute nodes' environment and the analysis environment.

Singularity containers work with setting `outputFileGroup`. We simply change the primary group *before* invoking the `singularity` command.

Furthermore, all variables that you can use in your bioinformatic job's top-level scripts that you use without Singularity containers, also are available in Singularity containers. By contrast, only a very limited number of environment variables on the executing environment are available.

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
        <cvalue name="apptainerArguments" value="--contain" type="string"
                description="Global parameters for `apptainer exec`. For instance '--contain'. Can be comma-separated list (type='string') or a type='bashArray'."/>
    </configurationvalues>

Minimal Container (DKFZ-only)
-----------------------------

The Roddy repository contains a ``Dockerfile`` in ``containers/dkfz_minimal`` to build a minimal container for the tasks.
This is basically, a plain CentOS 7.9 (yeah, "legacy") with some additional packages, like they are available in the DKFZ/ODCF cluster environment.
It is called "minimal" because it does not contain any reference data or software -- just the basic operating system that is needed to execute exactly the legacy software stack at our institution -- including the module system.
Therefore, using this container, the DKFZ/ODCF Roddy workflows can be executed as if they are executed in our cluster, provided that all the additional data and software are mounted into the container.

The container can be build with

.. code-block:: bash

    docker build -t ghcr.io/theroddywms/dkfz_minimal:$version -f Dockerfile .

If you do not want to build the container yourself, you can also download a pre-built container from the GitHub Container Registry:

.. code-block:: bash

    docker pull ghcr.io/theroddywms/dkfz_minimal:$version

Finally, for running the container, you first have to convert it into a Singularity container:

.. code-block:: bash

    singularity build dkfz_minimal_$version.sif docker-daemon://ghcr.io/theroddywms/dkfz_minimal:$version

Place the ``dkfz_minimal_$version.sif`` in your cluster on a shared filesystem and enter the path as value for the ``containerImage`` configuration value.
