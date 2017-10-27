.. Links
.. _`GitHub project site`: https://github.com/eilslabs/Roddy
.. _`JRE v1.8.*`: https://java.com/de/download/linux_manual.jsp
.. _`JDK v1.8.*`: http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html
.. _`Groovy 2.4.*`: http://groovy-lang.org/download.html
.. _`Maven Groovy repository`: http://repo1.maven.org/maven2/org/codehaus/groovy/groovy-binary/
.. _`GroovyServ` : https://kobo.github.io/groovyserv/

.. Document

Installation guide
==================

There are several minor versions of Roddy. They can be downloaded and installed in the same directory.
Minor versions mark changes in the Roddy API. This may or may not lead to incompatibilies of Roddy and Roddy plugins.
Installations for the different versions differ a bit, so we list all versions here.

Roddy uses Groovy, however, Groovy is a bit slow to start. So Roddy 2.4+ supports GroovyServ, which can be used by you to speed things up.
Roddy will try to install `GroovyServ`_ on its own. However, if that fails, you can still try to set it up on your own.
If it still does not work, you can also disable it.

Premises
--------
To install and run Roddy the following programs need to be installed on your computer:

- zip / unzip

- git

- bash

- lockfile (part of procmail mail-processing-package (v3.22))

As Roddy is Linux based, you will be able to find them in your package manager.

Roddy 2.2
---------
Will not be supported in the future. Releases are only available for legacy plugins.

Roddy 2.3
---------

1. Clone the repo and select the desired tag.

2. Step two depends on your role.

  - If you intend to use Roddy and do not want to develop plugins or Roddy itself:

    - Download any `JRE v1.8.*`_ (OpenJDK and SunJDK were tested). Also download `Groovy 2.4.*`_ [1]_

    - Open up the dist folder in the Roddy directory.

    - Create a folder named runtimeDevel

    - unzip / untar both archives in runtimeDevel

  - If you want to develop Roddy or Roddy plugins:

    - Download any `JDK v1.8.*`_ (OpenJDK and SunJDK were tested). Also download `Groovy 2.4.*`_ [1]_

    - Open up the dist folder in the Roddy directory.

    - Create a folder named runtimeDevel

    - unzip / untar both archives in runtimeDevel

3. Optionally unpack one or more of the release zips in *dist/bin/* directory.

Please see `Roddy version mix`_ for information about how to mix different versions of Roddy in the same directory.

Roddy 2.4
---------

For Roddy version 2.4 zips are deployed to Github releases (continuous deployment via Travis). The thus installed Roddy will contain all dependencies
but the JDK. Additionally, there is an automatic downloader for the JRE / JDK.

Roddy version mix
-----------------

Different Roddy versions can be co-installed the same installation folder.
Currently we do not offer prepackaged zip files, but you can easily assemble the version mix you need.

1. You need to install Roddy like in the description above.

2. Switch to the desired release tag.

3. Run Roddy with the pack option like

::

  ./roddy.sh pack

4. Switch back to master / develop and OR repeat steps 1 - 3 for additional Roddy versions.

If you take a look into your dist folder now, you'll see a new zip file and a folder with the proper version numbers.

Setup GroovyServ
----------------

As explained above, GroovyServ tremendously decreases the startup time of Groovy applications and Roddy will
try to download and set it up automatically. If that fails or if you want to set it up by yourself, do the following in your
Roddy directory:

.. code-block:: Bash

    mkdir -p dist/runtime
    cd dist/runtime

    # Download the GroovyServ binary zip archive from the `GroovyServ`_ download site,
    # unzip it and delete the archive afterwards.

    unzip groovyserv*.zip
    rm groovyserv*.zip

    # Last step, put Groovy and the Java binary folders to your PATH environment variable. This
    # is e.g. set in your ~/.bashrc file.

Now that's it. If you want to disable GroovyServ, you also do this.

.. code-block:: Bash

    mkdir -p dist/runtime
    cd dist/runtime
    touch gservforbidden

If you create the file, Roddy will not use GroovyServ.

.. Note::

    This setup was tested using GroovyServ 1.1.0!

Test your installation
----------------------

Head over to the Roddy directory and do

::

  ./roddy.sh

If everything is properly done, Roddy will print its help screen.
