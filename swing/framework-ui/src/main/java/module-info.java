module org.jminor.swing.framework.ui {
  requires org.slf4j;
  requires transitive org.jminor.swing.framework.model;
  requires transitive org.jminor.swing.common.ui;

  exports org.jminor.swing.framework.ui;

  uses org.jminor.common.CredentialsProvider;
}