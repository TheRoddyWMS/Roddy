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
To install and run Roddy the following programs need to be installed on your computer or the execution hosts:

- zip / unzip

- git

- bash

- lockfile (part of procmail mail-processing-package (v3.22), only needed on job execution hosts)

- A recent JDK (e.g. 1.8.0_121)

- Groovy (e.g. 2.4.7)

- The DefaultPlugin and the PluginBase (see below)

As Roddy is Linux based, you will be able to find them in your package manager. For the JDK and Groovy, which are both required on the host on which
you run Roddy, you may alternativel want to use [sdkman](http://sdkman.io/). The following will get you going:

```bash
curl -s "https://get.sdkman.io" | bash
sdk install groovy 2.4.13
sdk install java 8u151-oracle
```

Specific workflows you execute may require additional software.

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
except the JDK and Groovy. Additionally, there is an automatic downloader for the JRE / JDK.

1. Release ZIPs for Roddy and the Roddy environment are available via [Github Releases](https://github.com/eilslabs/Roddy/releases). Download the latest release of the RoddyEnv ZIP and unpack it and change into the Roddy environment directory (e.g. "Roddy").
2. After that you can install arbitrary releases of the Roddy ZIP into `dist/bin/$major.$minor.$patch` directories.
3. The default and base plugin repositories need to be cloned into the `dist/plugins/` directory.
   .. code-block:: Bash

   pushd dist/plugins
   git clone https://github.com/eilslabs/Roddy-Default-Plugin.git DefaultPlugin
   git clone https://github.com/eilslabs/Roddy-Base-Plugin PluginBase
   popd

Versioning
----------

The Roddy environment with the top-level "roddy.sh" allow you to co-install multiple Roddy versions. Simply install the different versions of Roddy,
e.g. from the release zips, into directories in "dist/bin" following the naming scheme "dist/bin/$major.$minor.$patch". The desired version can than
be selected during Roddy invocations using the "--useRoddyVersion" parameter.

Additionally, Roddy is capable of handling multiple versions of the same workflow plugin. Different versions of the default or base plugin need to be
installed in the "dist/plugins/" directory following the naming scheme "${workflowName}_$major.$minor.$patch[-$revision]", e.g. "DefaultPlugin_1.2.0".
Other plugins may be installed in arbitrary plugin directories using the same naming scheme.


[Optional] Setup GroovyServ
---------------------------

GroovyServ tremendously decreases the startup time of Groovy applications and Roddy will try to download and set it up automatically. If that fails or
if you want to set it up by yourself, do the following in your Roddy directory:

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
