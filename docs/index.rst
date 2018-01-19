.. Roddy documentation documentation master file, created by
   sphinx-quickstart on Mon Jul 17 12:03:00 2017.
   You can adapt this file completely to your liking, but it should at least
   contain the root `toctree` directive.

.. The toctree defines all documents in our site
..  toctree::
    :maxdepth: 3

    installationGuide
    exampleWorkflow
    usersGuide
    configuration
    roddyDevelopment
    pluginDevelopment

.. Here are the used hyperlinks in this document.
   They are mostly relative links
    developersGuide
.. _`GitHub project site`: https://github.com/eilslabs/Roddy
.. _`F.A.Q.`: https://github.com/eilslabs/Roddy/wiki/FAQ
.. _`Example workflow`: exampleWorkflow
.. _`Users Guide`: usersGuide
.. _`Plugin developers guide`: pluginDevelopersGuide
.. _`Developers guide`: developersGuide
.. _`RoddyToolLib`: https://github.com/eilslabs/RoddyToolLib
.. _`BatchEuphoria`: https://github.com/eilslabs/BatchEuphoria


The Roddy WMS
=============

What is Roddy
-------------

Roddy is a framework for development and management of script
based workflows on a batch processing cluster.

You can find the Roddy source code and its releases on our `GitHub project site`_


Key Features
------------

Roddy has several key features which make it a good choice to be used as
a base for workflows:

-  Multi-Level configuration system
-  Modular application design
-  Access to several cluster backends (via `BatchEuphoria`_)
-  Different versions of plugins/workflows and the Roddy core
   application are handled in a single installation
-  Various already implemented workflows
-  Callable stand-alone or integrable in other applications
-  Only a few dependencies and no database for the Roddy core
   application necessary
-  Various execution modes to support users to get their work done
   faster

The multi-layer configuration system and the handling of plugin versions
make Roddy particularly well suited for multi-user, multi-project
environments.

Where to start?
---------------

Take a look at the example workflow package: :doc:`exampleWorkflow`

Do you want to use it to run existing workflows? Then head over to the :doc:`usersGuide`

Do you want to develop workflows with it? Open up the :doc:`pluginDevelopment/pluginDevelopersGuide`

Do you want to develop it? See the :doc:`roddyDevelopment/developersGuide`

Do you have questions? Please visit the `F.A.Q.`_ section in our GitHub Wiki

License and associated projects
-------------------------------

Roddy is offered under an MIT based license.

We extracted from Roddy two possibly helpful open source libraries, again under MIT license:

- `RoddyToolLib`_ is a Java / Groovy library which provides several tools used in BatchEuphoria and Roddy. See the project description for more information.

- `BatchEuphoria`_ is a Java / Groovy library designed to offer easy access to cluster systems. Currently supported are PBS, SGE and LSF Rest

.. Indices and tables
.. /==================
.. /* ref:`genindex`
.. /* ref:`modindex`
.. /* ref:`search`
