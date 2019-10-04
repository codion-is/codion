/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui;

import org.jminor.common.DateFormats;
import org.jminor.common.Event;
import org.jminor.common.EventObserver;
import org.jminor.common.Events;
import org.jminor.common.FormatUtil;
import org.jminor.common.Item;
import org.jminor.common.Util;
import org.jminor.common.Value;
import org.jminor.common.ValueObserver;
import org.jminor.common.Values;
import org.jminor.common.model.combobox.FilteredComboBoxModel;
import org.jminor.swing.common.model.DocumentAdapter;
import org.jminor.swing.common.model.checkbox.TristateButtonModel;
import org.jminor.swing.common.model.combobox.ItemComboBoxModel;
import org.jminor.swing.common.ui.textfield.DecimalField;
import org.jminor.swing.common.ui.textfield.IntegerField;
import org.jminor.swing.common.ui.textfield.LongField;
import org.jminor.swing.common.ui.textfield.NumberField;

import javax.swing.BoundedRangeModel;
import javax.swing.ButtonModel;
import javax.swing.ComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.ItemEvent;
import java.math.BigDecimal;
import java.text.Format;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.Temporal;

/**
 * A factory class for Value instances based on UI components
 */
public final class UiValues {

  private UiValues() {}

  /**
   * @param textComponent the component
   * @param dateFormat the date format
   * @param immediateUpdate if true then the value is updated on each keystroke, otherwise on focus lost
   * @return a Value bound to the given component
   */
  public static Value<LocalDate> localDateValue(final JFormattedTextField textComponent, final String dateFormat,
                                                final boolean immediateUpdate) {
    return new TemporalUiValue<>(textComponent, dateFormat, immediateUpdate, LocalDate::parse);
  }

  /**
   * @param textComponent the component
   * @param dateFormat the date format
   * @param immediateUpdate if true then the value is updated on each keystroke, otherwise on focus lost
   * @return a Value bound to the given component
   */
  public static Value<LocalTime> localTimeValue(final JFormattedTextField textComponent, final String dateFormat,
                                                final boolean immediateUpdate) {
    return new TemporalUiValue<>(textComponent, dateFormat, immediateUpdate, LocalTime::parse);
  }

  /**
   * @param textComponent the component
   * @param dateFormat the date format
   * @param immediateUpdate if true then the value is updated on each keystroke, otherwise on focus lost
   * @return a Value bound to the given component
   */
  public static Value<LocalDateTime> localDateTimeValue(final JFormattedTextField textComponent, final String dateFormat,
                                                        final boolean immediateUpdate) {
    return new TemporalUiValue<>(textComponent, dateFormat, immediateUpdate, LocalDateTime::parse);
  }

  /**
   * @param integerField the component
   * @param usePrimitive if true then the int primitive is used, Integer otherwise
   * @param immediateUpdate if true then the value is updated on each keystroke, otherwise on focus lost
   * @return a Value bound to the given component
   */
  public static Value<Integer> integerValue(final IntegerField integerField, final boolean usePrimitive, final boolean immediateUpdate) {
    return new IntUIValue(integerField, usePrimitive, immediateUpdate);
  }

  /**
   * @param spinnerModel the spinner model
   * @return a Value bound to the given model
   */
  public static Value<Integer> integerValue(final SpinnerNumberModel spinnerModel) {
    return new SpinnerUIValue(spinnerModel);
  }

  /**
   * @param boundedRangeModel the bounded range model
   * @return a Value bound to the given model
   */
  public static Value<Integer> integerValue(final BoundedRangeModel boundedRangeModel) {
    return new BoundedRangeUIValue(boundedRangeModel);
  }

  /**
   * @param spinnerModel the spinner model
   * @return a Value bound to the given component
   */
  public static Value<Double> doubleValue(final SpinnerNumberModel spinnerModel) {
    return new SpinnerUIValue(spinnerModel);
  }

  /**
   * @param decimalField the component
   * @param usePrimitive if true then the double primitive is used, Double otherwise
   * @param immediateUpdate if true then the value is updated on each keystroke, otherwise on focus lost
   * @return a Value bound to the given component
   */
  public static Value<Double> doubleValue(final DecimalField decimalField, final boolean usePrimitive, final boolean immediateUpdate) {
    return new DoubleUIValue(decimalField, usePrimitive, immediateUpdate);
  }

  /**
   * @param decimalField the component
   * @param immediateUpdate if true then the value is updated on each keystroke, otherwise on focus lost
   * @return a Value bound to the given component
   */
  public static Value<BigDecimal> bigDecimalValue(final DecimalField decimalField, final boolean immediateUpdate) {
    return new BigDecimalUIValue(decimalField, immediateUpdate);
  }

  /**
   * @param longField the component
   * @param usePrimitive if true then the double primitive is used, Long otherwise
   * @param immediateUpdate if true then the value is updated on each keystroke, otherwise on focus lost
   * @return a Value bound to the given component
   */
  public static Value<Long> longValue(final LongField longField, final boolean usePrimitive, final boolean immediateUpdate) {
    return new LongUIValue(longField, usePrimitive, immediateUpdate);
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
    public final EventObserver<V> getChangeObserver() {
      return changeEvent.getObserver();
    }

    @Override
    public ValueObserver<V> getValueObserver() {
      return Values.valueObserver(this);
    }

    @Override
    public final void set(final V value) {
      if (SwingUtilities.isEventDispatchThread()) {
        setInternal(value);
      }
      else {
        try {
          SwingUtilities.invokeAndWait(() -> setInternal(value));
        }
        catch(final InterruptedException ex){
          Thread.currentThread().interrupt();
          throw new RuntimeException(ex);
        }
        catch (final Exception e) {
          throw new RuntimeException(e);
        }
      }
    }

    @Override
    public boolean isNullable() {
      return true;
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
      this.format = format == null ? FormatUtil.NULL_FORMAT : format;
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

    private String formatText(final String text) {
      try {
        return (String) formatter.stringToValue(text);
      }
      catch (final ParseException e) {
        return null;
      }
    }
  }

  private abstract static class NumberUIValue<T> extends UIValue<T> {
    private final NumberField textField;
    private final boolean usePrimitive;

    private NumberUIValue(final NumberField textField, final boolean usePrimitive, final boolean immediateUpdate) {
      this.textField = textField;
      this.usePrimitive = usePrimitive;
      if (immediateUpdate) {
        textField.getDocument().addDocumentListener(new DocumentAdapter() {
          @Override
          public final void contentsChanged(final DocumentEvent e) {
            fireChangeEvent();
          }
        });
      }
      else {
        textField.addFocusListener(new FocusAdapter() {
          @Override
          public void focusLost(final FocusEvent e) {
            if (!e.isTemporary()) {
              fireChangeEvent();
            }
          }
        });
      }
    }

    protected final boolean isUsePrimitive() {
      return usePrimitive;
    }

    protected final NumberField getTextField() {
      return textField;
    }

    protected Number getNumber() {
      return textField.getNumber();
    }
  }

  private static final class IntUIValue extends NumberUIValue<Integer> {

    private IntUIValue(final IntegerField integerField, final boolean usePrimitive, final boolean immediateUpdate) {
      super(integerField, usePrimitive, immediateUpdate);
    }

    @Override
    protected void setInternal(final Integer value) {
      getTextField().setNumber(value);
    }

    @Override
    public Integer get() {
      final Number number = getNumber();
      if (number == null) {
        return isUsePrimitive() ? 0 : null;
      }

      return number.intValue();
    }
  }

  private static final class DoubleUIValue extends NumberUIValue<Double> {

    private DoubleUIValue(final DecimalField decimalField, final boolean usePrimitive, final boolean immediateUpdate) {
      super(decimalField, usePrimitive, immediateUpdate);
    }

    @Override
    protected void setInternal(final Double value) {
      getTextField().setNumber(value);
    }

    @Override
    public Double get() {
      final Number number = getNumber();
      if (number == null) {
        return isUsePrimitive() ? 0d : null;
      }

      return number.doubleValue();
    }
  }

  private static final class BigDecimalUIValue extends NumberUIValue<BigDecimal> {

    private BigDecimalUIValue(final DecimalField decimalField, final boolean immediateUpdate) {
      super(decimalField, false, immediateUpdate);
    }

    @Override
    protected void setInternal(final BigDecimal value) {
      getTextField().setNumber(value);
    }

    @Override
    public BigDecimal get() {
      return (BigDecimal) getNumber();
    }
  }

  private static final class LongUIValue extends NumberUIValue<Long> {

    private LongUIValue(final LongField longField, final boolean usePrimitive, final boolean immediateUpdate) {
      super(longField, usePrimitive, immediateUpdate);
    }

    @Override
    protected void setInternal(final Long value) {
      getTextField().setNumber(value);
    }

    @Override
    public Long get() {
      final Number number = getNumber();
      if (number == null) {
        return isUsePrimitive() ? 0L : null;
      }

      return number.longValue();
    }
  }

  private static final class TemporalUiValue<T extends Temporal> extends TextUIValue<T> {
    private final DateTimeFormatter formatter;
    private final DateFormats.DateParser<T> dateParser;

    private TemporalUiValue(final JFormattedTextField textComponent, final String dateFormat,
                            final boolean immediateUpdate, final DateFormats.DateParser<T> dateParser) {
      super(textComponent, null, immediateUpdate);
      this.formatter = DateTimeFormatter.ofPattern(dateFormat);
      this.dateParser = dateParser;
    }

    @Override
    protected String textFromValue(final T value) {
      if (value == null) {
        return null;
      }

      return formatter.format(value);
    }

    @Override
    protected T valueFromText(final String text) {
      if (Util.nullOrEmpty(text)) {
        return null;
      }

      try {
        return dateParser.parse(text, formatter);
      }
      catch (final DateTimeParseException e) {
        return null;
      }
    }
  }

  private static final class ToggleUIValue extends UIValue<Boolean> {
    private final ButtonModel buttonModel;

    private ToggleUIValue(final ButtonModel buttonModel) {
      this.buttonModel = buttonModel;
      buttonModel.addItemListener(e -> fireChangeEvent());
    }

    @Override
    public Boolean get() {
      if (buttonModel instanceof TristateButtonModel && ((TristateButtonModel) buttonModel).isIndeterminate()) {
        return null;
      }

      return buttonModel.isSelected();
    }

    @Override
    public boolean isNullable() {
      return buttonModel instanceof TristateButtonModel;
    }

    @Override
    protected void setInternal(final Boolean value) {
      if (value == null && buttonModel instanceof TristateButtonModel) {
        ((TristateButtonModel) buttonModel).setIndeterminate();
      }
      else {
        buttonModel.setSelected(value != null && value);
      }
    }
  }

  private static final class SelectedItemUIValue<V> extends UIValue<V> {
    private final JComboBox<V> comboBox;

    private SelectedItemUIValue(final JComboBox<V> comboBox) {
      this.comboBox = comboBox;
      comboBox.addItemListener(e -> {
        if (e.getStateChange() == ItemEvent.SELECTED) {
          fireChangeEvent();
        }
      });
    }

    @Override
    public V get() {
      final ComboBoxModel<V> comboBoxModel = comboBox.getModel();
      if (comboBoxModel instanceof ItemComboBoxModel) {
        return (V) ((Item) comboBoxModel.getSelectedItem()).getValue();
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

  private static final class SpinnerUIValue<T extends Number> extends UIValue<T> {
    private final SpinnerNumberModel spinnerModel;

    private SpinnerUIValue(final SpinnerNumberModel spinnerModel) {
      this.spinnerModel = spinnerModel;
      this.spinnerModel.addChangeListener(e -> fireChangeEvent());
    }

    @Override
    public T get() {
      return (T) spinnerModel.getValue();
    }

    @Override
    protected void setInternal(final T value) {
      spinnerModel.setValue(value);
    }
  }

  private static final class BoundedRangeUIValue extends UIValue<Integer> {
    private final BoundedRangeModel rangeModel;

    public BoundedRangeUIValue(final BoundedRangeModel rangeModel) {
      this.rangeModel = rangeModel;
      this.rangeModel.addChangeListener(e -> fireChangeEvent());
    }

    @Override
    public Integer get() {
      return rangeModel.getValue();
    }

    @Override
    protected void setInternal(final Integer value) {
      rangeModel.setValue(value == null ? 0 : value);
    }
  }
}
