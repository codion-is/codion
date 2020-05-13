module dev.codion.framework.demos.petstore {
  requires dev.codion.swing.common.tools.ui;
  requires dev.codion.swing.framework.tools;
  requires dev.codion.swing.framework.ui;

  exports dev.codion.framework.demos.petstore.ui
          to dev.codion.swing.framework.ui;
}