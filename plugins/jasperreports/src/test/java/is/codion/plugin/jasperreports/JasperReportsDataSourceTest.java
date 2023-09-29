/*
 * Copyright (c) 2010 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.plugin.jasperreports;

import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.plugin.jasperreports.TestDomain.Department;

import net.sf.jasperreports.engine.JRField;
import net.sf.jasperreports.engine.JRPropertiesHolder;
import net.sf.jasperreports.engine.JRPropertiesMap;
import net.sf.jasperreports.engine.JRPropertyExpression;
import org.junit.jupiter.api.Test;

import java.util.List;

import static java.util.Collections.singletonList;

public class JasperReportsDataSourceTest {

  private static final Entities ENTITIES = new TestDomain().entities();

  @Test
  void iterator() throws Exception {
    Entity department = ENTITIES.builder(Department.TYPE)
            .with(Department.ID, 10)
            .with(Department.NAME, "name")
            .with(Department.LOCATION, "none")
            .build();
    EntityDefinition definition = ENTITIES.definition(Department.TYPE);
    List<Entity> entities = singletonList(department);
    JasperReportsDataSource<Entity> source =
            new JasperReportsDataSource<>(entities.iterator(), (entity, field) ->
                    entity.get(definition.attributes().get(field.getName())));
    while (source.next()) {
      JRField field = new TestField(Department.NAME.name());
      source.getFieldValue(field);
    }
  }

  private static class TestField implements JRField {
    private final String name;
    TestField(String name) {this.name = name;}
    @Override
    public String getName() {return name;}
    @Override
    public String getDescription() {return null;}
    @Override
    public void setDescription(String s) {}
    @Override
    public Class<String> getValueClass() {return null;}
    @Override
    public String getValueClassName() {return null;}
    @Override
    public boolean hasProperties() {return false;}
    @Override
    public JRPropertiesMap getPropertiesMap() {return null;}
    @Override
    public JRPropertiesHolder getParentProperties() {return null;}
    @Override
    public Object clone() {return null;}
    @Override
    public JRPropertyExpression[] getPropertyExpressions() {return new JRPropertyExpression[0];}
  }
}
