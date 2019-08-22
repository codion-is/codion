module org.jminor.swing.framework.ui {
  requires org.slf4j;
  requires transitive org.jminor.swing.framework.model;
  requires transitive org.jminor.swing.common.ui;

  exports org.jminor.swing.framework.ui;
  exports org.jminor.swing.framework.ui.reporting;

  uses org.jminor.common.CredentialsProvider;
}