package org.nofdev.servicefacade

import org.springframework.aop.framework.AopProxyUtils
import org.springframework.context.ApplicationListener
import org.springframework.context.annotation.Lazy
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service

import java.lang.reflect.Method

/**
 *  Created by tengfei liu on 2017/5/2 0002.
 */
@Component
@Lazy(true)
public class ServiceProviderAnnotationRegistrar implements ApplicationListener<ContextRefreshedEvent> {
    public static final Map<String, ServiceMethod> serviceMap = new HashMap<>()

    @Override
    void onApplicationEvent(ContextRefreshedEvent event) {
        Map<String, Object> map = event.getApplicationContext().getBeansWithAnnotation(Service.class);
        map?.each { k, proxyTargetObj ->
            Class targetClazz = AopProxyUtils.ultimateTargetClass(proxyTargetObj)
            Service provider = targetClazz.getAnnotation(Service.class)
            Class<?>[] interfaces = targetClazz.getInterfaces()
            if (provider && interfaces) {
//                未来自定义注解的时候使用这一段
//                Class serviceInterface = provider.serviceInterface()
//                if (serviceInterface == Object.class || serviceInterface == null) {
//                    serviceInterface = targetClazz.getInterfaces()[0]
//                }

                Class serviceInterface = interfaces[0]
                String interfaceName = serviceInterface.getSimpleName()

                if (interfaceName.endsWith("Facade") || interfaceName.endsWith("Service")) {

                    String interfacePackageName = serviceInterface.getPackage().name
                    String interfacePath = serviceInterface.getTypeName()

                    Method[] interfaceMethods = serviceInterface.getMethods();
                    interfaceMethods?.each { Method interfaceMethod ->
                        String interfaceMethodName = interfaceMethod.getName()
                        String interfaceMethodPath = "${interfacePath}#${interfaceMethodName}"
                        String interfaceType = interfaceName.endsWith("Facade") ? "Facade" : "Service"

                        String interfaceSimpleName = interfaceName.substring(0, interfaceName.indexOf(interfaceType))
                        String requestUrl = "/${interfaceType.toLowerCase()}/json/${interfacePackageName}/${interfaceSimpleName}/${interfaceMethodName}"

                        serviceMap.put(interfaceMethodPath, new ServiceMethod(
                                requestUrl: requestUrl,
                                interfaceType: interfaceType,
                                interfacePackageName: interfacePackageName,
                                interfaceName: interfaceName,
                                interfacePath: interfacePath,
                                interfaceMethodName: interfaceMethodName,
                                interfaceMethodPath: interfaceMethodPath,
                                interfaceClass: serviceInterface,
                                interfaceMethod: interfaceMethod,
                                targetClass: targetClazz,
                                targetObj: proxyTargetObj,
                                targetMethod: targetClazz.getMethod(interfaceMethod.getName(), interfaceMethod.getParameterTypes())
                        ))
                    }
                }
            }
        }
    }
}