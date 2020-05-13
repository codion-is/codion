module dev.codion.swing.framework.tools {
  requires org.slf4j;
  requires transitive dev.codion.framework.model;
  requires transitive dev.codion.swing.common.tools;
  requires transitive dev.codion.swing.common.model;

  exports dev.codion.swing.framework.tools.generator;
  exports dev.codion.swing.framework.tools.loadtest;
}