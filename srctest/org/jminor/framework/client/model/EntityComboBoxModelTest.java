/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model;

import org.jminor.common.db.criteria.SimpleCriteria;
import org.jminor.framework.db.EntityDbConnectionTest;
import org.jminor.framework.db.criteria.EntitySelectCriteria;
import org.jminor.framework.demos.empdept.domain.EmpDept;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.Property;

import static org.junit.Assert.*;
import org.junit.Test;

import java.util.Arrays;

/**
 * User: Bjorn Darri
 * Date: 11.10.2009
 * Time: 21:44:41
 */
public class EntityComboBoxModelTest {

  private final EntityComboBoxModel comboBoxModel;

  public EntityComboBoxModelTest() {
    new EmpDept();
    comboBoxModel = new DefaultEntityComboBoxModel(EmpDept.T_EMPLOYEE, EntityDbConnectionTest.DB_PROVIDER);
  }

  @Test
  public void testConstructor() {
    try {
      new DefaultEntityComboBoxModel(null, EntityDbConnectionTest.DB_PROVIDER);
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
    assertTrue(comboBoxModel.getSize() == 0);
    comboBoxModel.setSelectedItem("test");
    assertNull(comboBoxModel.getSelectedItem());
    comboBoxModel.refresh();
    assertTrue(comboBoxModel.getSize() > 0);
    assertTrue(comboBoxModel.isDataInitialized());

    try {
      comboBoxModel.setSelectedItem("test");
      fail("Should not be able to select a string");
    }
    catch (IllegalArgumentException e) {}

    try {
      comboBoxModel.setEntitySelectCriteria(new EntitySelectCriteria(EmpDept.T_DEPARTMENT));
      fail("Criteria entityID mismatch");
    }
    catch (RuntimeException e) {}

    final Entity clark = comboBoxModel.getDbProvider().getEntityDb().selectSingle(EmpDept.T_EMPLOYEE, EmpDept.EMPLOYEE_NAME, "CLARK");
    comboBoxModel.setSelectedItem(clark);
    assertEquals(clark, comboBoxModel.getSelectedEntity());
    comboBoxModel.setSelectedItem(null);
    comboBoxModel.setSelectedEntityByPrimaryKey(clark.getPrimaryKey());
    assertEquals(clark, comboBoxModel.getSelectedEntity());

    //test foreign key filtering
    final Entity sales = comboBoxModel.getDbProvider().getEntityDb().selectSingle(EmpDept.T_DEPARTMENT, EmpDept.DEPARTMENT_NAME, "SALES");
    comboBoxModel.setForeignKeyFilterEntities(EmpDept.EMPLOYEE_DEPARTMENT_FK, Arrays.asList(sales));
    for (int i = 0; i < comboBoxModel.getSize(); i++) {
      assertEquals(((Entity) comboBoxModel.getElementAt(0)).getForeignKeyValue(EmpDept.EMPLOYEE_DEPARTMENT_FK), sales);
    }
    final Entity research = comboBoxModel.getDbProvider().getEntityDb().selectSingle(EmpDept.T_DEPARTMENT, EmpDept.DEPARTMENT_NAME, "RESEARCH");
    comboBoxModel.createForeignKeyFilterComboBoxModel(EmpDept.EMPLOYEE_DEPARTMENT_FK).setSelectedItem(research);
    for (int i = 0; i < comboBoxModel.getSize(); i++) {
      assertEquals(((Entity) comboBoxModel.getElementAt(0)).getForeignKeyValue(EmpDept.EMPLOYEE_DEPARTMENT_FK), research);
    }

    comboBoxModel.clear();
    assertTrue(comboBoxModel.getSize() == 0);

    comboBoxModel.setEntitySelectCriteria(new EntitySelectCriteria(EmpDept.T_EMPLOYEE, new SimpleCriteria<Property>(" ename = 'CLARK'")));
    comboBoxModel.setForeignKeyFilterEntities(EmpDept.EMPLOYEE_DEPARTMENT_FK, null);

    comboBoxModel.forceRefresh();
    assertTrue(comboBoxModel.getSize() == 1);
  }
}