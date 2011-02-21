/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model;

import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.model.CancelException;
import org.jminor.common.model.DateUtil;
import org.jminor.common.model.StateObserver;
import org.jminor.common.model.combobox.FilteredComboBoxModel;
import org.jminor.common.model.valuemap.ValueMapValidator;
import org.jminor.common.model.valuemap.exception.ValidationException;
import org.jminor.framework.Configuration;
import org.jminor.framework.client.model.event.DeleteEvent;
import org.jminor.framework.client.model.event.DeleteListener;
import org.jminor.framework.client.model.event.InsertEvent;
import org.jminor.framework.client.model.event.InsertListener;
import org.jminor.framework.client.model.event.UpdateEvent;
import org.jminor.framework.client.model.event.UpdateListener;
import org.jminor.framework.db.EntityConnectionImplTest;
import org.jminor.framework.demos.empdept.domain.EmpDept;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.Property;
import org.jminor.framework.i18n.FrameworkMessages;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
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
    employeeEditModel = new DefaultEntityEditModel(EmpDept.T_EMPLOYEE, EntityConnectionImplTest.DB_PROVIDER);
  }

  @After
  public void tearDown() {
    Configuration.setValue(Configuration.PROPERTY_DEBUG_OUTPUT, debugOutput);
  }

  @Test
  public void initializePropertyComboBoxModel() {
    try {
      employeeEditModel.getPropertyComboBoxModel(jobProperty);
      fail();
    }
    catch (IllegalStateException e) {}
    final FilteredComboBoxModel model = employeeEditModel.initializePropertyComboBoxModel(jobProperty, null, "null");
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
  public void initializeEntityComboBoxModel() {
    try {
      employeeEditModel.getEntityComboBoxModel(deptProperty);
      fail();
    }
    catch (IllegalStateException e) {}
    final EntityComboBoxModel model = employeeEditModel.initializeEntityComboBoxModel(deptProperty);
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

  @Test
  public void getEntityComboBoxModel() {
    try {
      employeeEditModel.initializeEntityComboBoxModel(jobProperty.getPropertyID());
      fail();
    }
    catch (IllegalArgumentException e) {}
    try {
      employeeEditModel.getEntityComboBoxModel(jobProperty.getPropertyID());
      fail();
    }
    catch (IllegalArgumentException e) {}
    try {
      employeeEditModel.getEntityComboBoxModel(deptProperty.getPropertyID());
      fail();
    }
    catch (IllegalStateException e) {}
    final EntityComboBoxModel model = employeeEditModel.initializeEntityComboBoxModel(deptProperty.getPropertyID());
    assertNotNull(model);
    assertEquals(model, employeeEditModel.getEntityComboBoxModel(deptProperty));
  }

  @Test
  public void getEntityLookupModel() {
    try {
      employeeEditModel.initializeEntityLookupModel(jobProperty.getPropertyID());
      fail();
    }
    catch (IllegalArgumentException e) {}
    try {
      employeeEditModel.getEntityLookupModel(jobProperty.getPropertyID());
      fail();
    }
    catch (IllegalArgumentException e) {}
    try {
      employeeEditModel.getEntityLookupModel(deptProperty.getPropertyID());
      fail();
    }
    catch (IllegalStateException e) {}
    final EntityLookupModel model = employeeEditModel.initializeEntityLookupModel(deptProperty.getPropertyID());
    assertNotNull(model);
    assertEquals(model, employeeEditModel.getEntityLookupModel(deptProperty));
  }

  @Test
  public void initializeEntityLookupModel() {
    assertFalse(employeeEditModel.containsLookupModel(deptProperty.getPropertyID()));
    try {
      employeeEditModel.getEntityLookupModel(deptProperty);
      fail();
    }
    catch (IllegalStateException e) {}
    final EntityLookupModel model = employeeEditModel.initializeEntityLookupModel(deptProperty);
    assertNotNull(model);
    assertTrue(employeeEditModel.containsLookupModel(deptProperty.getPropertyID()));
    assertNotNull(employeeEditModel.getEntityLookupModel(deptProperty));
  }

  @Test
  public void createEntityLookupModel() {
    final EntityLookupModel model = employeeEditModel.createEntityLookupModel(EmpDept.EMPLOYEE_DEPARTMENT_FK);
    assertNotNull(model);
    assertEquals(EmpDept.T_DEPARTMENT, model.getEntityID());
    try {
      employeeEditModel.createEntityLookupModel(EmpDept.EMPLOYEE_COMMISSION);
      fail();
    }
    catch (IllegalArgumentException e) {}
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

  @Test
  public void test() throws Exception {
    try {
      new DefaultEntityEditModel(null, EntityConnectionImplTest.DB_PROVIDER);
      fail();
    }
    catch (IllegalArgumentException e) {}
    try {
      new DefaultEntityEditModel("entityID", null);
      fail();
    }
    catch (IllegalArgumentException e) {}
    try {
      new DefaultEntityEditModel(null, null);
      fail();
    }
    catch (IllegalArgumentException e) {}

    final StateObserver entityNullState = employeeEditModel.getEntityNullObserver();

    assertTrue(entityNullState.isActive());

    employeeEditModel.setReadOnly(false);
    assertFalse(employeeEditModel.isReadOnly());
    assertTrue(employeeEditModel.getAllowInsertObserver().isActive());
    assertTrue(employeeEditModel.getAllowUpdateObserver().isActive());
    assertTrue(employeeEditModel.getAllowDeleteObserver().isActive());

    final ActionListener listener = new ActionListener() {
      public void actionPerformed(final ActionEvent e) {}
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
    assertFalse(entityNullState.isActive());

    assertTrue("Active entity is not equal to the entity just set", employeeEditModel.getEntityCopy().propertyValuesEqual(employee));
    assertFalse("Active entity is new after an entity is set", employeeEditModel.isEntityNew());
    assertFalse(employeeEditModel.getModifiedObserver().isActive());
    employeeEditModel.setEntity(null);
    assertTrue("Active entity is new null after entity is set to null", employeeEditModel.isEntityNew());
    assertFalse(employeeEditModel.getModifiedObserver().isActive());
    assertTrue("Active entity is not null after entity is set to null", employeeEditModel.getEntityCopy().isPrimaryKeyNull());

    employeeEditModel.setEntity(employee);
    assertTrue("Active entity is null after selection is made", !employeeEditModel.getEntityCopy().isPrimaryKeyNull());
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
      employeeEditModel.validate(EmpDept.EMPLOYEE_COMMISSION, ValueMapValidator.INSERT);
      fail("Validation should fail on invalid commission value");
    }
    catch (ValidationException e) {
      assertEquals(EmpDept.EMPLOYEE_COMMISSION, e.getKey());
      assertEquals(50d, e.getValue());
      final Property property = Entities.getProperty(EmpDept.T_EMPLOYEE, (String) e.getKey());
      assertEquals("Validation message should fit", "'" + property + "' " +
              FrameworkMessages.get(FrameworkMessages.PROPERTY_VALUE_TOO_SMALL) + " " + property.getMin(), e.getMessage());
    }

    employeeEditModel.setValueMap(null);
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

      employeeEditModel.addAfterInsertListener(new InsertListener() {
        @Override
        protected void inserted(final InsertEvent event) {
          try {
            final Entity inserted = employeeEditModel.getConnectionProvider().getConnection().selectSingle(event.getInsertedKeys().get(0));
            assertEquals(department, inserted.getValue(EmpDept.EMPLOYEE_DEPARTMENT_FK));
          }
          catch (Exception ex) {
            ex.printStackTrace();
          }
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
      final UpdateListener listener = new UpdateListener() {
        @Override
        protected void updated(final UpdateEvent event) {
          assertEquals(toUpdate, event.getUpdatedEntities());
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
      employeeEditModel.addAfterDeleteListener(new DeleteListener() {
        @Override
        protected void deleted(final DeleteEvent event) {
          assertEquals(toDelete, event.getDeletedEntities());
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
}
