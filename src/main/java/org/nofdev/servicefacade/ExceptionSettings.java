package org.nofdev.servicefacade;

import org.springframework.stereotype.Component;

/**
 * Created by wangxuesong on 15/8/14.
 */
@Component
//@ConfigurationProperties(prefix = "org.nofdev.servicefacade.exception")
public class ExceptionSettings {

    private Boolean isTraceStack = false;

    public Boolean getIsTraceStack() {
        return isTraceStack;
    }

    public void setIsTraceStack(Boolean isTraceStack) {
        this.isTraceStack = isTraceStack;
    }
}
