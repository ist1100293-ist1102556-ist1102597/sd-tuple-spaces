FROM ubuntu:22.04

RUN apt-get update -y && apt-get upgrade -y

# Install git
RUN apt-get install -y git

# Install JDK
RUN apt-get install -y wget apt-transport-https gnupg
RUN wget -O - https://packages.adoptium.net/artifactory/api/gpg/key/public | apt-key add -
RUN echo "deb https://packages.adoptium.net/artifactory/deb $(awk -F= '/^VERSION_CODENAME/{print$2}' /etc/os-release) main" | tee /etc/apt/sources.list.d/adoptium.list
RUN apt-get update -y && apt-get install -y temurin-17-jdk

# Install Maven
RUN apt-get install -y maven

# Install Python
RUN apt-get install -y python3 python3-pip python3-venv

# Install Protocol Buffers
RUN apt-get install -y protobuf-compiler

# Cleanup
RUN apt-get autoremove -y && apt-get autoclean -y && apt-get clean