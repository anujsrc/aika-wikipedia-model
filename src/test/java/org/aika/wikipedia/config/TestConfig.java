package org.aika.wikipedia.config;


import org.aika.Model;
import org.aika.storage.InMemoryNeuronRepository;
import org.aika.storage.NeuronRepository;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

/**
 * Spring application context configuration using environment specific application properties.
 */
@Configuration
@PropertySource("classpath:application.properties")
@ComponentScan(basePackages = {"org.aika"})
@EnableMongoRepositories(basePackages = "org.aika.storage")
@EnableAutoConfiguration(exclude = DataSourceAutoConfiguration.class)
public class TestConfig {

    public static int NUMBER_OF_THREADS = 1;

    @Bean
    public Model aikaModel() {
        return new Model(null, NUMBER_OF_THREADS);
    }


    @Bean
    public NeuronRepository neuronRepository() {
        return new InMemoryNeuronRepository();
    }

}
