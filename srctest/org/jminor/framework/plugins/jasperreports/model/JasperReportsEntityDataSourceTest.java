/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.plugins.jasperreports.model;

import org.jminor.framework.db.EntityDbConnectionTest;
import org.jminor.framework.demos.empdept.domain.EmpDept;
import org.jminor.framework.domain.Entity;

import net.sf.jasperreports.engine.JRField;
import net.sf.jasperreports.engine.JRPropertiesHolder;
import net.sf.jasperreports.engine.JRPropertiesMap;
import static org.junit.Assert.*;
import org.junit.Test;

import java.util.List;

public class JasperReportsEntityDataSourceTest {

  @Test
  public void test() throws Exception {
    final List<Entity> entities = EntityDbConnectionTest.DB_PROVIDER.getEntityDb().selectAll(EmpDept.T_DEPARTMENT);
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
    public TestField(final String name) {this.name = name;}
    public String getName() {return name;}
    public String getDescription() {return null;}
    public void setDescription(final String s) {}
    public Class getValueClass() {return null;}
    public String getValueClassName() {return null;}
    public boolean hasProperties() {return false;}
    public JRPropertiesMap getPropertiesMap() {return null;}
    public JRPropertiesHolder getParentProperties() {return null;}
    @SuppressWarnings({"CloneDoesntDeclareCloneNotSupportedException", "CloneDoesntCallSuperClone"})
    @Override public Object clone() {return null;}
  }
}
