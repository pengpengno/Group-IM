
GRAALVM_LINK
https://github.com/gluonhq/graal/releases



>Gluon With Spring 

[Gluon With Spring ](https://github.com/cnico/GluonWithSpring)

GRAALVM COMPILE ISSUE  FOR REFERENCE
```shell

https://github.com/oracle/graal/issues/5678


https://stackoverflow.com/questions/73401121/spring-native-with-buildpacks-error-com-oracle-graal-pointsto-constraints-unreso
```




> https://docs.gluonhq.com/#_nativeimageargs
```
And as a result, hellofx.hellofx.o is created and can be found under target/gluonfx/aarch64-android/gvm/tmp/SVM-*/hellofx.hellofx.o.

Note that the process takes some time, so it is convenient to test first on desktop (and with HotSpot) as much as possible (i.e. with mvn gluonfx:run), so gluonfx:compile doesn’t have to be repeated due to avoidable errors.

Run mvn -Pandroid gluonfx:link to produce the native image. As a result, target/gluonfx/aarch64-android/libhellofx.so is created.

Finally, run mvn -Pandroid gluonfx:package to bundle the application into an Android APK that can be installed on a device and also to an Android App Bundle (AAB) that can be submitted to Google Play.
```


TURN 厂商 
[Xirsys](https://xirsys.com/)
[文档](https://docs.xirsys.com/?pg=api-turn)
