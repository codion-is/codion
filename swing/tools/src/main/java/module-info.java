module org.jminor.common.tools {
  requires java.sql;
  requires java.desktop;
  requires slf4j.api;
  requires jfreechart;
  requires org.jminor.common.core;
  requires org.jminor.common.db;
  exports org.jminor.common.tools;
}