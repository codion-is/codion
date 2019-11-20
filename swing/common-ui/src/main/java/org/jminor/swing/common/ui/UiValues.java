/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui;

import org.jminor.common.DateFormats;
import org.jminor.common.Formats;
import org.jminor.common.Item;
import org.jminor.common.model.combobox.FilteredComboBoxModel;
import org.jminor.common.value.AbstractValue;
import org.jminor.common.value.Value;
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

import static org.jminor.common.Util.nullOrEmpty;

/**
 * A factory class for Value instances based on UI components
 */
public final class UiValues {

  private UiValues() {}

  /**
   * @param textComponent the component
   * @param dateFormat the date format
   * @return a Value bound to the given component
   */
  public static Value<LocalDate> localDateValue(final JFormattedTextField textComponent, final String dateFormat) {
    return localDateValue(textComponent, dateFormat, true);
  }

  /**
   * @param textComponent the component
   * @param dateFormat the date format
   * @param updateOnKeystroke if true then the value is updated on each keystroke, otherwise on focus lost
   * @return a Value bound to the given component
   */
  public static Value<LocalDate> localDateValue(final JFormattedTextField textComponent,
                                                final String dateFormat, final boolean updateOnKeystroke) {
    return new TemporalUiValue<>(textComponent, dateFormat, updateOnKeystroke, LocalDate::parse);
  }

  /**
   * @param textComponent the component
   * @param dateFormat the date format
   * @return a Value bound to the given component
   */
  public static Value<LocalTime> localTimeValue(final JFormattedTextField textComponent, final String dateFormat) {
    return localTimeValue(textComponent, dateFormat, true);
  }

  /**
   * @param textComponent the component
   * @param dateFormat the date format
   * @param updateOnKeystroke if true then the value is updated on each keystroke, otherwise on focus lost
   * @return a Value bound to the given component
   */
  public static Value<LocalTime> localTimeValue(final JFormattedTextField textComponent,
                                                final String dateFormat, final boolean updateOnKeystroke) {
    return new TemporalUiValue<>(textComponent, dateFormat, updateOnKeystroke, LocalTime::parse);
  }

  /**
   * @param textComponent the component
   * @param dateFormat the date format
   * @return a Value bound to the given component
   */
  public static Value<LocalDateTime> localDateTimeValue(final JFormattedTextField textComponent, final String dateFormat) {
    return localDateTimeValue(textComponent, dateFormat, true);
  }

  /**
   * @param textComponent the component
   * @param dateFormat the date format
   * @param updateOnKeystroke if true then the value is updated on each keystroke, otherwise on focus lost
   * @return a Value bound to the given component
   */
  public static Value<LocalDateTime> localDateTimeValue(final JFormattedTextField textComponent, final String dateFormat,
                                                        final boolean updateOnKeystroke) {
    return new TemporalUiValue<>(textComponent, dateFormat, updateOnKeystroke, LocalDateTime::parse);
  }

  /**
   * @param integerField the component
   * @param nullable if false then the resulting Value returns 0 instead of null
   * @return a Value bound to the given component
   */
  public static Value<Integer> integerValue(final IntegerField integerField, final boolean nullable) {
    return integerValue(integerField, nullable, true);
  }

  /**
   * @param integerField the component
   * @param nullable if false then the resulting Value returns 0 instead of null
   * @param updateOnKeystroke if true then the value is updated on each keystroke, otherwise on focus lost
   * @return a Value bound to the given component
   */
  public static Value<Integer> integerValue(final IntegerField integerField, final boolean nullable,
                                            final boolean updateOnKeystroke) {
    return new IntUIValue(integerField, nullable, updateOnKeystroke);
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
   * @param nullable if false then the resulting Value returns 0 instead of null
   * @return a Value bound to the given component
   */
  public static Value<Double> doubleValue(final DecimalField decimalField, final boolean nullable) {
    return doubleValue(decimalField, nullable, true);
  }

  /**
   * @param decimalField the component
   * @param nullable if false then the resulting Value returns 0 instead of null
   * @param updateOnKeystroke if true then the value is updated on each keystroke, otherwise on focus lost
   * @return a Value bound to the given component
   */
  public static Value<Double> doubleValue(final DecimalField decimalField, final boolean nullable,
                                          final boolean updateOnKeystroke) {
    return new DoubleUIValue(decimalField, nullable, updateOnKeystroke);
  }

  /**
   * @param decimalField the component
   * @return a Value bound to the given component
   */
  public static Value<BigDecimal> bigDecimalValue(final DecimalField decimalField) {
    return bigDecimalValue(decimalField, true);
  }

  /**
   * @param decimalField the component
   * @param updateOnKeystroke if true then the value is updated on each keystroke, otherwise on focus lost
   * @return a Value bound to the given component
   */
  public static Value<BigDecimal> bigDecimalValue(final DecimalField decimalField, final boolean updateOnKeystroke) {
    return new BigDecimalUIValue(decimalField, updateOnKeystroke);
  }

  /**
   * @param longField the component
   * @param nullable if false then the resulting Value returns 0 instead of null
   * @return a Value bound to the given component
   */
  public static Value<Long> longValue(final LongField longField, final boolean nullable) {
    return longValue(longField, nullable, true);
  }

  /**
   * @param longField the component
   * @param nullable if false then the resulting Value returns 0 instead of null
   * @param updateOnKeystroke if true then the value is updated on each keystroke, otherwise on focus lost
   * @return a Value bound to the given component
   */
  public static Value<Long> longValue(final LongField longField, final boolean nullable,
                                      final boolean updateOnKeystroke) {
    return new LongUIValue(longField, nullable, updateOnKeystroke);
  }

  /**
   * @param textComponent the component
   * @return a Value bound to the given component
   */
  public static Value<String> textValue(final JTextComponent textComponent) {
    return textValue(textComponent, null);
  }

  /**
   * @param textComponent the component
   * @param format the format
   * @return a Value bound to the given component
   */
  public static Value<String> textValue(final JTextComponent textComponent, final Format format) {
    return textValue(textComponent, format, true);
  }

  /**
   * @param textComponent the component
   * @param format the format
   * @param updateOnKeystroke if true then the value is updated on each keystroke, otherwise on focus lost
   * @return a Value bound to the given component
   */
  public static Value<String> textValue(final JTextComponent textComponent, final Format format,
                                        final boolean updateOnKeystroke) {
    return new TextUIValue<>(textComponent, format, updateOnKeystroke);
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
   * @param comboBox the combo box
   * @return a Value bound to the given component
   */
  public static <V> Value<V> selectedItemValue(final JComboBox<V> comboBox) {
    return new SelectedItemUIValue<>(comboBox);
  }

  private abstract static class UIValue<V> extends AbstractValue<V> {

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

    protected abstract void setInternal(final V value);
  }

  private static class TextUIValue<V> extends UIValue<V> {
    private final Document document;
    private final JFormattedTextField.AbstractFormatter formatter;
    private final Format format;

    private TextUIValue(final JTextComponent textComponent, final Format format, final boolean updateOnKeystroke) {
      this.document = textComponent.getDocument();
      if (textComponent instanceof JFormattedTextField) {
        this.formatter = ((JFormattedTextField) textComponent).getFormatter();
      }
      else {
        this.formatter = null;
      }
      this.format = format == null ? Formats.NULL_FORMAT : format;
      if (updateOnKeystroke) {
        document.addDocumentListener(new DocumentAdapter() {
          @Override
          public final void contentsChanged(final DocumentEvent e) {
            fireChangeEvent(get());
          }
        });
      }
      else {
        textComponent.addFocusListener(new FocusAdapter() {
          @Override
          public void focusLost(final FocusEvent e) {
            if (!e.isTemporary()) {
              fireChangeEvent(get());
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
      if (nullOrEmpty(text)) {
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

  private abstract static class NumberUIValue<T extends Number> extends UIValue<T> {
    private final NumberField numberField;
    private final boolean nullable;

    private NumberUIValue(final NumberField numberField, final boolean nullable, final boolean updateOnKeystroke) {
      this.numberField = numberField;
      this.nullable = nullable;
      if (updateOnKeystroke) {
        numberField.getDocument().addDocumentListener(new DocumentAdapter() {
          @Override
          public final void contentsChanged(final DocumentEvent e) {
            fireChangeEvent(get());
          }
        });
      }
      else {
        numberField.addFocusListener(new FocusAdapter() {
          @Override
          public void focusLost(final FocusEvent e) {
            if (!e.isTemporary()) {
              fireChangeEvent(get());
            }
          }
        });
      }
    }

    @Override
    public final boolean isNullable() {
      return nullable;
    }

    protected void setNumber(final Number number) {
      numberField.setNumber(number);
    }

    protected final Number getNumber() {
      return numberField.getNumber();
    }
  }

  private static final class IntUIValue extends NumberUIValue<Integer> {

    private IntUIValue(final IntegerField integerField, final boolean nullable, final boolean updateOnKeystroke) {
      super(integerField, nullable, updateOnKeystroke);
    }

    @Override
    protected void setInternal(final Integer value) {
      setNumber(value);
    }

    @Override
    public Integer get() {
      final Number number = getNumber();
      if (number == null) {
        return isNullable() ? null : 0;
      }

      return number.intValue();
    }
  }

  private static final class DoubleUIValue extends NumberUIValue<Double> {

    private DoubleUIValue(final DecimalField decimalField, final boolean nullable, final boolean updateOnKeystroke) {
      super(decimalField, nullable, updateOnKeystroke);
    }

    @Override
    protected void setInternal(final Double value) {
      setNumber(value);
    }

    @Override
    public Double get() {
      final Number number = getNumber();
      if (number == null) {
        return isNullable() ? null : 0d;
      }

      return number.doubleValue();
    }
  }

  private static final class BigDecimalUIValue extends NumberUIValue<BigDecimal> {

    private BigDecimalUIValue(final DecimalField decimalField, final boolean updateOnKeystroke) {
      super(decimalField, true, updateOnKeystroke);
    }

    @Override
    protected void setInternal(final BigDecimal value) {
      setNumber(value);
    }

    @Override
    public BigDecimal get() {
      return (BigDecimal) getNumber();
    }
  }

  private static final class LongUIValue extends NumberUIValue<Long> {

    private LongUIValue(final LongField longField, final boolean nullable, final boolean updateOnKeystroke) {
      super(longField, nullable, updateOnKeystroke);
    }

    @Override
    protected void setInternal(final Long value) {
      setNumber(value);
    }

    @Override
    public Long get() {
      final Number number = getNumber();
      if (number == null) {
        return isNullable() ? null : 0L;
      }

      return number.longValue();
    }
  }

  private static final class TemporalUiValue<T extends Temporal> extends TextUIValue<T> {
    private final DateTimeFormatter formatter;
    private final DateFormats.DateParser<T> dateParser;

    private TemporalUiValue(final JFormattedTextField textComponent, final String dateFormat,
                            final boolean updateOnKeystroke, final DateFormats.DateParser<T> dateParser) {
      super(textComponent, null, updateOnKeystroke);
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
      if (nullOrEmpty(text)) {
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
      buttonModel.addItemListener(e -> fireChangeEvent(get()));
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
          fireChangeEvent(get());
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
      this.spinnerModel.addChangeListener(e -> fireChangeEvent(get()));
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
      this.rangeModel.addChangeListener(e -> fireChangeEvent(get()));
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
