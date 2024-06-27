package org.e4s.api.config;

import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HazelcastSettings {


    @Bean
    public ClientConfig clientConfig(){

        ClientConfig clientConfig = new ClientConfig();

        return clientConfig;
    }



}
