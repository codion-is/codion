/*
 * Copyright (c) 2004 - 2012, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui;

import org.jminor.common.model.DocumentAdapter;
import org.jminor.common.model.Event;
import org.jminor.common.model.EventAdapter;
import org.jminor.common.model.EventObserver;
import org.jminor.common.model.Events;
import org.jminor.common.model.Item;
import org.jminor.common.model.StateObserver;
import org.jminor.common.model.Util;
import org.jminor.common.model.Value;
import org.jminor.common.model.checkbox.TristateButtonModel;
import org.jminor.common.model.combobox.FilteredComboBoxModel;
import org.jminor.common.model.combobox.ItemComboBoxModel;
import org.jminor.common.ui.control.ToggleControl;
import org.jminor.common.ui.textfield.DoubleField;
import org.jminor.common.ui.textfield.IntField;

import javax.swing.ButtonModel;
import javax.swing.ComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
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
    dateValueLink(textComponent, new BeanModelValue<Date>(owner, beanPropertyName, isTimestamp ? Timestamp.class : Date.class,
            linkType, valueChangeEvent), linkType, dateFormat, isTimestamp);
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
    valueLink(modelValue, new DateUIValue(textComponent, dateFormat, isTimestamp), linkType);
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
    intValueLink(intField, new BeanModelValue<Integer>(owner, beanPropertyName, usePrimitive ? int.class : Integer.class, linkType,
            valueChangeEvent), linkType, usePrimitive, Util.getNonGroupingNumberFormat(true));
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
    valueLink(modelValue, new IntUIValue(intField, format, usePrimitive), linkType);
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
    doubleValueLink(doubleField, new BeanModelValue<Double>(owner, beanPropertyName, usePrimitive ? double.class : Double.class,
            linkType, valueChangeEvent), linkType, usePrimitive, Util.getNonGroupingNumberFormat());
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
    valueLink(modelValue, new DoubleUIValue(doubleField, format, usePrimitive), linkType);
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
    textValueLink(textComponent, new BeanModelValue<String>(owner, beanPropertyName, valueClass, linkType, valueChangeEvent),
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
  public static void textValueLink(final JTextComponent textComponent, final Value<String> modelValue, final LinkType linkType,
                                   final Format format, final boolean immediateUpdate) {
    setEditableDefault(textComponent, linkType);
    valueLink(modelValue, new TextUIValue<String>(textComponent, format, immediateUpdate), linkType);
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
    toggleValueLink(buttonModel, new BeanModelValue<Boolean>(owner, beanPropertyName, boolean.class, linkType, valueChangeEvent),
            linkType);
  }

  /**
   * @param buttonModel the button model to link with the value
   * @param modelValue the model value
   * @param linkType the link type
   */
  public static void toggleValueLink(final ButtonModel buttonModel, final Value<Boolean> modelValue, final LinkType linkType) {
    valueLink(modelValue, new ToggleUIValue(buttonModel), linkType);
  }

  public static ToggleControl toggleControl(final Object owner, final String beanPropertyName, final String caption) {
    return toggleControl(owner, beanPropertyName, caption, null);
  }

  public static ToggleControl toggleControl(final Object owner, final String beanPropertyName, final String caption,
                                            final EventObserver changeEvent) {
    return toggleControl(owner, beanPropertyName, caption, changeEvent, null);
  }

  public static ToggleControl toggleControl(final Object owner, final String beanPropertyName, final String caption,
                                            final EventObserver changeEvent, final StateObserver enabledObserver) {
    final ButtonModel buttonModel = new JToggleButton.ToggleButtonModel();
    toggleValueLink(buttonModel, owner, beanPropertyName, changeEvent, LinkType.READ_WRITE);

    return new ToggleControl(caption, buttonModel, enabledObserver);
  }

  /**
   * @param buttonModel the button model
   * @param modelValue the model value
   * @param linkType the link type
   */
  public static void tristateValueLink(final TristateButtonModel buttonModel, final Value<Boolean> modelValue,
                                       final LinkType linkType) {
    valueLink(modelValue, new TristateUIValue(buttonModel), linkType);
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
    selectedItemValueLink(box, new BeanModelValue<Object>(owner, beanPropertyName, valueClass, linkType, valueChangeEvent), linkType);
  }

  /**
   * @param box the combo box to link with the value
   * @param modelValue the model value
   * @param linkType the link type
   */
  public static void selectedItemValueLink(final JComboBox box, final Value<Object> modelValue, final LinkType linkType) {
    valueLink(modelValue, new SelectedItemUIValue(box), linkType);
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
    valueLink(new BeanModelValue<Integer>(owner, beanPropertyName, int.class, linkType, valueChangeEvent), new IntSpinnerUIValue(spinnerModel), linkType);
  }

  /**
   * Links the two values together
   * @param modelValue the model value
   * @param uiValue the ui value
   * @param linkType the link type
   * @param <V> the value type
   */
  public static <V> void valueLink(final Value<V> modelValue, final Value<V> uiValue, final LinkType linkType) {
    new ValueLink<V>(modelValue, uiValue, linkType);
  }

  private static void setEditableDefault(final JTextComponent component, final LinkType linkType) {
    if (linkType == LinkType.READ_ONLY) {
      component.setEditable(false);
    }
  }

  /**
   * An abstract base class for linking a UI component to a model value.
   * @param <V> the type of the value
   */
  private static final class ValueLink<V> {

    /**
     * The Object wrapping the model value
     */
    private final Value<V> modelValue;

    /**
     * The Object wrapping the ui value
     */
    private final Value<V> uiValue;

    /**
     * The link type
     */
    private final LinkType linkType;

    /**
     * True while the UI value is being updated
     */
    private boolean isUpdatingUI = false;

    /**
     * True while the model value is being updated
     */
    private boolean isUpdatingModel = false;

    /**
     * Instantiates a new ValueLink
     * @param modelValue the value wrapper for the linked value
     * @param linkType the link Type
     */
    private ValueLink(final Value<V> modelValue, final Value<V> uiValue, final LinkType linkType) {
      this.modelValue = Util.rejectNullValue(modelValue, "modelValue");
      this.uiValue = Util.rejectNullValue(uiValue, "uiValue");
      this.linkType = Util.rejectNullValue(linkType, "linkType");
      updateUI();
      bindEvents(modelValue, uiValue, linkType);
    }

    /**
     * Updates the model according to the UI.
     */
    private void updateModel() {
      if (linkType != LinkType.READ_ONLY && !isUpdatingModel && !isUpdatingUI) {
        try {
          isUpdatingModel = true;
          modelValue.set(uiValue.get());
        }
        finally {
          isUpdatingModel = false;
        }
      }
    }

    /**
     * Updates the UI according to the model.
     */
    private void updateUI() {
      if (linkType != LinkType.WRITE_ONLY && !isUpdatingModel) {
        try {
          isUpdatingUI = true;
          if (!SwingUtilities.isEventDispatchThread()) {
            try {
              SwingUtilities.invokeAndWait(new Runnable() {
                /** {@inheritDoc} */
                @Override
                public void run() {
                  uiValue.set(modelValue.get());
                }
              });
            }
            catch (Exception e) {
              throw new RuntimeException(e);
            }
          }
          else {
            uiValue.set(modelValue.get());
          }
        }
        finally {
          isUpdatingUI = false;
        }
      }
    }

    private void bindEvents(final Value<V> modelValue, final Value<V> uiValue, final LinkType linkType) {
      if (linkType != LinkType.WRITE_ONLY && modelValue.getChangeEvent() != null) {
        modelValue.getChangeEvent().addListener(new EventAdapter() {
          /** {@inheritDoc} */
          @Override
          public void eventOccurred() {
            updateUI();
          }
        });
      }
      if (linkType != LinkType.READ_ONLY && uiValue.getChangeEvent() != null) {
        uiValue.getChangeEvent().addListener(new EventAdapter() {
          /** {@inheritDoc} */
          @Override
          public void eventOccurred() {
            updateModel();
          }
        });
      }
    }
  }

  private static final class BeanModelValue<V> implements Value<V> {

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
    public V get() {
      try {
        return (V) getMethod.invoke(valueOwner);
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
    public void set(final V value) {
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

  private abstract static class UIValue<V> implements Value<V> {
    protected final Event changeEvent = Events.event();

    /** {@inheritDoc} */
    @Override
    public final EventObserver getChangeEvent() {
      return changeEvent.getObserver();
    }
  }

  private static class TextUIValue<V> extends UIValue<V> {
    private final JTextComponent textComponent;
    private final JFormattedTextField.AbstractFormatter formatter;
    private final Format format;

    private TextUIValue(final JTextComponent textComponent, final Format format, final boolean immediateUpdate) {
      this.textComponent = textComponent;
      if (textComponent instanceof JFormattedTextField) {
        this.formatter = ((JFormattedTextField) textComponent).getFormatter();
      }
      else {
        this.formatter = null;
      }
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
    public void set(final V value) {
      try {
        final String text = textFromValue(value);
        synchronized (textComponent) {
          final Document document = textComponent.getDocument();
          document.remove(0, document.getLength());
          if (value != null) {
            document.insertString(0, text, null);
          }
        }
      }
      catch (BadLocationException e) {
        throw new RuntimeException(e);
      }
    }

    /** {@inheritDoc} */
    @Override
    public V get() {
      return valueFromText(getText());
    }

    /**
     * Returns a String representation of the given value object, using the format,
     * an empty string is returned in case of a null value
     * @param value the value to return as String
     * @return a formatted String representation of the given value, an empty string if the value is null
     */
    protected String textFromValue(final V value) {
      return value == null ? "" : format.format(value);
    }

    /**
     * Returns a property value based on the given text, if the text can not
     * be parsed into a valid value, null is returned
     * @param text the text from which to parse a value
     * @return a value from the given text, or null if the parsing did not yield a valid value
     */
    protected V valueFromText(final String text) {
      if (Util.nullOrEmpty(text)) {
        return null;
      }

      try {
        return (V) format.parseObject(text);
      }
      catch (ParseException e) {
        return null;
      }
    }

    /**
     * @return the text from the linked text component
     */
    protected final String getText() {
      try {
        final String text = textComponent.getDocument().getText(0, textComponent.getDocument().getLength());
        if (formatter == null) {
          return text;
        }
        try {
          return (String) formatter.stringToValue(text);
        }
        catch (ParseException e) {
          return null;
        }
      }
      catch (BadLocationException e) {
        throw new RuntimeException(e);
      }
    }

    protected Format getFormat() {
      return format;
    }
  }

  private static final class IntUIValue extends TextUIValue<Integer> {
    private final boolean usePrimitive;

    private IntUIValue(final JTextComponent textComponent, final NumberFormat format, final boolean usePrimitive) {
      super(textComponent, format, true);
      this.usePrimitive = usePrimitive;
    }

    /** {@inheritDoc} */
    @Override
    protected Integer valueFromText(final String text) {
      if (text.isEmpty() && usePrimitive) {
        return 0;
      }

      return Util.getInt(text);
    }
  }

  private static final class DoubleUIValue extends TextUIValue<Double> {
    private final boolean usePrimitive;

    private DoubleUIValue(final JTextComponent textComponent, final NumberFormat format, final boolean usePrimitive) {
      super(textComponent, format, true);
      this.usePrimitive = usePrimitive;
    }

    /** {@inheritDoc} */
    @Override
    protected Double valueFromText(final String text) {
      if (text.isEmpty() && usePrimitive) {
        return 0d;
      }

      return Util.getDouble(text);
    }
  }

  private static final class DateUIValue extends TextUIValue<Date> {
    private final boolean isTimestamp;

    private DateUIValue(final JFormattedTextField textComponent, final Format format, final boolean isTimestamp) {
      super(textComponent, format, true);
      Util.rejectNullValue(format, "format");
      this.isTimestamp = isTimestamp;
    }

    @Override
    protected Date valueFromText(final String text) {
      final Date parsedValue = super.valueFromText(text);
      return parsedValue == null ? null : isTimestamp ? new Timestamp(parsedValue.getTime()) : new Date(parsedValue.getTime());
    }
  }

  private static class ToggleUIValue extends UIValue<Boolean> {
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
    public void set(final Boolean value) {
      buttonModel.setSelected(value != null && value);
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
    public void set(final Boolean value) {
      if (value == null) {
        ((TristateButtonModel) getButtonModel()).setIndeterminate();
      }
      else {
        getButtonModel().setSelected(value);
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

  private static final class SelectedItemUIValue extends UIValue<Object> {
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
        ((JTextField) comboBox.getEditor().getEditorComponent()).getDocument().addDocumentListener(new DocumentListener(comboBox));
      }
    }

    /** {@inheritDoc} */
    @Override
    public void set(final Object value) {
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

    private static final class DocumentListener extends DocumentAdapter {
      private final JComboBox comboBox;

      private DocumentListener(final JComboBox comboBox) {
        this.comboBox = comboBox;
      }

      /** {@inheritDoc} */
      @Override
      public void contentsChanged(final DocumentEvent e) {
        comboBox.getModel().setSelectedItem(comboBox.getEditor().getItem());
      }
    }
  }

  private static final class IntSpinnerUIValue extends UIValue<Integer> {
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
    public void set(final Integer value) {
      spinnerModel.setValue(value);
    }

    /** {@inheritDoc} */
    @Override
    public Integer get() {
      return (Integer) spinnerModel.getValue();
    }
  }
}
