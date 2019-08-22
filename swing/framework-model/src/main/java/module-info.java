module org.jminor.swing.framework.model {
  requires org.slf4j;
  requires json;
  requires transitive org.jminor.framework.model;
  requires transitive org.jminor.swing.common.model;

  exports org.jminor.swing.framework.model;
  exports org.jminor.swing.framework.model.reporting;
}