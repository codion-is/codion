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
 * Copyright (c) 2010 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.swing.framework.ui.component;

import is.codion.common.user.User;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.local.LocalEntityConnectionProvider;
import is.codion.framework.domain.entity.Entity;
import is.codion.swing.common.ui.component.text.NumberField;
import is.codion.swing.common.ui.component.value.ComponentValue;
import is.codion.swing.framework.model.component.EntityComboBoxModel;
import is.codion.swing.framework.ui.TestDomain;
import is.codion.swing.framework.ui.TestDomain.Department;
import is.codion.swing.framework.ui.TestDomain.Employee;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * User: Björn Darri
 * Date: 17.4.2010
 * Time: 12:06:44
 */
public class EntityComboBoxTest {

	private static final User UNIT_TEST_USER =
					User.parse(System.getProperty("codion.test.user", "scott:tiger"));

	private static final EntityConnectionProvider CONNECTION_PROVIDER = LocalEntityConnectionProvider.builder()
					.domain(new TestDomain())
					.user(UNIT_TEST_USER)
					.build();

	@Test
	void inputProvider() {
		EntityComboBoxModel model = EntityComboBoxModel.builder()
						.entityType(Department.TYPE)
						.connectionProvider(CONNECTION_PROVIDER)
						.build();
		model.items().refresh();
		Entity operations = CONNECTION_PROVIDER.connection().selectSingle(Department.NAME.equalTo("OPERATIONS"));
		model.selection().item().set(operations);
		ComponentValue<EntityComboBox, Entity> value = EntityComboBox.builder()
						.model(model)
						.buildValue();

		assertNotNull(value.get());

		Entity sales = CONNECTION_PROVIDER.connection().selectSingle(
						Department.NAME.equalTo("SALES"));

		model.setSelectedItem(sales);
		assertEquals(sales, value.get());
		model.setSelectedItem(null);
		assertNull(value.get());
	}

	@Test
	void integerSelectorField() {
		EntityComboBoxModel comboBoxModel = EntityComboBoxModel.builder()
						.entityType(Employee.TYPE)
						.connectionProvider(CONNECTION_PROVIDER)
						.build();
		comboBoxModel.items().refresh();
		Entity.Key jonesKey = comboBoxModel.connectionProvider().entities().primaryKey(Employee.TYPE, 3);
		comboBoxModel.select(jonesKey);
		EntityComboBox comboBox = EntityComboBox.builder()
						.model(comboBoxModel)
						.build();
		NumberField<Integer> empIdValue = comboBox.integerSelectorField(Employee.ID).build();
		assertEquals(3, empIdValue.get());
		Entity.Key blakeKey = comboBoxModel.connectionProvider().entities().primaryKey(Employee.TYPE, 5);
		comboBoxModel.select(blakeKey);
		assertEquals(5, empIdValue.get());
		comboBoxModel.setSelectedItem(null);
		assertNull(empIdValue.get());
		empIdValue.set(10);
		assertEquals("ADAMS", comboBoxModel.selection().item().getOrThrow().get(Employee.NAME));
		empIdValue.set(null);
		assertNull(comboBoxModel.selection().item().get());
	}
}