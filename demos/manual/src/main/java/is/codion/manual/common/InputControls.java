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
 * Copyright (c) 2004 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.manual.common;

import is.codion.common.item.Item;
import is.codion.common.state.State;
import is.codion.common.value.Value;
import is.codion.swing.common.model.component.combobox.FilterComboBoxModel;
import is.codion.swing.common.model.component.text.DocumentAdapter;
import is.codion.swing.common.ui.component.Components;
import is.codion.swing.common.ui.component.button.NullableCheckBox;
import is.codion.swing.common.ui.component.combobox.Completion;
import is.codion.swing.common.ui.component.text.NumberField;
import is.codion.swing.common.ui.component.text.TemporalField;
import is.codion.swing.common.ui.component.value.AbstractComponentValue;
import is.codion.swing.common.ui.component.value.ComponentValue;
import is.codion.swing.common.ui.control.CommandControl;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.common.ui.control.ToggleControl;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

public final class InputControls {

	static void control() {
		// tag::control[]
		State somethingEnabledState = State.state(true);

		CommandControl control = Control.builder()
						.command(() -> System.out.println("Doing something"))
						.caption("Do something")
						.mnemonic('D')
						.enabled(somethingEnabledState)
						.build();

		JButton somethingButton = new JButton(control);

		Control.ActionCommand actionCommand = actionEvent -> {
			if ((actionEvent.getModifiers() & ActionEvent.SHIFT_MASK) != 0) {
				System.out.println("Doing something else");
			}
		};
		CommandControl actionControl = Control.builder()
						.action(actionCommand)
						.caption("Do something else")
						.mnemonic('S')
						.build();

		JButton somethingElseButton = new JButton(actionControl);
		// end::control[]

		// tag::toggleControl[]
		State state = State.state();

		ToggleControl toggleStateControl = Control.builder()
						.toggle(state)
						.build();

		JToggleButton toggleButton = Components.toggleButton()
						.toggleControl(toggleStateControl)
						.text("Change state")
						.mnemonic('C')
						.build();

		Value<Boolean> booleanValue = Value.nonNull(false);

		ToggleControl toggleValueControl = Control.builder()
						.toggle(booleanValue)
						.build();

		JCheckBox checkBox = Components.checkBox()
						.toggleControl(toggleValueControl)
						.text("Change value")
						.mnemonic('V')
						.build();
		// end::toggleControl[]
	}

	void controls() {
		// tag::controls[]
		Controls controls = Controls.builder()
						.control(Control.builder()
										.command(this::doFirst)
										.caption("First")
										.mnemonic('F'))
						.control(Control.builder()
										.command(this::doSecond)
										.caption("Second")
										.mnemonic('S'))
						.control(Controls.builder()
										.caption("Submenu")
										.control(Control.builder()
														.command(this::doSubFirst)
														.caption("Sub-first")
														.mnemonic('b'))
										.control(Control.builder()
														.command(this::doSubSecond)
														.caption("Sub-second")
														.mnemonic('u')))
						.build();

		JMenu menu = Components.menu()
						.controls(controls)
						.build();

		Control firstControl = Control.builder()
						.command(this::doFirst)
						.caption("First")
						.mnemonic('F')
						.build();
		Control secondControl = Control.builder()
						.command(this::doSecond)
						.caption("Second")
						.mnemonic('S')
						.build();

		Controls twoControls = Controls.builder()
						.controls(firstControl, secondControl)
						.build();

		JPanel buttonPanel = Components.buttonPanel()
						.controls(twoControls)
						.build();
		// end::controls[]
	}

	void doFirst() {}

	void doSecond() {}

	void doSubFirst() {}

	void doSubSecond() {}

	static void basics() {
		// tag::basics[]
		//a nullable integer value, initialized to 42
		Value<Integer> integerValue =
						Value.nullable(42);

		//create a spinner linked to the value
		JSpinner spinner =
						Components.integerSpinner()
										.link(integerValue)
										.build();

		//create a NumberField component value, basically doing the same as
		//the above, with an extra step to expose the underlying ComponentValue
		ComponentValue<Integer, NumberField<Integer>> numberFieldValue =
						Components.integerField()
										//linked to the same value
										.link(integerValue)
										.buildValue();

		//fetch the input field from the component value
		NumberField<Integer> numberField = numberFieldValue.component();
		// end::basics[]
	}

	static void checkBox() {
		// tag::checkBox[]
		//non-nullable so use this value instead of null
		boolean nullValue = false;

		Value<Boolean> booleanValue =
						Value.builder()
										.nonNull(nullValue)
										.value(true)
										.build();

		JCheckBox checkBox =
						Components.checkBox()
										.link(booleanValue)
										.text("Check")
										.horizontalAlignment(SwingConstants.CENTER)
										.build();

		// end::checkBox[]
	}

	static void nullableCheckBox() {
		// tag::nullableCheckBox[]
		//nullable boolean value
		Value<Boolean> booleanValue = Value.nullable();

		NullableCheckBox checkBox =
						(NullableCheckBox) Components.checkBox()
										.link(booleanValue)
										.text("Check")
										.nullable(true)
										.build();
		// end::nullableCheckBox[]
	}

	static void booleanComboBox() {
		// tag::booleanComboBox[]
		Value<Boolean> booleanValue = Value.nullable();

		JComboBox<Item<Boolean>> comboBox =
						Components.booleanComboBox()
										.link(booleanValue)
										.toolTipText("Select a value")
										.build();
		// end::booleanComboBox[]
	}

	static void stringField() {
		// tag::stringField[]
		Value<String> stringValue = Value.nullable();

		JTextField textField =
						Components.stringField()
										.link(stringValue)
										.preferredWidth(120)
										.transferFocusOnEnter(true)
										.build();
		// end::stringField[]
	}

	static void characterField() {
		// tag::characterField[]
		Value<Character> characterValue = Value.nullable();

		JTextField textField =
						Components.characterField()
										.link(characterValue)
										.preferredWidth(120)
										.transferFocusOnEnter(true)
										.build();
		// end::characterField[]
	}

	static void textArea() {
		// tag::textArea[]
		Value<String> stringValue = Value.nullable();

		JTextArea textArea =
						Components.textArea()
										.link(stringValue)
										.rowsColumns(10, 20)
										.lineWrap(true)
										.build();
		// end::textArea[]
	}

	static void integerField() {
		// tag::integerField[]
		Value<Integer> integerValue = Value.nullable();

		NumberField<Integer> integerField =
						Components.integerField()
										.link(integerValue)
										.valueRange(0, 10_000)
										.groupingUsed(false)
										.build();
		// end::integerField[]
	}

	static void longField() {
		// tag::longField[]
		Value<Long> longValue = Value.nullable();

		NumberField<Long> longField =
						Components.longField()
										.link(longValue)
										.groupingUsed(true)
										.build();
		// end::longField[]
	}

	static void doubleField() {
		// tag::doubleField[]
		Value<Double> doubleValue = Value.nullable();

		NumberField<Double> doubleField =
						Components.doubleField()
										.link(doubleValue)
										.maximumFractionDigits(3)
										.decimalSeparator('.')
										.build();
		// end::doubleField[]
	}

	static void bigDecimalField() {
		// tag::bigDecimalField[]
		Value<BigDecimal> bigDecimalValue = Value.nullable();

		NumberField<BigDecimal> bigDecimalField =
						Components.bigDecimalField()
										.link(bigDecimalValue)
										.maximumFractionDigits(2)
										.groupingSeparator('.')
										.decimalSeparator(',')
										.build();
		// end::bigDecimalField[]
	}

	static void localTime() {
		// tag::localTime[]
		Value<LocalTime> localTimeValue = Value.nullable();

		TemporalField<LocalTime> temporalField =
						Components.localTimeField()
										.link(localTimeValue)
										.dateTimePattern("HH:mm:ss")
										.build();
		// end::localTime[]
	}

	static void localDate() {
		// tag::localDate[]
		Value<LocalDate> localDateValue = Value.nullable();

		TemporalField<LocalDate> temporalField =
						Components.localDateField()
										.link(localDateValue)
										.dateTimePattern("dd-MM-yyyy")
										.build();
		// end::localDate[]
	}

	static void localDateTime() {
		// tag::localDateTime[]
		Value<LocalDateTime> localDateTimeValue = Value.nullable();

		TemporalField<LocalDateTime> temporalField =
						Components.localDateTimeField()
										.link(localDateTimeValue)
										.dateTimePattern("dd-MM-yyyy HH:mm")
										.build();
		// end::localDateTime[]
	}

	static void selectionComboBox() {
		// tag::selectionComboBox[]
		Value<String> stringValue = Value.nullable();

		DefaultComboBoxModel<String> comboBoxModel =
						new DefaultComboBoxModel<>(new String[] {"one", "two", "three"});

		JComboBox<String> comboBox =
						Components.comboBox(comboBoxModel)
										.link(stringValue)
										.preferredWidth(160)
										.build();
		// end::selectionComboBox[]
	}

	static void filterComboBoxModel() {
		// tag::filterComboBoxModel[]
		Supplier<Collection<String>> items = () ->
						List.of("One", "Two", "Three");

		FilterComboBoxModel<String> model =
						FilterComboBoxModel.builder()
										.items(items)
										.nullItem("-")
										.build();

		JComboBox<String> comboBox =
						Components.comboBox(model)
										.mouseWheelScrolling(true)
										.build();

		// Hides the 'Two' item.
		model.items().visible().predicate()
						.set(item -> !item.equals("Two"));

		// Prints the selected item
		model.selection().item()
						.addConsumer(System.out::println);

		// Refreshes the items using the supplier from above
		model.items().refresh();
		// end::filterComboBoxModel[]
	}

	static void comboBoxCompletion() {
		// tag::comboBoxCompletion[]
		FilterComboBoxModel<String> model =
						FilterComboBoxModel.builder()
										.items(List.of("Jon", "Jón", "Jónsi"))
										.nullItem("-")
										.build();

		JComboBox<String> comboBox =
						Components.comboBox(model)
										// Auto completion
										.completionMode(Completion.Mode.AUTOCOMPLETE)
										// Accented characters not normalized
										.normalize(false)
										.build();
		// end::comboBoxCompletion[]
	}

	static void customTextFields() {
		// tag::customTextFields[]
		class Person {
			final String firstName;
			final String lastName;

			public Person(String firstName, String lastName) {
				this.firstName = firstName;
				this.lastName = lastName;
			}

			@Override
			public String toString() {
				return lastName + ", " + firstName;
			}
		}

		class PersonPanel extends JPanel {
			final JTextField firstNameField = new JTextField();
			final JTextField lastNameField = new JTextField();

			public PersonPanel() {
				setLayout(new GridLayout(2, 2));
				add(new JLabel("First name"));
				add(new JLabel("Last name"));
				add(firstNameField);
				add(lastNameField);
			}
		}

		class PersonPanelValue extends AbstractComponentValue<Person, PersonPanel> {

			public PersonPanelValue(PersonPanel component) {
				super(component);
				//We must call notifyListeners() each time this value changes,
				//that is, when either the first or last name changes.
				component.firstNameField.getDocument()
								.addDocumentListener((DocumentAdapter) e -> notifyListeners());
				component.lastNameField.getDocument()
								.addDocumentListener((DocumentAdapter) e -> notifyListeners());
			}

			@Override
			protected Person getComponentValue() {
				return new Person(component().firstNameField.getText(), component().lastNameField.getText());
			}

			@Override
			protected void setComponentValue(Person value) {
				component().firstNameField.setText(value == null ? null : value.firstName);
				component().lastNameField.setText(value == null ? null : value.lastName);
			}
		}

		Value<Person> personValue = Value.nullable();

		PersonPanel personPanel = new PersonPanel();

		Value<Person> personPanelValue = new PersonPanelValue(personPanel);

		personPanelValue.link(personValue);
		// end::customTextFields[]
	}
}
