/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.model;

import is.codion.common.db.database.Databases;
import is.codion.common.db.exception.DatabaseException;
import is.codion.common.db.exception.UpdateException;
import is.codion.common.event.EventDataListener;
import is.codion.common.event.EventListener;
import is.codion.common.model.CancelException;
import is.codion.common.state.State;
import is.codion.common.state.StateObserver;
import is.codion.common.user.User;
import is.codion.common.user.Users;
import is.codion.common.value.Value;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.local.LocalEntityConnectionProvider;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.Key;
import is.codion.framework.domain.entity.exception.ValidationException;
import is.codion.framework.domain.property.ColumnProperty;
import is.codion.framework.domain.property.ForeignKeyProperty;
import is.codion.framework.domain.property.Property;
import is.codion.framework.model.tests.TestDomain;

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
          Users.parseUser(System.getProperty("codion.test.user", "scott:tiger"));
  private static final Entities ENTITIES = new TestDomain().getEntities();
  private static final EntityConnectionProvider CONNECTION_PROVIDER = new LocalEntityConnectionProvider(
          Databases.getInstance()).setDomainClassName(TestDomain.class.getName()).setUser(UNIT_TEST_USER);

  private EntityEditModel employeeEditModel;
  private ColumnProperty jobProperty;
  private ForeignKeyProperty deptProperty;

  @BeforeEach
  public void setUp() {
    jobProperty = ENTITIES.getDefinition(TestDomain.T_EMP).getColumnProperty(TestDomain.EMP_JOB);
    deptProperty = ENTITIES.getDefinition(TestDomain.T_EMP).getForeignKeyProperty(TestDomain.EMP_DEPARTMENT_FK);
    employeeEditModel = new TestEntityEditModel(TestDomain.T_EMP, CONNECTION_PROVIDER);
  }

  @Test
  public void editEvents() throws DatabaseException, ValidationException {
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
    try {
      connection.beginTransaction();
      final Entity jones = connection.selectSingle(TestDomain.T_EMP, TestDomain.EMP_NAME, "JONES");
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
  public void getForeignKeyLookupModel() {
    assertFalse(employeeEditModel.containsLookupModel(TestDomain.EMP_DEPARTMENT_FK));
    final EntityLookupModel model = employeeEditModel.getForeignKeyLookupModel(TestDomain.EMP_DEPARTMENT_FK);
    assertTrue(employeeEditModel.containsLookupModel(TestDomain.EMP_DEPARTMENT_FK));
    assertNotNull(model);
    assertEquals(model, employeeEditModel.getForeignKeyLookupModel(TestDomain.EMP_DEPARTMENT_FK));
  }

  @Test
  public void createForeignKeyLookupModel() {
    final EntityLookupModel model = employeeEditModel.createForeignKeyLookupModel(TestDomain.EMP_DEPARTMENT_FK);
    assertNotNull(model);
    assertEquals(TestDomain.T_DEPARTMENT, model.getEntityType());
  }

  @Test
  public void refreshEntity() throws DatabaseException {
    final EntityConnection connection = employeeEditModel.getConnectionProvider().getConnection();
    try {
      connection.beginTransaction();
      final Entity employee = connection.selectSingle(TestDomain.T_EMP, TestDomain.EMP_NAME, "MARTIN");
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
  public void getEntityCopy() throws DatabaseException {
    final Entity employee = employeeEditModel.getConnectionProvider().getConnection().selectSingle(TestDomain.T_EMP,
            TestDomain.EMP_NAME, "MARTIN");
    employeeEditModel.setEntity(employee);
    final Entity copyWithPrimaryKeyValue = employeeEditModel.getEntityCopy();
    assertEquals(employee, copyWithPrimaryKeyValue);
    assertTrue(copyWithPrimaryKeyValue.getKey().isNotNull());
  }

  @Test
  public void constructorNullEntityType() {
    assertThrows(NullPointerException.class, () -> new TestEntityEditModel(null, CONNECTION_PROVIDER));
  }

  @Test
  public void constructorNullConnectionProvider() {
    assertThrows(NullPointerException.class, () -> new TestEntityEditModel(TestDomain.T_EMP, null));
  }

  @Test
  public void getDefaultForeignKeyValue() throws DatabaseException {
    final Entity employee = employeeEditModel.getConnectionProvider().getConnection().selectSingle(TestDomain.T_EMP,
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
    dept = employeeEditModel.getDefaultValue(TestDomain.EMP_DEPARTMENT_FK);
    assertNotNull(dept);
  }

  @Test
  public void test() throws Exception {
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
    employeeEditModel.addEntitiesChangedListener(listener);
    employeeEditModel.addBeforeRefreshListener(listener);
    employeeEditModel.addAfterRefreshListener(listener);

    assertEquals(TestDomain.T_EMP, employeeEditModel.getEntityType());

    employeeEditModel.refresh();
    assertTrue(employeeEditModel.isEntityNew());
    assertFalse(employeeEditModel.getModifiedObserver().get());

    final Entity employee = employeeEditModel.getConnectionProvider().getConnection().selectSingle(TestDomain.T_EMP, TestDomain.EMP_NAME, "MARTIN");
    employeeEditModel.setEntity(employee);
    assertFalse(primaryKeyNullState.get());
    assertFalse(entityNewState.get());

    assertTrue(employeeEditModel.getEntityCopy().valuesEqual(employee), "Active entity is not equal to the entity just set");
    assertFalse(employeeEditModel.isEntityNew(), "Active entity is new after an entity is set");
    assertFalse(employeeEditModel.getModifiedObserver().get());
    employeeEditModel.setEntity(null);
    assertTrue(employeeEditModel.isEntityNew(), "Active entity is new after entity is set to null");
    assertFalse(employeeEditModel.getModifiedObserver().get());
    assertTrue(employeeEditModel.getEntityCopy().getKey().isNull(), "Active entity primary key is not null after entity is set to null");

    employeeEditModel.setEntity(employee);
    assertTrue(employeeEditModel.getEntityCopy().getKey().isNotNull(), "Active entity primary key is null after entity is set");

    final Integer originalEmployeeId = employeeEditModel.get(TestDomain.EMP_ID);
    employeeEditModel.put(TestDomain.EMP_ID, null);
    assertTrue(primaryKeyNullState.get());
    employeeEditModel.put(TestDomain.EMP_ID, originalEmployeeId);
    assertFalse(primaryKeyNullState.get());

    employeeEditModel.setEntity(null);
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

    employeeEditModel.setEntity(null);
    assertTrue(employeeEditModel.getEntityCopy().getKey().isNull(), "Active entity is not null after model is cleared");

    employeeEditModel.removeAfterDeleteListener(eventDataListener);
    employeeEditModel.removeAfterInsertListener(eventDataListener);
    employeeEditModel.removeAfterUpdateListener(eventDataListener);
    employeeEditModel.removeBeforeDeleteListener(eventDataListener);
    employeeEditModel.removeBeforeInsertListener(eventDataListener);
    employeeEditModel.removeBeforeUpdateListener(eventDataListener);
    employeeEditModel.removeEntitiesChangedListener(listener);
    employeeEditModel.removeBeforeRefreshListener(listener);
    employeeEditModel.removeAfterRefreshListener(listener);
  }

  @Test
  public void insertReadOnly() throws CancelException, ValidationException, DatabaseException {
    employeeEditModel.setReadOnly(true);
    assertThrows(IllegalStateException.class, () -> employeeEditModel.insert());
  }

  @Test
  public void updateReadOnly() throws CancelException, ValidationException, DatabaseException {
    employeeEditModel.setReadOnly(true);
    assertThrows(IllegalStateException.class, () -> employeeEditModel.update());
  }

  @Test
  public void deleteReadOnly() throws CancelException, DatabaseException {
    employeeEditModel.setReadOnly(true);
    assertThrows(IllegalStateException.class, () -> employeeEditModel.delete());
  }

  @Test
  public void insert() throws Exception {
    try {
      assertTrue(employeeEditModel.insert(new ArrayList<>()).isEmpty());
      employeeEditModel.getConnectionProvider().getConnection().beginTransaction();
      employeeEditModel.put(TestDomain.EMP_COMMISSION, 1000d);
      employeeEditModel.put(TestDomain.EMP_HIREDATE, LocalDate.now());
      employeeEditModel.put(TestDomain.EMP_JOB, "CLERK");
      employeeEditModel.put(TestDomain.EMP_NAME, "Björn");
      employeeEditModel.put(TestDomain.EMP_SALARY, 1000d);

      final Entity tmpDept = ENTITIES.entity(TestDomain.T_DEPARTMENT);
      tmpDept.put(TestDomain.DEPARTMENT_ID, 99);
      tmpDept.put(TestDomain.DEPARTMENT_LOCATION, "Limbo");
      tmpDept.put(TestDomain.DEPARTMENT_NAME, "Judgment");

      final Entity department = employeeEditModel.getConnectionProvider().getConnection()
              .selectSingle(employeeEditModel.getConnectionProvider().getConnection().insert(tmpDept));

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
      assertTrue(entityCopy.getKey().isNotNull());
      assertEquals(entityCopy.getKey(), entityCopy.getOriginalKey());

      employeeEditModel.put(TestDomain.EMP_NAME, "Bobby");
      try {
        employeeEditModel.insert();
      }
      catch (final Exception e) {
        fail("Should be able to insert again");
      }
    }
    finally {
      employeeEditModel.getConnectionProvider().getConnection().rollbackTransaction();
    }
  }

  @Test
  public void update() throws Exception {
    try {
      assertThrows(UpdateException.class, () -> employeeEditModel.update());
      assertTrue(employeeEditModel.update(new ArrayList<>()).isEmpty());
      employeeEditModel.getConnectionProvider().getConnection().beginTransaction();
      employeeEditModel.setEntity(employeeEditModel.getConnectionProvider().getConnection().selectSingle(TestDomain.T_EMP, TestDomain.EMP_NAME, "MILLER"));
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
      employeeEditModel.getConnectionProvider().getConnection().rollbackTransaction();
    }
  }

  @Test
  public void delete() throws Exception {
    try {
      assertTrue(employeeEditModel.delete(new ArrayList<>()).isEmpty());
      employeeEditModel.getConnectionProvider().getConnection().beginTransaction();
      employeeEditModel.setEntity(employeeEditModel.getConnectionProvider().getConnection().selectSingle(TestDomain.T_EMP, TestDomain.EMP_NAME, "MILLER"));
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
      employeeEditModel.getConnectionProvider().getConnection().rollbackTransaction();
    }
  }

  @Test
  public void setEntity() throws Exception {
    final Entity martin = employeeEditModel.getConnectionProvider().getConnection().selectSingle(TestDomain.T_EMP, TestDomain.EMP_NAME, "MARTIN");
    final Entity king = employeeEditModel.getConnectionProvider().getConnection().selectSingle(TestDomain.T_EMP, TestDomain.EMP_NAME, "KING");
    employeeEditModel.setEntity(king);
    employeeEditModel.put(TestDomain.EMP_MGR_FK, martin);
    employeeEditModel.setEntity(null);
    king.put(TestDomain.EMP_MGR_FK, null);
    employeeEditModel.setEntity(king);
    assertNull(employeeEditModel.get(TestDomain.EMP_MGR_FK));
    employeeEditModel.setEntity(null);
    assertEquals(LocalDate.now(), employeeEditModel.get(TestDomain.EMP_HIREDATE));
    assertFalse(employeeEditModel.getEntityCopy().isModified(TestDomain.EMP_HIREDATE));
    assertFalse(employeeEditModel.getEntityCopy().isModified());
  }

  @Test
  public void setPersistValue() throws Exception {
    final Entity king = employeeEditModel.getConnectionProvider().getConnection().selectSingle(TestDomain.T_EMP, TestDomain.EMP_NAME, "KING");
    employeeEditModel.setEntity(king);
    assertNotNull(employeeEditModel.get(TestDomain.EMP_JOB));
    employeeEditModel.setPersistValue(TestDomain.EMP_JOB, true);
    employeeEditModel.setEntity(null);
    assertNotNull(employeeEditModel.get(TestDomain.EMP_JOB));
    employeeEditModel.setEntity(king);
    employeeEditModel.setPersistValue(TestDomain.EMP_JOB, false);
    employeeEditModel.setEntity(null);
    assertNull(employeeEditModel.get(TestDomain.EMP_JOB));
  }

  @Test
  public void containsUnsavedData() throws DatabaseException {
    employeeEditModel.setWarnAboutUnsavedData(true);
    employeeEditModel.setPersistValue(TestDomain.EMP_DEPARTMENT_FK, false);

    final EventDataListener<State> alwaysConfirmListener = data -> data.set(true);
    final EventDataListener<State> alwaysDenyListener = data -> data.set(false);

    employeeEditModel.addConfirmSetEntityObserver(alwaysConfirmListener);
    final Entity king = employeeEditModel.getConnectionProvider().getConnection().selectSingle(TestDomain.T_EMP, TestDomain.EMP_NAME, "KING");
    final Entity adams = employeeEditModel.getConnectionProvider().getConnection().selectSingle(TestDomain.T_EMP, TestDomain.EMP_NAME, "ADAMS");
    employeeEditModel.setEntity(king);
    employeeEditModel.put(TestDomain.EMP_NAME, "New name");
    employeeEditModel.setEntity(adams);
    assertEquals(adams, employeeEditModel.getEntityCopy());

    employeeEditModel.removeConfirmSetEntityObserver(alwaysConfirmListener);
    employeeEditModel.setEntity(null);
    employeeEditModel.addConfirmSetEntityObserver(alwaysDenyListener);

    employeeEditModel.put(TestDomain.EMP_NAME, "A name");
    employeeEditModel.setEntity(king);
    assertEquals("A name", employeeEditModel.get(TestDomain.EMP_NAME));

    employeeEditModel.removeConfirmSetEntityObserver(alwaysDenyListener);
    employeeEditModel.setEntity(null);
    employeeEditModel.addConfirmSetEntityObserver(alwaysDenyListener);

    employeeEditModel.put(TestDomain.EMP_DEPARTMENT_FK, king.get(TestDomain.EMP_DEPARTMENT_FK));
    employeeEditModel.setEntity(adams);
    assertEquals(king.get(TestDomain.EMP_DEPARTMENT_FK), employeeEditModel.get(TestDomain.EMP_DEPARTMENT_FK));

    employeeEditModel.setWarnAboutUnsavedData(false);
  }

  @Test
  public void replaceForeignKeyValues() throws DatabaseException {
    final Entity james = employeeEditModel.getConnectionProvider().getConnection()
            .selectSingle(TestDomain.T_EMP, TestDomain.EMP_NAME, "JAMES");
    employeeEditModel.setEntity(james);
    final Entity blake = employeeEditModel.getConnectionProvider().getConnection()
            .selectSingle(TestDomain.T_EMP, TestDomain.EMP_NAME, "BLAKE");
    assertNotSame(employeeEditModel.getForeignKey(TestDomain.EMP_MGR_FK), blake);
    employeeEditModel.replaceForeignKeyValues(singletonList(blake));
    assertSame(employeeEditModel.getForeignKey(TestDomain.EMP_MGR_FK), blake);
  }

  @Test
  public void value() {
    final Value<Integer> value = employeeEditModel.value(TestDomain.EMP_MGR);
    assertSame(value, employeeEditModel.value(TestDomain.EMP_MGR));
    value.set(42);
    assertEquals(42, employeeEditModel.get(TestDomain.EMP_MGR));
    employeeEditModel.put(TestDomain.EMP_MGR, 2);
    assertEquals(2, value.get());
  }

  private static final class TestEntityEditModel extends DefaultEntityEditModel {

    public TestEntityEditModel(final EntityType<?> entityType, final EntityConnectionProvider connectionProvider) {
      super(entityType, connectionProvider);
    }

    @Override
    public void addForeignKeyValues(final List<Entity> entities) {}

    @Override
    public void removeForeignKeyValues(final List<Entity> entities) {}

    @Override
    public void clear() {}

    @Override
    public <T> T getDefaultValue(final Attribute<T> attribute) {
      if (attribute.equals(TestDomain.EMP_HIREDATE)) {
        return (T) LocalDate.now();
      }

      return super.getDefaultValue(attribute);
    }
  }
}
