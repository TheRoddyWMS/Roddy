#
# Copyright (c) 2026 German Cancer Research Center (DKFZ).
#
# Distributed under the MIT License (license terms are at https://github.com/TheRoddyWMS/Roddy).
#
# dkfz_minimal 1.2.0 = dkfz_minimal 1.1.0 plus test frameworks and code-quality
# tooling for all three languages the analysisTools use: Perl (the Test::More
# suite plus the advised modules -- Const::Fast, Type::Tiny, Perl::Tidy, ... --
# from Roddy docs/developers/CodingGuidelines.adoc, "Advised packages"), shell
# (shunit2) and Python (pytest). This lets the COWorkflowsBasePlugin test suites
# and static checks run against the production Perl (5.16.3). See
# COWorkflowsBasePlugin docs/developers/RunningTests.adoc. It is built as an
# increment on 1.1.0 rather
# than folded into the base dkfz_minimal Dockerfile, because that base build
# sources from the DKFZ-internal vault while these extras come from public CPAN.
#
# Why cpanm rather than `yum install perl-*`: several of these modules
# (Const::Fast, Type::Tiny, Perl::Tidy, String::Util) are not
# packaged as RPMs in the CentOS 7 base or EPEL 7 repositories, so a
# yum-by-name install cannot satisfy them. CPAN can. Build tooling (make/gcc/
# curl) is pulled from the PUBLIC CentOS 7.9 vault because the image's internal
# DKFZ vault mirror is not reachable outside the DKFZ network. Both CPAN and the
# public vault are reached through the DKFZ internet proxy -- build with
#   docker build --build-arg HTTP_PROXY=$HTTP_PROXY --build-arg HTTPS_PROXY=$HTTPS_PROXY \
#                --build-arg NO_PROXY=$NO_PROXY -t ghcr.io/theroddywms/dkfz_minimal:1.2.0 .
# (Docker injects the *_PROXY build-args into each RUN automatically.)
#
# Image name: ghcr.io/theroddywms/dkfz_minimal:1.2.0
#
FROM ghcr.io/theroddywms/dkfz_minimal:1.1.0

LABEL maintainer="Philip R. Kensche <p.kensche@dkfz-heidelberg.de>"

LABEL org.opencontainers.image.source="https://github.com/TheRoddyWMS/Roddy"

# Restore the core build toolchain + Test::More that the minimal base image
# strips out, install the advised Perl modules from CPAN, then remove the
# compiler again -- all in ONE layer, so gcc/make never persist in the shipped
# image. Build tooling comes from the public CentOS 7.9 vault (the base image's
# internal DKFZ mirror is unreachable off-network). The toolchain is added as
# per-module noarch packages on purpose: perl-core/perl-devel would pin the newer
# perl NVR and upgrade perl-libs, colliding with the base's older i686 perl-libs
# under yum's multilib protection, whereas noarch modules only need
# perl(:MODULE_COMPAT) and so touch neither perl nor perl-libs. cpanm installs
# into the world-readable system site_perl, so the modules resolve for the
# unprivileged user under rootless Apptainer; PERL_CPANM_HOME keeps cpanm's
# transient build cache in /tmp rather than root's home directory.
RUN printf '%s\n' \
      '[centos-vault]' \
      'name=CentOS-7.9.2009 vault (public)' \
      'baseurl=http://vault.centos.org/7.9.2009/os/$basearch/' \
      'gpgcheck=0' \
      'enabled=1' \
      > /etc/yum.repos.d/centos-vault.repo && \
    yum --disablerepo='*' --enablerepo='centos-vault' -y install make gcc curl \
        perl-ExtUtils-MakeMaker perl-ExtUtils-Install perl-ExtUtils-Manifest \
        perl-ExtUtils-CBuilder perl-ExtUtils-ParseXS perl-ExtUtils-Embed \
        perl-Module-Build perl-Test-Simple perl-Test-Harness perl-CPAN-Meta && \
    curl -fsSL https://cpanmin.us | PERL_CPANM_HOME=/tmp/cpanm perl - --notest --no-man-pages \
        Const::Fast \
        Try::Tiny \
        Type::Tiny \
        String::Util \
        Test::More \
        Test::Warn \
        Test::Exception \
        Perl::Tidy && \
    yum -y autoremove make gcc && \
    yum clean all && \
    rm -rf /var/cache/yum /tmp/cpanm /etc/yum.repos.d/centos-vault.repo

# Shell + Python test frameworks. shunit2 is a single self-contained script
# (pinned to a release tag, matching the COWorkflowsBasePlugin CI); SHUNIT2 is
# exported so runTests.sh finds it with no extra setup. pytest is pure Python;
# pip resolves a version compatible with the base image's Python 3.6. Neither
# touches yum, so this stays a small layer (no rpmdb copy-up). curl and pip3
# come from the base image / the layer above.
ENV SHUNIT2=/opt/shunit2/shunit2
RUN mkdir -p /opt/shunit2 && \
    curl -fsSL https://raw.githubusercontent.com/kward/shunit2/v2.1.8/shunit2 \
        -o "$SHUNIT2" && \
    chmod +x "$SHUNIT2" && \
    pip3 install --no-cache-dir pytest