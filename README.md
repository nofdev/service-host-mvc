# 基于 spring-mvc 的服务暴露 sdk

在 spring-boot-web 项目下测试通过

TODO 除了ExceptionSettings 其他都没用到 spring boot, 考虑下个版本删除

TODO 返回值对日期的序列化默认为 long 类型的时间戳, 如果要返回 ISO 类型的, 如果是 spring boot 项目, 在配置文件中增加`spring.jackson.serialization.write_dates_as_timestamps=false`