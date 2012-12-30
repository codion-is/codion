/*
 * Copyright (c) 2004 - 2012, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui.control;

import org.jminor.common.model.EventObserver;
import org.jminor.common.model.StateObserver;
import org.jminor.common.model.Util;
import org.jminor.common.model.Value;
import org.jminor.common.model.checkbox.TristateButtonModel;
import org.jminor.common.ui.textfield.DoubleField;
import org.jminor.common.ui.textfield.IntField;

import javax.swing.ButtonModel;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JToggleButton;
import javax.swing.SpinnerNumberModel;
import javax.swing.text.JTextComponent;
import java.lang.reflect.Method;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.Format;
import java.text.NumberFormat;
import java.util.Date;

/**
 * A factory class for binding values to UI components
 */
public final class ValueLinks {

  public static void dateBeanValueLink(final JFormattedTextField textComponent, final Object owner,
                                       final String propertyName, final EventObserver valueChangeEvent,
                                       final LinkType linkType, final DateFormat dateFormat, final boolean isTimestamp) {
    dateValueLink(textComponent, new BeanModelValue(owner, propertyName, isTimestamp ? Timestamp.class : Date.class, linkType, valueChangeEvent),
            linkType, dateFormat, isTimestamp);
  }

  public static void dateValueLink(final JFormattedTextField textComponent, final Value modelValue,
                                   final LinkType linkType, final DateFormat dateFormat, final boolean isTimestamp) {
    new DateValueLink(textComponent, modelValue, linkType, dateFormat, isTimestamp);
  }

  /**
   * @param intField the int field to link with the value
   * @param owner the value owner
   * @param propertyName the property name
   * @param valueChangeEvent an EventObserver notified each time the value changes
   */
  public static void intBeanValueLink(final IntField intField, final Object owner, final String propertyName,
                                      final EventObserver valueChangeEvent) {
    intBeanValueLink(intField, owner, propertyName, valueChangeEvent, LinkType.READ_WRITE);
  }

  /**
   * @param intField the int field to link with the value
   * @param owner the value owner
   * @param propertyName the property name
   * @param valueChangeEvent an EventObserver notified each time the value changes
   * @param linkType the link type
   */
  public static void intBeanValueLink(final IntField intField, final Object owner, final String propertyName,
                                      final EventObserver valueChangeEvent, final LinkType linkType) {
    intBeanValueLink(intField, owner, propertyName, valueChangeEvent, linkType, true);
  }

  /**
   * @param intField the int field to link with the value
   * @param owner the value owner
   * @param propertyName the property name
   * @param valueChangeEvent an EventObserver notified each time the value changes
   * @param usePrimitive if true then the property is assumed to be a primitive, int instead of Integer
   */
  public static void intBeanValueLink(final IntField intField, final Object owner, final String propertyName,
                                      final EventObserver valueChangeEvent, final boolean usePrimitive) {
    intBeanValueLink(intField, owner, propertyName, valueChangeEvent, LinkType.READ_WRITE, usePrimitive);
  }

  /**
   * @param intField the int field to link with the value
   * @param owner the value owner
   * @param propertyName the property name
   * @param valueChangeEvent an EventObserver notified each time the value changes
   * @param linkType the link type
   * @param usePrimitive if true then the property is assumed to be a primitive, int instead of Integer
   */
  public static void intBeanValueLink(final IntField intField, final Object owner, final String propertyName,
                                      final EventObserver valueChangeEvent, final LinkType linkType, final boolean usePrimitive) {
    intValueLink(intField, new BeanModelValue(owner, propertyName, usePrimitive ? int.class : Integer.class, linkType, valueChangeEvent),
            linkType, usePrimitive, Util.getNonGroupingNumberFormat(true));
  }

  /**
   * @param intField the int field to link with the value
   * @param modelValue the model value
   * @param linkType the link type
   * @param usePrimitive if true then the property is assumed to be a primitive, int instead of Integer
   */
  public static void intValueLink(final IntField intField, final Value modelValue,
                                  final LinkType linkType, final boolean usePrimitive, final NumberFormat format) {
    new IntValueLink(intField, modelValue, linkType, usePrimitive, format);
  }

  /**
   * @param doubleField the double field to link with the value
   * @param owner the value owner
   * @param propertyName the property name
   * @param valueChangeEvent an EventObserver notified each time the value changes
   * @param usePrimitive if true then the property is assumed to be a primitive, double instead of Double
   */
  public static void doubleBeanValueLink(final DoubleField doubleField, final Object owner, final String propertyName,
                                         final EventObserver valueChangeEvent, final boolean usePrimitive) {
    doubleBeanValueLink(doubleField, owner, propertyName, valueChangeEvent, LinkType.READ_WRITE, usePrimitive);
  }

  /**
   * @param doubleField the double field to link with the value
   * @param owner the value owner
   * @param propertyName the property name
   * @param valueChangeEvent an EventObserver notified each time the value changes
   * @param linkType the link type
   * @param usePrimitive if true then the property is assumed to be a primitive, double instead of Double
   */
  public static void doubleBeanValueLink(final DoubleField doubleField, final Object owner, final String propertyName,
                                         final EventObserver valueChangeEvent, final LinkType linkType, final boolean usePrimitive) {
    doubleValueLink(doubleField, new BeanModelValue(owner, propertyName, usePrimitive ? double.class : Double.class, linkType, valueChangeEvent),
            linkType, usePrimitive, Util.getNonGroupingNumberFormat());
  }

  /**
   * @param doubleField the double field to link with the value
   * @param modelValue the model value
   * @param linkType the link type
   * @param usePrimitive if true then the property is assumed to be a primitive, double instead of Double
   */
  public static void doubleValueLink(final DoubleField doubleField, final Value modelValue,
                                     final LinkType linkType, final boolean usePrimitive, final NumberFormat format) {
    new DoubleValueLink(doubleField, modelValue, linkType, usePrimitive, format);
  }

  /**
   * @param textComponent the text component to link with the value
   * @param owner the value owner
   * @param propertyName the property name
   * @param valueChangeEvent an EventObserver notified each time the value changes
   */
  public static void textBeanValueLink(final JTextComponent textComponent, final Object owner, final String propertyName,
                                       final EventObserver valueChangeEvent) {
    textBeanValueLink(textComponent, owner, propertyName, String.class, valueChangeEvent, LinkType.READ_WRITE);
  }

  /**
   * @param textComponent the text component to link with the value
   * @param owner the value owner
   * @param propertyName the property name
   * @param valueClass the value class
   * @param valueChangeEvent an EventObserver notified each time the value changes
   */
  public static void textBeanValueLink(final JTextComponent textComponent, final Object owner, final String propertyName,
                                       final Class<?> valueClass, final EventObserver valueChangeEvent) {
    textBeanValueLink(textComponent, owner, propertyName, valueClass, valueChangeEvent, LinkType.READ_WRITE);
  }

  /**
   * @param textComponent the text component to link with the value
   * @param owner the value owner
   * @param propertyName the property name
   * @param valueClass the value class
   * @param valueChangeEvent an EventObserver notified each time the value changes
   * @param linkType the link type
   */
  public static void textBeanValueLink(final JTextComponent textComponent, final Object owner, final String propertyName,
                                       final Class<?> valueClass, final EventObserver valueChangeEvent, final LinkType linkType) {
    textBeanValueLink(textComponent, owner, propertyName, valueClass, valueChangeEvent, linkType, null);
  }

  /**
   * @param textComponent the text component to link with the value
   * @param owner the value owner
   * @param propertyName the property name
   * @param valueClass the value class
   * @param valueChangeEvent an EventObserver notified each time the value changes
   * @param linkType the link type
   * @param format the format to use when displaying the linked value,
   * null if no formatting should be performed
   */
  public static void textBeanValueLink(final JTextComponent textComponent, final Object owner, final String propertyName,
                                       final Class<?> valueClass, final EventObserver valueChangeEvent, final LinkType linkType,
                                       final Format format) {
    textBeanValueLink(textComponent, owner, propertyName, valueClass, valueChangeEvent, linkType, format, true);
  }

  /**
   * @param textComponent the text component to link with the value
   * @param owner the value owner
   * @param propertyName the property name
   * @param valueClass the value class
   * @param valueChangeEvent an EventObserver notified each time the value changes
   * @param linkType the link type
   * @param format the format to use when displaying the linked value,
   * null if no formatting should be performed
   * @param immediateUpdate if true then the underlying model value is updated on each keystroke,
   * otherwise it is updated on actionPerformed or focusLost
   */
  public static void textBeanValueLink(final JTextComponent textComponent, final Object owner, final String propertyName,
                                       final Class<?> valueClass, final EventObserver valueChangeEvent, final LinkType linkType,
                                       final Format format, final boolean immediateUpdate) {
    textValueLink(textComponent, new BeanModelValue(owner, propertyName, valueClass, linkType, valueChangeEvent),
            linkType, format, immediateUpdate);
  }

  /**
   * @param textComponent the text component to link with the value
   * @param linkType the link type
   * @param format the format to use when displaying the linked value,
   * null if no formatting should be performed
   * @param immediateUpdate if true then the underlying model value is updated on each keystroke,
   * otherwise it is updated on actionPerformed or focusLost
   */
  public static void textValueLink(final JTextComponent textComponent, final Value modelValue, final LinkType linkType,
                                   final Format format, final boolean immediateUpdate) {
    new TextValueLink(textComponent, modelValue, linkType, format, immediateUpdate);
  }

  /**
   * @param textComponent the text component to link with the value
   * @param modelValue the model value
   * @param linkType the link type
   * @param format the format
   */
  public static void formattedTextValueLink(final JFormattedTextField textComponent, final Value modelValue,
                                            final LinkType linkType, final Format format) {
    new FormattedTextValueLink(textComponent, modelValue, linkType, format);
  }

  /**
   * @param owner the value owner
   * @param propertyName the property name
   * @param valueChangeEvent an EventObserver notified each time the value changes
   */
  public static ButtonModel toggleBeanValueLink(final Object owner, final String propertyName, final EventObserver valueChangeEvent) {
    return toggleBeanValueLink(owner, propertyName, valueChangeEvent, null);
  }

  /**
   * @param owner the value owner
   * @param propertyName the property name
   * @param valueChangeEvent an EventObserver notified each time the value changes
   * @param caption the check box caption, if any
   */
  public static ButtonModel toggleBeanValueLink(final Object owner, final String propertyName, final EventObserver valueChangeEvent,
                                                final String caption) {
    return toggleBeanValueLink(owner, propertyName, valueChangeEvent, caption, LinkType.READ_WRITE);
  }

  /**
   * @param owner the value owner
   * @param propertyName the property name
   * @param valueChangeEvent an EventObserver notified each time the value changes
   * @param caption the check box caption, if any
   * @param linkType the link type
   */
  public static ButtonModel toggleBeanValueLink(final Object owner, final String propertyName, final EventObserver valueChangeEvent,
                                                final String caption, final LinkType linkType) {
    return toggleBeanValueLink(owner, propertyName, valueChangeEvent, caption, linkType, null);
  }

  /**
   * @param owner the value owner
   * @param propertyName the property name
   * @param valueChangeEvent an EventObserver notified each time the value changes
   * @param caption the check box caption, if any
   * @param linkType the link type
   * @param enabledObserver the state observer dictating the enable state of the control associated with this value link
   */
  public static ButtonModel toggleBeanValueLink(final Object owner, final String propertyName, final EventObserver valueChangeEvent,
                                                final String caption, final LinkType linkType, final StateObserver enabledObserver) {
    final ButtonModel buttonModel = new JToggleButton.ToggleButtonModel();
    toggleBeanValueLink(buttonModel, owner, propertyName, valueChangeEvent, caption, linkType, enabledObserver);

    return buttonModel;
  }

  /**
   * @param buttonModel the button model to link with the value
   * @param owner the value owner
   * @param propertyName the property name
   * @param valueChangeEvent an EventObserver notified each time the value changes
   */
  public static void toggleBeanValueLink(final ButtonModel buttonModel, final Object owner, final String propertyName,
                                         final EventObserver valueChangeEvent) {
    toggleBeanValueLink(buttonModel, owner, propertyName, valueChangeEvent, null);
  }

  /**
   * @param buttonModel the button model to link with the value
   * @param owner the value owner
   * @param propertyName the property name
   * @param valueChangeEvent an EventObserver notified each time the value changes
   * @param caption the check box caption, if any
   */
  public static void toggleBeanValueLink(final ButtonModel buttonModel, final Object owner, final String propertyName,
                                         final EventObserver valueChangeEvent, final String caption) {
    toggleBeanValueLink(buttonModel, owner, propertyName, valueChangeEvent, caption, LinkType.READ_WRITE);
  }

  /**
   * @param buttonModel the button model to link with the value
   * @param owner the value owner
   * @param propertyName the property name
   * @param valueChangeEvent an EventObserver notified each time the value changes
   * @param caption the check box caption, if any
   * @param linkType the link type
   */
  public static void toggleBeanValueLink(final ButtonModel buttonModel, final Object owner, final String propertyName,
                                         final EventObserver valueChangeEvent, final String caption, final LinkType linkType) {
    toggleBeanValueLink(buttonModel, owner, propertyName, valueChangeEvent, caption, linkType, null);
  }

  /**
   * @param buttonModel the button model to link with the value
   * @param owner the value owner
   * @param propertyName the property name
   * @param valueChangeEvent an EventObserver notified each time the value changes
   * @param caption the check box caption, if any
   * @param linkType the link type
   * @param enabledObserver the state observer dictating the enable state of the control associated with this value link
   */
  public static void toggleBeanValueLink(final ButtonModel buttonModel, final Object owner, final String propertyName,
                                         final EventObserver valueChangeEvent, final String caption, final LinkType linkType,
                                         final StateObserver enabledObserver) {
    toggleValueLink(buttonModel, new BeanModelValue(owner, propertyName, boolean.class, linkType, valueChangeEvent),
            caption, linkType, enabledObserver);
  }

  /**
   * @param buttonModel the button model to link with the value
   * @param modelValue the model value
   * @param caption the check box caption, if any
   * @param linkType the link type
   * @param enabledObserver the state observer dictating the enable state of the control associated with this value link
   */
  public static void toggleValueLink(final ButtonModel buttonModel, final Value modelValue,
                                     final String caption, final LinkType linkType, final StateObserver enabledObserver) {
    new ToggleValueLink(buttonModel, modelValue, caption, linkType, enabledObserver);
  }

  public static ToggleValueLink toggleControl(final Object owner, final String propertyName, final String caption,
                                              final EventObserver changeEvent) {
    return toggleControl(owner, propertyName, caption, changeEvent, (StateObserver) null);
  }

  public static ToggleValueLink toggleControl(final Object owner, final String propertyName, final String caption,
                                              final EventObserver changeEvent, final StateObserver enabledObserver) {
    return new ToggleValueLink(new JToggleButton.ToggleButtonModel(), new BeanModelValue(owner, propertyName, boolean.class,
            LinkType.READ_WRITE, changeEvent), caption, LinkType.READ_WRITE, enabledObserver);
  }

  public static ToggleValueLink toggleControl(final Object owner, final String propertyName, final String caption,
                                              final EventObserver changeEvent, final String description) {
    return (ToggleValueLink) new ToggleValueLink(new JToggleButton.ToggleButtonModel(),
            new BeanModelValue(owner, propertyName, boolean.class, LinkType.READ_WRITE, changeEvent), caption,
            LinkType.READ_WRITE, null).setDescription(description);
  }

  public static void tristateValueLink(final TristateButtonModel buttonModel, final Value modelValue,
                                       final LinkType linkType, final StateObserver enabledObserver) {
    new TristateValueLink(buttonModel, modelValue, null, linkType, enabledObserver);
  }

  /**
   * @param box the combo box to link with the value
   * @param owner the value owner
   * @param propertyName the property name
   * @param valueClass the value class
   * @param valueChangeEvent an EventObserver notified each time the value changes
   */
  public static void selectedItemBeanValueLink(final JComboBox box, final Object owner, final String propertyName,
                                               final Class valueClass, final EventObserver valueChangeEvent) {
    selectedItemBeanValueLink(box, owner, propertyName, valueClass, valueChangeEvent, LinkType.READ_WRITE);
  }

  /**
   * @param box the combo box to link with the value
   * @param owner the value owner
   * @param propertyName the property name
   * @param valueClass the value class
   * @param valueChangeEvent an EventObserver notified each time the value changes
   * @param linkType the link type
   */
  public static void selectedItemBeanValueLink(final JComboBox box, final Object owner, final String propertyName,
                                               final Class valueClass, final EventObserver valueChangeEvent,
                                               final LinkType linkType) {
    selectedItemValueLink(box, new BeanModelValue(owner, propertyName, valueClass, linkType, valueChangeEvent), linkType);
  }

  /**
   * @param box the combo box to link with the value
   * @param modelValue the model value
   * @param linkType the link type
   */
  public static void selectedItemValueLink(final JComboBox box, final Value modelValue, final LinkType linkType) {
    new SelectedItemValueLink(box, modelValue, linkType);
  }

  /**
   * @param owner the value owner
   * @param propertyName the property name
   * @param valueChangeEvent an EventObserver notified each time the value changes
   */
  public static SpinnerNumberModel intBeanSpinnerValueLink(final Object owner, final String propertyName,
                                                           final EventObserver valueChangeEvent) {
    return intBeanSpinnerValueLink(owner, propertyName, valueChangeEvent, LinkType.READ_WRITE);
  }

  /**
   * @param owner the value owner
   * @param propertyName the property name
   * @param valueChangeEvent an EventObserver notified each time the value changes
   * @param linkType the link type
   */
  public static SpinnerNumberModel intBeanSpinnerValueLink(final Object owner, final String propertyName,
                                                           final EventObserver valueChangeEvent, final LinkType linkType) {
    final SpinnerNumberModel numberModel = new SpinnerNumberModel();
    intBeanSpinnerValueLink(owner, propertyName, valueChangeEvent, linkType, numberModel);

    return numberModel;
  }

  /**
   * @param owner the value owner
   * @param propertyName the property name
   * @param valueChangeEvent an EventObserver notified each time the value changes
   * @param linkType the link type
   * @param spinnerModel the spinner model to use
   */
  public static void intBeanSpinnerValueLink(final Object owner, final String propertyName, final EventObserver valueChangeEvent,
                                             final LinkType linkType, final SpinnerNumberModel spinnerModel) {
    new IntSpinnerValueLink(new BeanModelValue(owner, propertyName, int.class, linkType, valueChangeEvent), linkType, spinnerModel);
  }

  private static final class BeanModelValue implements Value {

    private final Object valueOwner;
    private final Method getMethod;
    private final Method setMethod;
    private final EventObserver changeEvent;

    private BeanModelValue(final Object valueOwner, final String propertyName, final Class<?> valueClass, final LinkType linkType,
                           final EventObserver changeEvent) {
      Util.rejectNullValue(valueOwner, "valueOwner");
      Util.rejectNullValue(valueClass, "valueClass");
      if (Util.nullOrEmpty(propertyName)) {
        throw new IllegalArgumentException("propertyName is null or an empty string");
      }
      try {
        this.valueOwner = valueOwner;
        this.changeEvent = changeEvent;
        this.getMethod = Util.getGetMethod(valueClass, propertyName, valueOwner);
        if (linkType == LinkType.READ_ONLY) {
          this.setMethod = null;
        }
        else {
          this.setMethod = Util.getSetMethod(valueClass, propertyName, valueOwner);
        }
      }
      catch (NoSuchMethodException e) {
        throw new IllegalArgumentException("Bean property methods for " + propertyName + ", type: " + valueClass + " not found in class " + valueOwner.getClass().getName(), e);
      }
    }

    /** {@inheritDoc} */
    @Override
    public Object get() {
      try {
        return getMethod.invoke(valueOwner);
      }
      catch (RuntimeException re) {
        throw re;
      }
      catch (Exception e) {
        throw new RuntimeException(e);
      }
    }

    /** {@inheritDoc} */
    @Override
    public void set(final Object value) {
      try {
        setMethod.invoke(valueOwner, value);
      }
      catch (RuntimeException re) {
        throw re;
      }
      catch (Exception e) {
        throw new RuntimeException(e);
      }
    }

    /** {@inheritDoc} */
    @Override
    public EventObserver getChangeEvent() {
      return changeEvent;
    }
  }
}
