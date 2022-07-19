/*
 * Copyright (c) 2016 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.model;

import is.codion.common.db.exception.DatabaseException;
import is.codion.common.model.combobox.FilteredComboBoxModel;
import is.codion.common.user.User;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.local.LocalEntityConnectionProvider;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.property.ForeignKeyProperty;
import is.codion.framework.model.EntityComboBoxModel;
import is.codion.framework.model.test.TestDomain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.*;

public class SwingEntityEditModelTest {

  private static final User UNIT_TEST_USER =
          User.parse(System.getProperty("codion.test.user", "scott:tiger"));

  private static final EntityConnectionProvider CONNECTION_PROVIDER = LocalEntityConnectionProvider.builder()
            .domainClassName(TestDomain.class.getName())
            .user(UNIT_TEST_USER)
            .build();

  private SwingEntityEditModel employeeEditModel;

  @BeforeEach
  void setUp() {
    employeeEditModel = new SwingEntityEditModel(TestDomain.T_EMP, CONNECTION_PROVIDER);
  }

  @Test
  void getComboBoxModel() {
    FilteredComboBoxModel<String> model = employeeEditModel.getComboBoxModel(TestDomain.EMP_JOB);
    model.setNullString("null");
    assertNotNull(model);
    assertTrue(employeeEditModel.containsComboBoxModel(TestDomain.EMP_JOB));
    assertEquals(model, employeeEditModel.getComboBoxModel(TestDomain.EMP_JOB));
    employeeEditModel.refreshComboBoxModels();
    employeeEditModel.clearComboBoxModels();
    assertTrue(employeeEditModel.getComboBoxModel(TestDomain.EMP_JOB).isCleared());
    employeeEditModel.refreshComboBoxModels();
    employeeEditModel.clear();
    assertTrue(employeeEditModel.getComboBoxModel(TestDomain.EMP_JOB).isCleared());
  }

  @Test
  void getForeignKeyComboBoxModel() {
    assertFalse(employeeEditModel.containsComboBoxModel(TestDomain.EMP_DEPARTMENT_FK));
    EntityComboBoxModel model = employeeEditModel.getForeignKeyComboBoxModel(TestDomain.EMP_DEPARTMENT_FK);
    assertNotNull(model);
    assertTrue(model.isCleared());
    assertTrue(model.getItems().isEmpty());
    employeeEditModel.refreshComboBoxModels();
    assertFalse(model.isCleared());
    assertFalse(model.getItems().isEmpty());
    employeeEditModel.clearComboBoxModels();
    assertTrue(model.isCleared());
    assertTrue(model.getItems().isEmpty());
  }

  @Test
  void createForeignKeyComboBoxModel() {
    EntityComboBoxModel model = employeeEditModel.createForeignKeyComboBoxModel(TestDomain.EMP_DEPARTMENT_FK);
    assertNotNull(model);
    assertTrue(model.isCleared());
    assertTrue(model.getItems().isEmpty());
    ForeignKeyProperty deptProperty = employeeEditModel.getEntities().getDefinition(TestDomain.T_EMP).getForeignKeyProperty(TestDomain.EMP_DEPARTMENT_FK);
    assertEquals(deptProperty.getReferencedEntityType(), model.getEntityType());
    model.refresh();
    for (Entity department : model.getItems()) {
      assertTrue(department.contains(TestDomain.DEPARTMENT_ID));
      assertTrue(department.contains(TestDomain.DEPARTMENT_NAME));
      assertFalse(department.contains(TestDomain.DEPARTMENT_LOCATION));
    }
  }

  @Test
  void replaceForeignKeyValues() throws DatabaseException {
    Entity blake = employeeEditModel.getConnectionProvider().getConnection()
            .selectSingle(TestDomain.EMP_NAME, "BLAKE");
    employeeEditModel.getForeignKeyComboBoxModel(TestDomain.EMP_MGR_FK);
    employeeEditModel.refreshComboBoxModels();
    assertNotSame(employeeEditModel.getForeignKeyComboBoxModel(TestDomain.EMP_MGR_FK)
            .getEntity(blake.getPrimaryKey()).orElse(null), blake);
    employeeEditModel.replaceForeignKeyValues(singletonList(blake));
    assertSame(employeeEditModel.getForeignKeyComboBoxModel(TestDomain.EMP_MGR_FK)
            .getEntity(blake.getPrimaryKey()).orElse(null), blake);
  }

  @Test
  void initializeComboBoxModels() {
    employeeEditModel.initializeComboBoxModels(TestDomain.EMP_DEPARTMENT_FK, TestDomain.EMP_MGR_FK, TestDomain.EMP_JOB);
    assertFalse(employeeEditModel.getComboBoxModel(TestDomain.EMP_JOB).isCleared());
    assertFalse(employeeEditModel.getForeignKeyComboBoxModel(TestDomain.EMP_DEPARTMENT_FK).isCleared());
    assertFalse(employeeEditModel.getForeignKeyComboBoxModel(TestDomain.EMP_MGR_FK).isCleared());
  }
}
