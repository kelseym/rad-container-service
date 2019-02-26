package org.nrg.containers.config;

import java.util.concurrent.TimeUnit;

import org.nrg.containers.events.DockerStatusUpdater;
import org.nrg.framework.annotations.XnatPlugin;
import org.nrg.xnat.initialization.RootConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.scheduling.concurrent.ThreadPoolExecutorFactoryBean;
import org.springframework.scheduling.config.TriggerTask;
import org.springframework.scheduling.support.PeriodicTrigger;

import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;

@Configuration
@XnatPlugin(value = "containers",
        name = "containers",
        description = "Container Service",
        entityPackages = "org.nrg.containers",
        log4jPropertiesFile = "META-INF/resources/log4j.properties",
        version = ""
)
@ComponentScan(value = "org.nrg.containers",
        excludeFilters = @Filter(type = FilterType.REGEX, pattern = ".*TestConfig.*", value = {}))
@Import({RootConfig.class})
public class ContainersConfig {

	public static final int MAX_FINALIZING_LIMIT = 5;

	@Bean
	public ThreadPoolExecutorFactoryBean threadPoolExecutorFactoryBean() {
		//return new ThreadPoolExecutorFactoryBean();
		ThreadPoolExecutorFactoryBean tBean = new ThreadPoolExecutorFactoryBean();
		tBean.setCorePoolSize(MAX_FINALIZING_LIMIT);
		tBean.setThreadNamePrefix("container-finalizing-");
		return tBean;
	}
	

	
	@Bean
    public Module guavaModule() {
        return new GuavaModule();
    }
	

    @Bean
    public ObjectMapper objectMapper(final Jackson2ObjectMapperBuilder objectMapperBuilder) {
        return objectMapperBuilder.build();
    }

    @Bean
    public TriggerTask dockerEventPullerTask(final DockerStatusUpdater dockerStatusUpdater) {
        return new TriggerTask(
                dockerStatusUpdater,
                new PeriodicTrigger(10L, TimeUnit.SECONDS)
        );
    }
    

}