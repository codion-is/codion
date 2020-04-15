module org.jminor.framework.db.test {
  requires org.slf4j;
  requires org.junit.jupiter.api;
  requires transitive org.jminor.framework.db.core;

  exports org.jminor.framework.domain.entity.test;
}