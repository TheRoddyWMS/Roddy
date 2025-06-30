#
# Copyright (c) 2023 German Cancer Research Center (DKFZ).
#
# Distributed under the MIT License (license terms are at https://github.com/TheRoddyWMS/AlignmentAndQCWorkflows).
#
# This container only contains the base software stack installed on the DKFZ compute nodes.
# The get the workflows running, you need to additionally provide the mounts of the software
# stack:
#
#    /tbi/software/ /software /software/modules/3.2.10
#
# Image name: ghcr.io/theroddywms/dkfz_minimal:$version
#
FROM centos:centos7.9.2009

LABEL maintainer="Philip R. Kensche <p.kensche@dkfz-heidelberg.de>"

LABEL org.opencontainers.image.source="https://github.com/TheRoddyWMS/Roddy"

LABEL org.opencontainers.image.description="CentOS 7.9 container mimicking an old ODCF-DKFZ cluster node."

# Capitalized versions for many tools. Minuscle version at least for apt.
ARG HTTP_PROXY=""
ARG http_proxy="$HTTP_PROXY"
ARG HTTPS_PROXY=""
ARG https_proxy="$HTTPS_PROXY"
ARG NO_PROXY=""
ARG no_proxy="$NO_PROXY"

# Copy reference configuration and repositories (originally from CentOS cluster nodes.
# The yum repositories are DKFZ internal. The public repositories have been deactivated. It is
# therefore not possible to build the container outside the DKFZ :(
COPY etc/yum.conf /etc/yum.conf
COPY etc/yum.repos.d/ /etc/yum.repos.d/
COPY etc/pki/ /etc/pki/
RUN rpm --import /etc/pki/rpm-gpg/*

RUN yum clean all
RUN yum makecache

COPY packages.txt /packages.txt

RUN echo "skip_missing_names_on_install=False" >> /etc/yum.conf && \
    yum -y update && \
    yum repolist && \
    yum install -y $(cat /packages.txt | grep -vP "^#") && \
    yum clean all && \
    rm -rf /var/cache/yum

# Create a world readable/writable directory. Everybody (in the container) must be able to create
# scratch directories in this.
RUN mkdir /scratch && chmod a+rwX /scratch

COPY etc/profile.d/odcf-env.sh /etc/profile.d/odcf-env.sh
COPY etc/profile.d/modules.sh  /etc/profile.d/modules.sh
