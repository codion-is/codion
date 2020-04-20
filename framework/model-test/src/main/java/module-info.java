module org.jminor.framework.model.tests {
  requires org.slf4j;
  requires org.junit.jupiter.api;
  requires org.jminor.framework.db.local;
  requires transitive org.jminor.common.model;
  requires transitive org.jminor.framework.db.core;
  requires transitive org.jminor.framework.model;

  exports org.jminor.framework.model.tests;
}