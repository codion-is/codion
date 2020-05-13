module dev.codion.swing.framework.ui {
  requires org.slf4j;
  requires transitive dev.codion.swing.framework.model;
  requires transitive dev.codion.swing.common.ui;

  exports dev.codion.swing.framework.ui;
  exports dev.codion.swing.framework.ui.icons;

  uses dev.codion.common.CredentialsProvider;
  uses dev.codion.swing.framework.ui.icons.FrameworkIcons;

  provides dev.codion.swing.framework.ui.icons.FrameworkIcons
          with dev.codion.swing.framework.ui.icons.DefaultFrameworkIcons;
}