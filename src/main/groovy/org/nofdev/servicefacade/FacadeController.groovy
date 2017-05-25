package org.nofdev.servicefacade

import com.fasterxml.jackson.databind.JavaType
import com.fasterxml.jackson.databind.ObjectMapper
import groovy.transform.CompileStatic
import org.nofdev.logging.CustomLogger
import org.springframework.aop.framework.AopProxyUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.core.annotation.AnnotatedElementUtils
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.util.ReflectionUtils
import org.springframework.web.bind.annotation.*

import java.lang.reflect.Method
import java.lang.reflect.Type

/**
 * Created by wangxuesong on 15/8/14.
 */
@RestController
@RequestMapping("/facade")
@CompileStatic
public class FacadeController {

    private static final CustomLogger logger = CustomLogger.getLogger(FacadeController.class);

    @Autowired
    private ObjectMapper objectMapper

    @Autowired
    private ExceptionSettings exceptionSettings;

    @Autowired
    private ApplicationContext context;

    @Autowired(required = false)
    private Authentication authentication;

    @RequestMapping("json/{packageName}/{interfaceName}/{methodName}")
    public ResponseEntity<HttpJsonResponse> json(@PathVariable String packageName,
                                                 @PathVariable String interfaceName,
                                                 @PathVariable String methodName,
                                                 @RequestParam(value = "params", required = false) String params,
                                                 @RequestHeader(required = false) Map<String, String> header) {
        def serviceContext = ServiceContextHolder.serviceContext

        HttpJsonResponse<Object> httpJsonResponse = new HttpJsonResponse<>();
        httpJsonResponse.setCallId(serviceContext?.getCallId()?.id);
        httpJsonResponse.setVal(packageName);
        if (!interfaceName.endsWith("Facade")) {
            interfaceName += "Facade";
        }
        interfaceName = packageName + '.' + interfaceName;
        Object val = null;
        ExceptionMessage exceptionMessage = null;
        HttpStatus httpStatus = HttpStatus.OK;
        try {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            Class<?> interfaceClazz = classLoader.loadClass(interfaceName);
            Object service = context.getBean(interfaceClazz);
            Class utltimate = AopProxyUtils.ultimateTargetClass(service)
            logger.debug("To prevent exposing proxied remote services, the service is ${service} and the service annotations are ${utltimate.annotations}")
            if (!service || !AnnotatedElementUtils.hasAnnotation(utltimate.class, Service.class)) {
                throw new ServiceNotFoundException();
            }

            Method[] methods = interfaceClazz.getMethods();
            Method method = null;
            for (Method m : methods) {
                if (methodName.equals(m.getName())) {
                    method = m;
                    break;
                }
            }
            if (method != null) {
                if (authentication) {
                    authentication.tokenToUser(packageName, interfaceName, methodName, params, header)
                }

                if (params != null && !"null".equals(params)) {
                    val = ReflectionUtils.invokeMethod(method, service, deserialize(params, method.getGenericParameterTypes()).toArray());
                } else {
                    val = ReflectionUtils.invokeMethod(method, service);
                }
            } else {
                throw new ServiceNotFoundException();
            }
        } catch (AbstractBusinessException e) {
            logger.info(e.getMessage(), e);
//            httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
            exceptionMessage = formatException(e);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
//            httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
            exceptionMessage = formatException(new UnhandledException(e));
        }

        httpJsonResponse.setVal(val);
        httpJsonResponse.setErr(exceptionMessage);

        def httpHeaders = new HttpHeaders()

        serviceContext.each { k, v ->
            if (k == ServiceContext.CALLID) {
                httpHeaders.add(k, objectMapper.writeValueAsString(v))
            } else {
                httpHeaders.add(k, v?.toString())
            }
        }
        httpHeaders.add("Access-Control-Allow-Origin", "*")
        httpHeaders.add("Access-Control-Allow-Methods", "GET,HEAD,PUT,POST,DELETE")

        def responseEntity = new ResponseEntity<HttpJsonResponse>(httpJsonResponse, httpHeaders, httpStatus)
        return responseEntity
    }

    private List deserialize(String rawParams, Type[] paramTypes) throws IOException {
        List methodParams = objectMapper.readValue(rawParams, List.class);
        List<Object> params = new ArrayList<>();
        for (int i = 0; i < methodParams.size(); i++) {
//            logger.debug("The param {}'s type name is {}", i, paramTypes[i]?.toString());
            logger.debug("The param's type name") {
                [
                        paramIndex   : i,
                        paramTypeName: paramTypes[i]?.toString()
                ]
            };
            JavaType javaType = objectMapper.getTypeFactory().constructType(paramTypes[i]);
            params.add(objectMapper.convertValue(methodParams.get(i), javaType));
            logger.debug("The converted param's type name") {
                [
                        convertedParamIndex   : i,
                        convertedParamTypeName: params?.get(i)?.getClass()?.getName()
                ]
            };
//            logger.debug("The converted param {}'s type name is {}", i, params?.get(i)?.getClass()?.getName());
        }
        return params;
    }

    private ExceptionMessage formatException(Throwable throwable) {
        if (throwable == null) return null;
        ExceptionMessage exceptionMessage = new ExceptionMessage();
        if (throwable instanceof AbstractBusinessException) {
            exceptionMessage.setDatail(throwable?.datail)
        }
        exceptionMessage.setName(throwable.getClass().getName());
        exceptionMessage.setMsg(throwable.getMessage());
        exceptionMessage.setCause(formatException(throwable.getCause()));
        if (exceptionSettings && exceptionSettings.getIsTraceStack()) {
            logger.debug("The exception message will return trace info");
            try {
                StringWriter errors = new StringWriter();
                throwable.printStackTrace(new PrintWriter(errors));
                exceptionMessage.setStack(errors.toString());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return exceptionMessage;
    }

}