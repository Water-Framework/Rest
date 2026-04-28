/*
 * Copyright 2024 Aristide Cittadino
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package it.water.service.rest.spring;

import it.water.core.api.registry.ComponentRegistry;
import it.water.service.rest.api.WaterJacksonMapper;
import it.water.service.rest.spring.security.SpringJwtAuthenticationFilter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebMvc
@ComponentScan("it.water.service.rest.*")
public class WaterRestSpringConfiguration implements WebMvcConfigurer {
    @Autowired
    @Setter
    private ComponentRegistry componentRegistry;

    @Autowired
    @Setter
    private WaterJacksonMapper waterJacksonMapper;

    @Value("${water.rest.cors.origins:*}")
    private String corsOrigins;

    @Value("${water.rest.cors.methods:GET,POST,PUT,DELETE,OPTIONS,PATCH}")
    private String corsMethods;

    @Value("${water.rest.cors.headers:*}")
    private String corsHeaders;

    @Value("${water.rest.cors.credentials:false}")
    private boolean corsCredentials;

    @Value("${water.rest.cors.maxAge:3600}")
    private long corsMaxAge;

    public WaterRestSpringConfiguration() {
        super();
    }

    @Override
    public void addInterceptors(final InterceptorRegistry registry) {
        registry.addInterceptor(new SpringJwtAuthenticationFilter(componentRegistry));
    }

    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        for (HttpMessageConverter<?> converter : converters) {
            if (converter instanceof MappingJackson2HttpMessageConverter jacksonConverter) {
                // Aggiorna l'ObjectMapper del converter di default
                waterJacksonMapper.init(componentRegistry);
                jacksonConverter.setObjectMapper(waterJacksonMapper.getJacksonMapper());
            }
        }
    }

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        Arrays.stream(corsOrigins.split(",")).map(String::trim).forEach(config::addAllowedOriginPattern);
        Arrays.stream(corsMethods.split(",")).map(String::trim).forEach(config::addAllowedMethod);
        Arrays.stream(corsHeaders.split(",")).map(String::trim).forEach(config::addAllowedHeader);
        config.setAllowCredentials(corsCredentials);
        config.setMaxAge(corsMaxAge);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}
