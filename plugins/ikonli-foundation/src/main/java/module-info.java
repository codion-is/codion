module is.codion.swing.plugin.ikonli.foundation {
  requires is.codion.swing.common.ui;
  requires is.codion.swing.framework.ui;
  requires org.kordamp.ikonli.swing;
  requires org.kordamp.ikonli.foundation;

  exports is.codion.swing.plugin.ikonli.foundation;

  provides is.codion.swing.common.ui.icons.Icons
          with is.codion.swing.plugin.ikonli.foundation.IkonliFoundationIcons;
  provides is.codion.swing.framework.ui.icons.FrameworkIcons
          with is.codion.swing.plugin.ikonli.foundation.IkonliFoundationFrameworkIcons;
}