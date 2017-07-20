Tools and filenames
===================

The whole workflow structure in Roddy is built around files and filenames.
Files are used to create dependencies between steps in the workflow and files
also enable Roddy to rerun a workflow based on created files.

As Roddy strictly separates code and configuration, filenames are configured.
Of course you are allowed to make exceptions for e.g. initial files but the
standard is to create rules for filenames.

So how do you tie things up?

Filename patterns are used to define a single or a range of names for a file class.

File classes are used as input and output parameters for tool entries.

Tool entries tell Roddy how a script or a binary is called. Which files and
parameters go in and which files come out and which resources will be used by
jobs running this tool.

.. Note::
    In our experience, it is a good way to create tools on a step by step base so that:

    1. You create a tool entry, define an initial resource set and i/o parameters.

    2. Integrate the call into your workflow.

    3. Setup filename patterns for the tools output files.

Tool entries
------------

.. code-block:: Xml

    <tool name='testScript' value='testScriptSleep.sh' basepath='roddyTests'>
      <resourcesets>
          <rset size="l" memory="1" cores="1" nodes="1" walltime="5"/>
      </resourcesets>
      <input type="file" typeof="SimpleTestTextFile" scriptparameter="FILENAME_IN"/>
      <output type="file" typeof="SimpleTestTextFile" scriptparameter="FILENAME_OUT"/>
    </tool>

Each tool entry has a header:

.. code-block:: Xml

    <tool name='testScript' value='testScriptSleep.sh' basepath='roddyTests'>

* The value of the *name* attribute is used to call or manage the tool in a workflow.

* The value of the *value* attribute holds the script or binary name of the executed file.

* The value of the *basepath* attribute points to the tools folder in the plugins analyisTools folder.

Resource sets
~~~~~~~~~~~~~

Each tool can have several resource sets.

.. code-block:: Xml

    <rset size="l" memory="1" cores="1" nodes="1" walltime="5"/>

* The attribute *size* can be one of *t, xs, s, m, l, xl* and allows you to define
  resource sets for different cases. From extra small to extra large. *t* is a special
  case and can be used for test resources.

* Currently Roddy (or BatchEuphoria) can be used to request the resources *memory, cores, nodes* and *walltime*
  You can set values in different formats:

    - The default for memory is 1GB. Valid strings for it are for example:

      * 1 (which is 1 GB)

      * 1m/g/t

      * 0.5(m/g/t) which would be 500MB

    - The default cores value is 1. Other values are natural numbers in [1; n]

    - The default nodes value is 1. Other values are natural numbers in [1; n]

    - The default walltime is 1 hour. Other values are for example:

      * 00:10:00 which would be 10 minutes.

      * 24:00:00 would be aligned to 01:00:00:00 which is one day. All other values
        will be aligned as well.

      * 1h, 1d, 1h50m ... or other values in human readable format.

  .. Note:: The default size for resource sets used by Roddy is *l*

Input types
~~~~~~~~~~~

Output types
~~~~~~~~~~~~

Filename patterns
-----------------

Automatic filenames
-------------------

Synthetic classes
-----------------
