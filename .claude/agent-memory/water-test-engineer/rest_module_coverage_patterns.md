---
name: rest-module-coverage-patterns
description: Water Rest module (Rest-security, Rest-service) unit-test patterns, Gradle transitivity gotcha for Core-registry, and TestRuntimeInitializer shared-registry caveats discovered while raising coverage to >=80%.
metadata:
  type: project
---

Context: raised Rest-security (69.6%) and Rest-service (55.2%) instruction coverage toward 80% with pure JUnit5/Mockito unit tests (no REST controller instantiation — controllers are Karate-only per Rest/CLAUDE.md).

## Core-registry is NOT on Rest-*'s test compile classpath
`Core-testing-utils/build.gradle` declares `implementation project(":Core-registry")` (not `api`).
Since Rest-service/Rest-security consume Core-testing-utils via `testImplementation` (a
cross-project dependency), Gradle's api/implementation transitivity rule hides Core-registry
from their test compile classpath even though it's present at runtime.
**Why:** tried to use `it.water.core.registry.model.ComponentConfigurationFactory` in a
Rest-service test — would have failed to compile.
**How to apply:** when a test needs a `ComponentConfiguration` to call
`ComponentRegistry.registerComponent(...)` from a Rest-* (or any non-Core) module's test code,
implement `it.water.core.api.registry.ComponentConfiguration` inline as a small local test
class backed by a `java.util.Properties` field — don't import anything from
`it.water.core.registry.*`. Only `it.water.core.api.*` (Core-api) is safely available.
Same reasoning applies to any Water module whose only path to Core-registry is via
Core-testing-utils's `implementation` dependency — check the module's own build.gradle first.

## TestRuntimeInitializer's ComponentRegistry is a single JVM-wide singleton
`WaterTestExtension.getComponentsRegistry()` returns
`TestRuntimeInitializer.getInstance().getComponentRegistry()` — the SAME instance is shared
across every test class in a module's test run (not just within one test class), mirroring how
`ApplicationProperties` overrides also leak across tests unless explicitly reset (see
`RestSecurityTest.testJWTURLValidation`, `CorsOptionsImplTest` cleanup blocks).
**Why:** discovered when writing `WaterJacksonRoundTripTest` — registering a fake
`EntityExtensionService` component makes it visible to *every* later test in the same JVM run
that looks up that same interface with a matching `ComponentFilter`.
**How to apply:** (1) use `@TestMethodOrder(MethodOrderer.OrderAnnotation.class)` +
`@Order(n)` so a "before registration" assertion always runs before the test that registers
the component; (2) capture the `ComponentRegistration<T,?>` returned by `registerComponent`
and call `componentRegistry.unregisterComponent(registration)` in a `finally` block to avoid
leaking state into other test classes in the same module.

## ApplicationProperties#getPropertyOrDefault is a default method — must be stubbed directly
`ApplicationProperties.getPropertyOrDefault(name, default)` (String/long/boolean overloads) are
Java `default` interface methods that call `this.getProperty(name)` internally. A Mockito mock
of `ApplicationProperties` does NOT execute the real default-method body unless you use
`Mockito.CALLS_REAL_METHODS`; by default it just returns Mockito's default answer.
**How to apply:** when unit-testing an Options-style class (e.g. `JwtSecurityOptionsImpl`) that
calls `applicationProperties.getPropertyOrDefault(key, default)`, stub that exact overload
directly (`when(applicationProperties.getPropertyOrDefault(eq(key), eq(default))).thenReturn(...)`),
not `getProperty(key)`.

## GenericJWTAuthFilter reflection-hierarchy utilities — test in the same package, no subclass needed
`getAnnotationFromHierarchy` / `getAllAnnotationsFromHierarchy` in `GenericJWTAuthFilter` are
`protected`; since the test class lives in the same package
(`it.water.service.rest.security.jwt`), Java package-level protected access means you can call
them directly on a plain `new GenericJWTAuthFilter()` instance — no subclassing needed. Cover
with small local fixture classes: annotation declared directly on the method, declared only on
an overridden superclass method, declared only on an implemented interface method, and no
annotation anywhere (must return null / empty list).

## InMemoryTokenRevocationStore — MAX_KEYS cap-eviction branch is impractical to unit test
`evictIfNeeded()` calls `revoked.entrySet().removeIf(...)` on EVERY `revoke()` call (not just
when over the cap), so driving the map past `MAX_KEYS = 100_000` in a test is O(n^2) and would
make the suite extremely slow. The existing `InMemoryTokenRevocationStoreTest` intentionally
skips this branch (documented in its own comments) — treat it as an accepted, permanent
coverage gap rather than something to "fix" with more inserts.

## Jackson custom serializer/deserializer coverage
`WaterJsonSerializer`/`WaterJsonDeserializer`/`*Modifier` classes (package
`it.water.service.rest.jackson`) are wired into every bean (de)serialization via
`WaterDefaultJacksonMapper` → `WaterJacksonModule` → `BeanSerializerModifier`/
`BeanDeserializerModifier`. To cover them, get the real `WaterJacksonMapper` component via
`@Inject` + `WaterTestExtension` and round-trip a plain test POJO implementing `BaseEntity`
through `mapper.getJacksonMapper()`. To hit the `activeView != null` branches use
`mapper.writerWithView(SomeClass.class)` / `mapper.readerWithView(SomeClass.class)` — the view
marker class needs no special annotations for this purpose. To hit the `ExpandableEntity`
extension-conversion branch, implement a test entity with `@JsonAnyGetter` on the extra-fields
getter and a `@JsonAnySetter`-annotated two-arg `(String,Object)` method (not `@JsonAnySetter`
directly on the bulk `Map` setter).
