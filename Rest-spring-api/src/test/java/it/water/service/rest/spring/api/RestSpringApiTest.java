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
package it.water.service.rest.spring.api;

import it.water.core.api.registry.ComponentRegistry;
import it.water.service.rest.api.security.jwt.JwtTokenService;
import it.water.service.rest.spring.WaterRestSpringConfiguration;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.MethodName.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@ContextConfiguration(classes = WaterRestSpringConfiguration.class)
class RestSpringApiTest {
    @Autowired
    private TestRestTemplate template;
    @Autowired
    private TestRestController testRestController;
    @Autowired
    private ComponentRegistry componentRegistry;
    @Autowired
    private JwtTokenService jwtTokenService;
    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void checkRootApi() {
        ResponseEntity<String> response = template.getForEntity("/", String.class);
        Assertions.assertEquals(200, response.getStatusCodeValue());
    }

    @Test
    void checkStatusApi() {
        ResponseEntity<String> response = template.getForEntity("/status", String.class);
        Assertions.assertEquals(200, response.getStatusCodeValue());
    }

    @Test
    void checkSecurityFilter() {
        ResponseEntity<String> response = template.getForEntity("/test/anonymousOperation", String.class);
        Assertions.assertEquals(200, response.getStatusCodeValue());
        response = template.getForEntity("/test/authenticatedOperation", String.class);
        testRestController.authenticatedOperation();
        Assertions.assertEquals(401, response.getStatusCodeValue());
        FakeUser fk = new FakeUser();
        String jwt = this.jwtTokenService.generateJwtToken(fk);
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, "JWT " + jwt);
        HttpEntity httpEntity = new HttpEntity(headers);
        response = template.exchange("/test/authenticatedOperation", HttpMethod.GET, httpEntity, String.class);
        Assertions.assertEquals(200, response.getStatusCodeValue());
    }

    @Test
    void getSwagger() {
        ResponseEntity<String> response = template.getForEntity("/v2/api-docs", String.class);
        Assertions.assertEquals(200, response.getStatusCodeValue());
        String responseBody = response.getBody();
        Assertions.assertTrue(!responseBody.isEmpty());
        System.out.println("SWAGGER:" + responseBody);
    }

}
