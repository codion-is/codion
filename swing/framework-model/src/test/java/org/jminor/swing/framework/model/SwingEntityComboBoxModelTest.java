/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.framework.model;

import org.jminor.common.User;
import org.jminor.common.db.Databases;
import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.event.EventListener;
import org.jminor.common.value.Value;
import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.db.condition.Conditions;
import org.jminor.framework.db.local.LocalEntityConnectionProvider;
import org.jminor.framework.domain.Domain;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.model.EntityComboBoxModel;
import org.jminor.framework.model.EntityEditEvents;
import org.jminor.framework.model.TestDomain;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.*;

public final class SwingEntityComboBoxModelTest {

  private static final Domain DOMAIN = new TestDomain();

  private static final User UNIT_TEST_USER =
          User.parseUser(System.getProperty("jminor.test.user", "scott:tiger"));
  private static final EntityConnectionProvider CONNECTION_PROVIDER = new LocalEntityConnectionProvider(
          Databases.getInstance()).setDomainClassName(TestDomain.class.getName()).setUser(UNIT_TEST_USER);

  private final SwingEntityComboBoxModel comboBoxModel;

  public SwingEntityComboBoxModelTest() {
    comboBoxModel = new SwingEntityComboBoxModel(TestDomain.T_EMP, CONNECTION_PROVIDER);
  }

  @Test
  public void editEvents() {
    comboBoxModel.refresh();

    final Entity temp = DOMAIN.entity(TestDomain.T_EMP);
    temp.put(TestDomain.EMP_ID, -42);
    temp.put(TestDomain.EMP_NAME, "Noname");

    EntityEditEvents.notifyInserted(singletonList(temp));
    assertTrue(comboBoxModel.contains(temp, false));

    temp.put(TestDomain.EMP_NAME, "Newname");
    temp.save(TestDomain.EMP_NAME);

    final Map<Entity.Key, Entity> updated = new HashMap<>();
    updated.put(temp.getKey(), temp);

    EntityEditEvents.notifyUpdated(updated);
    assertEquals("Newname", comboBoxModel.getEntity(temp.getKey()).getString(TestDomain.EMP_NAME));

    EntityEditEvents.notifyDeleted(singletonList(temp));
    assertFalse(comboBoxModel.contains(temp, false));

    comboBoxModel.setListenToEditEvents(false);

    EntityEditEvents.notifyInserted(singletonList(temp));
    assertFalse(comboBoxModel.contains(temp, false));
  }

  @Test
  public void constructorNullEntityId() {
    assertThrows(NullPointerException.class, () -> new SwingEntityComboBoxModel(null, CONNECTION_PROVIDER));
  }

  @Test
  public void constructorNullConnectionProvider() {
    assertThrows(NullPointerException.class, () -> new SwingEntityComboBoxModel(TestDomain.T_EMP, null));
  }

  @Test
  public void foreignKeyFilterComboBoxModel() throws Exception {
    final EntityConnectionProvider connectionProvider = comboBoxModel.getConnectionProvider();
    final SwingEntityComboBoxModel empBox = new SwingEntityComboBoxModel(TestDomain.T_EMP, connectionProvider);
    empBox.setNullValue(connectionProvider.getDomain().createToStringEntity(TestDomain.T_EMP, "-"));
    empBox.refresh();
    assertEquals(17, empBox.getSize());
    final SwingEntityComboBoxModel deptBox = empBox.createForeignKeyFilterComboBoxModel(TestDomain.EMP_DEPARTMENT_FK);
    assertEquals(1, empBox.getSize());
    final Entity.Key accountingKey = connectionProvider.getDomain().key(TestDomain.T_DEPARTMENT, 10);
    deptBox.setSelectedEntityByKey(accountingKey);
    assertEquals(8, empBox.getSize());
    deptBox.setSelectedItem(null);
    assertEquals(1, empBox.getSize());
    final Entity.Key salesKey = connectionProvider.getDomain().key(TestDomain.T_DEPARTMENT, 30);
    deptBox.setSelectedEntityByKey(salesKey);
    assertEquals(5, empBox.getSize());
    empBox.setSelectedItem(empBox.getVisibleItems().get(1));
    empBox.setSelectedItem(null);
  }

  @Test
  public void setForeignKeyFilterEntities() throws Exception {
    comboBoxModel.refresh();
    final Entity blake = comboBoxModel.getConnectionProvider().getConnection().selectSingle(TestDomain.T_EMP, TestDomain.EMP_NAME, "BLAKE");
    comboBoxModel.setForeignKeyFilterEntities(TestDomain.EMP_MGR_FK, singletonList(blake));
    assertEquals(5, comboBoxModel.getSize());
    for (int i = 0; i < comboBoxModel.getSize(); i++) {
      final Entity item = comboBoxModel.getElementAt(i);
      assertEquals(item.getForeignKey(TestDomain.EMP_MGR_FK), blake);
    }

    final Entity sales = comboBoxModel.getConnectionProvider().getConnection().selectSingle(TestDomain.T_DEPARTMENT, TestDomain.DEPARTMENT_NAME, "SALES");
    comboBoxModel.setForeignKeyFilterEntities(TestDomain.EMP_DEPARTMENT_FK, singletonList(sales));
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
    comboBoxModel.setIncludeCondition(entity -> false);
    comboBoxModel.setSelectedEntityByKey(clark.getKey());
    assertEquals(clark, comboBoxModel.getSelectedValue());
    final Entity.Key nobodyPK = DOMAIN.key(TestDomain.T_EMP, -1);
    comboBoxModel.setSelectedEntityByKey(nobodyPK);
    assertEquals(clark, comboBoxModel.getSelectedValue());
  }

  @Test
  public void setSelectedEntityByPrimaryKeyNullValue() {
    assertThrows(NullPointerException.class, () -> comboBoxModel.setSelectedEntityByKey(null));
  }

  @Test
  public void integerValueSelector() {
    comboBoxModel.refresh();
    final Value<Integer> empIdValue = comboBoxModel.integerValueSelector(TestDomain.EMP_ID);
    assertNull(empIdValue.get());
    final Entity.Key jonesKey = comboBoxModel.getConnectionProvider().getDomain().key(TestDomain.T_EMP, 5);
    comboBoxModel.setSelectedEntityByKey(jonesKey);
    assertEquals(5, empIdValue.get());
    comboBoxModel.setSelectedItem(null);
    assertNull(empIdValue.get());
    empIdValue.set(10);
    assertEquals("ADAMS", comboBoxModel.getSelectedValue().getString(TestDomain.EMP_NAME));
    empIdValue.set(null);
    assertNull(comboBoxModel.getSelectedValue());
  }

  @Test
  public void test() throws DatabaseException {
    final AtomicInteger refreshed = new AtomicInteger();
    final EventListener refreshListener = refreshed::incrementAndGet;
    comboBoxModel.addRefreshListener(refreshListener);
    assertEquals(TestDomain.T_EMP, comboBoxModel.getEntityId());
    comboBoxModel.setStaticData(false);
    comboBoxModel.toString();
    assertEquals(0, comboBoxModel.getSize());
    assertNull(comboBoxModel.getSelectedItem());
    comboBoxModel.refresh();
    assertTrue(comboBoxModel.getSize() > 0);
    assertFalse(comboBoxModel.isCleared());

    final Entity clark = comboBoxModel.getConnectionProvider().getConnection().selectSingle(TestDomain.T_EMP, TestDomain.EMP_NAME, "CLARK");
    comboBoxModel.setSelectedItem(clark);
    assertEquals(clark, comboBoxModel.getSelectedValue());

    comboBoxModel.clear();
    assertEquals(0, comboBoxModel.getSize());

    comboBoxModel.setSelectConditionProvider(() -> Conditions.customCondition(TestDomain.EMP_CONDITION_3_ID));
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

  @Test
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
      assertNotSame(item, refreshedItem);
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
      assertSame(item, refreshedItem);
    }
  }

  @Test
  public void getEntity() {
    comboBoxModel.refresh();
    final Entity.Key allenPK = DOMAIN.key(TestDomain.T_EMP, 1);
    assertNotNull(comboBoxModel.getEntity(allenPK));
    final Entity.Key nobodyPK = DOMAIN.key(TestDomain.T_EMP, -1);
    assertNull(comboBoxModel.getEntity(nobodyPK));
  }
}