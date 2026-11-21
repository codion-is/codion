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

import is.codion.common.reactive.state.State;
import is.codion.common.utilities.user.User;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.exception.EntityModifiedException;
import is.codion.framework.db.local.LocalEntityConnection;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.exception.EntityValidationException;
import is.codion.framework.model.EditorLink;
import is.codion.framework.model.EntityMerge;
import is.codion.framework.model.EntityMerge.AttributeMerge;
import is.codion.swing.common.ui.control.CommandControl;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.ControlKey;
import is.codion.swing.common.ui.layout.Layouts;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.ui.TestDomain.Department;
import is.codion.swing.framework.ui.TestDomain.Employee;

import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import java.awt.event.ActionEvent;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.prefs.Preferences;

import static is.codion.common.model.preferences.JsonPreferences.jsonPreferences;
import static is.codion.swing.common.ui.control.ControlMap.controlMap;
import static is.codion.swing.framework.ui.EntityEditPanel.ControlKeys.*;
import static java.awt.event.InputEvent.*;
import static java.awt.event.KeyEvent.VK_A;
import static java.awt.event.KeyEvent.VK_P;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
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
		//columns of one type
		List<Column<String>> names = singletonList(Employee.NAME);
		new ConfigEditPanel(editModel, config -> config.excludeFromSelection(names));
		//an attribute from another entity is rejected, as the javadoc advertises
		assertThrows(IllegalArgumentException.class, () ->
						new ConfigEditPanel(editModel, config -> config.excludeFromSelection(singletonList(Department.NAME))));
	}

	@Test
	void persistToggleValidatesAttributes() {
		EditorComponents components = new ConfigEditPanel(departmentWithEmployees(), config -> {}).components();
		EditorComponents employees = components.detail().get(Employee.DEPARTMENT_FK);
		//a column and a foreign key are accepted
		components.persistToggle().add(Department.LOCATION);
		employees.persistToggle().set(asList(Employee.NAME, Employee.MGR_FK));
		//an attribute from another entity is rejected
		assertThrows(IllegalArgumentException.class, () -> components.persistToggle().add(Employee.NAME));
		//as is a derived one, having no value for the user to set
		assertThrows(IllegalArgumentException.class, () -> employees.persistToggle().add(Employee.DEPARTMENT_LOCATION));
		//and the foreign key linking a detail editor to its master
		assertThrows(IllegalArgumentException.class, () -> employees.persistToggle().add(Employee.DEPARTMENT_FK));
	}

	@Test
	void persistToggle() throws Exception {
		SwingEntityEditModel editModel = new SwingEntityEditModel(Employee.TYPE, CONNECTION);
		PersistEditPanel editPanel = new PersistEditPanel(editModel);
		editPanel.initialize();
		//applied on initialization, locked from then on
		assertThrows(IllegalStateException.class, () -> editPanel.components().persistToggle().add(Employee.JOB));
		State persist = editModel.editor().value(Employee.NAME).persist();
		assertFalse(persist.is());
		JComponent component = editPanel.component(Employee.NAME).get();
		JLabel label = editPanel.component(Employee.NAME).label();
		Icon dimmedPin = label.getIcon();
		assertNotNull(dimmedPin);
		//the key stroke, bound on the component, toggles
		KeyStroke keyStroke = KeyStroke.getKeyStroke(VK_P, ALT_DOWN_MASK);
		Object actionKey = component.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).get(keyStroke);
		assertNotNull(actionKey);
		component.getActionMap().get(actionKey).actionPerformed(new ActionEvent(component, ActionEvent.ACTION_PERFORMED, ""));
		assertTrue(persist.is());
		SwingUtilities.invokeAndWait(() -> {});
		assertNotSame(dimmedPin, label.getIcon());
		//a component without a persist toggle has neither the key stroke nor the pin
		JComponent job = editPanel.component(Employee.JOB).get();
		assertNull(job.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).get(keyStroke));
		assertNull(editPanel.component(Employee.JOB).label().getIcon());
	}

	@Test
	void persistToggleStoreRestore() {
		Preferences preferences = jsonPreferences();
		SwingEntityEditModel editModel = new SwingEntityEditModel(Employee.TYPE, CONNECTION);
		PersistEditPanel editPanel = new PersistEditPanel(editModel);
		//not initialized and nothing restored, the preferences are left as they are
		preferences.put("persist", "{}");
		editPanel.store(preferences);
		assertEquals("{}", preferences.get("persist", null));
		editPanel.initialize();
		State name = editModel.editor().value(Employee.NAME).persist();
		State department = editModel.editor().value(Employee.DEPARTMENT_FK).persist();
		//a column does not persist by default, a foreign key does
		assertFalse(name.is());
		assertTrue(department.is());
		//untouched, nothing stored
		editPanel.store(preferences);
		assertNull(preferences.get("persist", null));
		name.set(true);
		department.set(false);
		editPanel.store(preferences);

		//restored before the panel is initialized, as by the application,
		//the persist toggles not yet specified, applied on initialization
		SwingEntityEditModel restoredModel = new SwingEntityEditModel(Employee.TYPE, CONNECTION);
		PersistEditPanel restoredPanel = new PersistEditPanel(restoredModel);
		restoredPanel.restore(preferences);
		State restoredName = restoredModel.editor().value(Employee.NAME).persist();
		State restoredDepartment = restoredModel.editor().value(Employee.DEPARTMENT_FK).persist();
		assertFalse(restoredName.is());
		//not yet initialized, what was restored is stored
		Preferences notInitialized = jsonPreferences();
		restoredPanel.store(notInitialized);
		assertTrue(new JSONObject(preferences.get("persist", null)).similar(new JSONObject(notInitialized.get("persist", null))));
		restoredPanel.initialize();
		assertTrue(restoredName.is());
		assertFalse(restoredDepartment.is());

		//the states themselves are stored, so defaults changed since are not reversed,
		//and now being the defaults, nothing is stored
		SwingEntityEditModel changedModel = new SwingEntityEditModel(Employee.TYPE, CONNECTION);
		changedModel.editor().value(Employee.NAME).persist().set(true);
		changedModel.editor().value(Employee.DEPARTMENT_FK).persist().set(false);
		PersistEditPanel changedPanel = new PersistEditPanel(changedModel);
		changedPanel.restore(preferences);
		changedPanel.initialize();
		assertTrue(changedModel.editor().value(Employee.NAME).persist().is());
		assertFalse(changedModel.editor().value(Employee.DEPARTMENT_FK).persist().is());
		changedPanel.store(preferences);
		assertNull(preferences.get("persist", null));

		//restored after initialization, applied at once, the defaults restored in case nothing is stored,
		//a stored name without a persist toggle being ignored
		preferences.put("persist", "{\"attributes\":{\"job\":true}}");
		restoredPanel.restore(preferences);
		assertFalse(restoredName.is());
		assertTrue(restoredDepartment.is());
		assertFalse(restoredModel.editor().value(Employee.JOB).persist().is());

		//unreadable preferences revert to the defaults
		restoredName.set(true);
		preferences.put("persist", "not json");
		restoredPanel.restore(preferences);
		assertFalse(restoredName.is());
	}

	@Test
	void persistToggleDetail() {
		Preferences preferences = jsonPreferences();
		DepartmentPersistPanel editPanel = new DepartmentPersistPanel(departmentWithEmployees());
		editPanel.initialize();
		EditorComponents employees = editPanel.components().detail().get(Employee.DEPARTMENT_FK);
		assertThrows(IllegalStateException.class, () -> employees.persistToggle().add(Employee.JOB));
		assertNotNull(employees.component(Employee.NAME).label().getIcon());
		employees.editor().value(Employee.NAME).persist().set(true);
		editPanel.store(preferences);
		//the detail by its link name, the foreign key name by default
		JSONObject stored = new JSONObject(preferences.get("persist", null));
		assertFalse(stored.has("attributes"));
		assertTrue(stored.getJSONObject("details").getJSONObject(Employee.DEPARTMENT_FK.name())
						.getJSONObject("attributes").getBoolean(Employee.NAME.name()));

		DepartmentPersistPanel restoredPanel = new DepartmentPersistPanel(departmentWithEmployees());
		restoredPanel.restore(preferences);
		restoredPanel.initialize();
		EditorComponents restoredEmployees = restoredPanel.components().detail().get(Employee.DEPARTMENT_FK);
		assertTrue(restoredEmployees.editor().value(Employee.NAME).persist().is());
		assertFalse(restoredPanel.editor().value(Department.LOCATION).persist().is());
	}

	private static SwingEntityEditModel departmentWithEmployees() {
		SwingEntityEditModel departmentEditModel = new SwingEntityEditModel(Department.TYPE, CONNECTION);
		departmentEditModel.editor().detail().add(EditorLink.builder()
						.editor(new SwingEntityEditModel(Employee.TYPE, CONNECTION).editor())
						.foreignKey(Employee.DEPARTMENT_FK)
						.build());

		return departmentEditModel;
	}

	@Test
	void mergeModified() throws EntityValidationException {
		CONNECTION.startTransaction();
		try {
			SwingEntityEditModel editModel = new SwingEntityEditModel(Employee.TYPE, CONNECTION);
			List<EntityModifiedException> handled = new ArrayList<>();
			EntityEditPanel editPanel = new EntityEditPanel(editModel) {
				@Override
				protected void initializeUI() {}

				@Override
				protected void onEntityModifiedException(EntityModifiedException exception) {
					handled.add(exception);
				}
			};
			Entity allen = CONNECTION.selectSingle(Employee.NAME.equalTo("ALLEN"));
			editModel.editor().entity().set(allen);
			editModel.editor().value(Employee.NAME).set("MINE");
			// someone else raises the salary
			Entity theirs = allen.copy().mutable();
			theirs.set(Employee.SALARY, 2500d);
			CONNECTION.update(theirs);

			EntityModifiedException exception = assertThrows(EntityModifiedException.class, () -> editModel.editor().update());
			editPanel.onException(exception);
			assertEquals(singletonList(exception), handled);
			assertTrue(editPanel.merges(exception));
			assertFalse(new ConfigEditPanel(editModel, config -> config.mergeModified(false)).merges(exception));

			EntityMerge merge = editPanel.merge(exception).get();
			assertEquals(asList(Employee.NAME, Employee.SALARY), merge.attributes().stream()
							.map(AttributeMerge::attribute)
							.collect(toList()));
			// mine changed the name, theirs the salary, both pre-selected
			assertTrue(merge.complete().is());
			// as the dialog's Merge does
			editModel.editor().entity().replace(merge.merged());
			assertTrue(editModel.editor().entity().modified().is());
			editModel.editor().update();
			Entity updated = CONNECTION.select(allen.primaryKey());
			assertEquals("MINE", updated.get(Employee.NAME));
			assertEquals(2500d, updated.get(Employee.SALARY));

			// deleted by now, nothing to merge
			Entity mine = updated.copy().mutable();
			mine.set(Employee.NAME, "DELETED");
			CONNECTION.delete(updated.primaryKey());
			assertFalse(editPanel.merge(new EntityModifiedException(mine, updated, singletonList(Employee.NAME))).isPresent());
		}
		finally {
			CONNECTION.rollbackTransaction();
		}
	}

	@Test
	void mergeModifiedDetail() {
		CONNECTION.startTransaction();
		try {
			Entity operations = CONNECTION.selectSingle(Department.NAME.equalTo("OPERATIONS"));
			// the department's only employee, its detail
			Entity employee = CONNECTION.insertSelect(CONNECTION.entities().entity(Employee.TYPE)
							.with(Employee.NAME, "DETAIL")
							.with(Employee.DEPARTMENT_FK, operations)
							.with(Employee.SALARY, 1000d)
							.with(Employee.HIREDATE, LocalDate.now())
							.build());
			SwingEntityEditModel departmentEditModel = new SwingEntityEditModel(Department.TYPE, CONNECTION);
			SwingEntityEditModel employeeEditModel = new SwingEntityEditModel(Employee.TYPE, CONNECTION);
			departmentEditModel.editor().detail().add(EditorLink.builder()
							.editor(employeeEditModel.editor())
							.foreignKey(Employee.DEPARTMENT_FK)
							.build());
			EntityEditPanel editPanel = new EntityEditPanel(departmentEditModel) {
				@Override
				protected void initializeUI() {}
			};
			departmentEditModel.editor().entity().set(operations);
			assertEquals(employee, employeeEditModel.editor().entity().get());
			departmentEditModel.editor().value(Department.LOCATION).set("MINE");
			employeeEditModel.editor().value(Employee.NAME).set("MINE");
			// someone else raises the detail's salary
			Entity theirs = employee.copy().mutable();
			theirs.set(Employee.SALARY, 2000d);
			CONNECTION.update(theirs);

			EntityModifiedException exception = assertThrows(EntityModifiedException.class, () -> departmentEditModel.editor().update());
			// a detail editor's entity, which the department editor can not take, the exception displayed
			assertEquals(Employee.TYPE, exception.entity().type());
			assertFalse(editPanel.merges(exception));
		}
		finally {
			CONNECTION.rollbackTransaction();
		}
	}

	private static final class ConfigEditPanel extends EntityEditPanel {

		private ConfigEditPanel(SwingEntityEditModel editModel, java.util.function.Consumer<Config> config) {
			super(editModel, config);
		}

		@Override
		protected void initializeUI() {}
	}

	private static final class PersistEditPanel extends EntityEditPanel {

		private PersistEditPanel(SwingEntityEditModel editModel) {
			super(editModel);
		}

		@Override
		protected void initializeUI() {
			components().persistToggle().set(asList(Employee.NAME, Employee.DEPARTMENT_FK));
			create().textField(Employee.NAME);
			create().itemComboBox(Employee.JOB);
			create().comboBox(Employee.DEPARTMENT_FK);
			setLayout(Layouts.flexibleGridLayout(1, 3));
			addInputPanel(Employee.NAME);
			addInputPanel(Employee.JOB);
			addInputPanel(Employee.DEPARTMENT_FK);
		}
	}

	private static final class DepartmentPersistPanel extends EntityEditPanel {

		private DepartmentPersistPanel(SwingEntityEditModel editModel) {
			super(editModel);
		}

		@Override
		protected void initializeUI() {
			components().persistToggle().add(Department.LOCATION);
			EditorComponents employees = components().detail().get(Employee.DEPARTMENT_FK);
			employees.persistToggle().add(Employee.NAME);
			create().textField(Department.LOCATION);
			employees.create().textField(Employee.NAME);
			setLayout(Layouts.flexibleGridLayout(1, 2));
			addInputPanel(Department.LOCATION);
			add(employees.create().inputPanel(Employee.NAME).build());
		}
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
