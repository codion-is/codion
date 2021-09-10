/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.model;

import is.codion.common.db.database.DatabaseFactory;
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
          User.parseUser(System.getProperty("codion.test.user", "scott:tiger"));
  private static final Entities ENTITIES = new TestDomain().getEntities();
  private static final EntityConnectionProvider CONNECTION_PROVIDER = new LocalEntityConnectionProvider(
          DatabaseFactory.getDatabase()).setDomainClassName(TestDomain.class.getName()).setUser(UNIT_TEST_USER);

  private EntityEditModel employeeEditModel;

  @BeforeEach
  void setUp() {
    employeeEditModel = new TestEntityEditModel(TestDomain.T_EMP, CONNECTION_PROVIDER);
  }

  @Test
  void editEvents() throws DatabaseException, ValidationException {
    final AtomicInteger insertEvents = new AtomicInteger();
    final AtomicInteger updateEvents = new AtomicInteger();
    final AtomicInteger deleteEvents = new AtomicInteger();

    final EventDataListener<List<Entity>> insertListener = inserted -> insertEvents.incrementAndGet();
    final EventDataListener<Map<Key, Entity>> updateListener = udpated -> updateEvents.incrementAndGet();
    final EventDataListener<List<Entity>> deleteListener = deleted -> deleteEvents.incrementAndGet();

    EntityEditEvents.addInsertListener(TestDomain.T_EMP, insertListener);
    EntityEditEvents.addUpdateListener(TestDomain.T_EMP, updateListener);
    EntityEditEvents.addDeleteListener(TestDomain.T_EMP, deleteListener);

    employeeEditModel.setPostEditEvents(true);

    final EntityConnection connection = employeeEditModel.getConnectionProvider().getConnection();
    connection.beginTransaction();
    try {
      final Entity jones = connection.selectSingle(TestDomain.EMP_NAME, "JONES");
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
    assertThrows(IllegalArgumentException.class, () -> employeeEditModel.addValueListener(TestDomain.DEPARTMENT_ID, valueChange -> {}));
    assertThrows(IllegalArgumentException.class, () -> employeeEditModel.addValueEditListener(TestDomain.DEPARTMENT_ID, valueChange -> {}));
  }

  @Test
  void getForeignKeySearchModel() {
    assertFalse(employeeEditModel.containsSearchModel(TestDomain.EMP_DEPARTMENT_FK));
    final EntitySearchModel model = employeeEditModel.getForeignKeySearchModel(TestDomain.EMP_DEPARTMENT_FK);
    assertTrue(employeeEditModel.containsSearchModel(TestDomain.EMP_DEPARTMENT_FK));
    assertNotNull(model);
    assertEquals(model, employeeEditModel.getForeignKeySearchModel(TestDomain.EMP_DEPARTMENT_FK));
  }

  @Test
  void createForeignKeySearchModel() {
    final EntitySearchModel model = employeeEditModel.createForeignKeySearchModel(TestDomain.EMP_DEPARTMENT_FK);
    assertNotNull(model);
    assertEquals(TestDomain.T_DEPARTMENT, model.getEntityType());
  }

  @Test
  void refreshEntity() throws DatabaseException {
    final EntityConnection connection = employeeEditModel.getConnectionProvider().getConnection();
    connection.beginTransaction();
    try {
      final Entity employee = connection.selectSingle(TestDomain.EMP_NAME, "MARTIN");
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
    final Entity employee = employeeEditModel.getConnectionProvider().getConnection().selectSingle(
            TestDomain.EMP_NAME, "MARTIN");
    employeeEditModel.setEntity(employee);
    final Entity copyWithPrimaryKeyValue = employeeEditModel.getEntityCopy();
    assertEquals(employee, copyWithPrimaryKeyValue);
    assertTrue(copyWithPrimaryKeyValue.getPrimaryKey().isNotNull());
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
    final Entity employee = employeeEditModel.getConnectionProvider().getConnection().selectSingle(
            TestDomain.EMP_NAME, "MARTIN");
    employeeEditModel.setEntity(employee);
    //clear the department foreign key value
    Entity dept = employeeEditModel.getForeignKey(TestDomain.EMP_DEPARTMENT_FK);
    employeeEditModel.put(TestDomain.EMP_DEPARTMENT_FK, null);
    //set the reference key property value
    assertTrue(employeeEditModel.isNull(TestDomain.EMP_DEPARTMENT_FK));
    assertFalse(employeeEditModel.isNotNull(TestDomain.EMP_DEPARTMENT_FK));
    employeeEditModel.put(TestDomain.EMP_DEPARTMENT, dept.get(TestDomain.DEPARTMENT_ID));
    assertFalse(employeeEditModel.getEntityCopy().isLoaded(TestDomain.EMP_DEPARTMENT_FK));
    dept = employeeEditModel.getForeignKey(TestDomain.EMP_DEPARTMENT_FK);
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
    final StateObserver primaryKeyNullState = employeeEditModel.getPrimaryKeyNullObserver();
    final StateObserver entityNewState = employeeEditModel.getEntityNewObserver();

    assertTrue(primaryKeyNullState.get());
    assertTrue(entityNewState.get());

    employeeEditModel.setReadOnly(false);
    assertFalse(employeeEditModel.isReadOnly());
    assertTrue(employeeEditModel.getInsertEnabledObserver().get());
    assertTrue(employeeEditModel.getUpdateEnabledObserver().get());
    assertTrue(employeeEditModel.getDeleteEnabledObserver().get());

    employeeEditModel.setReadOnly(true);
    assertTrue(employeeEditModel.isReadOnly());
    assertFalse(employeeEditModel.getInsertEnabledObserver().get());
    assertFalse(employeeEditModel.getUpdateEnabledObserver().get());
    assertFalse(employeeEditModel.getDeleteEnabledObserver().get());

    employeeEditModel.setDeleteEnabled(true);
    assertFalse(employeeEditModel.isReadOnly());

    employeeEditModel.setDeleteEnabled(false);
    assertTrue(employeeEditModel.isReadOnly());

    employeeEditModel.setUpdateEnabled(true);
    assertFalse(employeeEditModel.isReadOnly());

    employeeEditModel.setUpdateEnabled(false);
    assertTrue(employeeEditModel.isReadOnly());

    employeeEditModel.setReadOnly(false);
    assertTrue(employeeEditModel.getInsertEnabledObserver().get());
    assertTrue(employeeEditModel.getUpdateEnabledObserver().get());
    assertTrue(employeeEditModel.getDeleteEnabledObserver().get());

    final EventDataListener eventDataListener = data -> {};
    employeeEditModel.addAfterDeleteListener(eventDataListener);
    employeeEditModel.addAfterInsertListener(eventDataListener);
    employeeEditModel.addAfterUpdateListener(eventDataListener);
    employeeEditModel.addBeforeDeleteListener(eventDataListener);
    employeeEditModel.addBeforeInsertListener(eventDataListener);
    employeeEditModel.addBeforeUpdateListener(eventDataListener);
    final EventListener listener = () -> {};
    employeeEditModel.addEntitiesEditedListener(listener);
    employeeEditModel.addBeforeRefreshListener(listener);
    employeeEditModel.addAfterRefreshListener(listener);

    assertEquals(TestDomain.T_EMP, employeeEditModel.getEntityType());

    employeeEditModel.refresh();
    assertTrue(employeeEditModel.isEntityNew());
    assertFalse(employeeEditModel.getModifiedObserver().get());

    final Entity employee = employeeEditModel.getConnectionProvider().getConnection().selectSingle(TestDomain.EMP_NAME, "MARTIN");
    employeeEditModel.setEntity(employee);
    assertFalse(primaryKeyNullState.get());
    assertFalse(entityNewState.get());

    assertTrue(employeeEditModel.getEntityCopy().columnValuesEqual(employee), "Active entity is not equal to the entity just set");
    assertFalse(employeeEditModel.isEntityNew(), "Active entity is new after an entity is set");
    assertFalse(employeeEditModel.getModifiedObserver().get());
    employeeEditModel.setDefaultValues();
    assertTrue(employeeEditModel.isEntityNew(), "Active entity is new after entity is set to null");
    assertFalse(employeeEditModel.getModifiedObserver().get());
    assertTrue(employeeEditModel.getEntityCopy().getPrimaryKey().isNull(), "Active entity primary key is not null after entity is set to null");

    employeeEditModel.setEntity(employee);
    assertTrue(employeeEditModel.getEntityCopy().getPrimaryKey().isNotNull(), "Active entity primary key is null after entity is set");

    final Integer originalEmployeeId = employeeEditModel.get(TestDomain.EMP_ID);
    employeeEditModel.put(TestDomain.EMP_ID, null);
    assertTrue(primaryKeyNullState.get());
    employeeEditModel.put(TestDomain.EMP_ID, originalEmployeeId);
    assertFalse(primaryKeyNullState.get());

    employeeEditModel.setDefaultValues();
    assertTrue(entityNewState.get());

    final Double originalCommission = employeeEditModel.get(TestDomain.EMP_COMMISSION);
    final double commission = 1500.5;
    final LocalDate originalHiredate = employeeEditModel.get(TestDomain.EMP_HIREDATE);
    final LocalDate hiredate = LocalDate.now();
    final String originalName = employeeEditModel.get(TestDomain.EMP_NAME);
    final String name = "Mr. Mr";

    employeeEditModel.put(TestDomain.EMP_COMMISSION, commission);
    assertTrue(employeeEditModel.getModifiedObserver().get());
    employeeEditModel.put(TestDomain.EMP_HIREDATE, hiredate);
    employeeEditModel.put(TestDomain.EMP_NAME, name);

    assertEquals(employeeEditModel.get(TestDomain.EMP_COMMISSION), commission, "Commission does not fit");
    assertEquals(employeeEditModel.get(TestDomain.EMP_HIREDATE), hiredate, "Hiredate does not fit");
    assertEquals(employeeEditModel.get(TestDomain.EMP_NAME), name, "Name does not fit");

    employeeEditModel.put(TestDomain.EMP_COMMISSION, originalCommission);
    assertTrue(employeeEditModel.isModified());
    assertTrue(employeeEditModel.getModifiedObserver().get());
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
    catch (final ValidationException e) {
      assertEquals(TestDomain.EMP_COMMISSION, e.getAttribute());
      assertEquals(50d, e.getValue());
      final Property<?> property = ENTITIES.getDefinition(TestDomain.T_EMP).getProperty(e.getAttribute());
      assertTrue(e.getMessage().contains(property.toString()));
      assertTrue(e.getMessage().contains(property.getMinimumValue().toString()));
    }

    employeeEditModel.setDefaultValues();
    assertTrue(employeeEditModel.getEntityCopy().getPrimaryKey().isNull(), "Active entity is not null after model is cleared");

    employeeEditModel.removeAfterDeleteListener(eventDataListener);
    employeeEditModel.removeAfterInsertListener(eventDataListener);
    employeeEditModel.removeAfterUpdateListener(eventDataListener);
    employeeEditModel.removeBeforeDeleteListener(eventDataListener);
    employeeEditModel.removeBeforeInsertListener(eventDataListener);
    employeeEditModel.removeBeforeUpdateListener(eventDataListener);
    employeeEditModel.removeEntitiesEditedListener(listener);
    employeeEditModel.removeBeforeRefreshListener(listener);
    employeeEditModel.removeAfterRefreshListener(listener);
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
    final EntityConnection connection = employeeEditModel.getConnectionProvider().getConnection();
    connection.beginTransaction();
    try {
      employeeEditModel.put(TestDomain.EMP_COMMISSION, 1000d);
      employeeEditModel.put(TestDomain.EMP_HIREDATE, LocalDate.now());
      employeeEditModel.put(TestDomain.EMP_JOB, "CLERK");
      employeeEditModel.put(TestDomain.EMP_NAME, "Björn");
      employeeEditModel.put(TestDomain.EMP_SALARY, 1000d);

      final Entity tmpDept = ENTITIES.builder(TestDomain.T_DEPARTMENT)
              .with(TestDomain.DEPARTMENT_ID, 99)
              .with(TestDomain.DEPARTMENT_LOCATION, "Limbo")
              .with(TestDomain.DEPARTMENT_NAME, "Judgment")
              .build();

      final Entity department = connection
              .selectSingle(connection.insert(tmpDept));

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
      final Entity entityCopy = employeeEditModel.getEntityCopy();
      assertTrue(entityCopy.getPrimaryKey().isNotNull());
      assertEquals(entityCopy.getPrimaryKey(), entityCopy.getOriginalPrimaryKey());

      employeeEditModel.put(TestDomain.EMP_NAME, "Bobby");
      try {
        employeeEditModel.insert();
      }
      catch (final Exception e) {
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
    final EntityConnection connection = employeeEditModel.getConnectionProvider().getConnection();
    connection.beginTransaction();
    try {
      employeeEditModel.setEntity(connection.selectSingle(TestDomain.EMP_NAME, "MILLER"));
      employeeEditModel.put(TestDomain.EMP_NAME, "BJORN");
      final List<Entity> toUpdate = singletonList(employeeEditModel.getEntityCopy());
      final EventDataListener<Map<Key, Entity>> listener = updatedEntities ->
              assertEquals(toUpdate, new ArrayList<>(updatedEntities.values()));
      employeeEditModel.addAfterUpdateListener(listener);
      employeeEditModel.setUpdateEnabled(false);
      assertFalse(employeeEditModel.isUpdateEnabled());
      assertThrows(IllegalStateException.class, () -> employeeEditModel.update());
      employeeEditModel.setUpdateEnabled(true);
      assertTrue(employeeEditModel.isUpdateEnabled());

      employeeEditModel.update();
      assertFalse(employeeEditModel.getModifiedObserver().get());
      employeeEditModel.removeAfterUpdateListener(listener);
    }
    finally {
      connection.rollbackTransaction();
    }
  }

  @Test
  void delete() throws Exception {
    assertTrue(employeeEditModel.delete(new ArrayList<>()).isEmpty());
    final EntityConnection connection = employeeEditModel.getConnectionProvider().getConnection();
    connection.beginTransaction();
    try {
      employeeEditModel.setEntity(connection.selectSingle(TestDomain.EMP_NAME, "MILLER"));
      final List<Entity> toDelete = singletonList(employeeEditModel.getEntityCopy());
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
    final Entity martin = employeeEditModel.getConnectionProvider().getConnection().selectSingle(TestDomain.EMP_NAME, "MARTIN");
    final Entity king = employeeEditModel.getConnectionProvider().getConnection().selectSingle(TestDomain.EMP_NAME, "KING");
    employeeEditModel.setEntity(king);
    employeeEditModel.put(TestDomain.EMP_MGR_FK, martin);
    employeeEditModel.setDefaultValues();
    king.put(TestDomain.EMP_MGR_FK, null);
    employeeEditModel.setEntity(king);
    assertNull(employeeEditModel.get(TestDomain.EMP_MGR_FK));
    employeeEditModel.setDefaultValues();
    assertEquals(LocalDate.now(), employeeEditModel.get(TestDomain.EMP_HIREDATE));
    assertFalse(employeeEditModel.getEntityCopy().isModified(TestDomain.EMP_HIREDATE));
    assertFalse(employeeEditModel.getEntityCopy().isModified());
  }

  @Test
  void setPersistValue() throws Exception {
    final Entity king = employeeEditModel.getConnectionProvider().getConnection().selectSingle(TestDomain.EMP_NAME, "KING");
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

    final EventDataListener<State> alwaysConfirmListener = data -> data.set(true);
    final EventDataListener<State> alwaysDenyListener = data -> data.set(false);

    employeeEditModel.addConfirmSetEntityObserver(alwaysConfirmListener);
    final Entity king = employeeEditModel.getConnectionProvider().getConnection().selectSingle(TestDomain.EMP_NAME, "KING");
    final Entity adams = employeeEditModel.getConnectionProvider().getConnection().selectSingle(TestDomain.EMP_NAME, "ADAMS");
    employeeEditModel.setEntity(king);
    employeeEditModel.put(TestDomain.EMP_NAME, "New name");
    employeeEditModel.setEntity(adams);
    assertEquals(adams, employeeEditModel.getEntityCopy());

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
    final Entity james = employeeEditModel.getConnectionProvider().getConnection()
            .selectSingle(TestDomain.EMP_NAME, "JAMES");
    employeeEditModel.setEntity(james);
    final Entity blake = employeeEditModel.getConnectionProvider().getConnection()
            .selectSingle(TestDomain.EMP_NAME, "BLAKE");
    assertNotSame(employeeEditModel.getForeignKey(TestDomain.EMP_MGR_FK), blake);
    employeeEditModel.replaceForeignKeyValues(singletonList(blake));
    assertSame(employeeEditModel.getForeignKey(TestDomain.EMP_MGR_FK), blake);
  }

  @Test
  void value() {
    final Value<Integer> value = employeeEditModel.value(TestDomain.EMP_MGR);
    assertSame(value, employeeEditModel.value(TestDomain.EMP_MGR));
    value.set(42);
    assertEquals(42, employeeEditModel.get(TestDomain.EMP_MGR));
    employeeEditModel.put(TestDomain.EMP_MGR, 2);
    assertEquals(2, value.get());
  }

  @Test
  void derivedProperties() {
    final EntityEditModel editModel = new DefaultEntityEditModel(TestDomain.T_DETAIL, employeeEditModel.getConnectionProvider()) {
      @Override
      public void addForeignKeyValues(final List<Entity> entities) {}
      @Override
      public void removeForeignKeyValues(final List<Entity> entities) {}
      @Override
      public void clear() {}
    };

    final AtomicInteger derivedCounter = new AtomicInteger();
    final AtomicInteger derivedEditCounter = new AtomicInteger();

    editModel.addValueListener(TestDomain.DETAIL_INT_DERIVED, valueChange -> derivedCounter.incrementAndGet());
    editModel.addValueEditListener(TestDomain.DETAIL_INT_DERIVED, valueChange -> derivedEditCounter.incrementAndGet());

    editModel.put(TestDomain.DETAIL_INT, 1);
    assertEquals(1, derivedCounter.get());
    assertEquals(1, derivedEditCounter.get());

    editModel.put(TestDomain.DETAIL_INT, 2);
    assertEquals(2, derivedCounter.get());
    assertEquals(2, derivedEditCounter.get());

    final Entity detail = ENTITIES.builder(TestDomain.T_DETAIL)
            .with(TestDomain.DETAIL_INT, 3)
            .build();
    editModel.setEntity(detail);
    assertEquals(3, derivedCounter.get());
    assertEquals(2, derivedEditCounter.get());
  }

  @Test
  void foreignKeyProperties() throws DatabaseException {
    final AtomicInteger deptNoChange = new AtomicInteger();
    employeeEditModel.addValueListener(TestDomain.EMP_DEPARTMENT, valueChange -> deptNoChange.incrementAndGet());
    final AtomicInteger deptChange = new AtomicInteger();
    employeeEditModel.addValueListener(TestDomain.EMP_DEPARTMENT_FK, valueChange -> deptChange.incrementAndGet());
    final AtomicInteger deptEdit = new AtomicInteger();
    employeeEditModel.addValueEditListener(TestDomain.EMP_DEPARTMENT_FK, valueChange -> deptEdit.incrementAndGet());

    Entity dept = employeeEditModel.getConnectionProvider().getConnection().selectSingle(TestDomain.DEPARTMENT_ID, 10);
    employeeEditModel.put(TestDomain.EMP_DEPARTMENT_FK, dept);

    employeeEditModel.put(TestDomain.EMP_DEPARTMENT, 20);
    assertEquals(2, deptNoChange.get());
    assertEquals(2, deptChange.get());
    assertEquals(2, deptEdit.get());

    dept = employeeEditModel.getConnectionProvider().getConnection().selectSingle(TestDomain.DEPARTMENT_ID, 20);
    employeeEditModel.put(TestDomain.EMP_DEPARTMENT_FK, dept);

    assertEquals(2, deptNoChange.get());
    assertEquals(3, deptChange.get());
    assertEquals(3, deptEdit.get());

    employeeEditModel.put(TestDomain.EMP_DEPARTMENT, 30);

    assertEquals(3, deptNoChange.get());
    assertEquals(4, deptChange.get());
    assertEquals(4, deptEdit.get());

    dept = employeeEditModel.getConnectionProvider().getConnection().selectSingle(TestDomain.DEPARTMENT_ID, 30);
    employeeEditModel.put(TestDomain.EMP_DEPARTMENT_FK, dept);

    assertEquals(3, deptNoChange.get());
    assertEquals(5, deptChange.get());
    assertEquals(5, deptEdit.get());
  }

  private static final class TestEntityEditModel extends DefaultEntityEditModel {

    public TestEntityEditModel(final EntityType<?> entityType, final EntityConnectionProvider connectionProvider) {
      super(entityType, connectionProvider);
      setDefaultValueSupplier(TestDomain.EMP_HIREDATE, LocalDate::now);
    }

    @Override
    public void addForeignKeyValues(final List<Entity> entities) {}

    @Override
    public void removeForeignKeyValues(final List<Entity> entities) {}

    @Override
    public void clear() {}
  }
}
