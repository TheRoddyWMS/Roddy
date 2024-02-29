#!/usr/bin/env bash
#
# Copyright (c) 2023 German Cancer Research Center (DKFZ).
#
# Distributed under the MIT License (license terms are at https://github.com/TheRoddyWMS/AlignmentAndQCWorkflows).
#
if [ -e ~/.config/modules/_use_modules_5 ]; then
    # load Modules 5.x when this "semaphore file" exists
    . /software/modules/5.0.1/init/profile.sh
else
    # pre-$CMD hack for zsh
    ## export modules_shell=zsh

    # Get current shell.  Hack for zsh as this shell returns
    # this script as $0 instead of the current shell
    #
    CMD=`ps --no-header -p $$ -o comm`


    # pre-$CMD loop for zsh
    # case "$0" in
    case "$CMD" in
              -sh|sh|*/sh)  export modules_shell=sh ;;
           -ksh|ksh|*/ksh)  export modules_shell=ksh ;;
           -zsh|zsh|*/zsh)  export modules_shell=zsh ;;
        -bash|bash|*/bash)  export modules_shell=bash ;;
    esac

    export MODULE_VERSION=3.2.10
    export MODULEPATH=/tbi/software/modules/packages:/tbi/software/modules/meta_modules:/software/.modules/sw:/software/.modules/meta

    module() {
            eval `/tbi/software/x86_64/modules/modules-$MODULE_VERSION/el7/Modules/$MODULE_VERSION/bin/modulecmd $modules_shell $*`;
    }

    # Only bash can export functions
    #
    if [ "$CMD" = "bash" ]; then
        export -f module
    fi
fi
