/*
 * Copyright (c) 2011 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.model;

import is.codion.common.db.exception.DatabaseException;
import is.codion.common.user.User;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.local.LocalEntityConnectionProvider;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.model.test.TestDomain;
import is.codion.framework.model.test.TestDomain.Department;
import is.codion.framework.model.test.TestDomain.Employee;
import is.codion.swing.framework.model.component.EntityComboBoxModel;

import org.junit.jupiter.api.Test;

import java.util.Collection;

import static is.codion.framework.db.condition.Condition.column;
import static is.codion.swing.framework.model.EntityComboBoxModelConditionModel.entityComboBoxModelConditionModel;
import static org.junit.jupiter.api.Assertions.*;

public class EntityComboBoxModelConditionModelTest {

  private static final User UNIT_TEST_USER =
          User.parse(System.getProperty("codion.test.user", "scott:tiger"));

  private static final EntityConnectionProvider CONNECTION_PROVIDER = LocalEntityConnectionProvider.builder()
          .domain(new TestDomain())
          .user(UNIT_TEST_USER)
          .build();

  @Test
  void searchEntitiesComboBoxModel() throws DatabaseException {
    EntityComboBoxModel comboBoxModel = new EntityComboBoxModel(Department.TYPE, CONNECTION_PROVIDER);
    EntityComboBoxModelConditionModel conditionModel = entityComboBoxModelConditionModel(Employee.DEPARTMENT_FK, comboBoxModel);
    Entity sales = CONNECTION_PROVIDER.connection().selectSingle(column(Department.NAME).equalTo("SALES"));
    comboBoxModel.setSelectedItem(sales);
    Collection<Entity> searchEntities = conditionModel.getEqualValues();
    assertEquals(1, searchEntities.size());
    assertTrue(searchEntities.contains(sales));
    comboBoxModel.refresh();
    assertEquals(sales, comboBoxModel.selectedValue());
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
