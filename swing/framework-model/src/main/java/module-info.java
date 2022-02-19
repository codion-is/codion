module is.codion.swing.framework.model {
  requires org.slf4j;
  requires org.json;
  requires transitive is.codion.framework.model;
  requires transitive is.codion.swing.common.model;

  exports is.codion.swing.framework.model;
}