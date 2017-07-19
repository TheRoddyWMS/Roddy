Filenames and tool entries
==========================

Roddy workflows and projects are configured with XML files. This
document will give you all the details you need to know about those
special files. Don’t be afraid of messing up things in configuration
files. Roddy checks at least a part (not everything) of the files, when
they get loaded and will inform you about structural errors as good as
possible.

Types of files
--------------

Roddy configuration files exist in three flavours: Project configuration
files, workflow or analysis configuration files and generic
configuration files. All file types may contain the same content type
though analysis configuration files will normally look different than
e.g. workflow configuration files. The main difference between the
different types is their place in the configuration tree that determines
the load order of configuration values, their filename and their header.

Filenames
---------

Inheritance structure
---------------------

Structure / Sections
--------------------

Header
~~~~~~

Configuration values
~~~~~~~~~~~~~~~~~~~~

Tool entries
~~~~~~~~~~~~

Filename patterns
~~~~~~~~~~~~~~~~~

Special: Autofilenames and Autofiletypes
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Enumerations
~~~~~~~~~~~~