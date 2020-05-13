module dev.codion.framework.db.test {
  requires org.slf4j;
  requires org.junit.jupiter.api;
  requires transitive dev.codion.framework.db.core;

  exports dev.codion.framework.domain.entity.test;
}