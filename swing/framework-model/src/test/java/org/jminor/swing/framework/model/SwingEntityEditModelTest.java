/*
 * Chinook.Copyright (c) 2004 - 2018, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.framework.model;

import org.jminor.common.User;
import org.jminor.common.db.Databases;
import org.jminor.common.model.combobox.FilteredComboBoxModel;
import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.db.local.LocalEntityConnectionProvider;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.Property;
import org.jminor.framework.model.EntityComboBoxModel;
import org.jminor.framework.model.testing.TestDomain;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class SwingEntityEditModelTest {

  private static final Entities ENTITIES = new TestDomain();

  private static final EntityConnectionProvider CONNECTION_PROVIDER = new LocalEntityConnectionProvider(ENTITIES, new User(
          System.getProperty("jminor.unittest.username", "scott"),
          System.getProperty("jminor.unittest.password", "tiger")), Databases.getInstance());

  private SwingEntityEditModel employeeEditModel;
  private Property.ColumnProperty jobProperty;
  private Property.ForeignKeyProperty deptProperty;

  @Before
  public void setUp() {
    jobProperty = ENTITIES.getColumnProperty(TestDomain.T_EMP, TestDomain.EMP_JOB);
    deptProperty = ENTITIES.getForeignKeyProperty(TestDomain.T_EMP, TestDomain.EMP_DEPARTMENT_FK);
    employeeEditModel = new SwingEntityEditModel(TestDomain.T_EMP, CONNECTION_PROVIDER);
  }

  @Test
  public void getComboBoxModel() {
    final FilteredComboBoxModel<String> model = (FilteredComboBoxModel<String>) employeeEditModel.getComboBoxModel(jobProperty.getPropertyId());
    model.setNullValue("null");
    assertNotNull(model);
    assertTrue(employeeEditModel.containsComboBoxModel(jobProperty.getPropertyId()));
    assertEquals(model, employeeEditModel.getComboBoxModel(jobProperty.getPropertyId()));
    employeeEditModel.refreshComboBoxModels();
    employeeEditModel.clearComboBoxModels();
    assertTrue(employeeEditModel.getComboBoxModel(jobProperty.getPropertyId()).isCleared());
    employeeEditModel.refreshComboBoxModels();
    employeeEditModel.clear();
    assertTrue(employeeEditModel.getComboBoxModel(jobProperty.getPropertyId()).isCleared());
  }

  @Test
  public void getForeignKeyComboBoxModel() {
    assertFalse(employeeEditModel.containsComboBoxModel(deptProperty.getPropertyId()));
    final EntityComboBoxModel model = employeeEditModel.getForeignKeyComboBoxModel(deptProperty);
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
    assertEquals(deptProperty.getForeignEntityId(), model.getEntityId());
  }

  @Test(expected = IllegalArgumentException.class)
  public void getForeignKeyComboBoxModelNonFKProperty() {
    employeeEditModel.getForeignKeyComboBoxModel(jobProperty.getPropertyId());
  }
}
