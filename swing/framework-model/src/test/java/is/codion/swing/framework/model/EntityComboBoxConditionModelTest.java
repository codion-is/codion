/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2011 - 2023, Björn Darri Sigurðsson.
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

import static is.codion.swing.framework.model.EntityComboBoxConditionModel.entityComboBoxConditionModel;
import static org.junit.jupiter.api.Assertions.*;

public class EntityComboBoxConditionModelTest {

  private static final User UNIT_TEST_USER =
          User.parse(System.getProperty("codion.test.user", "scott:tiger"));

  private static final EntityConnectionProvider CONNECTION_PROVIDER = LocalEntityConnectionProvider.builder()
          .domain(new TestDomain())
          .user(UNIT_TEST_USER)
          .build();

  @Test
  void searchEntitiesComboBoxModel() throws DatabaseException {
    EntityComboBoxModel comboBoxModel = new EntityComboBoxModel(Department.TYPE, CONNECTION_PROVIDER);
    EntityComboBoxConditionModel conditionModel = entityComboBoxConditionModel(Employee.DEPARTMENT_FK, comboBoxModel);
    Entity sales = CONNECTION_PROVIDER.connection().selectSingle(Department.NAME.equalTo("SALES"));
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
