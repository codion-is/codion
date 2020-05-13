module dev.codion.framework.model.tests {
  requires org.slf4j;
  requires org.junit.jupiter.api;
  requires dev.codion.framework.db.local;
  requires transitive dev.codion.common.model;
  requires transitive dev.codion.framework.db.core;
  requires transitive dev.codion.framework.model;

  exports dev.codion.framework.model.tests;
}