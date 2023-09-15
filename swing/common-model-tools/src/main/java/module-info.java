/**
 * Common Swing model tools, such as:<br>
 * <br>
 * {@link is.codion.swing.common.model.tools.loadtest.LoadTestModel}<br>
 * {@link is.codion.swing.common.model.tools.loadtest.UsageScenario}<br>
 */
module is.codion.swing.common.model.tools {
  requires org.slf4j;
  requires org.jfree.jfreechart;
  requires jdk.management;
  requires transitive java.desktop;
  requires transitive is.codion.common.db;
  requires transitive is.codion.common.model;
  requires transitive is.codion.swing.common.model;

  exports is.codion.swing.common.model.tools.loadtest;
  exports is.codion.swing.common.model.tools.randomizer;
}