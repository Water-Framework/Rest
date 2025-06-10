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
package it.water.service.rest.manager.cxf;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.water.core.api.model.User;
import it.water.core.api.security.Authenticable;
import it.water.core.api.service.Service;
import it.water.core.testing.utils.bundle.TestRuntimeInitializer;
import it.water.core.testing.utils.junit.WaterTestExtension;
import it.water.service.rest.api.security.jwt.JwtTokenService;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.Map;

/**
 * @Author Aristide Cittadino.
 */
@ExtendWith({MockitoExtension.class, WaterTestExtension.class})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CxfRestApiManagerTest implements Service {

    private static String baseApiUrl;
    private static ObjectMapper objectMapper = new ObjectMapper();

    @BeforeAll
    public static void beforeAll() {
        baseApiUrl = "http://localhost:" + TestRuntimeInitializer.getInstance().getRestServerPort() + "/water";
    }

    @Test
    void testRootRestService() throws IOException, ParseException {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpGet httpGet = new HttpGet(baseApiUrl);
        @SuppressWarnings("deprecation")
        ClassicHttpResponse response = httpClient.execute(httpGet);
        String responseBody = EntityUtils.toString(response.getEntity());
        Assertions.assertEquals(200, response.getCode());
        Assertions.assertEquals("Hi", responseBody);
    }

    @Test
    void testStatusService() throws IOException, ParseException {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        String apiUrl = baseApiUrl + "/status";
        HttpGet httpGet = new HttpGet(apiUrl);
        @SuppressWarnings("deprecation")
        ClassicHttpResponse response = httpClient.execute(httpGet);
        String responseBody = EntityUtils.toString(response.getEntity());
        Assertions.assertEquals(200, response.getCode());
        Assertions.assertEquals("Water Rest Module works!", responseBody);
    }

    @Test
    void getSwagger() throws IOException, ParseException {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        String apiUrl = baseApiUrl + "/swagger.json";
        HttpGet httpGet = new HttpGet(apiUrl);
        @SuppressWarnings("deprecation")
        ClassicHttpResponse response = httpClient.execute(httpGet);
        String responseBody = EntityUtils.toString(response.getEntity());
        Assertions.assertEquals(200, response.getCode());
        Assertions.assertTrue(!responseBody.isEmpty());
        System.out.println("SWAGGER:" + responseBody);
    }

    @Test
    void testFailAuthenticatedOperation() throws IOException, ParseException {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        String apiUrl = baseApiUrl + "/test/authenticatedOperation";
        HttpGet httpGet = new HttpGet(apiUrl);
        @SuppressWarnings("deprecation")
        ClassicHttpResponse response = httpClient.execute(httpGet);
        String responseBody = EntityUtils.toString(response.getEntity());
        Assertions.assertEquals(401, response.getCode());
        Assertions.assertTrue(!responseBody.isEmpty());
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    void testSuccessAuthenticatedOperation() throws IOException, ParseException {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        String apiUrl = baseApiUrl + "/test/authenticatedOperation";
        JwtTokenService jwtTokenService = TestRuntimeInitializer.getInstance().getComponentRegistry().findComponent(JwtTokenService.class, null);
        User fakeUser = new FakeUser();
        String token = "Bearer " + jwtTokenService.generateJwtToken((Authenticable) fakeUser);
        HttpGet httpGet = new HttpGet(apiUrl);
        httpGet.setHeader("Authorization", token);
        httpGet.setHeader("Content-Type", "application/json");
        httpGet.setHeader("Accept", "application/json");
        @SuppressWarnings("deprecation")
        ClassicHttpResponse response = httpClient.execute(httpGet);
        String responseBody = EntityUtils.toString(response.getEntity());
        System.out.println("RESPONSE:" + responseBody);
        Map<String, Object> map = objectMapper.readValue(responseBody, new TypeReference<Map>() {});
        Assertions.assertEquals(200, response.getCode());
        Assertions.assertTrue(!responseBody.isEmpty());
        Assertions.assertEquals("fieldA", map.get("fieldA"));
        Assertions.assertEquals("fieldB", map.get("fieldB"));
        Assertions.assertEquals("extensionField", map.get("extensionField"));
    }

    @Test
    void testAnonympusOperation() throws IOException {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        String json = "{\"fieldA\":\"fieldA\",\"fieldB\":\"fieldB\",\"extensionField\":\"extensionField\"}";
        String apiUrl = baseApiUrl + "/test/anonymousOperation";
        HttpPost httpPost = new HttpPost(apiUrl);
        HttpEntity httpEntity = new StringEntity(json);
        httpPost.setEntity(httpEntity);
        httpPost.setHeader("Content-Type", "application/json");
        httpPost.setHeader("Accept", "application/json");
        @SuppressWarnings("deprecation")
        ClassicHttpResponse response = httpClient.execute(httpPost);
        Assertions.assertEquals(204, response.getCode());
    }


}
