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
  void comboBoxModel() {
    FilteredComboBoxModel<String> model = employeeEditModel.comboBoxModel(TestDomain.EMP_JOB);
    model.setIncludeNull(true);
    model.setNullItem("null");
    assertNotNull(model);
    assertTrue(employeeEditModel.containsComboBoxModel(TestDomain.EMP_JOB));
    assertEquals(model, employeeEditModel.comboBoxModel(TestDomain.EMP_JOB));
    employeeEditModel.refreshComboBoxModels();
    employeeEditModel.clearComboBoxModels();
    assertTrue(employeeEditModel.comboBoxModel(TestDomain.EMP_JOB).isCleared());
    employeeEditModel.refreshComboBoxModels();
    employeeEditModel.clear();
    assertTrue(employeeEditModel.comboBoxModel(TestDomain.EMP_JOB).isCleared());
  }

  @Test
  void foreignKeyComboBoxModel() {
    assertFalse(employeeEditModel.containsComboBoxModel(TestDomain.EMP_DEPARTMENT_FK));
    EntityComboBoxModel model = employeeEditModel.foreignKeyComboBoxModel(TestDomain.EMP_DEPARTMENT_FK);
    assertNotNull(model);
    assertTrue(model.isCleared());
    assertTrue(model.items().isEmpty());
    employeeEditModel.refreshComboBoxModels();
    assertFalse(model.isCleared());
    assertFalse(model.items().isEmpty());
    employeeEditModel.clearComboBoxModels();
    assertTrue(model.isCleared());
    assertTrue(model.items().isEmpty());
  }

  @Test
  void createForeignKeyComboBoxModel() {
    EntityComboBoxModel model = employeeEditModel.createForeignKeyComboBoxModel(TestDomain.EMP_DEPARTMENT_FK);
    assertNotNull(model);
    assertTrue(model.isCleared());
    assertTrue(model.items().isEmpty());
    ForeignKeyProperty deptProperty = employeeEditModel.entities().definition(TestDomain.T_EMP).foreignKeyProperty(TestDomain.EMP_DEPARTMENT_FK);
    assertEquals(deptProperty.referencedType(), model.entityType());
    model.refresh();
    for (Entity department : model.items()) {
      assertTrue(department.contains(TestDomain.DEPARTMENT_ID));
      assertTrue(department.contains(TestDomain.DEPARTMENT_NAME));
      assertFalse(department.contains(TestDomain.DEPARTMENT_LOCATION));
    }
  }

  @Test
  void replaceForeignKeyValues() throws DatabaseException {
    Entity blake = employeeEditModel.connectionProvider().connection()
            .selectSingle(TestDomain.EMP_NAME, "BLAKE");
    employeeEditModel.foreignKeyComboBoxModel(TestDomain.EMP_MGR_FK);
    employeeEditModel.refreshComboBoxModels();
    assertNotSame(employeeEditModel.foreignKeyComboBoxModel(TestDomain.EMP_MGR_FK)
            .entity(blake.primaryKey()).orElse(null), blake);
    employeeEditModel.replaceForeignKeyValues(singletonList(blake));
    assertSame(employeeEditModel.foreignKeyComboBoxModel(TestDomain.EMP_MGR_FK)
            .entity(blake.primaryKey()).orElse(null), blake);
  }

  @Test
  void initializeComboBoxModels() {
    employeeEditModel.initializeComboBoxModels(TestDomain.EMP_DEPARTMENT_FK, TestDomain.EMP_MGR_FK, TestDomain.EMP_JOB);
    assertFalse(employeeEditModel.comboBoxModel(TestDomain.EMP_JOB).isCleared());
    assertFalse(employeeEditModel.foreignKeyComboBoxModel(TestDomain.EMP_DEPARTMENT_FK).isCleared());
    assertFalse(employeeEditModel.foreignKeyComboBoxModel(TestDomain.EMP_MGR_FK).isCleared());
  }
}
