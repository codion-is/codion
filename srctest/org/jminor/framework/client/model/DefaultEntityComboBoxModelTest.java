/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model;

import org.jminor.common.db.criteria.SimpleCriteria;
import org.jminor.framework.db.EntityConnectionImplTest;
import org.jminor.framework.db.criteria.EntityCriteriaUtil;
import org.jminor.framework.demos.empdept.domain.EmpDept;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.Property;

import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.*;

public final class DefaultEntityComboBoxModelTest {

  private final DefaultEntityComboBoxModel comboBoxModel;

  public DefaultEntityComboBoxModelTest() {
    EmpDept.init();
    comboBoxModel = new DefaultEntityComboBoxModel(EmpDept.T_EMPLOYEE, EntityConnectionImplTest.DB_PROVIDER);
  }

  @Test
  public void testConstructor() {
    try {
      new DefaultEntityComboBoxModel(null, EntityConnectionImplTest.DB_PROVIDER);
      fail();
    }
    catch (IllegalArgumentException e) {}
    try {
      new DefaultEntityComboBoxModel(EmpDept.T_EMPLOYEE, null);
      fail();
    }
    catch (IllegalArgumentException e) {}
    try {
      new DefaultEntityComboBoxModel(null, null);
      fail();
    }
    catch (IllegalArgumentException e) {}
  }

  @Test
  public void test() throws Exception {
    assertEquals(EmpDept.T_EMPLOYEE, comboBoxModel.getEntityID());
    comboBoxModel.setStaticData(false);
    comboBoxModel.toString();
    assertTrue(comboBoxModel.getSize() == 0);
    assertNull(comboBoxModel.getSelectedItem());
    comboBoxModel.refresh();
    assertTrue(comboBoxModel.getSize() > 0);
    assertFalse(comboBoxModel.isCleared());

    try {
      comboBoxModel.setEntitySelectCriteria(EntityCriteriaUtil.selectCriteria(EmpDept.T_DEPARTMENT));
      fail("Criteria entityID mismatch");
    }
    catch (RuntimeException e) {}

    final Entity clark = comboBoxModel.getConnectionProvider().getConnection().selectSingle(EmpDept.T_EMPLOYEE, EmpDept.EMPLOYEE_NAME, "CLARK");
    comboBoxModel.setSelectedItem(clark);
    assertEquals(clark, comboBoxModel.getSelectedValue());
    comboBoxModel.setSelectedItem("test");
    assertEquals("Selecting a string should not change the selection", clark, comboBoxModel.getSelectedValue());
    comboBoxModel.setSelectedItem(null);
    comboBoxModel.setSelectedEntityByPrimaryKey(clark.getPrimaryKey());
    assertEquals(clark, comboBoxModel.getSelectedValue());

    //test foreign key filtering
    final Entity sales = comboBoxModel.getConnectionProvider().getConnection().selectSingle(EmpDept.T_DEPARTMENT, EmpDept.DEPARTMENT_NAME, "SALES");
    comboBoxModel.setForeignKeyFilterEntities(EmpDept.EMPLOYEE_DEPARTMENT_FK, Arrays.asList(sales));
    for (int i = 0; i < comboBoxModel.getSize(); i++) {
      assertEquals(((Entity) comboBoxModel.getElementAt(0)).getForeignKeyValue(EmpDept.EMPLOYEE_DEPARTMENT_FK), sales);
    }
    final Entity research = comboBoxModel.getConnectionProvider().getConnection().selectSingle(EmpDept.T_DEPARTMENT, EmpDept.DEPARTMENT_NAME, "RESEARCH");
    comboBoxModel.createForeignKeyFilterComboBoxModel(EmpDept.EMPLOYEE_DEPARTMENT_FK).setSelectedItem(research);
    for (int i = 0; i < comboBoxModel.getSize(); i++) {
      assertEquals(((Entity) comboBoxModel.getElementAt(0)).getForeignKeyValue(EmpDept.EMPLOYEE_DEPARTMENT_FK), research);
    }

    comboBoxModel.clear();
    assertTrue(comboBoxModel.getSize() == 0);

    comboBoxModel.setEntitySelectCriteria(EntityCriteriaUtil.selectCriteria(EmpDept.T_EMPLOYEE,
            new SimpleCriteria<Property.ColumnProperty>(" ename = 'CLARK'")));
    comboBoxModel.setForeignKeyFilterEntities(EmpDept.EMPLOYEE_DEPARTMENT_FK, null);

    comboBoxModel.forceRefresh();
    assertTrue(comboBoxModel.getSize() == 1);
  }
}