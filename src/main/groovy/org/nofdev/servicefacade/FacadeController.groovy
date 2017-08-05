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

/**
 * Created by wangxuesong on 15/8/14.
 */
@RestController
@RequestMapping("/facade")
@CompileStatic
class FacadeController {

    private static final CustomLogger logger = CustomLogger.getLogger(FacadeController.class)

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
                                          @RequestParam(value = "params", required = false) String params,
                                          @RequestHeader(required = false) Map<String, String> header) {
        if (!interfaceName.endsWith("Facade")) {
            interfaceName += "Facade"
        }

        def serviceContext = ServiceContextHolder.serviceContext
        HttpJsonResponse<Object> httpJsonResponse = new HttpJsonResponse<>()
        httpJsonResponse.setCallId(serviceContext?.getCallId()?.id)
        Object val = null
        ExceptionMessage exceptionMessage = null
        HttpStatus httpStatus = HttpStatus.OK
        try {
            ServiceMethod serviceMethod = ServiceProviderAnnotationRegistrar.serviceMap.get("${packageName}.${interfaceName}#${methodName}".toString())
            if (serviceMethod == null) {
                throw new ServiceNotFoundException()
            }
            Object[] args = new Object[0]
            Type[] types=serviceMethod.interfaceMethod.getGenericParameterTypes()
            if (types && params != null && "null" != params) {
                args = deserialize(params, types).toArray()
            }
            if(args.length!=(types?types.length:0)){
                throw new ParamsException()
            }
            authentication?.tokenToUser(packageName, "${packageName}.${interfaceName}", methodName, params, header)
            val = ReflectionUtils.invokeMethod(serviceMethod.interfaceMethod, serviceMethod.targetObj, args)
        } catch (AbstractBusinessException e) {
            logger.info(e.getMessage(), e)
            exceptionMessage = formatException(e)
        } catch (UndeclaredThrowableException e) {
            def inner = e.cause
            def message = "Checked exception throws in ${packageName}.${interfaceName}#${methodName}: ${inner.class.canonicalName}: ${inner.message}"
            logger.error(message, inner)
            exceptionMessage = formatException(inner, message)
        } catch (Exception e) {
            logger.error(e.getMessage(), e)
            exceptionMessage = formatException(new UnhandledException(e))
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
        httpHeaders.add("Access-Control-Allow-Origin", "*")
        httpHeaders.add("Access-Control-Allow-Methods", "GET,HEAD,PUT,POST,DELETE")

        def responseEntity = new ResponseEntity<HttpJsonResponse>(httpJsonResponse, httpHeaders, httpStatus)
        return responseEntity
    }

    private List deserialize(String rawParams, Type[] paramTypes) throws IOException {
        List methodParams = objectMapper.readValue(rawParams, List.class)
        List<Object> params = new ArrayList<>()
        for (int i = 0; i < methodParams.size(); i++) {
            logger.debug("The param's type name") {
                [
                        paramIndex   : i,
                        paramTypeName: paramTypes[i]?.toString()
                ]
            }
            JavaType javaType = objectMapper.getTypeFactory().constructType(paramTypes[i])
            params.add(objectMapper.convertValue(methodParams.get(i), javaType))
            logger.debug("The converted param's type name") {
                [
                        convertedParamIndex   : i,
                        convertedParamTypeName: params?.get(i)?.getClass()?.getName()
                ]
            }
        }
        return params
    }

    private ExceptionMessage formatException(Throwable throwable, String message = null) {
        if (throwable == null) return null
        ExceptionMessage exceptionMessage = new ExceptionMessage()
        if (throwable instanceof AbstractBusinessException) {
            exceptionMessage.setDatail(throwable?.datail)
        }
        exceptionMessage.setName(throwable.getClass().getName())
        exceptionMessage.setMsg(message ?: throwable.getMessage())
        exceptionMessage.setCause(formatException(throwable.getCause()))
        if (exceptionSettings && exceptionSettings.getIsTraceStack()) {
            logger.debug("The exception message will return trace info")
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