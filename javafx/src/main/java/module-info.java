module org.jminor.javafx.framework {
  requires slf4j.api;
  requires javafx.graphics;
  requires javafx.controls;
  requires java.sql;
  requires json;
  requires org.jminor.common.core;
  requires org.jminor.common.db;
  requires org.jminor.common.model;
  requires org.jminor.framework.db.core;
  requires org.jminor.framework.model;

  exports org.jminor.javafx.framework.model;
  exports org.jminor.javafx.framework.ui;
  exports org.jminor.javafx.framework.ui.values;
}