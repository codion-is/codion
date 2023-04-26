/**
 * Framework Swing UI classes.
 */
module is.codion.swing.framework.ui {
  requires org.slf4j;
  requires transitive org.kordamp.ikonli.core;
  requires org.kordamp.ikonli.swing;
  requires transitive is.codion.swing.framework.model;
  requires transitive is.codion.swing.common.ui;

  exports is.codion.swing.framework.ui;
  exports is.codion.swing.framework.ui.component;
  exports is.codion.swing.framework.ui.icons;

  opens is.codion.swing.framework.ui.icons;

  uses is.codion.swing.framework.ui.icons.FrameworkIcons;

  provides is.codion.swing.framework.ui.icons.FrameworkIcons
          with is.codion.swing.framework.ui.icons.DefaultFrameworkIcons;
  provides org.kordamp.ikonli.IkonHandler
          with is.codion.swing.framework.ui.icons.FrameworkIkonHandler;
}