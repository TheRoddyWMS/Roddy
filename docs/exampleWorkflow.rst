Example workflow
=================

If you want to try out Roddy, you can download our example workflow. The
workflow is wrapped inside a Docker container and you can use it to test
some Roddys functionality in a controlled environment. The workflow
itself is used for somatic small indel calling. It is based on Platypus
and accepts paired control and tumor BAM files. Output files are in
VCF format.

Installation
------------

Make sure, you have a running Docker environment! Open the `de.NBI`_ /
`HD-HuB`_ `ownCloud repository`_

Download the Docker images:

* The base image for our example: roddybaseimage.tar.gz
* The workflow image itself: roddyplatypus.tar.gz

and import them into your Docker environment.

Also download:

* The workflow dependencies: PlatypusIndelCallingWorkflowDependencies.tar.gz

* The scripts to run the workflow: PlatypusIndelCallingBundle.tar.gz

Create unpack the scripts file. The bundle directory will be created.
Unpack the dependencies file and move the folder
dependenciesPlatypusIndel/ to the bundle directory. Create a working
directory and give it access rights like chmod 777

Now you are nearly prepared and only need files which you can analyse.
For this example, you will need a control and a tumor bam file plus
their index files. The bam files need to be aligned with BWA (we used
versions >= 0.7.8) against hs37d5 and duplication marking should be
turned on.

Example usage
-------------

The docker container uses a slighty simplified Roddy syntax. Head into
the extracted bundle directory. There you will finde the roddy.sh
script.

You can call the script in the following way:

::

    bash roddy.sh (mode) (dataset id) (control bam) (tumor bam) (work directory)

So to just run the example:

::

    bash roddy.sh run TEST [PATH_TO_YOUR_CONTROL] [PATH_TO_YOUR_TUMOR] [PATH_TO_YOUR_WORKING_DIR]

If everything is setup properly, the Roddy docker will now start and
create run the workflow. The workflow will take several hours to finish,
so make sure to run it in e.g. a screen session-

References
----------

-  `Platypus`_
-  `Annovar`_
-  `dbSNP`_
-  `BWA`_

.. _de.NBI: https://www.denbi.de
.. _HD-HuB: https://www.hd-hub.de
.. _ownCloud repository: https://owncloud.hd-hub.de/index.php/s/3OSHDIY1STzX6Lu
.. _Platypus: http://www.well.ox.ac.uk/platypus
.. _Annovar: http://annovar.openbioinformatics.org/en/latest/user-guide/download/
.. _dbSNP: https://www.ncbi.nlm.nih.gov/SNP/
.. _BWA: http://bio-bwa.sourceforge.net
