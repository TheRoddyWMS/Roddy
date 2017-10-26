Cheat sheet
===========

This page is for those amongst you, that need to rush in or just need a
fresreshment, when it comes to Roddy usage. We will mostly list useful
commands and that’s it. No big explanations or other things. If you
need this, open up the :doc:`walkthrough`.

Where?
~~~~~~

/icgc/ngs\_share/ngsPipelines/RoddyStable/roddy.sh

Create a new project
~~~~~~~~~~~~~~~~~~~~

::

    lang=bash
    bash roddy.sh prepareprojectconfig create [targetprojectfolder]

# Open up the applicationProperties.ini. Change:

::

    - The cluster settings
    - Add the COProjectConfigurations path which you need.

# Open the XML file. Change:

::

    - The project id in the header
    - Add analyses you need (see user guide, last part)
    - Add / change values you need (e.g. I/O dir)

Test
~~~~

::

    lang=bash
    bash roddy.sh listdatasets [project]@[analysis] --useconfig=[yourinifile]

Testrun / Run
~~~~~~~~~~~~~

::

    lang=bash
    bash roddy.sh testrerun [project]@[analysis]  [id] --useconfig=[yourinifile]