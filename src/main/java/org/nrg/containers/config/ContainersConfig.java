package org.nrg.containers.config;

import java.util.concurrent.TimeUnit;

import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;

import org.apache.activemq.command.ActiveMQQueue;
import org.nrg.containers.events.DockerStatusUpdater;
import org.nrg.framework.annotations.XnatPlugin;
import org.nrg.xnat.initialization.RootConfig;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.scheduling.concurrent.ThreadPoolExecutorFactoryBean;
import org.springframework.scheduling.config.TriggerTask;
import org.springframework.scheduling.support.PeriodicTrigger;

import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;

@EnableJms
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

	@Bean
	public ThreadPoolExecutorFactoryBean threadPoolExecutorFactoryBean() {
	    final int nthread = 5;
	    ThreadPoolExecutorFactoryBean tBean = new ThreadPoolExecutorFactoryBean();
	    tBean.setCorePoolSize(nthread);
	    tBean.setThreadNamePrefix("container-threadpool-");
	    return tBean;
	}

	@Bean
 	public DefaultJmsListenerContainerFactory containerListenerContainerFactory( @Qualifier("springConnectionFactory")
                                                                                 ConnectionFactory connectionFactory ) {
            DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
            factory.setConnectionFactory(connectionFactory);
            factory.setConcurrency("10-20");
            return factory;
    }

    @Bean(name = "containerStagingRequest")
    public Destination containerStagingRequest(@Value("containerStagingRequest") String containerStagingRequest) throws JMSException {
        return new ActiveMQQueue(containerStagingRequest);
    }

	@Bean(name = "containerFinalizeRequest")
	public Destination containerFinalizeRequest(@Value("containerFinalizeRequest") String containerFinalizeRequest) throws JMSException {
		return new ActiveMQQueue(containerFinalizeRequest);
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
