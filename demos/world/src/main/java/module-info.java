module org.jminor.framework.demos.world {
  requires org.jminor.swing.framework.ui;
  requires jfreechart;

  exports org.jminor.framework.demos.world.domain;
  exports org.jminor.framework.demos.world.ui
          to org.jminor.swing.framework.ui;
}