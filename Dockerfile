FROM hseeberger/scala-sbt

COPY . .

RUN sbt assembly

EXPOSE 9100

CMD java -jar /root/target/scala-2.11/mercury.jar