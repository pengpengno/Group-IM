package com.github.im.group.gui.config;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aot.AotDetector;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.ScannedGenericBeanDefinition;
import org.springframework.core.env.Environment;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternUtils;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.support.WebClientAdapter;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

/**
 * Description:
 * <p>
 *     Spring Framework does not currently support automatic registration of {@link HttpExchange},so manually register by
 *     create beans for each interface.
 *    base package is  {@link  HttpExchangeAutoRegister#HTTP_EXCHANGE_PACKAGE_PATH }
 *
 * </p>
 *
 * @author pengpeng
 * @version 1.0
 * @since 2024/11/26
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class HttpExchangeAutoRegister implements ApplicationContextAware {

    private static final String HTTP_EXCHANGE_PACKAGE_PATH = "com.github.im.group.gui.api";

    static final String DEFAULT_RESOURCE_PATTERN = "**/*.class";

    private static  HttpServiceProxyFactory httpServiceProxyFactory;

    @Override
    @SneakyThrows
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {

//        var isAotStarter = AotDetector.useGeneratedArtifacts();
//        if(isAotStarter){
//            // 判断下是否AOT 启动，则不注册
//            return;
//        }
//        BeanDefinitionRegistry registry = (BeanDefinitionRegistry) applicationContext.getAutowireCapableBeanFactory();
//        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
//        scanner.addIncludeFilter(new AnnotationTypeFilter(HttpExchange.class));
//
//        var environment = scanner.getEnvironment();
//        var basePackage = ClassUtils.convertClassNameToResourcePath(environment.resolveRequiredPlaceholders(HTTP_EXCHANGE_PACKAGE_PATH));
//
//        String packageSearchPath = ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX +
//                basePackage + '/' + DEFAULT_RESOURCE_PATTERN;
//
//        var defaultResourceLoader = new DefaultResourceLoader();
//
//        Resource[] resources = ResourcePatternUtils.getResourcePatternResolver(defaultResourceLoader)
//                .getResources(packageSearchPath);
//
//
//        for (Resource resource : resources) {
//            try {
//
//                MetadataReader metadataReader = scanner.getMetadataReaderFactory().getMetadataReader(resource);
//                var annotationMetadata = metadataReader.getAnnotationMetadata();
//                var hasHttpAnn = annotationMetadata.hasAnnotatedMethods(HttpExchange.class.getName());
//                if(!hasHttpAnn){
//                    //  not http exchange continue ;
//                    continue;
//                }
//                ScannedGenericBeanDefinition sbd = new ScannedGenericBeanDefinition(metadataReader);
//                sbd.setSource(resource);
//                // Load the interface class
//                var beanClassName = sbd.getBeanClassName();
//                Class<?> clazz = Class.forName(beanClassName);
//
//                var constainsProxyBean = !applicationContext.containsBean(HttpServiceProxyFactory.class.getSimpleName());
//                if (constainsProxyBean) {
//                    log.warn("HttpServiceProxyFactory 未初始化，无法注册 {}" , beanClassName);
//                    return;
//                }
//                // Get the HttpServiceProxyFactory bean from the context
////                HttpServiceProxyFactory factory = applicationContext.getBean(HttpServiceProxyFactory.class);
//                HttpServiceProxyFactory factory = applicationContext.getBean(HttpServiceProxyFactory.class);
//
//                // Create a proxy instance for the interface
////                Object proxy = factory.createClient(clazz);
////                Class<?> aClass = proxy.getClass();
//                // Register the proxy as a bean
//                var containsBeanDefinition = applicationContext.containsBeanDefinition(clazz.getSimpleName());
//                if(containsBeanDefinition){
//                    log.debug("already register {}",clazz.getSimpleName());
//                }
//
//
//                GenericBeanDefinition definition = new GenericBeanDefinition();
//                definition.setBeanClass(clazz); // Set the bean class to the interface
//                definition.setInstanceSupplier(() -> factory.createClient(clazz)); // Provide the proxy instance
//                var simpleName = clazz.getSimpleName();
//                registry.registerBeanDefinition(simpleName, definition);
//
//            } catch (ClassNotFoundException e) {
//                log.error("Failed to register @HttpExchange interface: " +resource.getFilename(), e);
//
////                throw new RuntimeException("Failed to register @HttpExchange interface: " +resource.getFilename(), e);
//            }
//        }
    }

}
