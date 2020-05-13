module dev.codion.swing.common.tools {
  requires org.slf4j;
  requires jfreechart;
  requires jdk.management;
  requires transitive java.desktop;
  requires transitive dev.codion.common.model;

  exports dev.codion.swing.common.tools.loadtest;
  exports dev.codion.swing.common.tools.randomizer;
}