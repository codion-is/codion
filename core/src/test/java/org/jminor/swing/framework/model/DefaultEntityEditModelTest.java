/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.framework.model;

import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.model.CancelException;
import org.jminor.common.model.DateUtil;
import org.jminor.common.model.EventInfoListener;
import org.jminor.common.model.EventListener;
import org.jminor.common.model.State;
import org.jminor.common.model.StateObserver;
import org.jminor.common.model.valuemap.exception.ValidationException;
import org.jminor.framework.Configuration;
import org.jminor.framework.db.EntityConnection;
import org.jminor.framework.db.EntityConnectionProvidersTest;
import org.jminor.framework.db.criteria.EntityCriteriaUtil;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.Property;
import org.jminor.framework.domain.TestDomain;
import org.jminor.framework.i18n.FrameworkMessages;
import org.jminor.swing.common.model.combobox.FilteredComboBoxModel;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.*;

public final class DefaultEntityEditModelTest {

  private DefaultEntityEditModel employeeEditModel;
  private Property.ColumnProperty jobProperty;
  private Property.ForeignKeyProperty deptProperty;

  @Before
  public void setUp() {
    TestDomain.init();
    jobProperty = Entities.getColumnProperty(TestDomain.T_EMP, TestDomain.EMP_JOB);
    deptProperty = Entities.getForeignKeyProperty(TestDomain.T_EMP, TestDomain.EMP_DEPARTMENT_FK);
    employeeEditModel = new DefaultEntityEditModel(TestDomain.T_EMP, EntityConnectionProvidersTest.CONNECTION_PROVIDER) {
      @Override
      public Object getDefaultValue(final Property property) {
        if (property.is(TestDomain.EMP_HIREDATE)) {
          return DateUtil.floorDate(new Date());
        }

        return super.getDefaultValue(property);
      }
    };
  }

  @Test
  public void getComboBoxModel() {
    final FilteredComboBoxModel<String> model = employeeEditModel.getComboBoxModel(jobProperty);
    model.setNullValue("null");
    assertNotNull(model);
    assertTrue(employeeEditModel.containsComboBoxModel(jobProperty.getPropertyID()));
    assertEquals(model, employeeEditModel.getComboBoxModel(jobProperty));
    employeeEditModel.refreshComboBoxModels();
    employeeEditModel.clearComboBoxModels();
    assertTrue(employeeEditModel.getComboBoxModel(jobProperty).isCleared());
    employeeEditModel.refreshComboBoxModels();
    employeeEditModel.clear();
    assertTrue(employeeEditModel.getComboBoxModel(jobProperty).isCleared());
  }

  @Test
  public void getForeignKeyComboBoxModel() {
    assertFalse(employeeEditModel.containsComboBoxModel(deptProperty.getPropertyID()));
    final EntityComboBoxModel model = employeeEditModel.getForeignKeyComboBoxModel(deptProperty);
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
  public void createForeignKeyComboBoxModel() {
    final EntityComboBoxModel model = employeeEditModel.createForeignKeyComboBoxModel(deptProperty);
    assertNotNull(model);
    assertTrue(model.isCleared());
    assertTrue(model.getAllItems().isEmpty());
    assertEquals(deptProperty.getReferencedEntityID(), model.getEntityID());
  }

  @Test(expected = IllegalArgumentException.class)
  public void getForeignKeyComboBoxModelNonFKProperty() {
    employeeEditModel.getForeignKeyComboBoxModel(jobProperty.getPropertyID());
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
      employee.setValue(TestDomain.EMP_NAME, "NOONE");
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
    final Entity employee = employeeEditModel.getConnectionProvider().getConnection().selectSingle(TestDomain.T_EMP, TestDomain.EMP_NAME, "MARTIN");
    employeeEditModel.setEntity(employee);
    final Entity copyWithPrimaryKeyValue = employeeEditModel.getEntityCopy();
    assertEquals(employee, copyWithPrimaryKeyValue);
    assertFalse(copyWithPrimaryKeyValue.isPrimaryKeyNull());
    final Entity copyWithoutPrimaryKeyValue = employeeEditModel.getEntityCopy(false);
    assertTrue(copyWithoutPrimaryKeyValue.isPrimaryKeyNull());
  }

  @Test(expected = IllegalArgumentException.class)
  public void constructorNullEntityID() {
    new DefaultEntityEditModel(null, EntityConnectionProvidersTest.CONNECTION_PROVIDER);
  }

  @Test(expected = IllegalArgumentException.class)
  public void constructorNullConnectionProvider() {
    new DefaultEntityEditModel("entityID", null);
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

    final EventInfoListener infoListener = new EventInfoListener() {
      @Override
      public void eventOccurred(final Object info) {}
    };
    employeeEditModel.addAfterDeleteListener(infoListener);
    employeeEditModel.addAfterInsertListener(infoListener);
    employeeEditModel.addAfterUpdateListener(infoListener);
    employeeEditModel.addBeforeDeleteListener(infoListener);
    employeeEditModel.addBeforeInsertListener(infoListener);
    employeeEditModel.addBeforeUpdateListener(infoListener);
    final EventListener listener = new EventListener() {
      @Override
      public void eventOccurred() {}
    };
    employeeEditModel.addEntitiesChangedListener(listener);
    employeeEditModel.addBeforeRefreshListener(listener);
    employeeEditModel.addAfterRefreshListener(listener);

    assertEquals(TestDomain.T_EMP, employeeEditModel.getEntityID());
    assertEquals(employeeEditModel.getConnectionProvider().getConnection().selectValues(TestDomain.EMP_JOB, EntityCriteriaUtil.criteria(TestDomain.T_EMP)),
            employeeEditModel.getValueProvider(jobProperty).getValues());

    employeeEditModel.refresh();
    assertTrue(employeeEditModel.isEntityNew());
    assertFalse(employeeEditModel.getModifiedObserver().isActive());

    final Entity employee = employeeEditModel.getConnectionProvider().getConnection().selectSingle(TestDomain.T_EMP, TestDomain.EMP_NAME, "MARTIN");
    employeeEditModel.setEntity(employee);
    assertFalse(primaryKeyNullState.isActive());
    assertFalse(entityNewState.isActive());

    assertTrue("Active entity is not equal to the entity just set", employeeEditModel.getEntityCopy().propertyValuesEqual(employee));
    assertFalse("Active entity is new after an entity is set", employeeEditModel.isEntityNew());
    assertFalse(employeeEditModel.getModifiedObserver().isActive());
    employeeEditModel.setEntity(null);
    assertTrue("Active entity is new after entity is set to null", employeeEditModel.isEntityNew());
    assertFalse(employeeEditModel.getModifiedObserver().isActive());
    assertTrue("Active entity primary key is not null after entity is set to null", employeeEditModel.getEntityCopy().isPrimaryKeyNull());

    employeeEditModel.setEntity(employee);
    assertTrue("Active entity primary key is null after entity is set", !employeeEditModel.getEntityCopy().isPrimaryKeyNull());

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
    assertTrue("Active entity is not null after model is cleared", employeeEditModel.getEntityCopy().isPrimaryKeyNull());

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
      assertTrue(employeeEditModel.insert(new ArrayList<Entity>()).isEmpty());
      employeeEditModel.getConnectionProvider().getConnection().beginTransaction();
      employeeEditModel.setValue(TestDomain.EMP_COMMISSION, 1000d);
      employeeEditModel.setValue(TestDomain.EMP_HIREDATE, DateUtil.floorDate(new Date()));
      employeeEditModel.setValue(TestDomain.EMP_JOB, "A Jobby");
      employeeEditModel.setValue(TestDomain.EMP_NAME, "Björn");
      employeeEditModel.setValue(TestDomain.EMP_SALARY, 1000d);

      final Entity tmpDept = Entities.entity(TestDomain.T_DEPARTMENT);
      tmpDept.setValue(TestDomain.DEPARTMENT_ID, 99);
      tmpDept.setValue(TestDomain.DEPARTMENT_LOCATION, "Limbo");
      tmpDept.setValue(TestDomain.DEPARTMENT_NAME, "Judgment");

      final Entity department = employeeEditModel.getConnectionProvider().getConnection().selectSingle(employeeEditModel.getConnectionProvider().getConnection().insert(Collections.singletonList(tmpDept)).get(0));

      employeeEditModel.setValue(TestDomain.EMP_DEPARTMENT_FK, department);

      employeeEditModel.addAfterInsertListener(new EventInfoListener<EntityEditModel.InsertEvent>() {
        @Override
        public void eventOccurred(final EntityEditModel.InsertEvent info) {
          assertEquals(department, info.getInsertedEntities().get(0).getValue(TestDomain.EMP_DEPARTMENT_FK));
        }
      });
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
      assertFalse(entityCopy.getPrimaryKey().isNull());
      assertEquals(entityCopy.getPrimaryKey(), entityCopy.getOriginalPrimaryKey());

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
      assertTrue(employeeEditModel.update(new ArrayList<Entity>()).isEmpty());
      employeeEditModel.getConnectionProvider().getConnection().beginTransaction();
      employeeEditModel.setEntity(employeeEditModel.getConnectionProvider().getConnection().selectSingle(TestDomain.T_EMP, TestDomain.EMP_NAME, "MILLER"));
      employeeEditModel.setValue(TestDomain.EMP_NAME, "BJORN");
      final List<Entity> toUpdate = Collections.singletonList(employeeEditModel.getEntityCopy());
      final EventInfoListener<EntityEditModel.UpdateEvent> listener = new EventInfoListener<EntityEditModel.UpdateEvent>() {
        @Override
        public void eventOccurred(final EntityEditModel.UpdateEvent info) {
          assertEquals(toUpdate, new ArrayList<>(info.getUpdatedEntities().values()));
        }
      };
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
      assertTrue(employeeEditModel.delete(new ArrayList<Entity>()).isEmpty());
      employeeEditModel.getConnectionProvider().getConnection().beginTransaction();
      employeeEditModel.setEntity(employeeEditModel.getConnectionProvider().getConnection().selectSingle(TestDomain.T_EMP, TestDomain.EMP_NAME, "MILLER"));
      final List<Entity> toDelete = Collections.singletonList(employeeEditModel.getEntityCopy());
      employeeEditModel.addAfterDeleteListener(new EventInfoListener<EntityEditModel.DeleteEvent>() {
        @Override
        public void eventOccurred(final EntityEditModel.DeleteEvent info) {
          assertEquals(toDelete, info.getDeletedEntities());
        }
      });
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
    king.setValue(TestDomain.EMP_MGR_FK, null);
    employeeEditModel.setEntity(king);
    assertNull(employeeEditModel.getValue(TestDomain.EMP_MGR_FK));
    employeeEditModel.setEntity(null);
    assertEquals(DateUtil.floorDate(new Date()), employeeEditModel.getValue(TestDomain.EMP_HIREDATE));
    assertFalse(employeeEditModel.getEntity().isModified(TestDomain.EMP_HIREDATE));
    assertFalse(employeeEditModel.getEntity().isModified());
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

    final EventInfoListener<State> alwaysConfirmListener = new EventInfoListener<State>() {
      @Override
      public void eventOccurred(final State info) {
        info.setActive(true);
      }
    };
    final EventInfoListener<State> alwaysDenyListener = new EventInfoListener<State>() {
      @Override
      public void eventOccurred(final State info) {
        info.setActive(false);
      }
    };

    employeeEditModel.addConfirmSetEntityObserver(alwaysConfirmListener);
    final Entity king = employeeEditModel.getConnectionProvider().getConnection().selectSingle(TestDomain.T_EMP, TestDomain.EMP_NAME, "KING");
    final Entity adams = employeeEditModel.getConnectionProvider().getConnection().selectSingle(TestDomain.T_EMP, TestDomain.EMP_NAME, "ADAMS");
    employeeEditModel.setEntity(king);
    employeeEditModel.setValue(TestDomain.EMP_NAME, "New name");
    employeeEditModel.setEntity(adams);
    assertEquals(adams, employeeEditModel.getEntity());

    employeeEditModel.removeConfirmSetEntityObserver(alwaysConfirmListener);
    employeeEditModel.setEntity(null);
    employeeEditModel.addConfirmSetEntityObserver(alwaysDenyListener);

    employeeEditModel.setValue(TestDomain.EMP_NAME, "A name");
    employeeEditModel.setEntity(king);
    assertEquals("A name", employeeEditModel.getValue(TestDomain.EMP_NAME));

    employeeEditModel.removeConfirmSetEntityObserver(alwaysDenyListener);
    employeeEditModel.setEntity(null);
    employeeEditModel.addConfirmSetEntityObserver(alwaysDenyListener);

    employeeEditModel.setValue(TestDomain.EMP_DEPARTMENT_FK, king.getValue(TestDomain.EMP_DEPARTMENT_FK));
    employeeEditModel.setEntity(adams);
    assertEquals(king.getValue(TestDomain.EMP_DEPARTMENT_FK), employeeEditModel.getValue(TestDomain.EMP_DEPARTMENT_FK));

    Configuration.setValue(Configuration.WARN_ABOUT_UNSAVED_DATA, false);
  }
}
