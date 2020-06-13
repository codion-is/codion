/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.model;

import is.codion.common.db.database.Databases;
import is.codion.common.db.exception.DatabaseException;
import is.codion.common.user.User;
import is.codion.common.user.Users;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.local.LocalEntityConnectionProvider;
import is.codion.framework.domain.Domain;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.model.DefaultEntityTableConditionModel;
import is.codion.framework.model.DefaultFilterModelFactory;
import is.codion.framework.model.EntityTableConditionModel;
import is.codion.framework.model.tests.TestDomain;

import org.junit.jupiter.api.Test;

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;

public class SwingForeignKeyConditionModelTest {

  private static final Domain DOMAIN = new TestDomain();

  private static final User UNIT_TEST_USER =
          Users.parseUser(System.getProperty("codion.test.user", "scott:tiger"));
  private static final EntityConnectionProvider CONNECTION_PROVIDER = new LocalEntityConnectionProvider(
          Databases.getInstance()).setDomainClassName(TestDomain.class.getName()).setUser(UNIT_TEST_USER);

  private final EntityTableConditionModel conditionModel = new DefaultEntityTableConditionModel(TestDomain.T_EMP,
          CONNECTION_PROVIDER, new DefaultFilterModelFactory(),
          new SwingConditionModelFactory());

  @Test
  public void refresh() {
    conditionModel.refresh();
    assertTrue(((SwingForeignKeyConditionModel) conditionModel.getConditionModel(TestDomain.EMP_DEPARTMENT_FK))
            .getEntityComboBoxModel().getSize() > 1);
    conditionModel.clear();
    assertEquals(0, ((SwingForeignKeyConditionModel) conditionModel.getConditionModel(TestDomain.EMP_DEPARTMENT_FK))
            .getEntityComboBoxModel().getSize());
  }

  @Test
  public void getSearchEntitiesComboBoxModel() throws DatabaseException {
    final SwingEntityComboBoxModel comboBoxModel = new SwingEntityComboBoxModel(TestDomain.T_DEPARTMENT, CONNECTION_PROVIDER);
    final SwingForeignKeyConditionModel conditionModel = new SwingForeignKeyConditionModel(
            DOMAIN.getDefinition(TestDomain.T_EMP).getForeignKeyProperty(TestDomain.EMP_DEPARTMENT_FK), comboBoxModel);
    final Entity sales = CONNECTION_PROVIDER.getConnection().selectSingle(TestDomain.T_DEPARTMENT, TestDomain.DEPARTMENT_NAME, "SALES");
    comboBoxModel.setSelectedItem(sales);
    Collection<Entity> searchEntities = conditionModel.getConditionEntities();
    assertEquals(1, searchEntities.size());
    assertTrue(searchEntities.contains(sales));
    comboBoxModel.refresh();
    assertEquals(sales, comboBoxModel.getSelectedValue());
    searchEntities = conditionModel.getConditionEntities();
    assertEquals(1, searchEntities.size());
    assertTrue(searchEntities.contains(sales));

    conditionModel.setUpperBound(null);
    assertNull(comboBoxModel.getSelectedItem());
    conditionModel.setUpperBound(sales);
    assertEquals(comboBoxModel.getSelectedItem(), sales);

    comboBoxModel.setSelectedItem(null);

    searchEntities = conditionModel.getConditionEntities();
    assertTrue(searchEntities.isEmpty());
  }
}
