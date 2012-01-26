/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.plugins.jasperreports.model;

import org.jminor.framework.db.EntityConnectionImplTest;
import org.jminor.framework.demos.empdept.domain.EmpDept;
import org.jminor.framework.domain.Entity;

import net.sf.jasperreports.engine.JRField;
import net.sf.jasperreports.engine.JRPropertiesHolder;
import net.sf.jasperreports.engine.JRPropertiesMap;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class JasperReportsEntityDataSourceTest {

  @Test
  public void test() throws Exception {
    final List<Entity> entities = EntityConnectionImplTest.CONNECTION_PROVIDER.getConnection().selectAll(EmpDept.T_DEPARTMENT);
    try {
      new JasperReportsEntityDataSource(null);
      fail();
    }
    catch (Exception e) {}
    final JasperReportsEntityDataSource source = new JasperReportsEntityDataSource(entities.iterator());
    while (source.next()) {
      final Entity dept = source.getCurrentEntity();
      assertTrue(entities.contains(dept));
      final JRField field = new TestField(EmpDept.DEPARTMENT_NAME);
      assertEquals(dept.getValue(EmpDept.DEPARTMENT_NAME), source.getFieldValue(field));
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
