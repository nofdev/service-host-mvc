package org.nofdev.servicefacade;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

@ControllerAdvice
public class GlobalControllerExceptionHandler {
    protected Logger logger;

    public GlobalControllerExceptionHandler() {
        logger = LoggerFactory.getLogger(getClass());
    }
    @ExceptionHandler
    @ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
    @ResponseBody
    public ExceptionMessage handleBusinessException(AbstractBusinessException e) throws Exception {
        logger.info(e.getMessage());
        if (AnnotationUtils.findAnnotation(e.getClass(), ResponseStatus.class) != null)
            throw e;
        return formatException(e);
    }

    @ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler
    @ResponseBody
    public ExceptionMessage exception(Throwable e) throws Exception {
        logger.error(e.getMessage(), e);
        return this.handleBusinessException(new UnhandledException(e.getMessage(), e.getCause()));
    }

    private ExceptionMessage formatException(Throwable throwable) {
        if (throwable == null) return null;
        ExceptionMessage exceptionMessage = new ExceptionMessage();
        exceptionMessage.setName(throwable.getClass().getName());
        exceptionMessage.setMsg(throwable.getLocalizedMessage());
        exceptionMessage.setCause(formatException(throwable.getCause()));
        return exceptionMessage;
    }
}