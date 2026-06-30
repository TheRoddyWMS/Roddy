#
# Copyright (c) 2023 German Cancer Research Center (DKFZ).
#
# Distributed under the MIT License (license terms are at https://github.com/TheRoddyWMS/AlignmentAndQCWorkflows).
#
if [ -e ~/.config/modules/_use_modules_5 ]; then
    # load Modules 5.x when this "semaphore file" exists
    . /software/modules/5.0.1/init/profile.sh
else
    export MODULE_VERSION=3.2.10
    export MODULEPATH=/software/.modules/sw:/software/.modules/meta:/software/.modules/workflows

    module() {
            eval `/tbi/software/x86_64/modules/modules-$MODULE_VERSION/el7/Modules/$MODULE_VERSION/bin/modulecmd bash $*`;
    }
    export -f module
fi
