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
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

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

    public WaterRestSpringConfiguration() {
        super();
    }

    @Override
    public void addInterceptors(final InterceptorRegistry registry) {
        registry.addInterceptor(new SpringJwtAuthenticationFilter(componentRegistry));
    }

    @Bean
    public MappingJackson2HttpMessageConverter mappingJackson2HttpMessageConverter() {
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        waterJacksonMapper.init(componentRegistry);
        converter.setObjectMapper(waterJacksonMapper.getJacksonMapper());
        return converter;
    }
}
