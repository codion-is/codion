module dev.codion.swing.plugin.ikonli.foundation {
  requires dev.codion.swing.common.ui;
  requires dev.codion.swing.framework.ui;
  requires org.kordamp.ikonli.swing;
  requires org.kordamp.ikonli.foundation;

  exports dev.codion.swing.plugin.ikonli.foundation;

  provides dev.codion.swing.common.ui.icons.Icons
          with dev.codion.swing.plugin.ikonli.foundation.IkonliFoundationIcons;
  provides dev.codion.swing.framework.ui.icons.FrameworkIcons
          with dev.codion.swing.plugin.ikonli.foundation.IkonliFoundationFrameworkIcons;
}