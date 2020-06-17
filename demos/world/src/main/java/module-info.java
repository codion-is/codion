module is.codion.framework.demos.world {
  requires is.codion.swing.framework.ui;
  requires is.codion.swing.plugin.ikonli.foundation;
  requires org.kordamp.ikonli.foundation;
  requires org.kordamp.ikonli.swing;
  requires jfreechart;

  exports is.codion.framework.demos.world.domain;
  exports is.codion.framework.demos.world.model
          to is.codion.swing.framework.model;
  exports is.codion.framework.demos.world.ui
          to is.codion.swing.framework.ui;
  //for accessing default methods in EntityType interfaces
  opens is.codion.framework.demos.world.domain.api
          to is.codion.framework.domain;
}