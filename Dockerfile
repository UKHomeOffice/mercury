FROM java:openjdk-8u111

EXPOSE 9100

COPY ./target/scala-2.11/mercury-assembly-0.1-SNAPSHOT.jar /app/mercury.jar

CMD java -jar /app/mercury.jar
