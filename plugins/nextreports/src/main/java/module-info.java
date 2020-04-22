module org.jminor.plugin.nextreports {
  requires java.desktop;
  requires nextreports.engine;
  requires org.jminor.common.db;

  exports org.jminor.plugin.nextreports.model;
}