module org.jminor.framework.demos.schemabrowser {
  requires org.jminor.swing.common.tools.ui;
  requires org.jminor.swing.framework.tools;
  requires org.jminor.swing.framework.ui;

  exports org.jminor.framework.demos.schemabrowser.domain
          to org.jminor.framework.db.local;
}