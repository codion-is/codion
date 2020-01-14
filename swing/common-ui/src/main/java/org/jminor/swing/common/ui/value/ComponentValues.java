/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui.value;

import org.jminor.common.Formats;
import org.jminor.common.Item;
import org.jminor.swing.common.model.checkbox.NullableToggleButtonModel;
import org.jminor.swing.common.ui.textfield.DecimalField;
import org.jminor.swing.common.ui.textfield.IntegerField;
import org.jminor.swing.common.ui.textfield.LongField;
import org.jminor.swing.common.ui.textfield.TextInputPanel;
import org.jminor.swing.common.ui.time.TemporalInputPanel;

import javax.swing.BoundedRangeModel;
import javax.swing.ButtonModel;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.text.JTextComponent;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.Format;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.Temporal;
import java.util.List;

/**
 * A factory class for Value instances based on UI components
 */
public final class ComponentValues {

  private ComponentValues() {}

  /**
   * Instantiates a new {@link ComponentValue} for {@link Temporal} values.
   * @param inputPanel the input panel to use
   * @param <V> the temporal value type
   * @return a Value bound to the given component
   */
  public static <V extends Temporal> ComponentValue<V, TemporalInputPanel<V>> temporalValue(final TemporalInputPanel<V> inputPanel) {
    return new TemporalInputPanelValue<>(inputPanel);
  }

  /**
   * @param textComponent the component
   * @param dateFormat the date format
   * @return a Value bound to the given component
   */
  public static ComponentValue<LocalDate, JFormattedTextField> localDateValue(final JFormattedTextField textComponent,
                                                                              final String dateFormat) {
    return localDateValue(textComponent, dateFormat, true);
  }

  /**
   * @param textComponent the component
   * @param dateFormat the date format
   * @param updateOnKeystroke if true then the value is updated on each keystroke, otherwise on focus lost
   * @return a Value bound to the given component
   */
  public static ComponentValue<LocalDate, JFormattedTextField> localDateValue(final JFormattedTextField textComponent,
                                                                              final String dateFormat,
                                                                              final boolean updateOnKeystroke) {
    return new TemporalFieldValue<>(textComponent, dateFormat, updateOnKeystroke, LocalDate::parse);
  }

  /**
   * @param textComponent the component
   * @param dateFormat the date format
   * @return a Value bound to the given component
   */
  public static ComponentValue<LocalTime, JFormattedTextField> localTimeValue(final JFormattedTextField textComponent,
                                                                              final String dateFormat) {
    return localTimeValue(textComponent, dateFormat, true);
  }

  /**
   * @param textComponent the component
   * @param dateFormat the date format
   * @param updateOnKeystroke if true then the value is updated on each keystroke, otherwise on focus lost
   * @return a Value bound to the given component
   */
  public static ComponentValue<LocalTime, JFormattedTextField> localTimeValue(final JFormattedTextField textComponent,
                                                                              final String dateFormat,
                                                                              final boolean updateOnKeystroke) {
    return new TemporalFieldValue<>(textComponent, dateFormat, updateOnKeystroke, LocalTime::parse);
  }

  /**
   * @param textComponent the component
   * @param dateFormat the date format
   * @return a Value bound to the given component
   */
  public static ComponentValue<LocalDateTime, JFormattedTextField> localDateTimeValue(final JFormattedTextField textComponent,
                                                                                      final String dateFormat) {
    return localDateTimeValue(textComponent, dateFormat, true);
  }

  /**
   * @param textComponent the component
   * @param dateFormat the date format
   * @param updateOnKeystroke if true then the value is updated on each keystroke, otherwise on focus lost
   * @return a Value bound to the given component
   */
  public static ComponentValue<LocalDateTime, JFormattedTextField> localDateTimeValue(final JFormattedTextField textComponent,
                                                                                      final String dateFormat,
                                                                                      final boolean updateOnKeystroke) {
    return new TemporalFieldValue<>(textComponent, dateFormat, updateOnKeystroke, LocalDateTime::parse);
  }

  /**
   * Instantiates a new Integer based ComponentValue.
   * @param initialValue the initial value
   * @return a Integer based ComponentValue
   */
  public static ComponentValue<Integer, IntegerField> integerValue(final Integer initialValue) {
    return integerValue(initialValue, NumberFormat.getIntegerInstance());
  }

  /**
   * Instantiates a new Integer based ComponentValue.
   * @param initialValue the initial value
   * @param format the number format to use
   * @return a Integer based ComponentValue
   */
  public static ComponentValue<Integer, IntegerField> integerValue(final Integer initialValue, final NumberFormat format) {
    final IntegerField integerField = new IntegerField(format);
    integerField.setInteger(initialValue);

    return integerValue(integerField, true);
  }

  /**
   * @param integerField the component
   * @param nullable if false then the resulting Value returns 0 instead of null
   * @return a Value bound to the given component
   */
  public static ComponentValue<Integer, IntegerField> integerValue(final IntegerField integerField, final boolean nullable) {
    return integerValue(integerField, nullable, true);
  }

  /**
   * @param integerField the component
   * @param nullable if false then the resulting Value returns 0 instead of null
   * @param updateOnKeystroke if true then the value is updated on each keystroke, otherwise on focus lost
   * @return a Value bound to the given component
   */
  public static ComponentValue<Integer, IntegerField> integerValue(final IntegerField integerField, final boolean nullable,
                                                                   final boolean updateOnKeystroke) {
    return new IntegerFieldValue(integerField, nullable, updateOnKeystroke);
  }

  /**
   * @param spinnerModel the spinner model
   * @return a Value bound to the given model
   */
  public static ComponentValue<Integer, SpinnerNumberModel> integerValue(final SpinnerNumberModel spinnerModel) {
    return new SpinnerNumberValue(spinnerModel);
  }

  /**
   * @param boundedRangeModel the bounded range model
   * @return a Value bound to the given model
   */
  public static ComponentValue<Integer, BoundedRangeModel> integerValue(final BoundedRangeModel boundedRangeModel) {
    return new IntegerBoundedRangeModelValue(boundedRangeModel);
  }

  /**
   * @param spinnerModel the spinner model
   * @return a Value bound to the given component
   */
  public static ComponentValue<Double, SpinnerNumberModel> doubleValue(final SpinnerNumberModel spinnerModel) {
    return new SpinnerNumberValue(spinnerModel);
  }

  /**
   * Instantiates a new Double based ComponentValue.
   * @param initialValue the initial value
   * @return a Double based ComponentValue
   */
  public static ComponentValue<Double, DecimalField> doubleValue(final Double initialValue) {
    return doubleValue(initialValue, new DecimalFormat());
  }

  /**
   * Instantiates a new Double based ComponentValue.
   * @param initialValue the initial value
   * @param format the number format to use
   * @return a Double based ComponentValue
   */
  public static ComponentValue<Double, DecimalField> doubleValue(final Double initialValue,
                                                                 final DecimalFormat format) {
    final DecimalField decimalField = new DecimalField(format);
    decimalField.setDouble(initialValue);

    return doubleValue(decimalField, true);
  }

  /**
   * @param decimalField the component
   * @param nullable if false then the resulting Value returns 0 instead of null
   * @return a Value bound to the given component
   */
  public static ComponentValue<Double, DecimalField> doubleValue(final DecimalField decimalField, final boolean nullable) {
    return doubleValue(decimalField, nullable, true);
  }

  /**
   * @param decimalField the component
   * @param nullable if false then the resulting Value returns 0 instead of null
   * @param updateOnKeystroke if true then the value is updated on each keystroke, otherwise on focus lost
   * @return a Value bound to the given component
   */
  public static ComponentValue<Double, DecimalField> doubleValue(final DecimalField decimalField, final boolean nullable,
                                                                 final boolean updateOnKeystroke) {
    return new DecimalFieldValue(decimalField, nullable, updateOnKeystroke);
  }

  /**
   * @param initialValue the initial value
   * @return a BigDecimal based ComponentValue
   */
  public static ComponentValue<BigDecimal, DecimalField> bigDecimalValue(final BigDecimal initialValue) {
    return bigDecimalValue(initialValue, Formats.getBigDecimalNumberFormat());
  }

  /**
   * @param initialValue the initial value
   * @param format the number format to use
   * @return a BigDecimal based ComponentValue
   */
  public static ComponentValue<BigDecimal, DecimalField> bigDecimalValue(final BigDecimal initialValue,
                                                                         final DecimalFormat format) {
    final DecimalField decimalField = new DecimalField(format);
    decimalField.setBigDecimal(initialValue);

    return bigDecimalValue(decimalField);
  }

  /**
   * @param decimalField the component
   * @return a Value bound to the given component
   */
  public static ComponentValue<BigDecimal, DecimalField> bigDecimalValue(final DecimalField decimalField) {
    return bigDecimalValue(decimalField, true);
  }

  /**
   * @param decimalField the component
   * @param updateOnKeystroke if true then the value is updated on each keystroke, otherwise on focus lost
   * @return a Value bound to the given component
   */
  public static ComponentValue<BigDecimal, DecimalField> bigDecimalValue(final DecimalField decimalField,
                                                                         final boolean updateOnKeystroke) {
    return new BigDecimalFieldValue(decimalField, updateOnKeystroke);
  }

  /**
   * Instantiates a new Long based ComponentValue.
   * @param initialValue the initial value
   * @return a Long based ComponentValue
   */
  public static ComponentValue<Long, LongField> longValue(final Long initialValue) {
    return longValue(initialValue, NumberFormat.getIntegerInstance());
  }

  /**
   * Instantiates a new Long based ComponentValue.
   * @param initialValue the initial value
   * @param format the number format to use
   * @return a Long based ComponentValue
   */
  public static ComponentValue<Long, LongField> longValue(final Long initialValue, final NumberFormat format) {
    final LongField longField = new LongField(format);
    longField.setLong(initialValue);

    return longValue(longField, true);
  }

  /**
   * @param longField the component
   * @param nullable if false then the resulting Value returns 0 instead of null
   * @return a Value bound to the given component
   */
  public static ComponentValue<Long, LongField> longValue(final LongField longField, final boolean nullable) {
    return longValue(longField, nullable, true);
  }

  /**
   * @param longField the component
   * @param nullable if false then the resulting Value returns 0 instead of null
   * @param updateOnKeystroke if true then the value is updated on each keystroke, otherwise on focus lost
   * @return a Value bound to the given component
   */
  public static ComponentValue<Long, LongField> longValue(final LongField longField, final boolean nullable,
                                                          final boolean updateOnKeystroke) {
    return new LongFieldValue(longField, nullable, updateOnKeystroke);
  }

  /**
   * @param textComponent the component
   * @return a Value bound to the given component
   */
  public static ComponentValue<String, JTextComponent> textValue(final JTextComponent textComponent) {
    return textValue(textComponent, null);
  }

  /**
   * @param textComponent the component
   * @param format the format
   * @return a Value bound to the given component
   */
  public static ComponentValue<String, JTextComponent> textValue(final JTextComponent textComponent, final Format format) {
    return textValue(textComponent, format, true);
  }

  /**
   * @param textComponent the component
   * @param format the format
   * @param updateOnKeystroke if true then the value is updated on each keystroke, otherwise on focus lost
   * @return a Value bound to the given component
   */
  public static ComponentValue<String, JTextComponent> textValue(final JTextComponent textComponent, final Format format,
                                                                 final boolean updateOnKeystroke) {
    return new TextComponentValue<>(textComponent, format, updateOnKeystroke);
  }

  /**
   * Instantiates a new String based ComponentValue.
   * @param inputDialogTitle the title to use for the lookup input dialog
   * @param initialValue the initial value
   * @param maxLength the maximum input length, -1 for no limit
   * @return a String based ComponentValue
   */
  public static ComponentValue<String, TextInputPanel> textValue(final String inputDialogTitle, final String initialValue,
                                                                 final int maxLength) {
    return new TextInputPanelValue(inputDialogTitle, initialValue, maxLength);
  }

  /**
   * Creates a boolean value based on the given button model.
   * If the button model is a {@link NullableToggleButtonModel} the value will be nullable otherwise not
   * @param buttonModel the button model
   * @return a Value bound to the given button model
   */
  public static ComponentValue<Boolean, ? extends ButtonModel> booleanButtonModelValue(final ButtonModel buttonModel) {
    if (buttonModel instanceof NullableToggleButtonModel) {
      return new BooleanNullableButtonModelValue((NullableToggleButtonModel) buttonModel);
    }

    return new BooleanButtonModelValue(buttonModel);
  }

  /**
   * Instantiates a new Boolean based ComponentValue.
   * @param initialValue the initial value
   * @return a Boolean based ComponentValue
   */
  public static ComponentValue<Boolean, JComboBox> booleanComboBoxValue(final Boolean initialValue) {
    return new BooleanComboBoxValue(initialValue);
  }

  /**
   * @return a blob based ComponentValue
   */
  public static ComponentValue<byte[], FileInputPanelValue.FileInputPanel> blobValue() {
    return new FileInputPanelValue();
  }

  /**
   * Instantiates a Item based ComponentValue.
   * @param initialValue the initial value
   * @param values the available values
   * @param <V> the value type
   * @return a ComponentValue based on a combo box
   */
  public static <V> ComponentValue<V, JComboBox<Item<V>>> selectedItemValue(final V initialValue, final List<Item<V>> values) {
    return new SelectedItemValue<>(initialValue, values);
  }

  /**
   * @param <V> the value type
   * @param comboBox the combo box
   * @return a Value bound to the given component
   */
  public static <V> ComponentValue<V, JComboBox<V>> selectedValue(final JComboBox<V> comboBox) {
    return new SelectedValue<>(comboBox);
  }
}
