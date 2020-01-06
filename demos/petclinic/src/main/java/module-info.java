module org.jminor.framework.demos.petclinic {
  requires org.jminor.swing.framework.ui;

  exports org.jminor.framework.demos.petclinic.domain.impl
          to org.jminor.framework.db.local;
  exports org.jminor.framework.demos.petclinic.model
          to org.jminor.swing.framework.model;
  exports org.jminor.framework.demos.petclinic.ui
          to org.jminor.swing.framework.ui;
}