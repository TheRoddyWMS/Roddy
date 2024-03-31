Singularity/Apptainer Job Containers
====================================

Since version 3.8.0 Roddy can execute jobs in Singularity/Apptainer containers.

When submitting a task to the cluster (or executing it locally) with Singularity or Apptainer, the ``wrapInScript.sh`` is not executed natively, but instead the call is wrapped in a Singularity call.

Configuration
-------------

There are a number of configuration values that need to be set to get this feature working:

  * ``jobExecutionEnvironment`` needs to be set to ``singularity`` or ``apptainer``.
  * ``containerEnginePath`` needs to be set to the path of the container engine. By default this is the name of the engine as provided in ``jobExecutionEnvironment``, e.g. "singularity". Thus it is assumed that the "singularity" executable is available in the environment (e.g. of ``bsub``).
  * ``containerImage`` needs to be set to the path of the container image.
  * ``containerMounts`` needs to be set to a list of paths to mount into the container. This should be of the format ``(mount1 mount2 mount3)``. Note that you don't have to add the ``inputBaseDirectory`` and ``outputBaseDirectory``, because these are added automatically. Be careful with symlinks, as Roddy does not resolve them (neither locally nor remotely).
  * ``apptainerArguments`` can be used to pass global parameters to ``apptainer exec``. For instance ``--contain``, which is advisable to unsure a proper separation between the compute nodes' environment and the analysis environment.

Singularity containers work with setting ``outputFileGroup``. We simply change the primary group *before* invoking the ``singularity`` command.

Furthermore, all variables that you can use in your bioinformatic job's top-level scripts that you use without Singularity containers, also are available in Singularity containers.
By contrast, only a very limited number of environment variables of the executing environment are available.
This is to achieve a good isolation of the container environment.

.. note::

    Singularity does not resolve symlinks, and Roddy does not help you to automatically resolve symlinks as mount points.
    The only exception are the ``inputBaseDirectory`` and ``outputBaseDirectory`` which are automatically mounted into the container.

    This means, if your need to access files you have to add them needed mounts to your ``containerMounts`` manually.

    Special care has to be taken for symlinks. For instance, if you access the tool ``x`` at path ``/your/software/x``, but ``software/`` is a symlink to path ``/anotherDir/software``, then you need to add both paths ``/your`` and ``/anotherDir`` to your ``containerMounts`` variable! Thus, both, the symlink origin ``/your/software`` and the symlink target ``/anotherDir/software`` are available in the container at the expected positions.

.. code-block:: XML

    <configurationvalues>
        <cvalue name="jobExecutionEnvironment" value="apptainer" type="string"
                description="Set this to 'bash' for native execution, 'apptainer', or 'singularity'. Default is 'bash'."/>
        <cvalue name="containerEnginePath" value="/usr/bin/apptainer" type="path"
                description="The path to the container engine. By default this is the name of the engine as provided in jobExecutionEnvironment."/>
        <cvalue name="containerImage" value="/path/to/job-containers/dkfz_minimal.sif" type="path"
                description="Path to a singularity/apptainer container."/>
        <cvalue name="containerMounts" value="( /your/software/ /software /your/virtualenvs/ /software/modules/3.2.10 /your/miniconda3:/containerInternal/miniconda3 /your/annotation/data /your/reference/genome:/containerInternal/ref:rw /true/symlinked/path )" type="bashArray"
                description="List of mount mount specifications. Can be comma-separated list (type='string') or a type='bashArray' (i.e. '(mount1 mount2 mount3)'). Paths must not contain any whitespaces (escaping or quoting is not implemented). Can be a specification of the form `/hostPath:/containerPath` or `/hostPath:/containerPath:rw`, similar to the format used by Apptainer. Note that you don't have to add the inputBaseDirectory and outputBaseDirectory, because these are added automatically. Be careful with symlinks, because Roddy does not resolve them, and filesystems mounted into other filesystems."/>
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

    cd $repoDir/containers/dkfz_minimal
    docker build -t ghcr.io/theroddywms/dkfz_minimal:$version -f Dockerfile .

If you do not want to build the container yourself, you can also download a pre-built container from the GitHub Container Registry:

.. code-block:: bash

    docker pull ghcr.io/theroddywms/dkfz_minimal:$version

Finally, for running the container, you first have to convert it into a Singularity container:

.. code-block:: bash

    singularity build dkfz_minimal_$version.sif docker-daemon://ghcr.io/theroddywms/dkfz_minimal:$version

Place the ``dkfz_minimal_$version.sif`` in your cluster on a shared filesystem and enter the path as value for the ``containerImage`` configuration value.

Build Your Own Container
------------------------

Roddy calls the singularity container with `singularity exec` or `apptainer exec` and exports few variables that are needed by the `wrapInScript.sh`.
The `wrapInScript.sh` has the following requirements

* Bash (prefer a recent version)
* `lockfile` (from `procmail`)
* Optionally `strace`

As long as your container starts the wrapper script with Bash and contains the `lockfile` command, it should work.
