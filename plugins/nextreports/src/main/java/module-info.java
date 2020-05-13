module dev.codion.plugin.nextreports {
  requires java.desktop;
  requires nextreports.engine;
  requires dev.codion.common.db;

  exports dev.codion.plugin.nextreports.model;
}