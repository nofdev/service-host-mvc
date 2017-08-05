# service-host-mvc

## What

It is a part of Nofdev RPC framework. 

It is a rpc server side util for java project based on spring mvc. 

## Maven

```
稳定版
<dependency>
    <groupId>org.nofdev</groupId>
    <artifactId>service-host-mvc</artifactId>
    <version>1.4.7</version>
</dependency>

```

## Notice

在 spring-boot-web 项目下测试通过

TODO 除了ExceptionSettings 其他都没用到 spring boot, 已经删除了 spring boot 的依赖, 所以 ExceptionSettings 无效

TODO 返回值对日期的序列化默认为 long 类型的时间戳, 如果要返回 ISO 类型的, 如果是 spring boot 项目, 在配置文件中增加`spring.jackson.serialization.write_dates_as_timestamps=false`, 这个理论上已经不需要了, 还没经过测试

## Samples

Refer: [https://github.com/yintai/generator-yintai-springboot](https://github.com/yintai/generator-yintai-springboot) sample test

### Request


```bash
curl -XGET https://demo.yintai.com/facade/json/com.yintai.demo/Demo/hello?params=["hello"]
```

### Response

```json
 {"val":"hello", "callId":"f354e509-46de-4b1f-9fe6-ec1b3fdd1917", "err":null}
```