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
    public static final String TOKEN = "X-Auth-Token"

    private static final CustomLogger log = CustomLogger.getLogger(ServiceContextAspect.class);

    @Autowired
    private ObjectMapper objectMapper

    @Pointcut("execution(public * org.nofdev.servicefacade.*Controller.json(..))")
    void entrancePointcut() {

    }

    @Around("entrancePointcut()")
    Object executionLogger(ProceedingJoinPoint joinPoint) {
        ServiceContextHolder.serviceContext.clear()
        extractServiceContent(joinPoint.args[4] as Map<String, String>)
        ServiceContextHolder.serviceContext.generateCallId()
        def result = joinPoint.proceed()
        result
    }

    private void extractServiceContent(Map<String, String> header) {
        def serviceContext = ServiceContextHolder.serviceContext

        String callIdToLowerCase = ServiceContext.CALLID.toString().toLowerCase()
        String prefixToLowerCase = ServiceContext.PREFIX.toString().toLowerCase()
        String tokenToLowerCase = TOKEN.toLowerCase()

        header.each { k, v ->

            String kToLowerCase = k.toLowerCase()

            if (kToLowerCase == callIdToLowerCase) {
                def callId = objectMapper.readValue(v, CallId.class)
                serviceContext.setCallId(callId)
            } else if (kToLowerCase.startsWith(prefixToLowerCase)) {
                serviceContext.put(k, v)
            } else if (kToLowerCase == tokenToLowerCase) {//TODO 增加可透传 header 设计
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
