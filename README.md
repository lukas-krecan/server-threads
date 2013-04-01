server-threads
==============

Experiment with Java Servlet threads and async servlets. It is possible to start the server using `mvn jetty:run`.

 Then it's possible to visit address [http://localhost:8080/threads/async?max=100](http://localhost:8080/threads/async?max=100) to test asynchronous behavior and
[http://localhost:8080/threads/sync?max=100](http://localhost:8080/threads/async?max=100) to test synchronous (please note that by default Jetty will not create
 more than 200 threads. Logs are written to `/tmp/server.log`.

 To configure Maven memory usage, use MAVEN_OPTS, for example `MAVEN_OPTS='-Xmx2048m -Xms2048m'`.

 Otherwise feel free to create WAR using `mvn package` and deploy it to a server of your choice.