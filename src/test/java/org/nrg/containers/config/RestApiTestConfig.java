package org.nrg.containers.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.mockito.Mockito;
import org.nrg.xdat.security.services.RoleHolder;
import org.nrg.xdat.security.services.RoleServiceI;
import org.nrg.xdat.security.services.UserManagementServiceI;
import org.nrg.xnat.services.XnatAppInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolExecutorFactoryBean;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import java.util.List;

import static org.powermock.api.mockito.PowerMockito.when;

@Configuration
@Import({ObjectMapperConfig.class})
public class RestApiTestConfig extends WebMvcConfigurerAdapter {
    @Bean
    @Qualifier("mockXnatAppInfo")
    public XnatAppInfo mockAppInfo() {
        XnatAppInfo mockXnatAppInfo = Mockito.mock(XnatAppInfo.class);
        when(mockXnatAppInfo.isPrimaryNode()).thenReturn(true);
        return mockXnatAppInfo;
    }

    @Bean
    public ThreadPoolExecutorFactoryBean threadPoolExecutorFactoryBean() {
        ThreadPoolExecutorFactoryBean tBean = new ThreadPoolExecutorFactoryBean();
        tBean.setCorePoolSize(1);
        tBean.setThreadNamePrefix("container-test-");
        return tBean;
    }

    @Bean
    @Qualifier("mockRoleService")
    public RoleServiceI mockRoleService() {
        return Mockito.mock(RoleServiceI.class);
    }

    @Bean
    public RoleHolder mockRoleHolder(@Qualifier("mockRoleService") final RoleServiceI roleServiceI,
                                     final NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
        return new RoleHolder(roleServiceI, namedParameterJdbcTemplate);
    }

    @Bean
    public NamedParameterJdbcTemplate mockNamedParameterJdbcTemplate() {
        return Mockito.mock(NamedParameterJdbcTemplate.class);
    }

    @Bean
    public UserManagementServiceI mockUserManagementServiceI() {
        return Mockito.mock(UserManagementServiceI.class);
    }

    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        converters.add(new StringHttpMessageConverter());
        converters.add(new MappingJackson2HttpMessageConverter(objectMapper));
    }

    @Autowired
    private ObjectMapper objectMapper;
}
