module dev.codion.swing.framework.model {
  requires org.slf4j;
  requires org.json;
  requires transitive dev.codion.framework.model;
  requires transitive dev.codion.swing.common.model;

  exports dev.codion.swing.framework.model;
}