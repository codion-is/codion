module is.codion.framework.model.tests {
  requires org.slf4j;
  requires org.junit.jupiter.api;
  requires is.codion.framework.db.local;
  requires transitive is.codion.common.model;
  requires transitive is.codion.framework.db.core;
  requires transitive is.codion.framework.model;

  exports is.codion.framework.model.test;
}