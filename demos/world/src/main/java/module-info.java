module org.jminor.framework.demos.world {
  requires org.jminor.swing.framework.ui;
  requires org.jminor.swing.plugin.ikonli.foundation;
  requires org.kordamp.ikonli.foundation;
  requires org.kordamp.ikonli.swing;
  requires jfreechart;

  exports org.jminor.framework.demos.world.domain;
  exports org.jminor.framework.demos.world.model
          to org.jminor.swing.framework.model;
  exports org.jminor.framework.demos.world.ui
          to org.jminor.swing.framework.ui;
}