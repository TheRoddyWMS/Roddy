Developers guide
----------------

Code guidelines
~~~~~~~~~~~~~~~
Roddy has no specific development or code style.
Here, we try to collect topics and settings, where we think that they might be important.

Code Format
^^^^^^^^^^^
We are mainly using IntelliJ IDEA and use the default settings for code formatting.

Collections as return types
^^^^^^^^^^^^^^^^^^^^^^^^^^^

By default, we do not return a copy (neither shallow, nor deep) of the Collection object. Be careful, not to modify the collection, if you do not change the contents of the object.

Keep it clean and simple
^^^^^^^^^^^^^^^^^^^^^^^^

We do not really enforce rules, but we try to keep things simple and readable.

- If a code block is not readable, try to make a method out of it.

- Reduce size and complexity of methods.

- Your code should be self explanatory. If it is not, try to make it that way.

We know, that we have a lot of issues in our codebase, but we listen to every improvement suggestion and constantly try to improve things.

Development model
~~~~~~~~~~~~~~~~~

For development we follow the standard git flow with feature branches
getting merged into the develop branch and merge into master branch upon
release. Currently we are discussing if we remove the development branch.
Roddys versioning system makes it easy to go back to previous versions.

Settings for Groovy classes
~~~~~~~~~~~~~~~~~~~~~~~~~~~

We will not accept Groovy classes without the @CompileStatic annotation. If you are in the rare situation that you need dynamic dispatch on more than
the object (this) itself, you can mark the affected methods with @CompileDynamic.

Roddy versioning scheme
~~~~~~~~~~~~~~~~~~~~~~~

Roddy version numbers consist of three entries: [major].[minor].[build].
These are added to the repository for releases.

The [major] entry is used to mark huge changes in the Roddy core
functions. Backward compatibility is most likely not granted and Roddy
will not execute plugins built with different [major] versions.

The [minor] entry marks smaller changes which might affect your plugin.
Backward compatibility might be affected and Roddy will warn you when a
plugin was built with another [minor] version. Only decrease this value,
when you increase the [major] version.

The [build] number is automatically increased when Roddy is packed or
compiled. You should only lower the build number, if you increase either
the [major] or [minor] version.

The combination of [major].[minor] can somehow be seen as the API level
of Roddy. For a “full API level” the plugin versions of “PluginBase” and
“DefaultPlugin” need to be considered as well.

If we have to maintain old plugin version with bugfixes or feature
backports for specific projects in production, then we extend the tag to
a full branch called “ReleaseBranch_$major.$minor.$build and tag the subversions with a "-$revision” suffix.

Below, you’ll find, how things are (or are supposed to be) handled in
git.

How to get started
~~~~~~~~~~~~~~~~~~

Have you already checked out the :doc:`../installationGuide`?
If not, please do so and do not forget to use the developer
settings instead of the user settings.

The first thing you'll need is a working Java 8+ installation and a Groovy installation (e.g. 2.4.9+).

Repository Structure
^^^^^^^^^^^^^^^^^^^^

::

    /
    roddy.sh                                          Top-level script
    ./RoddyCore/                                      The core project
        buildversion.txt                              Current buildversion
        Java/Groovy sources
    dist/
        bin/
            current/
            $major.$minor.$build/
        plugins/
        plugins_R$major.$minor/

Compiling Roddy
~~~~~~~~~~~~~~~

The preferred way to build Roddy is via Gradle. Please run

::

    ./gradlew build

This will download all necessary dependencies into the dist/bin/current/lib directory and create the Roddy.jar in dist/bin/current.

If you want to develop Roddy and additionally want to work on the RoddyToolLib or BatchEuphoria you can clone these libraries into neighbouring
directories and execute gradle in a composite build parameters

::

     ./gradlew build --include-build ../RoddyToolLib/ --include-build ../BatchEuphoria/


Packing Roddy
~~~~~~~~~~~~~

The packaging of Roddy is done using the gradle distribution plugin. There is two packaging targets

::

    ./gradlew roddyDistZip roddyEnvironmentDistZip

The distribution zips are put in the "gradleBuild/distribution" directory.

The "roddyEnvironmentDistZip" target will produce a zip with the top-level directory containing the roddy.sh and the essential dist/bin
subdirectories.

The "roddyDistZip" target will produce a zip with the content of the "dist/bin/current" directory. For deployment you should unzip it in that
directory and copy its content into an appropriately named dist/bin/ subdirectory, e.g. "current" for testing purposes or the version number, such as
2.4.10.
