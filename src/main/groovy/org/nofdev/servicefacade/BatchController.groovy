package org.nofdev.servicefacade

import com.fasterxml.jackson.databind.JavaType
import com.fasterxml.jackson.databind.ObjectMapper
import groovy.transform.CompileStatic
import org.nofdev.exception.ParamsException
import org.nofdev.logging.CustomLogger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.util.ReflectionUtils
import org.springframework.web.bind.annotation.*

import java.lang.reflect.Type
import java.lang.reflect.UndeclaredThrowableException
import java.util.concurrent.CompletableFuture

/**
 * Created by Liutengfei on 2016/7/19 0019.
 */
@RestController
@RequestMapping("/batch")
@CompileStatic
class BatchController {

    private static final CustomLogger log = CustomLogger.getLogger(BatchController.class)

    @Autowired
    private ObjectMapper objectMapper

    @Autowired
    private ExceptionSettings exceptionSettings

    @Autowired
    private ApplicationContext context

    @Autowired(required = false)
    private Authentication authentication

    @RequestMapping("json/{packageName}/{interfaceName}/{methodName}")
    ResponseEntity<HttpJsonResponse> json(@PathVariable String packageName,
                                          @PathVariable String interfaceName,
                                          @PathVariable String methodName,
                                          @RequestParam(value = "params", required = false) String[] params,
                                          @RequestHeader(required = false) Map<String, String> header) {
        def serviceContext = ServiceContextHolder.serviceContext

        HttpJsonResponse<Object> httpJsonResponse = new HttpJsonResponse<>()
        httpJsonResponse.setCallId(serviceContext?.getCallId()?.id)
        Object val = null
        ExceptionMessage exceptionMessage = new ExceptionMessage()
        HttpStatus httpStatus = HttpStatus.OK
        try {
            ServiceMethod serviceMethod = ServiceProviderAnnotationRegistrar.serviceMap.get("${packageName}.${interfaceName}#${methodName}".toString())
            if (serviceMethod == null) {
                throw new ServiceNotFoundException()
            }
            authentication?.tokenToUser(packageName, "${packageName}.${interfaceName}", methodName, params, header)

            Map result = new LinkedHashMap()
            List<CompletableFuture> futures = new ArrayList<>()
            exceptionMessage.children = new HashMap<>()
            for (int i = 0; i < params.length; i++) {
                final int index = i
                final String paramStr = params[index]
                final String key = String.valueOf(index)
                CompletableFuture future = CompletableFuture.supplyAsync({
                    Type[] types=serviceMethod.interfaceMethod.getGenericParameterTypes()
                    Object[] args=new Object[0]
                    if (types && paramStr != null && "null" != paramStr) {
                        args =deserialize(paramStr,types).toArray()
                    }
                    if(args.length!=(types?types.length:0)){
                        throw new ParamsException()
                    }
                    Object obj = ReflectionUtils.invokeMethod(serviceMethod.interfaceMethod, serviceMethod.targetObj,args)
                    return result.put(key, obj)
                }).exceptionally({ Throwable e ->
                    if (e instanceof UndeclaredThrowableException) e = e.cause
                    log.info("", e)
                    ExceptionMessage innerExceptionMessage = new ExceptionMessage()
                    innerExceptionMessage = formatException(innerExceptionMessage, e)
                    exceptionMessage.children.put(key, innerExceptionMessage)
                    return null
                })
                futures.add(future)
            }
            CompletableFuture allDoneFuture = CompletableFuture.allOf(futures.toArray(new CompletableFuture[futures.size()]))
            allDoneFuture.get()
            val = result
        } catch (AbstractBusinessException e) {
            log.info(e.getMessage(), e)
            exceptionMessage = formatException(exceptionMessage, e)
        } catch (Exception e) {
            log.error(e.getMessage(), e)
            exceptionMessage = formatException(exceptionMessage, new UnhandledException(e))
        }

        httpJsonResponse.setVal(val)
        httpJsonResponse.setErr(exceptionMessage)

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
        List methodParams = objectMapper.readValue(URLDecoder.decode(rawParams, "UTF-8"), List.class)
        List<Object> params = new ArrayList<>()
        for (int i = 0; i < methodParams.size(); i++) {
            log.debug("The converted param's type name") {
                [
                        convertedParamIndex   : i,
                        convertedParamTypeName: params?.get(i)?.getClass()?.getName()
                ]
            }
            JavaType javaType = objectMapper.getTypeFactory().constructType(paramTypes[i])
            params.add(objectMapper.convertValue(methodParams.get(i), javaType))
            log.debug("The converted param's type name") {
                [
                        convertedParamIndex   : i,
                        convertedParamTypeName: params?.get(i)?.getClass()?.getName()
                ]
            }
        }
        return params
    }

    private ExceptionMessage formatException(ExceptionMessage exceptionMessage, Throwable throwable) {
        if (throwable == null) return null
        if (throwable instanceof AbstractBusinessException) {
            exceptionMessage.setDetail(throwable?.detail)
        }
        exceptionMessage.setName(throwable.getClass().getName())
        exceptionMessage.setMsg(throwable.getMessage())
        exceptionMessage.setCause(formatException(new ExceptionMessage(), throwable.getCause()))
        if (exceptionSettings && exceptionSettings.getIsTraceStack()) {
            log.debug("The exception message will return trace info")
            try {
                StringWriter errors = new StringWriter()
                throwable.printStackTrace(new PrintWriter(errors))
                exceptionMessage.setStack(errors.toString())
            } catch (Exception e) {
                e.printStackTrace()
            }
        }
        return exceptionMessage
    }
}
