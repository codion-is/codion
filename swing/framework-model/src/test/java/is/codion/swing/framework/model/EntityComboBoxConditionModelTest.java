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
 * Copyright (c) 2011 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.swing.framework.model;

import is.codion.common.user.User;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.local.LocalEntityConnectionProvider;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.model.EntitySearchModel;
import is.codion.framework.model.test.TestDomain;
import is.codion.framework.model.test.TestDomain.Department;
import is.codion.swing.framework.model.component.EntityComboBoxModel;

import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.*;

public class EntityComboBoxConditionModelTest {

	private static final User UNIT_TEST_USER =
					User.parse(System.getProperty("codion.test.user", "scott:tiger"));

	private static final EntityConnectionProvider CONNECTION_PROVIDER = LocalEntityConnectionProvider.builder()
					.domain(new TestDomain())
					.user(UNIT_TEST_USER)
					.build();

	@Test
	void searchEntitiesComboBoxModel() {
		EntityComboBoxModel comboBoxModel = EntityComboBoxModel.builder(Department.TYPE, CONNECTION_PROVIDER)
						.includeNull(true)
						.build();
		EntitySearchModel searchModel = EntitySearchModel.builder(Department.TYPE, CONNECTION_PROVIDER).build();
		SwingForeignKeyConditionModel condition =
						SwingForeignKeyConditionModel.builder()
										.includeEqualOperators(comboBoxModel)
										.includeInOperators(searchModel)
										.build();
		EntityComboBoxModel equalComboBoxModel = condition.equalComboBoxModel();
		Entity sales = CONNECTION_PROVIDER.connection().selectSingle(Department.NAME.equalTo("SALES"));
		equalComboBoxModel.setSelectedItem(sales);
		Entity searchEntity = condition.operands().equal().get();
		assertSame(sales, searchEntity);
		equalComboBoxModel.items().refresh();
		assertEquals(sales, equalComboBoxModel.selection().item().get());

		condition.operands().equal().set(null);
		assertNotNull(equalComboBoxModel.getSelectedItem());
		condition.operands().equal().set(sales);
		assertEquals(equalComboBoxModel.getSelectedItem(), sales);

		equalComboBoxModel.setSelectedItem(null);
	}

	@Test
	void inSearchModel() {
		EntityComboBoxModel comboBoxModel = EntityComboBoxModel.builder(Department.TYPE, CONNECTION_PROVIDER)
						.includeNull(true)
						.build();
		EntitySearchModel searchModel = EntitySearchModel.builder(Department.TYPE, CONNECTION_PROVIDER).build();
		SwingForeignKeyConditionModel conditionModel =
						SwingForeignKeyConditionModel.builder()
										.includeEqualOperators(comboBoxModel)
										.includeInOperators(searchModel)
										.build();
		EntitySearchModel inSearchModel = conditionModel.inSearchModel();
		Entity sales = CONNECTION_PROVIDER.connection().selectSingle(Department.NAME.equalTo("SALES"));
		inSearchModel.selection().entity().set(sales);
		Collection<Entity> searchEntities = conditionModel.operands().in().get();
		assertEquals(1, searchEntities.size());
		assertTrue(searchEntities.contains(sales));
		Entity accounting = CONNECTION_PROVIDER.connection().selectSingle(Department.NAME.equalTo("ACCOUNTING"));
		List<Entity> salesAccounting = asList(sales, accounting);
		inSearchModel.selection().entities().set(salesAccounting);
		assertTrue(conditionModel.operands().in().get().contains(sales));
		assertTrue(conditionModel.operands().in().get().contains(accounting));
		searchEntities = conditionModel.operands().in().get();
		assertEquals(2, searchEntities.size());
		assertTrue(searchEntities.contains(sales));
		assertTrue(searchEntities.contains(accounting));
	}
}
