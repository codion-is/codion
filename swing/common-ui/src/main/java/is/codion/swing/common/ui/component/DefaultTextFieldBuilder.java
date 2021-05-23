/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import is.codion.common.value.Value;
import is.codion.swing.common.ui.dialog.Dialogs;
import is.codion.swing.common.ui.textfield.BigDecimalField;
import is.codion.swing.common.ui.textfield.DoubleField;
import is.codion.swing.common.ui.textfield.IntegerField;
import is.codion.swing.common.ui.textfield.LongField;
import is.codion.swing.common.ui.textfield.SizedDocument;
import is.codion.swing.common.ui.textfield.TemporalField;
import is.codion.swing.common.ui.textfield.TextFields;
import is.codion.swing.common.ui.value.ComponentValues;

import javax.swing.Action;
import javax.swing.JTextField;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.Format;
import java.text.NumberFormat;
import java.time.temporal.Temporal;
import java.util.Collection;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

final class DefaultTextFieldBuilder<T> extends AbstractTextComponentBuilder<T, JTextField, TextFieldBuilder<T>>
        implements TextFieldBuilder<T> {

  private final Class<T> valueClass;

  private Action action;
  private boolean selectAllOnFocusGained;
  private Supplier<Collection<T>> valueSupplier;
  private Format format;
  private String dateTimePattern;
  private Double maximumValue;
  private Double minimumValue;

  DefaultTextFieldBuilder(final Value<T> value, final Class<T> valueClass) {
    super(value);
    this.valueClass = valueClass;
  }

  @Override
  public TextFieldBuilder<T> action(final Action action) {
    this.action = requireNonNull(action);

    return transferFocusOnEnter(false);
  }

  @Override
  public TextFieldBuilder<T> selectAllOnFocusGained() {
    this.selectAllOnFocusGained = true;
    return this;
  }

  @Override
  public TextFieldBuilder<T> lookupDialog(final Supplier<Collection<T>> valueSupplier) {
    this.valueSupplier = requireNonNull(valueSupplier);
    return this;
  }

  @Override
  public TextFieldBuilder<T> format(final Format format) {
    this.format = format;
    return this;
  }

  @Override
  public TextFieldBuilder<T> dateTimePattern(final String dateTimePattern) {
    this.dateTimePattern = dateTimePattern;
    return this;
  }

  @Override
  public TextFieldBuilder<T> minimumValue(final Double minimumValue) {
    this.minimumValue = minimumValue;
    return this;
  }

  @Override
  public TextFieldBuilder<T> maximumValue(final Double maximumValue) {
    this.maximumValue = maximumValue;
    return this;
  }

  @Override
  protected JTextField buildComponent() {
    final JTextField textField = createTextField();
    ComponentValues.textFieldValue(textField, valueClass, value, updateOn, format);
    textField.setEditable(editable);
    textField.setColumns(columns);
    if (action != null) {
      textField.setAction(action);
    }
    if (selectAllOnFocusGained) {
      TextFields.selectAllOnFocusGained(textField);
    }
    if (upperCase) {
      TextFields.upperCase(textField);
    }
    if (lowerCase) {
      TextFields.lowerCase(textField);
    }
    if (valueSupplier != null) {
      Dialogs.addLookupDialog(textField, valueSupplier);
    }

    return textField;
  }

  private JTextField createTextField() {
    if (valueClass.equals(Integer.class)) {
      return initializeIntegerField();
    }
    if (valueClass.equals(Double.class)) {
      return initializeDoubleField();
    }
    if (valueClass.equals(BigDecimal.class)) {
      return initializeBigDecimalField();
    }
    if (valueClass.equals(Long.class)) {
      return initializeLongField();
    }
    if (Temporal.class.isAssignableFrom(valueClass)) {
      return initializeTemporalField();
    }
    if (valueClass.equals(String.class)) {
      return initializeStringField();
    }
    if (valueClass.equals(Character.class)) {
      return new JTextField(new SizedDocument(1), "", 1);
    }

    throw new IllegalArgumentException("Creating text fields for type: " + valueClass + " is not supported");
  }

  private IntegerField initializeIntegerField() {
    final IntegerField field = new IntegerField(cloneFormat((NumberFormat) format));
    if (minimumValue != null && maximumValue != null) {
      field.setRange(minimumValue, maximumValue);
    }

    return field;
  }

  private DoubleField initializeDoubleField() {
    final DoubleField field = new DoubleField((DecimalFormat) cloneFormat((NumberFormat) format));
    if (minimumValue != null && maximumValue != null) {
      field.setRange(Math.min(minimumValue, 0), maximumValue);
    }

    return field;
  }

  private BigDecimalField initializeBigDecimalField() {
    final BigDecimalField field = new BigDecimalField((DecimalFormat) cloneFormat((NumberFormat) format));
    if (minimumValue != null && maximumValue != null) {
      field.setRange(Math.min(minimumValue, 0), maximumValue);
    }

    return field;
  }

  private LongField initializeLongField() {
    final LongField field = new LongField(cloneFormat((NumberFormat) format));
    if (minimumValue != null && maximumValue != null) {
      field.setRange(minimumValue, maximumValue);
    }

    return field;
  }

  private TemporalField<Temporal> initializeTemporalField() {
    return TemporalField.builder((Class<Temporal>) valueClass)
            .dateTimePattern(dateTimePattern)
            .build();
  }

  private JTextField initializeStringField() {
    final SizedDocument sizedDocument = new SizedDocument();
    if (maximumLength > 0) {
      sizedDocument.setMaximumLength(maximumLength);
    }

    return new JTextField(sizedDocument, "", 0);
  }

  private static NumberFormat cloneFormat(final NumberFormat format) {
    final NumberFormat cloned = (NumberFormat) format.clone();
    cloned.setGroupingUsed(format.isGroupingUsed());
    cloned.setMaximumIntegerDigits(format.getMaximumIntegerDigits());
    cloned.setMaximumFractionDigits(format.getMaximumFractionDigits());
    cloned.setMinimumFractionDigits(format.getMinimumFractionDigits());
    cloned.setRoundingMode(format.getRoundingMode());
    cloned.setCurrency(format.getCurrency());
    cloned.setParseIntegerOnly(format.isParseIntegerOnly());

    return cloned;
  }
}
