/*
 * Copyright (c) 2004 - 2017, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.framework.model;

import org.jminor.common.EventListener;
import org.jminor.common.User;
import org.jminor.common.db.Databases;
import org.jminor.common.db.condition.Conditions;
import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.model.FilterCondition;
import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.db.local.LocalEntityConnectionProvider;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.Property;
import org.jminor.framework.model.EntityComboBoxModel;
import org.jminor.framework.model.testing.TestDomain;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public final class SwingEntityComboBoxModelTest {

  private static final Entities ENTITIES = new TestDomain();

  private static final EntityConnectionProvider CONNECTION_PROVIDER = new LocalEntityConnectionProvider(ENTITIES, new User(
          System.getProperty("jminor.unittest.username", "scott"),
          System.getProperty("jminor.unittest.password", "tiger")), Databases.getInstance());

  private final SwingEntityComboBoxModel comboBoxModel;

  public SwingEntityComboBoxModelTest() {
    comboBoxModel = new SwingEntityComboBoxModel(TestDomain.T_EMP, CONNECTION_PROVIDER);
  }

  @Test(expected = NullPointerException.class)
  public void constructorNullEntityID() {
    new SwingEntityComboBoxModel(null, CONNECTION_PROVIDER);
  }

  @Test(expected = NullPointerException.class)
  public void constructorNullConnectionProvider() {
    new SwingEntityComboBoxModel(TestDomain.T_EMP, null);
  }

  @Test
  public void setForeignKeyFilterEntities() throws Exception {
    comboBoxModel.refresh();
    final Entity blake = comboBoxModel.getConnectionProvider().getConnection().selectSingle(TestDomain.T_EMP, TestDomain.EMP_NAME, "BLAKE");
    comboBoxModel.setForeignKeyFilterEntities(TestDomain.EMP_MGR_FK, Collections.singletonList(blake));
    assertEquals(5, comboBoxModel.getSize());
    for (int i = 0; i < comboBoxModel.getSize(); i++) {
      final Entity item = comboBoxModel.getElementAt(i);
      assertEquals(item.getForeignKey(TestDomain.EMP_MGR_FK), blake);
    }

    final Entity sales = comboBoxModel.getConnectionProvider().getConnection().selectSingle(TestDomain.T_DEPARTMENT, TestDomain.DEPARTMENT_NAME, "SALES");
    comboBoxModel.setForeignKeyFilterEntities(TestDomain.EMP_DEPARTMENT_FK, Collections.singletonList(sales));
    assertEquals(2, comboBoxModel.getSize());
    for (int i = 0; i < comboBoxModel.getSize(); i++) {
      final Entity item = comboBoxModel.getElementAt(i);
      assertEquals(item.getForeignKey(TestDomain.EMP_DEPARTMENT_FK), sales);
      assertEquals(item.getForeignKey(TestDomain.EMP_MGR_FK), blake);
    }

    final Entity accounting = comboBoxModel.getConnectionProvider().getConnection().selectSingle(TestDomain.T_DEPARTMENT, TestDomain.DEPARTMENT_NAME, "ACCOUNTING");
    final EntityComboBoxModel deptComboBoxModel = comboBoxModel.createForeignKeyFilterComboBoxModel(TestDomain.EMP_DEPARTMENT_FK);
    deptComboBoxModel.setSelectedItem(accounting);
    assertEquals(3, comboBoxModel.getSize());
    for (int i = 0; i < comboBoxModel.getSize(); i++) {
      final Entity item = comboBoxModel.getElementAt(i);
      assertEquals(item.getForeignKey(TestDomain.EMP_DEPARTMENT_FK), accounting);
      assertEquals(item.getForeignKey(TestDomain.EMP_MGR_FK), blake);
    }
    for (final Entity employee : comboBoxModel.getAllItems()) {
      if (employee.getForeignKey(TestDomain.EMP_DEPARTMENT_FK).equals(accounting)) {
        comboBoxModel.setSelectedItem(employee);
        break;
      }
    }
    assertEquals(accounting, deptComboBoxModel.getSelectedValue());

    //non strict filtering
    comboBoxModel.setStrictForeignKeyFiltering(false);
    comboBoxModel.setForeignKeyFilterEntities(TestDomain.EMP_DEPARTMENT_FK, null);
    assertEquals(6, comboBoxModel.getSize());
    boolean kingFound = false;
    for (int i = 0; i < comboBoxModel.getSize(); i++) {
      final Entity item = comboBoxModel.getElementAt(i);
      if (Objects.equals(item.get(TestDomain.EMP_NAME), "KING")) {
        kingFound = true;
      }
      else {
        assertEquals(item.getForeignKey(TestDomain.EMP_MGR_FK), blake);
      }
    }
    assertTrue(kingFound);
  }

  @Test
  public void setSelectedEntityByKey() throws DatabaseException {
    comboBoxModel.refresh();
    final Entity clark = comboBoxModel.getConnectionProvider().getConnection().selectSingle(TestDomain.T_EMP, TestDomain.EMP_NAME, "CLARK");
    comboBoxModel.setSelectedEntityByKey(clark.getKey());
    assertEquals(clark, comboBoxModel.getSelectedValue());
    comboBoxModel.setSelectedItem(null);
    assertNull(comboBoxModel.getSelectedValue());
    comboBoxModel.setFilterCondition(new FilterCondition.RejectAllCondition<>());
    comboBoxModel.setSelectedEntityByKey(clark.getKey());
    assertEquals(clark, comboBoxModel.getSelectedValue());
    final Entity.Key nobodyPK = ENTITIES.key(TestDomain.T_EMP);
    nobodyPK.put(TestDomain.EMP_ID, -1);
    comboBoxModel.setSelectedEntityByKey(nobodyPK);
    assertEquals(clark, comboBoxModel.getSelectedValue());
  }

  @Test(expected = NullPointerException.class)
  public void setSelectedEntityByPrimaryKeyNullValue() {
    comboBoxModel.setSelectedEntityByKey(null);
  }

  @Test
  public void test() throws DatabaseException {
    final AtomicInteger refreshed = new AtomicInteger();
    final EventListener refreshListener = refreshed::incrementAndGet;
    comboBoxModel.addRefreshListener(refreshListener);
    assertEquals(TestDomain.T_EMP, comboBoxModel.getEntityID());
    comboBoxModel.setStaticData(false);
    comboBoxModel.toString();
    assertTrue(comboBoxModel.getSize() == 0);
    assertNull(comboBoxModel.getSelectedItem());
    comboBoxModel.refresh();
    assertTrue(comboBoxModel.getSize() > 0);
    assertFalse(comboBoxModel.isCleared());

    final Entity clark = comboBoxModel.getConnectionProvider().getConnection().selectSingle(TestDomain.T_EMP, TestDomain.EMP_NAME, "CLARK");
    comboBoxModel.setSelectedItem(clark);
    assertEquals(clark, comboBoxModel.getSelectedValue());

    comboBoxModel.clear();
    assertTrue(comboBoxModel.getSize() == 0);

    comboBoxModel.setSelectConditionProvider(() -> Conditions.<Property.ColumnProperty>stringCondition(" ename = 'CLARK'"));
    comboBoxModel.setForeignKeyFilterEntities(TestDomain.EMP_DEPARTMENT_FK, null);

    comboBoxModel.forceRefresh();
    assertEquals(1, comboBoxModel.getSize());
    assertEquals(2, refreshed.get());
    comboBoxModel.setSelectConditionProvider(null);
    comboBoxModel.forceRefresh();
    assertEquals(16, comboBoxModel.getSize());
    assertEquals(3, refreshed.get());
    comboBoxModel.removeRefreshListener(refreshListener);
  }

  public void setSelectedItemNonExistingString() {
    comboBoxModel.setSelectedItem("test");
    assertNull(comboBoxModel.getSelectedValue());
  }

  @Test
  public void selectString() {
    comboBoxModel.refresh();
    comboBoxModel.setSelectedItem(comboBoxModel.getElementAt(0));
    comboBoxModel.setSelectedItem("SCOTT");
    assertEquals(comboBoxModel.getSelectedItem().getString(TestDomain.EMP_NAME), "SCOTT");
  }

  @Test
  public void staticData() {
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
    final Entity.Key allenPK = ENTITIES.key(TestDomain.T_EMP);
    allenPK.put(TestDomain.EMP_ID, 1);
    assertNotNull(comboBoxModel.getEntity(allenPK));
    final Entity.Key nobodyPK = ENTITIES.key(TestDomain.T_EMP);
    nobodyPK.put(TestDomain.EMP_ID, -1);
    assertNull(comboBoxModel.getEntity(nobodyPK));
  }
}