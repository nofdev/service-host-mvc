package org.nofdev.servicefacade
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.joda.JodaModule
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
/**
 * Created by Qiang on 11/6/15.
 */
@Configuration
class JsonConfiguration {

    @Bean
    ObjectMapper objectMapper(){
        ObjectMapper objectMapper = new ObjectMapper()
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true)
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
        objectMapper.registerModule(new JodaModule());
    }
}
