package org.nofdev.servicefacade

import groovy.util.logging.Slf4j
import org.hamcrest.core.StringContains
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.internal.matchers.And
import org.mockito.internal.matchers.Not
import org.mockito.internal.matchers.NotNull
import org.mockito.internal.matchers.Or
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import org.springframework.test.context.web.WebAppConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext

import java.nio.charset.Charset

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

/**
 * Created by Qiang on 8/30/15.
 */
//@RunWith(MockitoJUnitRunner.class)
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = [WebAppContext])
@WebAppConfiguration
@Slf4j
class FacadeControllerTest {

    private MockMvc mockMvc

    @Autowired
    private WebApplicationContext webApplicationContext

    private static MediaType APPLICATION_JSON_UTF8


    @Before
    void setUp() {
//        mockMvc = MockMvcBuilders.standaloneSetup(facadeController).build()
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build()
        APPLICATION_JSON_UTF8 = new MediaType(MediaType.APPLICATION_JSON.getType(),
                MediaType.APPLICATION_JSON.getSubtype(),
                Charset.forName("utf8")
        )
    }

    @Test
    void testGet() {
        mockMvc.perform(get("/facade/json/org.nofdev.servicefacade/Demo/method1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON_UTF8))
                .andExpect(header().string(ServiceContext.CALLID, NotNull.NOT_NULL))
        //TODO 乱写一个地址的状态码是竟然200, 如/aa/facade/json/org.nofdev.servicefacade/Demo/method1
    }

    @Test
    void bugfixTestDeserialize() {
        mockMvc.perform(get("/facade/json/org.nofdev.servicefacade/Demo/getAllAttendUsers?params=[{}]"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON_UTF8))
                .andExpect(header().string(ServiceContext.CALLID, NotNull.NOT_NULL))
                .andExpect(content().string(StringContains.containsString("\"err\":null")))
    }

    @Test
    void bugfixTestDeserializeForNullParam() {
        mockMvc.perform(get("/facade/json/org.nofdev.servicefacade/Demo/getAllAttendUsers?params=[null]"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON_UTF8))
                .andExpect(header().string(ServiceContext.CALLID, NotNull.NOT_NULL))
                .andExpect(content().string(StringContains.containsString("\"err\":null")))
    }

    @Test
    void checkedExceptionWillUnwrapped() {
        mockMvc.perform(get("/facade/json/org.nofdev.servicefacade/Demo/throwsCheckedException"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON_UTF8))
                .andExpect(header().string(ServiceContext.CALLID, NotNull.NOT_NULL))
                .andExpect(content().string(
                    new And([
                            new Not(
                                    StringContains.containsString("UndeclaredThrowableException")
                            ),
                            StringContains.containsString("IOException"),
                            StringContains.containsString("throwsCheckedException"),
                    ])
                ))
    }
}


class UserDTO implements Serializable {
    /**
     * 姓名
     */
    String name
    /**
     * 年龄
     */
    Integer age
    /**
     * 生日
     */
    Date birthday
}

interface DemoFacade {
    String method1()

    void sayHello()

    List<UserDTO> getAllAttendUsers(UserDTO userDTO)

    void throwsCheckedException()
}

@Service
class DemoFacadeService implements DemoFacade {

    @Override
    void throwsCheckedException() {
        throw new IOException()
    }


    @Override
    String method1() {
        return 'Hello world'
    }

    @Override
    void sayHello() {

    }

    @Override
    List<UserDTO> getAllAttendUsers(UserDTO userDTO) {
        return null
    }
}

class TestException extends RuntimeException {
    TestException(String msg) {
        super(msg)
    }
}
