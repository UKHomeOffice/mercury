FROM openjdk:8

COPY target/scala-2.11/mercury.jar /code/mercury.jar

WORKDIR /code

EXPOSE 9100

ENTRYPOINT java -jar /code/mercury.jar