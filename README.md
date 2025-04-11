# Rest

## Overview

The Water REST service project provides a comprehensive and modular RESTful API for the Water ecosystem. Its primary goal is to enable seamless interaction with the Water platform's core functionalities through standard HTTP endpoints. This includes managing core services, ensuring secure access, and handling data persistence. The project is designed to be adaptable to various deployment environments, including OSGi and Spring, emphasizing customization and ease of integration. It serves as a crucial bridge between client applications and the Water platform, offering a secure and standardized way to access and manipulate data and services. The intended audience includes developers building applications that interact with the Water platform, system administrators responsible for deploying and configuring the REST API, and stakeholders interested in the overall architecture and functionality of the Water ecosystem.

## Technology Stack

*   **Language:** Java
*   **Build Tool:** Gradle
*   **Logging:** SLF4J
*   **REST API:** JAX-RS (Jakarta RESTful Web Services)
*   **API Documentation:** Swagger/OpenAPI
*   **REST Framework (OSGi):** Apache CXF
*   **REST Framework (Spring):** Spring Boot, Spring Web
*   **Spring API Documentation:** Springdoc-openapi-starter-webmvc-ui
*   **Code Optimization:** Lombok
*   **Compile-time Indexing:** Atteo Classindex
*   **Testing:** Mockito & JUnit
*   **Code Coverage:** Jacoco
*   **Dependency Management:** Maven
*   **Cryptography:** Bouncy Castle
*   **JWT Handling:** Nimbus JOSE+JWT
*   **JSON Processing:** Jackson
*   **ORM:** Jakarta Persistence API
*   **Transactions:** Jakarta Transaction API
*   **Classpath Scanning:** org.reflections
*   **HTTP Client:** org.apache.httpcomponents.client5
*   **Embedded Server (Testing):** org.eclipse.jetty
*   **OSGi Bundling:** biz.aQute.bnd.builder

## Directory Structure

```
Rest/
├── build.gradle                    # Root build file for the entire project
├── gradle.properties               # Gradle properties for project configuration
├── settings.gradle                 # Settings file defining subprojects
├── Rest-api/                       # Defines the core REST API interfaces and data models
│   ├── build.gradle                # Build file for the Rest-api module
│   └── src/                        # Source code for the Rest-api module
│       └── main/java/it/water/service/rest/api/
│           ├── RootApi.java        # Interface defining basic API endpoints
│           ├── StatusApi.java      # Interface defining status check endpoints
│           ├── WaterJacksonMapper.java # Provides Jackson ObjectMapper configuration
│           ├── options/            # Package containing interfaces for configuration options
│           │   ├── RestOptions.java    # Interface for accessing REST-related configuration options
│           │   └── JwtSecurityOptions.java # Interface for accessing JWT security configuration options
│           └── security/           # Package containing security-related classes
│               ├── LoggedIn.java     # JAX-RS NameBinding annotation for authenticated methods
│               ├── RsSecurityContext.java # Interface extending SecurityContext
│               └── jwt/              # Package for JWT-related interfaces
│                   └── JwtTokenService.java # Interface defining JWT token operations
├── Rest-api-manager-apache-cxf/  # Manages the REST API using Apache CXF
│   ├── build.gradle                # Build file for the Rest-api-manager-apache-cxf module
│   └── src/                        # Source code for the Rest-api-manager-apache-cxf module
│       └── main/java/it/water/service/rest/manager/cxf/
│           ├── CxfRestApiManager.java # Manages the REST API server using Apache CXF
│           ├── PerRequestProxyProvider.java # Creates REST service implementations on a per-request basis
│           └── security/filters/jwt/ # Package containing JWT authentication filter
│               └── CxfJwtAuthenticationFilter.java # CXF ContainerRequestFilter for JWT validation
├── Rest-jaxrs-api/                 # JAX-RS implementation of the core REST API interfaces
│   ├── build.gradle                # Build file for the Rest-jaxrs-api module
│   └── src/                        # Source code for the Rest-jaxrs-api module
│       └── main/java/it/water/service/rest/jaxrs/api/
│           ├── DefaultRootApi.java     # Implementation of RootApi
│           └── DefaultStatusApi.java   # Implementation of StatusApi
├── Rest-persistence/               # Provides REST endpoints for managing entities
│   ├── build.gradle                # Build file for the Rest-persistence module
│   └── src/                        # Source code for the Rest-persistence module
│       └── main/java/it/water/service/rest/persistence/
│           └── BaseEntityRestApi.java  # Abstract class providing generic REST operations for entities
├── Rest-security/                  # Implements security features, including JWT authentication
│   ├── build.gradle                # Build file for the Rest-security module
│   └── src/                        # Source code for the Rest-security module
│       └── main/java/it/water/service/rest/security/
│           ├── jwt/                  # Package containing JWT-related classes
│           │   ├── GenericJWTAuthFilter.java # Abstract class for JWT token validation
│           │   ├── NimbusJwtTokenService.java # Implementation of JwtTokenService using Nimbus JOSE+JWT
│           │   ├── JwtSecurityContext.java  # Implementation of RsSecurityContext for JWT-based security
│           │   └── JwtSecurityOptionsImpl.java # Implementation of JwtSecurityOptions
├── Rest-service/                   # Provides implementations for the core REST API and Jackson configuration
│   ├── build.gradle                # Build file for the Rest-service module
│   └── src/                        # Source code for the Rest-service module
│       └── main/java/it/water/service/rest/
│           ├── RestApiRegistryImpl.java # Manages the registration and retrieval of REST API services
│           ├── RestOptionsImpl.java    # Implementation of RestOptions
│           ├── RootRestApiImpl.java    # Implementation of RootApi
│           ├── StatusRestApiImpl.java  # Implementation of StatusApi
│           ├── WaterDefaultJacksonMapper.java # Default implementation of WaterJacksonMapper
│           └── jackson/              # Package for Jackson customization
│               ├── WaterJacksonModule.java # Jackson module for custom serialization/deserialization
│               ├── WaterJsonSerializer.java # Custom JSON serializer
│               ├── WaterJsonDeserializer.java # Custom JSON deserializer
│               ├── WaterJsonSerializerModifier.java # Modifies default JSON serializers
│               └── WaterJsonDeserializerModifier.java # Modifies default JSON deserializers
├── Rest-spring-api/               # Integrates the REST API with Spring
│   ├── build.gradle                # Build file for the Rest-spring-api module
│   └── src/                        # Source code for the Rest-spring-api module
│       └── main/java/it/water/service/rest/spring/
│           ├── WaterRestSpringConfiguration.java # Configures Spring MVC to expose the REST API
│           ├── security/             # Package containing Spring security filter
│           │   └── SpringJwtAuthenticationFilter.java # Spring HandlerInterceptor for JWT validation
│           ├── api/ # Spring API implementations
│           │   ├── SpringRootApi.java
│           │   └── SpringStatusApi.java
│           ├── controller/ # Spring Controllers
│           │   ├── RootController.java
│           │   └── StatusController.java
│           └── exception/ # Spring Exception Management
│               └── SpringExceptionMapper.java
└── README.md                       # Project documentation
```

## Getting Started

1.  **Prerequisites:**
    *   Java Development Kit (JDK) version 11 or higher
    *   Gradle version 7.0 or higher
    *   An IDE such as IntelliJ IDEA or Eclipse is recommended for development.
    *   Ensure environment variables such as `JAVA_HOME` are correctly configured.

2.  **Cloning the Repository:**
    *   Clone the repository using the following command:
        ```bash
        git clone https://github.com/Water-Framework/Rest.git
        ```

3.  **Build Steps:**
    *   Navigate to the root directory of the project.
    *   Run the following command to compile the code, run tests, and generate the build artifacts:
        ```bash
        gradle clean build
        ```
    *   To generate the Jacoco coverage report:
        ```bash
        gradle jacocoRootReport
        ```

4.  **Configuration and Environment Variables:**
    *   Most configuration options are set in the `it.water.application.properties` files within each module.
    *   Common environment variables include:
        *   `WATER_SERVICE_URL`: URL for core Water services.
        *   `WATER_FRONTEND_URL`: URL for the Water frontend.
        *   `WATER_REST_CONTEXT`: Root context for the REST API (e.g., `/water`).
        *   `WATER_REST_UPLOAD_ASSETS_FOLDER`: Path for file uploads.
        *   `WATER_REST_UPLOAD_MAX_SIZE`: Maximum file size for uploads.
        *   `WATER_REST_SECURITY_VALIDATE_JWT`: Enables/disables JWT validation.
        *   `WATER_REST_SECURITY_JWT_MILLIS_EXPIRATION`: JWT token expiration time in milliseconds.
    *   Security-related properties (keystore, passwords) should be securely managed and not hardcoded.

5.  **Module Usage:**

    *   **Rest-api:** This module defines the core interfaces and data models for the REST API. It doesn't contain any implementation. To use it, you would include it as a dependency in other modules that provide the actual API implementations.

        ```gradle
        dependencies {
            implementation project(":Rest-api")
        }
        ```
        This makes the interfaces and models defined in `Rest-api` available to your module, allowing you to implement the core API contracts.

    *   **Rest-api-manager-apache-cxf:** This module provides the REST API management using Apache CXF. It is responsible for starting and stopping the REST API server, configuring CXF features, and handling security filters.
        To integrate it, you would include it as a dependency and then configure the `CxfRestApiManager` with the necessary settings, such as the API endpoints and security configurations.

        ```gradle
        dependencies {
            implementation project(":Rest-api-manager-apache-cxf")
        }
        ```
        After including it as dependency, the CXF server can be started and stopped by calling `CxfRestApiManager.startRestApiServer()` and `CxfRestApiManager.stopRestApiServer()` respectively.

    *   **Rest-jaxrs-api:** This module provides a JAX-RS implementation of the core REST API interfaces defined in `Rest-api`. To use it, you include it as a dependency in your project. This module provides default implementations of the `RootApi` and `StatusApi` interfaces.

        ```gradle
        dependencies {
            implementation project(":Rest-jaxrs-api")
        }
        ```

    *   **Rest-persistence:** This module exposes REST endpoints for managing entities. It provides a `BaseEntityRestApi` abstract class that you can extend to create REST endpoints for your specific entities.

        ```gradle
        dependencies {
            implementation project(":Rest-persistence")
        }
        ```
        To use it, you would extend `BaseEntityRestApi` and provide an implementation of your `EntityService`.

    *   **Rest-security:** This module implements security features for the REST API, including JWT authentication and authorization. It provides a `GenericJWTAuthFilter` that you can use to protect your API endpoints. The `NimbusJwtTokenService` is used for generating and validating JWT tokens.

        ```gradle
        dependencies {
            implementation project(":Rest-security")
        }
        ```
         To secure your endpoints, you would annotate them with the `@LoggedIn` annotation, and the `GenericJWTAuthFilter` will handle the JWT validation.

    *   **Rest-service:** This module provides implementations for the core REST API and Jackson configuration. It includes the `RestApiRegistryImpl` for managing REST API services and the `WaterDefaultJacksonMapper` for configuring Jackson.

        ```gradle
        dependencies {
            implementation project(":Rest-service")
        }
        ```
        This module is typically used internally within the Water platform to provide the default REST API implementations.

    *   **Rest-spring-api:** This module integrates the REST API with Spring. It provides a `WaterRestSpringConfiguration` class that configures Spring MVC to expose the REST API.

        ```gradle
        dependencies {
            implementation project(":Rest-spring-api")
        }
        ```
        To use it, you would include it as a dependency in your Spring project and then configure the `WaterRestSpringConfiguration` class.

        **Example: Integrating Rest-spring-api in an external Spring Boot project**

        1.  **Add the dependency:** In your Spring Boot project's `build.gradle` file, add the `Rest-spring-api` module as a dependency:

            ```gradle
            dependencies {
                implementation project(":Rest-spring-api")
            }
            ```

        2.  **Enable the REST API:** In your Spring Boot application, import the `WaterRestSpringConfiguration` class. This will configure Spring MVC to expose the REST API endpoints.

            ```java
            import it.water.service.rest.spring.WaterRestSpringConfiguration;
            import org.springframework.context.annotation.Import;
            import org.springframework.boot.autoconfigure.SpringBootApplication;

            @SpringBootApplication
            @Import(WaterRestSpringConfiguration.class)
            public class MyApplication {
                public static void main(String[] args) {
                    SpringApplication.run(MyApplication.class, args);
                }
            }
            ```

        3.  **Configure application properties:** Configure the necessary properties in your `application.properties` or `application.yml` file. These properties include the REST context path, service URLs, and security settings:

            ```properties
            server.servlet.context-path=/
            water.rest.root.context=/water
            water.rest.services.url=http://localhost:8181
            water.rest.frontend.url=http://localhost:4200
            water.rest.security.jwt.validate=true
            water.rest.security.jwt.duration.millis=3600000
            ```

        4.  **Implement API interfaces (Optional):** If you want to customize the API implementations, you can create your own implementations of the interfaces defined in the `Rest-api` module (e.g., `RootApi`, `StatusApi`) and register them as Spring beans.

            ```java
            import it.water.service.rest.api.RootApi;
            import org.springframework.stereotype.Component;

            @Component
            public class CustomRootApi implements RootApi {
                @Override
                public String sayHi() {
                    return "Hello from Custom Root API!";
                }
            }
            ```

        5.  **Run the application:** Start your Spring Boot application. The REST API endpoints should now be accessible under the configured context path (e.g., `http://localhost:8080/water/sayHi`).

## Functional Analysis

### 1. Main Responsibilities of the System

The primary responsibilities of the Water REST service are:

*   **Exposing Core Water Functionalities:** Provides RESTful interfaces for accessing core services and functionalities of the Water platform.
*   **Security Management:** Implements security measures, including JWT-based authentication and authorization, to protect the API endpoints.
*   **Data Management:** Offers REST endpoints for managing entities and interacting with the persistence layer.
*   **Configuration Management:** Provides a flexible configuration mechanism via properties files, allowing for easy customization and environment-specific settings.
*   **Exception Handling:** Handles exceptions gracefully and returns appropriate HTTP responses with error details.
*   **API Documentation:** Generates API documentation using Swagger/OpenAPI, making it easy for developers to understand and use the API.
*   **Extensibility:** Supports a plugin-based architecture, allowing for easy extension and customization of the API.

### 2. Problems the System Solves

The Water REST service addresses the following problems:

*   **Standardized Access to Water Platform:** Provides a standardized and secure way to access the Water platform's core functionalities, eliminating the need for custom integrations.
*   **Integration with Diverse Environments:** Supports different deployment environments (OSGi, Spring), making it easy to integrate with existing systems.
*   **Security:** Protects the Water platform from unauthorized access by implementing robust authentication and authorization mechanisms.
*   **Scalability:** Designed to be scalable and handle a large number of requests.
*   **Maintainability:** Modular design and clear separation of concerns make the system easy to maintain and evolve.
*   **Discoverability:** The generated API documentation makes it easy for developers to discover and use the API endpoints.

### 3. Interaction of Modules and Components

The different modules and components of the Water REST service interact as follows:

*   **API Request Flow:** When a client sends a request to a REST endpoint, the request is intercepted by a security filter (`GenericJWTAuthFilter`, `CxfJwtAuthenticationFilter`, or `SpringJwtAuthenticationFilter`) if the endpoint is secured. The filter validates the JWT token using the `NimbusJwtTokenService`. If the token is valid, the request proceeds to the appropriate API implementation (`RootRestApiImpl`, `DefaultRootApi`, or `SpringRootApi`, depending on the environment). The API implementation processes the request and returns a response.
*   **Entity Management:** The `BaseEntityRestApi` receives requests to manage entities. It interacts with an `EntityService` to perform the actual data operations. The `EntityService` interacts with the persistence layer to store and retrieve data.
*   **Configuration and Options:** Components access configuration options via the `RestOptions` and `JwtSecurityOptions` interfaces. Implementations like `RestOptionsImpl` and `JwtSecurityOptionsImpl` retrieve these options from application properties.
*   **Exception Handling:** `GenericExceptionMapperProvider` (in JAX-RS environments) and `SpringExceptionMapper` (in Spring environments) handle exceptions thrown by the API implementations. They convert exceptions into appropriate HTTP responses with error details.
*   **Jackson Customization:** `WaterJacksonModule` is registered with Jackson `ObjectMapper` to provide custom serializers and deserializers. `WaterJsonSerializerModifier` and `WaterJsonDeserializerModifier` are used to modify the default serializers and deserializers. `WaterJsonSerializer` and `WaterJsonDeserializer` handle serialization and deserialization of `ExpandableEntity` and `EntityExtension` objects.

### 4. User-Facing vs. System-Facing Functionalities

*   **User-Facing Functionalities:**
    *   REST endpoints for accessing core services and managing entities.
    *   API documentation generated using Swagger/OpenAPI.
*   **System-Facing Functionalities:**
    *   Security filters for authentication and authorization.
    *   Exception handling mechanisms.
    *   Configuration management via properties files.
    *   Jackson customization for serialization and deserialization.
    *   REST API management using Apache CXF or Spring MVC.

The user-facing functionalities provide a way for external applications to interact with the Water platform, while the system-facing functionalities provide the underlying infrastructure and support for the API.

## Architectural Patterns and Design Principles Applied

*   **RESTful Architecture:** The project follows REST principles, using standard HTTP methods and resource-based endpoints.
*   **Microkernel Architecture:** The "Core" modules provide minimal functionality, and extensions are added through plugins or components.
*   **Dependency Injection (DI):** Used extensively (especially in Spring and OSGi), promoting loose coupling and testability.
*   **Interface-Based Programming:** APIs are defined as interfaces, allowing for multiple implementations and easier testing.
*   **Modularity:** The project is divided into modules with well-defined responsibilities, promoting reusability and maintainability.
*   **Separation of Concerns:** Different modules handle distinct aspects of the system (API definition, security, persistence, etc.).
*   **Configuration via Properties:**  Externalized configuration through properties files, allowing for easy customization and environment-specific settings.
*   **Interceptor Pattern:**  Used for authentication and other cross-cutting concerns. The `GenericJWTAuthFilter`, `CxfJwtAuthenticationFilter`, and `SpringJwtAuthenticationFilter` intercept requests and perform JWT validation.
*   **Template Method Pattern:** `GenericJWTAuthFilter` uses the template method pattern. The `validateToken` method is a template method that defines the steps for validating a JWT token, while the `getTokenFromRequest` method is a hook method that allows subclasses to customize how the token is retrieved from the request.
*   **Role-Based Access Control (RBAC):** The JWT tokens contain information about the user's roles, which are used to authorize access to specific API endpoints.

## Code Quality Analysis

The SonarQube analysis of the project indicates a healthy codebase with strong code quality metrics:

*   **Bugs:** 0 - No bugs were detected, indicating a stable and reliable codebase.
*   **Vulnerabilities:** 0 - No security vulnerabilities were identified, demonstrating a secure design and implementation.
*   **Code Smells:** 0 - The absence of code smells suggests clean, maintainable, and understandable code.
*   **Code Coverage:** 80.7% - This high level of test coverage ensures that a significant portion of the codebase is tested, reducing the risk of regressions and improving reliability.
*   **Duplication:** 0.0% - No duplicated code was found, indicating efficient and DRY (Don't Repeat Yourself) principles are followed.

These metrics collectively suggest a well-maintained and robust project. The absence of bugs, vulnerabilities, and code smells, combined with high test coverage and no code duplication, indicates a codebase that is easy to understand, maintain, and extend.

## Weaknesses and Areas for Improvement

Based on the project analysis and SonarQube report, the following are concrete TODO items for future releases and roadmap planning:

*   [ ] **Enhance API Documentation:** Expand Swagger/OpenAPI definitions to cover all endpoints and data models comprehensively. This includes adding detailed descriptions of request and response formats, parameters, and error codes.
*   [ ] **Improve Error Handling:** Provide more detailed and informative error messages and logging for debugging purposes. Include context-specific information in error messages to help developers quickly identify and resolve issues.
*   [ ] **Implement Rate Limiting:** Implement rate limiting to prevent abuse and ensure API availability. This will protect the API from denial-of-service attacks and ensure fair usage.
*   [ ] **Add Caching Mechanisms:** Introduce caching mechanisms to improve performance and reduce database load. Consider using a distributed cache to improve scalability.
*   [ ] **Support Additional Authentication Methods:** Integrate with other identity providers or authentication protocols, such as OAuth 2.0 or SAML, to provide more flexibility for users.
*   [ ] **Implement Monitoring and Alerting:** Add monitoring capabilities to track API usage and performance. Implement alerting to notify administrators of potential issues, such as high error rates or slow response times.
*   [ ] **Explore gRPC Support:** Investigate the possibility of offering gRPC endpoints for internal microservice communication. This could improve performance and reduce latency.
*   [ ] **Establish SonarQube Quality Gates:** Set up quality gates in SonarQube to automatically fail builds if new bugs, vulnerabilities, or code smells are introduced.
*   [ ] **Increase Test Coverage for Edge Cases:** Explore opportunities to further improve code coverage by writing tests for uncovered edge cases or complex logic.
*   [ ] **Keep Dependencies Updated:** Regularly update dependencies to their latest stable versions to benefit from security patches and bug fixes.
*   [ ] **Review and Update Swagger/OpenAPI Documentation:** Ensure the Swagger/OpenAPI documentation accurately reflects the current state of the API.

## Further Areas of Investigation

The following architectural and technical elements warrant additional exploration or clarification:

*   **Performance Bottlenecks:** Identify and address potential performance bottlenecks in the API. This could involve profiling the code, optimizing database queries, or implementing caching mechanisms.
*   **Scalability Considerations:** Evaluate the scalability of the API and identify potential limitations. Consider using a load balancer and a distributed cache to improve scalability.
*   **Integrations with External Systems:** Investigate potential integrations with other systems, such as CRM or ERP systems.
*   **Advanced Features:** Research and implement advanced features, such as support for webhooks or server-sent events.
*   **Areas with Significant Code Smells or Low Test Coverage:** Although the current SonarQube analysis shows no immediate issues, continuously monitor code quality metrics and investigate any areas with significant code smells or low test coverage.

## Attribution

Generated with the support of ArchAI, an automated documentation system.
