FROM  phusion/baseimage:0.9.20

RUN locale-gen en_US.UTF-8
ENV LANG="en_US.UTF-8" \
    LANGUAGE="en_US:en" \
    LC_ALL="en_US.UTF-8"

ENV JAVA_VER=8\
    JAVA_HOME=/usr/lib/jvm/java-8-oracle \
    ANT_VERSION=1.9.7 \
    MAVEN_VERSION=3.3.9

RUN echo 'deb http://ppa.launchpad.net/webupd8team/java/ubuntu xenial main' >> /etc/apt/sources.list  \
    && echo 'deb-src http://ppa.launchpad.net/webupd8team/java/ubuntu xenial main' >> /etc/apt/sources.list  \
    && apt-key adv --keyserver keyserver.ubuntu.com --recv-keys C2518248EEA14886 \
    && add-apt-repository universe \
    && apt-get update \

    && apt-get install -y python3-pip python3-click libxml2-dev libxslt1-dev \
    && pip3 install lxml \
    && apt-get install -y openssh-client \
    && apt-get install -y git \

    && echo oracle-java${JAVA_VER}-installer shared/accepted-oracle-license-v1-1 select true | /usr/bin/debconf-set-selections \
    && apt-get install -y --force-yes --no-install-recommends \
        oracle-java${JAVA_VER}-installer oracle-java${JAVA_VER}-set-default oracle-java8-unlimited-jce-policy \
    && rm -rf /var/cache/oracle-jdk-installer \
    && update-java-alternatives -s java-${JAVA_VER}-oracle \
    && echo "export JAVA_HOME=/usr/lib/jvm/java-${JAVA_VER}-oracle" >> ~/.bashrc \
    && echo "export JAVA_TOOL_OPTIONS=\"-Dfile.encoding=UTF-8\"" >> ~/.bashrc \

    && apt-get clean && rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/*

RUN cd && \
    wget -q http://archive.apache.org/dist/ant/binaries/apache-ant-${ANT_VERSION}-bin.tar.gz && \
    tar -xzf apache-ant-${ANT_VERSION}-bin.tar.gz && \
    mv apache-ant-${ANT_VERSION} /opt/ant && \
    rm apache-ant-${ANT_VERSION}-bin.tar.gz
ENV ANT_HOME=/opt/ant \
    PATH="${PATH}:/opt/ant/bin"

#RUN apt-get install -y maven
RUN cd && \
    wget -q http://apache.volia.net/maven/maven-3/${MAVEN_VERSION}/binaries/apache-maven-${MAVEN_VERSION}-bin.tar.gz && \
    tar -xzf apache-maven-${MAVEN_VERSION}-bin.tar.gz && \
    mv apache-maven-${MAVEN_VERSION} /opt/maven && \
    rm apache-maven-${MAVEN_VERSION}-bin.tar.gz

ENV M2_HOME=/opt/maven
ENV M2="${M2_HOME}/bin"
ENV MAVEN_OPTS="-Xmx512m -XX:MaxPermSize=256m" \
    PATH="${PATH}:$M2"

