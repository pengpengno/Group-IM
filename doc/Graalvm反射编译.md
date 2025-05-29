
GraalVM tracing-agent：



https://www.graalvm.org/22.0/reference-manual/native-image/Agent/
```shell
java -agentlib:native-image-agent=config-output-dir=META-INF/native-image -jar your-app.jar
```


protobuf in graalvm

https://stackoverflow.com/questions/77256791/using-protobuf-java-in-graalvm-native-image


configure with tracing agent 

https://www.graalvm.org/latest/reference-manual/native-image/guides/configure-with-tracing-agent/


# 编译数据
```shell
export PATH=$GRAALVM_HOME/bin:$PATH

```


### Gluon Issue 

use version  gluonfx: 1.0.25 
graalvm version ;


[Gluon Graalvm Release ](https://github.com/gluonhq/graal/releases)