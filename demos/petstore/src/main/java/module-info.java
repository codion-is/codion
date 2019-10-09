module org.jminor.framework.demos.petstore {
  requires org.jminor.swing.common.tools.ui;
  requires org.jminor.swing.framework.tools;
  requires org.jminor.swing.framework.ui;

  exports org.jminor.framework.demos.petstore.beans.ui
          to org.jminor.swing.framework.ui;
}