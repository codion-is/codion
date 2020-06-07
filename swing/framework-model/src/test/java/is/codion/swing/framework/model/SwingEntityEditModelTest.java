/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.model;

import is.codion.common.db.database.Databases;
import is.codion.common.db.exception.DatabaseException;
import is.codion.common.model.combobox.FilteredComboBoxModel;
import is.codion.common.user.User;
import is.codion.common.user.Users;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.local.LocalEntityConnectionProvider;
import is.codion.framework.domain.Domain;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.property.ColumnProperty;
import is.codion.framework.domain.property.ForeignKeyProperty;
import is.codion.framework.model.EntityComboBoxModel;
import is.codion.framework.model.tests.TestDomain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.*;

public class SwingEntityEditModelTest {

  private static final Domain DOMAIN = new TestDomain();

  private static final User UNIT_TEST_USER =
          Users.parseUser(System.getProperty("codion.test.user", "scott:tiger"));
  private static final EntityConnectionProvider CONNECTION_PROVIDER = new LocalEntityConnectionProvider(
          Databases.getInstance()).setDomainClassName(TestDomain.class.getName()).setUser(UNIT_TEST_USER);

  private SwingEntityEditModel employeeEditModel;
  private ColumnProperty<String> jobProperty;
  private ForeignKeyProperty deptProperty;

  @BeforeEach
  public void setUp() {
    jobProperty = DOMAIN.getDefinition(TestDomain.T_EMP).getColumnProperty(TestDomain.EMP_JOB);
    deptProperty = DOMAIN.getDefinition(TestDomain.T_EMP).getForeignKeyProperty(TestDomain.EMP_DEPARTMENT_FK);
    employeeEditModel = new SwingEntityEditModel(TestDomain.T_EMP, CONNECTION_PROVIDER);
  }

  @Test
  public void getComboBoxModel() {
    final FilteredComboBoxModel<String> model = employeeEditModel.getComboBoxModel(jobProperty.getAttribute());
    model.setNullString("null");
    assertNotNull(model);
    assertTrue(employeeEditModel.containsComboBoxModel(jobProperty.getAttribute()));
    assertEquals(model, employeeEditModel.getComboBoxModel(jobProperty.getAttribute()));
    employeeEditModel.refreshComboBoxModels();
    employeeEditModel.clearComboBoxModels();
    assertTrue(employeeEditModel.getComboBoxModel(jobProperty.getAttribute()).isCleared());
    employeeEditModel.refreshComboBoxModels();
    employeeEditModel.clear();
    assertTrue(employeeEditModel.getComboBoxModel(jobProperty.getAttribute()).isCleared());
  }

  @Test
  public void getForeignKeyComboBoxModel() {
    assertFalse(employeeEditModel.containsComboBoxModel(deptProperty.getAttribute()));
    final EntityComboBoxModel model = employeeEditModel.getForeignKeyComboBoxModel(deptProperty);
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
  public void createForeignKeyComboBoxModel() {
    final EntityComboBoxModel model = employeeEditModel.createForeignKeyComboBoxModel(deptProperty);
    assertNotNull(model);
    assertTrue(model.isCleared());
    assertTrue(model.getItems().isEmpty());
    assertEquals(deptProperty.getReferencedEntityType(), model.getEntityType());
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
