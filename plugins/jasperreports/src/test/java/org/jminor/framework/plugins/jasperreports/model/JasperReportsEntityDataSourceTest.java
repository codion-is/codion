/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.plugins.jasperreports.model;

import org.jminor.framework.db.criteria.EntityCriteriaUtil;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.Entity;

import net.sf.jasperreports.engine.JRField;
import net.sf.jasperreports.engine.JRPropertiesHolder;
import net.sf.jasperreports.engine.JRPropertiesMap;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class JasperReportsEntityDataSourceTest {

  static {
    TestDomain.init();
  }

  @Test(expected = IllegalArgumentException.class)
  public void constructorNullIterator() {
    new JasperReportsEntityDataSource(null);
  }

  @Test
  public void iterator() throws Exception {
    final Entity department = Entities.entity(TestDomain.T_DEPARTMENT);
    department.setValue(TestDomain.DEPARTMENT_ID, 10);
    department.setValue(TestDomain.DEPARTMENT_NAME, "name");
    department.setValue(TestDomain.DEPARTMENT_LOCATION, "none");
    final List<Entity> entities = Collections.singletonList(department);
    final JasperReportsEntityDataSource source = new JasperReportsEntityDataSource(entities.iterator());
    while (source.next()) {
      final Entity dept = source.getCurrentEntity();
      assertTrue(entities.contains(dept));
      final JRField field = new TestField(TestDomain.DEPARTMENT_NAME);
      assertEquals(dept.getValue(TestDomain.DEPARTMENT_NAME), source.getFieldValue(field));
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
    @SuppressWarnings({"CloneDoesntDeclareCloneNotSupportedException", "CloneDoesntCallSuperClone"})
    @Override public Object clone() {return null;}
  }
}
