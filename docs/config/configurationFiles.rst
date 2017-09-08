Configuration files
===================

Roddy currently supports two different types of configuration files:
- XML based which allows to use all configuration features
- Bash based which only allows a reduced set of configuration features

Normally, Roddy workflows and projects are configured with XML files.
This document will give you all the details you need to know about those
special files. Don’t be afraid of messing up things in configuration
files. Roddy checks at least a part (not everything) of the files, when
they get loaded and will inform you about structural errors as good as
possible.

Types of files
--------------

Roddy configuration files exist in three flavours:

- Project configuration files

- Workflow or analysis configuration files

- Generic configuration files.

All file types may contain the same content type
though analysis configuration files will normally look different than
e.g. project configuration files. The main difference between the
different types is their position in the configuration inheritance tree,
their filename and their header.

Filenames
---------

Roddy imposes some filename conventions to identify XML files when they
are loaded from disk:

-  Project configuration files look like projects*[yourfilename]*.xml
-  Workflow configuration files use the pattern
   analysis*[yourfilename]*.xml

Common configuration files do not use any pattern. You can name them
like you want, except for the above patterns.

.. Note: 

Inheritance structure
---------------------

Configurations and configuration files can be linked in several ways:

1. Subconfigurations extend their parent configuration(s)
2. Configuration files can import other configuration, this is only
   possible on the top-level of a configuration file, a subconfiguration
   cannot do this
3. Analysis configuration files can be imported as an analysis import by
   a project configuration or subconfiguration
4. An analysis can be imported by a project but not vice-versa

