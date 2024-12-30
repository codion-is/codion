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
 * Copyright (c) 2013 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.swing.framework.ui.component;

import is.codion.common.item.Item;
import is.codion.common.user.User;
import is.codion.common.value.Value;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.local.LocalEntityConnectionProvider;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.swing.common.model.component.combobox.FilterComboBoxModel;
import is.codion.swing.common.ui.component.button.NullableCheckBox;
import is.codion.swing.common.ui.component.combobox.Completion;
import is.codion.swing.common.ui.component.text.TextFieldPanel;
import is.codion.swing.common.ui.component.text.UpdateOn;
import is.codion.swing.common.ui.component.value.ComponentValue;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.ui.TestDomain;
import is.codion.swing.framework.ui.TestDomain.Detail;
import is.codion.swing.framework.ui.TestDomain.Detail.EnumType;
import is.codion.swing.framework.ui.TestDomain.Master;

import org.junit.jupiter.api.Test;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToggleButton;

import static is.codion.swing.framework.ui.component.EntityComponents.entityComponents;
import static org.junit.jupiter.api.Assertions.*;

public final class EntityComponentsTest {

	private static final User UNIT_TEST_USER =
					User.parse(System.getProperty("codion.test.user", "scott:tiger"));

	private static final EntityConnectionProvider CONNECTION_PROVIDER = LocalEntityConnectionProvider.builder()
					.domain(new TestDomain())
					.user(UNIT_TEST_USER)
					.build();

	private final SwingEntityEditModel editModel = new SwingEntityEditModel(Detail.TYPE, CONNECTION_PROVIDER);
	private final EntityComponents entityComponents = entityComponents(editModel.entityDefinition());

	@Test
	void checkBox() {
		editModel.editor().defaults();
		ComponentValue<Boolean, JCheckBox> componentValue =
						entityComponents.checkBox(Detail.BOOLEAN)
										.transferFocusOnEnter(true)
										.link(editModel.value(Detail.BOOLEAN))
										.buildValue();
		JCheckBox box = componentValue.component();
		assertTrue(box.isSelected());//default value is true
		assertTrue(editModel.value(Detail.BOOLEAN).getOrThrow());

		box.doClick();

		assertFalse(box.isSelected());
		assertFalse(editModel.value(Detail.BOOLEAN).getOrThrow());

		editModel.value(Detail.BOOLEAN).set(true);
		assertTrue(box.isSelected());
	}

	@Test
	void toggleButton() {
		editModel.editor().defaults();
		ComponentValue<Boolean, JToggleButton> componentValue =
						entityComponents.toggleButton(Detail.BOOLEAN)
										.transferFocusOnEnter(true)
										.link(editModel.value(Detail.BOOLEAN))
										.buildValue();
		JToggleButton box = componentValue.component();
		assertTrue(box.isSelected());//default value is true
		assertTrue(editModel.value(Detail.BOOLEAN).getOrThrow());

		box.doClick();

		assertFalse(box.isSelected());
		assertFalse(editModel.value(Detail.BOOLEAN).getOrThrow());

		editModel.value(Detail.BOOLEAN).set(true);
		assertTrue(box.isSelected());
	}

	@Test
	void nullableCheckBox() {
		editModel.editor().defaults();
		ComponentValue<Boolean, JCheckBox> componentValue =
						entityComponents.checkBox(Detail.BOOLEAN_NULLABLE)
										.transferFocusOnEnter(true)
										.nullable(true)
										.link(editModel.value(Detail.BOOLEAN_NULLABLE))
										.buildValue();
		NullableCheckBox box = (NullableCheckBox) componentValue.component();
		assertTrue(box.isSelected());//default value is true
		assertTrue(editModel.value(Detail.BOOLEAN_NULLABLE).getOrThrow());

		box.getMouseListeners()[1].mouseClicked(null);

		assertNull(box.model().toggleState().get());
		assertNull(editModel.value(Detail.BOOLEAN_NULLABLE).get());

		editModel.value(Detail.BOOLEAN_NULLABLE).set(false);
		assertFalse(box.isSelected());
	}

	@Test
	void booleanComboBox() {
		editModel.editor().defaults();
		editModel.value(Detail.BOOLEAN).set(true);
		ComponentValue<Boolean, JComboBox<Item<Boolean>>> componentValue =
						entityComponents.booleanComboBox(Detail.BOOLEAN)
										.transferFocusOnEnter(true)
										.link(editModel.value(Detail.BOOLEAN))
										.buildValue();
		FilterComboBoxModel<Item<Boolean>> boxModel = (FilterComboBoxModel<Item<Boolean>>) componentValue.component().getModel();
		assertTrue(boxModel.selection().item().getOrThrow().value());
		boxModel.setSelectedItem(null);
		assertNull(editModel.value(Detail.BOOLEAN).get());

		editModel.value(Detail.BOOLEAN).set(false);
		assertFalse(boxModel.selection().item().getOrThrow().value());
	}

	@Test
	void itemComboBox() {
		ComponentValue<Integer, JComboBox<Item<Integer>>> componentValue =
						entityComponents.itemComboBox(Detail.INT_VALUE_LIST)
										.transferFocusOnEnter(true)
										.link(editModel.value(Detail.INT_VALUE_LIST))
										.buildValue();
		JComboBox<Item<Integer>> comboBox = componentValue.component();

		FilterComboBoxModel<Item<Integer>> model = (FilterComboBoxModel<Item<Integer>>) comboBox.getModel();
		assertEquals(0, model.items().visible().indexOf(Item.item(null)));
		assertTrue(model.items().contains(Item.item(null)));

		assertNull(editModel.value(Detail.INT_VALUE_LIST).get());
		comboBox.setSelectedItem(1);
		assertEquals(1, editModel.value(Detail.INT_VALUE_LIST).get());
		comboBox.setSelectedItem(2);
		assertEquals(2, editModel.value(Detail.INT_VALUE_LIST).get());
		comboBox.setSelectedItem(3);
		assertEquals(3, editModel.value(Detail.INT_VALUE_LIST).get());
		comboBox.setSelectedItem(4);//does not exist
		assertEquals(3, editModel.value(Detail.INT_VALUE_LIST).get());
	}

	@Test
	void nullableUnsortedItemComboBox() {
		ComponentValue<Integer, JComboBox<Item<Integer>>> componentValue =
						entityComponents.itemComboBox(Detail.INT_VALUE_LIST)
										.sorted(false)
										.buildValue();
		FilterComboBoxModel<Item<Integer>> model = (FilterComboBoxModel<Item<Integer>>) componentValue.component().getModel();

		//null item should be first, regardless of sorting
		assertEquals(0, model.items().visible().indexOf(Item.item(null)));
	}

	@Test
	void comboBox() {
		DefaultComboBoxModel<Integer> boxModel = new DefaultComboBoxModel<>(new Integer[] {0, 1, 2, 3});
		ComponentValue<Integer, JComboBox<Integer>> componentValue =
						entityComponents.comboBox(Detail.INT, boxModel)
										.completionMode(Completion.Mode.NONE)//otherwise a non-existing element can be selected, last test fails
										.transferFocusOnEnter(true)
										.link(editModel.value(Detail.INT))
										.buildValue();
		JComboBox<Integer> box = componentValue.component();

		assertNull(editModel.value(Detail.INT).get());
		box.setSelectedItem(1);
		assertEquals(1, editModel.value(Detail.INT).get());
		box.setSelectedItem(2);
		assertEquals(2, editModel.value(Detail.INT).get());
		box.setSelectedItem(3);
		assertEquals(3, editModel.value(Detail.INT).get());
		box.setSelectedItem(4);//does not exist
		assertEquals(3, editModel.value(Detail.INT).get());
	}

	@Test
	void enumComboBox() {
		JComboBox<?> comboBox = (JComboBox<?>) entityComponents.component(Detail.ENUM_TYPE).build();
		FilterComboBoxModel<EnumType> comboBoxModel = (FilterComboBoxModel<EnumType>) comboBox.getModel();
		comboBoxModel.items().refresh();
		assertEquals(4, comboBoxModel.getSize());
		for (EnumType enumType : EnumType.values()) {
			assertTrue(comboBoxModel.items().contains(enumType));
		}
	}

	@Test
	void textField() {
		ComponentValue<String, JTextField> componentValue =
						entityComponents.textField(Detail.STRING)
										.columns(10)
										.upperCase(true)
										.selectAllOnFocusGained(true)
										.link(editModel.value(Detail.STRING))
										.buildValue();
		JTextField field = componentValue.component();
		field.setText("hello");
		assertEquals("HELLO", editModel.value(Detail.STRING).get());

		entityComponents.textField(Detail.DATE)
						.link(editModel.value(Detail.DATE))
						.buildValue();
		entityComponents.textField(Detail.TIME)
						.link(editModel.value(Detail.TIME))
						.buildValue();
		entityComponents.textField(Detail.TIMESTAMP)
						.link(editModel.value(Detail.TIMESTAMP))
						.buildValue();
		entityComponents.textField(Detail.OFFSET)
						.link(editModel.value(Detail.OFFSET))
						.buildValue();
	}

	@Test
	void textArea() {
		ComponentValue<String, JTextArea> componentValue =
						entityComponents.textArea(Detail.STRING)
										.transferFocusOnEnter(true)
										.rowsColumns(4, 2)
										.updateOn(UpdateOn.VALUE_CHANGE)
										.lineWrap(true)
										.wrapStyleWord(true)
										.link(editModel.value(Detail.STRING))
										.buildValue();
		JTextArea textArea = componentValue.component();
		textArea.setText("hello");
		assertEquals("hello", editModel.value(Detail.STRING).get());
	}

	@Test
	void textFieldPanel() {
		ComponentValue<String, TextFieldPanel> componentValue =
						entityComponents.textFieldPanel(Detail.STRING)
										.transferFocusOnEnter(true)
										.columns(10)
										.buttonFocusable(true)
										.updateOn(UpdateOn.VALUE_CHANGE)
										.link(editModel.value(Detail.STRING))
										.buildValue();
		TextFieldPanel inputPanel = componentValue.component();
		inputPanel.setText("hello");
		assertEquals("hello", editModel.value(Detail.STRING).get());
	}

	@Test
	void maskedTextField() {
		ComponentValue<String, JFormattedTextField> componentValue =
						entityComponents.maskedTextField(Detail.STRING)
										.mask("##:##")
										.valueContainsLiteralCharacters(true)
										.columns(6)
										.commitsOnValidEdit(true)
										.focusLostBehaviour(JFormattedTextField.COMMIT)
										.link(editModel.value(Detail.STRING))
										.buildValue();
		JFormattedTextField field = componentValue.component();
		field.setText("1234");
		assertEquals("12:34", editModel.value(Detail.STRING).get());
	}

	@Test
	void entityTextField() {
		ComponentValue<Entity, JTextField> componentValue =
						entityComponents.textField(Detail.MASTER_FK)
										.link(editModel.value(Detail.MASTER_FK))
										.buildValue();
		JTextField field = componentValue.component();
		Entity entity = editModel.entities().builder(Master.TYPE).with(Master.NAME, "name").build();
		editModel.value(Detail.MASTER_FK).set(entity);
		assertEquals("name", field.getText());
	}

	@Test
	void searchField() {
		entityComponents.searchField(Detail.MASTER_FK, editModel.searchModel(Detail.MASTER_FK))
						.columns(20)
						.upperCase(true)
						.lowerCase(false)
						.searchHintEnabled(true)
						.buildValue();
	}

	@Test
	void entityComboBox() {
		entityComponents.comboBox(Detail.MASTER_FK, editModel.comboBoxModel(Detail.MASTER_FK))
						.link(editModel.value(Detail.MASTER_FK))
						.buildValue();
	}

	@Test
	void integerSpinner() {
		Value<Integer> value = Value.nullable();
		ComponentValue<Integer, JSpinner> componentValue =
						entityComponents.integerSpinner(Detail.INT)
										.link(value)
										.buildValue();
		JSpinner spinner = componentValue.component();
		value.set(100);
		assertEquals(100, componentValue.get());
		spinner.setValue(42);
		assertEquals(42, value.get());
	}

	@Test
	void doubleSpinner() {
		Value<Double> value = Value.nullable();
		ComponentValue<Double, JSpinner> componentValue =
						entityComponents.doubleSpinner(Detail.DOUBLE)
										.link(value)
										.buildValue();
		JSpinner spinner = componentValue.component();
		value.set(100d);
		assertEquals(100d, componentValue.get());
		spinner.setValue(42d);
		assertEquals(42d, value.get());
	}

	@Test
	void slider() {
		Value<Integer> value = Value.nullable();
		ComponentValue<Integer, JSlider> componentValue =
						entityComponents.slider(Detail.INT)
										.link(value)
										.buildValue();
		JSlider slider = componentValue.component();
		value.set(100);
		assertEquals(100, slider.getValue());
		slider.setValue(42);
		assertEquals(42, value.get());
	}

	@Test
	void component() {
		EntityDefinition definition = CONNECTION_PROVIDER.entities().definition(Detail.TYPE);
		definition.columns().definitions()
						.forEach(columnDefinition -> entityComponents.component(columnDefinition.attribute()).build());
	}
}
