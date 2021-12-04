/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.manual.common;

import is.codion.common.item.Item;
import is.codion.common.state.State;
import is.codion.common.value.Value;
import is.codion.swing.common.model.textfield.DocumentAdapter;
import is.codion.swing.common.ui.Utilities;
import is.codion.swing.common.ui.checkbox.NullableCheckBox;
import is.codion.swing.common.ui.combobox.SteppedComboBox;
import is.codion.swing.common.ui.component.AbstractComponentValue;
import is.codion.swing.common.ui.component.ComponentValue;
import is.codion.swing.common.ui.component.ComponentValues;
import is.codion.swing.common.ui.component.Components;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.common.ui.dialog.Dialogs;
import is.codion.swing.common.ui.textfield.BigDecimalField;
import is.codion.swing.common.ui.textfield.DoubleField;
import is.codion.swing.common.ui.textfield.IntegerField;
import is.codion.swing.common.ui.textfield.LongField;
import is.codion.swing.common.ui.textfield.TemporalField;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
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

    ComponentValue<Boolean, JCheckBox> componentValue =
            Components.checkBox(booleanValue)
                    .caption("Check")
                    .horizontalAlignment(SwingConstants.CENTER)
                    .buildComponentValue();

    JCheckBox checkBox = componentValue.getComponent();

    // end::checkBox[]
  }

  static void nullableCheckBox() {
    // tag::nullableCheckBox[]
    //nullable boolean value
    Value<Boolean> booleanValue = Value.value();

    ComponentValue<Boolean, JCheckBox> componentValue =
            Components.checkBox(booleanValue)
                    .caption("Check")
                    .nullable(true)
                    .buildComponentValue();

    NullableCheckBox nullableCheckBox =
            (NullableCheckBox) componentValue.getComponent();
    // end::nullableCheckBox[]
  }

  static void booleanComboBox() {
    // tag::booleanComboBox[]
    Value<Boolean> booleanValue = Value.value();

    ComponentValue<Boolean, SteppedComboBox<Item<Boolean>>> componentValue =
            Components.booleanComboBox(booleanValue)
                    .toolTipText("Select a value")
                    .buildComponentValue();

    SteppedComboBox<Item<Boolean>> comboBox = componentValue.getComponent();
    // end::booleanComboBox[]
  }

  static void textField() {
    // tag::textField[]
    Value<String> stringValue = Value.value();

    ComponentValue<String, JTextField> componentValue =
            Components.textField(stringValue)
                    .preferredWidth(120)
                    .transferFocusOnEnter(true)
                    .buildComponentValue();

    JTextField textField = componentValue.getComponent();
    // end::textField[]
  }

  static void textArea() {
    // tag::textArea[]
    Value<String> stringValue = Value.value();

    ComponentValue<String, JTextArea> componentValue =
            Components.textArea(stringValue)
                    .rowsColumns(10, 20)
                    .lineWrap(true)
                    .buildComponentValue();

    JTextArea textArea = componentValue.getComponent();
    // end::textArea[]
  }

  static void integerField() {
    // tag::integerField[]
    Value<Integer> integerValue = Value.value();

    ComponentValue<Integer, IntegerField> componentValue =
            Components.integerField(integerValue)
                    .range(0, 10_000)
                    .groupingUsed(false)
                    .buildComponentValue();

    IntegerField integerField = componentValue.getComponent();
    // end::integerField[]
  }

  static void longField() {
    // tag::longField[]
    Value<Long> longValue = Value.value();

    ComponentValue<Long, LongField> componentValue =
            Components.longField(longValue)
                    .groupingUsed(true)
                    .buildComponentValue();

    final LongField longField = componentValue.getComponent();
    // end::longField[]
  }

  static void doubleField() {
    // tag::doubleField[]
    Value<Double> doubleValue = Value.value();

    ComponentValue<Double, DoubleField> componentValue =
            Components.doubleField(doubleValue)
                    .maximumFractionDigits(3)
                    .decimalSeparator('.')
                    .buildComponentValue();

    DoubleField doubleField = componentValue.getComponent();
    // end::doubleField[]
  }

  static void bigDecimalField() {
    // tag::bigDecimalField[]
    Value<BigDecimal> bigDecimalValue = Value.value();

    ComponentValue<BigDecimal, BigDecimalField> componentValue =
            Components.bigDecimalField(bigDecimalValue)
                    .maximumFractionDigits(2)
                    .groupingSeparator('.')
                    .decimalSeparator(',')
                    .buildComponentValue();

    BigDecimalField decimalField = componentValue.getComponent();
    // end::bigDecimalField[]
  }

  static void localTime() {
    // tag::localTime[]
    Value<LocalTime> localTimeValue = Value.value();

    ComponentValue<LocalTime, TemporalField<LocalTime>> componentValue =
            Components.localTimeField("HH:mm:ss", localTimeValue)
                    .buildComponentValue();

    TemporalField<LocalTime> temporalField = componentValue.getComponent();
    // end::localTime[]
  }

  static void localDate() {
    // tag::localDate[]
    Value<LocalDate> localDateValue = Value.value();

    ComponentValue<LocalDate, TemporalField<LocalDate>> componentValue =
            Components.localDateField("dd-MM-yyyy", localDateValue)
                    .buildComponentValue();

    TemporalField<LocalDate> temporalField = componentValue.getComponent();
    // end::localDate[]
  }

  static void localDateTime() {
    // tag::localDateTime[]
    Value<LocalDateTime> localDateTimeValue = Value.value();

    ComponentValue<LocalDateTime, TemporalField<LocalDateTime>> componentValue =
            Components.localDateTimeField("dd-MM-yyyy HH:mm", localDateTimeValue)
                    .buildComponentValue();

    TemporalField<LocalDateTime> temporalField = componentValue.getComponent();
    // end::localDateTime[]
  }

  static void selectionComboBox() {
    // tag::selectionComboBox[]
    Value<String> stringValue = Value.value();

    DefaultComboBoxModel<String> comboBoxModel =
            new DefaultComboBoxModel<>(new String[] {"one", "two", "three"});

    ComponentValue<String, SteppedComboBox<String>> componentValue =
            Components.comboBox(comboBoxModel, stringValue)
                    .preferredWidth(160)
                    .buildComponentValue();

    SteppedComboBox<String> comboBox = componentValue.getComponent();
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
    IntegerField horizontalAlignmentField = new IntegerField(5);

    Value<Integer> horizontalAlignmentValue =
            Value.propertyValue(horizontalAlignmentField, "horizontalAlignment",
                    int.class, Utilities.propertyChangeObserver(horizontalAlignmentField, "horizontalAlignment"));

    Value<Integer> fieldValue =
            ComponentValues.integerField(horizontalAlignmentField, false);

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
