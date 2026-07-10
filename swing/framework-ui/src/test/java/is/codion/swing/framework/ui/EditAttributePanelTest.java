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
 * Copyright (c) 2026, Björn Darri Sigurðsson.
 */
package is.codion.swing.framework.ui;

import is.codion.common.utilities.user.User;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.local.LocalEntityConnectionProvider;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.exception.EntityValidationException;
import is.codion.swing.common.ui.component.Components;
import is.codion.swing.common.ui.component.value.ComponentValue;
import is.codion.swing.framework.model.SwingEntityEditor;
import is.codion.swing.framework.ui.TestDomain.Employee;

import org.junit.jupiter.api.Test;

import javax.swing.JTextField;

import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.*;

public final class EditAttributePanelTest {

	private static final User UNIT_TEST_USER =
					User.parse(System.getProperty("codion.test.user", "scott:tiger"));

	private static final EntityConnectionProvider CONNECTION_PROVIDER = LocalEntityConnectionProvider.builder()
					.domain(new TestDomain())
					.user(UNIT_TEST_USER)
					.build();

	@Test
	void synchronousThrowDoesNotArmUpdating() {
		SwingEntityEditor editor = new SwingEntityEditor(Employee.TYPE, CONNECTION_PROVIDER);
		Entity employee = CONNECTION_PROVIDER.connection().selectSingle(Employee.NAME.equalTo("BLAKE"));
		ComponentValue<JTextField, String> componentValue = Components.stringField().buildValue();

		EditAttributePanel<String> panel =
						new EditAttributePanel<>(editor, singletonList(employee), Employee.NAME, componentValue, "Name");
		componentValue.set("MODIFIED");
		assertTrue(panel.update().enabled().orElseThrow().is());

		//tasks().update() notifies the before-update consumers, synchronously, before any worker exists
		editor.events().before().update().addConsumer(entities -> {
			throw new IllegalStateException("boom");
		});

		assertThrows(IllegalStateException.class, panel::performUpdate);

		//both controls of the modal dialog are disabled while updating, so arming the state before
		//the throw would have left the dialog with no way of closing it, freezing the application
		assertTrue(panel.cancel().enabled().orElseThrow().is());
		assertTrue(panel.update().enabled().orElseThrow().is());
	}

	@Test
	void propagatedValueEscapesTheAttributeScopedGate() {
		SwingEntityEditor editor = new SwingEntityEditor(Employee.TYPE, CONNECTION_PROVIDER);
		//editing the name propagates an invalid salary, below the range the domain defines
		editor.value(Employee.NAME).propagate(Employee.SALARY, name -> 1d);

		Entity employee = CONNECTION_PROVIDER.connection().selectSingle(Employee.NAME.equalTo("BLAKE"));
		Entity toUpdate = employee.copy().mutable();
		//exactly what the panel does, which applies the propagators
		editor.value(Employee.NAME).set(toUpdate, "MODIFIED");
		assertTrue(toUpdate.modified(Employee.SALARY));

		//the panel's ok control gate validates the edited attribute alone, and passes
		assertDoesNotThrow(() -> editor.validator().getOrThrow().validate(toUpdate, Employee.NAME));
		//the task validates every modified attribute, the propagated one among them, and throws.
		//A legacy entity invalid on an unmodified attribute is not affected, EntityValidator.validated()
		//validating only the modified values of an existing entity. The propagated value is the divergence
		assertThrows(EntityValidationException.class, () -> editor.tasks().update(singletonList(toUpdate)));
	}
}
