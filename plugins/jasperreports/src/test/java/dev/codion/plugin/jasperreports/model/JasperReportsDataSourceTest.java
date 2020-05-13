/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package dev.codion.plugin.jasperreports.model;

import dev.codion.framework.domain.entity.Entities;
import dev.codion.framework.domain.entity.Entity;

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
  public void iterator() throws Exception {
    final Entity department = ENTITIES.entity(TestDomain.T_DEPARTMENT);
    department.put(TestDomain.DEPARTMENT_ID, 10);
    department.put(TestDomain.DEPARTMENT_NAME, "name");
    department.put(TestDomain.DEPARTMENT_LOCATION, "none");
    final List<Entity> entities = singletonList(department);
    final JasperReportsDataSource<Entity> source =
            new JasperReportsDataSource<>(entities.iterator(), (entity, field) -> entity.get(field.getName()));
    while (source.next()) {
      final JRField field = new TestField(TestDomain.DEPARTMENT_NAME);
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
    public Class getValueClass() {return null;}
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
