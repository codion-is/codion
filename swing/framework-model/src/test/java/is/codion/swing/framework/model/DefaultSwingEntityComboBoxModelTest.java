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
 * Copyright (c) 2009 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.swing.framework.model;

import is.codion.common.utilities.user.User;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.local.LocalEntityConnectionProvider;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.model.test.TestDomain;
import is.codion.framework.model.test.TestDomain.Employee;
import is.codion.swing.framework.model.component.SwingEntityComboBoxModel;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Tests the Swing {@code ComboBoxModel} coat of {@link SwingEntityComboBoxModel} — the entity logic is tested in
 * {@code is.codion.framework.model.DefaultEntityComboBoxModelTest}.
 */
public final class DefaultSwingEntityComboBoxModelTest {

	private static final User UNIT_TEST_USER =
					User.parse(System.getProperty("codion.test.user", "scott:tiger"));

	private static final EntityConnectionProvider CONNECTION_PROVIDER = LocalEntityConnectionProvider.builder()
					.domain(new TestDomain())
					.user(UNIT_TEST_USER)
					.build();

	@Test
	void comboBoxModelCoat() {
		SwingEntityComboBoxModel model = SwingEntityComboBoxModel.builder()
						.entityType(Employee.TYPE)
						.connectionProvider(CONNECTION_PROVIDER)
						.includeNull(true)
						.build();
		model.items().refresh();
		// getSize/getElementAt expose the null item + the entities, the ListModel surface a JComboBox needs
		assertEquals(model.items().included().size() + 1, model.getSize());// + null item
		assertEquals("-", model.getElementAt(0).toString());// null item first
		Entity first = model.items().included().get().get(0);
		assertEquals(first, model.getElementAt(1));
		// the Object-based ComboBoxModel setter delegates to the wrapped model
		model.setSelectedItem(first);
		assertEquals(first, model.selection().item().get());
		assertEquals(first, model.getSelectedItem());
		model.setSelectedItem(null);
		assertNull(model.selection().item().get());
		assertEquals("-", model.getSelectedItem().toString());// the null item representation
	}
}
