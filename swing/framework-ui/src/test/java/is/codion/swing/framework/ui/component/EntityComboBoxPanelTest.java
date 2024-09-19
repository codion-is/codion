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
 * Copyright (c) 2023 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.swing.framework.ui.component;

import is.codion.common.db.exception.DatabaseException;
import is.codion.common.user.User;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.local.LocalEntityConnectionProvider;
import is.codion.framework.domain.entity.Entity;
import is.codion.swing.common.ui.component.value.ComponentValue;
import is.codion.swing.framework.model.component.EntityComboBoxModel;
import is.codion.swing.framework.ui.TestDomain;
import is.codion.swing.framework.ui.TestDomain.Department;

import org.junit.jupiter.api.Test;

import static is.codion.swing.framework.model.component.EntityComboBoxModel.entityComboBoxModel;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public final class EntityComboBoxPanelTest {

	private static final User UNIT_TEST_USER =
					User.parse(System.getProperty("codion.test.user", "scott:tiger"));

	private static final EntityConnectionProvider CONNECTION_PROVIDER = LocalEntityConnectionProvider.builder()
					.domain(new TestDomain())
					.user(UNIT_TEST_USER)
					.build();

	@Test
	void test() throws DatabaseException {
		EntityComboBoxModel model = entityComboBoxModel(Department.TYPE, CONNECTION_PROVIDER);
		model.refresh();
		ComponentValue<Entity, EntityComboBoxPanel> value = EntityComboBoxPanel.builder(model, () -> null)
						.buildValue();
		Entity sales = CONNECTION_PROVIDER.connection().selectSingle(
						Department.NAME.equalTo("SALES"));
		model.setSelectedItem(sales);
		assertEquals(sales, value.get());
		value.clear();
		Entity entity = model.selectionModel().selectedValue();
		assertNull(entity);
		value.set(sales);
		assertEquals(sales, model.selectionModel().selectedValue());
	}
}
