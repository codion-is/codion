module org.jminor.plugin.nextreports {
  requires java.desktop;
  requires nextreports.engine;
  requires org.jminor.common.db;

  exports org.jminor.framework.plugins.nextreports.model;
  exports org.jminor.framework.plugins.nextreports.swing;
}