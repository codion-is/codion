/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.manual.common;

import is.codion.common.formats.LocaleDateTimePattern;
import is.codion.common.item.Item;
import is.codion.common.state.State;
import is.codion.common.value.Value;
import is.codion.swing.common.model.combobox.BooleanComboBoxModel;
import is.codion.swing.common.model.textfield.DocumentAdapter;
import is.codion.swing.common.ui.Components;
import is.codion.swing.common.ui.checkbox.NullableCheckBox;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.common.ui.control.ToggleControl;
import is.codion.swing.common.ui.dialog.Dialogs;
import is.codion.swing.common.ui.textfield.BigDecimalField;
import is.codion.swing.common.ui.textfield.DoubleField;
import is.codion.swing.common.ui.textfield.IntegerField;
import is.codion.swing.common.ui.textfield.LongField;
import is.codion.swing.common.ui.value.AbstractComponentValue;
import is.codion.swing.common.ui.value.BooleanValues;
import is.codion.swing.common.ui.value.NumericalValues;
import is.codion.swing.common.ui.value.SelectedValues;
import is.codion.swing.common.ui.value.StringValues;
import is.codion.swing.common.ui.value.TemporalValues;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
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

    Control control = Control.builder()
            .name("Do something")
            .command(() -> System.out.println("Doing something"))
            .enabledState(somethingEnabledState)
            .mnemonic('D')
            .build();

    JButton somethingButton = control.createButton();

    Control actionControl = Control.builder()
            .name("Do something else")
            .actionCommand(actionEvent -> {
              if ((actionEvent.getModifiers() & ActionEvent.SHIFT_MASK) != 0) {
                System.out.println("Doing something else");
              }
            })
            .mnemonic('S')
            .build();

    JButton somethingElseButton = actionControl.createButton();
    // end::control[]

    // tag::toggleControl[]
    State state = State.state();

    ToggleControl stateControl = ToggleControl.builder()
            .name("Change state")
            .state(state)
            .mnemonic('C')
            .build();

    JToggleButton toggleButton = stateControl.createToggleButton();

    Value<Boolean> booleanValue = Value.value();

    ToggleControl valueControl = ToggleControl.builder()
            .name("Change value")
            .value(booleanValue)
            .mnemonic('V')
            .build();

    JCheckBox checkBox = valueControl.createCheckBox();
    // end::toggleControl[]
  }

  void controls() {
    // tag::controls[]
    Controls controls = Controls.builder()
            .control(Control.builder()
                    .name("First")
                    .mnemonic('F')
                    .command(this::doFirst))
            .control(Control.builder()
                    .name("Second")
                    .mnemonic('S')
                    .command(this::doSecond))
            .control(Controls.builder()
                    .name("Submenu")
                    .control(Control.builder()
                            .name("Sub-first")
                            .mnemonic('b')
                            .command(this::doSubFirst))
                    .control(Control.builder()
                            .name("Sub-second")
                            .mnemonic('u')
                            .command(this::doSubSecond)))
            .build();

    JMenu menu = controls.createMenu();

    Control firstControl = Control.builder()
            .name("First")
            .mnemonic('F')
            .command(this::doFirst)
            .build();
    Control secondControl = Control.builder()
            .name("Second")
            .mnemonic('S')
            .command(this::doSecond)
            .build();

    Controls flatControls = Controls.builder()
            .controls(firstControl, secondControl)
            .build();

    JPanel buttonPanel = flatControls.createHorizontalButtonPanel();
    // end::controls[]
  }

  void doFirst() {}
  void doSecond() {}
  void doSubFirst() {}
  void doSubSecond() {}

  static void checkBox() {
    // tag::checkBox[]
    //non-nullable boolean value
    boolean initialValue = true;
    boolean nullValue = false;

    Value<Boolean> booleanValue = Value.value(initialValue, nullValue);

    JCheckBox checkBox = new JCheckBox();

    BooleanValues.booleanToggleButtonValue(checkBox).link(booleanValue);

    // end::checkBox[]
  }

  static void nullableCheckBox() {
    // tag::nullableCheckBox[]
    //nullable boolean value
    Value<Boolean> booleanValue = Value.value();

    NullableCheckBox checkBox = new NullableCheckBox();

    BooleanValues.booleanToggleButtonValue(checkBox).link(booleanValue);

    // end::nullableCheckBox[]
  }

  static void booleanComboBox() {
    // tag::booleanComboBox[]
    Value<Boolean> booleanValue = Value.value();

    JComboBox<Item<Boolean>> comboBox = new JComboBox<>(new BooleanComboBoxModel());

    BooleanValues.booleanComboBoxValue(comboBox).link(booleanValue);
    // end::booleanComboBox[]
  }

  static void textField() {
    // tag::textField[]
    Value<String> stringValue = Value.value();

    JTextField textField = new JTextField();

    StringValues.stringTextComponentValue(textField).link(stringValue);
    // end::textField[]
  }

  static void textArea() {
    // tag::textArea[]
    Value<String> stringValue = Value.value();

    JTextArea textArea = new JTextArea();

    StringValues.stringTextComponentValue(textArea).link(stringValue);
    // end::textArea[]
  }

  static void integerField() {
    // tag::integerField[]
    Value<Integer> integerValue = Value.value();

    IntegerField integerField = new IntegerField();

    NumericalValues.integerFieldValue(integerField).link(integerValue);
    // end::integerField[]
  }

  static void longField() {
    // tag::longField[]
    Value<Long> longValue = Value.value();

    LongField longField = new LongField();

    NumericalValues.longFieldValue(longField).link(longValue);
    // end::longField[]
  }

  static void doubleField() {
    // tag::doubleField[]
    Value<Double> doubleValue = Value.value();

    DoubleField doubleField = new DoubleField();

    NumericalValues.doubleFieldValue(doubleField).link(doubleValue);
    // end::doubleField[]
  }

  static void bigDecimalField() {
    // tag::bigDecimalField[]
    Value<BigDecimal> bigDecimalValue = Value.value();

    BigDecimalField bigDecimalField = new BigDecimalField();

    NumericalValues.bigDecimalFieldValue(bigDecimalField).link(bigDecimalValue);
    // end::bigDecimalField[]
  }

  static void localTime() {
    // tag::localTime[]
    Value<LocalTime> localTimeValue = Value.value();

    String dateTimePattern = "HH:mm:ss";

    JFormattedTextField textField =
            new JFormattedTextField(LocaleDateTimePattern.getMask(dateTimePattern));

    TemporalValues.localTimeFieldValueBuilder()
            .component(textField)
            .dateTimePattern(dateTimePattern)
            .build()
            .link(localTimeValue);
    // end::localTime[]
  }

  static void localDate() {
    // tag::localDate[]
    Value<LocalDate> localDateValue = Value.value();

    String dateTimePattern = "dd-MM-yyyy";

    JFormattedTextField textField =
            new JFormattedTextField(LocaleDateTimePattern.getMask(dateTimePattern));

    TemporalValues.localDateFieldValueBuilder()
            .component(textField)
            .dateTimePattern(dateTimePattern)
            .build()
            .link(localDateValue);
    // end::localDate[]
  }

  static void localDateTime() {
    // tag::localDateTime[]
    Value<LocalDateTime> localDateTimeValue = Value.value();

    String dateTimePattern = "dd-MM-yyyy HH:mm";

    JFormattedTextField textField =
            new JFormattedTextField(LocaleDateTimePattern.getMask(dateTimePattern));

    TemporalValues.localDateFieldTimeValueBuilder()
            .component(textField)
            .dateTimePattern(dateTimePattern)
            .build()
            .link(localDateTimeValue);
    // end::localDateTime[]
  }

  static void selectionComboBox() {
    // tag::selectionComboBox[]
    Value<String> stringValue = Value.value();

    JComboBox<String> comboBox = new JComboBox<>(new String[] {"one", "two", "three"});

    SelectedValues.selectedValue(comboBox).link(stringValue);
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
        setLayout(new GridLayout(2, 2, 5, 5));
        add(new JLabel("First name"));
        add(new JLabel("Last name"));
        add(firstNameField);
        add(lastNameField);
      }
    }

    class PersonPanelValue extends AbstractComponentValue<Person, PersonPanel> {

      public PersonPanelValue(PersonPanel component) {
        super(component);
        //We must call notifyValueChange each time this value changes,
        //that is, when either the first or last name changes.
        component.firstNameField.getDocument()
                .addDocumentListener((DocumentAdapter) e -> notifyValueChange());
        component.lastNameField.getDocument()
                .addDocumentListener((DocumentAdapter) e -> notifyValueChange());
      }

      @Override
      protected Person getComponentValue(PersonPanel component) {
        return new Person(component.firstNameField.getText(), component.lastNameField.getText());
      }

      @Override
      protected void setComponentValue(PersonPanel component, Person value) {
        component.firstNameField.setText(value == null ? null : value.firstName);
        component.lastNameField.setText(value == null ? null : value.lastName);
      }
    }

    Value<Person> personValue = Value.value();

    PersonPanel personPanel = new PersonPanel();

    Value<Person> personPanelValue = new PersonPanelValue(personPanel);

    personPanelValue.link(personValue);
    // end::customTextFields[]
  }

  static void customTextFieldHorizontalAlignment() {
    // tag::customTextFieldHorizontalAlignment[]
    IntegerField horizontalAlignmentField = new IntegerField(5);

    Value<Integer> horizontalAlignmentValue =
            Value.propertyValue(horizontalAlignmentField, "horizontalAlignment",
                    int.class, Components.propertyChangeObserver(horizontalAlignmentField, "horizontalAlignment"));

    Value<Integer> fieldValue =
            NumericalValues.integerFieldValueBuilder()
                    .component(horizontalAlignmentField)
                    .nullable(false)
                    .build();

    fieldValue.link(horizontalAlignmentValue);

    JPanel panel = new JPanel();
    panel.add(horizontalAlignmentField);

    fieldValue.addListener(panel::revalidate);

    Dialogs.displayInDialog(null, panel, "test");
    // end::customTextFieldHorizontalAlignment[]
  }
}
