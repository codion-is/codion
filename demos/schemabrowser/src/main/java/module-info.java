module is.codion.framework.demos.schemabrowser {
  requires is.codion.swing.common.tools.ui;
  requires is.codion.swing.framework.tools;
  requires is.codion.swing.framework.ui;

  provides is.codion.framework.domain.Domain
          with is.codion.framework.demos.schemabrowser.domain.SchemaBrowser;
}