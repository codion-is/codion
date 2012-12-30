/*
 * Copyright (c) 2004 - 2012, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui.control;

import org.jminor.common.model.DocumentAdapter;
import org.jminor.common.model.Event;
import org.jminor.common.model.EventObserver;
import org.jminor.common.model.Events;
import org.jminor.common.model.Item;
import org.jminor.common.model.StateObserver;
import org.jminor.common.model.Util;
import org.jminor.common.model.Value;
import org.jminor.common.model.checkbox.TristateButtonModel;
import org.jminor.common.model.combobox.FilteredComboBoxModel;
import org.jminor.common.model.combobox.ItemComboBoxModel;
import org.jminor.common.ui.textfield.DoubleField;
import org.jminor.common.ui.textfield.IntField;

import javax.swing.ButtonModel;
import javax.swing.ComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.lang.reflect.Method;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.Format;
import java.text.NumberFormat;
import java.text.ParseException;
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
    new ValueLink(modelValue, new DateUIValue(textComponent, dateFormat, isTimestamp), linkType);
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
    new ValueLink(modelValue, new IntUIValue(intField, format, usePrimitive), linkType);
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
    new ValueLink(modelValue, new DoubleUIValue(doubleField, format, usePrimitive), linkType);
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
    if (linkType == LinkType.READ_ONLY) {//todo side effect?
      textComponent.setEditable(false);
    }
    new ValueLink(modelValue, new TextUIValue(textComponent, format, immediateUpdate), linkType);
  }

  /**
   * @param textComponent the text component to link with the value
   * @param modelValue the model value
   * @param linkType the link type
   * @param format the format
   */
  public static void formattedTextValueLink(final JFormattedTextField textComponent, final Value modelValue,
                                            final LinkType linkType, final Format format) {
    new ValueLink(modelValue, new FormattedTextUIValue(textComponent, format, true), linkType);
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
    new ToggleValueLink(buttonModel, modelValue, new ToggleUIValue(buttonModel), caption, linkType, enabledObserver);
  }

  public static ToggleValueLink toggleControl(final Object owner, final String propertyName, final String caption,
                                              final EventObserver changeEvent) {
    return toggleControl(owner, propertyName, caption, changeEvent, (StateObserver) null);
  }

  public static ToggleValueLink toggleControl(final Object owner, final String propertyName, final String caption,
                                              final EventObserver changeEvent, final StateObserver enabledObserver) {
    final ButtonModel buttonModel = new JToggleButton.ToggleButtonModel();
    return new ToggleValueLink(buttonModel, new BeanModelValue(owner, propertyName, boolean.class,
            LinkType.READ_WRITE, changeEvent), new ToggleUIValue(buttonModel), caption, LinkType.READ_WRITE, enabledObserver);
  }

  public static ToggleValueLink toggleControl(final Object owner, final String propertyName, final String caption,
                                              final EventObserver changeEvent, final String description) {
    final ButtonModel buttonModel = new JToggleButton.ToggleButtonModel();
    return (ToggleValueLink) new ToggleValueLink(buttonModel,
            new BeanModelValue(owner, propertyName, boolean.class, LinkType.READ_WRITE, changeEvent),
            new ToggleUIValue(buttonModel), caption, LinkType.READ_WRITE, null).setDescription(description);
  }

  public static void tristateValueLink(final TristateButtonModel buttonModel, final Value modelValue,
                                       final LinkType linkType, final StateObserver enabledObserver) {
    new ValueLink<Boolean>(modelValue, new TristateUIValue(buttonModel), linkType, enabledObserver);
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
    new ValueLink(modelValue, new SelectedItemUIValue(box), linkType);
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
    new ValueLink(new BeanModelValue(owner, propertyName, int.class, linkType, valueChangeEvent), new IntSpinnerUIValue(spinnerModel), linkType);
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

  private static abstract class UIValue implements Value {
    protected final Event changeEvent = Events.event();

    /** {@inheritDoc} */
    @Override
    public final EventObserver getChangeEvent() {
      return changeEvent.getObserver();
    }
  }

  private static class TextUIValue extends UIValue {
    private final JTextComponent textComponent;
    private final Format format;

    private TextUIValue(final JTextComponent textComponent, final Format format, final boolean immediateUpdate) {
      this.textComponent = textComponent;
      this.format = format == null ? new Util.NullFormat() : format;
      if (immediateUpdate) {
        textComponent.getDocument().addDocumentListener(new DocumentAdapter() {
          /** {@inheritDoc} */
          @Override
          public final void contentsChanged(final DocumentEvent e) {
            changeEvent.fire();
          }
        });
      }
      else {
        textComponent.addFocusListener(new FocusAdapter() {
          /** {@inheritDoc} */
          @Override
          public void focusLost(final FocusEvent e) {
            changeEvent.fire();
          }
        });
      }
    }

    /** {@inheritDoc} */
    @Override
    public void set(final Object value) {
      try {
        synchronized (textComponent) {
          final Document document = textComponent.getDocument();
          document.remove(0, document.getLength());
          if (value != null) {
            document.insertString(0, textFromValue(value), null);
          }
        }
      }
      catch (BadLocationException e) {
        throw new RuntimeException(e);
      }
    }

    /** {@inheritDoc} */
    @Override
    public Object get() {
      return valueFromText(getText());
    }

    /**
     * Returns a String representation of the given value object, using the format,
     * an empty string is returned in case of a null value
     * @param value the value to return as String
     * @return a formatted String representation of the given value, an empty string if the value is null
     */
    protected String textFromValue(final Object value) {
      return value == null ? "" : format.format(value);
    }

    /**
     * Returns a property value based on the given text, if the text can not
     * be parsed into a valid value, null is returned
     * @param text the text from which to parse a value
     * @return a value from the given text, or null if the parsing did not yield a valid value
     */
    protected Object valueFromText(final String text) {
      if (text != null && text.isEmpty()) {
        return null;
      }

      return text;
    }

    /**
     * @return the text from the linked text component
     */
    protected final String getText() {
      try {
        return translate(textComponent.getDocument().getText(0, textComponent.getDocument().getLength()));
      }
      catch (BadLocationException e) {
        throw new RuntimeException(e);
      }
    }

    /**
     * Provides a hook into the value getting mechanism.
     * @param text the value returned from the UI component
     * @return the translated value
     */
    protected String translate(final String text) {
      return text;
    }

    protected Format getFormat() {
      return format;
    }
  }

  private static final class IntUIValue extends TextUIValue {
    private final boolean usePrimitive;

    private IntUIValue(final JTextComponent textComponent, final NumberFormat format, final boolean usePrimitive) {
      super(textComponent, format, true);
      this.usePrimitive = usePrimitive;
    }

    /** {@inheritDoc} */
    @Override
    protected Object valueFromText(final String text) {
      if (text.isEmpty() && usePrimitive) {
        return 0;
      }

      return Util.getInt(text);
    }
  }

  private static final class DoubleUIValue extends TextUIValue {
    private final boolean usePrimitive;

    private DoubleUIValue(final JTextComponent textComponent, final NumberFormat format, final boolean usePrimitive) {
      super(textComponent, format, true);
      this.usePrimitive = usePrimitive;
    }

    /** {@inheritDoc} */
    @Override
    protected Object valueFromText(final String text) {
      if (text.isEmpty() && usePrimitive) {
        return 0;
      }

      return Util.getDouble(text);
    }
  }

  private static class FormattedTextUIValue extends TextUIValue {
    private final JFormattedTextField.AbstractFormatter formatter;

    private FormattedTextUIValue(final JFormattedTextField textComponent, final Format format, final boolean immediateUpdate) {
      super(textComponent, format, immediateUpdate);
      this.formatter = textComponent.getFormatter();
    }

    /** {@inheritDoc} */
    @Override
    protected Object valueFromText(final String text) {
      if (text == null) {
        return null;
      }

      try {
        return translateParsedValue(getFormat().parseObject(text));
      }
      catch (ParseException nf) {
        return null;
      }
    }

    /** {@inheritDoc} */
    @Override
    protected final String translate(final String text) {
      try {
        return (String) formatter.stringToValue(text);
      }
      catch (ParseException e) {
        return null;
      }
    }

    /**
     * Allows for a hook into the value parsing mechanism, so that
     * a value returned by the format parsing can be replaced with, say
     * a subclass, or some more appropriate value.
     * By default this simple returns the value.
     * @param parsedValue the value to translate
     * @return a translated value
     */
    protected Object translateParsedValue(final Object parsedValue) {
      return parsedValue;
    }
  }

  private static final class DateUIValue extends FormattedTextUIValue {
    private final boolean isTimestamp;

    private DateUIValue(final JFormattedTextField textComponent, final Format format, final boolean isTimestamp) {
      super(textComponent, format, true);
      this.isTimestamp = isTimestamp;
    }

    /** {@inheritDoc} */
    @Override
    protected Object translateParsedValue(final Object parsedValue) {
      final Date formatted = (Date) parsedValue;
      return formatted == null ? null : isTimestamp ? new Timestamp(formatted.getTime()) : new Date(formatted.getTime());
    }
  }

  private static class ToggleUIValue extends UIValue {
    private final ButtonModel buttonModel;

    private ToggleUIValue(final ButtonModel buttonModel) {
      this.buttonModel = buttonModel;
      buttonModel.addItemListener(new ItemListener() {
        /** {@inheritDoc} */
        @Override
        public void itemStateChanged(final ItemEvent e) {
          changeEvent.fire();
        }
      });
    }

    /** {@inheritDoc} */
    @Override
    public void set(final Object value) {
      buttonModel.setSelected(value != null && (Boolean) value);
    }

    /** {@inheritDoc} */
    @Override
    public Boolean get() {
      return buttonModel.isSelected();
    }

    protected ButtonModel getButtonModel() {
      return buttonModel;
    }
  }

  private static final class TristateUIValue extends ToggleUIValue {

    private TristateUIValue(final TristateButtonModel buttonModel) {
      super(buttonModel);
    }

    /** {@inheritDoc} */
    @Override
    public void set(final Object value) {
      if (value == null) {
        ((TristateButtonModel) getButtonModel()).setIndeterminate();
      }
      else {
        getButtonModel().setSelected((Boolean) value);
      }
    }

    /** {@inheritDoc} */
    @Override
    public Boolean get() {
      if (((TristateButtonModel) getButtonModel()).isIndeterminate()) {
        return null;
      }

      return getButtonModel().isSelected();
    }
  }

  private static final class SelectedItemUIValue extends UIValue {
    private final JComboBox comboBox;

    private SelectedItemUIValue(final JComboBox comboBox) {
      this.comboBox = comboBox;
      comboBox.addItemListener(new ItemListener() {
        /** {@inheritDoc} */
        @Override
        public void itemStateChanged(final ItemEvent e) {
          if (e.getStateChange() == ItemEvent.SELECTED) {
            changeEvent.fire();
          }
        }
      });
      if (comboBox.isEditable()) {
        ((JTextField) comboBox.getEditor().getEditorComponent()).getDocument().addDocumentListener(new DocumentAdapter() {
          /** {@inheritDoc} */
          @Override
          public void contentsChanged(final DocumentEvent e) {
            comboBox.getModel().setSelectedItem(comboBox.getEditor().getItem());
          }
        });
      }

    }

    /** {@inheritDoc} */
    @Override
    public void set(Object value) {
      comboBox.getModel().setSelectedItem(value);
    }

    /** {@inheritDoc} */
    @Override
    public Object get() {
      final ComboBoxModel comboBoxModel = comboBox.getModel();
      if (comboBoxModel instanceof ItemComboBoxModel) {
        return ((Item) comboBoxModel.getSelectedItem()).getItem();
      }
      else if (comboBoxModel instanceof FilteredComboBoxModel) {
        return ((FilteredComboBoxModel) comboBoxModel).getSelectedValue();
      }

      return comboBoxModel.getSelectedItem();
    }
  }

  private static final class IntSpinnerUIValue extends UIValue {
    private final SpinnerNumberModel spinnerModel;

    private IntSpinnerUIValue(final SpinnerNumberModel spinnerModel) {
      this.spinnerModel = spinnerModel;
      this.spinnerModel.addChangeListener(new ChangeListener() {
        /** {@inheritDoc} */
        @Override
        public void stateChanged(final ChangeEvent e) {
          changeEvent.fire();
        }
      });
    }

    /** {@inheritDoc} */
    @Override
    public void set(final Object value) {
      spinnerModel.setValue(value);
    }

    /** {@inheritDoc} */
    @Override
    public Integer get() {
      return (Integer) spinnerModel.getValue();
    }
  }
}
