module dev.codion.framework.demos.petclinic {
  requires dev.codion.swing.framework.ui;

  exports dev.codion.framework.demos.petclinic.domain.impl
          to dev.codion.framework.db.local;
  exports dev.codion.framework.demos.petclinic.model
          to dev.codion.swing.framework.model;
  exports dev.codion.framework.demos.petclinic.ui
          to dev.codion.swing.framework.ui;
}