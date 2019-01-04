module org.jminor.swing.common.tools {
  requires slf4j.api;
  requires jfreechart;
  requires transitive java.desktop;
  requires transitive org.jminor.common.model;

  exports org.jminor.swing.common.tools;
}