/*
 * Copyright (c) 2004 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.manual.common;

import is.codion.common.item.Item;
import is.codion.common.state.State;
import is.codion.common.value.Value;
import is.codion.swing.common.model.component.text.DocumentAdapter;
import is.codion.swing.common.ui.component.Components;
import is.codion.swing.common.ui.component.button.NullableCheckBox;
import is.codion.swing.common.ui.component.text.NumberField;
import is.codion.swing.common.ui.component.text.TemporalField;
import is.codion.swing.common.ui.component.value.AbstractComponentValue;
import is.codion.swing.common.ui.component.value.ComponentValue;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.Controls;

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

public final class InputControls {

  static void control() {
    // tag::control[]
    State somethingEnabledState = State.state(true);

    Control control = Control.builder(() -> System.out.println("Doing something"))
            .name("Do something")
            .mnemonic('D')
            .enabled(somethingEnabledState)
            .build();

    JButton somethingButton = new JButton(control);

    Control.ActionCommand actionCommand = actionEvent -> {
      if ((actionEvent.getModifiers() & ActionEvent.SHIFT_MASK) != 0) {
        System.out.println("Doing something else");
      }
    };
    Control actionControl = Control.actionControlBuilder(actionCommand)
            .name("Do something else")
            .mnemonic('S')
            .build();

    JButton somethingElseButton = new JButton(actionControl);
    // end::control[]

    // tag::toggleControl[]
    State state = State.state();

    JToggleButton toggleButton = Components.toggleButton(state)
            .text("Change state")
            .mnemonic('C')
            .build();

    Value<Boolean> booleanValue = Value.value();

    JCheckBox checkBox = Components.checkBox(booleanValue)
            .text("Change value")
            .mnemonic('V')
            .build();
    // end::toggleControl[]
  }

  void controls() {
    // tag::controls[]
    Controls controls = Controls.builder()
            .control(Control.builder(this::doFirst)
                    .name("First")
                    .mnemonic('F'))
            .control(Control.builder(this::doSecond)
                    .name("Second")
                    .mnemonic('S'))
            .control(Controls.builder()
                    .name("Submenu")
                    .control(Control.builder(this::doSubFirst)
                            .name("Sub-first")
                            .mnemonic('b'))
                    .control(Control.builder(this::doSubSecond)
                            .name("Sub-second")
                            .mnemonic('u')))
            .build();

    JMenu menu = Components.menu(controls).build();

    Control firstControl = Control.builder(this::doFirst)
            .name("First")
            .mnemonic('F')
            .build();
    Control secondControl = Control.builder(this::doSecond)
            .name("Second")
            .mnemonic('S')
            .build();

    Controls twoControls = Controls.builder()
            .controls(firstControl, secondControl)
            .build();

    JPanel buttonPanel = Components.buttonPanel(twoControls).build();
    // end::controls[]
  }

  void doFirst() {}
  void doSecond() {}
  void doSubFirst() {}
  void doSubSecond() {}

  static void basics() {
    // tag::basics[]
    //an integer based value, initialized to 42
    Value<Integer> integerValue = Value.value(42);

    //create a spinner linked to the value
    JSpinner spinner =
            Components.integerSpinner(integerValue)
                    .build();

    //create a NumberField component value, basically doing the same as
    //the above, with an extra step to expose the underlying ComponentValue
    ComponentValue<Integer, NumberField<Integer>> numberFieldValue =
            Components.integerField()
                    .buildValue();

    //linked to the same value
    numberFieldValue.link(integerValue);

    //fetch the input field from the component value
    NumberField<Integer> numberField = numberFieldValue.component();
    // end::basics[]
  }

  static void checkBox() {
    // tag::checkBox[]
    //non-nullable so use this value instead of null
    boolean nullValue = false;

    Value<Boolean> booleanValue = Value.value(true, nullValue);

    JCheckBox checkBox =
            Components.checkBox(booleanValue)
                    .text("Check")
                    .horizontalAlignment(SwingConstants.CENTER)
                    .build();

    // end::checkBox[]
  }

  static void nullableCheckBox() {
    // tag::nullableCheckBox[]
    //nullable boolean value
    Value<Boolean> booleanValue = Value.value();

    NullableCheckBox checkBox =
            (NullableCheckBox) Components.checkBox(booleanValue)
                    .text("Check")
                    .nullable(true)
                    .build();
    // end::nullableCheckBox[]
  }

  static void booleanComboBox() {
    // tag::booleanComboBox[]
    Value<Boolean> booleanValue = Value.value();

    JComboBox<Item<Boolean>> comboBox =
            Components.booleanComboBox(booleanValue)
                    .toolTipText("Select a value")
                    .build();
    // end::booleanComboBox[]
  }

  static void stringField() {
    // tag::stringField[]
    Value<String> stringValue = Value.value();

    JTextField textField =
            Components.stringField(stringValue)
                    .preferredWidth(120)
                    .transferFocusOnEnter(true)
                    .build();
    // end::stringField[]
  }

  static void characterField() {
    // tag::characterField[]
    Value<Character> characterValue = Value.value();

    JTextField textField =
            Components.characterField(characterValue)
                    .preferredWidth(120)
                    .transferFocusOnEnter(true)
                    .build();
    // end::characterField[]
  }

  static void textArea() {
    // tag::textArea[]
    Value<String> stringValue = Value.value();

    JTextArea textArea =
            Components.textArea(stringValue)
                    .rowsColumns(10, 20)
                    .lineWrap(true)
                    .build();
    // end::textArea[]
  }

  static void integerField() {
    // tag::integerField[]
    Value<Integer> integerValue = Value.value();

    NumberField<Integer> integerField =
            Components.integerField(integerValue)
                    .valueRange(0, 10_000)
                    .groupingUsed(false)
                    .build();
    // end::integerField[]
  }

  static void longField() {
    // tag::longField[]
    Value<Long> longValue = Value.value();

    NumberField<Long> longField =
            Components.longField(longValue)
                    .groupingUsed(true)
                    .build();
    // end::longField[]
  }

  static void doubleField() {
    // tag::doubleField[]
    Value<Double> doubleValue = Value.value();

    NumberField<Double> doubleField =
            Components.doubleField(doubleValue)
                    .maximumFractionDigits(3)
                    .decimalSeparator('.')
                    .build();
    // end::doubleField[]
  }

  static void bigDecimalField() {
    // tag::bigDecimalField[]
    Value<BigDecimal> bigDecimalValue = Value.value();

    NumberField<BigDecimal> bigDecimalField =
            Components.bigDecimalField(bigDecimalValue)
                    .maximumFractionDigits(2)
                    .groupingSeparator('.')
                    .decimalSeparator(',')
                    .build();
    // end::bigDecimalField[]
  }

  static void localTime() {
    // tag::localTime[]
    Value<LocalTime> localTimeValue = Value.value();

    TemporalField<LocalTime> temporalField =
            Components.localTimeField("HH:mm:ss", localTimeValue)
                    .build();
    // end::localTime[]
  }

  static void localDate() {
    // tag::localDate[]
    Value<LocalDate> localDateValue = Value.value();

    TemporalField<LocalDate> temporalField =
            Components.localDateField("dd-MM-yyyy", localDateValue)
                    .build();
    // end::localDate[]
  }

  static void localDateTime() {
    // tag::localDateTime[]
    Value<LocalDateTime> localDateTimeValue = Value.value();

    TemporalField<LocalDateTime> temporalField =
            Components.localDateTimeField("dd-MM-yyyy HH:mm", localDateTimeValue)
                    .build();
    // end::localDateTime[]
  }

  static void selectionComboBox() {
    // tag::selectionComboBox[]
    Value<String> stringValue = Value.value();

    DefaultComboBoxModel<String> comboBoxModel =
            new DefaultComboBoxModel<>(new String[] {"one", "two", "three"});

    JComboBox<String> comboBox =
            Components.comboBox(comboBoxModel, stringValue)
                    .preferredWidth(160)
                    .build();
    // end::selectionComboBox[]
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

    Value<Person> personValue = Value.value();

    PersonPanel personPanel = new PersonPanel();

    Value<Person> personPanelValue = new PersonPanelValue(personPanel);

    personPanelValue.link(personValue);
    // end::customTextFields[]
  }
}
