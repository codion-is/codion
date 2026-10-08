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
 * Copyright (c) 2018 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.swing.framework.ui;

import is.codion.common.utilities.user.User;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.local.LocalEntityConnection;
import is.codion.framework.domain.entity.Entity;
import is.codion.swing.common.ui.control.CommandControl;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.ControlKey;
import is.codion.swing.common.ui.layout.Layouts;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.ui.TestDomain.Department;
import is.codion.swing.framework.ui.TestDomain.Employee;

import org.junit.jupiter.api.Test;

import javax.swing.JComponent;
import javax.swing.KeyStroke;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static is.codion.swing.common.ui.control.ControlMap.controlMap;
import static is.codion.swing.framework.ui.EntityEditPanel.ControlKeys.*;
import static java.awt.event.InputEvent.*;
import static java.awt.event.KeyEvent.VK_A;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.*;

public final class EntityEditPanelTest {

	private static final User UNIT_TEST_USER =
					User.parse(System.getProperty("codion.test.user", "scott:tiger"));

	private static final EntityConnection CONNECTION = LocalEntityConnection.builder()
					.domain(new TestDomain())
					.user(UNIT_TEST_USER)
					.build();

	@Test
	void test() {
		SwingEntityEditModel editModel = new SwingEntityEditModel(Employee.TYPE, CONNECTION);
		TestEditPanel editPanel = new TestEditPanel(editModel);
		assertThrows(IllegalStateException.class, editPanel::controls);
		editPanel.initialize();
		editPanel.controls();

		assertEquals(editModel, editPanel.model());
		assertFalse(editPanel.active().is());
		editPanel.active().set(true);
		assertTrue(editPanel.active().is());

		Entity martin = editModel.connection().selectSingle(Employee.NAME.equalTo("MARTIN"));
		editModel.editor().entity().set(martin);
		assertTrue(editModel.editor().entity().exists().is());
		editPanel.clearAndRequestFocus();
		assertFalse(editModel.editor().entity().exists().is());

		assertNotNull(editPanel.control(INSERT));
		assertNotNull(editPanel.control(UPDATE));
		assertNotNull(editPanel.control(DELETE));
		assertNotNull(editPanel.control(CLEAR));
	}

	@Test
	void excludeFromSelectionValidatesAttributes() {
		SwingEntityEditModel editModel = new SwingEntityEditModel(Employee.TYPE, CONNECTION);
		//an attribute belonging to the entity is accepted
		new ConfigEditPanel(editModel, config -> config.excludeFromSelection(singletonList(Employee.NAME)));
		//an attribute from another entity is rejected, as the javadoc advertises
		assertThrows(IllegalArgumentException.class, () ->
						new ConfigEditPanel(editModel, config -> config.excludeFromSelection(singletonList(Department.NAME))));
	}

	private static final class ConfigEditPanel extends EntityEditPanel {

		private ConfigEditPanel(SwingEntityEditModel editModel, java.util.function.Consumer<Config> config) {
			super(editModel, config);
		}

		@Override
		protected void initializeUI() {}
	}

	private static final class TestEditPanel extends EntityEditPanel {

		public TestEditPanel(SwingEntityEditModel editModel) {
			super(editModel);
			create().textField(Employee.NAME);
			create().itemComboBox(Employee.JOB);
			create().comboBox(Employee.MGR_FK);
			create().comboBox(Employee.DEPARTMENT_FK);
			create().textField(Employee.SALARY);
			create().textField(Employee.COMMISSION);
			create().temporalInput(Employee.HIREDATE);
		}

		@Override
		protected void initializeUI() {
			setLayout(Layouts.flexibleGridLayout(3, 3));

			addInputPanel(Employee.NAME);
			addInputPanel(Employee.JOB);
			addInputPanel(Employee.DEPARTMENT_FK);

			addInputPanel(Employee.MGR_FK);
			addInputPanel(Employee.SALARY);
			addInputPanel(Employee.COMMISSION);

			addInputPanel(Employee.HIREDATE);
		}
	}

	@Test
	void initializeReentrant() {
		SwingEntityEditModel editModel = new SwingEntityEditModel(Employee.TYPE, CONNECTION);
		AtomicInteger initializations = new AtomicInteger();
		EntityEditPanel editPanel = new EntityEditPanel(editModel) {
			@Override
			protected void initializeUI() {
				initializations.incrementAndGet();
				//as a nested event loop pumped during the initialization might
				initialize();
				assertFalse(initialized());
			}
		};
		editPanel.initialize();
		assertEquals(1, initializations.get());
		assertTrue(editPanel.initialized());
	}

	@Test
	void keyStrokes() {
		// every control given a key stroke is bound on the panel, one set in setupControls() included
		List<ControlKey<?>> controlKeys = new ArrayList<>(controlMap(EntityEditPanel.ControlKeys.class).keys());
		CommandControl selectInputField = Control.builder().command(() -> {}).build();
		SwingEntityEditModel editModel = new SwingEntityEditModel(Employee.TYPE, CONNECTION);
		EntityEditPanel editPanel = new EntityEditPanel(editModel, config ->
						controlKeys.forEach(controlKey -> config.keyStroke(controlKey, keyStroke ->
										keyStroke.set(keyStroke(controlKeys.indexOf(controlKey)))))) {
			@Override
			protected void setupControls() {
				control(SELECT_INPUT_FIELD).set(selectInputField);
			}

			@Override
			protected void initializeUI() {}
		};
		editPanel.initialize();
		for (ControlKey<?> controlKey : controlKeys) {
			Control control = editPanel.control(controlKey).get();
			if (control != null) {
				assertTrue(bound(editPanel, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT,
								keyStroke(controlKeys.indexOf(controlKey)), control), controlKey.name());
			}
		}
		assertTrue(bound(editPanel, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT,
						keyStroke(controlKeys.indexOf(SELECT_INPUT_FIELD)), selectInputField));
		assertTrue(bound(editPanel, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT,
						keyStroke(controlKeys.indexOf(INSERT)), editPanel.control(INSERT).get()));
	}

	@Test
	void keyStrokeCollision() {
		SwingEntityEditModel editModel = new SwingEntityEditModel(Employee.TYPE, CONNECTION);
		EntityEditPanel editPanel = new EntityEditPanel(editModel, config ->
						config.keyStroke(CLEAR, keyStroke -> keyStroke.set(SELECT_INPUT_FIELD.defaultKeystroke().get()))) {
			@Override
			protected void initializeUI() {}
		};
		assertThrows(IllegalStateException.class, editPanel::initialize);
	}

	@Test
	void controlSetInSetupControls() {
		SwingEntityEditModel editModel = new SwingEntityEditModel(Employee.TYPE, CONNECTION);
		CommandControl insert = Control.builder().command(() -> {}).build();
		EntityEditPanel editPanel = new EntityEditPanel(editModel) {
			@Override
			protected void setupControls() {
				control(INSERT).set(insert);
			}

			@Override
			protected void initializeUI() {}
		};
		editPanel.initialize();
		assertSame(insert, editPanel.control(INSERT).get());
	}

	private static KeyStroke keyStroke(int index) {
		return KeyStroke.getKeyStroke(VK_A + index % 26, CTRL_DOWN_MASK | ALT_DOWN_MASK | SHIFT_DOWN_MASK | (index < 26 ? 0 : META_DOWN_MASK));
	}

	private static boolean bound(JComponent component, int condition, KeyStroke keyStroke, Control control) {
		Object actionKey = component.getInputMap(condition).get(keyStroke);

		return actionKey != null && component.getActionMap().get(actionKey) == control;
	}
}
