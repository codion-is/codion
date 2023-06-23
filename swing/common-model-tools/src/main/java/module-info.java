/**
 * Common Swing tools model classes.
 */
module is.codion.swing.common.tools {
  requires org.slf4j;
  requires org.jfree.jfreechart;
  requires jdk.management;
  requires transitive java.desktop;
  requires transitive is.codion.common.db;
  requires transitive is.codion.common.model;

  exports is.codion.swing.common.model.tools.loadtest;
  exports is.codion.swing.common.model.tools.randomizer;
}