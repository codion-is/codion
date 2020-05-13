module dev.codion.framework.demos.world {
  requires dev.codion.swing.framework.ui;
  requires dev.codion.swing.plugin.ikonli.foundation;
  requires org.kordamp.ikonli.foundation;
  requires org.kordamp.ikonli.swing;
  requires jfreechart;

  exports dev.codion.framework.demos.world.domain;
  exports dev.codion.framework.demos.world.model
          to dev.codion.swing.framework.model;
  exports dev.codion.framework.demos.world.ui
          to dev.codion.swing.framework.ui;
}