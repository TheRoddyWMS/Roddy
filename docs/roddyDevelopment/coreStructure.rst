Application structure
---------------------

::

    /
    roddy.sh                                          Top-level script
    dist/
        bin/
            develop/
            $major.$minor.$build/
        plugins/
            DefaultPlugin
            PluginBase
        plugins_R$major.$minor/                       Plugin directory for specific Roddy versions
        runtimeDevel/
            groovy-$major.$minor.$build
            jdk, jre, jdk_$major.$minor._$revision


The runtimeDevel/ directory is only required for Roddy up to version 2.3.