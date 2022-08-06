/*
 * Copyright (c) 2009 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.model;

import is.codion.common.db.exception.DatabaseException;
import is.codion.common.db.exception.UpdateException;
import is.codion.common.event.EventDataListener;
import is.codion.common.event.EventListener;
import is.codion.common.model.CancelException;
import is.codion.common.state.State;
import is.codion.common.state.StateObserver;
import is.codion.common.user.User;
import is.codion.common.value.Value;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.local.LocalEntityConnectionProvider;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.Key;
import is.codion.framework.domain.entity.exception.ValidationException;
import is.codion.framework.domain.property.Property;
import is.codion.framework.model.test.TestDomain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.*;

public final class DefaultEntityEditModelTest {

  private static final User UNIT_TEST_USER =
          User.parse(System.getProperty("codion.test.user", "scott:tiger"));

  private static final Entities ENTITIES = new TestDomain().entities();

  private static final EntityConnectionProvider CONNECTION_PROVIDER = LocalEntityConnectionProvider.builder()
          .domainClassName(TestDomain.class.getName())
          .user(UNIT_TEST_USER)
          .build();

  private EntityEditModel employeeEditModel;

  @BeforeEach
  void setUp() {
    employeeEditModel = new TestEntityEditModel(TestDomain.T_EMP, CONNECTION_PROVIDER);
  }

  @Test
  void editEvents() throws DatabaseException, ValidationException {
    AtomicInteger insertEvents = new AtomicInteger();
    AtomicInteger updateEvents = new AtomicInteger();
    AtomicInteger deleteEvents = new AtomicInteger();

    EventDataListener<List<Entity>> insertListener = inserted -> insertEvents.incrementAndGet();
    EventDataListener<Map<Key, Entity>> updateListener = udpated -> updateEvents.incrementAndGet();
    EventDataListener<List<Entity>> deleteListener = deleted -> deleteEvents.incrementAndGet();

    EntityEditEvents.addInsertListener(TestDomain.T_EMP, insertListener);
    EntityEditEvents.addUpdateListener(TestDomain.T_EMP, updateListener);
    EntityEditEvents.addDeleteListener(TestDomain.T_EMP, deleteListener);

    employeeEditModel.setPostEditEvents(true);

    EntityConnection connection = employeeEditModel.connectionProvider().connection();
    connection.beginTransaction();
    try {
      Entity jones = connection.selectSingle(TestDomain.EMP_NAME, "JONES");
      employeeEditModel.setEntity(jones);
      employeeEditModel.put(TestDomain.EMP_NAME, "Noname");
      employeeEditModel.insert();
      assertEquals(1, insertEvents.get());
      employeeEditModel.put(TestDomain.EMP_NAME, "Another");
      employeeEditModel.update();
      assertEquals(1, updateEvents.get());
      employeeEditModel.delete();
      assertEquals(1, deleteEvents.get());
    }
    finally {
      connection.rollbackTransaction();
    }

    EntityEditEvents.removeInsertListener(TestDomain.T_EMP, insertListener);
    EntityEditEvents.removeUpdateListener(TestDomain.T_EMP, updateListener);
    EntityEditEvents.removeDeleteListener(TestDomain.T_EMP, deleteListener);
  }

  @Test
  void listeners() {
    assertThrows(IllegalArgumentException.class, () -> employeeEditModel.addValueListener(TestDomain.DEPARTMENT_ID, value -> {}));
    assertThrows(IllegalArgumentException.class, () -> employeeEditModel.addEditListener(TestDomain.DEPARTMENT_ID, value -> {}));
  }

  @Test
  void getForeignKeySearchModel() {
    assertFalse(employeeEditModel.containsSearchModel(TestDomain.EMP_DEPARTMENT_FK));
    EntitySearchModel model = employeeEditModel.foreignKeySearchModel(TestDomain.EMP_DEPARTMENT_FK);
    assertTrue(employeeEditModel.containsSearchModel(TestDomain.EMP_DEPARTMENT_FK));
    assertNotNull(model);
    assertEquals(model, employeeEditModel.foreignKeySearchModel(TestDomain.EMP_DEPARTMENT_FK));
  }

  @Test
  void createForeignKeySearchModel() {
    EntitySearchModel model = employeeEditModel.createForeignKeySearchModel(TestDomain.EMP_DEPARTMENT_FK);
    assertNotNull(model);
    assertEquals(TestDomain.T_DEPARTMENT, model.entityType());
  }

  @Test
  void refreshEntity() throws DatabaseException {
    EntityConnection connection = employeeEditModel.connectionProvider().connection();
    connection.beginTransaction();
    try {
      Entity employee = connection.selectSingle(TestDomain.EMP_NAME, "MARTIN");
      employeeEditModel.refreshEntity();
      employeeEditModel.setEntity(employee);
      employee.put(TestDomain.EMP_NAME, "NOONE");
      connection.update(employee);
      employeeEditModel.refreshEntity();
      assertEquals("NOONE", employeeEditModel.get(TestDomain.EMP_NAME));
    }
    finally {
      connection.rollbackTransaction();
    }
  }

  @Test
  void getEntityCopy() throws DatabaseException {
    Entity employee = employeeEditModel.connectionProvider().connection().selectSingle(
            TestDomain.EMP_NAME, "MARTIN");
    employeeEditModel.setEntity(employee);
    Entity copyWithPrimaryKeyValue = employeeEditModel.entityCopy();
    assertEquals(employee, copyWithPrimaryKeyValue);
    assertTrue(copyWithPrimaryKeyValue.primaryKey().isNotNull());
  }

  @Test
  void constructorNullEntityType() {
    assertThrows(NullPointerException.class, () -> new TestEntityEditModel(null, CONNECTION_PROVIDER));
  }

  @Test
  void constructorNullConnectionProvider() {
    assertThrows(NullPointerException.class, () -> new TestEntityEditModel(TestDomain.T_EMP, null));
  }

  @Test
  void getDefaultForeignKeyValue() throws DatabaseException {
    Entity employee = employeeEditModel.connectionProvider().connection().selectSingle(
            TestDomain.EMP_NAME, "MARTIN");
    employeeEditModel.setEntity(employee);
    //clear the department foreign key value
    Entity dept = employeeEditModel.referencedEntity(TestDomain.EMP_DEPARTMENT_FK);
    employeeEditModel.put(TestDomain.EMP_DEPARTMENT_FK, null);
    //set the reference key property value
    assertTrue(employeeEditModel.isNull(TestDomain.EMP_DEPARTMENT_FK));
    assertFalse(employeeEditModel.isNotNull(TestDomain.EMP_DEPARTMENT_FK));
    employeeEditModel.put(TestDomain.EMP_DEPARTMENT, dept.get(TestDomain.DEPARTMENT_ID));
    assertFalse(employeeEditModel.entityCopy().isLoaded(TestDomain.EMP_DEPARTMENT_FK));
    dept = employeeEditModel.get(TestDomain.EMP_DEPARTMENT_FK);
    assertNull(dept);
    employeeEditModel.setDefaultValues();
    assertNotNull(employeeEditModel.get(TestDomain.EMP_DEPARTMENT_FK));
  }

  @Test
  void defaultValueSupplier() throws DatabaseException {
    employeeEditModel.setDefaultValueSupplier(TestDomain.EMP_NAME, () -> "Scott");
    assertTrue(employeeEditModel.isNull(TestDomain.EMP_NAME));
    employeeEditModel.setDefaultValues();
    assertEquals("Scott", employeeEditModel.get(TestDomain.EMP_NAME));

    employeeEditModel.setDefaultValueSupplier(TestDomain.EMP_NAME, () -> null);
    employeeEditModel.setDefaultValues();
    assertTrue(employeeEditModel.isNull(TestDomain.EMP_NAME));
  }

  @Test
  void test() throws Exception {
    StateObserver primaryKeyNullState = employeeEditModel.primaryKeyNullObserver();
    StateObserver entityNewState = employeeEditModel.entityNewObserver();

    assertTrue(primaryKeyNullState.get());
    assertTrue(entityNewState.get());

    employeeEditModel.setReadOnly(false);
    assertFalse(employeeEditModel.isReadOnly());
    assertTrue(employeeEditModel.insertEnabledObserver().get());
    assertTrue(employeeEditModel.updateEnabledObserver().get());
    assertTrue(employeeEditModel.deleteEnabledObserver().get());

    employeeEditModel.setReadOnly(true);
    assertTrue(employeeEditModel.isReadOnly());
    assertFalse(employeeEditModel.insertEnabledObserver().get());
    assertFalse(employeeEditModel.updateEnabledObserver().get());
    assertFalse(employeeEditModel.deleteEnabledObserver().get());

    employeeEditModel.setDeleteEnabled(true);
    assertFalse(employeeEditModel.isReadOnly());

    employeeEditModel.setDeleteEnabled(false);
    assertTrue(employeeEditModel.isReadOnly());

    employeeEditModel.setUpdateEnabled(true);
    assertFalse(employeeEditModel.isReadOnly());

    employeeEditModel.setUpdateEnabled(false);
    assertTrue(employeeEditModel.isReadOnly());

    employeeEditModel.setReadOnly(false);
    assertTrue(employeeEditModel.insertEnabledObserver().get());
    assertTrue(employeeEditModel.updateEnabledObserver().get());
    assertTrue(employeeEditModel.deleteEnabledObserver().get());

    EventDataListener eventDataListener = data -> {};
    employeeEditModel.addAfterDeleteListener(eventDataListener);
    employeeEditModel.addAfterInsertListener(eventDataListener);
    employeeEditModel.addAfterUpdateListener(eventDataListener);
    employeeEditModel.addBeforeDeleteListener(eventDataListener);
    employeeEditModel.addBeforeInsertListener(eventDataListener);
    employeeEditModel.addBeforeUpdateListener(eventDataListener);
    EventListener listener = () -> {};
    employeeEditModel.addEntitiesEditedListener(listener);

    assertEquals(TestDomain.T_EMP, employeeEditModel.entityType());

    employeeEditModel.refresh();
    assertTrue(employeeEditModel.isEntityNew());
    assertFalse(employeeEditModel.modifiedObserver().get());

    Entity employee = employeeEditModel.connectionProvider().connection().selectSingle(TestDomain.EMP_NAME, "MARTIN");
    employeeEditModel.setEntity(employee);
    assertFalse(primaryKeyNullState.get());
    assertFalse(entityNewState.get());

    assertTrue(employeeEditModel.entityCopy().columnValuesEqual(employee), "Active entity is not equal to the entity just set");
    assertFalse(employeeEditModel.isEntityNew(), "Active entity is new after an entity is set");
    assertFalse(employeeEditModel.modifiedObserver().get());
    employeeEditModel.setDefaultValues();
    assertTrue(employeeEditModel.isEntityNew(), "Active entity is new after entity is set to null");
    assertFalse(employeeEditModel.modifiedObserver().get());
    assertTrue(employeeEditModel.entityCopy().primaryKey().isNull(), "Active entity primary key is not null after entity is set to null");

    employeeEditModel.setEntity(employee);
    assertTrue(employeeEditModel.entityCopy().primaryKey().isNotNull(), "Active entity primary key is null after entity is set");

    Integer originalEmployeeId = employeeEditModel.get(TestDomain.EMP_ID);
    employeeEditModel.put(TestDomain.EMP_ID, null);
    assertTrue(primaryKeyNullState.get());
    employeeEditModel.put(TestDomain.EMP_ID, originalEmployeeId);
    assertFalse(primaryKeyNullState.get());

    employeeEditModel.setDefaultValues();
    assertTrue(entityNewState.get());

    Double originalCommission = employeeEditModel.get(TestDomain.EMP_COMMISSION);
    final double commission = 1500.5;
    LocalDate originalHiredate = employeeEditModel.get(TestDomain.EMP_HIREDATE);
    LocalDate hiredate = LocalDate.now();
    String originalName = employeeEditModel.get(TestDomain.EMP_NAME);
    final String name = "Mr. Mr";

    employeeEditModel.put(TestDomain.EMP_COMMISSION, commission);
    assertTrue(employeeEditModel.modifiedObserver().get());
    employeeEditModel.put(TestDomain.EMP_HIREDATE, hiredate);
    employeeEditModel.put(TestDomain.EMP_NAME, name);

    assertEquals(employeeEditModel.get(TestDomain.EMP_COMMISSION), commission, "Commission does not fit");
    assertEquals(employeeEditModel.get(TestDomain.EMP_HIREDATE), hiredate, "Hiredate does not fit");
    assertEquals(employeeEditModel.get(TestDomain.EMP_NAME), name, "Name does not fit");

    employeeEditModel.put(TestDomain.EMP_COMMISSION, originalCommission);
    assertTrue(employeeEditModel.isModified());
    assertTrue(employeeEditModel.modifiedObserver().get());
    employeeEditModel.put(TestDomain.EMP_HIREDATE, originalHiredate);
    assertTrue(employeeEditModel.isModified());
    employeeEditModel.put(TestDomain.EMP_NAME, originalName);
    assertFalse(employeeEditModel.isModified());

    employeeEditModel.put(TestDomain.EMP_COMMISSION, 50d);
    assertNotNull(employeeEditModel.remove(TestDomain.EMP_COMMISSION));
    assertNull(employeeEditModel.get(TestDomain.EMP_COMMISSION));

    //test validation
    try {
      employeeEditModel.put(TestDomain.EMP_COMMISSION, 50d);
      employeeEditModel.validate(TestDomain.EMP_COMMISSION);
      fail("Validation should fail on invalid commission value");
    }
    catch (ValidationException e) {
      assertEquals(TestDomain.EMP_COMMISSION, e.getAttribute());
      assertEquals(50d, e.getValue());
      Property<?> property = ENTITIES.definition(TestDomain.T_EMP).property(e.getAttribute());
      assertTrue(e.getMessage().contains(property.toString()));
      assertTrue(e.getMessage().contains(property.minimumValue().toString()));
    }

    employeeEditModel.setDefaultValues();
    assertTrue(employeeEditModel.entityCopy().primaryKey().isNull(), "Active entity is not null after model is cleared");

    employeeEditModel.removeAfterDeleteListener(eventDataListener);
    employeeEditModel.removeAfterInsertListener(eventDataListener);
    employeeEditModel.removeAfterUpdateListener(eventDataListener);
    employeeEditModel.removeBeforeDeleteListener(eventDataListener);
    employeeEditModel.removeBeforeInsertListener(eventDataListener);
    employeeEditModel.removeBeforeUpdateListener(eventDataListener);
    employeeEditModel.removeEntitiesEditedListener(listener);
  }

  @Test
  void insertReadOnly() throws CancelException, ValidationException, DatabaseException {
    employeeEditModel.setReadOnly(true);
    assertThrows(IllegalStateException.class, () -> employeeEditModel.insert());
  }

  @Test
  void updateReadOnly() throws CancelException, ValidationException, DatabaseException {
    employeeEditModel.setReadOnly(true);
    assertThrows(IllegalStateException.class, () -> employeeEditModel.update());
  }

  @Test
  void deleteReadOnly() throws CancelException, DatabaseException {
    employeeEditModel.setReadOnly(true);
    assertThrows(IllegalStateException.class, () -> employeeEditModel.delete());
  }

  @Test
  void insert() throws Exception {
    assertTrue(employeeEditModel.insert(new ArrayList<>()).isEmpty());
    EntityConnection connection = employeeEditModel.connectionProvider().connection();
    connection.beginTransaction();
    try {
      employeeEditModel.put(TestDomain.EMP_COMMISSION, 1000d);
      employeeEditModel.put(TestDomain.EMP_HIREDATE, LocalDate.now());
      employeeEditModel.put(TestDomain.EMP_JOB, "CLERK");
      employeeEditModel.put(TestDomain.EMP_NAME, "Björn");
      employeeEditModel.put(TestDomain.EMP_SALARY, 1000d);

      Entity tmpDept = ENTITIES.builder(TestDomain.T_DEPARTMENT)
              .with(TestDomain.DEPARTMENT_ID, 99)
              .with(TestDomain.DEPARTMENT_LOCATION, "Limbo")
              .with(TestDomain.DEPARTMENT_NAME, "Judgment")
              .build();

      Entity department = connection
              .select(connection.insert(tmpDept));

      employeeEditModel.put(TestDomain.EMP_DEPARTMENT_FK, department);

      employeeEditModel.addAfterInsertListener(insertedEntities ->
              assertEquals(department, insertedEntities.get(0).get(TestDomain.EMP_DEPARTMENT_FK)));
      employeeEditModel.setInsertEnabled(false);
      assertFalse(employeeEditModel.isInsertEnabled());
      assertThrows(IllegalStateException.class, () -> employeeEditModel.insert());
      employeeEditModel.setInsertEnabled(true);
      assertTrue(employeeEditModel.isInsertEnabled());

      employeeEditModel.insert();
      assertFalse(employeeEditModel.isEntityNew());
      Entity entityCopy = employeeEditModel.entityCopy();
      assertTrue(entityCopy.primaryKey().isNotNull());
      assertEquals(entityCopy.primaryKey(), entityCopy.originalPrimaryKey());

      employeeEditModel.put(TestDomain.EMP_NAME, "Bobby");
      try {
        employeeEditModel.insert();
      }
      catch (Exception e) {
        fail("Should be able to insert again");
      }
    }
    finally {
      connection.rollbackTransaction();
    }
  }

  @Test
  void update() throws Exception {
    assertThrows(UpdateException.class, () -> employeeEditModel.update());
    assertTrue(employeeEditModel.update(new ArrayList<>()).isEmpty());
    EntityConnection connection = employeeEditModel.connectionProvider().connection();
    connection.beginTransaction();
    try {
      employeeEditModel.setEntity(connection.selectSingle(TestDomain.EMP_NAME, "MILLER"));
      employeeEditModel.put(TestDomain.EMP_NAME, "BJORN");
      List<Entity> toUpdate = singletonList(employeeEditModel.entityCopy());
      EventDataListener<Map<Key, Entity>> listener = updatedEntities ->
              assertEquals(toUpdate, new ArrayList<>(updatedEntities.values()));
      employeeEditModel.addAfterUpdateListener(listener);
      employeeEditModel.setUpdateEnabled(false);
      assertFalse(employeeEditModel.isUpdateEnabled());
      assertThrows(IllegalStateException.class, () -> employeeEditModel.update());
      employeeEditModel.setUpdateEnabled(true);
      assertTrue(employeeEditModel.isUpdateEnabled());

      employeeEditModel.update();
      assertFalse(employeeEditModel.modifiedObserver().get());
      employeeEditModel.removeAfterUpdateListener(listener);
    }
    finally {
      connection.rollbackTransaction();
    }
  }

  @Test
  void delete() throws Exception {
    assertTrue(employeeEditModel.delete(new ArrayList<>()).isEmpty());
    EntityConnection connection = employeeEditModel.connectionProvider().connection();
    connection.beginTransaction();
    try {
      employeeEditModel.setEntity(connection.selectSingle(TestDomain.EMP_NAME, "MILLER"));
      List<Entity> toDelete = singletonList(employeeEditModel.entityCopy());
      employeeEditModel.addAfterDeleteListener(deletedEntities -> assertEquals(toDelete, deletedEntities));
      employeeEditModel.setDeleteEnabled(false);
      assertFalse(employeeEditModel.isDeleteEnabled());
      assertThrows(IllegalStateException.class, () -> employeeEditModel.delete());
      employeeEditModel.setDeleteEnabled(true);
      assertTrue(employeeEditModel.isDeleteEnabled());

      employeeEditModel.delete();
    }
    finally {
      connection.rollbackTransaction();
    }
  }

  @Test
  void setEntity() throws Exception {
    Entity martin = employeeEditModel.connectionProvider().connection().selectSingle(TestDomain.EMP_NAME, "MARTIN");
    Entity king = employeeEditModel.connectionProvider().connection().selectSingle(TestDomain.EMP_NAME, "KING");
    employeeEditModel.setEntity(king);
    employeeEditModel.put(TestDomain.EMP_MGR_FK, martin);
    employeeEditModel.setDefaultValues();
    king.put(TestDomain.EMP_MGR_FK, null);
    employeeEditModel.setEntity(king);
    assertNull(employeeEditModel.get(TestDomain.EMP_MGR_FK));
    employeeEditModel.setDefaultValues();
    assertEquals(LocalDate.now(), employeeEditModel.get(TestDomain.EMP_HIREDATE));
    assertFalse(employeeEditModel.entityCopy().isModified(TestDomain.EMP_HIREDATE));
    assertFalse(employeeEditModel.entityCopy().isModified());
  }

  @Test
  void setPersistValue() throws Exception {
    Entity king = employeeEditModel.connectionProvider().connection().selectSingle(TestDomain.EMP_NAME, "KING");
    employeeEditModel.setEntity(king);
    assertNotNull(employeeEditModel.get(TestDomain.EMP_JOB));
    employeeEditModel.setPersistValue(TestDomain.EMP_JOB, true);
    employeeEditModel.setDefaultValues();
    assertNotNull(employeeEditModel.get(TestDomain.EMP_JOB));
    employeeEditModel.setEntity(king);
    employeeEditModel.setPersistValue(TestDomain.EMP_JOB, false);
    employeeEditModel.setDefaultValues();
    assertNull(employeeEditModel.get(TestDomain.EMP_JOB));
    assertThrows(IllegalArgumentException.class, () -> employeeEditModel.setPersistValue(TestDomain.DEPARTMENT_ID, true));
    assertThrows(IllegalArgumentException.class, () -> employeeEditModel.isPersistValue(TestDomain.DEPARTMENT_ID));
  }

  @Test
  void containsUnsavedData() throws DatabaseException {
    employeeEditModel.setWarnAboutUnsavedData(true);
    employeeEditModel.setPersistValue(TestDomain.EMP_DEPARTMENT_FK, false);

    EventDataListener<State> alwaysConfirmListener = data -> data.set(true);
    EventDataListener<State> alwaysDenyListener = data -> data.set(false);

    employeeEditModel.addConfirmSetEntityObserver(alwaysConfirmListener);
    Entity king = employeeEditModel.connectionProvider().connection().selectSingle(TestDomain.EMP_NAME, "KING");
    Entity adams = employeeEditModel.connectionProvider().connection().selectSingle(TestDomain.EMP_NAME, "ADAMS");
    employeeEditModel.setEntity(king);
    employeeEditModel.put(TestDomain.EMP_NAME, "New name");
    employeeEditModel.setEntity(adams);
    assertEquals(adams, employeeEditModel.entityCopy());

    employeeEditModel.removeConfirmSetEntityObserver(alwaysConfirmListener);
    employeeEditModel.setDefaultValues();
    employeeEditModel.addConfirmSetEntityObserver(alwaysDenyListener);

    employeeEditModel.put(TestDomain.EMP_NAME, "A name");
    employeeEditModel.setEntity(king);
    assertEquals("A name", employeeEditModel.get(TestDomain.EMP_NAME));

    employeeEditModel.removeConfirmSetEntityObserver(alwaysDenyListener);
    employeeEditModel.setDefaultValues();
    employeeEditModel.addConfirmSetEntityObserver(alwaysDenyListener);

    employeeEditModel.put(TestDomain.EMP_DEPARTMENT_FK, king.get(TestDomain.EMP_DEPARTMENT_FK));
    employeeEditModel.setEntity(adams);
    assertEquals(king.get(TestDomain.EMP_DEPARTMENT_FK), employeeEditModel.get(TestDomain.EMP_DEPARTMENT_FK));

    employeeEditModel.setWarnAboutUnsavedData(false);
  }

  @Test
  void replaceForeignKeyValues() throws DatabaseException {
    Entity james = employeeEditModel.connectionProvider().connection()
            .selectSingle(TestDomain.EMP_NAME, "JAMES");
    employeeEditModel.setEntity(james);
    Entity blake = employeeEditModel.connectionProvider().connection()
            .selectSingle(TestDomain.EMP_NAME, "BLAKE");
    assertNotSame(employeeEditModel.referencedEntity(TestDomain.EMP_MGR_FK), blake);
    employeeEditModel.replaceForeignKeyValues(singletonList(blake));
    assertSame(employeeEditModel.referencedEntity(TestDomain.EMP_MGR_FK), blake);
  }

  @Test
  void value() {
    Value<Integer> value = employeeEditModel.value(TestDomain.EMP_MGR);
    assertSame(value, employeeEditModel.value(TestDomain.EMP_MGR));
    value.set(42);
    assertEquals(42, employeeEditModel.get(TestDomain.EMP_MGR));
    employeeEditModel.put(TestDomain.EMP_MGR, 2);
    assertEquals(2, value.get());
    value.set(null);
    assertFalse(employeeEditModel.getOptional(TestDomain.EMP_MGR).isPresent());
    assertFalse(employeeEditModel.getOptional(TestDomain.EMP_MGR_FK).isPresent());
    value.set(3);
    assertTrue(employeeEditModel.getOptional(TestDomain.EMP_MGR).isPresent());
  }

  @Test
  void derivedProperties() {
    EntityEditModel editModel = new DefaultEntityEditModel(TestDomain.T_DETAIL, employeeEditModel.connectionProvider()) {
      @Override
      public void addForeignKeyValues(List<Entity> entities) {}
      @Override
      public void removeForeignKeyValues(List<Entity> entities) {}
      @Override
      public void clear() {}
      @Override
      public StateObserver refreshingObserver() {
        return null;
      }
      @Override
      public void addRefreshingObserver(StateObserver refreshingObserver) {}
    };

    AtomicInteger derivedCounter = new AtomicInteger();
    AtomicInteger derivedEditCounter = new AtomicInteger();

    editModel.addValueListener(TestDomain.DETAIL_INT_DERIVED, value -> derivedCounter.incrementAndGet());
    editModel.addEditListener(TestDomain.DETAIL_INT_DERIVED, value -> derivedEditCounter.incrementAndGet());

    editModel.put(TestDomain.DETAIL_INT, 1);
    assertEquals(1, derivedCounter.get());
    assertEquals(1, derivedEditCounter.get());

    editModel.put(TestDomain.DETAIL_INT, 2);
    assertEquals(2, derivedCounter.get());
    assertEquals(2, derivedEditCounter.get());

    Entity detail = ENTITIES.builder(TestDomain.T_DETAIL)
            .with(TestDomain.DETAIL_INT, 3)
            .build();
    editModel.setEntity(detail);
    assertEquals(3, derivedCounter.get());
    assertEquals(2, derivedEditCounter.get());
  }

  @Test
  void foreignKeyProperties() throws DatabaseException {
    AtomicInteger deptNoChange = new AtomicInteger();
    employeeEditModel.addValueListener(TestDomain.EMP_DEPARTMENT, value -> deptNoChange.incrementAndGet());
    AtomicInteger deptChange = new AtomicInteger();
    employeeEditModel.addValueListener(TestDomain.EMP_DEPARTMENT_FK, value -> deptChange.incrementAndGet());
    AtomicInteger deptEdit = new AtomicInteger();
    employeeEditModel.addEditListener(TestDomain.EMP_DEPARTMENT_FK, value -> deptEdit.incrementAndGet());

    Entity dept = employeeEditModel.connectionProvider().connection().selectSingle(TestDomain.DEPARTMENT_ID, 10);
    employeeEditModel.put(TestDomain.EMP_DEPARTMENT_FK, dept);

    employeeEditModel.put(TestDomain.EMP_DEPARTMENT, 20);
    assertEquals(2, deptNoChange.get());
    assertEquals(2, deptChange.get());
    assertEquals(2, deptEdit.get());

    dept = employeeEditModel.connectionProvider().connection().selectSingle(TestDomain.DEPARTMENT_ID, 20);
    employeeEditModel.put(TestDomain.EMP_DEPARTMENT_FK, dept);

    assertEquals(2, deptNoChange.get());
    assertEquals(3, deptChange.get());
    assertEquals(3, deptEdit.get());

    employeeEditModel.put(TestDomain.EMP_DEPARTMENT, 30);

    assertEquals(3, deptNoChange.get());
    assertEquals(4, deptChange.get());
    assertEquals(4, deptEdit.get());

    dept = employeeEditModel.connectionProvider().connection().selectSingle(TestDomain.DEPARTMENT_ID, 30);
    employeeEditModel.put(TestDomain.EMP_DEPARTMENT_FK, dept);

    assertEquals(3, deptNoChange.get());
    assertEquals(5, deptChange.get());
    assertEquals(5, deptEdit.get());
  }

  @Test
  void initializeForeignKeyToNull() throws DatabaseException {
    Entity dept = employeeEditModel.connectionProvider().connection().selectSingle(TestDomain.DEPARTMENT_ID, 10);

    employeeEditModel.initialize(TestDomain.EMP_DEPARTMENT_FK, dept);
    assertEquals(dept, employeeEditModel.get(TestDomain.EMP_DEPARTMENT_FK));

    employeeEditModel.initialize(TestDomain.EMP_DEPARTMENT_FK, null);
    assertEquals(dept, employeeEditModel.get(TestDomain.EMP_DEPARTMENT_FK));

    employeeEditModel.setInitializeForeignKeyToNull(true);

    employeeEditModel.initialize(TestDomain.EMP_DEPARTMENT_FK, null);
    assertTrue(employeeEditModel.isNull(TestDomain.EMP_DEPARTMENT_FK));

    employeeEditModel.setInitializeForeignKeyToNull(false);
    employeeEditModel.put(TestDomain.EMP_DEPARTMENT_FK, dept);
    employeeEditModel.initialize(TestDomain.EMP_DEPARTMENT_FK, null);
    assertEquals(dept, employeeEditModel.get(TestDomain.EMP_DEPARTMENT_FK));
  }

  private static final class TestEntityEditModel extends DefaultEntityEditModel {

    public TestEntityEditModel(EntityType entityType, EntityConnectionProvider connectionProvider) {
      super(entityType, connectionProvider);
      setDefaultValueSupplier(TestDomain.EMP_HIREDATE, LocalDate::now);
    }

    @Override
    public void addForeignKeyValues(List<Entity> entities) {}

    @Override
    public void removeForeignKeyValues(List<Entity> entities) {}

    @Override
    public void clear() {}

    @Override
    public StateObserver refreshingObserver() {
      return null;
    }

    @Override
    public void addRefreshingObserver(StateObserver refreshingObserver) {}
  }
}
