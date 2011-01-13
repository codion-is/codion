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

  private DefaultEntityEditModel editModel;
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
    editModel = new DefaultEntityEditModel(EmpDept.T_EMPLOYEE, EntityConnectionImplTest.DB_PROVIDER);
  }

  @After
  public void tearDown() {
    Configuration.setValue(Configuration.PROPERTY_DEBUG_OUTPUT, debugOutput);
  }

  @Test
  public void initializePropertyComboBoxModel() {
    try {
      editModel.getPropertyComboBoxModel(jobProperty);
      fail();
    }
    catch (IllegalStateException e) {}
    final FilteredComboBoxModel model = editModel.initializePropertyComboBoxModel(jobProperty, null, "null");
    assertNotNull(model);
    assertTrue(editModel.containsComboBoxModel(jobProperty.getPropertyID()));
    assertEquals(model, editModel.getPropertyComboBoxModel(jobProperty));
    editModel.refreshComboBoxModels();
    editModel.clearComboBoxModels();
    assertTrue(editModel.getPropertyComboBoxModel(jobProperty).isCleared());
    editModel.refreshComboBoxModels();
    editModel.clear();
    assertTrue(editModel.getPropertyComboBoxModel(jobProperty).isCleared());
  }

  @Test
  public void initializeEntityComboBoxModel() {
    try {
      editModel.getEntityComboBoxModel(deptProperty);
      fail();
    }
    catch (IllegalStateException e) {}
    final EntityComboBoxModel model = editModel.initializeEntityComboBoxModel(deptProperty);
    assertNotNull(model);
    assertTrue(model.isCleared());
    assertTrue(model.getAllItems().isEmpty());
    editModel.refreshComboBoxModels();
    assertFalse(model.isCleared());
    assertFalse(model.getAllItems().isEmpty());
    editModel.clearComboBoxModels();
    assertTrue(model.isCleared());
    assertTrue(model.getAllItems().isEmpty());
  }

  @Test
  public void createEntityComboBoxModel() {
    final EntityComboBoxModel model = editModel.createEntityComboBoxModel(deptProperty);
    assertNotNull(model);
    assertTrue(model.isCleared());
    assertTrue(model.getAllItems().isEmpty());
    assertEquals(deptProperty.getReferencedEntityID(), model.getEntityID());
  }

  @Test
  public void getEntityComboBoxModel() {
    try {
      editModel.initializeEntityComboBoxModel(jobProperty.getPropertyID());
      fail();
    }
    catch (IllegalArgumentException e) {}
    try {
      editModel.getEntityComboBoxModel(jobProperty.getPropertyID());
      fail();
    }
    catch (IllegalArgumentException e) {}
    try {
      editModel.getEntityComboBoxModel(deptProperty.getPropertyID());
      fail();
    }
    catch (IllegalStateException e) {}
    final EntityComboBoxModel model = editModel.initializeEntityComboBoxModel(deptProperty.getPropertyID());
    assertNotNull(model);
    assertEquals(model, editModel.getEntityComboBoxModel(deptProperty));
  }

  @Test
  public void createEntityLookupModel() {
    final EntityLookupModel model = editModel.createEntityLookupModel(EmpDept.T_DEPARTMENT,
            Entities.getSearchProperties(EmpDept.T_DEPARTMENT), null);
    assertNotNull(model);
    assertEquals(EmpDept.T_DEPARTMENT, model.getEntityID());
  }

  @Test
  public void getEntityCopy() throws DatabaseException {
    final Entity employee = editModel.getConnectionProvider().getConnection().selectSingle(EmpDept.T_EMPLOYEE, EmpDept.EMPLOYEE_NAME, "MARTIN");
    editModel.setEntity(employee);
    final Entity copyWithPrimaryKeyValue = editModel.getEntityCopy();
    assertEquals(employee, copyWithPrimaryKeyValue);
    assertFalse(copyWithPrimaryKeyValue.isPrimaryKeyNull());
    final Entity copyWithoutPrimaryKeyValue = editModel.getEntityCopy(false);
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

    final StateObserver entityNullState = editModel.getEntityNullObserver();

    assertTrue(entityNullState.isActive());

    editModel.setReadOnly(false);
    assertFalse(editModel.isReadOnly());
    assertTrue(editModel.getAllowInsertObserver().isActive());
    assertTrue(editModel.getAllowUpdateObserver().isActive());
    assertTrue(editModel.getAllowDeleteObserver().isActive());

    final ActionListener listener = new ActionListener() {
      public void actionPerformed(final ActionEvent e) {}
    };
    editModel.addAfterDeleteListener(listener);
    editModel.addAfterInsertListener(listener);
    editModel.addAfterUpdateListener(listener);
    editModel.addBeforeDeleteListener(listener);
    editModel.addBeforeInsertListener(listener);
    editModel.addBeforeUpdateListener(listener);
    editModel.addEntitiesChangedListener(listener);
    editModel.addBeforeRefreshListener(listener);
    editModel.addAfterRefreshListener(listener);

    assertEquals(EmpDept.T_EMPLOYEE, editModel.getEntityID());
    assertEquals(editModel.getConnectionProvider().getConnection().selectPropertyValues(EmpDept.T_EMPLOYEE, EmpDept.EMPLOYEE_JOB, true),
            editModel.getValueProvider(jobProperty).getValues());

    editModel.refresh();
    assertTrue(editModel.isEntityNew());
    assertFalse(editModel.getModifiedObserver().isActive());

    final Entity employee = editModel.getConnectionProvider().getConnection().selectSingle(EmpDept.T_EMPLOYEE, EmpDept.EMPLOYEE_NAME, "MARTIN");
    editModel.setEntity(employee);
    assertFalse(entityNullState.isActive());

    assertTrue("Active entity is not equal to the entity just set", editModel.getEntityCopy().propertyValuesEqual(employee));
    assertFalse("Active entity is new after an entity is set", editModel.isEntityNew());
    assertFalse(editModel.getModifiedObserver().isActive());
    editModel.setEntity(null);
    assertTrue("Active entity is new null after entity is set to null", editModel.isEntityNew());
    assertFalse(editModel.getModifiedObserver().isActive());
    assertTrue("Active entity is not null after entity is set to null", editModel.getEntityCopy().isPrimaryKeyNull());

    editModel.setEntity(employee);
    assertTrue("Active entity is null after selection is made", !editModel.getEntityCopy().isPrimaryKeyNull());
    editModel.setEntity(null);

    final Double originalCommission = (Double) editModel.getValue(EmpDept.EMPLOYEE_COMMISSION);
    final double commission = 1500.5;
    final Date originalHiredate = (Date) editModel.getValue(EmpDept.EMPLOYEE_HIREDATE);
    final Date hiredate = new Date();
    final String originalName = (String) editModel.getValue(EmpDept.EMPLOYEE_NAME);
    final String name = "Mr. Mr";

    editModel.setValue(EmpDept.EMPLOYEE_COMMISSION, commission);
    assertTrue(editModel.getModifiedObserver().isActive());
    editModel.setValue(EmpDept.EMPLOYEE_HIREDATE, hiredate);
    editModel.setValue(EmpDept.EMPLOYEE_NAME, name);

    assertEquals("Commission does not fit", editModel.getValue(EmpDept.EMPLOYEE_COMMISSION), commission);
    assertEquals("Hiredate does not fit", editModel.getValue(EmpDept.EMPLOYEE_HIREDATE), hiredate);
    assertEquals("Name does not fit", editModel.getValue(EmpDept.EMPLOYEE_NAME), name);

    editModel.setValue(EmpDept.EMPLOYEE_COMMISSION, originalCommission);
    assertTrue(editModel.isModified());
    assertTrue(editModel.getModifiedObserver().isActive());
    editModel.setValue(EmpDept.EMPLOYEE_HIREDATE, originalHiredate);
    assertTrue(editModel.isModified());
    editModel.setValue(EmpDept.EMPLOYEE_NAME, originalName);
    assertFalse(editModel.isModified());

    //test validation
    try {
      editModel.setValue(EmpDept.EMPLOYEE_COMMISSION, 50d);
      editModel.validate(EmpDept.EMPLOYEE_COMMISSION, ValueMapValidator.INSERT);
      fail("Validation should fail on invalid commission value");
    }
    catch (ValidationException e) {
      assertEquals(EmpDept.EMPLOYEE_COMMISSION, e.getKey());
      assertEquals(50d, e.getValue());
      final Property property = Entities.getProperty(EmpDept.T_EMPLOYEE, (String) e.getKey());
      assertEquals("Validation message should fit", "'" + property + "' " +
              FrameworkMessages.get(FrameworkMessages.PROPERTY_VALUE_TOO_SMALL) + " " + property.getMin(), e.getMessage());
    }

    editModel.setValueMap(null);
    assertTrue("Active entity is not null after model is cleared", editModel.getEntityCopy().isPrimaryKeyNull());

    editModel.removeAfterDeleteListener(listener);
    editModel.removeAfterInsertListener(listener);
    editModel.removeAfterUpdateListener(listener);
    editModel.removeBeforeDeleteListener(listener);
    editModel.removeBeforeInsertListener(listener);
    editModel.removeBeforeUpdateListener(listener);
    editModel.removeEntitiesChangedListener(listener);
    editModel.removeBeforeRefreshListener(listener);
    editModel.removeAfterRefreshListener(listener);
  }

  @Test(expected = UnsupportedOperationException.class)
  public void insertReadOnly() throws CancelException, ValidationException, DatabaseException {
    editModel.setReadOnly(true);
    editModel.insert();
  }

  @Test(expected = UnsupportedOperationException.class)
  public void updateReadOnly() throws CancelException, ValidationException, DatabaseException {
    editModel.setReadOnly(true);
    editModel.update();
  }

  @Test(expected = UnsupportedOperationException.class)
  public void deleteReadOnly() throws CancelException, ValidationException, DatabaseException {
    editModel.setReadOnly(true);
    editModel.delete();
  }

  @Test
  public void insert() throws Exception {
    try {
      assertTrue(editModel.insert(new ArrayList<Entity>()).isEmpty());
      editModel.getConnectionProvider().getConnection().beginTransaction();
      editModel.setValue(EmpDept.EMPLOYEE_COMMISSION, 1000d);
      editModel.setValue(EmpDept.EMPLOYEE_HIREDATE, DateUtil.floorDate(new Date()));
      editModel.setValue(EmpDept.EMPLOYEE_JOB, "A Jobby");
      editModel.setValue(EmpDept.EMPLOYEE_NAME, "Björn");
      editModel.setValue(EmpDept.EMPLOYEE_SALARY, 1000d);

      final Entity tmpDept = Entities.entity(EmpDept.T_DEPARTMENT);
      tmpDept.setValue(EmpDept.DEPARTMENT_ID, 99);
      tmpDept.setValue(EmpDept.DEPARTMENT_LOCATION, "Limbo");
      tmpDept.setValue(EmpDept.DEPARTMENT_NAME, "Judgment");

      final Entity department = editModel.getConnectionProvider().getConnection().selectSingle(editModel.getConnectionProvider().getConnection().insert(Arrays.asList(tmpDept)).get(0));

      editModel.setValue(EmpDept.EMPLOYEE_DEPARTMENT_FK, department);

      editModel.addAfterInsertListener(new InsertListener() {
        @Override
        protected void inserted(final InsertEvent event) {
          try {
            final Entity inserted = editModel.getConnectionProvider().getConnection().selectSingle(event.getInsertedKeys().get(0));
            assertEquals(department, inserted.getValue(EmpDept.EMPLOYEE_DEPARTMENT_FK));
          }
          catch (Exception ex) {
            ex.printStackTrace();
          }
        }
      });
      editModel.setInsertAllowed(false);
      assertFalse(editModel.isInsertAllowed());
      try {
        editModel.insert();
        fail("Should not be able to insert");
      }
      catch (UnsupportedOperationException e) {}
      editModel.setInsertAllowed(true);
      assertTrue(editModel.isInsertAllowed());

      editModel.insert();

      editModel.setValue(EmpDept.EMPLOYEE_NAME, "Bobby");
      try {
        editModel.insert();
      }
      catch (Exception e) {
        fail("Should be able to insert again");
      }
    }
    finally {
      editModel.getConnectionProvider().getConnection().rollbackTransaction();
    }
  }

  @Test
  public void update() throws Exception {
    try {
      assertTrue(editModel.update().isEmpty());
      assertTrue(editModel.update(new ArrayList<Entity>()).isEmpty());
      editModel.getConnectionProvider().getConnection().beginTransaction();
      editModel.setEntity(editModel.getConnectionProvider().getConnection().selectSingle(EmpDept.T_EMPLOYEE, EmpDept.EMPLOYEE_NAME, "MILLER"));
      editModel.setValue(EmpDept.EMPLOYEE_NAME, "BJORN");
      final List<Entity> toUpdate = Arrays.asList(editModel.getEntityCopy());
      final UpdateListener listener = new UpdateListener() {
        @Override
        protected void updated(final UpdateEvent event) {
          assertEquals(toUpdate, event.getUpdatedEntities());
        }
      };
      editModel.addAfterUpdateListener(listener);
      editModel.setUpdateAllowed(false);
      assertFalse(editModel.isUpdateAllowed());
      try {
        editModel.update();
        fail("Should not be able to update");
      }
      catch (UnsupportedOperationException e) {}
      editModel.setUpdateAllowed(true);
      assertTrue(editModel.isUpdateAllowed());

      editModel.update();
      assertFalse(editModel.getModifiedObserver().isActive());
      editModel.removeAfterUpdateListener(listener);
    }
    finally {
      editModel.getConnectionProvider().getConnection().rollbackTransaction();
    }
  }

  @Test
  public void delete() throws Exception {
    try {
      assertTrue(editModel.delete(new ArrayList<Entity>()).isEmpty());
      editModel.getConnectionProvider().getConnection().beginTransaction();
      editModel.setEntity(editModel.getConnectionProvider().getConnection().selectSingle(EmpDept.T_EMPLOYEE, EmpDept.EMPLOYEE_NAME, "MILLER"));
      final List<Entity> toDelete = Arrays.asList(editModel.getEntityCopy());
      editModel.addAfterDeleteListener(new DeleteListener() {
        @Override
        protected void deleted(final DeleteEvent event) {
          assertEquals(toDelete, event.getDeletedEntities());
        }
      });
      editModel.setDeleteAllowed(false);
      assertFalse(editModel.isDeleteAllowed());
      try {
        editModel.delete();
        fail("Should not be able to delete");
      }
      catch (UnsupportedOperationException e) {}
      editModel.setDeleteAllowed(true);
      assertTrue(editModel.isDeleteAllowed());

      editModel.delete();
    }
    finally {
      editModel.getConnectionProvider().getConnection().rollbackTransaction();
    }
  }
}
