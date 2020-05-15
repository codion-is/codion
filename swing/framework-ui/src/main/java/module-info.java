module is.codion.swing.framework.ui {
  requires org.slf4j;
  requires transitive is.codion.swing.framework.model;
  requires transitive is.codion.swing.common.ui;

  exports is.codion.swing.framework.ui;
  exports is.codion.swing.framework.ui.icons;

  uses is.codion.common.CredentialsProvider;
  uses is.codion.swing.framework.ui.icons.FrameworkIcons;

  provides is.codion.swing.framework.ui.icons.FrameworkIcons
          with is.codion.swing.framework.ui.icons.DefaultFrameworkIcons;
}