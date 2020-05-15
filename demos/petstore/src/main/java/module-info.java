module is.codion.framework.demos.petstore {
  requires is.codion.swing.common.tools.ui;
  requires is.codion.swing.framework.tools;
  requires is.codion.swing.framework.ui;

  exports is.codion.framework.demos.petstore.ui
          to is.codion.swing.framework.ui;
}