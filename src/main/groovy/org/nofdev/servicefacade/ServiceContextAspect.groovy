package org.nofdev.servicefacade

import com.fasterxml.jackson.databind.ObjectMapper
import groovy.transform.CompileStatic
import org.aspectj.lang.JoinPoint
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Before
import org.aspectj.lang.annotation.Pointcut
import org.nofdev.logging.CustomLogger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component

/**
 * Created by Qiang on 16/11/2016.
 */
@Aspect
@Component
@CompileStatic
@Order(1)
class ServiceContextAspect {
    private static final CustomLogger log = CustomLogger.getLogger(ServiceContextAspect.class);

    @Autowired
    private ObjectMapper objectMapper

    @Pointcut("execution(public * org.nofdev.servicefacade.*Controller.json(..))")
    void entrancePointcut() {

    }

    //TODO 设置 ServiceContext 貌似不应该被 AOP
    @Before("entrancePointcut()")
    void serviceContext(JoinPoint joinPoint) {
        def serviceContext = extractServiceContent(joinPoint.args[4] as Map<String, String>)
        def callId = serviceContext?.getCallId()
        def thisId = UUID.randomUUID().toString()
        if (callId) {
            callId.parent = callId.id
            callId.id = thisId
        } else {
            callId = new CallId(id: thisId, root: thisId)
            serviceContext.setCallId(callId)
        }

//        MDC.put(ServiceContext.CALLID.toString(), objectMapper.writeValueAsString(callId))
        ServiceContextHolder.setServiceContext(serviceContext)
    }

    private ServiceContext extractServiceContent(Map<String, String> header) {
        def serviceContext = new ServiceContext()
        header.each { k, v ->
            if (k.toLowerCase() == ServiceContext.CALLID.toString().toLowerCase()) {
                def callId = objectMapper.readValue(v, CallId.class)
                serviceContext.setCallId(callId)
            } else if (k.toLowerCase().startsWith(ServiceContext.PREFIX.toString().toLowerCase())) {
                serviceContext.put(k, v)
            } else {
                log.trace("没有以 Service-Context 打头的 log 不会被放入上下文对象") {
                    [
                            type   : "ServiceContext",
                            headers: header?.toString()
                    ]
                }
            }
        }
        serviceContext
    }
}
