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

import it.water.service.rest.api.security.LoggedIn;
import org.junit.jupiter.api.Assertions;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestRestController implements TestApi {

    @Override
    @LoggedIn(issuers = "it.water.service.rest.spring.api.FakeUser")
    public TestPojo authenticatedOperation() {
        TestPojo testPojo = new TestPojo();
        testPojo.setFieldA("fieldA");
        testPojo.setFieldB("fieldB");
        return testPojo;
    }

    @Override
    public void postAnonymousOperation(TestPojo testPojo) {
        Assertions.assertNotNull(testPojo);
        Assertions.assertEquals("fieldA", testPojo.getFieldA());
        Assertions.assertEquals("fieldB", testPojo.getFieldB());
    }

    @Override
    public void anonymousOperation() {
        Assertions.assertTrue(true);
    }
}
