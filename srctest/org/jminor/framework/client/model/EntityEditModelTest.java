/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model;

import org.jminor.common.model.DateUtil;
import org.jminor.common.model.State;
import org.jminor.framework.client.model.event.DeleteEvent;
import org.jminor.framework.client.model.event.InsertEvent;
import org.jminor.framework.client.model.event.UpdateEvent;
import org.jminor.framework.client.model.exception.ValidationException;
import org.jminor.framework.db.EntityDbConnectionTest;
import org.jminor.framework.demos.empdept.beans.EmployeeModel;
import org.jminor.framework.demos.empdept.domain.EmpDept;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.EntityRepository;
import org.jminor.framework.i18n.FrameworkMessages;

import static org.junit.Assert.*;
import org.junit.Test;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class EntityEditModelTest {

  private EmployeeModel employeeModel = new EmployeeModel(EntityDbConnectionTest.DB_PROVIDER);

  @Test
  public void test() throws Exception {
    try {
      new EntityEditModel(null, EntityDbConnectionTest.DB_PROVIDER);
      fail();
    }
    catch (IllegalArgumentException e) {}
    try {
      new EntityEditModel("entityID", null);
      fail();
    }
    catch (IllegalArgumentException e) {}
    try {
      new EntityEditModel(null, null);
      fail();
    }
    catch (IllegalArgumentException e) {}

    final EntityEditModel editModel = employeeModel.getEditModel();
    final State entityNullState = editModel.getEntityNullState();

    assertTrue(entityNullState.isActive());

    employeeModel.getTableModel().refresh();
    assertTrue(editModel.isEntityNull());
    assertFalse(editModel.getEntityModifiedState().isActive());

    employeeModel.getTableModel().setSelectedItemIndex(0);

    assertFalse(entityNullState.isActive());

    assertTrue("Active entity is not equal to the selected entity",
            editModel.getEntityCopy().propertyValuesEqual(employeeModel.getTableModel().getEntityAtViewIndex(0)));

    assertFalse("Active entity is null after an entity is selected", editModel.isEntityNull());
    assertFalse(editModel.getEntityModifiedState().isActive());
    employeeModel.getTableModel().getSelectionModel().clearSelection();
    assertTrue("Active entity is not null after selection is cleared", editModel.isEntityNull());
    assertFalse(editModel.getEntityModifiedState().isActive());
    assertTrue("Active entity is not null after selection is cleared", editModel.getEntityCopy().isNull());

    employeeModel.getTableModel().setSelectedItemIndex(0);
    assertTrue("Active entity is null after selection is made", !editModel.getEntityCopy().isNull());

    final Double originalCommission = (Double) editModel.getValue(EmpDept.EMPLOYEE_COMMISSION);
    final double commission = 1500.5;
    final Timestamp originalHiredate = (Timestamp) editModel.getValue(EmpDept.EMPLOYEE_HIREDATE);
    final Timestamp hiredate = new Timestamp(System.currentTimeMillis());
    final String originalName = (String) editModel.getValue(EmpDept.EMPLOYEE_NAME);
    final String name = "Mr. Mr";

    editModel.setValue(EmpDept.EMPLOYEE_COMMISSION, commission);
    assertTrue(editModel.getEntityModifiedState().isActive());
    editModel.setValue(EmpDept.EMPLOYEE_HIREDATE, hiredate);
    editModel.setValue(EmpDept.EMPLOYEE_NAME, name);

    assertEquals("Commission does not fit", editModel.getValue(EmpDept.EMPLOYEE_COMMISSION), commission);
    assertEquals("Hiredate does not fit", editModel.getValue(EmpDept.EMPLOYEE_HIREDATE), hiredate);
    assertEquals("Name does not fit", editModel.getValue(EmpDept.EMPLOYEE_NAME), name);

    editModel.setValue(EmpDept.EMPLOYEE_COMMISSION, originalCommission);
    assertTrue(editModel.isEntityModified());
    assertTrue(editModel.getEntityModifiedState().isActive());
    editModel.setValue(EmpDept.EMPLOYEE_HIREDATE, originalHiredate);
    assertTrue(editModel.isEntityModified());
    editModel.setValue(EmpDept.EMPLOYEE_NAME, originalName);
    assertFalse(editModel.isEntityModified());

    //test validation
    try {
      editModel.setValue(EmpDept.EMPLOYEE_COMMISSION, 50d);
      editModel.validate(EntityRepository.getProperty(EmpDept.T_EMPLOYEE, EmpDept.EMPLOYEE_COMMISSION), EntityEditModel.INSERT);
      fail("Validation should fail on invalid commission value");
    }
    catch (ValidationException e) {
      assertEquals(EntityRepository.getProperty(EmpDept.T_EMPLOYEE, EmpDept.EMPLOYEE_COMMISSION), e.getProperty());
      assertEquals(50d, e.getValue());
      assertEquals("Validation message should fit", "'" + e.getProperty() + "' " +
              FrameworkMessages.get(FrameworkMessages.PROPERTY_VALUE_TOO_SMALL) + " " + e.getProperty().getMin(), e.getMessage());
    }

    editModel.clear();
    assertTrue("Active entity is not null after model is cleared", employeeModel.getEditModel().getEntityCopy().isNull());
  }

  @Test
  public void insert() throws Exception {
    final EntityEditModel editModel = employeeModel.getEditModel();
    try {
      editModel.getDbProvider().getEntityDb().beginTransaction();
      editModel.setValue(EmpDept.EMPLOYEE_COMMISSION, 1000d);
      editModel.setValue(EmpDept.EMPLOYEE_HIREDATE, new Timestamp(DateUtil.floorDate(new Date()).getTime()));
      editModel.setValue(EmpDept.EMPLOYEE_JOB, "A Jobby");
      editModel.setValue(EmpDept.EMPLOYEE_NAME, "Björn");
      editModel.setValue(EmpDept.EMPLOYEE_SALARY, 1000d);

      Entity department = new Entity(EmpDept.T_DEPARTMENT);
      department.setValue(EmpDept.DEPARTMENT_ID, 99);
      department.setValue(EmpDept.DEPARTMENT_LOCATION, "Limbo");
      department.setValue(EmpDept.DEPARTMENT_NAME, "Judgment");

      department = editModel.getDbProvider().getEntityDb().selectSingle(editModel.getDbProvider().getEntityDb().insert(Arrays.asList(department)).get(0));

      editModel.setValue(EmpDept.EMPLOYEE_DEPARTMENT_FK, department);

      final Entity toInsert = editModel.getEntityCopy();
      editModel.eventAfterInsert().addListener(new ActionListener() {
        public void actionPerformed(final ActionEvent e) {
          try {
            final InsertEvent de = (InsertEvent) e;
            final Entity inserted = editModel.getDbProvider().getEntityDb().selectSingle(de.getInsertedKeys().get(0));
            toInsert.setValue(EmpDept.EMPLOYEE_ID, inserted.getValue(EmpDept.EMPLOYEE_ID));
            assertTrue(toInsert.propertyValuesEqual(inserted));
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
      catch (Exception e) {}
      editModel.setInsertAllowed(true);
      assertTrue(editModel.isInsertAllowed());

      editModel.insert();
    }
    finally {
      editModel.getDbProvider().getEntityDb().rollbackTransaction();
    }
  }

  @Test
  public void update() throws Exception {
    final EntityEditModel editModel = employeeModel.getEditModel();
    try {
      editModel.getDbProvider().getEntityDb().beginTransaction();
      employeeModel.getTableModel().refresh();
      employeeModel.getTableModel().setSelectedItemIndex(0);
      editModel.setValue(EmpDept.EMPLOYEE_NAME, "BJORN");
      final List<Entity> toUpdate = employeeModel.getTableModel().getSelectedEntities();
      editModel.eventAfterUpdate().addListener(new ActionListener() {
        public void actionPerformed(final ActionEvent e) {
          final UpdateEvent de = (UpdateEvent) e;
          assertEquals(toUpdate, de.getUpdatedEntities());
        }
      });
      editModel.setUpdateAllowed(false);
      assertFalse(editModel.isUpdateAllowed());
      try {
        editModel.update();
        fail("Should not be able to update");
      }
      catch (Exception e) {}
      editModel.setUpdateAllowed(true);
      assertTrue(editModel.isUpdateAllowed());

      editModel.update();
    }
    finally {
      editModel.getDbProvider().getEntityDb().rollbackTransaction();
    }
  }

  @Test
  public void delete() throws Exception {
    final EntityEditModel editModel = employeeModel.getEditModel();
    try {
      editModel.getDbProvider().getEntityDb().beginTransaction();
      employeeModel.getTableModel().refresh();
      employeeModel.getTableModel().setSelectedItemIndex(0);
      final List<Entity> toDelete = employeeModel.getTableModel().getSelectedEntities();
      editModel.eventAfterDelete().addListener(new ActionListener() {
        public void actionPerformed(final ActionEvent e) {
          final DeleteEvent de = (DeleteEvent) e;
          assertEquals(toDelete, de.getDeletedEntities());
        }
      });
      editModel.setDeleteAllowed(false);
      assertFalse(editModel.isDeleteAllowed());
      try {
        editModel.delete();
        fail("Should not be able to delete");
      }
      catch (Exception e) {}
      editModel.setDeleteAllowed(true);
      assertTrue(editModel.isDeleteAllowed());

      editModel.delete();
    }
    finally {
      editModel.getDbProvider().getEntityDb().rollbackTransaction();
    }
  }
}
