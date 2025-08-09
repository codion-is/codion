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
 * Copyright (c) 2021 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component;

import is.codion.common.item.Item;
import is.codion.common.model.filter.FilterModel.VisibleItems;
import is.codion.common.state.State;
import is.codion.common.value.Value;
import is.codion.common.value.ValueList;
import is.codion.common.value.ValueSet;
import is.codion.swing.common.model.component.combobox.FilterComboBoxModel;
import is.codion.swing.common.model.component.list.FilterListModel;
import is.codion.swing.common.ui.component.button.NullableCheckBox;
import is.codion.swing.common.ui.component.button.ToggleButtonType;
import is.codion.swing.common.ui.component.combobox.Completion;
import is.codion.swing.common.ui.component.list.FilterList;
import is.codion.swing.common.ui.component.list.ListBuilder;
import is.codion.swing.common.ui.component.panel.InputPanelLayout;
import is.codion.swing.common.ui.component.text.NumberField;
import is.codion.swing.common.ui.component.text.TemporalField;
import is.codion.swing.common.ui.component.text.TemporalFieldPanel;
import is.codion.swing.common.ui.component.text.TextAreaBuilder;
import is.codion.swing.common.ui.component.text.TextComponentBuilder.CaretPosition;
import is.codion.swing.common.ui.component.text.TextFieldPanel;
import is.codion.swing.common.ui.component.text.UpdateOn;
import is.codion.swing.common.ui.component.value.ComponentValue;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.common.ui.control.Controls.ControlsBuilder;
import is.codion.swing.common.ui.control.ToggleControl;
import is.codion.swing.common.ui.icon.Logos;

import org.junit.jupiter.api.Test;

import javax.swing.AbstractAction;
import javax.swing.DefaultBoundedRangeModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.SpinnerListModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.ComponentOrientation;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static is.codion.common.item.Item.item;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.*;

public final class ComponentsTest {

	private final Controls controls = Controls.builder()
					.caption("SubMenu")
					.controls(Control.builder()
													.command(() -> {})
													.caption("one"),
									Control.builder()
													.command(() -> {})
													.caption("two"),
									Control.builder()
													.toggle(State.state())
													.caption("three"))
					.build();

	@Test
	void testDoubleLink() {
		Value<Long> value = Value.nullable();
		ComponentValue<Long, NumberField<Long>> componentValue = Components.longField()
						.link(value)
						.buildValue();
		assertThrows(IllegalStateException.class, () -> componentValue.link(value));
	}

	@Test
	void shortField() {
		Value<Short> value = Value.nullable((short) 42);
		ComponentValue<Short, NumberField<Short>> componentValue = Components.shortField()
						.valueRange(0, 100)
						.font(Font.getFont("arial"))
						.minimumHeight(10)
						.minimumWidth(10)
						.foreground(Color.WHITE)
						.background(Color.BLACK)
						.link(value)
						.buildValue();
		assertEquals("42", componentValue.component().getText());
	}

	@Test
	void integerField() {
		Value<Integer> value = Value.nullable(42);
		ComponentValue<Integer, NumberField<Integer>> componentValue = Components.integerField()
						.valueRange(0, 100)
						.font(Font.getFont("arial"))
						.minimumHeight(10)
						.minimumWidth(10)
						.foreground(Color.WHITE)
						.background(Color.BLACK)
						.link(value)
						.buildValue();
		assertEquals("42", componentValue.component().getText());
	}

	@Test
	void longField() {
		Value<Long> value = Value.nullable(42L);
		ComponentValue<Long, NumberField<Long>> componentValue = Components.longField()
						.valueRange(0, 100)
						.groupingSeparator('.')
						.maximumHeight(10)
						.maximumWidth(10)
						.link(value)
						.buildValue();
		assertEquals("42", componentValue.component().getText());
	}

	@Test
	void doubleField() {
		Value<Double> value = Value.nullable(42.2);
		ComponentValue<Double, NumberField<Double>> componentValue = Components.doubleField()
						.valueRange(0, 100)
						.maximumFractionDigits(2)
						.groupingSeparator('.')
						.decimalSeparator(',')
						.minimumSize(new Dimension(10, 10))
						.maximumSize(new Dimension(10, 10))
						.link(value)
						.buildValue();
		assertEquals(componentValue.component().get(), value.get());
	}

	@Test
	void bigDecimalField() {
		Value<BigDecimal> value = Value.nullable(BigDecimal.valueOf(42.2));
		ComponentValue<BigDecimal, NumberField<BigDecimal>> componentValue = Components.bigDecimalField()
						.maximumFractionDigits(2)
						.groupingSeparator('.')
						.decimalSeparator(',')
						.maximumSize(new Dimension(10, 10))
						.link(value)
						.buildValue();
		assertEquals(componentValue.component().get(), value.get());
	}

	@Test
	void localTimeField() {
		Value<LocalTime> value = Value.nullable(LocalTime.now());
		ComponentValue<LocalTime, TemporalField<LocalTime>> componentValue =
						Components.localTimeField()
										.dateTimePattern("HH:mm")
										.focusLostBehaviour(JFormattedTextField.COMMIT)
										.link(value)
										.buildValue();
		assertEquals(componentValue.get(), value.get().truncatedTo(ChronoUnit.MINUTES));
	}

	@Test
	void localDateField() {
		Value<LocalDate> value = Value.nullable(LocalDate.now());
		ComponentValue<LocalDate, TemporalField<LocalDate>> componentValue =
						Components.localDateField()
										.dateTimePattern("dd-MM-yyyy")
										.focusLostBehaviour(JFormattedTextField.COMMIT)
										.link(value)
										.buildValue();
		assertEquals(componentValue.get(), value.get());
	}

	@Test
	void localDateTimeField() {
		Value<LocalDateTime> value = Value.nullable(LocalDateTime.now());
		ComponentValue<LocalDateTime, TemporalField<LocalDateTime>> componentValue =
						Components.localDateTimeField()
										.dateTimePattern("dd-MM-yyyy HH:mm")
										.focusLostBehaviour(JFormattedTextField.COMMIT)
										.link(value)
										.buildValue();
		assertEquals(componentValue.get(), value.get().truncatedTo(ChronoUnit.MINUTES));
	}

	@Test
	void offsetDateTimeField() {
		OffsetDateTime now = OffsetDateTime.now();
		Value<OffsetDateTime> value = Value.nullable(now);
		ComponentValue<OffsetDateTime, TemporalField<OffsetDateTime>> componentValue =
						Components.offsetDateTimeField()
										.dateTimePattern("dd-MM-yyyy HH:mm.ss")
										.focusLostBehaviour(JFormattedTextField.COMMIT)
										.link(value)
										.buildValue();
		assertEquals(now.truncatedTo(ChronoUnit.SECONDS), componentValue.get());
	}

	@Test
	void temporalFieldPanel() {
		LocalDate date = LocalDate.now();

		Value<LocalDate> value = Value.nullable();
		ComponentValue<LocalDate, TemporalFieldPanel<LocalDate>> componentValue =
						Components.temporalFieldPanel()
										.temporalClass(LocalDate.class)
										.link(value)
										.dateTimePattern("dd-MM-yyyy")
										.columns(8)
										.selectAllOnFocusGained(true)
										.updateOn(UpdateOn.VALUE_CHANGE)
										.buildValue();
		assertNull(componentValue.get());

		componentValue.component().set(date);

		assertEquals(value.get(), date);

		componentValue.component().temporalField().setText(DateTimeFormatter.ofPattern("dd-MM-yyyy").format(date));
		assertEquals(date, componentValue.get());
	}

	@Test
	void button() {
		Components.button()
						.iconTextGap(5)
						.action(Control.command(() -> {}))
						.preferredSize(new Dimension(10, 10))
						.borderPainted(false)
						.focusPainted(false)
						.rolloverEnabled(true)
						.multiClickThreshold(10)
						.contentAreaFilled(false)
						.buildValue();
	}

	@Test
	void checkBox() {
		Value<Boolean> value = Value.builder()
						.nonNull(false)
						.value(true)
						.build();
		ComponentValue<Boolean, JCheckBox> componentValue = Components.checkBox()
						.link(value)
						.text("caption")
						.horizontalAlignment(SwingConstants.CENTER)
						.includeText(true)
						.transferFocusOnEnter(true)
						.buildValue();
		JCheckBox button = componentValue.component();
		assertTrue(button.isSelected());
		assertTrue(value.getOrThrow());

		button.doClick();

		assertFalse(button.isSelected());
		assertFalse(value.getOrThrow());

		value.set(true);
		assertTrue(button.isSelected());

		State enabledState = State.state(true);
		State state = State.state();
		ToggleControl toggleControl = Control.builder()
						.toggle(state)
						.enabled(enabledState)
						.build();
		JCheckBox checkBox = Components.checkBox()
						.toggle(toggleControl)
						.build();
		state.set(true);
		assertTrue(toggleControl.value().getOrThrow());
		toggleControl.value().set(false);
		assertFalse(state.is());

		SwingUtilities.invokeLater(() -> {
			enabledState.set(false);
			assertFalse(checkBox.isEnabled());
			enabledState.set(true);
			assertTrue(checkBox.isEnabled());
		});
	}

	@Test
	void checkBoxLinkState() {
		State state = State.state(true);
		JCheckBox checkBox = Components.checkBox()
						.link(state)
						.build();
		assertTrue(state.is());
		assertTrue(checkBox.isSelected());
	}

	@Test
	void toggleButton() {
		Value<Boolean> value = Value.builder()
						.nonNull(false)
						.value(true)
						.build();
		ComponentValue<Boolean, JToggleButton> componentValue = Components.toggleButton()
						.link(value)
						.text("caption")
						.includeText(true)
						.transferFocusOnEnter(true)
						.buildValue();
		JToggleButton button = componentValue.component();
		assertTrue(button.isSelected());
		assertTrue(value.getOrThrow());

		button.doClick();

		assertFalse(button.isSelected());
		assertFalse(value.getOrThrow());

		value.set(true);
		assertTrue(button.isSelected());

		State enabledState = State.state(true);
		State state = State.state();
		ToggleControl toggleControl = Control.builder()
						.toggle(state)
						.enabled(enabledState)
						.build();
		JToggleButton toggleButton = Components.toggleButton()
						.toggle(toggleControl)
						.buildValue()
						.component();
		state.set(true);
		assertTrue(toggleControl.value().getOrThrow());
		toggleControl.value().set(false);
		assertFalse(state.is());

		SwingUtilities.invokeLater(() -> {
			enabledState.set(false);
			assertFalse(toggleButton.isEnabled());
			enabledState.set(true);
			assertTrue(toggleButton.isEnabled());
		});
	}

	@Test
	void radioButton() {
		assertThrows(IllegalArgumentException.class, () -> Components.radioButton()
						.link((Value.nullable())));

		Value<Boolean> value = Value.builder()
						.nonNull(false)
						.value(true)
						.build();
		ComponentValue<Boolean, JRadioButton> componentValue = Components.radioButton()
						.link(value)
						.text("caption")
						.includeText(true)
						.transferFocusOnEnter(true)
						.buildValue();
		JRadioButton button = componentValue.component();
		assertTrue(button.isSelected());
		assertTrue(value.getOrThrow());

		button.doClick();

		assertFalse(button.isSelected());
		assertFalse(value.getOrThrow());

		value.set(true);
		assertTrue(button.isSelected());

		State enabledState = State.state(true);
		State state = State.state();
		ToggleControl toggleControl = Control.builder()
						.toggle(state)
						.enabled(enabledState)
						.build();
		JRadioButton radioButton = Components.radioButton()
						.toggle(toggleControl)
						.buildValue()
						.component();
		state.set(true);
		assertTrue(toggleControl.value().getOrThrow());
		toggleControl.value().set(false);
		assertFalse(state.is());

		SwingUtilities.invokeLater(() -> {
			enabledState.set(false);
			assertFalse(radioButton.isEnabled());
			enabledState.set(true);
			assertTrue(radioButton.isEnabled());
		});
	}

	@Test
	void menuItem() {
		Components.menuItem();
		Control control = Control.command(() -> {});
		Components.menuItem().control(control).buildValue();
	}

	@Test
	void checkBoxMenuItem() {
		assertThrows(IllegalArgumentException.class, () -> Components.checkBoxMenuItem().link(Value.nullable()));

		State enabledState = State.state(true);
		State state = State.state(true);
		ToggleControl toggleControl = Control.builder()
						.toggle(state)
						.enabled(enabledState)
						.build();
		JCheckBoxMenuItem checkBox = Components.checkBoxMenuItem()
						.toggle(toggleControl)
						.build();
		assertTrue(toggleControl.value().getOrThrow());
		toggleControl.value().set(false);
		assertFalse(state.is());
		checkBox.setSelected(true);
		assertTrue(state.is());

		SwingUtilities.invokeLater(() -> {
			enabledState.set(false);
			assertFalse(checkBox.isEnabled());
			enabledState.set(true);
			assertTrue(checkBox.isEnabled());
		});
	}

	@Test
	void radioButtonMenuItem() {
		assertThrows(IllegalArgumentException.class, () -> Components.radioButtonMenuItem()
						.link(Value.nullable()));

		State enabledState = State.state(true);
		State state = State.state(true);
		ToggleControl toggleControl = Control.builder()
						.toggle(state)
						.enabled(enabledState)
						.build();
		JRadioButtonMenuItem button = Components.radioButtonMenuItem()
						.toggle(toggleControl)
						.buildValue()
						.component();
		assertTrue(toggleControl.value().getOrThrow());
		toggleControl.value().set(false);
		assertFalse(state.is());
		button.setSelected(true);
		assertTrue(state.is());

		SwingUtilities.invokeLater(() -> {
			enabledState.set(false);
			assertFalse(button.isEnabled());
			enabledState.set(true);
			assertTrue(button.isEnabled());
		});
	}

	@Test
	void nullableCheckBox() {
		Value<Boolean> value = Value.nullable(true);
		ComponentValue<Boolean, JCheckBox> componentValue = Components.checkBox()
						.link(value)
						.transferFocusOnEnter(true)
						.nullable(true)
						.buildValue();
		NullableCheckBox box = (NullableCheckBox) componentValue.component();
		assertTrue(box.isSelected());
		assertTrue(value.getOrThrow());

		box.getMouseListeners()[1].mouseClicked(null);

		assertNull(box.model().get());
		assertNull(value.get());

		value.set(false);
		assertFalse(box.isSelected());
	}

	@Test
	void booleanComboBox() {
		Value<Boolean> value = Value.nullable(true);
		ComponentValue<Boolean, JComboBox<Item<Boolean>>> componentValue =
						Components.booleanComboBox()
										.maximumRowCount(5)
										.transferFocusOnEnter(true)
										.link(value)
										.buildValue();
		FilterComboBoxModel<Item<Boolean>> boxModel =
						(FilterComboBoxModel<Item<Boolean>>) componentValue.component().getModel();
		assertTrue(boxModel.selection().item().getOrThrow().getOrThrow());
		boxModel.setSelectedItem(null);
		assertNull(value.get());

		value.set(false);
		assertFalse(boxModel.selection().item().getOrThrow().getOrThrow());
	}

	@Test
	void itemComboBox() {
		List<Item<Integer>> items = asList(item(0, "0"), item(1, "1"),
						item(2, "2"), item(3, "3"));
		Value<Integer> value = Value.nullable();
		ComponentValue<Integer, JComboBox<Item<Integer>>> componentValue = Components.itemComboBox()
						.items(items)
						.mouseWheelScrollingWithWrapAround(true)
						.transferFocusOnEnter(true)
						.sorted(true)
						.link(value)
						.nullable(true)
						.buildValue();
		JComboBox<Item<Integer>> comboBox = componentValue.component();
		FilterComboBoxModel<Item<Integer>> model = (FilterComboBoxModel<Item<Integer>>) comboBox.getModel();
		assertEquals(0, model.items().visible().indexOf(null));
		assertTrue(model.items().contains(null));

		assertNull(value.get());
		comboBox.setSelectedItem(1);
		assertEquals(1, value.get());
		comboBox.setSelectedItem(2);
		assertEquals(2, value.get());
		comboBox.setSelectedItem(3);
		assertEquals(3, value.get());
		comboBox.setSelectedItem(4);//does not exist
		assertNull(value.get());
	}

	@Test
	void comboBox() {
		DefaultComboBoxModel<String> boxModel = new DefaultComboBoxModel<>(new String[] {"0", "1", "2", "3"});
		Value<String> value = Value.nullable();
		ComponentValue<String, JComboBox<String>> componentValue = Components.comboBox()
						.model(boxModel)
						.completionMode(Completion.Mode.NONE)//otherwise, a non-existing element can be selected, last test fails
						.editable(true)
						.componentOrientation(ComponentOrientation.RIGHT_TO_LEFT)
						.maximumRowCount(5)
						.link(value)
						.mouseWheelScrollingWithWrapAround(true)
						.transferFocusOnEnter(true)
						.buildValue();
		JComboBox<String> box = componentValue.component();

		assertNull(value.get());
		box.setSelectedItem("1");
		assertEquals("1", value.get());
		box.setSelectedItem("2");
		assertEquals("2", value.get());
		box.setSelectedItem("3");
		assertEquals("3", value.get());
		box.setSelectedItem("4");//does not exist, but editable
		assertEquals("4", value.get());
	}

	@Test
	void stringField() {
		Value<String> value = Value.nullable();
		ComponentValue<String, JTextField> componentValue = Components.stringField()
						.columns(10)
						.upperCase(true)
						.selectAllOnFocusGained(true)
						.action(Control.command(() -> {}))
						.format(null)
						.horizontalAlignment(SwingConstants.CENTER)
						.link(value)
						.buildValue();
		JTextField field = componentValue.component();
		field.setText("hello");
		assertEquals("HELLO", value.get());
	}

	@Test
	void textArea() {
		Value<String> value = Value.nullable();
		TextAreaBuilder builder = Components.textArea()
						.transferFocusOnEnter(true)
						.autoscrolls(true)
						.rowsColumns(4, 2)
						.updateOn(UpdateOn.VALUE_CHANGE)
						.lineWrap(true)
						.wrapStyleWord(true)
						.link(value);
		ComponentValue<String, JTextArea> componentValue = builder
						.buildValue();
		JTextArea textArea = componentValue.component();
		textArea.setText("hello");
		assertEquals("hello", value.get());
		builder.scrollPane().build();

		// initial caret position
		builder = Components.textArea()
						.value("hello there")
						.caretPosition(CaretPosition.START);
		textArea = builder.build();
		assertEquals(0, textArea.getCaretPosition());

		builder.caretPosition(CaretPosition.END);
		textArea = builder.build();
		assertEquals(textArea.getDocument().getLength(), textArea.getCaretPosition());

		Value<String> stringValue = Value.nullable("hello there");
		builder = Components.textArea()
						.link(stringValue)
						.caretPosition(CaretPosition.START);
		textArea = builder.build();
		assertEquals(0, textArea.getCaretPosition());

		builder.caretPosition(CaretPosition.END);
		textArea = builder.build();
		assertEquals(textArea.getDocument().getLength(), textArea.getCaretPosition());
	}

	@Test
	void textFieldPanel() {
		Value<String> value = Value.nullable();
		ComponentValue<String, TextFieldPanel> componentValue = Components.textFieldPanel()
						.transferFocusOnEnter(true)
						.columns(10)
						.buttonFocusable(true)
						.upperCase(false)
						.lowerCase(true)
						.selectAllOnFocusGained(true)
						.textAreaSize(new Dimension(100, 100))
						.maximumLength(10)
						.caption("caption")
						.dialogTitle("title")
						.updateOn(UpdateOn.VALUE_CHANGE)
						.link(value)
						.buildValue();
		TextFieldPanel textFieldPanel = componentValue.component();
		textFieldPanel.setText("hello");
		assertEquals("hello", value.get());

		assertEquals(value.get(), componentValue.get());

		textFieldPanel.setText("");

		assertNull(componentValue.get());

		componentValue.component().setText("tester");
		assertEquals("tester", componentValue.get());

		componentValue.component().setText("");
		assertNull(componentValue.get());

		assertThrows(IllegalArgumentException.class, () -> textFieldPanel.textField().setText("asdfasdfasdfasdfasdf"));
	}

	@Test
	void formattedTextField() {
		Value<String> value = Value.nullable();
		ComponentValue<String, JFormattedTextField> componentValue = Components.maskedTextField()
						.mask("##:##")
						.valueContainsLiteralCharacters(true)
						.columns(6)
						.commitsOnValidEdit(true)
						.placeholderCharacter('_')
						.validCharacters("12345")
						.invalidCharacters("6789")
						.focusLostBehaviour(JFormattedTextField.COMMIT)
						.emptyStringToNullValue(true)
						.link(value)
						.buildValue();
		JFormattedTextField field = componentValue.component();
		field.setText("1234");
		assertEquals("12:34", value.get());
		field.setText("");
		assertNull(value.get());
	}

	@Test
	void integerSpinner() {
		Value<Integer> value = Value.nullable(10);
		ComponentValue<Integer, JSpinner> componentValue = Components.integerSpinner()
						.minimum(0)
						.maximum(100)
						.stepSize(10)
						.mouseWheelScrolling(true)
						.transferFocusOnEnter(true)
						.link(value)
						.buildValue();
		assertEquals(10, componentValue.get());
		value.set(50);
		assertEquals(50, componentValue.get());
		componentValue.set(null);
		assertEquals(0, value.get());
		value.set(null);
		assertEquals(0, componentValue.get());

		Components.integerSpinner()
						.minimum(10)
						.value(10)
						.build();
		componentValue = Components.integerSpinner()
						.maximum(10)
						.value(10)
						.buildValue();
		assertEquals(10, componentValue.get());
		assertThrows(IllegalArgumentException.class, () -> Components.integerSpinner()
						.minimum(10)
						.value(9)
						.build());
		assertThrows(IllegalArgumentException.class, () -> Components.integerSpinner()
						.maximum(10)
						.value(11)
						.build());
	}

	@Test
	void doubleSpinner() {
		Value<Double> value = Value.nullable(10d);
		ComponentValue<Double, JSpinner> componentValue = Components.doubleSpinner()
						.minimum(0d)
						.maximum(100d)
						.stepSize(10d)
						.columns(5)
						.mouseWheelScrollingReversed(true)
						.transferFocusOnEnter(true)
						.link(value)
						.buildValue();
		assertEquals(10d, componentValue.get());
		value.set(50d);
		assertEquals(50d, componentValue.get());

		componentValue = Components.doubleSpinner()
						.minimum(0d)
						.value(null)
						.buildValue();
		assertEquals(0d, componentValue.get());
		componentValue = Components.doubleSpinner()
						.buildValue();
		assertEquals(0d, componentValue.get());

		Components.doubleSpinner()
						.minimum(10d)
						.value(10d)
						.build();
		componentValue = Components.doubleSpinner()
						.maximum(10d)
						.value(10d)
						.buildValue();
		assertEquals(10d, componentValue.get());
		assertThrows(IllegalArgumentException.class, () -> Components.doubleSpinner()
						.minimum(10d)
						.value(9d)
						.build());
		assertThrows(IllegalArgumentException.class, () -> Components.doubleSpinner()
						.maximum(10d)
						.value(11d)
						.build());
	}

	@Test
	void listSpinner() {
		Value<String> value = Value.nullable();
		ComponentValue<String, JSpinner> componentValue = Components.<String>listSpinner()
						.model(new SpinnerListModel(asList("One", "Two")))
						.columns(5)
						.horizontalAlignment(SwingConstants.CENTER)
						.mouseWheelScrolling(true)
						.editable(false)
						.transferFocusOnEnter(true)
						.link(value)
						.buildValue();
		assertEquals("One", componentValue.get());
		value.set("Two");
		assertEquals("Two", componentValue.get());
	}

	@Test
	void itemSpinner() {
		Value<Integer> value = Value.nullable();
		SpinnerListModel spinnerModel = new SpinnerListModel(asList(item(1, "One"), item(2, "Two")));
		ComponentValue<Integer, JSpinner> componentValue = Components.<Integer>itemSpinner()
						.model(spinnerModel)
						.columns(5)
						.mouseWheelScrolling(true)
						.editable(false)
						.transferFocusOnEnter(true)
						.link(value)
						.buildValue();
		assertEquals(1, componentValue.get());
		value.set(2);
		assertEquals(2, componentValue.get());
	}

	@Test
	void slider() {
		Value<Integer> value = Value.nullable(10);
		ComponentValue<Integer, JSlider> componentValue = Components.slider()
						.model(new DefaultBoundedRangeModel(0, 0, 0, 100))
						.snapToTicks(true)
						.paintTrack(true)
						.paintTicks(true)
						.paintLabels(true)
						.inverted(false)
						.minorTickSpacing(1)
						.majorTickSpacing(10)
						.mouseWheelScrollingReversed(true)
						.link(value)
						.orientation(SwingConstants.VERTICAL)
						.buildValue();
		assertEquals(10, componentValue.get());
		value.set(50);
		assertEquals(50, componentValue.get());
	}

	@Test
	void label() {
		Value<String> textValue = Value.nullable("label");
		ComponentValue<String, JLabel> componentValue = Components.<String>label()
						.text(textValue)
						.icon(Logos.logoTransparent())
						.iconTextGap(5)
						.displayedMnemonic('l')
						.labelFor(new JButton())
						.buildValue();
		assertEquals("label", componentValue.component().getText());
		textValue.set("hello");
		assertEquals("hello", componentValue.component().getText());
	}

	@Test
	void listSelectedItems() {
		FilterListModel<String> listModel = FilterListModel.builder()
						.items(asList("one", "two", "three"))
						.build();

		ValueList<String> textValue = ValueList.valueList(singletonList("two"));
		ListBuilder.SelectedItems<String> listBuilder = Components.list()
						.model(listModel)
						.selectedItems()
						.visibleRowCount(4)
						.layoutOrientation(JList.VERTICAL)
						.fixedCellHeight(10)
						.fixedCellWidth(10)
						.link(textValue);
		ComponentValue<List<String>, FilterList<String>> componentValue = listBuilder
						.buildValue();
		VisibleItems<String> visibleItems = listModel.items().visible();
		assertTrue(componentValue.component().isSelectedIndex(visibleItems.indexOf("two")));
		assertEquals(singletonList("two"), componentValue.get());
		textValue.add("three");
		assertTrue(componentValue.component().isSelectedIndex(visibleItems.indexOf("two")));
		assertTrue(componentValue.component().isSelectedIndex(visibleItems.indexOf("three")));
		assertEquals(asList("two", "three"), componentValue.get());
		listBuilder.scrollPane().build();
	}

	@Test
	void listSelectedItem() {
		FilterListModel<String> listModel = FilterListModel.builder()
						.items(asList("one", "two", "three"))
						.build();

		Value<String> textValue = Value.nullable("two");
		ListBuilder.SelectedItem<String> listBuilder = Components.list()
						.model(listModel)
						.selectedItem()
						.visibleRowCount(4)
						.layoutOrientation(JList.VERTICAL)
						.fixedCellHeight(10)
						.fixedCellWidth(10)
						.link(textValue);
		ComponentValue<String, FilterList<String>> componentValue = listBuilder
						.buildValue();
		VisibleItems<String> visibleItems = listModel.items().visible();
		assertTrue(componentValue.component().isSelectedIndex(visibleItems.indexOf("two")));
		assertEquals("two", componentValue.get());
		textValue.set("three");
		assertFalse(componentValue.component().isSelectedIndex(visibleItems.indexOf("two")));
		assertTrue(componentValue.component().isSelectedIndex(visibleItems.indexOf("three")));
		assertEquals("three", componentValue.get());
		componentValue.set("one");
		assertTrue(componentValue.component().isSelectedIndex(visibleItems.indexOf("one")));
		assertFalse(componentValue.component().isSelectedIndex(visibleItems.indexOf("two")));
		assertFalse(componentValue.component().isSelectedIndex(visibleItems.indexOf("three")));
		listBuilder.scrollPane().build();
	}

	@Test
	void listItems() {
		ValueList<String> textValue = ValueList.valueList(asList("one", "two", "three"));
		ListBuilder.Items<String> listBuilder = Components.list()
						.model(FilterListModel.builder()
										.items(asList("one", "two", "three"))
										.build())
						.items()
						.visibleRowCount(4)
						.layoutOrientation(JList.VERTICAL)
						.fixedCellHeight(10)
						.fixedCellWidth(10)
						.link(textValue);
		ComponentValue<List<String>, FilterList<String>> componentValue = listBuilder.buildValue();
		assertEquals(asList("one", "two", "three"), componentValue.get());
		textValue.add("four");
		assertEquals(asList("one", "two", "three", "four"), componentValue.get());
		FilterListModel<String> listModel = componentValue.component().getModel();
		listModel.items().remove("two");
		assertEquals(asList("one", "three", "four"), componentValue.get());
		listModel.items().remove("one");
		assertEquals(asList("three", "four"), componentValue.get());
		listBuilder.scrollPane().build();
	}

	@Test
	void multipleLinkedValues() {
		Value<String> firstValue = Value.nullable();
		Value<String> secondValue = Value.nullable();
		Value<String> thirdValue = Value.nullable();
		firstValue.link(thirdValue);
		JTextField textField = Components.stringField()
						.link(thirdValue)
						.link(secondValue.observable())
						.build();
		textField.setText("1");
		assertEquals("1", firstValue.get());
		assertEquals("1", thirdValue.get());
		assertTrue(secondValue.isNull());// read only
		firstValue.set("2");
		assertEquals("2", textField.getText());
		assertTrue(secondValue.isNull());
		secondValue.set("3");
		assertEquals("3", firstValue.get());
		assertEquals("3", thirdValue.get());
		assertEquals("3", textField.getText());
	}

	@Test
	void listBox() {
		Set<String> items = new HashSet<>(asList("one", "two", "three"));
		ComponentValue<Set<String>, JComboBox<String>> componentValue =
						Components.listBox()
										.itemValue(Components.stringField().buildValue())
										.linkedValue(ValueSet.valueSet(items))
										.buildValue();
		assertEquals(items, componentValue.get());
	}

	@Test
	void validatorInvalidValue() {
		Value.Validator<String> validator = value -> {
			if ("test".equals(value)) {
				throw new IllegalArgumentException();
			}
		};
		assertThrows(IllegalArgumentException.class, () -> Components.textField()
						.valueClass(String.class)
						.value("test")
						.validator(validator)
						.build());

		Value<String> stringValue = Value.nullable("test");
		assertThrows(IllegalArgumentException.class, () -> Components.textField()
						.valueClass(String.class)
						.link(stringValue)
						.validator(validator)
						.build());
		assertThrows(IllegalArgumentException.class, () -> Components.textField()
						.valueClass(String.class)
						.link(stringValue.observable())
						.validator(validator)
						.build());
	}

	@Test
	void addConstraintsComponent() {
		assertThrows(IllegalArgumentException.class, () -> Components.panel()
						.add(new JLabel(), new JLabel()));
	}

	@Test
	void toolBar() {
		Components.toolBar()
						.action(new AbstractAction() {
							@Override
							public void actionPerformed(ActionEvent e) {}
						})
						.borderPainted(true)
						.floatable(true)
						.rollover(true)
						.orientation(SwingConstants.VERTICAL)
						.build();
	}

	@Test
	void menuBar() {
		ControlsBuilder base = Controls.builder();
		base.control(controls);

		JMenuBar menu = Components.menu()
						.controls(base)
						.buildMenuBar();
		assertEquals(1, menu.getMenuCount());
		assertEquals("SubMenu", menu.getMenu(0).getText());
		assertEquals(3, menu.getMenu(0).getItemCount());
		assertEquals("one", menu.getMenu(0).getItem(0).getText());
		assertEquals("two", menu.getMenu(0).getItem(1).getText());
		assertEquals("three", menu.getMenu(0).getItem(2).getText());
	}

	@Test
	void popupMenu() {
		Controls base = Controls.builder()
						.control(controls)
						.build();

		Components.menu()
						.controls(base)
						.buildPopupMenu();
	}

	@Test
	void buttonPanel() {
		JPanel base = new JPanel();
		base.add(Components.buttonPanel()
						.controls(Controls.builder()
										.caption("SubMenu")
										.controls(Control.builder()
																		.command(() -> {})
																		.caption("one"),
														Control.builder()
																		.command(() -> {})
																		.caption("two"),
														Control.builder()
																		.toggle(State.state())
																		.caption("three"))
										.controls(Controls.builder()
														.control(Control.builder()
																		.command(() -> {})
																		.caption("four")))
										.build())
						.orientation(SwingConstants.VERTICAL)
						.build());

		// Regression v0.18.43, shared button builders in AbstractControlPanelBuilder
		ToggleControl toggle1 = Control.toggle(Value.nonNull(false));
		ToggleControl toggle2 = Control.toggle(Value.nonNull(false));
		ToggleControl toggle3 = Control.toggle(Value.nonNull(false));

		Components.buttonPanel()
						.controls(Controls.builder()
										.actions(toggle1, toggle2, toggle3)
										.build())
						.toggleButtonType(ToggleButtonType.CHECKBOX)
						.build();
	}

	@Test
	void inputPanel() {
		assertThrows(IllegalArgumentException.class, () -> InputPanelLayout.border()
						.labelConstraints(BorderLayout.WEST)
						.componentConstraints(BorderLayout.WEST));
		assertThrows(IllegalArgumentException.class, () -> InputPanelLayout.border()
						.componentConstraints(BorderLayout.WEST)
						.labelConstraints(BorderLayout.WEST));
	}
}
