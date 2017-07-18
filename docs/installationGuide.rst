.. Links
.. _`GitHub project site`: https://github.com/eilslabs/Roddy
.. _`JRE v1.8.*`: https://java.com/de/download/linux_manual.jsp
.. _`JDK v1.8.*`: http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html
.. _`Groovy 2.4.*`: http://groovy-lang.org/download.html
.. _`Maven Groovy repository`: http://repo1.maven.org/maven2/org/codehaus/groovy/groovy-binary/

.. Document

Installation guide
==================

There are several minor versions of Roddy. They can be downloaded and installed in the same directory.
Minor versions mark changes in the Roddy API. This may or may not lead to incompatibilies of Roddy and Roddy plugins.
Installations for the different versions differ a bit, so we list all versions here.

Premises
--------
To install and run Roddy the following programs need to be installed on your computer:

- zip / unzip

- git

- bash

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

Roddy version 2.4 is installed in the same way as 2.3. In addition, there will be an automatic downloader for JRE / JDK and Groovy.
If you want to use this, you can skip the download steps.


.. [1] If you cannot find the necessary Groovy version, you can also download it from the _`Maven Groovy repository`

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



Test your installation
----------------------

Head over to the Roddy directory and do

::

  ./roddy.sh

If everything is properly done, Roddy will print its help screen.