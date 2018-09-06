Application structure
---------------------

The overall structure of a Roddy installation is as follows:

::

    /
    roddy.sh                                          # Top-level script
    dist/
        bin/
            develop/                                  # Optional development version
            $major.$minor.$build/
        plugins/                                      # Since Roddy 3
            DefaultPlugin
            PluginBase
        plugins_R$major.$minor/                       # Plugin directory for specific Roddy versions. Usually in mixed installations of 2.3 and 2.4
        runtimeDevel/                                 # Optional
            groovy-$major.$minor.$build
            jdk, jre, jdk_$major.$minor._$revision

