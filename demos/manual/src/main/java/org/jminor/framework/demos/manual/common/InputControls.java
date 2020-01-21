/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.manual.common;

import org.jminor.common.Item;
import org.jminor.common.value.Value;
import org.jminor.common.value.Values;
import org.jminor.swing.common.model.checkbox.NullableToggleButtonModel;
import org.jminor.swing.common.model.combobox.BooleanComboBoxModel;
import org.jminor.swing.common.ui.checkbox.NullableCheckBox;
import org.jminor.swing.common.ui.textfield.DecimalField;
import org.jminor.swing.common.ui.textfield.IntegerField;
import org.jminor.swing.common.ui.textfield.LongField;
import org.jminor.swing.common.ui.value.BooleanValues;
import org.jminor.swing.common.ui.value.NumericalValues;
import org.jminor.swing.common.ui.value.SelectedValues;
import org.jminor.swing.common.ui.value.TemporalValues;
import org.jminor.swing.common.ui.value.TextValues;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
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

    JToggleButton.ToggleButtonModel buttonModel =
            new JToggleButton.ToggleButtonModel();

    BooleanValues.booleanValueLink(buttonModel, booleanValue);

    JCheckBox checkBox = new JCheckBox();
    checkBox.setModel(buttonModel);
    // end::checkBox[]
  }

  static void nullableCheckBox() {
    // tag::nullableCheckBox[]
    //nullable boolean value
    Value<Boolean> booleanValue = Values.value();

    NullableToggleButtonModel buttonModel =
            new NullableToggleButtonModel();

    BooleanValues.booleanValueLink(buttonModel, booleanValue);

    NullableCheckBox checkBox = new NullableCheckBox(buttonModel);
    // end::nullableCheckBox[]
  }

  static void booleanComboBox() {
    // tag::booleanComboBox[]
    Value<Boolean> booleanValue = Values.value();

    JComboBox<Item<Boolean>> comboBox = new JComboBox<>(new BooleanComboBoxModel());

    BooleanValues.booleanValueLink(comboBox, booleanValue);
    // end::booleanComboBox[]
  }

  static void textField() {
    // tag::textField[]
    Value<String> stringValue = Values.value();

    JTextField textField = new JTextField();

    TextValues.textValueLink(textField, stringValue);
    // end::textField[]
  }

  static void textArea() {
    // tag::textArea[]
    Value<String> stringValue = Values.value();

    JTextArea textArea = new JTextArea();

    TextValues.textValueLink(textArea, stringValue);
    // end::textArea[]
  }

  static void integerField() {
    // tag::integerField[]
    Value<Integer> integerValue = Values.value();

    IntegerField integerField = new IntegerField();

    NumericalValues.integerValueLink(integerField, integerValue);
    // end::integerField[]
  }

  static void longField() {
    // tag::longField[]
    Value<Long> longValue = Values.value();

    LongField longField = new LongField();

    NumericalValues.longValueLink(longField, longValue);
    // end::longField[]
  }

  static void doubleField() {
    // tag::doubleField[]
    Value<Double> doubleValue = Values.value();

    DecimalField doubleField = new DecimalField();

    NumericalValues.doubleValueLink(doubleField, doubleValue);
    // end::doubleField[]
  }

  static void bigDecimalField() {
    // tag::bigDecimalField[]
    Value<BigDecimal> bigDecimalValue = Values.value();

    DecimalField bigDecimalField = new DecimalField();

    NumericalValues.bigDecimalValueLink(bigDecimalField, bigDecimalValue);
    // end::bigDecimalField[]
  }

  static void localTime() {
    // tag::localTime[]
    Value<LocalTime> localTimeValue = Values.value();

    JFormattedTextField textField = new JFormattedTextField();

    TemporalValues.localTimeValueLink(textField, localTimeValue, "HH:mm:ss");
    // end::localTime[]
  }

  static void localDate() {
    // tag::localDate[]
    Value<LocalDate> localDateValue = Values.value();

    JFormattedTextField textField = new JFormattedTextField();

    TemporalValues.localDateValueLink(textField, localDateValue, "dd-MM-yyyy");
    // end::localDate[]
  }

  static void localDateTime() {
    // tag::localDateTime[]
    Value<LocalDateTime> localDateTimeValue = Values.value();

    JFormattedTextField textField = new JFormattedTextField();

    TemporalValues.localDateTimeValueLink(textField, localDateTimeValue, "dd-MM-yyyy HH:mm");
    // end::localDateTime[]
  }

  static void selectionComboBox() {
    // tag::selectionComboBox[]
    Value<String> stringValue = Values.value();

    JComboBox<String> comboBox = new JComboBox<>(new String[] {"one", "two", "three"});

    SelectedValues.selectedValueLink(comboBox, stringValue);
    // end::selectionComboBox[]
  }
}
