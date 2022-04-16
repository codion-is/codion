/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.manual.common;

import is.codion.common.item.Item;
import is.codion.common.state.State;
import is.codion.common.value.Value;
import is.codion.swing.common.model.component.textfield.DocumentAdapter;
import is.codion.swing.common.ui.Utilities;
import is.codion.swing.common.ui.component.AbstractComponentValue;
import is.codion.swing.common.ui.component.ComponentValue;
import is.codion.swing.common.ui.component.Components;
import is.codion.swing.common.ui.component.checkbox.NullableCheckBox;
import is.codion.swing.common.ui.component.textfield.NumberField;
import is.codion.swing.common.ui.component.textfield.TemporalField;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.common.ui.dialog.Dialogs;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JPanel;
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
            .caption("Do something")
            .mnemonic('D')
            .enabledState(somethingEnabledState)
            .build();

    JButton somethingButton = new JButton(control);

    Control.ActionCommand actionCommand = actionEvent -> {
      if ((actionEvent.getModifiers() & ActionEvent.SHIFT_MASK) != 0) {
        System.out.println("Doing something else");
      }
    };
    Control actionControl = Control.actionControlBuilder(actionCommand)
            .caption("Do something else")
            .mnemonic('S')
            .build();

    JButton somethingElseButton = new JButton(actionControl);
    // end::control[]

    // tag::toggleControl[]
    State state = State.state();

    JToggleButton toggleButton = Components.toggleButton(state)
            .caption("Change state")
            .mnemonic('C')
            .build();

    Value<Boolean> booleanValue = Value.value();

    JCheckBox checkBox = Components.checkBox(booleanValue)
            .caption("Change value")
            .mnemonic('V')
            .build();
    // end::toggleControl[]
  }

  void controls() {
    // tag::controls[]
    Controls controls = Controls.builder()
            .control(Control.builder(this::doFirst)
                    .caption("First")
                    .mnemonic('F'))
            .control(Control.builder(this::doSecond)
                    .caption("Second")
                    .mnemonic('S'))
            .control(Controls.builder()
                    .caption("Submenu")
                    .control(Control.builder(this::doSubFirst)
                            .caption("Sub-first")
                            .mnemonic('b'))
                    .control(Control.builder(this::doSubSecond)
                            .caption("Sub-second")
                            .mnemonic('u')))
            .build();

    JMenu menu = controls.createMenu();

    Control firstControl = Control.builder(this::doFirst)
            .caption("First")
            .mnemonic('F')
            .build();
    Control secondControl = Control.builder(this::doSecond)
            .caption("Second")
            .mnemonic('S')
            .build();

    Controls twoControls = Controls.builder()
            .controls(firstControl, secondControl)
            .build();

    JPanel buttonPanel = twoControls.createHorizontalButtonPanel();
    // end::controls[]
  }

  void doFirst() {}
  void doSecond() {}
  void doSubFirst() {}
  void doSubSecond() {}

  static void checkBox() {
    // tag::checkBox[]
    //non-nullable so use this value instead of null
    boolean nullValue = false;

    Value<Boolean> booleanValue = Value.value(true, nullValue);

    JCheckBox checkBox =
            Components.checkBox(booleanValue)
                    .caption("Check")
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
                    .caption("Check")
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

  static void textField() {
    // tag::textField[]
    Value<String> stringValue = Value.value();

    JTextField textField =
            Components.textField(stringValue)
                    .preferredWidth(120)
                    .transferFocusOnEnter(true)
                    .build();
    // end::textField[]
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
    ComponentValue<Integer, NumberField<Integer>> fieldValue =
            Components.integerField()
                    .buildComponentValue();

    NumberField<Integer> horizontalAlignmentField = fieldValue.getComponent();

    Value<Integer> horizontalAlignmentValue =
            Value.propertyValue(horizontalAlignmentField, "horizontalAlignment",
                    int.class, Utilities.propertyChangeObserver(horizontalAlignmentField, "horizontalAlignment"));

    fieldValue.link(horizontalAlignmentValue);

    JPanel panel = new JPanel();
    panel.add(horizontalAlignmentField);

    fieldValue.addListener(panel::revalidate);

    Dialogs.componentDialog(panel)
            .title("test")
            .show();
    // end::customTextFieldHorizontalAlignment[]
  }
}
