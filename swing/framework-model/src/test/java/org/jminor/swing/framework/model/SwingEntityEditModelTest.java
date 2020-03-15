/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.framework.model;

import org.jminor.common.db.Databases;
import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.model.combobox.FilteredComboBoxModel;
import org.jminor.common.user.User;
import org.jminor.common.user.Users;
import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.db.local.LocalEntityConnectionProvider;
import org.jminor.framework.domain.Domain;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.property.ColumnProperty;
import org.jminor.framework.domain.property.ForeignKeyProperty;
import org.jminor.framework.model.EntityComboBoxModel;
import org.jminor.framework.model.TestDomain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.*;

public class SwingEntityEditModelTest {

  private static final Domain DOMAIN = new TestDomain();

  private static final User UNIT_TEST_USER =
          Users.parseUser(System.getProperty("jminor.test.user", "scott:tiger"));
  private static final EntityConnectionProvider CONNECTION_PROVIDER = new LocalEntityConnectionProvider(
          Databases.getInstance()).setDomainClassName(TestDomain.class.getName()).setUser(UNIT_TEST_USER);

  private SwingEntityEditModel employeeEditModel;
  private ColumnProperty jobProperty;
  private ForeignKeyProperty deptProperty;

  @BeforeEach
  public void setUp() {
    jobProperty = DOMAIN.getDefinition(TestDomain.T_EMP).getColumnProperty(TestDomain.EMP_JOB);
    deptProperty = DOMAIN.getDefinition(TestDomain.T_EMP).getForeignKeyProperty(TestDomain.EMP_DEPARTMENT_FK);
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

  @Test
  public void getForeignKeyComboBoxModelNonFKProperty() {
    assertThrows(IllegalArgumentException.class, () -> employeeEditModel.getForeignKeyComboBoxModel(jobProperty.getPropertyId()));
  }

  @Test
  public void replaceForeignKeyValues() throws DatabaseException {
    final Entity blake = employeeEditModel.getConnectionProvider().getConnection()
            .selectSingle(TestDomain.T_EMP, TestDomain.EMP_NAME, "BLAKE");
    employeeEditModel.getForeignKeyComboBoxModel(TestDomain.EMP_MGR_FK);
    employeeEditModel.refreshComboBoxModels();
    assertNotSame(employeeEditModel.getForeignKeyComboBoxModel(TestDomain.EMP_MGR_FK)
            .getEntity(blake.getKey()), blake);
    employeeEditModel.replaceForeignKeyValues(singletonList(blake));
    assertSame(employeeEditModel.getForeignKeyComboBoxModel(TestDomain.EMP_MGR_FK)
            .getEntity(blake.getKey()), blake);
  }
}
