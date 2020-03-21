/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.plugin.jasperreports.model;

import org.jminor.framework.domain.Domain;
import org.jminor.framework.domain.entity.Entity;

import net.sf.jasperreports.engine.JRField;
import net.sf.jasperreports.engine.JRPropertiesHolder;
import net.sf.jasperreports.engine.JRPropertiesMap;
import net.sf.jasperreports.engine.JRPropertyExpression;
import org.junit.jupiter.api.Test;

import java.util.List;

import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.*;

public class JasperReportsEntityDataSourceTest {

  private static final Domain DOMAIN = new TestDomain();

  @Test
  public void constructorNullIterator() {
    assertThrows(NullPointerException.class, () -> new JasperReportsEntityDataSource(null));
  }

  @Test
  public void iterator() throws Exception {
    final Entity department = DOMAIN.entity(TestDomain.T_DEPARTMENT);
    department.put(TestDomain.DEPARTMENT_ID, 10);
    department.put(TestDomain.DEPARTMENT_NAME, "name");
    department.put(TestDomain.DEPARTMENT_LOCATION, "none");
    final List<Entity> entities = singletonList(department);
    final JasperReportsEntityDataSource source = new JasperReportsEntityDataSource(entities.iterator());
    while (source.next()) {
      final Entity dept = source.getCurrentEntity();
      assertTrue(entities.contains(dept));
      final JRField field = new TestField(TestDomain.DEPARTMENT_NAME);
      assertEquals(dept.get(TestDomain.DEPARTMENT_NAME), source.getFieldValue(field));
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
