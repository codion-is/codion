/*
 * Copyright (c) 2004 - 2013, Björn Darri Sigurðsson. All Rights Reserved.
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

import javax.swing.ButtonModel;
import javax.swing.ComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JTextField;
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
import java.sql.Timestamp;
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
   * @param textComponent the component
   * @param dateFormat the date format
   * @param isTimestamp if true then a Timestamp is used, Date otherwise
   * @return a Value bound to the given component
   */
  public static Value<Date> dateValue(final JFormattedTextField textComponent, final DateFormat dateFormat, final boolean isTimestamp) {
    return new DateUIValue(textComponent, dateFormat, isTimestamp);
  }

  /**
   * @param intField the component
   * @param usePrimitive if true then the int primitive is used, Integer otherwise
   * @param format the number format
   * @return a Value bound to the given component
   */
  public static Value<Integer> integerValue(final IntField intField, final boolean usePrimitive, final NumberFormat format) {
    return new IntUIValue(intField, format, usePrimitive);
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
   * @return a Value bound to the given component
   */
  public static Value<Double> doubleValue(final DoubleField doubleField, final boolean usePrimitive, final NumberFormat format) {
    return new DoubleUIValue(doubleField, format, usePrimitive);
  }

  /**
   * @param textComponent the component
   * @param format the format
   * @param immediateUpdate if true then the value is updated on each keystroke, otherwise on focus lost
   * @return a Value bound to the given component
   */
  public static Value<String> textValue(final JTextComponent textComponent, final Format format, final boolean immediateUpdate) {
    return new TextUIValue<String>(textComponent, format, immediateUpdate);
  }

  /**
   * @param buttonModel the button model
   * @return a Value bound to the given component
   */
  public static Value<Boolean> booleanValue(final ButtonModel buttonModel) {
    return new ToggleUIValue(buttonModel);
  }

  /**
   * @param buttonModel the button model
   * @return a Value bound to the given component
   */
  public static Value<Boolean> tristateValue(final TristateButtonModel buttonModel) {
    return new TristateUIValue(buttonModel);
  }

  /**
   * @param box the combo box
   * @return a Value bound to the given component
   */
  public static Value<Object> selectedItemValue(final JComboBox box) {
    return new SelectedItemUIValue(box);
  }

  private abstract static class UIValue<V> implements Value<V> {
    protected final Event changeEvent = Events.event();

    /** {@inheritDoc} */
    @Override
    public final EventObserver getChangeEvent() {
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
            /** {@inheritDoc} */
            @Override
            public void run() {
              setInternal(value);
            }
          });
        }
        catch (Exception e) {
          throw new RuntimeException(e);
        }
      }
    }

    protected abstract void setInternal(final V value);
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
    protected void setInternal(final V value) {
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
      super(textComponent, Util.rejectNullValue(format, "format"), true);
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
    public Boolean get() {
      return buttonModel.isSelected();
    }

    /** {@inheritDoc} */
    @Override
    protected void setInternal(final Boolean value) {
      buttonModel.setSelected(value != null && value);
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
    public Boolean get() {
      if (((TristateButtonModel) getButtonModel()).isIndeterminate()) {
        return null;
      }

      return getButtonModel().isSelected();
    }

    /** {@inheritDoc} */
    @Override
    protected void setInternal(final Boolean value) {
      if (value == null) {
        ((TristateButtonModel) getButtonModel()).setIndeterminate();
      }
      else {
        getButtonModel().setSelected(value);
      }
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

    /** {@inheritDoc} */
    @Override
    protected void setInternal(final Object value) {
      comboBox.getModel().setSelectedItem(value);
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
    public Integer get() {
      return (Integer) spinnerModel.getValue();
    }

    /** {@inheritDoc} */
    @Override
    protected void setInternal(final Integer value) {
      spinnerModel.setValue(value);
    }
  }
}
