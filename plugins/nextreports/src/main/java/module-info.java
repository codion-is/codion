module org.jminor.framework.plugins.nextreports {
  requires java.sql;
  requires nextreports.engine;
  requires org.jminor.common.core;
  requires org.jminor.common.db;

  exports org.jminor.framework.plugins.nextreports.model;
  exports org.jminor.framework.plugins.nextreports.swing;
}