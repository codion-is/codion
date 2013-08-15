/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model;

import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.model.CancelException;
import org.jminor.common.model.DateUtil;
import org.jminor.common.model.EventAdapter;
import org.jminor.common.model.EventListener;
import org.jminor.common.model.StateObserver;
import org.jminor.common.model.combobox.FilteredComboBoxModel;
import org.jminor.common.model.valuemap.ValueChangeEvent;
import org.jminor.common.model.valuemap.ValueChangeListener;
import org.jminor.common.model.valuemap.exception.ValidationException;
import org.jminor.framework.Configuration;
import org.jminor.framework.db.DefaultEntityConnectionTest;
import org.jminor.framework.demos.empdept.domain.EmpDept;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.Property;
import org.jminor.framework.i18n.FrameworkMessages;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.*;

public final class DefaultEntityEditModelTest {

  private DefaultEntityEditModel employeeEditModel;
  private Property.ColumnProperty jobProperty;
  private Property.ForeignKeyProperty deptProperty;
  private boolean debugOutput;

  @Before
  public void setUp() {
    EmpDept.init();
    jobProperty = Entities.getColumnProperty(EmpDept.T_EMPLOYEE, EmpDept.EMPLOYEE_JOB);
    deptProperty = Entities.getForeignKeyProperty(EmpDept.T_EMPLOYEE, EmpDept.EMPLOYEE_DEPARTMENT_FK);
    debugOutput = Configuration.getBooleanValue(Configuration.PROPERTY_DEBUG_OUTPUT);
    Configuration.setValue(Configuration.PROPERTY_DEBUG_OUTPUT, true);
    employeeEditModel = new DefaultEntityEditModel(EmpDept.T_EMPLOYEE, DefaultEntityConnectionTest.CONNECTION_PROVIDER) {
      @Override
      public Object getDefaultValue(final Property property) {
        if (property.is(EmpDept.EMPLOYEE_HIREDATE)) {
          return DateUtil.floorDate(new Date());
        }

        return super.getDefaultValue(property);
      }
    };
  }

  @After
  public void tearDown() {
    Configuration.setValue(Configuration.PROPERTY_DEBUG_OUTPUT, debugOutput);
  }

  @Test
  public void getPropertyComboBoxModel() {
    final FilteredComboBoxModel model = employeeEditModel.getPropertyComboBoxModel(jobProperty);
    model.setNullValueString("null");
    assertNotNull(model);
    assertTrue(employeeEditModel.containsComboBoxModel(jobProperty.getPropertyID()));
    assertEquals(model, employeeEditModel.getPropertyComboBoxModel(jobProperty));
    employeeEditModel.refreshComboBoxModels();
    employeeEditModel.clearComboBoxModels();
    assertTrue(employeeEditModel.getPropertyComboBoxModel(jobProperty).isCleared());
    employeeEditModel.refreshComboBoxModels();
    employeeEditModel.clear();
    assertTrue(employeeEditModel.getPropertyComboBoxModel(jobProperty).isCleared());
  }

  @Test
  public void getEntityComboBoxModel() {
    assertFalse(employeeEditModel.containsComboBoxModel(deptProperty.getPropertyID()));
    final EntityComboBoxModel model = employeeEditModel.getEntityComboBoxModel(deptProperty);
    assertTrue(employeeEditModel.containsComboBoxModel(deptProperty.getPropertyID()));
    assertNotNull(model);
    assertTrue(model.isCleared());
    assertTrue(model.getAllItems().isEmpty());
    employeeEditModel.refreshComboBoxModels();
    assertFalse(model.isCleared());
    assertFalse(model.getAllItems().isEmpty());
    employeeEditModel.clearComboBoxModels();
    assertTrue(model.isCleared());
    assertTrue(model.getAllItems().isEmpty());
  }

  @Test
  public void createEntityComboBoxModel() {
    final EntityComboBoxModel model = employeeEditModel.createEntityComboBoxModel(deptProperty);
    assertNotNull(model);
    assertTrue(model.isCleared());
    assertTrue(model.getAllItems().isEmpty());
    assertEquals(deptProperty.getReferencedEntityID(), model.getEntityID());
  }

  @Test(expected = IllegalArgumentException.class)
  public void getEntityComboBoxModelNonFKProperty() {
    employeeEditModel.getEntityComboBoxModel(jobProperty.getPropertyID());
  }

  @Test(expected = IllegalArgumentException.class)
  public void getEntityLookupModelNonFKProperty() {
    employeeEditModel.getEntityLookupModel(jobProperty.getPropertyID());
  }

  @Test
  public void getEntityLookupModel() {
    assertFalse(employeeEditModel.containsLookupModel(deptProperty.getPropertyID()));
    final EntityLookupModel model = employeeEditModel.getEntityLookupModel(deptProperty.getPropertyID());
    assertTrue(employeeEditModel.containsLookupModel(deptProperty.getPropertyID()));
    assertNotNull(model);
    assertEquals(model, employeeEditModel.getEntityLookupModel(deptProperty.getPropertyID()));
  }

  @Test
  public void createEntityLookupModel() {
    final EntityLookupModel model = employeeEditModel.createEntityLookupModel(Entities.getForeignKeyProperty(EmpDept.T_EMPLOYEE, EmpDept.EMPLOYEE_DEPARTMENT_FK));
    assertNotNull(model);
    assertEquals(EmpDept.T_DEPARTMENT, model.getEntityID());
  }

  @Test
  public void getEntityCopy() throws DatabaseException {
    final Entity employee = employeeEditModel.getConnectionProvider().getConnection().selectSingle(EmpDept.T_EMPLOYEE, EmpDept.EMPLOYEE_NAME, "MARTIN");
    employeeEditModel.setEntity(employee);
    final Entity copyWithPrimaryKeyValue = employeeEditModel.getEntityCopy();
    assertEquals(employee, copyWithPrimaryKeyValue);
    assertFalse(copyWithPrimaryKeyValue.isPrimaryKeyNull());
    final Entity copyWithoutPrimaryKeyValue = employeeEditModel.getEntityCopy(false);
    assertTrue(copyWithoutPrimaryKeyValue.isPrimaryKeyNull());
  }

  @Test(expected = IllegalArgumentException.class)
  public void constructorNullEntityID() {
    new DefaultEntityEditModel(null, DefaultEntityConnectionTest.CONNECTION_PROVIDER);
  }

  @Test(expected = IllegalArgumentException.class)
  public void constructorNullConnectionProvider() {
    new DefaultEntityEditModel("entityID", null);
  }

  @Test
  public void test() throws Exception {
    final StateObserver primaryKeyNullState = employeeEditModel.getPrimaryKeyNullObserver();

    assertTrue(primaryKeyNullState.isActive());

    employeeEditModel.setReadOnly(false);
    assertFalse(employeeEditModel.isReadOnly());
    assertTrue(employeeEditModel.getAllowInsertObserver().isActive());
    assertTrue(employeeEditModel.getAllowUpdateObserver().isActive());
    assertTrue(employeeEditModel.getAllowDeleteObserver().isActive());

    final EventListener listener = new EventAdapter() {
      @Override
      public void eventOccurred() {}
    };
    employeeEditModel.addAfterDeleteListener(listener);
    employeeEditModel.addAfterInsertListener(listener);
    employeeEditModel.addAfterUpdateListener(listener);
    employeeEditModel.addBeforeDeleteListener(listener);
    employeeEditModel.addBeforeInsertListener(listener);
    employeeEditModel.addBeforeUpdateListener(listener);
    employeeEditModel.addEntitiesChangedListener(listener);
    employeeEditModel.addBeforeRefreshListener(listener);
    employeeEditModel.addAfterRefreshListener(listener);

    assertEquals(EmpDept.T_EMPLOYEE, employeeEditModel.getEntityID());
    assertEquals(employeeEditModel.getConnectionProvider().getConnection().selectPropertyValues(EmpDept.T_EMPLOYEE, EmpDept.EMPLOYEE_JOB, true),
            employeeEditModel.getValueProvider(jobProperty).getValues());

    employeeEditModel.refresh();
    assertTrue(employeeEditModel.isEntityNew());
    assertFalse(employeeEditModel.getModifiedObserver().isActive());

    final Entity employee = employeeEditModel.getConnectionProvider().getConnection().selectSingle(EmpDept.T_EMPLOYEE, EmpDept.EMPLOYEE_NAME, "MARTIN");
    employeeEditModel.setEntity(employee);
    assertFalse(primaryKeyNullState.isActive());

    assertTrue("Active entity is not equal to the entity just set", employeeEditModel.getEntityCopy().propertyValuesEqual(employee));
    assertFalse("Active entity is new after an entity is set", employeeEditModel.isEntityNew());
    assertFalse(employeeEditModel.getModifiedObserver().isActive());
    employeeEditModel.setEntity(null);
    assertTrue("Active entity is new after entity is set to null", employeeEditModel.isEntityNew());
    assertFalse(employeeEditModel.getModifiedObserver().isActive());
    assertTrue("Active entity primary key is not null after entity is set to null", employeeEditModel.getEntityCopy().isPrimaryKeyNull());

    employeeEditModel.setEntity(employee);
    assertTrue("Active entity primary key is null after entity is set", !employeeEditModel.getEntityCopy().isPrimaryKeyNull());

    final Integer originalEmployeeId = (Integer) employeeEditModel.getValue(EmpDept.EMPLOYEE_ID);
    employeeEditModel.setValue(EmpDept.EMPLOYEE_ID, null);
    assertTrue(primaryKeyNullState.isActive());
    employeeEditModel.setValue(EmpDept.EMPLOYEE_ID, originalEmployeeId);
    assertFalse(primaryKeyNullState.isActive());

    employeeEditModel.setEntity(null);

    final Double originalCommission = (Double) employeeEditModel.getValue(EmpDept.EMPLOYEE_COMMISSION);
    final double commission = 1500.5;
    final Date originalHiredate = (Date) employeeEditModel.getValue(EmpDept.EMPLOYEE_HIREDATE);
    final Date hiredate = new Date();
    final String originalName = (String) employeeEditModel.getValue(EmpDept.EMPLOYEE_NAME);
    final String name = "Mr. Mr";

    employeeEditModel.setValue(EmpDept.EMPLOYEE_COMMISSION, commission);
    assertTrue(employeeEditModel.getModifiedObserver().isActive());
    employeeEditModel.setValue(EmpDept.EMPLOYEE_HIREDATE, hiredate);
    employeeEditModel.setValue(EmpDept.EMPLOYEE_NAME, name);

    assertEquals("Commission does not fit", employeeEditModel.getValue(EmpDept.EMPLOYEE_COMMISSION), commission);
    assertEquals("Hiredate does not fit", employeeEditModel.getValue(EmpDept.EMPLOYEE_HIREDATE), hiredate);
    assertEquals("Name does not fit", employeeEditModel.getValue(EmpDept.EMPLOYEE_NAME), name);

    employeeEditModel.setValue(EmpDept.EMPLOYEE_COMMISSION, originalCommission);
    assertTrue(employeeEditModel.isModified());
    assertTrue(employeeEditModel.getModifiedObserver().isActive());
    employeeEditModel.setValue(EmpDept.EMPLOYEE_HIREDATE, originalHiredate);
    assertTrue(employeeEditModel.isModified());
    employeeEditModel.setValue(EmpDept.EMPLOYEE_NAME, originalName);
    assertFalse(employeeEditModel.isModified());

    //test validation
    try {
      employeeEditModel.setValue(EmpDept.EMPLOYEE_COMMISSION, 50d);
      employeeEditModel.validate(EmpDept.EMPLOYEE_COMMISSION);
      fail("Validation should fail on invalid commission value");
    }
    catch (ValidationException e) {
      assertEquals(EmpDept.EMPLOYEE_COMMISSION, e.getKey());
      assertEquals(50d, e.getValue());
      final Property property = Entities.getProperty(EmpDept.T_EMPLOYEE, (String) e.getKey());
      assertEquals("Validation message should fit", "'" + property + "' " +
              FrameworkMessages.get(FrameworkMessages.PROPERTY_VALUE_TOO_SMALL) + " " + property.getMin(), e.getMessage());
    }

    employeeEditModel.setEntity(null);
    assertTrue("Active entity is not null after model is cleared", employeeEditModel.getEntityCopy().isPrimaryKeyNull());

    employeeEditModel.removeAfterDeleteListener(listener);
    employeeEditModel.removeAfterInsertListener(listener);
    employeeEditModel.removeAfterUpdateListener(listener);
    employeeEditModel.removeBeforeDeleteListener(listener);
    employeeEditModel.removeBeforeInsertListener(listener);
    employeeEditModel.removeBeforeUpdateListener(listener);
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
  public void deleteReadOnly() throws CancelException, ValidationException, DatabaseException {
    employeeEditModel.setReadOnly(true);
    employeeEditModel.delete();
  }

  @Test
  public void insert() throws Exception {
    try {
      assertTrue(employeeEditModel.insert(new ArrayList<Entity>()).isEmpty());
      employeeEditModel.getConnectionProvider().getConnection().beginTransaction();
      employeeEditModel.setValue(EmpDept.EMPLOYEE_COMMISSION, 1000d);
      employeeEditModel.setValue(EmpDept.EMPLOYEE_HIREDATE, DateUtil.floorDate(new Date()));
      employeeEditModel.setValue(EmpDept.EMPLOYEE_JOB, "A Jobby");
      employeeEditModel.setValue(EmpDept.EMPLOYEE_NAME, "Björn");
      employeeEditModel.setValue(EmpDept.EMPLOYEE_SALARY, 1000d);

      final Entity tmpDept = Entities.entity(EmpDept.T_DEPARTMENT);
      tmpDept.setValue(EmpDept.DEPARTMENT_ID, 99);
      tmpDept.setValue(EmpDept.DEPARTMENT_LOCATION, "Limbo");
      tmpDept.setValue(EmpDept.DEPARTMENT_NAME, "Judgment");

      final Entity department = employeeEditModel.getConnectionProvider().getConnection().selectSingle(employeeEditModel.getConnectionProvider().getConnection().insert(Arrays.asList(tmpDept)).get(0));

      employeeEditModel.setValue(EmpDept.EMPLOYEE_DEPARTMENT_FK, department);

      employeeEditModel.addAfterInsertListener(new EventAdapter<EntityEditModel.InsertEvent>() {
        @Override
        public void eventOccurred(final EntityEditModel.InsertEvent eventInfo) {
          assertEquals(department, eventInfo.getInsertedEntities().get(0).getValue(EmpDept.EMPLOYEE_DEPARTMENT_FK));
        }
      });
      employeeEditModel.setInsertAllowed(false);
      assertFalse(employeeEditModel.isInsertAllowed());
      try {
        employeeEditModel.insert();
        fail("Should not be able to insert");
      }
      catch (UnsupportedOperationException e) {}
      employeeEditModel.setInsertAllowed(true);
      assertTrue(employeeEditModel.isInsertAllowed());

      employeeEditModel.insert();
      assertFalse(employeeEditModel.isEntityNew());
      final Entity entityCopy = employeeEditModel.getEntityCopy();
      assertFalse(entityCopy.getPrimaryKey().isNull());
      assertEquals(entityCopy.getPrimaryKey(), entityCopy.getOriginalPrimaryKey());

      employeeEditModel.setValue(EmpDept.EMPLOYEE_NAME, "Bobby");
      try {
        employeeEditModel.insert();
      }
      catch (Exception e) {
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
      assertTrue(employeeEditModel.update(new ArrayList<Entity>()).isEmpty());
      employeeEditModel.getConnectionProvider().getConnection().beginTransaction();
      employeeEditModel.setEntity(employeeEditModel.getConnectionProvider().getConnection().selectSingle(EmpDept.T_EMPLOYEE, EmpDept.EMPLOYEE_NAME, "MILLER"));
      employeeEditModel.setValue(EmpDept.EMPLOYEE_NAME, "BJORN");
      final List<Entity> toUpdate = Arrays.asList(employeeEditModel.getEntityCopy());
      final EventListener<EntityEditModel.UpdateEvent> listener = new EventAdapter<EntityEditModel.UpdateEvent>() {
        @Override
        public void eventOccurred(final EntityEditModel.UpdateEvent eventInfo) {
          assertEquals(toUpdate, eventInfo.getUpdatedEntities());
        }
      };
      employeeEditModel.addAfterUpdateListener(listener);
      employeeEditModel.setUpdateAllowed(false);
      assertFalse(employeeEditModel.isUpdateAllowed());
      try {
        employeeEditModel.update();
        fail("Should not be able to update");
      }
      catch (UnsupportedOperationException e) {}
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
      assertTrue(employeeEditModel.delete(new ArrayList<Entity>()).isEmpty());
      employeeEditModel.getConnectionProvider().getConnection().beginTransaction();
      employeeEditModel.setEntity(employeeEditModel.getConnectionProvider().getConnection().selectSingle(EmpDept.T_EMPLOYEE, EmpDept.EMPLOYEE_NAME, "MILLER"));
      final List<Entity> toDelete = Arrays.asList(employeeEditModel.getEntityCopy());
      employeeEditModel.addAfterDeleteListener(new EventAdapter<EntityEditModel.DeleteEvent>() {
        @Override
        public void eventOccurred(final EntityEditModel.DeleteEvent eventInfo) {
          assertEquals(toDelete, eventInfo.getDeletedEntities());
        }
      });
      employeeEditModel.setDeleteAllowed(false);
      assertFalse(employeeEditModel.isDeleteAllowed());
      try {
        employeeEditModel.delete();
        fail("Should not be able to delete");
      }
      catch (UnsupportedOperationException e) {}
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
    final Entity martin = employeeEditModel.getConnectionProvider().getConnection().selectSingle(EmpDept.T_EMPLOYEE, EmpDept.EMPLOYEE_NAME, "MARTIN");
    final Entity king = employeeEditModel.getConnectionProvider().getConnection().selectSingle(EmpDept.T_EMPLOYEE, EmpDept.EMPLOYEE_NAME, "KING");
    employeeEditModel.setEntity(king);
    employeeEditModel.setValue(EmpDept.EMPLOYEE_MGR_FK, martin);
    employeeEditModel.setEntity(null);
    king.setValue(EmpDept.EMPLOYEE_MGR_FK, null);
    employeeEditModel.setEntity(king);
    assertNull(employeeEditModel.getValue(EmpDept.EMPLOYEE_MGR_FK));
    employeeEditModel.setEntity(null);
    assertEquals(DateUtil.floorDate(new Date()), employeeEditModel.getValue(EmpDept.EMPLOYEE_HIREDATE));
    assertFalse(employeeEditModel.getEntity().isModified(EmpDept.EMPLOYEE_HIREDATE));
    assertFalse(employeeEditModel.getEntity().isModified());
  }

  @Test
  public void setValuePersistent() throws Exception {
    final Entity king = employeeEditModel.getConnectionProvider().getConnection().selectSingle(EmpDept.T_EMPLOYEE, EmpDept.EMPLOYEE_NAME, "KING");
    employeeEditModel.setEntity(king);
    assertNotNull(employeeEditModel.getValue(EmpDept.EMPLOYEE_JOB));
    employeeEditModel.setValuePersistent(EmpDept.EMPLOYEE_JOB, true);
    employeeEditModel.setEntity(null);
    assertNotNull(employeeEditModel.getValue(EmpDept.EMPLOYEE_JOB));
    employeeEditModel.setEntity(king);
    employeeEditModel.setValuePersistent(EmpDept.EMPLOYEE_JOB, false);
    employeeEditModel.setEntity(null);
    assertNull(employeeEditModel.getValue(EmpDept.EMPLOYEE_JOB));
  }

  @Test
  public void testListeners() throws Exception {
    final Collection<Object> anyValueChangeCounter = new ArrayList<Object>();
    final Collection<Object> valueChangeCounter = new ArrayList<Object>();
    final Collection<Object> valueSetCounter = new ArrayList<Object>();
    final Collection<Object> valueMapSetCounter = new ArrayList<Object>();

    final ValueChangeListener anyValueChangeListener = new ValueChangeListener() {
      @Override
      protected void valueChanged(final ValueChangeEvent event) {
        anyValueChangeCounter.add(new Object());
      }
    };
    final ValueChangeListener valueChangeListener = new ValueChangeListener() {
      @Override
      protected void valueChanged(final ValueChangeEvent event) {
        valueChangeCounter.add(new Object());
      }
    };
    final EventListener valueSetListener = new EventAdapter() {
      @Override
      public void eventOccurred() {
        valueSetCounter.add(new Object());
      }
    };
    final EventListener valueMapSetListener = new EventAdapter() {
      @Override
      public void eventOccurred() {
        valueMapSetCounter.add(new Object());
      }
    };

    final DefaultEntityEditModel model = new DefaultEntityEditModel(EmpDept.T_DEPARTMENT, DefaultEntityConnectionTest.CONNECTION_PROVIDER);

    model.getValueChangeObserver().addListener(anyValueChangeListener);
    model.addValueListener(EmpDept.DEPARTMENT_ID, valueChangeListener);
    model.addValueSetListener(EmpDept.DEPARTMENT_ID, valueSetListener);
    model.addEntitySetListener(valueMapSetListener);

    model.setValue(EmpDept.DEPARTMENT_ID, 1);
    assertTrue(valueSetCounter.size() == 1);
    assertTrue(valueChangeCounter.size() == 1);
    assertTrue(anyValueChangeCounter.size() == 1);

    model.setValue(EmpDept.DEPARTMENT_ID, 1);
    assertTrue(valueSetCounter.size() == 1);
    assertTrue(valueChangeCounter.size() == 1);
    assertTrue(anyValueChangeCounter.size() == 1);

    assertFalse(model.isNullable(EmpDept.DEPARTMENT_ID));
    assertTrue(!model.isValueNull(EmpDept.DEPARTMENT_ID));
    assertEquals(1, model.getValue(EmpDept.DEPARTMENT_ID));

    model.setValue(EmpDept.DEPARTMENT_ID, null);
    assertTrue(valueSetCounter.size() == 2);
    assertTrue(valueChangeCounter.size() == 2);
    assertTrue(anyValueChangeCounter.size() == 2);
    assertTrue(model.isValueNull(EmpDept.DEPARTMENT_ID));

    model.setValue(EmpDept.DEPARTMENT_NAME, "Name");
    assertTrue(valueSetCounter.size() == 2);
    assertTrue(valueChangeCounter.size() == 2);
    assertTrue(anyValueChangeCounter.size() == 3);

    model.getEntity();
    model.clear();
    model.refresh();

    assertNotNull(model.getDefaultEntity());

    final Entity newEntity = Entities.entity(EmpDept.T_DEPARTMENT);
    newEntity.setValue(EmpDept.DEPARTMENT_ID, 1);
    newEntity.setValue(EmpDept.DEPARTMENT_LOCATION, "NY");
    newEntity.setValue(EmpDept.DEPARTMENT_NAME, "a name");

    model.setEntity(newEntity);
    assertEquals(1, model.getValue(EmpDept.DEPARTMENT_ID));
    assertTrue(valueMapSetCounter.size() == 1);

    model.setValue(EmpDept.DEPARTMENT_ID, null);
    try {
      model.validate(EmpDept.DEPARTMENT_ID);
      fail();
    }
    catch (ValidationException e) {}

    assertFalse(model.isValid(EmpDept.DEPARTMENT_ID));
    assertFalse(model.isValid());

    model.setValue(EmpDept.DEPARTMENT_ID, 1);
    assertTrue(model.isValid(EmpDept.DEPARTMENT_ID));
    assertTrue(model.isValid());

    model.removeValueListener(EmpDept.DEPARTMENT_ID, valueChangeListener);
    model.removeValueSetListener(EmpDept.DEPARTMENT_ID, valueSetListener);
    model.removeEntitySetListener(valueMapSetListener);
  }
}
