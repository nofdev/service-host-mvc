# service-host-mvc

## What

It is a part of Nofdev RPC framework. 

It is a rpc server side util for java project based on spring mvc. 

## Notice

在 spring-boot-web 项目下测试通过

TODO 除了ExceptionSettings 其他都没用到 spring boot, 已经删除了 spring boot 的依赖, 所以 ExceptionSettings 无效

TODO 返回值对日期的序列化默认为 long 类型的时间戳, 如果要返回 ISO 类型的, 如果是 spring boot 项目, 在配置文件中增加`spring.jackson.serialization.write_dates_as_timestamps=false`, 这个理论上已经不需要了, 还没经过测试