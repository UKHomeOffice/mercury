FROM quay.io/ukhomeofficedigital/openjdk8

ENV USER user_hocs_reporting
ENV GROUP group_hocs_reporting

RUN groupadd -r ${GROUP} && \
    useradd -r -g ${GROUP} ${USER} -d /code && \
    mkdir -p /code && \
    chown -R ${USER}:${GROUP} /code

COPY . .

COPY target/scala-2.11/mercury.jar /code/mercury.jar

# Set version numbers
ENV SCALA_VERSION 2.11.11
ENV SBT_VERSION 1.0.1
ENV SCALA_HOME /usr/share/scala
ENV SBT_HOME /usr/share/sbt

# Install APK dependencies
RUN apk add --no-cache wget ca-certificates bash && \
    update-ca-certificates && \
    apk add --no-cache openssl

# Install Scala
RUN cd /tmp && \
    wget "https://downloads.typesafe.com/scala/${SCALA_VERSION}/scala-${SCALA_VERSION}.tgz" && \
    tar xzf "scala-${SCALA_VERSION}.tgz" && \
    mkdir "${SCALA_HOME}" && \
    rm "/tmp/scala-${SCALA_VERSION}/bin/"*.bat && \
    mv "/tmp/scala-${SCALA_VERSION}/bin" "/tmp/scala-${SCALA_VERSION}/lib" "${SCALA_HOME}" && \
    ln -s "${SCALA_HOME}/bin/"* "/usr/bin/" && \
    rm -rf "/tmp/"*

# Install SBT
RUN cd /tmp && \
    wget "https://cocl.us/sbt-${SBT_VERSION}.tgz" && \
    tar xzf "sbt-${SBT_VERSION}.tgz" && \
    mv "/tmp/sbt" "${SBT_HOME}" && \
    ln -s "${SBT_HOME}/bin/"* "/usr/bin/" && \
    rm -rf "/tmp/"*

# download Java Cryptography Extension
#RUN cd /tmp && \
#    wget "http://download.oracle.com/otn-pub/java/jce/8/jce_policy-8.zip" --header='Cookie: oraclelicense=accept-securebackup-cookie' && \
#    unzip jce_policy-8.zip && \
#    rm jce_policy-8.zip && \
#    yes | cp -v /tmp/UnlimitedJCEPolicyJDK8/*.jar /usr/lib/jvm/java-8-openjdk-amd64/jre/lib/security/

EXPOSE 9100

USER ${USER}

ENTRYPOINT java -jar /code/mercury.jar
