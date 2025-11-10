FROM cloudbees/cloudbees-core-mm:latest-jdk21 AS base

#FROM quay.io/centos/centos:stream9 AS build
FROM registry.access.redhat.com/ubi8:8.10 AS build

# cloudBees base is this one
#FROM redhat/ubi8-micro:8.10 AS build

COPY --from=base / /cbroot

# Add CentOS Stream 9 repo manually to access stress-ng
# Might not be cmpatible with UBI8
# COPY resources/appstream /etc/yum.repos.d/centos-stream.repo


RUN dnf update -y && \
    dnf install -y git gcc make procps-ng && \
    git clone https://github.com/ColinIanKing/stress-ng.git /tmp/stress-ng && \
    make -C /tmp/stress-ng && \
    make -C /tmp/stress-ng install DESTDIR=/cbroot


# Install over dnf wont work
## --allowerasing
#RUN dnf update -y && \
#    dnf install --installroot=/cbroot -y  --setopt=install_weak_deps=False stress-ng && \
#    dnf clean --installroot=/cbroot all && \
#    rm -f /etc/yum.repos.d/centos-stream.repo

FROM base
COPY --from=build /cbroot/ /
