package org.nofdev.servicefacade

import java.lang.reflect.Method

/**
 * Created by tengfei liu on 2017/5/7 0007.
 */
class ServiceMethod {
    //com.company.contact
    String interfacePackageName
    //com.company.contact.SmsFacade
    String interfacePath
    //SmsFacade
    String interfaceName
    //checkSmsCode
    String interfaceMethodName
    //com.company.contact.SmsFacade#checkSmsCode
    String interfaceMethodPath
    //com.company.contact.sms.SmsFacadeImpl
    Class targetClass
    //com.company.contact.sms.SmsFacadeImpl@3dfe520
    Object targetObj
    //public void com.company.contact.sms.SmsFacadeImpl.checkSmsCode(java.lang.String,com.company.contact.sms.SmsTypeEnum,java.lang.String)
    Method targetMethod
    //interface com.company.contact.SmsFacade
    Class interfaceClass
    //public abstract void com.company.contact.SmsFacade.checkSmsCode(java.lang.String,com.company.contact.sms.SmsTypeEnum,java.lang.String)
    Method interfaceMethod
    /**
     * 接口类型，Facade/Service
     */
    String interfaceType
    /**
     * 请求路径
     */
    String requestUrl
}
