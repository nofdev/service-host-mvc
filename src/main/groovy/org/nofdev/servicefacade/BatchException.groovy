package org.nofdev.servicefacade;

class BatchException extends AbstractBusinessException {
    static String DEFAULT_EXCEPTION_MESSAGE = "批处理异常";

    BatchException() {
        super(DEFAULT_EXCEPTION_MESSAGE)
    }

    BatchException(String message) {
        super(message)
    }

    BatchException(String message, Throwable cause) {
        super(message, cause)
    }

    BatchException(Throwable cause) {
        super(cause)
    }
}