module is.codion.swing.framework.tools {
  requires org.slf4j;
  requires transitive is.codion.framework.model;
  requires transitive is.codion.swing.common.tools;
  requires transitive is.codion.swing.common.model;

  exports is.codion.swing.framework.tools.explorer;
  exports is.codion.swing.framework.tools.loadtest;
  exports is.codion.swing.framework.tools.metadata;
}