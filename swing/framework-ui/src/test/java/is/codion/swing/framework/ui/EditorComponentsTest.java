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
 * Copyright (c) 2024 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.swing.framework.ui;

import is.codion.common.utilities.user.User;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.local.LocalEntityConnection;
import is.codion.framework.model.EditorLink;
import is.codion.swing.common.ui.component.Components;
import is.codion.swing.common.ui.component.text.NumberField;
import is.codion.swing.common.ui.component.value.ComponentValue;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.ui.EditorComponents.ComponentFactory;
import is.codion.swing.framework.ui.TestDomain.Department;
import is.codion.swing.framework.ui.TestDomain.Detail;
import is.codion.swing.framework.ui.TestDomain.Employee;

import org.junit.jupiter.api.Test;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import static org.junit.jupiter.api.Assertions.*;

public final class EditorComponentsTest {

	private static final User UNIT_TEST_USER =
					User.parse(System.getProperty("codion.test.user", "scott:tiger"));

	private static final EntityConnection CONNECTION = LocalEntityConnection.builder()
					.domain(new TestDomain())
					.user(UNIT_TEST_USER)
					.build();

	@Test
	void test() {
		SwingEntityEditModel editModel = new SwingEntityEditModel(Employee.TYPE, CONNECTION);
		EditorComponents components = EditorComponents.editorComponents(editModel.editor());
		ComponentFactory create = new ComponentFactory(components);
		create.textField(Employee.NAME);
		assertThrows(IllegalStateException.class, () -> create.textField(Employee.NAME));
		JTextField nameField = (JTextField) components.component(Employee.NAME).get();
		assertNotNull(nameField);
		assertThrows(IllegalStateException.class, () -> create.textField(Employee.NAME));
		assertFalse(components.component(Employee.JOB).optional().isPresent());
		assertThrows(IllegalStateException.class, () -> components.component(Employee.NAME).set(new JLabel()));

		ComponentValue<NumberField<Double>, Double> salary = Components.doubleField().buildValue();
		components.component(Employee.SALARY).set(salary);
		salary.set(2000d);
		assertEquals(salary.get(), editModel.editor().value(Employee.SALARY).get());
	}

	@Test
	void labelConfigurationKeepsTheCaption() {
		SwingEntityEditModel editModel = new SwingEntityEditModel(Employee.TYPE, CONNECTION);
		EditorComponents components = EditorComponents.editorComponents(editModel.editor());
		ComponentFactory create = new ComponentFactory(components);
		//the caption comes from the attribute definition, configuring the label must not discard it
		create.textField(Employee.NAME)
						.label(label -> label.horizontalAlignment(SwingConstants.TRAILING));
		JLabel label = components.component(Employee.NAME).label();
		assertEquals(editModel.entityDefinition().attributes().definition(Employee.NAME).caption(), label.getText());
		assertEquals(SwingConstants.TRAILING, label.getHorizontalAlignment());
	}

	@Test
	void detailComponentsShareTheSettings() {
		SwingEntityEditModel departmentEditModel = new SwingEntityEditModel(Department.TYPE, CONNECTION);
		SwingEntityEditModel employeeEditModel = new SwingEntityEditModel(Employee.TYPE, CONNECTION);
		departmentEditModel.editor().detail().add(EditorLink.builder()
						.editor(employeeEditModel.editor())
						.foreignKey(Employee.DEPARTMENT_FK)
						.build());
		EditorComponents components = EditorComponents.editorComponents(departmentEditModel.editor());
		components.defaults().textFieldColumns().set(7);
		EditorComponents detailComponents = components.detail().get(Employee.DEPARTMENT_FK);
		assertSame(components.defaults(), detailComponents.defaults());
		JTextField nameField = detailComponents.create().textField(Employee.NAME).build();
		assertEquals(7, nameField.getColumns());
	}

	@Test
	void textFieldColumns() {
		SwingEntityEditModel editModel = new SwingEntityEditModel(Employee.TYPE, CONNECTION);
		EditorComponents components = EditorComponents.editorComponents(editModel.editor());
		components.defaults().textFieldColumns().set(7);
		ComponentFactory create = components.create();
		assertEquals(7, create.textField(Employee.NAME).build().getColumns());
		assertEquals(7, create.searchField(Employee.DEPARTMENT_FK).build().getColumns());
		assertEquals(7, create.textInput(Employee.JOB).build().textField().getColumns());
		assertEquals(7, create.temporalInput(Employee.HIREDATE).build().temporalField().getColumns());
		// explicitly specified columns win
		assertEquals(3, create.textField(Employee.SALARY).columns(3).build().getColumns());
		// 0 disables the default, the field sizing to its contents
		components.defaults().textFieldColumns().set(0);
		assertEquals(0, create.textField(Employee.COMMISSION).build().getColumns());
		// the application wide default initializes the panel default
		EditorComponents.TEXT_FIELD_COLUMNS.set(20);
		try {
			assertEquals(20, EditorComponents.editorComponents(editModel.editor()).defaults().textFieldColumns().getOrThrow());
		}
		finally {
			EditorComponents.TEXT_FIELD_COLUMNS.set(12);
		}
	}

	@Test
	void derived() {
		SwingEntityEditModel editModel = new SwingEntityEditModel(Detail.TYPE, CONNECTION);
		EditorComponents components = EditorComponents.editorComponents(editModel.editor());
		ComponentFactory factory = new ComponentFactory(components);
		JTextField textField = factory.textField(Detail.INT_DERIVED).build();
		assertFalse(textField.isEnabled());
	}

	@Test
	void inputPanelLabel() {
		SwingEntityEditModel editModel = new SwingEntityEditModel(Employee.TYPE, CONNECTION);
		EditorComponents components = EditorComponents.editorComponents(editModel.editor());
		ComponentFactory create = new ComponentFactory(components);
		create.textField(Employee.NAME);
		create.textField(Employee.JOB);
		create.textField(Employee.SALARY);

		// The caption based label the factory supplies, when nothing else is specified.
		assertEquals(Employee.NAME.name(), ((JLabel) labelOf(create.inputPanel(Employee.NAME).build())).getText());

		// Overriding it, through an inherited overload and through one of the panel builder's own. Which of the
		// five label() overloads a call binds to follows from the argument's static type, so any of them has to
		// be able to replace the factory's - it is the last call that counts, not which overload was used.
		assertEquals("The job", ((JLabel) labelOf(create.inputPanel(Employee.JOB)
						.label("The job")
						.build())).getText());
		JTextField ownLabel = new JTextField("The salary");
		assertSame(ownLabel, labelOf(create.inputPanel(Employee.SALARY)
						.label(() -> ownLabel)
						.build()));
	}

	// The label is the panel's first child, the input component its second - see InputPanelLayout.
	private static JComponent labelOf(JPanel inputPanel) {
		assertEquals(2, inputPanel.getComponentCount());

		return (JComponent) inputPanel.getComponent(0);
	}
}
