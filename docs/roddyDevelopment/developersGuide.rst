Developers guide
----------------

Code guidelines
~~~~~~~~~~~~~~~
Roddy has no specific development or code style.
Here, we try to collect topics and settings, where we think that they might be important.

Code Format
^^^^^^^^^^~
We are mainly using IntelliJ IDEA and use the default settings for code formatting.

Collections as return types
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

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

We will not accept Groovy classes without the @CompileStatic annotation.


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

Repository Structure
^^^^^^^^^^^^^^^^^^^^

::

    /
    roddy.sh                                          Top-level script
    ./RoddyCore                                       The core project
        buildversion.txt                              Current buildversion
        Java/Groovy sources
    dist/
        bin/
            current/
            $major.$minor.$build
        plugins/
        plugins_R$major.$minor
        runtimeDevel
            groovy-$major.$minor.$build
            jdk, jre, jdk_$major.$minor._$revision

Compiling Roddy
~~~~~~~~~~~~~~~

Currently, the compilation & packaging is implemented in the top-level
roddy.sh script that itself calls a number of scripts in the
dist/bin/current/helperScripts directory. On the long run we will
probably implement a Gradle-based re-implementation of the workflow.

Compiling Roddy is easy:

::

    bash roddy.sh compile

Will compile a new “current” version.


Packing Roddy
~~~~~~~~~~~~~

Similar to compile, Roddy has a pack option:

::

    bash roddy.sh pack

Will pack current to a directory called $major.$minor.$build.