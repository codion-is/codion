/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui;

import org.jminor.common.EventObserver;
import org.jminor.common.Value;
import org.jminor.common.Values;
import org.jminor.swing.common.ui.textfield.DecimalField;
import org.jminor.swing.common.ui.textfield.IntegerField;
import org.jminor.swing.common.ui.textfield.LongField;

import javax.swing.ButtonModel;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JToggleButton;
import javax.swing.SpinnerNumberModel;
import javax.swing.text.JTextComponent;
import java.math.BigDecimal;
import java.text.Format;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * A factory class for binding values to UI components
 */
public final class ValueLinks {

  private ValueLinks() {}

  /**
   * Links a date value with a given text component
   * @param textComponent the text component to link with the value
   * @param value the model value
   * @param dateFormat the data format
   * @param updateTrigger when the component should update the value
   */
  public static void localDateValueLink(final JFormattedTextField textComponent, final Value<LocalDate> value,
                                        final String dateFormat, final UpdateTrigger updateTrigger) {
    textComponent.setEditable(updateTrigger != UpdateTrigger.READ_ONLY);
    Values.link(value, UiValues.localDateValue(textComponent, dateFormat, updateTrigger), updateTrigger == UpdateTrigger.READ_ONLY);
  }

  /**
   * Links a date value with a given text component
   * @param textComponent the text component to link with the value
   * @param value the model value
   * @param dateFormat the data format
   * @param updateTrigger when the component should update the value
   */
  public static void localTimeValueLink(final JFormattedTextField textComponent, final Value<LocalTime> value,
                                        final String dateFormat, final UpdateTrigger updateTrigger) {
    textComponent.setEditable(updateTrigger != UpdateTrigger.READ_ONLY);
    Values.link(value, UiValues.localTimeValue(textComponent, dateFormat, updateTrigger), updateTrigger == UpdateTrigger.READ_ONLY);
  }

  /**
   * Links a date value with a given text component
   * @param textComponent the text component to link with the value
   * @param value the model value
   * @param dateFormat the data format
   * @param updateTrigger when the component should update the value
   */
  public static void localDateTimeValueLink(final JFormattedTextField textComponent, final Value<LocalDateTime> value,
                                            final String dateFormat, final UpdateTrigger updateTrigger) {
    textComponent.setEditable(updateTrigger != UpdateTrigger.READ_ONLY);
    Values.link(value, UiValues.localDateTimeValue(textComponent, dateFormat, updateTrigger), updateTrigger == UpdateTrigger.READ_ONLY);
  }

  /**
   * @param integerField the int field to link with the value
   * @param value the model value
   * @param nullable if false then 0 is used instead of null
   * @param updateTrigger when the component should update the value
   */
  public static void integerValueLink(final IntegerField integerField, final Value<Integer> value, final boolean nullable,
                                      final UpdateTrigger updateTrigger) {
    integerField.setEditable(updateTrigger != UpdateTrigger.READ_ONLY);
    Values.link(value, UiValues.integerValue(integerField, nullable, updateTrigger), updateTrigger == UpdateTrigger.READ_ONLY);
  }

  /**
   * @param decimalField the decimal field to link with the value
   * @param value the model value
   * @param nullable if false then 0 is used instead of null
   * @param updateTrigger when the component should update the value
   */
  public static void doubleValueLink(final DecimalField decimalField, final Value<Double> value, final boolean nullable,
                                     final UpdateTrigger updateTrigger) {
    decimalField.setEditable(updateTrigger != UpdateTrigger.READ_ONLY);
    Values.link(value, UiValues.doubleValue(decimalField, nullable, updateTrigger), updateTrigger == UpdateTrigger.READ_ONLY);
  }

  /**
   * @param decimalField the decimal field to link with the value
   * @param value the model value
   * @param updateTrigger when the component should update the value
   */
  public static void bigDecimalValueLink(final DecimalField decimalField, final Value<BigDecimal> value,
                                         final UpdateTrigger updateTrigger) {
    decimalField.setEditable(updateTrigger != UpdateTrigger.READ_ONLY);
    Values.link(value, UiValues.bigDecimalValue(decimalField, updateTrigger), updateTrigger == UpdateTrigger.READ_ONLY);
  }

  /**
   * @param longField the long field to link with the value
   * @param value the model value
   * @param nullable if false then 0 is used instead of null
   * @param updateTrigger when the component should update the value
   */
  public static void longValueLink(final LongField longField, final Value<Long> value, final boolean nullable,
                                   final UpdateTrigger updateTrigger) {
    longField.setEditable(updateTrigger != UpdateTrigger.READ_ONLY);
    Values.link(value, UiValues.longValue(longField, nullable, updateTrigger), updateTrigger == UpdateTrigger.READ_ONLY);
  }

  /**
   * @param textComponent the text component to link with the value
   * @param value the value to link with the component
   */
  public static void textValueLink(final JTextComponent textComponent, final Value<String> value) {
    textValueLink(textComponent, value, null, UpdateTrigger.KEYSTROKE);
  }

  /**
   * @param textComponent the text component to link with the value
   * @param value the value to link with the component
   * @param format the format to use when displaying the linked value, null if no formatting should be performed
   * @param updateTrigger when the component should update the value
   */
  public static void textValueLink(final JTextComponent textComponent, final Value<String> value, final Format format,
                                   final UpdateTrigger updateTrigger) {
    textComponent.setEditable(updateTrigger != UpdateTrigger.READ_ONLY);
    Values.link(value, UiValues.textValue(textComponent, format, updateTrigger), updateTrigger == UpdateTrigger.READ_ONLY);
  }

  /**
   * @param owner the value owner
   * @param propertyName the property name
   * @param valueChangeEvent an EventObserver notified each time the value changes
   * @return a ButtomModel based on the value
   */
  public static ButtonModel toggleValueLink(final Object owner, final String propertyName, final EventObserver<Boolean> valueChangeEvent) {
    return toggleValueLink(owner, propertyName, valueChangeEvent, false);
  }

  /**
   * @param owner the value owner
   * @param propertyName the property name
   * @param valueChangeEvent an EventObserver notified each time the value changes
   * @param readOnly if true the component will be read only
   * @return a ButtomModel based on the value
   */
  public static ButtonModel toggleValueLink(final Object owner, final String propertyName, final EventObserver<Boolean> valueChangeEvent,
                                            final boolean readOnly) {
    final ButtonModel buttonModel = new JToggleButton.ToggleButtonModel();
    toggleValueLink(buttonModel, owner, propertyName, valueChangeEvent, readOnly);

    return buttonModel;
  }

  /**
   * @param buttonModel the button model to link with the value
   * @param owner the value owner
   * @param propertyName the property name
   * @param valueChangeEvent an EventObserver notified each time the value changes
   */
  public static void toggleValueLink(final ButtonModel buttonModel, final Object owner, final String propertyName,
                                     final EventObserver<Boolean> valueChangeEvent) {
    toggleValueLink(buttonModel, owner, propertyName, valueChangeEvent, false);
  }

  /**
   * @param buttonModel the button model to link with the value
   * @param owner the value owner
   * @param propertyName the property name
   * @param valueChangeEvent an EventObserver notified each time the value changes
   * @param readOnly if true the component will be read only
   */
  public static void toggleValueLink(final ButtonModel buttonModel, final Object owner, final String propertyName,
                                     final EventObserver<Boolean> valueChangeEvent, final boolean readOnly) {
    toggleValueLink(buttonModel, Values.propertyValue(owner, propertyName, boolean.class, valueChangeEvent), readOnly);
  }

  /**
   * @param buttonModel the button model to link with the value
   * @param value the model value
   * @param readOnly if true the component will be read only
   */
  public static void toggleValueLink(final ButtonModel buttonModel, final Value<Boolean> value, final boolean readOnly) {
    Values.link(value, UiValues.booleanValue(buttonModel), readOnly);
  }

  /**
   * @param <V> the value type
   * @param comboBox the combo box to link with the value
   * @param owner the value owner
   * @param propertyName the property name
   * @param valueClass the value class
   * @param valueChangeEvent an EventObserver notified each time the value changes
   */
  public static <V> void selectedItemValueLink(final JComboBox<V> comboBox, final Object owner, final String propertyName,
                                               final Class<V> valueClass, final EventObserver<V> valueChangeEvent) {
    selectedItemValueLink(comboBox, owner, propertyName, valueClass, valueChangeEvent, false);
  }

  /**
   * @param <V> the value type
   * @param comboBox the combo box to link with the value
   * @param owner the value owner
   * @param propertyName the property name
   * @param valueClass the value class
   * @param valueChangeEvent an EventObserver notified each time the value changes
   * @param readOnly if true the component will be read only
   */
  public static <V> void selectedItemValueLink(final JComboBox<V> comboBox, final Object owner, final String propertyName,
                                               final Class<V> valueClass, final EventObserver<V> valueChangeEvent,
                                               final boolean readOnly) {
    selectedItemValueLink(comboBox, Values.propertyValue(owner, propertyName, valueClass, valueChangeEvent), readOnly);
  }

  /**
   * @param <V> the value type
   * @param comboBox the combo box to link with the value
   * @param value the model value
   */
  public static <V> void selectedItemValueLink(final JComboBox<V> comboBox, final Value<V> value) {
    Values.link(value, UiValues.selectedItemValue(comboBox), false);
  }

  /**
   * @param <V> the value type
   * @param comboBox the combo box to link with the value
   * @param value the model value
   * @param readOnly if true the component will be read only
   */
  public static <V> void selectedItemValueLink(final JComboBox<V> comboBox, final Value<V> value, final boolean readOnly) {
    Values.link(value, UiValues.selectedItemValue(comboBox), readOnly);
  }

  /**
   * @param owner the value owner
   * @param propertyName the property name
   * @param valueChangeEvent an EventObserver notified each time the value changes
   * @return a SpinnerNumberModel based on the value
   */
  public static SpinnerNumberModel intSpinnerValueLink(final Object owner, final String propertyName,
                                                       final EventObserver<Integer> valueChangeEvent) {
    return intSpinnerValueLink(owner, propertyName, valueChangeEvent, false);
  }

  /**
   * @param owner the value owner
   * @param propertyName the property name
   * @param valueChangeEvent an EventObserver notified each time the value changes
   * @param readOnly if true the value link will be read only
   * @return a SpinnerNumberModel based on the value
   */
  public static SpinnerNumberModel intSpinnerValueLink(final Object owner, final String propertyName,
                                                       final EventObserver<Integer> valueChangeEvent, final boolean readOnly) {
    final SpinnerNumberModel numberModel = new SpinnerNumberModel();
    intSpinnerValueLink(owner, propertyName, valueChangeEvent, numberModel, readOnly);

    return numberModel;
  }

  /**
   * @param integerValue the value
   * @return a SpinnerNumberModel based on the value
   */
  public static SpinnerNumberModel intSpinnerValueLink(final Value<Integer> integerValue) {
    return intSpinnerValueLink(integerValue, false);
  }

  /**
   * @param integerValue the value
   * @param readOnly if true the value link will be read only
   * @return a SpinnerNumberModel based on the value
   */
  public static SpinnerNumberModel intSpinnerValueLink(final Value<Integer> integerValue, final boolean readOnly) {
    final SpinnerNumberModel numberModel = new SpinnerNumberModel();
    intSpinnerValueLink(numberModel, integerValue, readOnly);

    return numberModel;
  }

  /**
   * @param owner the value owner
   * @param propertyName the property name
   * @param valueChangeEvent an EventObserver notified each time the value changes
   * @param spinnerModel the spinner model to use
   * @param readOnly if true the value link will be read only
   */
  public static void intSpinnerValueLink(final Object owner, final String propertyName, final EventObserver<Integer> valueChangeEvent,
                                         final SpinnerNumberModel spinnerModel, final boolean readOnly) {
    intSpinnerValueLink(spinnerModel, Values.propertyValue(owner, propertyName, int.class, valueChangeEvent), readOnly);
  }

  /**
   * @param spinnerModel the spinner model
   * @param integerValue the value
   */
  public static void intSpinnerValueLink(final SpinnerNumberModel spinnerModel, final Value<Integer> integerValue) {
    intSpinnerValueLink(spinnerModel, integerValue, false);
  }

  /**
   * @param spinnerModel the spinner model
   * @param integerValue the value
   * @param readOnly if true the value link will be read only
   */
  public static void intSpinnerValueLink(final SpinnerNumberModel spinnerModel, final Value<Integer> integerValue, final boolean readOnly) {
    Values.link(integerValue, UiValues.integerValue(spinnerModel), readOnly);
  }
}
