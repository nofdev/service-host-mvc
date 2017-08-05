package org.nofdev.servicefacade

import com.fasterxml.jackson.databind.ObjectMapper
import groovy.transform.CompileStatic
import org.aspectj.lang.JoinPoint
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
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
@Order(-100)
class ServiceContextAspect {
    private static final CustomLogger log = CustomLogger.getLogger(ServiceContextAspect.class);

    @Autowired
    private ObjectMapper objectMapper

    @Pointcut("execution(public * org.nofdev.servicefacade.*Controller.json(..))")
    void entrancePointcut() {

    }

    @Around("entrancePointcut()")
    Object executionLogger(ProceedingJoinPoint joinPoint) {
        try {
            ServiceContextHolder.serviceContext.clear()
            extractServiceContent(joinPoint.args[4] as Map<String, String>)
            ServiceContextHolder.serviceContext.generateCallId()
            return joinPoint.proceed()
        } finally {
            ServiceContextHolder.serviceContext.clear()
        }
    }

    private void extractServiceContent(Map<String, String> header) {
        def serviceContext = ServiceContextHolder.serviceContext
        header.each { k, v ->
            if (k.toLowerCase() == ServiceContext.CALLID.toString().toLowerCase()) {
                def callId = objectMapper.readValue(v, CallId.class)
                serviceContext.setCallId(callId)
            } else if (k.toLowerCase().startsWith(ServiceContext.PREFIX.toString().toLowerCase())) {
                serviceContext.put(k, v)
            } else if (k.toLowerCase() == 'x-auth-token') {//TODO 增加可透传 header 设计
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
    }
}
