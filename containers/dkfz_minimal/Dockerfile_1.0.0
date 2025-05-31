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



# Capitalized versions for many tools. Minuscle version at least for apt.
ARG HTTP_PROXY=""
ARG http_proxy="$HTTP_PROXY"
ARG HTTPS_PROXY=""
ARG https_proxy="$HTTPS_PROXY"
ARG NO_PROXY=""
ARG no_proxy="$NO_PROXY"

RUN echo "skip_missing_names_on_install=False" >> /etc/yum.conf && \
    yum -y update && \
    yum -y install epel-release && \
    yum repolist && \
    yum install -y \
      file \
      which \
      psmisc \
      pbzip2 \
      pigz \
      bzip2 \
      gzip \
      unzip \
      zip \
      p7zip \
      zlib \
      ghostscript \
      libpng12 \
      libXpm \
      poppler-utils \
      openmpi \
      glpk \
      perl-XML-Twig \
      fftw-devel \
      libgit2 \
      harfbuzz-icu \
      mbuffer \
      blas \
      openblas \
      openblas-serial \
      openblas-static \
      lapack \
      perl-Data-Dumper \
      perl-Config-Simple \
      perl-Env \
      procmail \
      bc \
      udunits2 \
      netcdf \
      perl-List-MoreUtils \
      perl-Parallel-ForkManager \
      perl-DateTime \
      gsl \
      libwebp \
      openjpeg2 \
      python3-pip \
      zstd \
      jq \
      mariadb-libs-5.5.68-1.el7.i686  \
      mesa-libGLU-9.0.0-4.el7.i686 \
      tree \
      gnuplot \
      tk \
      tcl \
      readline \
      udunits2-devel \
      strace && \
    yum clean all && \
    rm -rf /var/cache/yum

# Create a world readable/writable directory. Everybody (in the container) must be able to create
# scratch directories in this.
RUN mkdir /scratch && chmod a+rwX /scratch

COPY etc/profile.d/odcf-env.sh /etc/profile.d/odcf-env.sh
COPY etc/profile.d/modules.sh  /etc/profile.d/modules.sh
