/*
 * Copyright (c) 2004 - 2012, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui;

import org.jminor.common.model.EventObserver;
import org.jminor.common.model.Util;
import org.jminor.common.model.Value;
import org.jminor.common.model.Values;
import org.jminor.common.model.checkbox.TristateButtonModel;
import org.jminor.common.ui.textfield.DoubleField;
import org.jminor.common.ui.textfield.IntField;

import javax.swing.ButtonModel;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JToggleButton;
import javax.swing.SpinnerNumberModel;
import javax.swing.text.JTextComponent;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.Format;
import java.text.NumberFormat;
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
   * @param linkType the link type
   * @param dateFormat the data format
   * @param isTimestamp if true then Timestamp values are used, otherwise Date
   */
  public static void dateValueLink(final JFormattedTextField textComponent, final Object owner,
                                   final String beanPropertyName, final EventObserver valueChangeEvent,
                                   final LinkType linkType, final DateFormat dateFormat, final boolean isTimestamp) {
    dateValueLink(textComponent, Values.<Date>beanValue(owner, beanPropertyName, isTimestamp ? Timestamp.class : Date.class,
            valueChangeEvent, linkType == LinkType.READ_ONLY), linkType, dateFormat, isTimestamp);
  }

  /**
   * Links a date value with a given text component
   * @param textComponent the text component to link with the value
   * @param modelValue the model value
   * @param linkType the link type
   * @param dateFormat the data format
   * @param isTimestamp if true then Timestamp values are used, otherwise Date
   */
  public static void dateValueLink(final JFormattedTextField textComponent, final Value<Date> modelValue,
                                   final LinkType linkType, final DateFormat dateFormat, final boolean isTimestamp) {
    setEditableDefault(textComponent, linkType);
    valueLink(modelValue, UiValues.dateValue(textComponent, dateFormat, isTimestamp), linkType);
  }

  /**
   * @param intField the int field to link with the value
   * @param owner the value owner
   * @param beanPropertyName the property name
   * @param valueChangeEvent an EventObserver notified each time the value changes
   */
  public static void intValueLink(final IntField intField, final Object owner, final String beanPropertyName,
                                  final EventObserver valueChangeEvent) {
    intValueLink(intField, owner, beanPropertyName, valueChangeEvent, LinkType.READ_WRITE);
  }

  /**
   * @param intField the int field to link with the value
   * @param owner the value owner
   * @param beanPropertyName the property name
   * @param valueChangeEvent an EventObserver notified each time the value changes
   * @param linkType the link type
   */
  public static void intValueLink(final IntField intField, final Object owner, final String beanPropertyName,
                                  final EventObserver valueChangeEvent, final LinkType linkType) {
    intValueLink(intField, owner, beanPropertyName, valueChangeEvent, linkType, true);
  }

  /**
   * @param intField the int field to link with the value
   * @param owner the value owner
   * @param beanPropertyName the property name
   * @param valueChangeEvent an EventObserver notified each time the value changes
   * @param usePrimitive if true then the property is assumed to be a primitive, int instead of Integer
   */
  public static void intValueLink(final IntField intField, final Object owner, final String beanPropertyName,
                                  final EventObserver valueChangeEvent, final boolean usePrimitive) {
    intValueLink(intField, owner, beanPropertyName, valueChangeEvent, LinkType.READ_WRITE, usePrimitive);
  }

  /**
   * @param intField the int field to link with the value
   * @param owner the value owner
   * @param beanPropertyName the property name
   * @param valueChangeEvent an EventObserver notified each time the value changes
   * @param linkType the link type
   * @param usePrimitive if true then the property is assumed to be a primitive, int instead of Integer
   */
  public static void intValueLink(final IntField intField, final Object owner, final String beanPropertyName,
                                  final EventObserver valueChangeEvent, final LinkType linkType, final boolean usePrimitive) {
    intValueLink(intField, Values.<Integer>beanValue(owner, beanPropertyName, usePrimitive ? int.class : Integer.class,
            valueChangeEvent, linkType == LinkType.READ_ONLY), linkType, usePrimitive, Util.getNonGroupingNumberFormat(true));
  }

  /**
   * @param intField the int field to link with the value
   * @param modelValue the model value
   * @param linkType the link type
   * @param usePrimitive if true then the property is assumed to be a primitive, int instead of Integer
   */
  public static void intValueLink(final IntField intField, final Value<Integer> modelValue, final LinkType linkType,
                                  final boolean usePrimitive, final NumberFormat format) {
    setEditableDefault(intField, linkType);
    valueLink(modelValue, UiValues.integerValue(intField, usePrimitive, format), linkType);
  }

  /**
   * @param doubleField the double field to link with the value
   * @param owner the value owner
   * @param beanPropertyName the property name
   * @param valueChangeEvent an EventObserver notified each time the value changes
   * @param usePrimitive if true then the property is assumed to be a primitive, double instead of Double
   */
  public static void doubleValueLink(final DoubleField doubleField, final Object owner, final String beanPropertyName,
                                     final EventObserver valueChangeEvent, final boolean usePrimitive) {
    doubleValueLink(doubleField, owner, beanPropertyName, valueChangeEvent, LinkType.READ_WRITE, usePrimitive);
  }

  /**
   * @param doubleField the double field to link with the value
   * @param owner the value owner
   * @param beanPropertyName the property name
   * @param valueChangeEvent an EventObserver notified each time the value changes
   * @param linkType the link type
   * @param usePrimitive if true then the property is assumed to be a primitive, double instead of Double
   */
  public static void doubleValueLink(final DoubleField doubleField, final Object owner, final String beanPropertyName,
                                     final EventObserver valueChangeEvent, final LinkType linkType, final boolean usePrimitive) {
    doubleValueLink(doubleField, Values.<Double>beanValue(owner, beanPropertyName, usePrimitive ? double.class : Double.class,
            valueChangeEvent, linkType == LinkType.READ_ONLY), linkType, usePrimitive, Util.getNonGroupingNumberFormat());
  }

  /**
   * @param doubleField the double field to link with the value
   * @param modelValue the model value
   * @param linkType the link type
   * @param usePrimitive if true then the property is assumed to be a primitive, double instead of Double
   */
  public static void doubleValueLink(final DoubleField doubleField, final Value<Double> modelValue, final LinkType linkType,
                                     final boolean usePrimitive, final NumberFormat format) {
    setEditableDefault(doubleField, linkType);
    valueLink(modelValue, UiValues.doubleValue(doubleField, usePrimitive, format), linkType);
  }

  /**
   * @param textComponent the text component to link with the value
   * @param owner the value owner
   * @param beanPropertyName the property name
   * @param valueChangeEvent an EventObserver notified each time the value changes
   */
  public static void textValueLink(final JTextComponent textComponent, final Object owner, final String beanPropertyName,
                                   final EventObserver valueChangeEvent) {
    textValueLink(textComponent, owner, beanPropertyName, String.class, valueChangeEvent, LinkType.READ_WRITE);
  }

  /**
   * @param textComponent the text component to link with the value
   * @param owner the value owner
   * @param beanPropertyName the property name
   * @param valueClass the value class
   * @param valueChangeEvent an EventObserver notified each time the value changes
   */
  public static void textValueLink(final JTextComponent textComponent, final Object owner, final String beanPropertyName,
                                   final Class<?> valueClass, final EventObserver valueChangeEvent) {
    textValueLink(textComponent, owner, beanPropertyName, valueClass, valueChangeEvent, LinkType.READ_WRITE);
  }

  /**
   * @param textComponent the text component to link with the value
   * @param owner the value owner
   * @param beanPropertyName the property name
   * @param valueClass the value class
   * @param valueChangeEvent an EventObserver notified each time the value changes
   * @param linkType the link type
   */
  public static void textValueLink(final JTextComponent textComponent, final Object owner, final String beanPropertyName,
                                   final Class<?> valueClass, final EventObserver valueChangeEvent, final LinkType linkType) {
    textValueLink(textComponent, owner, beanPropertyName, valueClass, valueChangeEvent, linkType, null);
  }

  /**
   * @param textComponent the text component to link with the value
   * @param owner the value owner
   * @param beanPropertyName the property name
   * @param valueClass the value class
   * @param valueChangeEvent an EventObserver notified each time the value changes
   * @param linkType the link type
   * @param format the format to use when displaying the linked value,
   * null if no formatting should be performed
   */
  public static void textValueLink(final JTextComponent textComponent, final Object owner, final String beanPropertyName,
                                   final Class<?> valueClass, final EventObserver valueChangeEvent, final LinkType linkType,
                                   final Format format) {
    textValueLink(textComponent, owner, beanPropertyName, valueClass, valueChangeEvent, linkType, format, true);
  }

  /**
   * @param textComponent the text component to link with the value
   * @param owner the value owner
   * @param beanPropertyName the property name
   * @param valueClass the value class
   * @param valueChangeEvent an EventObserver notified each time the value changes
   * @param linkType the link type
   * @param format the format to use when displaying the linked value,
   * null if no formatting should be performed
   * @param immediateUpdate if true then the underlying model value is updated on each keystroke,
   * otherwise it is updated on actionPerformed or focusLost
   */
  public static void textValueLink(final JTextComponent textComponent, final Object owner, final String beanPropertyName,
                                   final Class<?> valueClass, final EventObserver valueChangeEvent, final LinkType linkType,
                                   final Format format, final boolean immediateUpdate) {
    textValueLink(textComponent, Values.<String>beanValue(owner, beanPropertyName, valueClass, valueChangeEvent,
            linkType == LinkType.READ_ONLY), linkType, format, immediateUpdate);
  }

  /**
   * @param textComponent the text component to link with the value
   * @param linkType the link type
   * @param format the format to use when displaying the linked value,
   * null if no formatting should be performed
   * @param immediateUpdate if true then the underlying model value is updated on each keystroke,
   * otherwise it is updated on actionPerformed or focusLost
   */
  public static void textValueLink(final JTextComponent textComponent, final Value<String> modelValue, final LinkType linkType,
                                   final Format format, final boolean immediateUpdate) {
    setEditableDefault(textComponent, linkType);
    valueLink(modelValue, UiValues.textValue(textComponent, format, immediateUpdate), linkType);
  }

  /**
   * @param owner the value owner
   * @param propertyName the property name
   * @param valueChangeEvent an EventObserver notified each time the value changes
   */
  public static ButtonModel toggleValueLink(final Object owner, final String propertyName, final EventObserver valueChangeEvent) {
    return toggleValueLink(owner, propertyName, valueChangeEvent, LinkType.READ_WRITE);
  }

  /**
   * @param owner the value owner
   * @param propertyName the property name
   * @param valueChangeEvent an EventObserver notified each time the value changes
   * @param linkType the link type
   */
  public static ButtonModel toggleValueLink(final Object owner, final String propertyName, final EventObserver valueChangeEvent,
                                            final LinkType linkType) {
    final ButtonModel buttonModel = new JToggleButton.ToggleButtonModel();
    toggleValueLink(buttonModel, owner, propertyName, valueChangeEvent, linkType);

    return buttonModel;
  }

  /**
   * @param buttonModel the button model to link with the value
   * @param owner the value owner
   * @param beanPropertyName the property name
   * @param valueChangeEvent an EventObserver notified each time the value changes
   */
  public static void toggleValueLink(final ButtonModel buttonModel, final Object owner, final String beanPropertyName,
                                     final EventObserver valueChangeEvent) {
    toggleValueLink(buttonModel, owner, beanPropertyName, valueChangeEvent, LinkType.READ_WRITE);
  }

  /**
   * @param buttonModel the button model to link with the value
   * @param owner the value owner
   * @param beanPropertyName the property name
   * @param valueChangeEvent an EventObserver notified each time the value changes
   * @param linkType the link type
   */
  public static void toggleValueLink(final ButtonModel buttonModel, final Object owner, final String beanPropertyName,
                                     final EventObserver valueChangeEvent, final LinkType linkType) {
    toggleValueLink(buttonModel, Values.<Boolean>beanValue(owner, beanPropertyName, boolean.class, valueChangeEvent,
            linkType == LinkType.READ_ONLY), linkType);
  }

  /**
   * @param buttonModel the button model to link with the value
   * @param modelValue the model value
   * @param linkType the link type
   */
  public static void toggleValueLink(final ButtonModel buttonModel, final Value<Boolean> modelValue, final LinkType linkType) {
    valueLink(modelValue, UiValues.booleanValue(buttonModel), linkType);
  }

  /**
   * @param buttonModel the button model
   * @param modelValue the model value
   * @param linkType the link type
   */
  public static void tristateValueLink(final TristateButtonModel buttonModel, final Value<Boolean> modelValue,
                                       final LinkType linkType) {
    valueLink(modelValue, UiValues.tristateValue(buttonModel), linkType);
  }

  /**
   * @param box the combo box to link with the value
   * @param owner the value owner
   * @param beanPropertyName the property name
   * @param valueClass the value class
   * @param valueChangeEvent an EventObserver notified each time the value changes
   */
  public static void selectedItemValueLink(final JComboBox box, final Object owner, final String beanPropertyName,
                                           final Class valueClass, final EventObserver valueChangeEvent) {
    selectedItemValueLink(box, owner, beanPropertyName, valueClass, valueChangeEvent, LinkType.READ_WRITE);
  }

  /**
   * @param box the combo box to link with the value
   * @param owner the value owner
   * @param beanPropertyName the property name
   * @param valueClass the value class
   * @param valueChangeEvent an EventObserver notified each time the value changes
   * @param linkType the link type
   */
  public static void selectedItemValueLink(final JComboBox box, final Object owner, final String beanPropertyName,
                                           final Class valueClass, final EventObserver valueChangeEvent,
                                           final LinkType linkType) {
    selectedItemValueLink(box, Values.<Object>beanValue(owner, beanPropertyName, valueClass, valueChangeEvent,
            linkType == LinkType.READ_ONLY), linkType);
  }

  /**
   * @param box the combo box to link with the value
   * @param modelValue the model value
   * @param linkType the link type
   */
  public static void selectedItemValueLink(final JComboBox box, final Value<Object> modelValue, final LinkType linkType) {
    valueLink(modelValue, UiValues.selectedItemValue(box), linkType);
  }

  /**
   * @param owner the value owner
   * @param beanPropertyName the property name
   * @param valueChangeEvent an EventObserver notified each time the value changes
   */
  public static SpinnerNumberModel intSpinnerValueLink(final Object owner, final String beanPropertyName,
                                                       final EventObserver valueChangeEvent) {
    return intSpinnerValueLink(owner, beanPropertyName, valueChangeEvent, LinkType.READ_WRITE);
  }

  /**
   * @param owner the value owner
   * @param beanPropertyName the property name
   * @param valueChangeEvent an EventObserver notified each time the value changes
   * @param linkType the link type
   */
  public static SpinnerNumberModel intSpinnerValueLink(final Object owner, final String beanPropertyName,
                                                       final EventObserver valueChangeEvent, final LinkType linkType) {
    final SpinnerNumberModel numberModel = new SpinnerNumberModel();
    intSpinnerValueLink(owner, beanPropertyName, valueChangeEvent, linkType, numberModel);

    return numberModel;
  }

  /**
   * @param owner the value owner
   * @param beanPropertyName the property name
   * @param valueChangeEvent an EventObserver notified each time the value changes
   * @param linkType the link type
   * @param spinnerModel the spinner model to use
   */
  public static void intSpinnerValueLink(final Object owner, final String beanPropertyName, final EventObserver valueChangeEvent,
                                         final LinkType linkType, final SpinnerNumberModel spinnerModel) {
    valueLink(Values.<Integer>beanValue(owner, beanPropertyName, int.class, valueChangeEvent, linkType == LinkType.READ_ONLY),
            UiValues.integerValue(spinnerModel), linkType);
  }

  /**
   * Links the two values together
   * @param modelValue the model value
   * @param uiValue the ui value
   * @param linkType the link type
   * @param <V> the value type
   */
  public static <V> void valueLink(final Value<V> modelValue, final Value<V> uiValue, final LinkType linkType) {
    Values.link(modelValue, uiValue, linkType == LinkType.READ_ONLY);
  }

  private static void setEditableDefault(final JTextComponent component, final LinkType linkType) {
    if (linkType == LinkType.READ_ONLY) {
      component.setEditable(false);
    }
  }
}
