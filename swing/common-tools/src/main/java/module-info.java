module org.jminor.swing.common.tools {
  requires jfreechart;
  requires jdk.management;
  requires transitive java.desktop;
  requires transitive org.jminor.common.model;

  exports org.jminor.swing.common.tools.loadtest;
}