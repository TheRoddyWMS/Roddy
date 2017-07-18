Plugin developers guide
=======================

This page should give you an idea and proper knowledge to start your own
Roddy based workflows.

Initially you should at least read the “Where to start” section.
Afterwards you can decide if you want either:

***“10 minutes of meaty meat”*** or ***“Get a new Roddy workflow running
in 10 minutes”***

or

***A full walkthrough***

Where to start
--------------

Have you already installed Roddy, a JDK and Groovy? If not:

-  Get a Roddy binary from BitBucket
-  Get the JDK 1.8.x => Install it and link it to
   ~/.roddy/runtimeDevel/jdk
-  Get the Groovy JDK 2.3.x => Install it and link it to
   ~/.roddy/runtimeDevel/groovy
-  Check if Roddy is running

Before you create a new workflow, you have to decide, which type of
workflow you want to create:

-  Brawl
-  Native
-  Java

And if you want to create a new plugin or if you want to use an existing
plugin.

Also you need to know how your workflow will be called.

What next? 10 minutes walkthrough
---------------------------------

Click `10MinutesOfMeat`_ to follow a quick walkthrough to get a simple
workflow running in around 10 minutes.

However, this short tutorial will teach you the very basics of Roddy
workflows and plugins but will not overwhelm you with too many details.
If you think that is not enough, head on to the full walkthrough.

What next? Full walkthrough
---------------------------

Let Roddy help you…
~~~~~~~~~~~~~~~~~~~

Call Roddy like this:

::

    lang=bash
    bash roddy.sh createnewworkflow PluginID[:dependencyPlugin] [native|brawl:]WorkflowID

-  Set //PluginID// to either an existing or a new Plugin.
-  Set //dependenyPlugin// to a parent plugin
-  Select if you want a Java, a native (Bash) or a Brawl workflow
-  Finally, set the workflows name with at //WorkflowID//

So e.g. create a Java workflow called FirstWorkflow in a plugin called
NewPlugin:

::

    lang=bash
    bash roddy.sh createnewworkflow NewPlugin FirstWorkflow

or e.g. create a Brawl workflow called SecondWorkflow in another plugin
and set it to depend on NewPlugin:

::

    lang=bash
    bash roddy.sh createnewworkflow AnotherPlugin:NewPlugin SecondWorkflow

***Oh I have something new now… but where is it?***

Good question, that totally depends on your application ini file and the
setup plugin directories. So look up the file and take a look into all
configured directories.

What, if I want to do that by myself?
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Sure, you can do that. If you want to create a new plugin, you need to:

-  Create the plugin directory
-  Create a README file, if you want one. The filename needs to be
   README.(workflowID).txt
-  Create a buildversion.txt file

   -

-  Create a buildinfo.txt file

   -  dependson=PluginBase:1.0.24
   -  dependson=COWorkflows:current

If you use an existing plugin for the new workflow, you need to create
the configuration files by either

1. copying and modifying an existing configuration or
2. creating a new configuration or

Ok, now let’s get to the meat!
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Short list of development steps
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

You can use the following guideline, which should list all necessary
steps i

.. _10MinutesOfMeat: