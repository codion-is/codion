/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui;

import org.jminor.common.model.DocumentAdapter;
import org.jminor.common.model.Event;
import org.jminor.common.model.EventObserver;
import org.jminor.common.model.Events;
import org.jminor.common.model.Item;
import org.jminor.common.model.Util;
import org.jminor.common.model.Value;
import org.jminor.common.model.checkbox.TristateButtonModel;
import org.jminor.common.model.combobox.FilteredComboBoxModel;
import org.jminor.common.model.combobox.ItemComboBoxModel;
import org.jminor.common.ui.textfield.DoubleField;
import org.jminor.common.ui.textfield.IntField;
import org.jminor.common.ui.textfield.LongField;

import javax.swing.ButtonModel;
import javax.swing.ComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
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
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.text.DateFormat;
import java.text.Format;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Date;

/**
 * A factory class for Value instances based on UI components
 */
public final class UiValues {

  private UiValues() {}

  /**
   * Note that when using Types.TIME the date fields (year, month and day of month) are
   * set to 1970, january and 1 respectively
   * @param textComponent the component
   * @param dateFormat the date format
   * @param sqlType the actual sql type (Types.DATE, Types.TIMESTAMP or Types.TIME)
   * @param immediateUpdate if true then the value is updated on each keystroke, otherwise on focus lost
   * @return a Value bound to the given component
   */
  public static Value<Date> dateValue(final JFormattedTextField textComponent, final DateFormat dateFormat,
                                      final int sqlType, final boolean immediateUpdate) {
    return new DateUIValue(textComponent, dateFormat, sqlType, immediateUpdate);
  }

  /**
   * @param intField the component
   * @param usePrimitive if true then the int primitive is used, Integer otherwise
   * @param format the number format
   * @param immediateUpdate if true then the value is updated on each keystroke, otherwise on focus lost
   * @return a Value bound to the given component
   */
  public static Value<Integer> integerValue(final IntField intField, final boolean usePrimitive,
                                            final NumberFormat format, final boolean immediateUpdate) {
    return new IntUIValue(intField, format, usePrimitive, immediateUpdate);
  }

  /**
   * @param spinnerModel the spinner model
   * @return a Value bound to the given component
   */
  public static Value<Integer> integerValue(final SpinnerNumberModel spinnerModel) {
    return new IntSpinnerUIValue(spinnerModel);
  }

  /**
   * @param doubleField the component
   * @param usePrimitive if true then the double primitive is used, Double otherwise
   * @param format the number format
   * @param immediateUpdate if true then the value is updated on each keystroke, otherwise on focus lost
   * @return a Value bound to the given component
   */
  public static Value<Double> doubleValue(final DoubleField doubleField, final boolean usePrimitive,
                                          final NumberFormat format, final boolean immediateUpdate) {
    return new DoubleUIValue(doubleField, format, usePrimitive, immediateUpdate);
  }

  /**
   * @param longField the component
   * @param usePrimitive if true then the double primitive is used, Long otherwise
   * @param format the number format
   * @param immediateUpdate if true then the value is updated on each keystroke, otherwise on focus lost
   * @return a Value bound to the given component
   */
  public static Value<Long> longValue(final LongField longField, final boolean usePrimitive,
                                      final NumberFormat format, final boolean immediateUpdate) {
    return new LongUIValue(longField, format, usePrimitive, immediateUpdate);
  }

  /**
   * @param textComponent the component
   * @param format the format
   * @param immediateUpdate if true then the value is updated on each keystroke, otherwise on focus lost
   * @return a Value bound to the given component
   */
  public static Value<String> textValue(final JTextComponent textComponent, final Format format, final boolean immediateUpdate) {
    return new TextUIValue<>(textComponent, format, immediateUpdate);
  }

  /**
   * @param buttonModel the button model
   * @return a Value bound to the given component
   */
  public static Value<Boolean> booleanValue(final ButtonModel buttonModel) {
    return new ToggleUIValue(buttonModel);
  }

  /**
   * @param <V> the value type
   * @param box the combo box
   * @return a Value bound to the given component
   */
  public static <V> Value<V> selectedItemValue(final JComboBox<V> box) {
    return new SelectedItemUIValue<>(box);
  }

  private abstract static class UIValue<V> implements Value<V> {
    private final Event<V> changeEvent = Events.event();

    @Override
    public final EventObserver<V> getObserver() {
      return changeEvent.getObserver();
    }

    @Override
    public final void set(final V value) {
      if (SwingUtilities.isEventDispatchThread()) {
        setInternal(value);
      }
      else {
        try {
          SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
              setInternal(value);
            }
          });
        }
        catch (final Exception e) {
          throw new RuntimeException(e);
        }
      }
    }

    protected final void fireChangeEvent() {
      changeEvent.fire();
    }

    protected abstract void setInternal(final V value);
  }

  private static class TextUIValue<V> extends UIValue<V> {
    private final Document document;
    private final JFormattedTextField.AbstractFormatter formatter;
    private final Format format;

    private TextUIValue(final JTextComponent textComponent, final Format format, final boolean immediateUpdate) {
      this.document = textComponent.getDocument();
      if (textComponent instanceof JFormattedTextField) {
        this.formatter = ((JFormattedTextField) textComponent).getFormatter();
      }
      else {
        this.formatter = null;
      }
      this.format = format == null ? Util.NULL_FORMAT : format;
      if (immediateUpdate) {
        document.addDocumentListener(new DocumentAdapter() {
          @Override
          public final void contentsChanged(final DocumentEvent e) {
            fireChangeEvent();
          }
        });
      }
      else {
        textComponent.addFocusListener(new FocusAdapter() {
          @Override
          public void focusLost(final FocusEvent e) {
            if (!e.isTemporary()) {
              fireChangeEvent();
            }
          }
        });
      }
    }

    @Override
    protected void setInternal(final V value) {
      try {
        final String text = textFromValue(value);
        synchronized (document) {
          document.remove(0, document.getLength());
          if (value != null) {
            document.insertString(0, text, null);
          }
        }
      }
      catch (final BadLocationException e) {
        throw new RuntimeException(e);
      }
    }

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
      catch (final ParseException e) {
        return null;
      }
    }

    /**
     * @return the text from the linked text component
     */
    protected final String getText() {
      try {
        final String text;
        synchronized (document) {
          text = document.getText(0, document.getLength());
        }
        if (formatter == null) {
          return text;
        }

        return formatText(text);
      }
      catch (final BadLocationException e) {
        throw new RuntimeException(e);
      }
    }

    protected Format getFormat() {
      return format;
    }

    private String formatText(final String text) {
      try {
        return (String) formatter.stringToValue(text);
      }
      catch (final ParseException e) {
        return null;
      }
    }
  }

  private static final class IntUIValue extends TextUIValue<Integer> {
    private final boolean usePrimitive;

    private IntUIValue(final JTextComponent textComponent, final NumberFormat format, final boolean usePrimitive,
                       final boolean immediateUpdate) {
      super(textComponent, format, immediateUpdate);
      this.usePrimitive = usePrimitive;
    }

    @Override
    protected Integer valueFromText(final String text) {
      if (text.length() == 0 && usePrimitive) {
        return 0;
      }

      return Util.getInt(text);
    }
  }

  private static final class DoubleUIValue extends TextUIValue<Double> {
    private final boolean usePrimitive;

    private DoubleUIValue(final JTextComponent textComponent, final NumberFormat format, final boolean usePrimitive,
                          final boolean immediateUpdate) {
      super(textComponent, format, immediateUpdate);
      this.usePrimitive = usePrimitive;
    }

    @Override
    protected Double valueFromText(final String text) {
      if (text.length() == 0 && usePrimitive) {
        return 0d;
      }

      return Util.getDouble(text);
    }
  }

  private static final class LongUIValue extends TextUIValue<Long> {
    private final boolean usePrimitive;

    private LongUIValue(final JTextComponent textComponent, final NumberFormat format, final boolean usePrimitive,
                        final boolean immediateUpdate) {
      super(textComponent, format, immediateUpdate);
      this.usePrimitive = usePrimitive;
    }

    @Override
    protected Long valueFromText(final String text) {
      if (text.length() == 0 && usePrimitive) {
        return 0l;
      }

      return Util.getLong(text);
    }
  }

  private static final class DateUIValue extends TextUIValue<Date> {
    private final int sqlType;

    private DateUIValue(final JFormattedTextField textComponent, final Format format, final int sqlType,
                        final boolean immediateUpdate) {
      super(textComponent, Util.rejectNullValue(format, "format"), immediateUpdate);
      if (sqlType != Types.DATE && sqlType != Types.TIMESTAMP && sqlType != Types.TIME) {
        throw new IllegalArgumentException("DateUIValue only applicable to: Types.DATE, Types.TIMESTAMP and Types.TIME");
      }
      this.sqlType = sqlType;
    }

    @Override
    protected Date valueFromText(final String text) {
      final Date parsedValue = super.valueFromText(text);
      if (parsedValue == null) {
        return null;
      }
      switch (sqlType) {
        case Types.DATE: return parsedValue;
        case Types.TIMESTAMP : return new Timestamp(parsedValue.getTime());
        case Types.TIME : return new Time(parsedValue.getTime());
        default: throw new IllegalStateException("Illegal sql type for DateUIValue: " + sqlType);
      }
    }
  }

  private static final class ToggleUIValue extends UIValue<Boolean> {
    private final ButtonModel buttonModel;

    private ToggleUIValue(final ButtonModel buttonModel) {
      this.buttonModel = buttonModel;
      buttonModel.addItemListener(new ItemListener() {
        @Override
        public void itemStateChanged(final ItemEvent e) {
          fireChangeEvent();
        }
      });
    }

    @Override
    public Boolean get() {
      if (buttonModel instanceof TristateButtonModel && ((TristateButtonModel) buttonModel).isIndeterminate()) {
        return null;
      }

      return buttonModel.isSelected();
    }

    @Override
    protected void setInternal(final Boolean value) {
      buttonModel.setSelected(value != null && value);
      if (buttonModel instanceof TristateButtonModel && value == null) {
        ((TristateButtonModel) getButtonModel()).setIndeterminate();
      }
    }

    private ButtonModel getButtonModel() {
      return buttonModel;
    }
  }

  private static final class SelectedItemUIValue<V> extends UIValue<V> {
    private final JComboBox<V> comboBox;

    private SelectedItemUIValue(final JComboBox<V> comboBox) {
      this.comboBox = comboBox;
      comboBox.addItemListener(new ItemListener() {
        @Override
        public void itemStateChanged(final ItemEvent e) {
          if (e.getStateChange() == ItemEvent.SELECTED) {
            fireChangeEvent();
          }
        }
      });
    }

    @Override
    public V get() {
      final ComboBoxModel<V> comboBoxModel = comboBox.getModel();
      if (comboBoxModel instanceof ItemComboBoxModel) {
        return (V) ((Item) comboBoxModel.getSelectedItem()).getItem();
      }
      else if (comboBoxModel instanceof FilteredComboBoxModel) {
        return (V) ((FilteredComboBoxModel) comboBoxModel).getSelectedValue();
      }

      return (V) comboBoxModel.getSelectedItem();
    }

    @Override
    protected void setInternal(final Object value) {
      comboBox.getModel().setSelectedItem(value);
    }
  }

  private static final class IntSpinnerUIValue extends UIValue<Integer> {
    private final SpinnerNumberModel spinnerModel;

    private IntSpinnerUIValue(final SpinnerNumberModel spinnerModel) {
      this.spinnerModel = spinnerModel;
      this.spinnerModel.addChangeListener(new ChangeListener() {
        @Override
        public void stateChanged(final ChangeEvent e) {
          fireChangeEvent();
        }
      });
    }

    @Override
    public Integer get() {
      return (Integer) spinnerModel.getValue();
    }

    @Override
    protected void setInternal(final Integer value) {
      spinnerModel.setValue(value);
    }
  }
}
