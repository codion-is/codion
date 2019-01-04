module org.jminor.swing.framework.tools {
  requires slf4j.api;
  requires transitive org.jminor.framework.model;
  requires transitive org.jminor.swing.common.tools;
  requires transitive org.jminor.swing.common.model;

  exports org.jminor.swing.framework.tools;
}