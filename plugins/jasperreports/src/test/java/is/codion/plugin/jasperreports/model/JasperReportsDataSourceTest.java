/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.plugin.jasperreports.model;

import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;

import net.sf.jasperreports.engine.JRField;
import net.sf.jasperreports.engine.JRPropertiesHolder;
import net.sf.jasperreports.engine.JRPropertiesMap;
import net.sf.jasperreports.engine.JRPropertyExpression;
import org.junit.jupiter.api.Test;

import java.util.List;

import static java.util.Collections.singletonList;

public class JasperReportsDataSourceTest {

  private static final Entities ENTITIES = new TestDomain().getEntities();

  @Test
  void iterator() throws Exception {
    final Entity department = ENTITIES.builder(TestDomain.T_DEPARTMENT)
            .with(TestDomain.DEPARTMENT_ID, 10)
            .with(TestDomain.DEPARTMENT_NAME, "name")
            .with(TestDomain.DEPARTMENT_LOCATION, "none")
            .build();
    final EntityDefinition definition = ENTITIES.getDefinition(TestDomain.T_DEPARTMENT);
    final List<Entity> entities = singletonList(department);
    final JasperReportsDataSource<Entity> source =
            new JasperReportsDataSource<>(entities.iterator(), (entity, field) ->
                    entity.get(definition.getAttribute(field.getName())));
    while (source.next()) {
      final JRField field = new TestField(TestDomain.DEPARTMENT_NAME.getName());
      source.getFieldValue(field);
    }
  }

  private static class TestField implements JRField {
    private final String name;
    TestField(final String name) {this.name = name;}
    @Override
    public String getName() {return name;}
    @Override
    public String getDescription() {return null;}
    @Override
    public void setDescription(final String s) {}
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
