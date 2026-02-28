# Rest Module — HTTP REST Layer

## Purpose
Provides the dual-runtime REST infrastructure for the Water Framework, supporting both **JAX-RS (Apache CXF)** for OSGi/Karaf and **Spring MVC** for Spring Boot. Defines the `RestApi` interface hierarchy, automatic CRUD endpoint generation, JWT security filters, JSON view serialization, exception mapping, and the `RestApiManager` lifecycle component.

## Sub-modules

| Sub-module | Runtime | Role |
|---|---|---|
| `Rest-api` | All | Core REST interfaces, `BaseEntityRestApi<T>`, JSON view definitions, exception mapping, Swagger annotations |
| `Rest-jaxrs-api` | OSGi/CXF | `@FrameworkRestApi`, JAX-RS base classes, CXF-specific annotations |
| `Rest-spring-api` | Spring | `@FrameworkRestController`, Spring MVC base classes |
| `Rest-persistence` | All | `BaseEntityRestApi<T>` — automatic CRUD endpoints for JPA entities |
| `Rest-service` | All | `RestApiManagerImpl` — discovers and registers/deregisters REST controllers |
| `Rest-security` | All | JWT filters: `CxfJwtAuthenticationFilter` (OSGi), `SpringJwtAuthenticationFilter`, `GenericJWTAuthFilter` |
| `Rest-api-manager-apache-cxf` | OSGi | CXF bus configuration, embedded HTTP server setup |

## REST Interface Hierarchy

```
RestApi (Core-api marker)
  └─ BaseEntityRestApi<T>     // CRUD HTTP endpoints
       └─ MyEntityRestApi     // your module's REST interface (in -api submodule)
            └─ MyEntityRestController  // implementation (in -service submodule)
```

## BaseEntityRestApi<T> — Auto-generated CRUD Endpoints

```java
// Implemented automatically by extending framework base classes
interface BaseEntityRestApi<T extends BaseEntity> extends RestApi {

    @POST   @Path("/")           T save(T entity);
    @PUT    @Path("/")           T update(T entity);
    @GET    @Path("/{id}")       T find(@PathParam("id") long id);
    @GET    @Path("/")           PaginatedResult<T> findAll(
                                     @QueryParam("delta") int delta,
                                     @QueryParam("page") int page,
                                     @QueryParam("filter") String filter);
    @DELETE @Path("/{id}")       void remove(@PathParam("id") long id);
    @GET    @Path("/all")        Collection<T> findAll();
}
```

## REST Controller Pattern (Dual-Runtime)

```java
// In your -api submodule
@Path("/water/myentities")
@RequestMapping("/water/myentities")    // Spring MVC annotation
public interface MyEntityRestApi extends BaseEntityRestApi<MyEntity> {
    // Additional custom endpoints here
}

// In your -service submodule (JAX-RS / CXF)
@FrameworkRestApi
public class MyEntityRestController extends BaseEntityRestControllerImpl<MyEntity>
    implements MyEntityRestApi {
    @Inject @Setter private MyEntityApi myEntityApi;
    // delegates to myEntityApi
}

// In your -service-spring submodule (Spring MVC)
@FrameworkRestController
public class MyEntitySpringRestController extends MyEntityRestController {
    // Spring-specific wiring, if any override needed
}
```

## JSON View Hierarchy

```
WaterJsonView.Compact          // minimal: id + key identifier only
  └─ WaterJsonView.Extended    // all public-safe fields
       ├─ WaterJsonView.Public         // external-facing APIs
       ├─ WaterJsonView.Internal       // service-to-service communication
       ├─ WaterJsonView.Secured        // authenticated users only
       └─ WaterJsonView.Privacy        // PII — restricted, sensitive fields
```

Usage on entity fields:
```java
@JsonView(WaterJsonView.Extended.class)
private String name;

@JsonView(WaterJsonView.Privacy.class)
private String socialSecurityNumber;
```

REST controllers serialize using `WaterJsonView.Extended` by default.

## Exception → HTTP Status Mapping

| Exception | HTTP Status |
|---|---|
| `ValidationException` | 422 Unprocessable Entity |
| `UnauthorizedException` | 401 Unauthorized |
| `EntityNotFound` | 404 Not Found |
| `DuplicateEntityException` | 409 Conflict |
| `NoResultException` | 404 Not Found |
| `WaterRuntimeException` | 500 Internal Server Error |

## JWT Security Filters

All REST endpoints are protected by JWT filters unless `water.testMode=true`.

```properties
# Disable JWT validation in tests
water.rest.security.jwt.validate=false
water.rest.security.jwt.validate.by.jws=false
water.testMode=true
```

Filter chain:
1. Extract `Authorization: Bearer <token>` header
2. Validate JWT signature against keystore
3. Populate `SecurityContext` with user claims
4. Proceed to controller or return 401

## RestApiManager

Automatically discovers all `@FrameworkRestApi` / `@FrameworkRestController` components and registers them with the embedded HTTP server (CXF bus or Spring DispatcherServlet).

- **OSGi:** registers as CXF JAX-RS endpoints on the OSGi HTTP service
- **Spring:** auto-detected via component scan, registered with `DispatcherServlet`

## Base URL Convention
All Water REST endpoints follow: `/water/<entity-plural-lowercase>`

Example: `MyEntity` → `/water/myentities`

## Swagger / OpenAPI Integration
Add `@Api`, `@ApiOperation`, `@ApiParam` annotations on `RestApi` interface methods. The framework auto-generates Swagger UI at `/water/swagger`.

## Testing Rules — MANDATORY
- **RestControllerImpl MUST be tested exclusively via Karate feature files**
- NEVER write JUnit `@Test` that directly calls or instantiates a `RestControllerImpl`
- NEVER use `MockMvc`, `WebTestClient`, or `RestAssured` in JUnit tests for REST controllers
- Karate runner classes (`@Karate.Test`) are the only JUnit entry point for REST testing
- Business logic and permission tests belong in `ApiTest` classes (service layer)

## Karate Runner Pattern
```java
// Water native (service submodule)
@ExtendWith(WaterTestExtension.class)
public class MyEntityRestApiTest implements Service {
    @Inject @Setter private ComponentRegistry componentRegistry;

    @BeforeEach void beforeEach() { TestRuntimeUtils.impersonateAdmin(componentRegistry); }

    @Karate.Test
    Karate restInterfaceTest() {
        return Karate.run("classpath:karate")
            .systemProperty("webServerPort", TestRuntimeInitializer.getInstance().getRestServerPort())
            .systemProperty("host", "localhost")
            .systemProperty("protocol", "http");
    }
}

// Spring Boot variant
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {"water.testMode=true", "water.rest.security.jwt.validate=false"})
public class MyEntityRestSpringApiTest {
    @Autowired ComponentRegistry componentRegistry;
    @LocalServerPort int serverPort;

    @Karate.Test
    Karate restInterfaceTest() {
        return Karate.run("../MyModule-service/src/test/resources/karate")
            .systemProperty("webServerPort", String.valueOf(serverPort))
            .systemProperty("host", "localhost")
            .systemProperty("protocol", "http");
    }
}
```

## Dependencies
- `it.water.core:Core-api` — `RestApi` marker, `Runtime`
- `it.water.repository:Repository-entity` — `BaseEntity`, `PaginatedResult`
- `jakarta.ws.rs:jakarta.ws.rs-api` — JAX-RS annotations
- `org.springframework:spring-webmvc` — Spring MVC annotations
- `com.nimbusds:nimbus-jose-jwt` — JWT parsing/validation
- `com.intuit.karate:karate-junit5` — REST integration testing
