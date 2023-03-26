/**
 * Framework Swing tools model classes.
 */
module is.codion.swing.framework.tools {
  requires org.slf4j;
  requires transitive is.codion.swing.common.tools;
  requires transitive is.codion.swing.framework.model;

  exports is.codion.swing.framework.tools.explorer;
  exports is.codion.swing.framework.tools.loadtest;
  exports is.codion.swing.framework.tools.metadata;
}