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
 * Copyright (c) 2010 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.swing.framework.ui.component;

import is.codion.common.user.User;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.local.LocalEntityConnectionProvider;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.model.EntitySearchModel;
import is.codion.swing.common.ui.component.value.ComponentValue;
import is.codion.swing.framework.ui.TestDomain;
import is.codion.swing.framework.ui.TestDomain.Department;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;

/**
 * User: Björn Darri
 * Date: 17.4.2010
 * Time: 12:06:44
 */
public class EntitySearchFieldTest {

  private static final User UNIT_TEST_USER =
          User.parse(System.getProperty("codion.test.user", "scott:tiger"));

  private static final EntityConnectionProvider CONNECTION_PROVIDER = LocalEntityConnectionProvider.builder()
          .domain(new TestDomain())
          .user(UNIT_TEST_USER)
          .build();

  @Test
  void componentValue() throws Exception {
    EntitySearchModel searchModel = EntitySearchModel.builder(Department.TYPE, CONNECTION_PROVIDER).build();
    ComponentValue<Entity, EntitySearchField> value = EntitySearchField.builder(searchModel)
            .buildValue();

    assertNull(value.get());

    Entity sales = CONNECTION_PROVIDER.connection().selectSingle(Department.NAME.equalTo("SALES"));

    searchModel.entity().set(sales);
    assertEquals(sales, value.get());
    searchModel.entity().set(null);
    assertNull(value.get());

    ComponentValue<Collection<Entity>, EntitySearchField> multiSelectionValue = value.component().multiSelectionValue();
    assertTrue(multiSelectionValue.get().isEmpty());

    ComponentValue<Entity, EntitySearchField> singleSelectionValue = value.component().singleSelectionValue();
    assertNull(singleSelectionValue.get());

    Entity research = CONNECTION_PROVIDER.connection().selectSingle(Department.NAME.equalTo("RESEARCH"));

    searchModel.entities().set(Arrays.asList(sales, research));

    assertTrue(multiSelectionValue.get().containsAll(Arrays.asList(sales, research)));
    assertEquals(singleSelectionValue.get(), sales);

    singleSelectionValue.set(null);

    assertTrue(searchModel.entities().get().isEmpty());
    assertNull(singleSelectionValue.get());
  }
}
