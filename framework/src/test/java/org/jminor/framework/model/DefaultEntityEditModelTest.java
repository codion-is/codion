/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.model;

import org.jminor.common.DateUtil;
import org.jminor.common.EventInfoListener;
import org.jminor.common.EventListener;
import org.jminor.common.State;
import org.jminor.common.StateObserver;
import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.model.CancelException;
import org.jminor.common.model.valuemap.exception.ValidationException;
import org.jminor.framework.Configuration;
import org.jminor.framework.db.EntityConnection;
import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.db.EntityConnectionProvidersTest;
import org.jminor.framework.db.condition.EntityConditions;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.Property;
import org.jminor.framework.domain.TestDomain;
import org.jminor.framework.i18n.FrameworkMessages;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.*;

public final class DefaultEntityEditModelTest {

  private EntityEditModel employeeEditModel;
  private Property.ColumnProperty jobProperty;
  private Property.ForeignKeyProperty deptProperty;

  @Before
  public void setUp() {
    TestDomain.init();
    jobProperty = Entities.getColumnProperty(TestDomain.T_EMP, TestDomain.EMP_JOB);
    deptProperty = Entities.getForeignKeyProperty(TestDomain.T_EMP, TestDomain.EMP_DEPARTMENT_FK);
    employeeEditModel = new TestEntityEditModel(TestDomain.T_EMP, EntityConnectionProvidersTest.CONNECTION_PROVIDER);
  }

  @Test(expected = IllegalArgumentException.class)
  public void getForeignKeyLookupModelNonFKProperty() {
    employeeEditModel.getForeignKeyLookupModel(jobProperty.getPropertyID());
  }

  @Test
  public void getForeignKeyLookupModel() {
    assertFalse(employeeEditModel.containsLookupModel(deptProperty.getPropertyID()));
    final EntityLookupModel model = employeeEditModel.getForeignKeyLookupModel(deptProperty.getPropertyID());
    assertTrue(employeeEditModel.containsLookupModel(deptProperty.getPropertyID()));
    assertNotNull(model);
    assertEquals(model, employeeEditModel.getForeignKeyLookupModel(deptProperty.getPropertyID()));
  }

  @Test
  public void createForeignKeyLookupModel() {
    final EntityLookupModel model = employeeEditModel.createForeignKeyLookupModel(Entities.getForeignKeyProperty(TestDomain.T_EMP, TestDomain.EMP_DEPARTMENT_FK));
    assertNotNull(model);
    assertEquals(TestDomain.T_DEPARTMENT, model.getEntityID());
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

  @Test(expected = IllegalArgumentException.class)
  public void constructorNullEntityID() {
    new TestEntityEditModel(null, EntityConnectionProvidersTest.CONNECTION_PROVIDER);
  }

  @Test(expected = NullPointerException.class)
  public void constructorNullConnectionProvider() {
    new TestEntityEditModel("entityID", null);
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
            Entities.getProperty(TestDomain.T_EMP, TestDomain.EMP_DEPARTMENT_FK));
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

    final EventInfoListener infoListener = info -> {};
    employeeEditModel.addAfterDeleteListener(infoListener);
    employeeEditModel.addAfterInsertListener(infoListener);
    employeeEditModel.addAfterUpdateListener(infoListener);
    employeeEditModel.addBeforeDeleteListener(infoListener);
    employeeEditModel.addBeforeInsertListener(infoListener);
    employeeEditModel.addBeforeUpdateListener(infoListener);
    final EventListener listener = () -> {};
    employeeEditModel.addEntitiesChangedListener(listener);
    employeeEditModel.addBeforeRefreshListener(listener);
    employeeEditModel.addAfterRefreshListener(listener);

    assertEquals(TestDomain.T_EMP, employeeEditModel.getEntityID());
    assertEquals(employeeEditModel.getConnectionProvider().getConnection().selectValues(TestDomain.EMP_JOB, EntityConditions.condition(TestDomain.T_EMP)),
            employeeEditModel.getValueProvider(jobProperty).values());

    employeeEditModel.refresh();
    assertTrue(employeeEditModel.isEntityNew());
    assertFalse(employeeEditModel.getModifiedObserver().isActive());

    final Entity employee = employeeEditModel.getConnectionProvider().getConnection().selectSingle(TestDomain.T_EMP, TestDomain.EMP_NAME, "MARTIN");
    employeeEditModel.setEntity(employee);
    assertFalse(primaryKeyNullState.isActive());
    assertFalse(entityNewState.isActive());

    assertTrue("Active entity is not equal to the entity just set", employeeEditModel.getEntityCopy().valuesEqual(employee));
    assertFalse("Active entity is new after an entity is set", employeeEditModel.isEntityNew());
    assertFalse(employeeEditModel.getModifiedObserver().isActive());
    employeeEditModel.setEntity(null);
    assertTrue("Active entity is new after entity is set to null", employeeEditModel.isEntityNew());
    assertFalse(employeeEditModel.getModifiedObserver().isActive());
    assertTrue("Active entity primary key is not null after entity is set to null", employeeEditModel.getEntityCopy().isKeyNull());

    employeeEditModel.setEntity(employee);
    assertTrue("Active entity primary key is null after entity is set", !employeeEditModel.getEntityCopy().isKeyNull());

    final Integer originalEmployeeId = (Integer) employeeEditModel.getValue(TestDomain.EMP_ID);
    employeeEditModel.setValue(TestDomain.EMP_ID, null);
    assertTrue(primaryKeyNullState.isActive());
    employeeEditModel.setValue(TestDomain.EMP_ID, originalEmployeeId);
    assertFalse(primaryKeyNullState.isActive());

    employeeEditModel.setEntity(null);
    assertTrue(entityNewState.isActive());

    final Double originalCommission = (Double) employeeEditModel.getValue(TestDomain.EMP_COMMISSION);
    final double commission = 1500.5;
    final Date originalHiredate = (Date) employeeEditModel.getValue(TestDomain.EMP_HIREDATE);
    final Date hiredate = new Date();
    final String originalName = (String) employeeEditModel.getValue(TestDomain.EMP_NAME);
    final String name = "Mr. Mr";

    employeeEditModel.setValue(TestDomain.EMP_COMMISSION, commission);
    assertTrue(employeeEditModel.getModifiedObserver().isActive());
    employeeEditModel.setValue(TestDomain.EMP_HIREDATE, hiredate);
    employeeEditModel.setValue(TestDomain.EMP_NAME, name);

    assertEquals("Commission does not fit", employeeEditModel.getValue(TestDomain.EMP_COMMISSION), commission);
    assertEquals("Hiredate does not fit", employeeEditModel.getValue(TestDomain.EMP_HIREDATE), hiredate);
    assertEquals("Name does not fit", employeeEditModel.getValue(TestDomain.EMP_NAME), name);

    employeeEditModel.setValue(TestDomain.EMP_COMMISSION, originalCommission);
    assertTrue(employeeEditModel.isModified());
    assertTrue(employeeEditModel.getModifiedObserver().isActive());
    employeeEditModel.setValue(TestDomain.EMP_HIREDATE, originalHiredate);
    assertTrue(employeeEditModel.isModified());
    employeeEditModel.setValue(TestDomain.EMP_NAME, originalName);
    assertFalse(employeeEditModel.isModified());

    //test validation
    try {
      employeeEditModel.setValue(TestDomain.EMP_COMMISSION, 50d);
      employeeEditModel.validate(TestDomain.EMP_COMMISSION);
      fail("Validation should fail on invalid commission value");
    }
    catch (final ValidationException e) {
      assertEquals(TestDomain.EMP_COMMISSION, e.getKey());
      assertEquals(50d, e.getValue());
      final Property property = Entities.getProperty(TestDomain.T_EMP, (String) e.getKey());
      assertEquals("Validation message should fit", "'" + property + "' " +
              FrameworkMessages.get(FrameworkMessages.PROPERTY_VALUE_TOO_SMALL) + " " + property.getMin(), e.getMessage());
    }

    employeeEditModel.setEntity(null);
    assertTrue("Active entity is not null after model is cleared", employeeEditModel.getEntityCopy().isKeyNull());

    employeeEditModel.removeAfterDeleteListener(infoListener);
    employeeEditModel.removeAfterInsertListener(infoListener);
    employeeEditModel.removeAfterUpdateListener(infoListener);
    employeeEditModel.removeBeforeDeleteListener(infoListener);
    employeeEditModel.removeBeforeInsertListener(infoListener);
    employeeEditModel.removeBeforeUpdateListener(infoListener);
    employeeEditModel.removeEntitiesChangedListener(listener);
    employeeEditModel.removeBeforeRefreshListener(listener);
    employeeEditModel.removeAfterRefreshListener(listener);
  }

  @Test(expected = UnsupportedOperationException.class)
  public void insertReadOnly() throws CancelException, ValidationException, DatabaseException {
    employeeEditModel.setReadOnly(true);
    employeeEditModel.insert();
  }

  @Test(expected = UnsupportedOperationException.class)
  public void updateReadOnly() throws CancelException, ValidationException, DatabaseException {
    employeeEditModel.setReadOnly(true);
    employeeEditModel.update();
  }

  @Test(expected = UnsupportedOperationException.class)
  public void deleteReadOnly() throws CancelException, DatabaseException {
    employeeEditModel.setReadOnly(true);
    employeeEditModel.delete();
  }

  @Test
  public void insert() throws Exception {
    try {
      assertTrue(employeeEditModel.insert(new ArrayList<>()).isEmpty());
      employeeEditModel.getConnectionProvider().getConnection().beginTransaction();
      employeeEditModel.setValue(TestDomain.EMP_COMMISSION, 1000d);
      employeeEditModel.setValue(TestDomain.EMP_HIREDATE, DateUtil.floorDate(new Date()));
      employeeEditModel.setValue(TestDomain.EMP_JOB, "CLERK");
      employeeEditModel.setValue(TestDomain.EMP_NAME, "Björn");
      employeeEditModel.setValue(TestDomain.EMP_SALARY, 1000d);

      final Entity tmpDept = Entities.entity(TestDomain.T_DEPARTMENT);
      tmpDept.put(TestDomain.DEPARTMENT_ID, 99);
      tmpDept.put(TestDomain.DEPARTMENT_LOCATION, "Limbo");
      tmpDept.put(TestDomain.DEPARTMENT_NAME, "Judgment");

      final Entity department = employeeEditModel.getConnectionProvider().getConnection().selectSingle(employeeEditModel.getConnectionProvider().getConnection().insert(Collections.singletonList(tmpDept)).get(0));

      employeeEditModel.setValue(TestDomain.EMP_DEPARTMENT_FK, department);

      employeeEditModel.addAfterInsertListener(info ->
              assertEquals(department, info.getInsertedEntities().get(0).get(TestDomain.EMP_DEPARTMENT_FK)));
      employeeEditModel.setInsertAllowed(false);
      assertFalse(employeeEditModel.isInsertAllowed());
      try {
        employeeEditModel.insert();
        fail("Should not be able to insert");
      }
      catch (final UnsupportedOperationException ignored) {/*ignored*/}
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
      final EventInfoListener<EntityEditModel.UpdateEvent> listener = info ->
              assertEquals(toUpdate, new ArrayList<>(info.getUpdatedEntities().values()));
      employeeEditModel.addAfterUpdateListener(listener);
      employeeEditModel.setUpdateAllowed(false);
      assertFalse(employeeEditModel.isUpdateAllowed());
      try {
        employeeEditModel.update();
        fail("Should not be able to update");
      }
      catch (final UnsupportedOperationException ignored) {/*ignored*/}
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
      employeeEditModel.addAfterDeleteListener(info -> assertEquals(toDelete, info.getDeletedEntities()));
      employeeEditModel.setDeleteAllowed(false);
      assertFalse(employeeEditModel.isDeleteAllowed());
      try {
        employeeEditModel.delete();
        fail("Should not be able to delete");
      }
      catch (final UnsupportedOperationException ignored) {/*ignored*/}
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
    assertEquals(DateUtil.floorDate(new Date()), employeeEditModel.getValue(TestDomain.EMP_HIREDATE));
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
    Configuration.setValue(Configuration.WARN_ABOUT_UNSAVED_DATA, true);
    employeeEditModel.setValuePersistent(TestDomain.EMP_DEPARTMENT_FK, false);

    final EventInfoListener<State> alwaysConfirmListener = info -> info.setActive(true);
    final EventInfoListener<State> alwaysDenyListener = info -> info.setActive(false);

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

    Configuration.setValue(Configuration.WARN_ABOUT_UNSAVED_DATA, false);
  }

  private static final class TestEntityEditModel extends DefaultEntityEditModel {

    public TestEntityEditModel(final String entityID, final EntityConnectionProvider connectionProvider) {
      super(entityID, connectionProvider);
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
        return DateUtil.floorDate(new Date());
      }

      return super.getDefaultValue(property);
    }
  }
}
