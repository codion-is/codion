/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.model;

import is.codion.common.db.database.Databases;
import is.codion.common.db.exception.DatabaseException;
import is.codion.common.event.EventListener;
import is.codion.common.user.User;
import is.codion.common.user.Users;
import is.codion.common.value.Value;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.condition.Conditions;
import is.codion.framework.db.local.LocalEntityConnectionProvider;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.Key;
import is.codion.framework.model.EntityComboBoxModel;
import is.codion.framework.model.EntityEditEvents;
import is.codion.framework.model.tests.TestDomain;

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

  private static final Entities ENTITIES = new TestDomain().getEntities();

  private static final User UNIT_TEST_USER =
          Users.parseUser(System.getProperty("codion.test.user", "scott:tiger"));
  private static final EntityConnectionProvider CONNECTION_PROVIDER = new LocalEntityConnectionProvider(
          Databases.getInstance()).setDomainClassName(TestDomain.class.getName()).setUser(UNIT_TEST_USER);

  private final SwingEntityComboBoxModel comboBoxModel;

  public SwingEntityComboBoxModelTest() {
    comboBoxModel = new SwingEntityComboBoxModel(TestDomain.T_EMP, CONNECTION_PROVIDER);
  }

  @Test
  public void editEvents() {
    comboBoxModel.refresh();

    final Entity temp = ENTITIES.entity(TestDomain.T_EMP);
    temp.put(TestDomain.EMP_ID, -42);
    temp.put(TestDomain.EMP_NAME, "Noname");

    EntityEditEvents.notifyInserted(singletonList(temp));
    assertTrue(comboBoxModel.isVisible(temp));

    temp.put(TestDomain.EMP_NAME, "Newname");
    temp.save(TestDomain.EMP_NAME);

    final Map<Key, Entity> updated = new HashMap<>();
    updated.put(temp.getPrimaryKey(), temp);

    EntityEditEvents.notifyUpdated(updated);
    assertEquals("Newname", comboBoxModel.getEntity(temp.getPrimaryKey()).get(TestDomain.EMP_NAME));

    EntityEditEvents.notifyDeleted(singletonList(temp));
    assertFalse(comboBoxModel.isVisible(temp));

    comboBoxModel.setListenToEditEvents(false);

    EntityEditEvents.notifyInserted(singletonList(temp));
    assertFalse(comboBoxModel.isVisible(temp));
  }

  @Test
  public void constructorNullEntityType() {
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
    empBox.setNullString("-");
    empBox.refresh();
    assertEquals(17, empBox.getSize());
    final SwingEntityComboBoxModel deptBox = empBox.createForeignKeyFilterComboBoxModel(TestDomain.EMP_DEPARTMENT_FK);
    assertEquals(1, empBox.getSize());
    final Key accountingKey = connectionProvider.getEntities().primaryKey(TestDomain.T_DEPARTMENT, 10);
    deptBox.setSelectedEntityByKey(accountingKey);
    assertEquals(8, empBox.getSize());
    deptBox.setSelectedItem(null);
    assertEquals(1, empBox.getSize());
    final Key salesKey = connectionProvider.getEntities().primaryKey(TestDomain.T_DEPARTMENT, 30);
    deptBox.setSelectedEntityByKey(salesKey);
    assertEquals(5, empBox.getSize());
    empBox.setSelectedItem(empBox.getVisibleItems().get(1));
    empBox.setSelectedItem(null);
  }

  @Test
  public void foreignKeyConditionComboBoxModel() throws Exception {
    final EntityConnectionProvider connectionProvider = comboBoxModel.getConnectionProvider();
    final SwingEntityComboBoxModel empBox = new SwingEntityComboBoxModel(TestDomain.T_EMP, connectionProvider);
    empBox.setNullString("-");
    empBox.refresh();
    assertEquals(17, empBox.getSize());
    final SwingEntityComboBoxModel deptBox = empBox.createForeignKeyConditionComboBoxModel(TestDomain.EMP_DEPARTMENT_FK);
    assertEquals(1, empBox.getSize());
    final Key accountingKey = connectionProvider.getEntities().primaryKey(TestDomain.T_DEPARTMENT, 10);
    deptBox.setSelectedEntityByKey(accountingKey);
    assertEquals(8, empBox.getSize());
    deptBox.setSelectedItem(null);
    assertEquals(1, empBox.getSize());
    final Key salesKey = connectionProvider.getEntities().primaryKey(TestDomain.T_DEPARTMENT, 30);
    deptBox.setSelectedEntityByKey(salesKey);
    assertEquals(5, empBox.getSize());
    empBox.setSelectedItem(empBox.getVisibleItems().get(1));
    empBox.setSelectedItem(null);
  }

  @Test
  public void setForeignKeyFilterEntities() throws Exception {
    comboBoxModel.refresh();
    final Entity blake = comboBoxModel.getConnectionProvider().getConnection().selectSingle(TestDomain.EMP_NAME, "BLAKE");
    comboBoxModel.setForeignKeyFilterEntities(TestDomain.EMP_MGR_FK, singletonList(blake));
    assertEquals(5, comboBoxModel.getSize());
    for (int i = 0; i < comboBoxModel.getSize(); i++) {
      final Entity item = comboBoxModel.getElementAt(i);
      assertEquals(item.getForeignKey(TestDomain.EMP_MGR_FK), blake);
    }

    final Entity sales = comboBoxModel.getConnectionProvider().getConnection().selectSingle(TestDomain.DEPARTMENT_NAME, "SALES");
    comboBoxModel.setForeignKeyFilterEntities(TestDomain.EMP_DEPARTMENT_FK, singletonList(sales));
    assertEquals(2, comboBoxModel.getSize());
    for (int i = 0; i < comboBoxModel.getSize(); i++) {
      final Entity item = comboBoxModel.getElementAt(i);
      assertEquals(item.getForeignKey(TestDomain.EMP_DEPARTMENT_FK), sales);
      assertEquals(item.getForeignKey(TestDomain.EMP_MGR_FK), blake);
    }

    final Entity accounting = comboBoxModel.getConnectionProvider().getConnection().selectSingle(TestDomain.DEPARTMENT_NAME, "ACCOUNTING");
    final EntityComboBoxModel deptComboBoxModel = comboBoxModel.createForeignKeyFilterComboBoxModel(TestDomain.EMP_DEPARTMENT_FK);
    deptComboBoxModel.setSelectedItem(accounting);
    assertEquals(3, comboBoxModel.getSize());
    for (int i = 0; i < comboBoxModel.getSize(); i++) {
      final Entity item = comboBoxModel.getElementAt(i);
      assertEquals(item.getForeignKey(TestDomain.EMP_DEPARTMENT_FK), accounting);
      assertEquals(item.getForeignKey(TestDomain.EMP_MGR_FK), blake);
    }
    for (final Entity employee : comboBoxModel.getItems()) {
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
    final Entity clark = comboBoxModel.getConnectionProvider().getConnection().selectSingle(TestDomain.EMP_NAME, "CLARK");
    comboBoxModel.setSelectedEntityByKey(clark.getPrimaryKey());
    assertEquals(clark, comboBoxModel.getSelectedValue());
    comboBoxModel.setSelectedItem(null);
    assertNull(comboBoxModel.getSelectedValue());
    comboBoxModel.setIncludeCondition(entity -> false);
    comboBoxModel.setSelectedEntityByKey(clark.getPrimaryKey());
    assertEquals(clark, comboBoxModel.getSelectedValue());
    final Key nobodyPK = ENTITIES.primaryKey(TestDomain.T_EMP, -1);
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
    assertThrows(IllegalArgumentException.class, () -> comboBoxModel.integerValueSelector(TestDomain.DEPARTMENT_ID));
    final Value<Integer> empIdValue = comboBoxModel.integerValueSelector(TestDomain.EMP_ID);
    assertNull(empIdValue.get());
    final Key jonesKey = comboBoxModel.getConnectionProvider().getEntities().primaryKey(TestDomain.T_EMP, 5);
    comboBoxModel.setSelectedEntityByKey(jonesKey);
    assertEquals(5, empIdValue.get());
    comboBoxModel.setSelectedItem(null);
    assertNull(empIdValue.get());
    empIdValue.set(10);
    assertEquals("ADAMS", comboBoxModel.getSelectedValue().get(TestDomain.EMP_NAME));
    empIdValue.set(null);
    assertNull(comboBoxModel.getSelectedValue());
  }

  @Test
  public void test() throws DatabaseException {
    final AtomicInteger refreshed = new AtomicInteger();
    final EventListener refreshListener = refreshed::incrementAndGet;
    comboBoxModel.addRefreshListener(refreshListener);
    assertEquals(TestDomain.T_EMP, comboBoxModel.getEntityType());
    comboBoxModel.setStaticData(false);
    comboBoxModel.toString();
    assertEquals(0, comboBoxModel.getSize());
    assertNull(comboBoxModel.getSelectedItem());
    comboBoxModel.refresh();
    assertTrue(comboBoxModel.getSize() > 0);
    assertFalse(comboBoxModel.isCleared());

    final Entity clark = comboBoxModel.getConnectionProvider().getConnection().selectSingle(TestDomain.EMP_NAME, "CLARK");
    comboBoxModel.setSelectedItem(clark);
    assertEquals(clark, comboBoxModel.getSelectedValue());

    comboBoxModel.clear();
    assertEquals(0, comboBoxModel.getSize());

    comboBoxModel.setSelectConditionProvider(() -> Conditions.customCondition(TestDomain.EMP_CONDITION_3_TYPE));
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
    assertEquals(comboBoxModel.getSelectedValue().get(TestDomain.EMP_NAME), "SCOTT");
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
    final Key allenPK = ENTITIES.primaryKey(TestDomain.T_EMP, 1);
    assertNotNull(comboBoxModel.getEntity(allenPK));
    final Key nobodyPK = ENTITIES.primaryKey(TestDomain.T_EMP, -1);
    assertNull(comboBoxModel.getEntity(nobodyPK));
  }
}