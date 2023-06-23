/**
 * Schema browser demo.
 */
module is.codion.framework.demos.schemabrowser {
  requires is.codion.swing.common.ui.tools;
  requires is.codion.swing.framework.model.tools;
  requires is.codion.swing.framework.ui;

  exports is.codion.framework.demos.schemabrowser.client.ui
          to is.codion.swing.framework.ui;

  provides is.codion.framework.domain.Domain
          with is.codion.framework.demos.schemabrowser.domain.SchemaBrowser;
}