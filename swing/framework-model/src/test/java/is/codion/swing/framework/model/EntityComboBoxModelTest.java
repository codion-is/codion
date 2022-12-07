/*
 * Copyright (c) 2009 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.model;

import is.codion.common.db.exception.DatabaseException;
import is.codion.common.event.EventListener;
import is.codion.common.user.User;
import is.codion.common.value.Value;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.condition.Condition;
import is.codion.framework.db.local.LocalEntityConnectionProvider;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.Key;
import is.codion.framework.model.EntityEditEvents;
import is.codion.framework.model.test.TestDomain;
import is.codion.framework.model.test.TestDomain.Department;
import is.codion.framework.model.test.TestDomain.Employee;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.*;

public final class EntityComboBoxModelTest {

  private static final Entities ENTITIES = new TestDomain().entities();

  private static final User UNIT_TEST_USER =
          User.parse(System.getProperty("codion.test.user", "scott:tiger"));

  private static final EntityConnectionProvider CONNECTION_PROVIDER = LocalEntityConnectionProvider.builder()
            .domainClassName(TestDomain.class.getName())
            .user(UNIT_TEST_USER)
            .build();

  private final EntityComboBoxModel comboBoxModel;

  public EntityComboBoxModelTest() {
    comboBoxModel = new EntityComboBoxModel(Employee.TYPE, CONNECTION_PROVIDER);
  }

  @Test
  void editEvents() {
    comboBoxModel.refresh();

    Entity temp = ENTITIES.builder(Employee.TYPE)
            .with(Employee.ID, -42)
            .with(Employee.NAME, "Noname")
            .build();

    EntityEditEvents.notifyInserted(singletonList(temp));
    assertTrue(comboBoxModel.isVisible(temp));

    temp.put(Employee.NAME, "Newname");
    temp.save(Employee.NAME);

    Map<Key, Entity> updated = new HashMap<>();
    updated.put(temp.primaryKey(), temp);

    EntityEditEvents.notifyUpdated(updated);
    assertEquals("Newname", comboBoxModel.entity(temp.primaryKey()).orElse(null).get(Employee.NAME));

    EntityEditEvents.notifyDeleted(singletonList(temp));
    assertFalse(comboBoxModel.isVisible(temp));

    comboBoxModel.setListenToEditEvents(false);

    EntityEditEvents.notifyInserted(singletonList(temp));
    assertFalse(comboBoxModel.isVisible(temp));
  }

  @Test
  void constructorNullEntityType() {
    assertThrows(NullPointerException.class, () -> new EntityComboBoxModel(null, CONNECTION_PROVIDER));
  }

  @Test
  void constructorNullConnectionProvider() {
    assertThrows(NullPointerException.class, () -> new EntityComboBoxModel(Employee.TYPE, null));
  }

  @Test
  void foreignKeyFilterComboBoxModel() throws Exception {
    EntityConnectionProvider connectionProvider = comboBoxModel.connectionProvider();
    EntityComboBoxModel empBox = new EntityComboBoxModel(Employee.TYPE, connectionProvider);
    empBox.setNullCaption("-");
    empBox.refresh();
    assertEquals(17, empBox.getSize());
    EntityComboBoxModel deptBox = empBox.createForeignKeyFilterComboBoxModel(Employee.DEPARTMENT_FK);
    assertEquals(1, empBox.getSize());
    Key accountingKey = connectionProvider.entities().primaryKey(Department.TYPE, 10);
    deptBox.selectByKey(accountingKey);
    assertEquals(8, empBox.getSize());
    deptBox.setSelectedItem(null);
    assertEquals(1, empBox.getSize());
    Key salesKey = connectionProvider.entities().primaryKey(Department.TYPE, 30);
    deptBox.selectByKey(salesKey);
    assertEquals(5, empBox.getSize());
    empBox.setSelectedItem(empBox.visibleItems().get(1));
    empBox.setSelectedItem(null);
  }

  @Test
  void foreignKeyConditionComboBoxModel() throws Exception {
    EntityConnectionProvider connectionProvider = comboBoxModel.connectionProvider();
    EntityComboBoxModel empBox = new EntityComboBoxModel(Employee.TYPE, connectionProvider);
    empBox.setNullCaption("-");
    empBox.refresh();
    assertEquals(17, empBox.getSize());
    EntityComboBoxModel deptBox = empBox.createForeignKeyConditionComboBoxModel(Employee.DEPARTMENT_FK);
    assertEquals(1, empBox.getSize());
    Key accountingKey = connectionProvider.entities().primaryKey(Department.TYPE, 10);
    deptBox.selectByKey(accountingKey);
    assertEquals(8, empBox.getSize());
    deptBox.setSelectedItem(null);
    assertEquals(1, empBox.getSize());
    Key salesKey = connectionProvider.entities().primaryKey(Department.TYPE, 30);
    deptBox.selectByKey(salesKey);
    assertEquals(5, empBox.getSize());
    empBox.setSelectedItem(empBox.visibleItems().get(1));
    empBox.setSelectedItem(null);
  }

  @Test
  void setForeignKeyFilterEntities() throws Exception {
    comboBoxModel.refresh();
    Entity blake = comboBoxModel.connectionProvider().connection().selectSingle(Employee.NAME, "BLAKE");
    comboBoxModel.setForeignKeyFilterKeys(Employee.MGR_FK, singletonList(blake.primaryKey()));
    assertEquals(5, comboBoxModel.getSize());
    for (int i = 0; i < comboBoxModel.getSize(); i++) {
      Entity item = comboBoxModel.getElementAt(i);
      assertEquals(item.referencedEntity(Employee.MGR_FK), blake);
    }

    Entity sales = comboBoxModel.connectionProvider().connection().selectSingle(Department.NAME, "SALES");
    comboBoxModel.setForeignKeyFilterKeys(Employee.DEPARTMENT_FK, singletonList(sales.primaryKey()));
    assertEquals(2, comboBoxModel.getSize());
    for (int i = 0; i < comboBoxModel.getSize(); i++) {
      Entity item = comboBoxModel.getElementAt(i);
      assertEquals(item.referencedEntity(Employee.DEPARTMENT_FK), sales);
      assertEquals(item.referencedEntity(Employee.MGR_FK), blake);
    }

    Entity accounting = comboBoxModel.connectionProvider().connection().selectSingle(Department.NAME, "ACCOUNTING");
    EntityComboBoxModel deptComboBoxModel = comboBoxModel.createForeignKeyFilterComboBoxModel(Employee.DEPARTMENT_FK);
    deptComboBoxModel.setSelectedItem(accounting);
    assertEquals(3, comboBoxModel.getSize());
    for (int i = 0; i < comboBoxModel.getSize(); i++) {
      Entity item = comboBoxModel.getElementAt(i);
      assertEquals(item.referencedEntity(Employee.DEPARTMENT_FK), accounting);
      assertEquals(item.referencedEntity(Employee.MGR_FK), blake);
    }
    for (Entity employee : comboBoxModel.items()) {
      if (employee.referencedEntity(Employee.DEPARTMENT_FK).equals(accounting)) {
        comboBoxModel.setSelectedItem(employee);
        break;
      }
    }
    assertEquals(accounting, deptComboBoxModel.selectedValue());

    //non strict filtering
    comboBoxModel.setStrictForeignKeyFiltering(false);
    comboBoxModel.setForeignKeyFilterKeys(Employee.DEPARTMENT_FK, null);
    assertEquals(6, comboBoxModel.getSize());
    boolean kingFound = false;
    for (int i = 0; i < comboBoxModel.getSize(); i++) {
      Entity item = comboBoxModel.getElementAt(i);
      if (Objects.equals(item.get(Employee.NAME), "KING")) {
        kingFound = true;
      }
      else {
        assertEquals(item.referencedEntity(Employee.MGR_FK), blake);
      }
    }
    assertTrue(kingFound);
  }

  @Test
  void setSelectedEntityByKey() throws DatabaseException {
    comboBoxModel.refresh();
    Entity clark = comboBoxModel.connectionProvider().connection().selectSingle(Employee.NAME, "CLARK");
    comboBoxModel.selectByKey(clark.primaryKey());
    assertEquals(clark, comboBoxModel.selectedValue());
    comboBoxModel.setSelectedItem(null);
    assertNull(comboBoxModel.selectedValue());
    comboBoxModel.setIncludeCondition(entity -> false);
    comboBoxModel.selectByKey(clark.primaryKey());
    assertEquals(clark, comboBoxModel.selectedValue());
    Key nobodyPK = ENTITIES.primaryKey(Employee.TYPE, -1);
    comboBoxModel.selectByKey(nobodyPK);
    assertEquals(clark, comboBoxModel.selectedValue());
  }

  @Test
  void setSelectedEntityByPrimaryKeyNullValue() {
    assertThrows(NullPointerException.class, () -> comboBoxModel.selectByKey(null));
  }

  @Test
  void selectorValue() {
    comboBoxModel.refresh();
    assertThrows(IllegalArgumentException.class, () -> comboBoxModel.createSelectorValue(Department.ID));
    Value<Integer> empIdValue = comboBoxModel.createSelectorValue(Employee.ID);
    assertNull(empIdValue.get());
    Key jonesKey = comboBoxModel.connectionProvider().entities().primaryKey(Employee.TYPE, 5);
    comboBoxModel.selectByKey(jonesKey);
    assertEquals(5, empIdValue.get());
    comboBoxModel.setSelectedItem(null);
    assertNull(empIdValue.get());
    empIdValue.set(10);
    assertEquals("ADAMS", comboBoxModel.selectedValue().get(Employee.NAME));
    empIdValue.set(null);
    assertNull(comboBoxModel.selectedValue());
  }

  @Test
  void selectAttributes() {
    comboBoxModel.setSelectAttributes(Arrays.asList(Employee.NAME, Employee.DEPARTMENT_FK));
    comboBoxModel.refresh();
    for (Entity emp : comboBoxModel.items()) {
      assertTrue(emp.contains(Employee.ID));
      assertTrue(emp.contains(Employee.NAME));
      assertTrue(emp.contains(Employee.DEPARTMENT));
      assertTrue(emp.contains(Employee.DEPARTMENT_FK));
      assertFalse(emp.contains(Employee.JOB));
      assertFalse(emp.contains(Employee.MGR));
      assertFalse(emp.contains(Employee.COMMISSION));
      assertFalse(emp.contains(Employee.HIREDATE));
      assertFalse(emp.contains(Employee.SALARY));
    }
    comboBoxModel.setSelectAttributes(emptyList());
    comboBoxModel.refresh();
    for (Entity emp : comboBoxModel.items()) {
      assertTrue(emp.contains(Employee.ID));
      assertTrue(emp.contains(Employee.NAME));
      assertTrue(emp.contains(Employee.DEPARTMENT));
      assertTrue(emp.contains(Employee.DEPARTMENT_FK));
      assertTrue(emp.contains(Employee.JOB));
      assertTrue(emp.contains(Employee.MGR));
      assertTrue(emp.contains(Employee.COMMISSION));
      assertTrue(emp.contains(Employee.HIREDATE));
      assertTrue(emp.contains(Employee.SALARY));
    }
  }

  @Test
  void test() throws DatabaseException {
    AtomicInteger refreshed = new AtomicInteger();
    EventListener refreshListener = refreshed::incrementAndGet;
    comboBoxModel.addRefreshListener(refreshListener);
    assertEquals(Employee.TYPE, comboBoxModel.entityType());
    comboBoxModel.setStaticData(false);
    comboBoxModel.toString();
    assertEquals(0, comboBoxModel.getSize());
    assertNull(comboBoxModel.getSelectedItem());
    comboBoxModel.refresh();
    assertTrue(comboBoxModel.getSize() > 0);
    assertFalse(comboBoxModel.isCleared());

    Entity clark = comboBoxModel.connectionProvider().connection().selectSingle(Employee.NAME, "CLARK");
    comboBoxModel.setSelectedItem(clark);
    assertEquals(clark, comboBoxModel.selectedValue());

    comboBoxModel.clear();
    assertEquals(0, comboBoxModel.getSize());

    comboBoxModel.setSelectConditionSupplier(() -> Condition.customCondition(Employee.CONDITION_3_TYPE));
    comboBoxModel.setForeignKeyFilterKeys(Employee.DEPARTMENT_FK, null);

    comboBoxModel.forceRefresh();
    assertEquals(1, comboBoxModel.getSize());
    assertEquals(2, refreshed.get());
    comboBoxModel.setSelectConditionSupplier(null);
    comboBoxModel.forceRefresh();
    assertEquals(16, comboBoxModel.getSize());
    assertEquals(3, refreshed.get());
    comboBoxModel.removeRefreshListener(refreshListener);
  }

  @Test
  void setSelectedItemNonExistingString() {
    comboBoxModel.setSelectedItem("test");
    assertNull(comboBoxModel.selectedValue());
  }

  @Test
  void selectString() {
    comboBoxModel.refresh();
    comboBoxModel.setSelectedItem(comboBoxModel.getElementAt(0));
    comboBoxModel.setSelectedItem("SCOTT");
    assertEquals(comboBoxModel.selectedValue().get(Employee.NAME), "SCOTT");
  }

  @Test
  void staticData() {
    comboBoxModel.refresh();
    List<Entity> items = new ArrayList<>(comboBoxModel.visibleItems());
    comboBoxModel.refresh();
    List<Entity> refreshedItems = comboBoxModel.visibleItems();

    Iterator<Entity> itemIterator = items.iterator();
    Iterator<Entity> refreshedIterator = refreshedItems.iterator();
    while (itemIterator.hasNext()) {
      Entity item = itemIterator.next();
      Entity refreshedItem = refreshedIterator.next();
      assertEquals(item, refreshedItem);
      assertNotSame(item, refreshedItem);
    }

    comboBoxModel.clear();
    assertFalse(comboBoxModel.isStaticData());
    comboBoxModel.setStaticData(true);
    assertTrue(comboBoxModel.isStaticData());

    comboBoxModel.refresh();
    items = new ArrayList<>(comboBoxModel.visibleItems());
    comboBoxModel.refresh();
    refreshedItems = comboBoxModel.visibleItems();

    itemIterator = items.iterator();
    refreshedIterator = refreshedItems.iterator();
    while (itemIterator.hasNext()) {
      Entity item = itemIterator.next();
      Entity refreshedItem = refreshedIterator.next();
      assertEquals(item, refreshedItem);
      assertSame(item, refreshedItem);
    }
  }

  @Test
  void entity() {
    comboBoxModel.refresh();
    Key allenPK = ENTITIES.primaryKey(Employee.TYPE, 1);
    assertNotNull(comboBoxModel.entity(allenPK));
    Key nobodyPK = ENTITIES.primaryKey(Employee.TYPE, -1);
    assertFalse(comboBoxModel.entity(nobodyPK).isPresent());
  }

  @Test
  void nullCaption() {
    comboBoxModel.refresh();
    assertFalse(comboBoxModel.containsItem(null));
    comboBoxModel.setNullCaption("-");
    assertTrue(comboBoxModel.containsItem(null));
    assertEquals("-", comboBoxModel.getSelectedItem().toString());
    assertNull(comboBoxModel.selectedValue());
    comboBoxModel.setIncludeNull(false);
    assertFalse(comboBoxModel.containsItem(null));
  }
}