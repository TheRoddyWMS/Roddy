XML configuration files
=======================

Structure / Sections
--------------------

Each configuration file is built up after the following pattern

::

    lang=xml
    <configuration name='test' description='Example.' ...>
        <availableAnalyses />
        <configurationvalues />
        <processingTools />
        <filenames ... />
        <enumerations>
        <subconfigurations />
    </configuration>

However, keep in mind, that not every section makes sense for every type
of XML file. E.g. //availableAnalyses// only makes sense in project XML
files, whereas filenames and processing tools will moste likely only be
used within analysis XML files.

Header
~~~~~~

Different file types use different XML headers. This is necessary to set
the different behaviours of those file types. Furthermore, for analysis
configuration files, headers differ for the various workflow types.
Let’s start with generic configuration files.

Brawl based workflows
^^^^^^^^^^^^^^^^^^^^^

\`\`\` lang=xml <configuration configurationType=‘analysis’
name=‘dellyAnalysisBrawl’ description=‘An example Brawl analysis.’
class=‘de.dkfz.roddy.core.Analysis’ brawlWorkflow=“BrawlTest”
brawlBaseWorkflow=“WorkflowUsingMergedBams”
imports=“commonCOWorkflowsSettings” listOfUsedTools=“script1,script2”
usedToolFolders="