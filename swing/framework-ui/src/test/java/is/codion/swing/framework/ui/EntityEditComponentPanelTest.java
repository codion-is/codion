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
 * Copyright (c) 2024 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.swing.framework.ui;

import is.codion.common.utilities.user.User;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.local.LocalEntityConnectionProvider;
import is.codion.swing.common.ui.component.Components;
import is.codion.swing.common.ui.component.text.NumberField;
import is.codion.swing.common.ui.component.value.ComponentValue;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.ui.TestDomain.Employee;

import org.junit.jupiter.api.Test;

import javax.swing.JLabel;
import javax.swing.JTextField;

import static org.junit.jupiter.api.Assertions.*;

public final class EntityEditComponentPanelTest {

	private static final User UNIT_TEST_USER =
					User.parse(System.getProperty("codion.test.user", "scott:tiger"));

	private static final EntityConnectionProvider CONNECTION_PROVIDER = LocalEntityConnectionProvider.builder()
					.domain(new TestDomain())
					.user(UNIT_TEST_USER)
					.build();

	@Test
	void test() {
		SwingEntityEditModel editModel = new SwingEntityEditModel(Employee.TYPE, CONNECTION_PROVIDER);
		EntityEditComponentPanel componentPanel = new EntityEditComponentPanel(editModel);
		componentPanel.createTextField(Employee.NAME);
		assertThrows(IllegalStateException.class, () -> componentPanel.createTextField(Employee.NAME));
		JTextField nameField = (JTextField) componentPanel.component(Employee.NAME).get();
		assertNotNull(nameField);
		assertThrows(IllegalStateException.class, () -> componentPanel.createTextField(Employee.NAME));
		assertFalse(componentPanel.component(Employee.JOB).optional().isPresent());
		assertThrows(IllegalStateException.class, () -> componentPanel.component(Employee.NAME).set(new JLabel()));

		ComponentValue<NumberField<Double>, Double> salary = Components.doubleField().buildValue();
		componentPanel.component(Employee.SALARY).set(salary);
		salary.set(2000d);
		assertEquals(salary.get(), editModel.editor().value(Employee.SALARY).get());
	}
}
