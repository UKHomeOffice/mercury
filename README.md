Mercury for Scala
=================
TODO

Application built with the following (main) technologies:

- Scala

- SBT

- Akka

Application
-----------
The application is configured as per a typical Scala application, where the default configuration file is "application.conf" (or reference.conf).
This default file can be overridden with other "conf" files and then given to the application upon boot with the following example Java option:
```
-Dconfig.file=test-classes/application.test.conf
```

Individual configuration properties can be overridden again by Java options e.g. to override which Mongodb to connect:
```
-Dmongo.db=some-other-mongo
```

where this overrides the default in application.conf.

Build and Deploy
----------------
The project is built with SBT. On a Mac (sorry everyone else) do:
```
brew install sbt
```

It is also a good idea to install Typesafe Activator (which sits on top of SBT) for when you need to create new projects - it also has some SBT extras, so running an application with Activator instead of SBT can be useful. On Mac do:
```
brew install typesafe-activator
```

To compile:
```
sbt compile
```

or
```
activator compile
```

To run the specs:
```
sbt test
```

To actually run the application, you can simply:
```
sbt run
```

or first "assemble" it:
```
sbt assembly
```

This packages up an executable JAR - Note that "assembly" will first compile and test.

Then just run as any executable JAR, with any extra Java options for overriding configurations.

For example, to use a config file (other than the default application.conf) which is located on the file system (in this case in the boot directory)
```
java -Dconfig.file=test-classes/my-application.conf -jar <jar name>.jar
```

Note that the log configuration file could also be included e.g.
```bash
-Dlogback.configurationFile=path/to/my-logback.xml
```

So a more indepth startup with sbt itself could be:
```
sbt test:run -Dconfig.file=target/scala-2.11/test-classes/application.test.conf -Dlogback.configurationFile=target/scala-2.11/test-classes/logback.test.xml
```

Note the use of test:run. Usually we would only use "run", but as this is a library, there is no default "main" class, but we do have an example test "main" class.

And another example:

running from directory of the executable JAR using a config that is within said JAR:
```
java -Dconfig.resource=application.uat.conf -jar <jar name>.jar
```

Docker
------
Build an image (includes running tests and generating a fat JAR):
```
sbt docker
```

A built image can be pushed:
```
sbt dockerPush
```

To build and push an image:
```
sbt dockerBuildAndPush
```

To run and check the running service via its "health" endpoint:
```
sbt docker run
OR
docker run -p 9100:9100 uk.gov.homeoffice/mercury

curl http://localhost:9100/health
```

Noting that using "sbt docker run" will in fact do everything i.e. run tests, build image and run in container.

SBT - Revolver (keep things going while developing/testing)
-----------------------------------------------------------
sbt-revolver is a plugin for SBT enabling a super-fast development turnaround for your Scala applications:

See https://github.com/spray/sbt-revolver

For development, you can use ~re-start to go into "triggered restart" mode.
Your application starts up and SBT watches for changes in your source (or resource) files.
If a change is detected SBT recompiles the required classes and sbt-revolver automatically restarts your application. 
When you press &lt;ENTER&gt; SBT leaves "triggered restart" and returns to the normal prompt keeping your application running.

Gatling - Performance (Integration) Testing
-------------------------------------------
Performance tests are under src/it, and test reports are written to the "target" directory.

To execute Gatling performance integration tests from withing SBT:
```
gatling-it:test
```