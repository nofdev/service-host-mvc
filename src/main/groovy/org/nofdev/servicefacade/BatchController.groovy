package org.nofdev.servicefacade

import com.fasterxml.jackson.databind.JavaType
import com.fasterxml.jackson.databind.ObjectMapper
import groovy.transform.CompileStatic
import org.nofdev.exception.BatchException
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
import java.util.concurrent.CompletableFuture
import java.util.stream.Collectors
/**
 * Created by Liutengfei on 2016/7/19 0019.
 */
@RestController
@RequestMapping("/batch")
@CompileStatic
class BatchController {

    private static final CustomLogger log = CustomLogger.getLogger(BatchController.class);

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
                                                 @RequestParam(value = "params", required = false) String[] params,
                                                 @RequestHeader(required = false) Map<String, String> header) {
        def serviceContext = ServiceContextHolder.serviceContext

        HttpJsonResponse<Object> httpJsonResponse = new HttpJsonResponse<>();
        httpJsonResponse.setCallId(serviceContext?.getCallId()?.id);
        httpJsonResponse.setVal(packageName);
        interfaceName = packageName + '.' + interfaceName;
        Object val = null;
        ExceptionMessage exceptionMessage = new ExceptionMessage();
        HttpStatus httpStatus = HttpStatus.OK;
        try {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            Class<?> interfaceClazz = classLoader.loadClass(interfaceName);
            Object service = context.getBean(interfaceClazz);
            Class utltimate = AopProxyUtils.ultimateTargetClass(service)
            log.debug("To prevent exposing remote services, the service is ${service} and the service annotations are ${utltimate.annotations}")
            if (!service || !AnnotatedElementUtils.hasAnnotation(utltimate.class, Service.class)) {
                throw new BatchException();
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
                if(authentication){authentication.tokenToUser(packageName,interfaceName,methodName, params,header)}

                Map result = new LinkedHashMap()
                List<CompletableFuture> futures = new ArrayList<>()
                exceptionMessage.children = new HashMap<>()

                if (params != null && params.length > 0) {
                    for (int i = 0; i < params.length; i++) {
                        final int index = i;
                        final String paramStr = params[index]
                        final String key = String.valueOf(index)

                        CompletableFuture future = CompletableFuture.supplyAsync({
                            Object obj = ReflectionUtils.invokeMethod(method, service, deserialize(paramStr, method.getGenericParameterTypes()).toArray())
                            return result.put(key, obj)
                        }).exceptionally({ Throwable e ->
                            log.info("", e);
                            ExceptionMessage innerExceptionMessage = new ExceptionMessage()
                            innerExceptionMessage = formatException(innerExceptionMessage, e)
                            exceptionMessage.children.put(key, innerExceptionMessage)
                            return null;
                        });
                        futures.add(future)
                    }

                    CompletableFuture<Void> allDoneFuture = CompletableFuture.allOf(futures.toArray(new CompletableFuture[futures.size()]));
                    CompletableFuture<List> listFuture = allDoneFuture
                            .thenApply({ v -> futures.parallelStream().map({CompletableFuture f -> f.join() }).collect(Collectors.toList())
                    })
                    listFuture.get()
                    val = result
                } else {
                    throw new BatchException();
                }
            } else {
                throw new ServiceNotFoundException()
            }
        } catch (AbstractBusinessException e) {
            log.info(e.getMessage(), e);
//            httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
            exceptionMessage = formatException(exceptionMessage, e);
        } catch (BatchException e) {
            log.info(e.getMessage(), e);
//            httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
            exceptionMessage = formatException(exceptionMessage, e);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
//            httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
            exceptionMessage = formatException(exceptionMessage, new UnhandledException(e));
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

        def responseEntity = new ResponseEntity<HttpJsonResponse>(httpJsonResponse, httpHeaders, httpStatus)
        return responseEntity
    }


    private List deserialize(String rawParams, Type[] paramTypes) throws IOException {
        List methodParams = objectMapper.readValue(URLDecoder.decode(rawParams, "UTF-8"), List.class);
        List<Object> params = new ArrayList<>();
        for (int i = 0; i < methodParams.size(); i++) {
            log.debug("The converted param's type name") {
                [
                        convertedParamIndex   : i,
                        convertedParamTypeName: params?.get(i)?.getClass()?.getName()
                ]
            };
            JavaType javaType = objectMapper.getTypeFactory().constructType(paramTypes[i]);
            params.add(objectMapper.convertValue(methodParams.get(i), javaType));
            log.debug("The converted param's type name") {
                [
                        convertedParamIndex   : i,
                        convertedParamTypeName: params?.get(i)?.getClass()?.getName()
                ]
            };
        }
        return params;
    }

    private ExceptionMessage formatException(ExceptionMessage exceptionMessage, Throwable throwable) {
        if (throwable == null) return null;
        if (throwable instanceof AbstractBusinessException){
            exceptionMessage.setDatail(throwable?.datail)
        }
        exceptionMessage.setName(throwable.getClass().getName());
        exceptionMessage.setMsg(throwable.getMessage());
        exceptionMessage.setCause(formatException(new ExceptionMessage(), throwable.getCause()));
        if (exceptionSettings && exceptionSettings.getIsTraceStack()) {
            log.debug("The exception message will return trace info");
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
