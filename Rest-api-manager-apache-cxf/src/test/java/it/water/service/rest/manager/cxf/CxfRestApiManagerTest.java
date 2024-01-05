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

import it.water.core.api.entity.Authenticable;
import it.water.core.api.model.User;
import it.water.core.testing.utils.bundle.TestInitializer;
import it.water.service.rest.api.security.jwt.JwtTokenService;
import org.apache.cxf.transport.servlet.CXFServlet;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;

/**
 * @Author Aristide Cittadino.
 */
@ExtendWith(MockitoExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CxfRestApiManagerTest {
    private TestInitializer testInitializer;

    @BeforeAll
    public void initializeTestFramework() throws Exception {
        startJetty();
        testInitializer = new TestInitializer();
        testInitializer.withFakePermissionManager().start();
    }

    private void startJetty() throws Exception {
        Server server = new Server(8080);
        // Register and map the dispatcher servlet
        final ServletHolder servletHolder = new ServletHolder(new CXFServlet());
        final ServletContextHandler context = new ServletContextHandler();
        context.setContextPath("/");
        context.addServlet(servletHolder, "/water/*");
        server.setHandler(context);
        server.start();
    }

    @Test
    void testRootRestService() throws IOException {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        String apiUrl = "http://localhost:8080/water";
        HttpGet httpGet = new HttpGet(apiUrl);
        HttpResponse response = httpClient.execute(httpGet);
        String responseBody = EntityUtils.toString(response.getEntity());
        Assertions.assertEquals(200, response.getStatusLine().getStatusCode());
        Assertions.assertEquals("Hi", responseBody);
    }

    @Test
    void testStatusService() throws IOException {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        String apiUrl = "http://localhost:8080/water/status";
        HttpGet httpGet = new HttpGet(apiUrl);
        HttpResponse response = httpClient.execute(httpGet);
        String responseBody = EntityUtils.toString(response.getEntity());
        Assertions.assertEquals(200, response.getStatusLine().getStatusCode());
        Assertions.assertEquals("Water Rest Module works!", responseBody);
    }

    @Test
    void getSwagger() throws IOException {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        String apiUrl = "http://localhost:8080/water/swagger.json";
        HttpGet httpGet = new HttpGet(apiUrl);
        HttpResponse response = httpClient.execute(httpGet);
        String responseBody = EntityUtils.toString(response.getEntity());
        Assertions.assertEquals(200, response.getStatusLine().getStatusCode());
        Assertions.assertTrue(!responseBody.isEmpty());
        System.out.println("SWAGGER:" + responseBody);
    }

    @Test
    void testFailAuthenticatedOperation() throws IOException {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        String apiUrl = "http://localhost:8080/water/test/authenticatedOperation";
        HttpGet httpGet = new HttpGet(apiUrl);
        HttpResponse response = httpClient.execute(httpGet);
        String responseBody = EntityUtils.toString(response.getEntity());
        Assertions.assertEquals(401, response.getStatusLine().getStatusCode());
        Assertions.assertTrue(!responseBody.isEmpty());
    }

    @Test
    void testSuccessAuthenticatedOperation() throws IOException {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        String apiUrl = "http://localhost:8080/water/test/authenticatedOperation";
        JwtTokenService jwtTokenService = testInitializer.getComponentRegistry().findComponent(JwtTokenService.class,null);
        User fakeUser = new FakeUser();
        String token = "JWT "+jwtTokenService.generateJwtToken((Authenticable) fakeUser);
        HttpGet httpGet = new HttpGet(apiUrl);
        httpGet.setHeader("Authorization",token);
        HttpResponse response = httpClient.execute(httpGet);
        String responseBody = EntityUtils.toString(response.getEntity());
        Assertions.assertEquals(200, response.getStatusLine().getStatusCode());
        Assertions.assertTrue(!responseBody.isEmpty());
    }

    @Test
    void testAnonympusOperation() throws IOException {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        String apiUrl = "http://localhost:8080/water/test/anonymousOperation";
        HttpGet httpGet = new HttpGet(apiUrl);
        HttpResponse response = httpClient.execute(httpGet);
        String responseBody = EntityUtils.toString(response.getEntity());
        Assertions.assertEquals(200, response.getStatusLine().getStatusCode());
        Assertions.assertTrue(!responseBody.isEmpty());
    }


}
