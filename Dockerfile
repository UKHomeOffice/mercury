FROM hseeberger/scala-sbt

COPY . .

RUN sbt assembly

CMD java -jar /root/target/scala-2.11/mercury-assembly-0.1-SNAPSHOT.jar