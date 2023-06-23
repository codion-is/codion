/**
 * Petstore demo.
 */
module is.codion.framework.demos.petstore {
  requires is.codion.swing.common.ui.tools;
  requires is.codion.swing.framework.model.tools;
  requires is.codion.swing.framework.ui;

  exports is.codion.framework.demos.petstore.model
          to is.codion.swing.framework.model, is.codion.swing.framework.ui;
  exports is.codion.framework.demos.petstore.ui
          to is.codion.swing.framework.ui;

  provides is.codion.framework.domain.Domain
          with is.codion.framework.demos.petstore.domain.Petstore;
}