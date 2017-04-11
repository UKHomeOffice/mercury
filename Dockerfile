FROM hseeberger/scala-sbt

COPY . .

COPY target/scala-2.11/mercury.jar /code/mercury.jar

EXPOSE 9100

ENTRYPOINT java -jar /code/mercury.jar