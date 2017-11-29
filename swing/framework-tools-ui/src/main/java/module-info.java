module org.jminor.swing.framework.tools.ui {
  requires java.desktop;
  requires slf4j.api;
  requires org.jminor.common.core;
  requires org.jminor.common.model;
  requires org.jminor.swing.common.ui;

  requires org.jminor.swing.framework.tools;
  exports org.jminor.swing.framework.tools.ui;
}