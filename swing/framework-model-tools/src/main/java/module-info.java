/**
 * Framework Swing model tools, such as:<br>
 * <br>
 * {@link is.codion.swing.framework.model.tools.loadtest.EntityLoadTestModel}<br>
 */
module is.codion.swing.framework.model.tools {
  requires org.slf4j;
  requires transitive is.codion.swing.common.model.tools;
  requires transitive is.codion.swing.framework.model;

  exports is.codion.swing.framework.model.tools.explorer;
  exports is.codion.swing.framework.model.tools.loadtest;
  exports is.codion.swing.framework.model.tools.metadata;
}