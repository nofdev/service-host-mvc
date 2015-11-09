package org.nofdev.servicefacade;

import org.springframework.stereotype.Component;

/**
 * Created by wangxuesong on 15/8/14.
 * 是否打印堆栈的设置, 如果需要打印堆栈的话, 各应用要覆盖这个实现
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