module org.jminor.swing.common.tools {
  requires org.slf4j;
  requires jfreechart;
  requires transitive java.desktop;
  requires transitive org.jminor.common.model;

  exports org.jminor.swing.common.tools;
}