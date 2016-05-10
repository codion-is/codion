/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui;

import org.jminor.common.EventObserver;
import org.jminor.common.model.Value;
import org.jminor.common.model.Values;
import org.jminor.swing.common.ui.textfield.DoubleField;
import org.jminor.swing.common.ui.textfield.IntField;
import org.jminor.swing.common.ui.textfield.LongField;

import javax.swing.ButtonModel;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JToggleButton;
import javax.swing.SpinnerNumberModel;
import javax.swing.text.JTextComponent;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.text.DateFormat;
import java.text.Format;
import java.util.Date;

/**
 * A factory class for binding values to UI components
 */
public final class ValueLinks {

  private ValueLinks() {}

  /**
   * Links a date bean value with a given text component
   * @param textComponent the text component to link with the value
   * @param owner the value owner
   * @param beanPropertyName the property name
   * @param valueChangeEvent an EventObserver notified each time the value changes
   * @param readOnly if true the component will be read only
   * @param dateFormat the data format
   * @param sqlType the actual sql type (Types.DATE, Types.TIMESTAMP or Types.TIME)
   * @param immediateUpdate if true then the underlying model value is updated on each keystroke
   */
  @SuppressWarnings("unchecked")
  public static void dateValueLink(final JFormattedTextField textComponent, final Object owner,
                                   final String beanPropertyName, final EventObserver<Date> valueChangeEvent,
                                   final boolean readOnly, final DateFormat dateFormat, final int sqlType,
                                   final boolean immediateUpdate) {
    dateValueLink(textComponent, Values.beanValue(owner, beanPropertyName, getDateTypeClass(sqlType),
            valueChangeEvent), readOnly, dateFormat, sqlType, immediateUpdate);
  }

  /**
   * Links a date value with a given text component
   * @param textComponent the text component to link with the value
   * @param value the model value
   * @param readOnly if true the component will be read only
   * @param dateFormat the data format
   * @param sqlType the actual sql type (Types.DATE, Types.TIMESTAMP or Types.TIME)
   * @param immediateUpdate if true then the underlying model value is updated on each keystroke
   */
  public static void dateValueLink(final JFormattedTextField textComponent, final Value<Date> value,
                                   final boolean readOnly, final DateFormat dateFormat, final int sqlType,
                                   final boolean immediateUpdate) {
    textComponent.setEditable(!readOnly);
    Values.link(value, UiValues.dateValue(textComponent, dateFormat, sqlType, immediateUpdate), readOnly);
  }

  /**
   * @param intField the int field to link with the value
   * @param owner the value owner
   * @param beanPropertyName the property name
   * @param valueChangeEvent an EventObserver notified each time the value changes
   * @param usePrimitive if true then the property is assumed to be a primitive, int instead of Integer
   * @param immediateUpdate if true then the underlying model value is updated on each keystroke
   */
  public static void intValueLink(final IntField intField, final Object owner, final String beanPropertyName,
                                  final EventObserver<Integer> valueChangeEvent, final boolean usePrimitive,
                                  final boolean immediateUpdate) {
    intValueLink(intField, Values.beanValue(owner, beanPropertyName, usePrimitive ? int.class : Integer.class,
            valueChangeEvent), usePrimitive, false, immediateUpdate);
  }

  /**
   * @param intField the int field to link with the value
   * @param owner the value owner
   * @param beanPropertyName the property name
   * @param valueChangeEvent an EventObserver notified each time the value changes
   * @param usePrimitive if true then the property is assumed to be a primitive, int instead of Integer
   * @param readOnly if true the component will be read only
   * @param immediateUpdate if true then the underlying model value is updated on each keystroke
   */
  public static void intValueLink(final IntField intField, final Object owner, final String beanPropertyName,
                                  final EventObserver<Integer> valueChangeEvent, final boolean usePrimitive,
                                  final boolean readOnly, final boolean immediateUpdate) {
    intValueLink(intField, Values.beanValue(owner, beanPropertyName, usePrimitive ? int.class : Integer.class,
            valueChangeEvent), usePrimitive, readOnly, immediateUpdate);
  }

  /**
   * @param intField the int field to link with the value
   * @param value the model value
   * @param usePrimitive if true then the property is assumed to be a primitive, int instead of Integer
   * @param readOnly if true the component will be read only
   * @param immediateUpdate if true then the underlying model value is updated on each keystroke
   */
  public static void intValueLink(final IntField intField, final Value<Integer> value, final boolean usePrimitive,
                                  final boolean readOnly, final boolean immediateUpdate) {
    intField.setEditable(!readOnly);
    Values.link(value, UiValues.integerValue(intField, usePrimitive, immediateUpdate), readOnly);
  }

  /**
   * @param doubleField the double field to link with the value
   * @param owner the value owner
   * @param beanPropertyName the property name
   * @param valueChangeEvent an EventObserver notified each time the value changes
   * @param usePrimitive if true then the property is assumed to be a primitive, double instead of Double
   * @param immediateUpdate if true then the underlying model value is updated on each keystroke
   */
  public static void doubleValueLink(final DoubleField doubleField, final Object owner, final String beanPropertyName,
                                     final EventObserver<Double> valueChangeEvent, final boolean usePrimitive,
                                     final boolean immediateUpdate) {
    doubleValueLink(doubleField, owner, beanPropertyName, valueChangeEvent, usePrimitive, false, immediateUpdate);
  }

  /**
   * @param doubleField the double field to link with the value
   * @param owner the value owner
   * @param beanPropertyName the property name
   * @param valueChangeEvent an EventObserver notified each time the value changes
   * @param usePrimitive if true then the property is assumed to be a primitive, double instead of Double
   * @param readOnly if true the component will be read only
   * @param immediateUpdate if true then the underlying model value is updated on each keystroke
   */
  public static void doubleValueLink(final DoubleField doubleField, final Object owner, final String beanPropertyName,
                                     final EventObserver<Double> valueChangeEvent, final boolean usePrimitive,
                                     final boolean readOnly, final boolean immediateUpdate) {
    doubleValueLink(doubleField, Values.beanValue(owner, beanPropertyName, usePrimitive ? double.class : Double.class,
            valueChangeEvent), usePrimitive, readOnly, immediateUpdate);
  }

  /**
   * @param doubleField the double field to link with the value
   * @param value the model value
   * @param usePrimitive if true then the property is assumed to be a primitive, double instead of Double
   * @param readOnly if true the component will be read only
   * @param immediateUpdate if true then the underlying model value is updated on each keystroke
   */
  public static void doubleValueLink(final DoubleField doubleField, final Value<Double> value, final boolean usePrimitive,
                                     final boolean readOnly, final boolean immediateUpdate) {
    doubleField.setEditable(!readOnly);
    Values.link(value, UiValues.doubleValue(doubleField, usePrimitive, immediateUpdate), readOnly);
  }

  /**
   * @param longField the long field to link with the value
   * @param owner the value owner
   * @param beanPropertyName the property name
   * @param valueChangeEvent an EventObserver notified each time the value changes
   * @param usePrimitive if true then the property is assumed to be a primitive, double instead of Double
   * @param immediateUpdate if true then the underlying model value is updated on each keystroke
   */
  public static void longValueLink(final LongField longField, final Object owner, final String beanPropertyName,
                                   final EventObserver<Long> valueChangeEvent, final boolean usePrimitive,
                                   final boolean immediateUpdate) {
    longValueLink(longField, owner, beanPropertyName, valueChangeEvent, usePrimitive, false, immediateUpdate);
  }

  /**
   * @param longField the long field to link with the value
   * @param owner the value owner
   * @param beanPropertyName the property name
   * @param valueChangeEvent an EventObserver notified each time the value changes
   * @param usePrimitive if true then the property is assumed to be a primitive, double instead of Double
   * @param readOnly if true the component will be read only
   * @param immediateUpdate if true then the underlying model value is updated on each keystroke
   */
  public static void longValueLink(final LongField longField, final Object owner, final String beanPropertyName,
                                   final EventObserver<Long> valueChangeEvent, final boolean usePrimitive,
                                   final boolean readOnly, final boolean immediateUpdate) {
    longValueLink(longField, Values.beanValue(owner, beanPropertyName, usePrimitive ? long.class : Long.class,
            valueChangeEvent), usePrimitive, readOnly, immediateUpdate);
  }

  /**
   * @param longField the long field to link with the value
   * @param value the model value
   * @param usePrimitive if true then the property is assumed to be a primitive, double instead of Double
   * @param readOnly if true the component will be read only
   * @param immediateUpdate if true then the underlying model value is updated on each keystroke
   */
  public static void longValueLink(final LongField longField, final Value<Long> value, final boolean usePrimitive,
                                   final boolean readOnly, final boolean immediateUpdate) {
    longField.setEditable(!readOnly);
    Values.link(value, UiValues.longValue(longField, usePrimitive, immediateUpdate), readOnly);
  }

  /**
   * @param textComponent the text component to link with the value
   * @param owner the value owner
   * @param beanPropertyName the property name
   * @param valueChangeEvent an EventObserver notified each time the value changes
   */
  public static void textValueLink(final JTextComponent textComponent, final Object owner, final String beanPropertyName,
                                   final EventObserver<String> valueChangeEvent) {
    textValueLink(textComponent, owner, beanPropertyName, valueChangeEvent, false);
  }

  /**
   * @param textComponent the text component to link with the value
   * @param owner the value owner
   * @param beanPropertyName the property name
   * @param valueChangeEvent an EventObserver notified each time the value changes
   * @param readOnly if true the component will be read only
   */
  public static void textValueLink(final JTextComponent textComponent, final Object owner, final String beanPropertyName,
                                   final EventObserver<String> valueChangeEvent, final boolean readOnly) {
    textValueLink(textComponent, owner, beanPropertyName, valueChangeEvent, null, readOnly);
  }

  /**
   * @param textComponent the text component to link with the value
   * @param owner the value owner
   * @param beanPropertyName the property name
   * @param valueChangeEvent an EventObserver notified each time the value changes
   * @param format the format to use when displaying the linked value
   * @param readOnly if true the component will be read only
   */
  public static void textValueLink(final JTextComponent textComponent, final Object owner, final String beanPropertyName,
                                   final EventObserver<String> valueChangeEvent, final Format format,
                                   final boolean readOnly) {
    textValueLink(textComponent, owner, beanPropertyName, valueChangeEvent, format, true, readOnly);
  }

  /**
   * @param textComponent the text component to link with the value
   * @param owner the value owner
   * @param beanPropertyName the property name
   * @param valueChangeEvent an EventObserver notified each time the value changes
   * @param format the format to use when displaying the linked value, null if no formatting should be performed
   * @param immediateUpdate if true then the underlying model value is updated on each keystroke
   * @param readOnly if true the component will be read only
   */
  public static void textValueLink(final JTextComponent textComponent, final Object owner, final String beanPropertyName,
                                   final EventObserver<String> valueChangeEvent, final Format format,
                                   final boolean immediateUpdate, final boolean readOnly) {
    textValueLink(textComponent, Values.beanValue(owner, beanPropertyName, String.class, valueChangeEvent), format, immediateUpdate, readOnly);
  }

  /**
   * @param textComponent the text component to link with the value
   * @param value the value to link with the component
   * @param format the format to use when displaying the linked value, null if no formatting should be performed
   * @param immediateUpdate if true then the underlying model value is updated on each keystroke
   * @param readOnly if true the component will be read only
   */
  public static void textValueLink(final JTextComponent textComponent, final Value<String> value, final Format format,
                                   final boolean immediateUpdate, final boolean readOnly) {
    textComponent.setEditable(!readOnly);
    Values.link(value, UiValues.textValue(textComponent, format, immediateUpdate), readOnly);
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
   * @param beanPropertyName the property name
   * @param valueChangeEvent an EventObserver notified each time the value changes
   */
  public static void toggleValueLink(final ButtonModel buttonModel, final Object owner, final String beanPropertyName,
                                     final EventObserver<Boolean> valueChangeEvent) {
    toggleValueLink(buttonModel, owner, beanPropertyName, valueChangeEvent, false);
  }

  /**
   * @param buttonModel the button model to link with the value
   * @param owner the value owner
   * @param beanPropertyName the property name
   * @param valueChangeEvent an EventObserver notified each time the value changes
   * @param readOnly if true the component will be read only
   */
  public static void toggleValueLink(final ButtonModel buttonModel, final Object owner, final String beanPropertyName,
                                     final EventObserver<Boolean> valueChangeEvent, final boolean readOnly) {
    toggleValueLink(buttonModel, Values.beanValue(owner, beanPropertyName, boolean.class, valueChangeEvent), readOnly);
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
   * @param box the combo box to link with the value
   * @param owner the value owner
   * @param beanPropertyName the property name
   * @param valueClass the value class
   * @param valueChangeEvent an EventObserver notified each time the value changes
   */
  public static <V> void selectedItemValueLink(final JComboBox<V> box, final Object owner, final String beanPropertyName,
                                               final Class<V> valueClass, final EventObserver<V> valueChangeEvent) {
    selectedItemValueLink(box, owner, beanPropertyName, valueClass, valueChangeEvent, false);
  }

  /**
   * @param <V> the value type
   * @param box the combo box to link with the value
   * @param owner the value owner
   * @param beanPropertyName the property name
   * @param valueClass the value class
   * @param valueChangeEvent an EventObserver notified each time the value changes
   * @param readOnly if true the component will be read only
   */
  public static <V> void selectedItemValueLink(final JComboBox<V> box, final Object owner, final String beanPropertyName,
                                               final Class<V> valueClass, final EventObserver<V> valueChangeEvent,
                                               final boolean readOnly) {
    selectedItemValueLink(box, Values.beanValue(owner, beanPropertyName, valueClass, valueChangeEvent), readOnly);
  }

  /**
   * @param <V> the value type
   * @param box the combo box to link with the value
   * @param value the model value
   */
  public static <V> void selectedItemValueLink(final JComboBox<V> box, final Value<V> value) {
    Values.link(value, UiValues.selectedItemValue(box), false);
  }

  /**
   * @param <V> the value type
   * @param box the combo box to link with the value
   * @param value the model value
   * @param readOnly if true the component will be read only
   */
  public static <V> void selectedItemValueLink(final JComboBox<V> box, final Value<V> value, final boolean readOnly) {
    Values.link(value, UiValues.selectedItemValue(box), readOnly);
  }

  /**
   * @param owner the value owner
   * @param beanPropertyName the property name
   * @param valueChangeEvent an EventObserver notified each time the value changes
   * @return a SpinnerNumberModel based on the value
   */
  public static SpinnerNumberModel intSpinnerValueLink(final Object owner, final String beanPropertyName,
                                                       final EventObserver<Integer> valueChangeEvent) {
    return intSpinnerValueLink(owner, beanPropertyName, valueChangeEvent, false);
  }

  /**
   * @param owner the value owner
   * @param beanPropertyName the property name
   * @param valueChangeEvent an EventObserver notified each time the value changes
   * @param readOnly if true the component will be read only
   * @return a SpinnerNumberModel based on the value
   */
  public static SpinnerNumberModel intSpinnerValueLink(final Object owner, final String beanPropertyName,
                                                       final EventObserver<Integer> valueChangeEvent, final boolean readOnly) {
    final SpinnerNumberModel numberModel = new SpinnerNumberModel();
    intSpinnerValueLink(owner, beanPropertyName, valueChangeEvent, numberModel, readOnly);

    return numberModel;
  }

  /**
   * @param owner the value owner
   * @param beanPropertyName the property name
   * @param valueChangeEvent an EventObserver notified each time the value changes
   * @param spinnerModel the spinner model to use
   * @param readOnly if true the component will be read only
   */
  public static void intSpinnerValueLink(final Object owner, final String beanPropertyName, final EventObserver<Integer> valueChangeEvent,
                                         final SpinnerNumberModel spinnerModel, final boolean readOnly) {
    Values.link(Values.beanValue(owner, beanPropertyName, int.class, valueChangeEvent),
            UiValues.integerValue(spinnerModel), readOnly);
  }

  private static Class getDateTypeClass(final int sqlType) {
    switch (sqlType) {
      case Types.DATE:
        return Date.class;
      case Types.TIMESTAMP:
        return Timestamp.class;
      case Types.TIME:
        return Time.class;
      default:
        throw new IllegalArgumentException("Not a date based type: " + sqlType);
    }
  }
}
