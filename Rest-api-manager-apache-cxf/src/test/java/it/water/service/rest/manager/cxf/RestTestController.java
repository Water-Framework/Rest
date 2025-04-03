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

import it.water.core.api.model.EntityExtension;
import it.water.core.api.model.ExpandableEntity;
import it.water.core.api.service.rest.FrameworkRestController;
import org.junit.jupiter.api.Assertions;

@FrameworkRestController(referredRestApi = RestTestApi.class)
public class RestTestController implements RestTestApi {
    @Override
    public TestPojo authenticatedOperation() {
        TestPojo testPojo = new TestPojo();
        testPojo.setFieldA("fieldA");
        testPojo.setFieldB("fieldB");
        TestPojoExtension testPojoExtension = new TestPojoExtension();
        testPojoExtension.setExtensionField("extensionField");
        testPojo.setExtension(testPojoExtension);
        return testPojo;
    }

    @Override
    public void anonymousOperation(TestPojo testPojo) {
        Assertions.assertNotNull(testPojo);
        Assertions.assertEquals("fieldA", testPojo.getFieldA());
        Assertions.assertEquals("fieldB", testPojo.getFieldB());
        TestPojoExtension testPojoExtension = (TestPojoExtension) testPojo.getExtension();
        Assertions.assertEquals("extensionField",testPojoExtension.getExtensionField());
        Assertions.assertNotNull(testPojo.getExtension());
    }
}
