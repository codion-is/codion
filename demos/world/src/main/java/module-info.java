module is.codion.framework.demos.world {
  requires is.codion.swing.framework.ui;
  requires is.codion.plugin.jasperreports;
  requires is.codion.plugin.jackson.json;
  requires jasperreports;
  requires org.jfree.jfreechart;
  requires org.json;
  requires jxmapviewer2;

  exports is.codion.framework.demos.world.domain;
  exports is.codion.framework.demos.world.model
          to is.codion.swing.framework.model;
  exports is.codion.framework.demos.world.ui
          to is.codion.swing.framework.ui;
  //for loading reports from classpath
  opens is.codion.framework.demos.world.ui;
  //for accessing default methods in EntityType interfaces
  opens is.codion.framework.demos.world.domain.api
          to is.codion.framework.domain;

  provides is.codion.framework.domain.Domain
          with is.codion.framework.demos.world.domain.WorldImpl;
  // tag::customSerializer[]
  provides is.codion.plugin.jackson.json.domain.EntityObjectMapperFactory
          with is.codion.framework.demos.world.domain.api.WorldObjectMapperFactory;
  // end::customSerializer[]
}