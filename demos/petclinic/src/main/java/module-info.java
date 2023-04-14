/**
 * Petclinic demo.
 */
module is.codion.framework.demos.petclinic {
  requires is.codion.swing.framework.ui;
  requires com.formdev.flatlaf.intellijthemes;

  exports is.codion.framework.demos.petclinic.model
          to is.codion.swing.framework.ui;
  exports is.codion.framework.demos.petclinic.ui
          to is.codion.swing.framework.ui;

  provides is.codion.framework.domain.Domain
          with is.codion.framework.demos.petclinic.domain.Petclinic;
}