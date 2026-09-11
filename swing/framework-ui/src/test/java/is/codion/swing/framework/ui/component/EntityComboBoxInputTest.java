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
 * Copyright (c) 2023 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.swing.framework.ui.component;

import is.codion.common.utilities.user.User;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.local.LocalEntityConnection;
import is.codion.framework.domain.entity.Entity;
import is.codion.swing.common.ui.component.value.ComponentValue;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.key.KeyEvents;
import is.codion.swing.framework.model.component.SwingEntityComboBoxModel;
import is.codion.swing.framework.ui.TestDomain;
import is.codion.swing.framework.ui.TestDomain.Department;

import org.junit.jupiter.api.Test;

import javax.swing.KeyStroke;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusListener;

import static java.awt.event.KeyEvent.VK_F5;
import static java.util.Arrays.asList;
import static javax.swing.JComponent.WHEN_FOCUSED;
import static org.junit.jupiter.api.Assertions.*;

public final class EntityComboBoxInputTest {

	private static final User UNIT_TEST_USER =
					User.parse(System.getProperty("codion.test.user", "scott:tiger"));

	private static final EntityConnection CONNECTION = LocalEntityConnection.builder()
					.domain(new TestDomain())
					.user(UNIT_TEST_USER)
					.build();

	@Test
	void test() {
		SwingEntityComboBoxModel model = SwingEntityComboBoxModel.builder()
						.entityType(Department.TYPE)
						.connection(CONNECTION)
						.build();
		model.items().refresh();
		ComponentValue<EntityComboBoxInput, Entity> value = EntityComboBoxInput.builder()
						.model(model)
						.editPanel(() -> null)
						.buildValue();
		Entity sales = CONNECTION.selectSingle(
						Department.NAME.equalTo("SALES"));
		model.selection().item().set(sales);
		assertEquals(sales, value.get());
		value.clear();
		Entity entity = model.selection().item().get();
		assertNull(entity);
		value.set(sales);
		assertEquals(sales, model.selection().item().get());
	}

	@Test
	void keyEventsAndListenersLandOnTheComboBox() {
		FocusListener focusListener = new FocusAdapter() {};
		EntityComboBoxInput panel = EntityComboBoxInput.builder()
						.model(SwingEntityComboBoxModel.builder()
										.entityType(Department.TYPE)
										.connection(CONNECTION)
										.build())
						.editPanel(() -> null)
						.keyEvent(KeyEvents.builder()
										.keyCode(VK_F5)
										.action(Control.action(e -> {})))
						.focusListener(focusListener)
						.build();
		KeyStroke f5 = KeyStroke.getKeyStroke(VK_F5, 0);
		assertNull(panel.getInputMap(WHEN_FOCUSED).get(f5));
		assertNotNull(panel.comboBox().getInputMap(WHEN_FOCUSED).get(f5));
		assertFalse(asList(panel.getFocusListeners()).contains(focusListener));
		assertTrue(asList(panel.comboBox().getFocusListeners()).contains(focusListener));
	}
}
