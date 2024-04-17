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
	void inputProvider() throws Exception {
		EntityComboBoxModel model = new EntityComboBoxModel(Department.TYPE, CONNECTION_PROVIDER);
		model.refresh();
		Entity operations = CONNECTION_PROVIDER.connection().selectSingle(Department.NAME.equalTo("OPERATIONS"));
		model.setSelectedItem(operations);
		ComponentValue<Entity, EntityComboBox> value = EntityComboBox.builder(model)
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
		EntityComboBoxModel comboBoxModel = new EntityComboBoxModel(Employee.TYPE, CONNECTION_PROVIDER);
		comboBoxModel.refresh();
		Entity.Key jonesKey = comboBoxModel.connectionProvider().entities().primaryKey(Employee.TYPE, 3);
		comboBoxModel.select(jonesKey);
		EntityComboBox comboBox = EntityComboBox.builder(comboBoxModel).build();
		NumberField<Integer> empIdValue = comboBox.integerSelectorField(Employee.ID).build();
		assertEquals(3, empIdValue.getNumber());
		Entity.Key blakeKey = comboBoxModel.connectionProvider().entities().primaryKey(Employee.TYPE, 5);
		comboBoxModel.select(blakeKey);
		assertEquals(5, empIdValue.getNumber());
		comboBoxModel.setSelectedItem(null);
		assertNull(empIdValue.getNumber());
		empIdValue.setNumber(10);
		assertEquals("ADAMS", comboBoxModel.selectedValue().get(Employee.NAME));
		empIdValue.setNumber(null);
		assertNull(comboBoxModel.selectedValue());
	}
}