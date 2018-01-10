package org.nofdev.servicefacade

import com.fasterxml.jackson.databind.ObjectMapper
import groovy.transform.CompileStatic
import org.aspectj.lang.JoinPoint
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.*
import org.nofdev.logging.CustomLogger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.annotation.Order
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component

/**
 * Created by Qiang on 5/18/16.
 */
@Aspect
@Component
@CompileStatic
@Order(-80)
class ServiceEntranceAspect {
    private static final CustomLogger log = CustomLogger.getLogger(ServiceEntranceAspect.class);

    @Autowired
    private ObjectMapper objectMapper

    @Pointcut("execution(public * org.nofdev.servicefacade.*Controller.json(..))")
    void entrancePointcut() {

    }

    @Around("entrancePointcut()")
    Object performance(ProceedingJoinPoint joinPoint) {
        def args = joinPoint.args
        long startTime = System.currentTimeMillis()
        def object = joinPoint.proceed()
        long endTime = System.currentTimeMillis()
        def duration = endTime - startTime
        log.debug("监控服务执行时长") {
            [
                    type    : "QoS",
                    duration: duration,
                    call    : "$args[0].$args[1].$args[2]"
            ]
        }
        object
    }


    @Before("entrancePointcut()")
    void requestLog(JoinPoint joinPoint) {
        def args = joinPoint.args
        log.debug("记录服务请求状态") {
            [
                    type   : "Request",
                    call   : "${args[0]}.${args[1]}.${args[2]}",
                    params : "${args[3]}",
                    headers: "${args[4]}"
            ]
        }
    }

    @AfterReturning(pointcut = "entrancePointcut()", returning = "result")
    void responseLog(JoinPoint joinPoint, ResponseEntity<HttpJsonResponse> result) {
        def args = joinPoint.args
        log.debug("记录服务响应状态") {
            [
                    type   : "Response",
                    call   : "${args[0]}.${args[1]}.${args[2]}",
                    body   : objectMapper.writeValueAsString(result.getBody()),
                    headers: result.getHeaders()
            ]
        }
    }

}
