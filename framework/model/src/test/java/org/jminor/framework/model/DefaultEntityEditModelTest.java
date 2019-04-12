/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.model;

import org.jminor.common.EventDataListener;
import org.jminor.common.EventListener;
import org.jminor.common.State;
import org.jminor.common.StateObserver;
import org.jminor.common.User;
import org.jminor.common.db.Databases;
import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.db.valuemap.exception.ValidationException;
import org.jminor.common.model.CancelException;
import org.jminor.framework.db.EntityConnection;
import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.db.condition.EntityConditions;
import org.jminor.framework.db.local.LocalEntityConnectionProvider;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.Property;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public final class DefaultEntityEditModelTest {

  private static final Entities ENTITIES = new TestDomain();
  private static final EntityConnectionProvider CONNECTION_PROVIDER = new LocalEntityConnectionProvider(
          Databases.getInstance()).setDomainClassName(TestDomain.class.getName()).setUser(new User(
          System.getProperty("jminor.unittest.username", "scott"),
          System.getProperty("jminor.unittest.password", "tiger").toCharArray()));
  private static final EntityConditions ENTITY_CONDITIONS = CONNECTION_PROVIDER.getConditions();

  private EntityEditModel employeeEditModel;
  private Property.ColumnProperty jobProperty;
  private Property.ForeignKeyProperty deptProperty;

  @BeforeEach
  public void setUp() {
    jobProperty = ENTITIES.getColumnProperty(TestDomain.T_EMP, TestDomain.EMP_JOB);
    deptProperty = ENTITIES.getForeignKeyProperty(TestDomain.T_EMP, TestDomain.EMP_DEPARTMENT_FK);
    employeeEditModel = new TestEntityEditModel(TestDomain.T_EMP, CONNECTION_PROVIDER);
  }

  @Test
  public void getForeignKeyLookupModelNonFKProperty() {
    assertThrows(IllegalArgumentException.class, () -> employeeEditModel.getForeignKeyLookupModel(jobProperty.getPropertyId()));
  }

  @Test
  public void getForeignKeyLookupModel() {
    assertFalse(employeeEditModel.containsLookupModel(deptProperty.getPropertyId()));
    final EntityLookupModel model = employeeEditModel.getForeignKeyLookupModel(deptProperty.getPropertyId());
    assertTrue(employeeEditModel.containsLookupModel(deptProperty.getPropertyId()));
    assertNotNull(model);
    assertEquals(model, employeeEditModel.getForeignKeyLookupModel(deptProperty.getPropertyId()));
  }

  @Test
  public void createForeignKeyLookupModel() {
    final EntityLookupModel model = employeeEditModel.createForeignKeyLookupModel(ENTITIES.getForeignKeyProperty(TestDomain.T_EMP, TestDomain.EMP_DEPARTMENT_FK));
    assertNotNull(model);
    assertEquals(TestDomain.T_DEPARTMENT, model.getEntityId());
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
      connection.update(Collections.singletonList(employee));
      employeeEditModel.refreshEntity();
      assertEquals("NOONE", employeeEditModel.getValue(TestDomain.EMP_NAME));
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
    assertFalse(copyWithPrimaryKeyValue.isKeyNull());
    final Entity copyWithoutPrimaryKeyValue = employeeEditModel.getEntityCopy(false);
    assertTrue(copyWithoutPrimaryKeyValue.isKeyNull());
  }

  @Test
  public void constructorNullEntityId() {
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
    Entity dept = employeeEditModel.getForeignKeyValue(TestDomain.EMP_DEPARTMENT_FK);
    employeeEditModel.setValue(TestDomain.EMP_DEPARTMENT_FK, null);
    //set the reference key property value
    assertTrue(employeeEditModel.isValueNull(TestDomain.EMP_DEPARTMENT_FK));
    employeeEditModel.setValue(TestDomain.EMP_DEPARTMENT, dept.get(TestDomain.DEPARTMENT_ID));
    assertFalse(employeeEditModel.getEntityCopy().isLoaded(TestDomain.EMP_DEPARTMENT_FK));
    dept = employeeEditModel.getForeignKeyValue(TestDomain.EMP_DEPARTMENT_FK);
    assertNull(dept);
    dept = (Entity) employeeEditModel.getDefaultValue(
            ENTITIES.getProperty(TestDomain.T_EMP, TestDomain.EMP_DEPARTMENT_FK));
    assertNotNull(dept);
  }

  @Test
  public void test() throws Exception {
    final StateObserver primaryKeyNullState = employeeEditModel.getPrimaryKeyNullObserver();
    final StateObserver entityNewState = employeeEditModel.getEntityNewObserver();

    assertTrue(primaryKeyNullState.isActive());
    assertTrue(entityNewState.isActive());

    employeeEditModel.setReadOnly(false);
    assertFalse(employeeEditModel.isReadOnly());
    assertTrue(employeeEditModel.getAllowInsertObserver().isActive());
    assertTrue(employeeEditModel.getAllowUpdateObserver().isActive());
    assertTrue(employeeEditModel.getAllowDeleteObserver().isActive());

    employeeEditModel.setReadOnly(true);
    assertTrue(employeeEditModel.isReadOnly());
    assertFalse(employeeEditModel.getAllowInsertObserver().isActive());
    assertFalse(employeeEditModel.getAllowUpdateObserver().isActive());
    assertFalse(employeeEditModel.getAllowDeleteObserver().isActive());

    employeeEditModel.setDeleteAllowed(true);
    assertFalse(employeeEditModel.isReadOnly());

    employeeEditModel.setDeleteAllowed(false);
    assertTrue(employeeEditModel.isReadOnly());

    employeeEditModel.setUpdateAllowed(true);
    assertFalse(employeeEditModel.isReadOnly());

    employeeEditModel.setUpdateAllowed(false);
    assertTrue(employeeEditModel.isReadOnly());

    employeeEditModel.setReadOnly(false);
    assertTrue(employeeEditModel.getAllowInsertObserver().isActive());
    assertTrue(employeeEditModel.getAllowUpdateObserver().isActive());
    assertTrue(employeeEditModel.getAllowDeleteObserver().isActive());

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

    assertEquals(TestDomain.T_EMP, employeeEditModel.getEntityId());
    assertEquals(employeeEditModel.getConnectionProvider().getConnection().selectValues(TestDomain.EMP_JOB,
            ENTITY_CONDITIONS.condition(TestDomain.T_EMP)),
            employeeEditModel.getValueProvider(jobProperty).values());

    employeeEditModel.refresh();
    assertTrue(employeeEditModel.isEntityNew());
    assertFalse(employeeEditModel.getModifiedObserver().isActive());

    final Entity employee = employeeEditModel.getConnectionProvider().getConnection().selectSingle(TestDomain.T_EMP, TestDomain.EMP_NAME, "MARTIN");
    employeeEditModel.setEntity(employee);
    assertFalse(primaryKeyNullState.isActive());
    assertFalse(entityNewState.isActive());

    assertTrue(employeeEditModel.getEntityCopy().valuesEqual(employee), "Active entity is not equal to the entity just set");
    assertFalse(employeeEditModel.isEntityNew(), "Active entity is new after an entity is set");
    assertFalse(employeeEditModel.getModifiedObserver().isActive());
    employeeEditModel.setEntity(null);
    assertTrue(employeeEditModel.isEntityNew(), "Active entity is new after entity is set to null");
    assertFalse(employeeEditModel.getModifiedObserver().isActive());
    assertTrue(employeeEditModel.getEntityCopy().isKeyNull(), "Active entity primary key is not null after entity is set to null");

    employeeEditModel.setEntity(employee);
    assertFalse(employeeEditModel.getEntityCopy().isKeyNull(), "Active entity primary key is null after entity is set");

    final Integer originalEmployeeId = (Integer) employeeEditModel.getValue(TestDomain.EMP_ID);
    employeeEditModel.setValue(TestDomain.EMP_ID, null);
    assertTrue(primaryKeyNullState.isActive());
    employeeEditModel.setValue(TestDomain.EMP_ID, originalEmployeeId);
    assertFalse(primaryKeyNullState.isActive());

    employeeEditModel.setEntity(null);
    assertTrue(entityNewState.isActive());

    final Double originalCommission = (Double) employeeEditModel.getValue(TestDomain.EMP_COMMISSION);
    final double commission = 1500.5;
    final LocalDate originalHiredate = (LocalDate) employeeEditModel.getValue(TestDomain.EMP_HIREDATE);
    final LocalDate hiredate = LocalDate.now();
    final String originalName = (String) employeeEditModel.getValue(TestDomain.EMP_NAME);
    final String name = "Mr. Mr";

    employeeEditModel.setValue(TestDomain.EMP_COMMISSION, commission);
    assertTrue(employeeEditModel.getModifiedObserver().isActive());
    employeeEditModel.setValue(TestDomain.EMP_HIREDATE, hiredate);
    employeeEditModel.setValue(TestDomain.EMP_NAME, name);

    assertEquals(employeeEditModel.getValue(TestDomain.EMP_COMMISSION), commission, "Commission does not fit");
    assertEquals(employeeEditModel.getValue(TestDomain.EMP_HIREDATE), hiredate, "Hiredate does not fit");
    assertEquals(employeeEditModel.getValue(TestDomain.EMP_NAME), name, "Name does not fit");

    employeeEditModel.setValue(TestDomain.EMP_COMMISSION, originalCommission);
    assertTrue(employeeEditModel.isModified());
    assertTrue(employeeEditModel.getModifiedObserver().isActive());
    employeeEditModel.setValue(TestDomain.EMP_HIREDATE, originalHiredate);
    assertTrue(employeeEditModel.isModified());
    employeeEditModel.setValue(TestDomain.EMP_NAME, originalName);
    assertFalse(employeeEditModel.isModified());

    employeeEditModel.setValue(TestDomain.EMP_COMMISSION, 50d);
    assertNotNull(employeeEditModel.removeValue(TestDomain.EMP_COMMISSION));
    assertNull(employeeEditModel.getValue(TestDomain.EMP_COMMISSION));

    //test validation
    try {
      employeeEditModel.setValue(TestDomain.EMP_COMMISSION, 50d);
      employeeEditModel.validate(ENTITIES.getProperty(TestDomain.T_EMP, TestDomain.EMP_COMMISSION));
      fail("Validation should fail on invalid commission value");
    }
    catch (final ValidationException e) {
      assertEquals(TestDomain.EMP_COMMISSION, e.getKey());
      assertEquals(50d, e.getValue());
      final Property property = ENTITIES.getProperty(TestDomain.T_EMP, (String) e.getKey());
      assertTrue(e.getMessage().contains(property.toString()));
      assertTrue(e.getMessage().contains(property.getMin().toString()));
    }

    employeeEditModel.setEntity(null);
    assertTrue(employeeEditModel.getEntityCopy().isKeyNull(), "Active entity is not null after model is cleared");

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
      employeeEditModel.setValue(TestDomain.EMP_COMMISSION, 1000d);
      employeeEditModel.setValue(TestDomain.EMP_HIREDATE, LocalDate.now());
      employeeEditModel.setValue(TestDomain.EMP_JOB, "CLERK");
      employeeEditModel.setValue(TestDomain.EMP_NAME, "Björn");
      employeeEditModel.setValue(TestDomain.EMP_SALARY, 1000d);

      final Entity tmpDept = ENTITIES.entity(TestDomain.T_DEPARTMENT);
      tmpDept.put(TestDomain.DEPARTMENT_ID, 99);
      tmpDept.put(TestDomain.DEPARTMENT_LOCATION, "Limbo");
      tmpDept.put(TestDomain.DEPARTMENT_NAME, "Judgment");

      final Entity department = employeeEditModel.getConnectionProvider().getConnection().selectSingle(employeeEditModel.getConnectionProvider().getConnection().insert(Collections.singletonList(tmpDept)).get(0));

      employeeEditModel.setValue(TestDomain.EMP_DEPARTMENT_FK, department);

      employeeEditModel.addAfterInsertListener(data ->
              assertEquals(department, data.getInsertedEntities().get(0).get(TestDomain.EMP_DEPARTMENT_FK)));
      employeeEditModel.setInsertAllowed(false);
      assertFalse(employeeEditModel.isInsertAllowed());
      assertThrows(IllegalStateException.class, () -> employeeEditModel.insert());
      employeeEditModel.setInsertAllowed(true);
      assertTrue(employeeEditModel.isInsertAllowed());

      employeeEditModel.insert();
      assertFalse(employeeEditModel.isEntityNew());
      final Entity entityCopy = employeeEditModel.getEntityCopy();
      assertFalse(entityCopy.getKey().isNull());
      assertEquals(entityCopy.getKey(), entityCopy.getOriginalKey());

      employeeEditModel.setValue(TestDomain.EMP_NAME, "Bobby");
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
      assertTrue(employeeEditModel.update().isEmpty());
      assertTrue(employeeEditModel.update(new ArrayList<>()).isEmpty());
      employeeEditModel.getConnectionProvider().getConnection().beginTransaction();
      employeeEditModel.setEntity(employeeEditModel.getConnectionProvider().getConnection().selectSingle(TestDomain.T_EMP, TestDomain.EMP_NAME, "MILLER"));
      employeeEditModel.setValue(TestDomain.EMP_NAME, "BJORN");
      final List<Entity> toUpdate = Collections.singletonList(employeeEditModel.getEntityCopy());
      final EventDataListener<EntityEditModel.UpdateEvent> listener = data ->
              assertEquals(toUpdate, new ArrayList<>(data.getUpdatedEntities().values()));
      employeeEditModel.addAfterUpdateListener(listener);
      employeeEditModel.setUpdateAllowed(false);
      assertFalse(employeeEditModel.isUpdateAllowed());
      assertThrows(IllegalStateException.class, () -> employeeEditModel.update());
      employeeEditModel.setUpdateAllowed(true);
      assertTrue(employeeEditModel.isUpdateAllowed());

      employeeEditModel.update();
      assertFalse(employeeEditModel.getModifiedObserver().isActive());
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
      final List<Entity> toDelete = Collections.singletonList(employeeEditModel.getEntityCopy());
      employeeEditModel.addAfterDeleteListener(data -> assertEquals(toDelete, data.getDeletedEntities()));
      employeeEditModel.setDeleteAllowed(false);
      assertFalse(employeeEditModel.isDeleteAllowed());
      assertThrows(IllegalStateException.class, () -> employeeEditModel.delete());
      employeeEditModel.setDeleteAllowed(true);
      assertTrue(employeeEditModel.isDeleteAllowed());

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
    employeeEditModel.setValue(TestDomain.EMP_MGR_FK, martin);
    employeeEditModel.setEntity(null);
    king.put(TestDomain.EMP_MGR_FK, null);
    employeeEditModel.setEntity(king);
    assertNull(employeeEditModel.getValue(TestDomain.EMP_MGR_FK));
    employeeEditModel.setEntity(null);
    assertEquals(LocalDate.now(), employeeEditModel.getValue(TestDomain.EMP_HIREDATE));
    assertFalse(employeeEditModel.getEntityCopy().isModified(TestDomain.EMP_HIREDATE));
    assertFalse(employeeEditModel.getEntityCopy().isModified());
  }

  @Test
  public void setValuePersistent() throws Exception {
    final Entity king = employeeEditModel.getConnectionProvider().getConnection().selectSingle(TestDomain.T_EMP, TestDomain.EMP_NAME, "KING");
    employeeEditModel.setEntity(king);
    assertNotNull(employeeEditModel.getValue(TestDomain.EMP_JOB));
    employeeEditModel.setValuePersistent(TestDomain.EMP_JOB, true);
    employeeEditModel.setEntity(null);
    assertNotNull(employeeEditModel.getValue(TestDomain.EMP_JOB));
    employeeEditModel.setEntity(king);
    employeeEditModel.setValuePersistent(TestDomain.EMP_JOB, false);
    employeeEditModel.setEntity(null);
    assertNull(employeeEditModel.getValue(TestDomain.EMP_JOB));
  }

  @Test
  public void containsUnsavedData() throws DatabaseException {
    employeeEditModel.setWarnAboutUnsavedData(true);
    employeeEditModel.setValuePersistent(TestDomain.EMP_DEPARTMENT_FK, false);

    final EventDataListener<State> alwaysConfirmListener = data -> data.setActive(true);
    final EventDataListener<State> alwaysDenyListener = data -> data.setActive(false);

    employeeEditModel.addConfirmSetEntityObserver(alwaysConfirmListener);
    final Entity king = employeeEditModel.getConnectionProvider().getConnection().selectSingle(TestDomain.T_EMP, TestDomain.EMP_NAME, "KING");
    final Entity adams = employeeEditModel.getConnectionProvider().getConnection().selectSingle(TestDomain.T_EMP, TestDomain.EMP_NAME, "ADAMS");
    employeeEditModel.setEntity(king);
    employeeEditModel.setValue(TestDomain.EMP_NAME, "New name");
    employeeEditModel.setEntity(adams);
    assertEquals(adams, employeeEditModel.getEntityCopy());

    employeeEditModel.removeConfirmSetEntityObserver(alwaysConfirmListener);
    employeeEditModel.setEntity(null);
    employeeEditModel.addConfirmSetEntityObserver(alwaysDenyListener);

    employeeEditModel.setValue(TestDomain.EMP_NAME, "A name");
    employeeEditModel.setEntity(king);
    assertEquals("A name", employeeEditModel.getValue(TestDomain.EMP_NAME));

    employeeEditModel.removeConfirmSetEntityObserver(alwaysDenyListener);
    employeeEditModel.setEntity(null);
    employeeEditModel.addConfirmSetEntityObserver(alwaysDenyListener);

    employeeEditModel.setValue(TestDomain.EMP_DEPARTMENT_FK, king.get(TestDomain.EMP_DEPARTMENT_FK));
    employeeEditModel.setEntity(adams);
    assertEquals(king.get(TestDomain.EMP_DEPARTMENT_FK), employeeEditModel.getValue(TestDomain.EMP_DEPARTMENT_FK));

    employeeEditModel.setWarnAboutUnsavedData(false);
  }

  private static final class TestEntityEditModel extends DefaultEntityEditModel {

    public TestEntityEditModel(final String entityId, final EntityConnectionProvider connectionProvider) {
      super(entityId, connectionProvider);
    }

    @Override
    public void addForeignKeyValues(final List<Entity> values) {}

    @Override
    public void removeForeignKeyValues(final List<Entity> values) {}

    @Override
    public void clear() {}

    @Override
    public Object getDefaultValue(final Property property) {
      if (property.is(TestDomain.EMP_HIREDATE)) {
        return LocalDate.now();
      }

      return super.getDefaultValue(property);
    }
  }
}
