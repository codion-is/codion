module org.jminor.swing.plugin.ikonli.foundation {
  requires org.jminor.swing.common.ui;
  requires org.jminor.swing.framework.ui;
  requires org.kordamp.ikonli.swing;
  requires org.kordamp.ikonli.foundation;

  exports org.jminor.swing.plugin.ikonli.foundation;

  provides org.jminor.swing.common.ui.icons.Icons
          with org.jminor.swing.plugin.ikonli.foundation.IkonliFoundationIcons;
  provides org.jminor.swing.framework.ui.icons.FrameworkIcons
          with org.jminor.swing.plugin.ikonli.foundation.IkonliFrameworkFoundationIcons;
}