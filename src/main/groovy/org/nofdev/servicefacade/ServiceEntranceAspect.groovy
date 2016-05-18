package org.nofdev.servicefacade

import com.fasterxml.jackson.databind.ObjectMapper
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.aspectj.lang.JoinPoint
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.*
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
/**
 * Created by Qiang on 5/18/16.
 */
@Aspect
@Component
@Slf4j
@CompileStatic
class ServiceEntranceAspect {

    @Autowired
    private ObjectMapper objectMapper

    @Pointcut("execution(public * org.nofdev.servicefacade.*Controller.json(..))")
    void entrancePointcut(){

    }

    @Around("entrancePointcut()")
    Object performance(ProceedingJoinPoint joinPoint){
        def args = joinPoint.args
        long startTime = System.currentTimeMillis()
        def object = joinPoint.proceed()
        long endTime = System.currentTimeMillis()
        def duration = endTime - startTime
        log.info("JSON facade call: ${args[0]}.${args[1]}.${args[2]} execute took ${duration} ms")
        object
    }

    //TODO 设置 ServiceContext 貌似不应该被 AOP
    @Before("entrancePointcut()")
    void mdcAndServiceContext(JoinPoint joinPoint){
        def serviceContext = extractServiceContent(joinPoint.args[4] as Map<String,String>)
        def callId = serviceContext?.getCallId()
        def thisId = UUID.randomUUID().toString()
        if (callId) {
            callId.parent = callId.id
            callId.id = thisId
        } else {
            callId = new CallId(id: thisId, root: thisId)
            serviceContext.setCallId(callId)
        }

        MDC.put(ServiceContext.CALLID.toString(), objectMapper.writeValueAsString(callId))
        ServiceContextHolder.setServiceContext(serviceContext)
    }

    @Before("entrancePointcut()")
    void requestLog(JoinPoint joinPoint){
        def args = joinPoint.args
        log.info("JSON facade call: ${args[0]}.${args[1]}.${args[2]} request with params ${args[3]} and headers ${args[4]}");
    }

    @AfterReturning(pointcut="entrancePointcut()",returning = "result")
    void responseLog(JoinPoint joinPoint, ResponseEntity<HttpJsonResponse> result){
        def args = joinPoint.args
        log.info("JSON facade call: ${args[0]}.${args[1]}.${args[2]} response with body ${objectMapper.writeValueAsString(result.getBody())} and headers ${result.getHeaders()}")
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
                //什么都不干
            }
        }
        serviceContext
    }
}
