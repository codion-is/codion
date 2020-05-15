module is.codion.framework.db.test {
  requires org.slf4j;
  requires org.junit.jupiter.api;
  requires transitive is.codion.framework.db.core;

  exports is.codion.framework.domain.entity.test;
}