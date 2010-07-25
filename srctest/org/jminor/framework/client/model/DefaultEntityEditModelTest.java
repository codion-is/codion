/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model;

import org.jminor.common.model.DateUtil;
import org.jminor.common.model.State;
import org.jminor.common.model.valuemap.DefaultValueChangeMapEditModel;
import org.jminor.common.model.valuemap.exception.ValidationException;
import org.jminor.framework.client.model.event.DeleteEvent;
import org.jminor.framework.client.model.event.InsertEvent;
import org.jminor.framework.client.model.event.UpdateEvent;
import org.jminor.framework.db.EntityDbConnectionTest;
import org.jminor.framework.demos.empdept.domain.EmpDept;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.EntityRepository;
import org.jminor.framework.domain.Property;
import org.jminor.framework.i18n.FrameworkMessages;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class DefaultEntityEditModelTest {

  private DefaultEntityEditModel editModel = new DefaultEntityEditModel(EmpDept.T_EMPLOYEE, EntityDbConnectionTest.DB_PROVIDER);

  @Before
  public void setUp() {
    new EmpDept();
  }

  @Test
  public void test() throws Exception {
    try {
      new DefaultEntityEditModel(null, EntityDbConnectionTest.DB_PROVIDER);
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

    final State entityNullState = editModel.stateEntityNull();

    assertTrue(entityNullState.isActive());

    editModel.refresh();
    assertTrue(editModel.isEntityNew());
    assertFalse(editModel.stateModified().isActive());

    //todo
//    employeeModel.getTableModel().setSelectedItemIndex(0);
//
//    assertFalse(entityNullState.isActive());
//
//    assertTrue("Active entity is not equal to the selected entity",
//            editModel.getEntityCopy().propertyValuesEqual(employeeModel.getTableModel().getItemAtViewIndex(0)));
//
//    assertFalse("Active entity is new after an entity is selected", editModel.isEntityNew());
//    assertFalse(editModel.stateModified().isActive());
//    employeeModel.getTableModel().clearSelection();
//    assertTrue("Active entity is new null after selection is cleared", editModel.isEntityNew());
//    assertFalse(editModel.stateModified().isActive());
//    assertTrue("Active entity is not null after selection is cleared", editModel.getEntityCopy().isNull());
//
//    employeeModel.getTableModel().setSelectedItemIndex(0);
//    assertTrue("Active entity is null after selection is made", !editModel.getEntityCopy().isNull());

    final Double originalCommission = (Double) editModel.getValue(EmpDept.EMPLOYEE_COMMISSION);
    final double commission = 1500.5;
    final Date originalHiredate = (Date) editModel.getValue(EmpDept.EMPLOYEE_HIREDATE);
    final Date hiredate = new Date();
    final String originalName = (String) editModel.getValue(EmpDept.EMPLOYEE_NAME);
    final String name = "Mr. Mr";

    editModel.setValue(EmpDept.EMPLOYEE_COMMISSION, commission);
    assertTrue(editModel.stateModified().isActive());
    editModel.setValue(EmpDept.EMPLOYEE_HIREDATE, hiredate);
    editModel.setValue(EmpDept.EMPLOYEE_NAME, name);

    assertEquals("Commission does not fit", editModel.getValue(EmpDept.EMPLOYEE_COMMISSION), commission);
    assertEquals("Hiredate does not fit", editModel.getValue(EmpDept.EMPLOYEE_HIREDATE), hiredate);
    assertEquals("Name does not fit", editModel.getValue(EmpDept.EMPLOYEE_NAME), name);

    editModel.setValue(EmpDept.EMPLOYEE_COMMISSION, originalCommission);
    assertTrue(editModel.isEntityModified());
    assertTrue(editModel.stateModified().isActive());
    editModel.setValue(EmpDept.EMPLOYEE_HIREDATE, originalHiredate);
    assertTrue(editModel.isEntityModified());
    editModel.setValue(EmpDept.EMPLOYEE_NAME, originalName);
    assertFalse(editModel.isEntityModified());

    //test validation
    try {
      editModel.setValue(EmpDept.EMPLOYEE_COMMISSION, 50d);
      editModel.validate(EmpDept.EMPLOYEE_COMMISSION, DefaultValueChangeMapEditModel.INSERT);
      fail("Validation should fail on invalid commission value");
    }
    catch (ValidationException e) {
      assertEquals(EmpDept.EMPLOYEE_COMMISSION, e.getKey());
      assertEquals(50d, e.getValue());
      final Property property = EntityRepository.getProperty(EmpDept.T_EMPLOYEE, (String) e.getKey());
      assertEquals("Validation message should fit", "'" + property + "' " +
              FrameworkMessages.get(FrameworkMessages.PROPERTY_VALUE_TOO_SMALL) + " " + property.getMin(), e.getMessage());
    }

    editModel.setValueMap(null);
    assertTrue("Active entity is not null after model is cleared", editModel.getEntityCopy().isNull());
  }

  @Test
  public void insert() throws Exception {
    try {
      editModel.getDbProvider().getEntityDb().beginTransaction();
      editModel.setValue(EmpDept.EMPLOYEE_COMMISSION, 1000d);
      editModel.setValue(EmpDept.EMPLOYEE_HIREDATE, DateUtil.floorDate(new Date()));
      editModel.setValue(EmpDept.EMPLOYEE_JOB, "A Jobby");
      editModel.setValue(EmpDept.EMPLOYEE_NAME, "Björn");
      editModel.setValue(EmpDept.EMPLOYEE_SALARY, 1000d);

      Entity department = Entities.entityInstance(EmpDept.T_DEPARTMENT);
      department.setValue(EmpDept.DEPARTMENT_ID, 99);
      department.setValue(EmpDept.DEPARTMENT_LOCATION, "Limbo");
      department.setValue(EmpDept.DEPARTMENT_NAME, "Judgment");

      department = editModel.getDbProvider().getEntityDb().selectSingle(editModel.getDbProvider().getEntityDb().insert(Arrays.asList(department)).get(0));

      editModel.setValue(EmpDept.EMPLOYEE_DEPARTMENT_FK, department);

      final Entity toInsert = editModel.getEntityCopy();
      editModel.eventAfterInsert().addListener(new ActionListener() {
        public void actionPerformed(final ActionEvent e) {
          try {
            final InsertEvent insertEvent = (InsertEvent) e;
            final Entity inserted = editModel.getDbProvider().getEntityDb().selectSingle(insertEvent.getInsertedKeys().get(0));
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
    try {
      editModel.getDbProvider().getEntityDb().beginTransaction();
      editModel.setEntity(editModel.getDbProvider().getEntityDb().selectSingle(EmpDept.T_EMPLOYEE, EmpDept.EMPLOYEE_NAME, "MILLER"));
      editModel.setValue(EmpDept.EMPLOYEE_NAME, "BJORN");
      final List<Entity> toUpdate = Arrays.asList(editModel.getEntityCopy());
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
      assertFalse(editModel.stateModified().isActive());
    }
    finally {
      editModel.getDbProvider().getEntityDb().rollbackTransaction();
    }
  }

  @Test
  public void delete() throws Exception {
    try {
      editModel.getDbProvider().getEntityDb().beginTransaction();
      editModel.setEntity(editModel.getDbProvider().getEntityDb().selectSingle(EmpDept.T_EMPLOYEE, EmpDept.EMPLOYEE_NAME, "MILLER"));
      final List<Entity> toDelete = Arrays.asList(editModel.getEntityCopy());
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
