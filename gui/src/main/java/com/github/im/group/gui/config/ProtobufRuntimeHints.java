package com.github.im.group.gui.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import com.google.protobuf.GeneratedMessage;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.springframework.aot.hint.ExecutableMode;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;

import com.google.protobuf.GeneratedMessageV3;
import com.google.protobuf.ProtocolMessageEnum;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportRuntimeHints;

@Slf4j
public class ProtobufRuntimeHints implements RuntimeHintsRegistrar {
    
    private static final String PACKAGES_TO_SCAN_FILENAME = "META-INF/native-image/protobuf-packages.properties";

    @Override
    public void registerHints(RuntimeHints hint, ClassLoader classLoader) {
            
        // packages from file
        Set<String> packagesToScan = null;
        try {
             packagesToScan = loadPackagesToScan();
            logInfo("Loaded packages to scan:\n" + packagesToScan);         
        } catch (IOException e) {           
            throw new RuntimeException("Failed to load packages to scan", e);
        }
        
        // register
        for (String packageName : packagesToScan) {
            registerGrpcClassesFromReflection(hint, packageName);
        }
    }
    
    private Set<String> loadPackagesToScan() throws IOException {
        
        InputStream stream = this.getClass()
                .getClassLoader()
                .getResourceAsStream(PACKAGES_TO_SCAN_FILENAME);
        
        if (stream == null) {
            throw new RuntimeException("Resource not found: " + PACKAGES_TO_SCAN_FILENAME);
        }
        
        Properties props = new Properties();
        props.load(stream);
        return props.stringPropertyNames();
    }

    @SuppressWarnings("rawtypes")
    private static void registerGrpcClassesFromReflection(RuntimeHints hint, String packageName) {

        Reflections reflections = new Reflections(packageName, Scanners.SubTypes);
        Set<Class<? extends GeneratedMessageV3>> messageClasses = 
                    reflections.getSubTypesOf(GeneratedMessageV3.class);

        Set<Class<? extends GeneratedMessage>> messageClasses2 =
                reflections.getSubTypesOf(GeneratedMessage.class);

        Set<Class<? extends GeneratedMessageV3.Builder>> builderClasses = 
                reflections.getSubTypesOf(GeneratedMessageV3.Builder.class);

        Set<Class<? extends GeneratedMessage.Builder>> builderClasses2 =
                reflections.getSubTypesOf(GeneratedMessage.Builder.class);


        Set<Class<? extends ProtocolMessageEnum>> enums = 
                reflections.getSubTypesOf(ProtocolMessageEnum.class);       
        
        Set<Class<?>> classesToBeRegistered = new HashSet<>();
        classesToBeRegistered.addAll(messageClasses);
        classesToBeRegistered.addAll(builderClasses);
        classesToBeRegistered.addAll(messageClasses2);
        classesToBeRegistered.addAll(builderClasses2);
        classesToBeRegistered.addAll(enums);
        
        logInfo("Registering package [" + packageName + "], classes [" + classesToBeRegistered.size() + "]");
        for (Class<?> clazz : classesToBeRegistered) {
            registerClass(hint, clazz);
        }
    }
    
    private static void registerClass(RuntimeHints hints, Class<?> clazz) {
        
        String className = clazz.getName();
        try {
            // register class
            hints.reflection().registerType(clazz);
            
            // register all methods
            int methodsCount = 0;
            for (java.lang.reflect.Method method : clazz.getMethods()) {
                hints.reflection().registerMethod(method, ExecutableMode.INVOKE);
                methodsCount++;
            }

            logInfo("Registered class: [" + className + "], methods [" + methodsCount + "]");

        } catch (RuntimeException re) {
            logError("Failed to register class: [" + className + "] " + re.getMessage());
        }       
    }
    
    private static void logInfo(String msg) {
        msg = "ProtobufReflectionHints: " + msg;
        log.info(msg);
        System.out.println(msg);
    }

    private static void logError(String msg) {
        msg = "ProtobufReflectionHints: " + msg;
        log.error(msg);
        System.err.println(msg);
    }

    public static void main(String[] args) {
            RuntimeHints hints = new RuntimeHints();
        new ProtobufRuntimeHints().registerHints(hints, ProtobufRuntimeHints.class.getClassLoader());
    }
}