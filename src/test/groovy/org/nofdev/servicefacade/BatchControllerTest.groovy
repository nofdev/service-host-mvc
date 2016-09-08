package org.nofdev.servicefacade

import groovy.util.logging.Slf4j
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.internal.matchers.NotNull
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import org.springframework.test.context.web.WebAppConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.result.MockMvcResultHandlers
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext

import java.nio.charset.Charset
import java.util.concurrent.TimeUnit

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

/**
 * Created by Liutengfei on 2016/7/20 0020.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = [WebAppContext])
@WebAppConfiguration
@Slf4j
class BatchControllerTest {
    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext

    private static MediaType APPLICATION_JSON_UTF8


    @Before
    public void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build()
        APPLICATION_JSON_UTF8 = new MediaType(MediaType.APPLICATION_JSON.getType(),
                MediaType.APPLICATION_JSON.getSubtype(),
                Charset.forName("utf8")
        );
    }

    @Test
    public void getVoid() {
        String params1 = URLEncoder.encode('[{"accountid":"1","nickname":"tom"}]', "UTF-8")
        String params2 = URLEncoder.encode('[{"accountid":"2","nickname":"jerry"}]', "UTF-8")
        String params3 = URLEncoder.encode('[{"accountid":"3","nickname":"bill"}]', "UTF-8")
        String params4 = URLEncoder.encode('[{"accountid":"4","nickname":"hhh"}]', "UTF-8")

        mockMvc.perform(get("/batch/json/org.nofdev.servicefacade/BatchDemo/consume?params=${params1}&params=${params2}&params=${params3}&params=${params4}"))
                .andExpect(status().isOk())//验证状态码
                .andExpect(content().contentType(APPLICATION_JSON_UTF8))
                .andExpect(header().string(ServiceContext.CALLID, NotNull.NOT_NULL))
                .andDo(MockMvcResultHandlers.print())//输出MvcResult到控制台

    }

    @Test
    public void getObj() {
        String params1 = URLEncoder.encode('[{"accountid":"1","nickname":"tom"}]', "UTF-8")
        String params2 = URLEncoder.encode('[{"accountid":"2","nickname":"jerry"}]', "UTF-8")
        String params3 = URLEncoder.encode('[{"accountid":"3","nickname":"bill"}]', "UTF-8")
        String params4 = URLEncoder.encode('[{"accountid":"4","nickname":"hhh"}]', "UTF-8")

        mockMvc.perform(get("/batch/json/org.nofdev.servicefacade/BatchDemo/consumeAndsupplier?params=${params1}&params=${params2}&params=${params3}&params=${params4}"))
                .andExpect(status().isOk())//验证状态码
                .andExpect(content().contentType(APPLICATION_JSON_UTF8))
                .andExpect(header().string(ServiceContext.CALLID, NotNull.NOT_NULL))
                .andDo(MockMvcResultHandlers.print())//输出MvcResult到控制台

    }

    @Test
    public void getList() {
        String params1 = URLEncoder.encode('[{"accountid":"1","nickname":"tom"}]', "UTF-8")
        String params2 = URLEncoder.encode('[{"accountid":"2","nickname":"jerry"}]', "UTF-8")
        String params3 = URLEncoder.encode('[{"accountid":"3","nickname":"bill"}]', "UTF-8")
        String params4 = URLEncoder.encode('[{"accountid":"4","nickname":"hhh"}]', "UTF-8")

        mockMvc.perform(get("/batch/json/org.nofdev.servicefacade/BatchDemo/getAllUsers?params=${params1}&params=${params2}&params=${params3}&params=${params4}"))
                .andExpect(status().isOk())//验证状态码
                .andExpect(content().contentType(APPLICATION_JSON_UTF8))
                .andExpect(header().string(ServiceContext.CALLID, NotNull.NOT_NULL))
                .andDo(MockMvcResultHandlers.print())//输出MvcResult到控制台

    }


}

class ProfileDTO {
    String accountid
    String nickname
}

interface BatchDemoFacade {
    void consume(ProfileDTO userDTO);

    ProfileDTO consumeAndsupplier(ProfileDTO userDTO);

    List<ProfileDTO> getAllUsers(ProfileDTO userDTO);
}

@Service
class BatchDemoFacadeService implements BatchDemoFacade {

    @Override
    void consume(ProfileDTO dto) {
        if (dto.accountid.equals("1")) {
            TimeUnit.SECONDS.sleep(12)
        } else if (dto.accountid.equals("2")) {
            TimeUnit.SECONDS.sleep(15)
        } else {
            TimeUnit.SECONDS.sleep(10)
        }
        if (dto.accountid.equals("3")) {
            throw new RuntimeException("出错了")
        }
    }

    @Override
    ProfileDTO consumeAndsupplier(ProfileDTO dto) {
        if (dto.accountid.equals("1")) {
            TimeUnit.SECONDS.sleep(12)
        } else if (dto.accountid.equals("2")) {
            TimeUnit.SECONDS.sleep(15)
        } else {
            TimeUnit.SECONDS.sleep(10)
        }
        if (dto.accountid.equals("3")) {
            throw new RuntimeException("出错了")
        }
        return dto
    }

    @Override
    List<ProfileDTO> getAllUsers(ProfileDTO dto) {
        return null
    }
}