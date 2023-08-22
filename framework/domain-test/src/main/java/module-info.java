/**
 * Domain model unit test classes.<br>
 * <br>
 * {@link is.codion.framework.domain.entity.test.EntityTestUnit}<br>
 * {@link is.codion.framework.domain.entity.test.EntityTestUtil}<br>
 * @provides is.codion.framework.db.EntityConnectionProvider
 */
module is.codion.framework.domain.test {
  requires org.slf4j;
  requires org.junit.jupiter.api;
  requires transitive is.codion.framework.db.core;
  requires is.codion.framework.db.local;

  exports is.codion.framework.domain.entity.test;
}