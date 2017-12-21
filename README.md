# Roddy?

Roddy is a framework for development and management of script based workflows on a batch processing cluster.

# Documentation

You can find our documentation under http://roddy-documentation.readthedocs.io

# Installation from release packages

Release ZIPs are available via [Github Releases](https://github.com/eilslabs/Roddy/releases). You first need a to set up some kind of runtime
environment into which you can install multiple versions of Roddy. Download the latest release of the RoddyEnv ZIP and unpack it.

After that you can install arbitrary releases of the Roddy ZIP into the dist/bin/ directory.   

# Build instructions

Roddy and its dependencies [BatchEuphoria](https://github.com/eilslabs/BatchEuphoria) and [RoddyToolLib](https://github.com/eilslabs/RoddyToolLib) use
the Gradle build system. Specifically, it uses the [composite build feature](https://docs.gradle.org/current/userguide/composite_builds.html) of 
Gradle. You can either let the gradle wrapper do all the work and download all dependecies including BatchEuphoria and RoddyToolLib for you, or you
get your own clones of the BatchEuphoria and RoddyToolLib git repos and reference them with the `--includeBuild` parameter like this

```bash
git clone https://github.com/eilslabs/RoddyToolLib.git
git clone https://github.com/eilslabs/BatchEuphoria.git
git clone https://github.com/eilslabs/Roddy.git
cd Roddy
./gradlew roddyDistZip --include-build ../RoddyToolLib/ --include-build ../BatchEuphoria/
./gradlew roddyEnvironmentDistZip --include-build ../RoddyToolLib/ --include-build ../BatchEuphoria/
```

The two example tasks build the distribution ZIPs for the Roddy core installed in `dist/bin/` and the runtime environment with the top-level starter
script `roddy.sh`. To build everything and run the unit and integration tests use: 

```bash
./gradlew build --include-build ../RoddyToolLib/ --include-build ../BatchEuphoria/
```

# IntelliJ Idea configuration

This is likely not the only way to configure your project with Gradle, but it's one that works.

If you are behind a proxy you should first configure the proxy for Gradle. Create `$HOME/.gradle/gradle.properties` with the appropriate settings. You can use the following template:

```
systemProp.http.proxyHost=
systemProp.http.proxyPort=
systemProp.https.proxyHost=
systemProp.https.proxyPort=
```

Then

1. Download and activate the Gradle-plugin of IntelliJ, if you have not done so already.
2. Open a new project. The project should be an "Empty Project".
3. Clone the `RoddyToolLib`, `BatchEuphoria` and `Roddy` into your new empty project. The `DefaultPlugin` and `PluginBase` plugins are required for some of the integration tests and should be present for most useful things you can do with Roddy.
    ```bash
    git clone https://github.com/eilslabs/RoddyToolLib
    git clone https://github.com/eilslabs/BatchEuphoria
    git clone https://github.com/eilslabs/Roddy
    mkdir -p Roddy/dist/plugins
    pushd Roddy/dist/plugins
    git clone https://github.com/eilslabs/Roddy-Default-Plugin.git DefaultPlugin
    git clone https://github.com/eilslabs/Roddy-Base-Plugin PluginBase
    popd
    ```
4. Import the five source repositories via "File" -> "Project Structure" -> "+" (Module pane). For import select the `build.gradle` from the specific repository.
5. Open the Gradle tasks window by clicking on the Gradle symbol on the task bar. If there is no Gradle symbol in the tool bars of IntelliJ, select "View" -> "Tool Windows" -> "Gradle".
6. Configure the composite Gradle builds by right-clicking on the gradle project.
7. Now if you go to the Gradle toolbar and select the `build` target of Roddy, RoddyToolLib, BatchEuphoria and Roddy itself will be build with Gradle.

## Setting up plugins in the project 

After these initial steps you can add your Roddy plugins to you project. We usually clone the plugin repositories into a dedicated `plugins_R2.4/` directory just beneath the root project directory (the now not so empty project that you initially created). This directory is then used for the `usePluginVersion` command-line option or in the `applicationProperties.ini`. The only exception are the `DefaultPlugin` and `PluginBase` that need to be in the `Roddy/dist/plugins` directory.

In IntelliJ then add the repository to your project as a module, ideally by directly importing the `.iml` file from the repository. Make sure that the plugin modules depends on the PluginBase, Roddy_main and maybe RoddyToolLib_main modules.

## Running Roddy from within IntelliJ

For running Roddy with parameters from IntelliJ you an "Application" configuration with "-enableassertions -Xms4m -Xmx50m" as VM options, the path to your `Roddy/` repository as working dir and `de.dkfz.roddy.Roddy` as Main class. When debugging plugin code you should use the plugin's repository root for "Use class path of module".
