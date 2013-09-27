/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model;

import org.jminor.common.db.criteria.SimpleCriteria;
import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.model.EventListener;
import org.jminor.common.model.FilterCriteria;
import org.jminor.framework.db.DefaultEntityConnectionTest;
import org.jminor.framework.db.criteria.EntityCriteriaUtil;
import org.jminor.framework.demos.empdept.domain.EmpDept;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.Property;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import static org.junit.Assert.*;

public final class DefaultEntityComboBoxModelTest {

  private final DefaultEntityComboBoxModel comboBoxModel;

  public DefaultEntityComboBoxModelTest() {
    EmpDept.init();
    comboBoxModel = new DefaultEntityComboBoxModel(EmpDept.T_EMPLOYEE, DefaultEntityConnectionTest.CONNECTION_PROVIDER);
  }

  @Test(expected = IllegalArgumentException.class)
  public void constructorNullEntityID() {
    new DefaultEntityComboBoxModel(null, DefaultEntityConnectionTest.CONNECTION_PROVIDER);
  }

  @Test(expected = IllegalArgumentException.class)
  public void constructorNullConnectionProvider() {
    new DefaultEntityComboBoxModel(EmpDept.T_EMPLOYEE, null);
  }

  @Test
  public void setForeignKeyFilterEntities() throws Exception {
    comboBoxModel.refresh();
    final Entity blake = comboBoxModel.getConnectionProvider().getConnection().selectSingle(EmpDept.T_EMPLOYEE, EmpDept.EMPLOYEE_NAME, "BLAKE");
    comboBoxModel.setForeignKeyFilterEntities(EmpDept.EMPLOYEE_MGR_FK, Arrays.asList(blake));
    assertEquals(6, comboBoxModel.getSize());
    for (int i = 0; i < comboBoxModel.getSize(); i++) {
      final Entity item = (Entity) comboBoxModel.getElementAt(i);
      if (item.isValueNull(EmpDept.EMPLOYEE_MGR_FK)) {
        assertEquals("KING", item.getStringValue(EmpDept.EMPLOYEE_NAME));
      }
      else {
        assertEquals(item.getForeignKeyValue(EmpDept.EMPLOYEE_MGR_FK), blake);
      }
    }

    final Entity sales = comboBoxModel.getConnectionProvider().getConnection().selectSingle(EmpDept.T_DEPARTMENT, EmpDept.DEPARTMENT_NAME, "SALES");
    comboBoxModel.setForeignKeyFilterEntities(EmpDept.EMPLOYEE_DEPARTMENT_FK, Arrays.asList(sales));
    assertEquals(2, comboBoxModel.getSize());
    for (int i = 0; i < comboBoxModel.getSize(); i++) {
      final Entity item = (Entity) comboBoxModel.getElementAt(i);
      assertEquals(item.getForeignKeyValue(EmpDept.EMPLOYEE_DEPARTMENT_FK), sales);
      assertEquals(item.getForeignKeyValue(EmpDept.EMPLOYEE_MGR_FK), blake);
    }

    final Entity accounting = comboBoxModel.getConnectionProvider().getConnection().selectSingle(EmpDept.T_DEPARTMENT, EmpDept.DEPARTMENT_NAME, "ACCOUNTING");
    final EntityComboBoxModel deptComboBoxModel = comboBoxModel.createForeignKeyFilterComboBoxModel(EmpDept.EMPLOYEE_DEPARTMENT_FK);
    deptComboBoxModel.setSelectedItem(accounting);
    assertEquals(4, comboBoxModel.getSize());
    for (int i = 0; i < comboBoxModel.getSize(); i++) {
      final Entity item = (Entity) comboBoxModel.getElementAt(i);
      assertEquals(item.getForeignKeyValue(EmpDept.EMPLOYEE_DEPARTMENT_FK), accounting);
      if (item.isValueNull(EmpDept.EMPLOYEE_MGR_FK)) {
        assertEquals("KING", item.getStringValue(EmpDept.EMPLOYEE_NAME));
      }
      else {
        assertEquals(item.getForeignKeyValue(EmpDept.EMPLOYEE_MGR_FK), blake);
      }
    }
    for (final Entity employee : comboBoxModel.getAllItems()) {
      if (employee.getForeignKeyValue(EmpDept.EMPLOYEE_DEPARTMENT_FK).equals(accounting)) {
        comboBoxModel.setSelectedItem(employee);
        break;
      }
    }
    assertEquals(accounting, deptComboBoxModel.getSelectedValue());
  }

  @Test(expected = IllegalArgumentException.class)
  public void setEntitySelectCriteriaEntityIDMismatch() {
    comboBoxModel.setEntitySelectCriteria(EntityCriteriaUtil.selectCriteria(EmpDept.T_DEPARTMENT));
  }

  @Test
  public void setEntitySelectCriteriaNullValueDefaultCriteria() {
    comboBoxModel.setEntitySelectCriteria(null);
  }

  @Test
  public void setSelectedEntityByPrimaryKey() throws DatabaseException {
    comboBoxModel.refresh();
    final Entity clark = comboBoxModel.getConnectionProvider().getConnection().selectSingle(EmpDept.T_EMPLOYEE, EmpDept.EMPLOYEE_NAME, "CLARK");
    comboBoxModel.setSelectedEntityByPrimaryKey(clark.getPrimaryKey());
    assertEquals(clark, comboBoxModel.getSelectedValue());
    comboBoxModel.setSelectedItem(null);
    assertNull(comboBoxModel.getSelectedValue());
    comboBoxModel.setFilterCriteria(new FilterCriteria.RejectAllCriteria<org.jminor.framework.domain.Entity>());
    comboBoxModel.setSelectedEntityByPrimaryKey(clark.getPrimaryKey());
    assertEquals(clark, comboBoxModel.getSelectedValue());
    final Entity.Key nobodyPK = Entities.key(EmpDept.T_EMPLOYEE);
    nobodyPK.setValue(EmpDept.EMPLOYEE_ID, -1);
    comboBoxModel.setSelectedEntityByPrimaryKey(nobodyPK);
    assertEquals(clark, comboBoxModel.getSelectedValue());
  }

  @Test(expected = IllegalArgumentException.class)
  public void setSelectedEntityByPrimaryKeyNullValue() {
    comboBoxModel.setSelectedEntityByPrimaryKey(null);
  }

  @Test
  public void test() throws DatabaseException {
    final Collection<Object> refreshed = new ArrayList<>();
    final EventListener refreshListener = new EventListener() {
      @Override
      public void eventOccurred() {
        refreshed.add(new Object());
      }
    };
    comboBoxModel.addRefreshListener(refreshListener);
    assertEquals(EmpDept.T_EMPLOYEE, comboBoxModel.getEntityID());
    comboBoxModel.setStaticData(false);
    comboBoxModel.toString();
    assertTrue(comboBoxModel.getSize() == 0);
    assertNull(comboBoxModel.getSelectedItem());
    comboBoxModel.refresh();
    assertTrue(comboBoxModel.getSize() > 0);
    assertFalse(comboBoxModel.isCleared());

    final Entity clark = comboBoxModel.getConnectionProvider().getConnection().selectSingle(EmpDept.T_EMPLOYEE, EmpDept.EMPLOYEE_NAME, "CLARK");
    comboBoxModel.setSelectedItem(clark);
    assertEquals(clark, comboBoxModel.getSelectedValue());
    comboBoxModel.setSelectedItem("test");
    assertEquals("Selecting a string should not change the selection", clark, comboBoxModel.getSelectedValue());

    comboBoxModel.clear();
    assertTrue(comboBoxModel.getSize() == 0);

    comboBoxModel.setEntitySelectCriteria(EntityCriteriaUtil.selectCriteria(EmpDept.T_EMPLOYEE,
            new SimpleCriteria<Property.ColumnProperty>(" ename = 'CLARK'")));
    comboBoxModel.setForeignKeyFilterEntities(EmpDept.EMPLOYEE_DEPARTMENT_FK, null);

    comboBoxModel.forceRefresh();
    assertTrue(comboBoxModel.getSize() == 1);
    assertEquals(2, refreshed.size());
    comboBoxModel.removeRefreshListener(refreshListener);
  }

  @Test
  public void staticData() throws DatabaseException {
    comboBoxModel.refresh();
    List<Entity> items = new ArrayList<>(comboBoxModel.getVisibleItems());
    comboBoxModel.refresh();
    List<Entity> refreshedItems = comboBoxModel.getVisibleItems();

    Iterator<Entity> itemIterator = items.iterator();
    Iterator<Entity> refreshedIterator = refreshedItems.iterator();
    while (itemIterator.hasNext()) {
      final Entity item = itemIterator.next();
      final Entity refreshedItem = refreshedIterator.next();
      assertEquals(item, refreshedItem);
      assertFalse(item == refreshedItem);
    }

    comboBoxModel.clear();
    assertFalse(comboBoxModel.isStaticData());
    comboBoxModel.setStaticData(true);
    assertTrue(comboBoxModel.isStaticData());

    comboBoxModel.refresh();
    items = new ArrayList<>(comboBoxModel.getVisibleItems());
    comboBoxModel.refresh();
    refreshedItems = comboBoxModel.getVisibleItems();

    itemIterator = items.iterator();
    refreshedIterator = refreshedItems.iterator();
    while (itemIterator.hasNext()) {
      final Entity item = itemIterator.next();
      final Entity refreshedItem = refreshedIterator.next();
      assertEquals(item, refreshedItem);
      assertTrue(item == refreshedItem);
    }
  }

  @Test
  public void getEntity() {
    comboBoxModel.refresh();
    final Entity.Key allenPK = Entities.key(EmpDept.T_EMPLOYEE);
    allenPK.setValue(EmpDept.EMPLOYEE_ID, 1);
    assertNotNull(comboBoxModel.getEntity(allenPK));
    final Entity.Key nobodyPK = Entities.key(EmpDept.T_EMPLOYEE);
    nobodyPK.setValue(EmpDept.EMPLOYEE_ID, -1);
    assertNull(comboBoxModel.getEntity(nobodyPK));
  }
}