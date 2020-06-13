module is.codion.framework.demos.petclinic {
  requires is.codion.swing.framework.ui;
  requires is.codion.swing.plugin.ikonli.foundation;

  exports is.codion.framework.demos.petclinic.domain
          to is.codion.framework.db.local;
  exports is.codion.framework.demos.petclinic.model
          to is.codion.swing.framework.model;
  exports is.codion.framework.demos.petclinic.ui
          to is.codion.swing.framework.ui;
}