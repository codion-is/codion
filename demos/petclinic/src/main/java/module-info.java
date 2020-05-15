module is.codion.framework.demos.petclinic {
  requires is.codion.swing.framework.ui;

  exports is.codion.framework.demos.petclinic.domain.impl
          to is.codion.framework.db.local;
  exports is.codion.framework.demos.petclinic.model
          to is.codion.swing.framework.model;
  exports is.codion.framework.demos.petclinic.ui
          to is.codion.swing.framework.ui;
}