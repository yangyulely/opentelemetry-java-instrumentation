/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.equalTo;
import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.satisfies;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.instrumentation.testing.junit.AgentInstrumentationExtension;
import io.opentelemetry.instrumentation.testing.junit.InstrumentationExtension;
import io.opentelemetry.sdk.testing.assertj.TraceAssert;
import io.opentelemetry.sdk.trace.data.StatusData;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;
import java.util.List;
import java.util.Optional;
import org.hibernate.Version;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.data.jpa.repository.JpaRepository;

public abstract class AbstractSpringJpaTest<
    ENTITY, REPOSITORY extends JpaRepository<ENTITY, Long>> {

  @RegisterExtension
  private static final InstrumentationExtension testing = AgentInstrumentationExtension.create();

  abstract ENTITY newCustomer(String firstName, String lastName);

  abstract Long id(ENTITY customer);

  abstract void setFirstName(ENTITY customer, String firstName);

  abstract Class<REPOSITORY> repositoryClass();

  abstract REPOSITORY repository();

  abstract List<ENTITY> findByLastName(REPOSITORY repository, String lastName);

  abstract List<ENTITY> findSpecialCustomers(REPOSITORY repository);

  abstract Optional<ENTITY> findOneByLastName(REPOSITORY repository, String lastName);

  void clearData() {
    testing.clearData();
  }

  @Test
  void testObjectMethod() {
    REPOSITORY repo = repository();

    testing.runWithSpan("toString test", repo::toString);

    // Asserting that a span is NOT created for toString
    testing.waitAndAssertTraces(
        trace ->
            trace
                .hasSize(1)
                .hasSpansSatisfyingExactly(
                    span -> span.hasName("toString test").hasTotalAttributeCount(0)));
  }

  static void assertHibernate4Trace(TraceAssert trace, String repoClassName) {
    trace
        .hasSize(2)
        .hasSpansSatisfyingExactly(
            span ->
                span.hasName("JpaCustomerRepository.save")
                    .hasKind(SpanKind.INTERNAL)
                    .hasAttributesSatisfyingExactly(
                        equalTo(SemanticAttributes.CODE_NAMESPACE, repoClassName),
                        equalTo(SemanticAttributes.CODE_FUNCTION, "save")),
            span ->
                span.hasName("INSERT test.JpaCustomer")
                    .hasKind(SpanKind.CLIENT)
                    .hasParent(trace.getSpan(0))
                    .hasAttributesSatisfyingExactly(
                        equalTo(SemanticAttributes.DB_SYSTEM, "hsqldb"),
                        equalTo(SemanticAttributes.DB_NAME, "test"),
                        equalTo(SemanticAttributes.DB_USER, "sa"),
                        equalTo(SemanticAttributes.DB_CONNECTION_STRING, "hsqldb:mem:"),
                        satisfies(
                            SemanticAttributes.DB_STATEMENT, val -> val.startsWith("insert ")),
                        equalTo(SemanticAttributes.DB_OPERATION, "INSERT"),
                        equalTo(SemanticAttributes.DB_SQL_TABLE, "JpaCustomer")));
  }

  static void assertHibernateTrace(TraceAssert trace, String repoClassName) {
    trace
        .hasSize(3)
        .hasSpansSatisfyingExactly(
            span ->
                span.hasName("JpaCustomerRepository.save")
                    .hasKind(SpanKind.INTERNAL)
                    .hasAttributesSatisfyingExactly(
                        equalTo(SemanticAttributes.CODE_NAMESPACE, repoClassName),
                        equalTo(SemanticAttributes.CODE_FUNCTION, "save")),
            span ->
                span.hasName("CALL test")
                    .hasKind(SpanKind.CLIENT)
                    .hasAttributesSatisfyingExactly(
                        equalTo(SemanticAttributes.DB_SYSTEM, "hsqldb"),
                        equalTo(SemanticAttributes.DB_NAME, "test"),
                        equalTo(SemanticAttributes.DB_USER, "sa"),
                        equalTo(SemanticAttributes.DB_CONNECTION_STRING, "hsqldb:mem:"),
                        satisfies(
                            SemanticAttributes.DB_STATEMENT,
                            val -> val.startsWith("call next value for ")),
                        equalTo(SemanticAttributes.DB_OPERATION, "CALL")),
            span ->
                span.hasName("INSERT test.JpaCustomer")
                    .hasKind(SpanKind.CLIENT)
                    .hasParent(trace.getSpan(0))
                    .hasAttributesSatisfyingExactly(
                        equalTo(SemanticAttributes.DB_SYSTEM, "hsqldb"),
                        equalTo(SemanticAttributes.DB_NAME, "test"),
                        equalTo(SemanticAttributes.DB_USER, "sa"),
                        equalTo(SemanticAttributes.DB_CONNECTION_STRING, "hsqldb:mem:"),
                        satisfies(
                            SemanticAttributes.DB_STATEMENT, val -> val.startsWith("insert ")),
                        equalTo(SemanticAttributes.DB_OPERATION, "INSERT"),
                        equalTo(SemanticAttributes.DB_SQL_TABLE, "JpaCustomer")));
  }

  @Test
  void testCrud() {
    boolean isHibernate4 = Version.getVersionString().startsWith("4.");
    REPOSITORY repo = repository();
    String repoClassName = repositoryClass().getName();

    ENTITY customer = newCustomer("Bob", "Anonymous");

    assertNull(id(customer));
    assertFalse(repo.findAll().iterator().hasNext());

    testing.waitAndAssertTraces(
        trace ->
            trace
                .hasSize(2)
                .hasSpansSatisfyingExactly(
                    span ->
                        span.hasName("JpaCustomerRepository.findAll")
                            .hasKind(SpanKind.INTERNAL)
                            .hasAttributesSatisfyingExactly(
                                equalTo(SemanticAttributes.CODE_NAMESPACE, repoClassName),
                                equalTo(SemanticAttributes.CODE_FUNCTION, "findAll")),
                    span ->
                        span.hasName("SELECT test.JpaCustomer")
                            .hasKind(SpanKind.CLIENT)
                            .hasParent(trace.getSpan(0))
                            .hasAttributesSatisfyingExactly(
                                equalTo(SemanticAttributes.DB_SYSTEM, "hsqldb"),
                                equalTo(SemanticAttributes.DB_NAME, "test"),
                                equalTo(SemanticAttributes.DB_USER, "sa"),
                                equalTo(SemanticAttributes.DB_CONNECTION_STRING, "hsqldb:mem:"),
                                satisfies(
                                    SemanticAttributes.DB_STATEMENT,
                                    val -> val.startsWith("select ")),
                                equalTo(SemanticAttributes.DB_OPERATION, "SELECT"),
                                equalTo(SemanticAttributes.DB_SQL_TABLE, "JpaCustomer"))));
    clearData();

    repo.save(customer);
    assertNotNull(id(customer));
    Long savedId = id(customer);
    if (isHibernate4) {
      testing.waitAndAssertTraces(trace -> assertHibernate4Trace(trace, repoClassName));
    } else {
      testing.waitAndAssertTraces(trace -> assertHibernateTrace(trace, repoClassName));
    }
    clearData();

    setFirstName(customer, "Bill");
    repo.save(customer);
    assertEquals(id(customer), savedId);
    testing.waitAndAssertTraces(
        trace ->
            trace
                .hasSize(3)
                .hasSpansSatisfyingExactly(
                    span ->
                        span.hasName("JpaCustomerRepository.save")
                            .hasKind(SpanKind.INTERNAL)
                            .hasAttributesSatisfyingExactly(
                                equalTo(SemanticAttributes.CODE_NAMESPACE, repoClassName),
                                equalTo(SemanticAttributes.CODE_FUNCTION, "save")),
                    span ->
                        span.hasName("SELECT test.JpaCustomer")
                            .hasKind(SpanKind.CLIENT)
                            .hasParent(trace.getSpan(0))
                            .hasAttributesSatisfyingExactly(
                                equalTo(SemanticAttributes.DB_SYSTEM, "hsqldb"),
                                equalTo(SemanticAttributes.DB_NAME, "test"),
                                equalTo(SemanticAttributes.DB_USER, "sa"),
                                equalTo(SemanticAttributes.DB_CONNECTION_STRING, "hsqldb:mem:"),
                                satisfies(
                                    SemanticAttributes.DB_STATEMENT,
                                    val -> val.startsWith("select ")),
                                equalTo(SemanticAttributes.DB_OPERATION, "SELECT"),
                                equalTo(SemanticAttributes.DB_SQL_TABLE, "JpaCustomer")),
                    span ->
                        span.hasName("UPDATE test.JpaCustomer")
                            .hasKind(SpanKind.CLIENT)
                            .hasParent(trace.getSpan(0))
                            .hasAttributesSatisfyingExactly(
                                equalTo(SemanticAttributes.DB_SYSTEM, "hsqldb"),
                                equalTo(SemanticAttributes.DB_NAME, "test"),
                                equalTo(SemanticAttributes.DB_USER, "sa"),
                                equalTo(SemanticAttributes.DB_CONNECTION_STRING, "hsqldb:mem:"),
                                satisfies(
                                    SemanticAttributes.DB_STATEMENT,
                                    val -> val.startsWith("update ")),
                                equalTo(SemanticAttributes.DB_OPERATION, "UPDATE"),
                                equalTo(SemanticAttributes.DB_SQL_TABLE, "JpaCustomer"))));
    clearData();

    customer = findByLastName(repo, "Anonymous").get(0);
    testing.waitAndAssertTraces(
        trace ->
            trace
                .hasSize(2)
                .hasSpansSatisfyingExactly(
                    span ->
                        span.hasName("JpaCustomerRepository.findByLastName")
                            .hasKind(SpanKind.INTERNAL)
                            .hasAttributesSatisfyingExactly(
                                equalTo(SemanticAttributes.CODE_NAMESPACE, repoClassName),
                                equalTo(SemanticAttributes.CODE_FUNCTION, "findByLastName")),
                    span ->
                        span.hasName("SELECT test.JpaCustomer")
                            .hasKind(SpanKind.CLIENT)
                            .hasParent(trace.getSpan(0))
                            .hasAttributesSatisfyingExactly(
                                equalTo(SemanticAttributes.DB_SYSTEM, "hsqldb"),
                                equalTo(SemanticAttributes.DB_NAME, "test"),
                                equalTo(SemanticAttributes.DB_USER, "sa"),
                                equalTo(SemanticAttributes.DB_CONNECTION_STRING, "hsqldb:mem:"),
                                satisfies(
                                    SemanticAttributes.DB_STATEMENT,
                                    val -> val.startsWith("select ")),
                                equalTo(SemanticAttributes.DB_OPERATION, "SELECT"),
                                equalTo(SemanticAttributes.DB_SQL_TABLE, "JpaCustomer"))));
    clearData();

    repo.delete(customer);
    testing.waitAndAssertTraces(
        trace ->
            trace
                .hasSize(3)
                .hasSpansSatisfyingExactly(
                    span ->
                        span.hasName("JpaCustomerRepository.delete")
                            .hasKind(SpanKind.INTERNAL)
                            .hasAttributesSatisfyingExactly(
                                equalTo(SemanticAttributes.CODE_NAMESPACE, repoClassName),
                                equalTo(SemanticAttributes.CODE_FUNCTION, "delete")),
                    span ->
                        span.hasName("SELECT test.JpaCustomer")
                            .hasKind(SpanKind.CLIENT)
                            .hasParent(trace.getSpan(0))
                            .hasAttributesSatisfyingExactly(
                                equalTo(SemanticAttributes.DB_SYSTEM, "hsqldb"),
                                equalTo(SemanticAttributes.DB_NAME, "test"),
                                equalTo(SemanticAttributes.DB_USER, "sa"),
                                equalTo(SemanticAttributes.DB_CONNECTION_STRING, "hsqldb:mem:"),
                                satisfies(
                                    SemanticAttributes.DB_STATEMENT,
                                    val -> val.startsWith("select ")),
                                equalTo(SemanticAttributes.DB_OPERATION, "SELECT"),
                                equalTo(SemanticAttributes.DB_SQL_TABLE, "JpaCustomer")),
                    span ->
                        span.hasName("DELETE test.JpaCustomer")
                            .hasKind(SpanKind.CLIENT)
                            .hasParent(trace.getSpan(0))
                            .hasAttributesSatisfyingExactly(
                                equalTo(SemanticAttributes.DB_SYSTEM, "hsqldb"),
                                equalTo(SemanticAttributes.DB_NAME, "test"),
                                equalTo(SemanticAttributes.DB_USER, "sa"),
                                equalTo(SemanticAttributes.DB_CONNECTION_STRING, "hsqldb:mem:"),
                                satisfies(
                                    SemanticAttributes.DB_STATEMENT,
                                    val -> val.startsWith("delete ")),
                                equalTo(SemanticAttributes.DB_OPERATION, "DELETE"),
                                equalTo(SemanticAttributes.DB_SQL_TABLE, "JpaCustomer"))));
  }

  @Test
  void testCustomRepositoryMethod() {
    REPOSITORY repo = repository();
    String repoClassName = repositoryClass().getName();
    List<ENTITY> customers = findSpecialCustomers(repo);

    assertTrue(customers.isEmpty());

    testing.waitAndAssertTraces(
        trace ->
            trace
                .hasSize(2)
                .hasSpansSatisfyingExactly(
                    span ->
                        span.hasName("JpaCustomerRepository.findSpecialCustomers")
                            .hasKind(SpanKind.INTERNAL)
                            .hasAttributesSatisfyingExactly(
                                equalTo(SemanticAttributes.CODE_NAMESPACE, repoClassName),
                                equalTo(SemanticAttributes.CODE_FUNCTION, "findSpecialCustomers")),
                    span ->
                        span.hasName("SELECT test.JpaCustomer")
                            .hasKind(SpanKind.CLIENT)
                            .hasParent(trace.getSpan(0))
                            .hasAttributesSatisfyingExactly(
                                equalTo(SemanticAttributes.DB_SYSTEM, "hsqldb"),
                                equalTo(SemanticAttributes.DB_NAME, "test"),
                                equalTo(SemanticAttributes.DB_USER, "sa"),
                                equalTo(SemanticAttributes.DB_CONNECTION_STRING, "hsqldb:mem:"),
                                satisfies(
                                    SemanticAttributes.DB_STATEMENT,
                                    val -> val.startsWith("select ")),
                                equalTo(SemanticAttributes.DB_OPERATION, "SELECT"),
                                equalTo(SemanticAttributes.DB_SQL_TABLE, "JpaCustomer"))));
  }

  @Test
  void testFailedRepositoryMethod() {
    // given
    REPOSITORY repo = repository();
    String repoClassName = repositoryClass().getName();

    String commonLastName = "Smith";
    repo.save(newCustomer("Alice", commonLastName));
    repo.save(newCustomer("Bob", commonLastName));
    clearData();

    // when
    IncorrectResultSizeDataAccessException expectedException =
        catchThrowableOfType(
            () -> findOneByLastName(repo, commonLastName),
            IncorrectResultSizeDataAccessException.class);

    // then
    assertNotNull(expectedException);
    testing.waitAndAssertTraces(
        trace ->
            trace
                .hasSize(2)
                .hasSpansSatisfyingExactly(
                    span ->
                        span.hasName("JpaCustomerRepository.findOneByLastName")
                            .hasKind(SpanKind.INTERNAL)
                            .hasStatus(StatusData.error())
                            .hasException(expectedException)
                            .hasAttributesSatisfyingExactly(
                                equalTo(SemanticAttributes.CODE_NAMESPACE, repoClassName),
                                equalTo(SemanticAttributes.CODE_FUNCTION, "findOneByLastName")),
                    span ->
                        span.hasName("SELECT test.JpaCustomer")
                            .hasKind(SpanKind.CLIENT)
                            .hasParent(trace.getSpan(0))
                            .hasAttributesSatisfyingExactly(
                                equalTo(SemanticAttributes.DB_SYSTEM, "hsqldb"),
                                equalTo(SemanticAttributes.DB_NAME, "test"),
                                equalTo(SemanticAttributes.DB_USER, "sa"),
                                equalTo(SemanticAttributes.DB_CONNECTION_STRING, "hsqldb:mem:"),
                                satisfies(
                                    SemanticAttributes.DB_STATEMENT,
                                    val -> val.startsWith("select ")),
                                equalTo(SemanticAttributes.DB_OPERATION, "SELECT"),
                                equalTo(SemanticAttributes.DB_SQL_TABLE, "JpaCustomer"))));
  }
}
