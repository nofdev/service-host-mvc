package org.nofdev.servicefacade;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


/**
 * Created by wangxuesong on 15/8/14.
 */
@RestController
@RequestMapping("/facade")
public class FacadeController {


    private static final Logger logger = LoggerFactory.getLogger(FacadeController.class);

    @Autowired
    private ExceptionSettings excepitonSettings;

    @Autowired
    private ApplicationContext context;

    @RequestMapping("json/{packageName}/{interfaceName}/{methodName}")
    public ResponseEntity<HttpJsonResponse> json(@PathVariable String packageName,
                                                 @PathVariable String interfaceName,
                                                 @PathVariable String methodName,
                                                 @RequestParam(value = "params", required = false) String params) {
        HttpJsonResponse<Object> httpJsonResponse = new HttpJsonResponse<Object>();
        httpJsonResponse.setCallId(UUID.randomUUID().toString());
        httpJsonResponse.setVal(packageName);
        if (!interfaceName.endsWith("Facade")) {
            interfaceName += "Facade";
        }
        interfaceName = packageName + '.' + interfaceName;
        logger.info("JSON facade call(callId:{}): {}.{}{}", httpJsonResponse.getCallId(), interfaceName, methodName, params);
        Object val = null;
        ExceptionMessage exceptionMessage = null;
        HttpStatus httpStatus = HttpStatus.OK;
        try {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            Class<?> interfaceClazz = classLoader.loadClass(interfaceName);
            Object service = context.getBean(interfaceClazz);

            if(service==null){
                throw new UnhandledException();//TODO 这里要抛出一个404
            }

            Method[] methods = ReflectionUtils.getAllDeclaredMethods(interfaceClazz);
            Method method = null;
            for (Method m : methods) {
                if (methodName.equals(m.getName())) {
                    method = m;
                    break;
                }
            }
            if (method != null) {
                if (params != null && !"null".equals(params)) {
                    val = ReflectionUtils.invokeMethod(method, service, deserialize(params, method.getGenericParameterTypes()).toArray());
                } else {
                    val = ReflectionUtils.invokeMethod(method, service);
                }
            }else {
                throw new UnhandledException();//TODO 这里要抛出一个404
            }
        } catch (AbstractBusinessException e) {
            logger.info(e.getMessage(), e);
            httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
            exceptionMessage = formatException(e);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
            exceptionMessage = formatException(new UnhandledException(e));
        }

        httpJsonResponse.setVal(val);
        httpJsonResponse.setErr(exceptionMessage);

        ResponseEntity<HttpJsonResponse> responseEntity = new ResponseEntity<HttpJsonResponse>(httpJsonResponse, httpStatus);
        return responseEntity;
    }

    private List deserialize(String rawParams, Type[] paramTypes) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);

        List methodParams = objectMapper.readValue(rawParams, List.class);
        List<Object> params = new ArrayList<Object>();
        for (int i = 0; i < methodParams.size(); i++) {
            logger.debug("The param {}'s type name is {}", i, paramTypes[i].toString());
            JavaType javaType = objectMapper.getTypeFactory().constructType(paramTypes[i]);
            params.add(objectMapper.convertValue(methodParams.get(i), javaType));
            logger.debug("The converted param {}'s type name is {}", i, params.get(i).getClass().getName());
        }
        return params;
    }

    private ExceptionMessage formatException(Throwable throwable) {
        if (throwable == null) return null;
        ExceptionMessage exceptionMessage = new ExceptionMessage();
        exceptionMessage.setName(throwable.getClass().getName());
        exceptionMessage.setMsg(throwable.getMessage());
        exceptionMessage.setCause(formatException(throwable.getCause()));
        if (excepitonSettings.getIsTraceStack()) {
            logger.debug("The exception message will return trace info");
            try {
                exceptionMessage.setStack(new ObjectMapper().writeValueAsString(throwable.getStackTrace()));
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        }
        return exceptionMessage;
    }

}