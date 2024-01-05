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
package it.water.service.rest.security.jwt;

import it.water.core.permission.exceptions.UnauthorizedException;
import it.water.core.testing.utils.bundle.TestInitializer;
import it.water.service.rest.api.security.LoggedIn;
import it.water.service.rest.api.security.jwt.JwtTokenService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.annotation.Annotation;
import java.security.Principal;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@ExtendWith(MockitoExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RestSecurityTest {
    private static Logger logger = LoggerFactory.getLogger(RestSecurityTest.class);
    private TestInitializer testInitializer;

    @BeforeAll
    public void initializeTestFramework() throws Exception {
        testInitializer = new TestInitializer();
        testInitializer.withFakePermissionManager().start();
    }

    @Test
    void testJwtTokenService() {
        Assertions.assertNotNull(getJwtTokenService());
    }

    @Test
    void testReleaseJWTWithNoRoles() {
        //fake user with no roles
        TestUser u = new TestUser("fakeUser", Collections.emptySet());
        String token = getJwtTokenService().generateJwtToken(u);
        logger.info("TOKEN : {}", token);
        Assertions.assertNotNull(token);
        //validating generated token
        Assertions.assertTrue(getJwtTokenService().validateToken(Collections.singletonList(TestUser.class.getName()), token));
        Set<Principal> principalSet = getJwtTokenService().getPrincipals(token);
        //Checking User has been mapped as principal
        Assertions.assertEquals(1, principalSet.size());
        Assertions.assertEquals("fakeUser", principalSet.iterator().next().getName());
    }

    @Test
    void testReleaseJWTWithRoles() {
        Set<TestRole> roles = new HashSet<>();
        roles.add(new TestRole("Role1"));
        roles.add(new TestRole("Role2"));
        //fake user with no roles
        TestUser u = new TestUser("fakeUser", roles);
        String token = getJwtTokenService().generateJwtToken(u);
        logger.info("TOKEN : {}", token);
        Assertions.assertNotNull(token);
        //validating generated token
        Assertions.assertTrue(getJwtTokenService().validateToken(Collections.singletonList(TestUser.class.getName()), token));
        Set<Principal> principalSet = getJwtTokenService().getPrincipals(token);
        //Checking User has been mapped as principal together with roles so
        // there are 3 principal 1 user principal and 2 roles principals
        Assertions.assertEquals(3, principalSet.size());
    }

    @Test
    void testJWTSecurityContext() {
        Set<TestRole> roles = new HashSet<>();
        roles.add(new TestRole("Role1"));
        roles.add(new TestRole("Role2"));
        //fake user with no roles
        TestUser u = new TestUser("fakeUser", roles);
        String token = getJwtTokenService().generateJwtToken(u);
        JwtSecurityContext jwtSecurityContext = new JwtSecurityContext(getJwtTokenService().getPrincipals(token));
        Assertions.assertTrue(jwtSecurityContext.isSecure());
        Assertions.assertFalse(jwtSecurityContext.isAdmin());
        Assertions.assertTrue(jwtSecurityContext.isUserInRole("Role1"));
        Assertions.assertFalse(jwtSecurityContext.isUserInRole("Role3"));
        Assertions.assertEquals("JWT", jwtSecurityContext.getAuthenticationScheme());
    }

    @Test
    void testJWTSecurityContextWithCustomPermission() {
        Set<TestRole> roles = new HashSet<>();
        roles.add(new TestRole("Role1"));
        roles.add(new TestRole("Role2"));
        //fake user with no roles
        TestUser u = new TestUser("fakeUser", roles);
        String token = getJwtTokenService().generateJwtToken(u);
        JwtSecurityContext jwtSecurityContext = new JwtSecurityContext(getJwtTokenService().getPrincipals(token), "default");
        Assertions.assertTrue(jwtSecurityContext.isSecure());
        Assertions.assertFalse(jwtSecurityContext.isAdmin());
        Assertions.assertTrue(jwtSecurityContext.isUserInRole("Role1"));
        Assertions.assertFalse(jwtSecurityContext.isUserInRole("Role3"));
        Assertions.assertEquals("JWT", jwtSecurityContext.getAuthenticationScheme());
    }

    @Test
    void testGenericJWTAuthFilter() {
        GenericJWTAuthFilter genericJWTAuthFilter = new GenericJWTAuthFilter();
        String authorization = "JWT 123";
        String cookieValue = "JWT 456";
        String token = genericJWTAuthFilter.getTokenFromRequest(authorization, null);
        Assertions.assertEquals("123", token);
        token = genericJWTAuthFilter.getTokenFromRequest(null, cookieValue);
        Assertions.assertEquals("456", token);
        Assertions.assertThrows(UnauthorizedException.class, () -> genericJWTAuthFilter.getTokenFromRequest(null, null));
        Set<TestRole> roles = new HashSet<>();
        roles.add(new TestRole("Role1"));
        TestUser u = new TestUser("fakeUser", roles);
        token = getJwtTokenService().generateJwtToken(u);
        authorization = "JWT " + token;
        LoggedIn loggedIn = new LoggedIn() {
            @Override
            public String[] issuers() {
                return new String[]{TestUser.class.getName()};
            }

            @Override
            public Class<? extends Annotation> annotationType() {
                return null;
            }
        };
        genericJWTAuthFilter.validateToken(getJwtTokenService(), loggedIn, authorization, cookieValue);
    }

    @Test
    void testJWTFailure() {
        TestUser u = new TestUser("fakeUser", Collections.emptySet());
        String token = getJwtTokenService().generateJwtToken(u);
        token += "manipulatingToken";
        //validating generated token
        Assertions.assertFalse(getJwtTokenService().validateToken(Collections.singletonList(TestUser.class.getName()), token));
        Set<Principal> principalSet = getJwtTokenService().getPrincipals(token);
        //Checking User has been mapped as principal
        Assertions.assertEquals(0, principalSet.size());
        Assertions.assertEquals(0, getJwtTokenService().getPrincipals("invalidToken").size());
    }

    @Test
    void testJWTURLValidation() {
        File publicKey = new File("src/test/resources/certs/public_key.json");
        testInitializer.getApplicationProperties().override(JWTConstants.JWT_PROP_VALIDATE_BY_JWS, "true");
        testInitializer.getApplicationProperties().override(JWTConstants.JWT_PROP_JWS_URL, publicKey.toURI().toString());
        TestUser u = new TestUser("fakeUser", Collections.emptySet());
        String token = getJwtTokenService().generateJwtToken(u);
        String jwk = getJwtTokenService().getJWK();
        System.out.println(jwk);
        Assertions.assertTrue(getJwtTokenService().validateToken(Collections.singletonList(TestUser.class.getName()), token));
        testInitializer.getApplicationProperties().override(JWTConstants.JWT_PROP_VALIDATE_BY_JWS, "false");
    }


    private JwtTokenService getJwtTokenService() {
        return testInitializer.getComponentRegistry().findComponent(JwtTokenService.class, null);
    }
}
