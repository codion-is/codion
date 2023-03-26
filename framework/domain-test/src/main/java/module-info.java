/**
 * Unit test base classes.
 */
module is.codion.framework.domain.test {
  requires org.slf4j;
  requires org.junit.jupiter.api;
  requires transitive is.codion.framework.db.core;

  exports is.codion.framework.domain.entity.test;
}