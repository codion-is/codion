/**
 * Framework Swing tools model classes.
 */
module is.codion.swing.framework.model.tools {
  requires org.slf4j;
  requires transitive is.codion.swing.common.model.tools;
  requires transitive is.codion.swing.framework.model;

  exports is.codion.swing.framework.model.tools.explorer;
  exports is.codion.swing.framework.model.tools.loadtest;
  exports is.codion.swing.framework.model.tools.metadata;
}