#
# Copyright (c) 2023 German Cancer Research Center (DKFZ).
#
# Distributed under the MIT License (license terms are at https://github.com/TheRoddyWMS/AlignmentAndQCWorkflows).
#
# This container only contains the software stack installed on the DKFZ compute nodes as needed
# for the AlignmentAndQCWorkflow, IndelCallingWorkflow, ACEseqWorkflow, SophiaWorkflow, and
# RNAseqWorkflow.
#
FROM centos:centos7.9.2009_dkfz_minimal:1.0.0

LABEL maintainer="Philip R. Kensche <p.kensche@dkfz.de>"

LABEL org.opencontainers.image.source="https://github.com/TheRoddyWMS/Roddy"

# Capitalized versions for many tools. Minuscle version at least for apt.
ARG HTTP_PROXY=""
ARG http_proxy="$HTTP_PROXY"
ARG HTTPS_PROXY=""
ARG https_proxy="$HTTPS_PROXY"
ARG NO_PROXY=""
ARG no_proxy="$NO_PROXY"

# The virtualens and Conda envs
COPY /dkfz /dkfz

# The majority of the software stack managed with cluster modules.
COPY /software /software



FROM theroddywms/roddyjobbase:develop

LABEL maintainer="Philip R. Kensche <p.kensche@dkfz.de>"

LABEL org.opencontainers.image.source="https://github.com/DKFZ-ODCF/AlignmentAndQCWorkflow"

#ENV CONFIG?
#ENV SCRATCH_DIR
#ENV LSB_JOBID
#ENV LSB

# Install the module software and make it load.
#
# 1. Start a bash session, like the one created by the bsub command, namely without loading any
#    user configurations -- only the well controlled base-environment:
#
#    bash --noprofile --norc --init-file $baseEnvironmentScript
#
#    Now the environments are sourced in the same order as they are sourced by the wrapInScript.sh:
#
# 2. Source the job configuration script or at least create the variables needed for the environment
#    setup script (e.g. in particular the `..._VERSION` variables)
# !!!! The environment variables SCRATCH, LSB_..., etc. need to be present in the container!
# 3. Source the workflow-specific environment setup script
# 4. You can now run the dependency-paths-for-modules.py script to collect all relevant directories.
# 5. Remove from the list the directories which you think are irrelevant (e.g. `/usr`). The
#    roddyjobbase base container should contain all OS-level software, such that these do not
#    have to be copied.
# 6. Copy the remaining directories with they full path (from /) into the software/ subdirectory.
# 7. Make sure that there are no broken links! (use rsync???)
# 8. Build the container. The content of software/subdirectory will be copied to the / of the
#    image.
