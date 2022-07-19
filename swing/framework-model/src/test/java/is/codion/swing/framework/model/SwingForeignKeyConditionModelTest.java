/*
 * Copyright (c) 2011 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.model;

import is.codion.common.db.exception.DatabaseException;
import is.codion.common.user.User;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.local.LocalEntityConnectionProvider;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.model.DefaultEntityTableConditionModel;
import is.codion.framework.model.DefaultFilterModelFactory;
import is.codion.framework.model.EntityTableConditionModel;
import is.codion.framework.model.test.TestDomain;

import org.junit.jupiter.api.Test;

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;

public class SwingForeignKeyConditionModelTest {

  private static final User UNIT_TEST_USER =
          User.parse(System.getProperty("codion.test.user", "scott:tiger"));

  private static final EntityConnectionProvider CONNECTION_PROVIDER = LocalEntityConnectionProvider.builder()
            .domainClassName(TestDomain.class.getName())
            .user(UNIT_TEST_USER)
            .build();

  private final EntityTableConditionModel conditionModel = new DefaultEntityTableConditionModel(TestDomain.T_EMP,
          CONNECTION_PROVIDER, new DefaultFilterModelFactory(), new SwingConditionModelFactory(CONNECTION_PROVIDER));

  @Test
  void refresh() {
    conditionModel.refresh();
    assertTrue(((SwingForeignKeyConditionModel) conditionModel.getConditionModel(TestDomain.EMP_DEPARTMENT_FK))
            .getEntityComboBoxModel().getSize() > 1);
  }

  @Test
  void getSearchEntitiesComboBoxModel() throws DatabaseException {
    SwingEntityComboBoxModel comboBoxModel = new SwingEntityComboBoxModel(TestDomain.T_DEPARTMENT, CONNECTION_PROVIDER);
    SwingForeignKeyConditionModel conditionModel = new SwingForeignKeyConditionModel(TestDomain.EMP_DEPARTMENT_FK, comboBoxModel);
    Entity sales = CONNECTION_PROVIDER.getConnection().selectSingle(TestDomain.DEPARTMENT_NAME, "SALES");
    comboBoxModel.setSelectedItem(sales);
    Collection<Entity> searchEntities = conditionModel.getEqualValues();
    assertEquals(1, searchEntities.size());
    assertTrue(searchEntities.contains(sales));
    comboBoxModel.refresh();
    assertEquals(sales, comboBoxModel.getSelectedValue());
    searchEntities = conditionModel.getEqualValues();
    assertEquals(1, searchEntities.size());
    assertTrue(searchEntities.contains(sales));

    conditionModel.setEqualValue(null);
    assertNull(comboBoxModel.getSelectedItem());
    conditionModel.setEqualValue(sales);
    assertEquals(comboBoxModel.getSelectedItem(), sales);

    comboBoxModel.setSelectedItem(null);

    searchEntities = conditionModel.getEqualValues();
    assertTrue(searchEntities.isEmpty());
  }
}
