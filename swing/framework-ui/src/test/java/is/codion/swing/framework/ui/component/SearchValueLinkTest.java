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
 * Copyright (c) 2010 - 2023, Björn Darri Sigurðsson.
 */
package is.codion.swing.framework.ui.component;

import is.codion.common.user.User;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.local.LocalEntityConnectionProvider;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.model.EntityEditModel;
import is.codion.framework.model.EntitySearchModel;
import is.codion.swing.common.ui.component.value.ComponentValue;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.ui.TestDomain;
import is.codion.swing.framework.ui.TestDomain.Department;
import is.codion.swing.framework.ui.TestDomain.Employee;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SearchValueLinkTest {

  private static final User UNIT_TEST_USER =
          User.parse(System.getProperty("codion.test.user", "scott:tiger"));

  private static final EntityConnectionProvider CONNECTION_PROVIDER = LocalEntityConnectionProvider.builder()
          .domain(new TestDomain())
          .user(UNIT_TEST_USER)
          .build();

  private final EntityEditModel model = new SwingEntityEditModel(Employee.TYPE, CONNECTION_PROVIDER);
  private final EntityComponents inputComponents = new EntityComponents(model.entityDefinition());

  @Test
  void test() throws Exception {
    ComponentValue<Entity, EntitySearchField> componentValue =
            inputComponents.foreignKeySearchField(Employee.DEPARTMENT_FK,
                    model.foreignKeySearchModel(Employee.DEPARTMENT_FK)).buildValue();
    componentValue.link(model.value(Employee.DEPARTMENT_FK));
    EntitySearchModel searchModel = componentValue.component().model();
    assertTrue(searchModel.getEntities().isEmpty());
    Entity department = model.connectionProvider().connection().selectSingle(Department.NAME.equalTo("SALES"));
    model.put(Employee.DEPARTMENT_FK, department);
    assertEquals(searchModel.getEntities().size(), 1);
    assertEquals(searchModel.getEntities().iterator().next(), department);
    department = model.connectionProvider().connection().selectSingle(Department.NAME.equalTo("OPERATIONS"));
    searchModel.setEntity(department);
    assertEquals(model.get(Employee.DEPARTMENT_FK), department);
  }
}