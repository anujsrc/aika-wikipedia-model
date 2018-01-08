package org.aika.wikipedia.config;


import org.aika.Model;
import org.aika.storage.InMemoryNeuronRepository;
import org.aika.storage.MongoNeuronRepository;
import org.aika.storage.MongoSuspensionHook;
import org.aika.storage.NeuronRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.*;

/**
 * Spring application context configuration using environment specific application properties.
 */
@Configuration
@ComponentScan(basePackages = {"org.aika"})
@PropertySource("classpath:application.properties")
public class ApplicationConfig {

    public static int NUMBER_OF_THREADS = 1;


    @Autowired
    MongoSuspensionHook suspensionHook;


    @Bean
    public Model aikaModel() {
        return new Model(suspensionHook, NUMBER_OF_THREADS);
    }


    @Bean
    public NeuronRepository neuronRepository() {
        return new MongoNeuronRepository();
    }
}
