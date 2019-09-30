module org.jminor.framework.demos.chinook {
  requires org.jminor.framework.db.local;
  requires org.jminor.swing.common.tools.ui;
  requires org.jminor.swing.framework.tools;
  requires org.jminor.swing.framework.ui;
  requires org.jminor.javafx.framework;
  requires org.jminor.framework.plugins.jasperreports;

  exports org.jminor.framework.demos.chinook.beans.ui
          to org.jminor.swing.framework.ui;
  exports org.jminor.framework.demos.chinook.domain.impl
          to org.jminor.framework.db.local;
}