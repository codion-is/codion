/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.manual.common;

import is.codion.common.item.Item;
import is.codion.common.value.Nullable;
import is.codion.common.value.Value;
import is.codion.common.value.Values;
import is.codion.swing.common.model.checkbox.NullableToggleButtonModel;
import is.codion.swing.common.model.combobox.BooleanComboBoxModel;
import is.codion.swing.common.model.textfield.DocumentAdapter;
import is.codion.swing.common.ui.Components;
import is.codion.swing.common.ui.checkbox.NullableCheckBox;
import is.codion.swing.common.ui.dialog.Dialogs;
import is.codion.swing.common.ui.textfield.BigDecimalField;
import is.codion.swing.common.ui.textfield.DoubleField;
import is.codion.swing.common.ui.textfield.IntegerField;
import is.codion.swing.common.ui.textfield.LongField;
import is.codion.swing.common.ui.value.AbstractComponentValue;
import is.codion.swing.common.ui.value.BooleanValues;
import is.codion.swing.common.ui.value.NumericalValues;
import is.codion.swing.common.ui.value.SelectedValues;
import is.codion.swing.common.ui.value.TemporalValues;
import is.codion.swing.common.ui.value.TextValues;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import java.awt.GridLayout;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public final class InputControls {

  static void checkBox() {
    // tag::checkBox[]
    //non-nullable boolean value
    boolean initialValue = true;
    boolean nullValue = false;

    Value<Boolean> booleanValue = Values.value(initialValue, nullValue);

    JToggleButton.ToggleButtonModel buttonModel = new JToggleButton.ToggleButtonModel();

    BooleanValues.booleanButtonModelValue(buttonModel).link(booleanValue);

    JCheckBox checkBox = new JCheckBox();
    checkBox.setModel(buttonModel);
    // end::checkBox[]
  }

  static void nullableCheckBox() {
    // tag::nullableCheckBox[]
    //nullable boolean value
    Value<Boolean> booleanValue = Values.value();

    NullableToggleButtonModel buttonModel = new NullableToggleButtonModel();

    BooleanValues.booleanButtonModelValue(buttonModel).link(booleanValue);

    NullableCheckBox checkBox = new NullableCheckBox(buttonModel);
    // end::nullableCheckBox[]
  }

  static void booleanComboBox() {
    // tag::booleanComboBox[]
    Value<Boolean> booleanValue = Values.value();

    JComboBox<Item<Boolean>> comboBox = new JComboBox<>(new BooleanComboBoxModel());

    BooleanValues.booleanComboBoxValue(comboBox).link(booleanValue);
    // end::booleanComboBox[]
  }

  static void textField() {
    // tag::textField[]
    Value<String> stringValue = Values.value();

    JTextField textField = new JTextField();

    TextValues.textValue(textField).link(stringValue);
    // end::textField[]
  }

  static void textArea() {
    // tag::textArea[]
    Value<String> stringValue = Values.value();

    JTextArea textArea = new JTextArea();

    TextValues.textValue(textArea).link(stringValue);
    // end::textArea[]
  }

  static void integerField() {
    // tag::integerField[]
    Value<Integer> integerValue = Values.value();

    IntegerField integerField = new IntegerField();

    NumericalValues.integerValue(integerField).link(integerValue);
    // end::integerField[]
  }

  static void longField() {
    // tag::longField[]
    Value<Long> longValue = Values.value();

    LongField longField = new LongField();

    NumericalValues.longValue(longField).link(longValue);
    // end::longField[]
  }

  static void doubleField() {
    // tag::doubleField[]
    Value<Double> doubleValue = Values.value();

    DoubleField doubleField = new DoubleField();

    NumericalValues.doubleValue(doubleField).link(doubleValue);
    // end::doubleField[]
  }

  static void bigDecimalField() {
    // tag::bigDecimalField[]
    Value<BigDecimal> bigDecimalValue = Values.value();

    BigDecimalField bigDecimalField = new BigDecimalField();

    NumericalValues.bigDecimalValue(bigDecimalField).link(bigDecimalValue);
    // end::bigDecimalField[]
  }

  static void localTime() {
    // tag::localTime[]
    Value<LocalTime> localTimeValue = Values.value();

    JFormattedTextField textField = new JFormattedTextField();

    TemporalValues.localTimeValue(textField, "HH:mm:ss").link(localTimeValue);
    // end::localTime[]
  }

  static void localDate() {
    // tag::localDate[]
    Value<LocalDate> localDateValue = Values.value();

    JFormattedTextField textField = new JFormattedTextField();

    TemporalValues.localDateValue(textField, "dd-MM-yyyy").link(localDateValue);
    // end::localDate[]
  }

  static void localDateTime() {
    // tag::localDateTime[]
    Value<LocalDateTime> localDateTimeValue = Values.value();

    JFormattedTextField textField = new JFormattedTextField();

    TemporalValues.localDateTimeValue(textField, "dd-MM-yyyy HH:mm").link(localDateTimeValue);
    // end::localDateTime[]
  }

  static void selectionComboBox() {
    // tag::selectionComboBox[]
    Value<String> stringValue = Values.value();

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

    Value<Person> personValue = Values.value();

    PersonPanel personPanel = new PersonPanel();

    Value<Person> personPanelValue = new PersonPanelValue(personPanel);

    personPanelValue.link(personValue);
    // end::customTextFields[]
  }

  static void customTextFieldHorizontalAlignment() {
    // tag::customTextFieldHorizontalAlignment[]
    IntegerField horizontalAlignmentField = new IntegerField(5);

    Value<Integer> horizontalAlignmentValue =
            Values.propertyValue(horizontalAlignmentField, "horizontalAlignment",
                    int.class, Components.propertyChangeObserver(horizontalAlignmentField, "horizontalAlignment"));

    Value<Integer> fieldValue =
            NumericalValues.integerValue(horizontalAlignmentField, Nullable.NO);

    fieldValue.link(horizontalAlignmentValue);

    JPanel panel = new JPanel();
    panel.add(horizontalAlignmentField);

    fieldValue.addListener(panel::revalidate);

    Dialogs.displayInDialog(null, panel, "test");
    // end::customTextFieldHorizontalAlignment[]
  }
}
