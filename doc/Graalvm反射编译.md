
GraalVM tracing-agent：



https://www.graalvm.org/22.0/reference-manual/native-image/Agent/
```shell
java -agentlib:native-image-agent=config-output-dir=META-INF/native-image -jar your-app.jar
```


protbuf in graalvm

https://stackoverflow.com/questions/77256791/using-protobuf-java-in-graalvm-native-image


configure with tracing agent 

https://www.graalvm.org/latest/reference-manual/native-image/guides/configure-with-tracing-agent/