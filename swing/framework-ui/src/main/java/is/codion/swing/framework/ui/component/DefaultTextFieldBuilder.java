/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui.component;

import is.codion.common.value.Value;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.property.Property;
import is.codion.swing.common.ui.textfield.BigDecimalField;
import is.codion.swing.common.ui.textfield.DoubleField;
import is.codion.swing.common.ui.textfield.IntegerField;
import is.codion.swing.common.ui.textfield.LongField;
import is.codion.swing.common.ui.textfield.SizedDocument;
import is.codion.swing.common.ui.textfield.TemporalField;
import is.codion.swing.common.ui.textfield.TextFields;
import is.codion.swing.common.ui.value.ComponentValues;
import is.codion.swing.common.ui.value.UpdateOn;

import javax.swing.Action;
import javax.swing.JTextField;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.time.temporal.Temporal;

import static java.util.Objects.requireNonNull;

final class DefaultTextFieldBuilder<T> extends AbstractComponentBuilder<T, JTextField, TextFieldBuilder<T>>
        implements TextFieldBuilder<T> {

  private UpdateOn updateOn = UpdateOn.KEYSTROKE;
  private int columns;
  private Action action;
  private boolean selectAllOnFocusGained;
  private boolean upperCase;
  private boolean lowerCase;

  DefaultTextFieldBuilder(final Property<T> attribute, final Value<T> value) {
    super(attribute, value);
  }

  @Override
  public TextFieldBuilder<T> updateOn(final UpdateOn updateOn) {
    this.updateOn = updateOn;
    return this;
  }

  @Override
  public TextFieldBuilder<T> columns(final int columns) {
    this.columns = columns;
    return this;
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
  public TextFieldBuilder<T> upperCase() {
    this.upperCase = true;
    this.lowerCase = false;
    return this;
  }

  @Override
  public TextFieldBuilder<T> lowerCase() {
    this.lowerCase = true;
    this.upperCase = false;
    return this;
  }

  @Override
  protected JTextField buildComponent() {
    final JTextField textField = createTextField();
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

    return textField;
  }

  private JTextField createTextField() {
    final JTextField textField = createTextField(property);
    final Attribute<T> attribute = property.getAttribute();
    if (attribute.isString()) {
      ComponentValues.textComponent(textField, property.getFormat(), updateOn).link((Value<String>) value);
    }
    else if (attribute.isCharacter()) {
      ComponentValues.characterTextField(textField, updateOn).link((Value<Character>) value);
    }
    else if (attribute.isInteger()) {
      ComponentValues.integerFieldBuilder()
              .component((IntegerField) textField)
              .updateOn(updateOn)
              .build()
              .link((Value<Integer>) value);
    }
    else if (attribute.isDouble()) {
      ComponentValues.doubleFieldBuilder()
              .component((DoubleField) textField)
              .updateOn(updateOn)
              .build()
              .link((Value<Double>) value);
    }
    else if (attribute.isBigDecimal()) {
      ComponentValues.bigDecimalFieldBuilder()
              .component((BigDecimalField) textField)
              .updateOn(updateOn)
              .build()
              .link((Value<BigDecimal>) value);
    }
    else if (attribute.isLong()) {
      ComponentValues.longFieldBuilder()
              .component((LongField) textField)
              .updateOn(updateOn)
              .build()
              .link((Value<Long>) value);
    }
    else if (attribute.isTemporal()) {
      ComponentValues.temporalField((TemporalField<Temporal>) textField, updateOn).link((Value<Temporal>) value);
    }
    else {
      throw new IllegalArgumentException("Text fields not implemented for attribute type: " + attribute);
    }

    return textField;
  }

  static JTextField createTextField(final Property<?> property) {
    final Attribute<?> attribute = property.getAttribute();
    if (attribute.isInteger()) {
      return initializeIntegerField((Property<Integer>) property);
    }
    else if (attribute.isDouble()) {
      return initializeDoubleField((Property<Double>) property);
    }
    else if (attribute.isBigDecimal()) {
      return initializeBigDecimalField((Property<BigDecimal>) property);
    }
    else if (attribute.isLong()) {
      return initializeLongField((Property<Long>) property);
    }
    else if (attribute.isTemporal()) {
      return new TemporalField<>((Class<Temporal>) attribute.getTypeClass(), property.getDateTimePattern());
    }
    else if (attribute.isString()) {
      return initializeStringField(property.getMaximumLength());
    }
    else if (attribute.isCharacter()) {
      return new JTextField(new SizedDocument(1), "", 1);
    }

    throw new IllegalArgumentException("Creating text fields for type: " + attribute.getTypeClass() + " is not implemented (" + property + ")");
  }

  private static JTextField initializeStringField(final int maximumLength) {
    final SizedDocument sizedDocument = new SizedDocument();
    if (maximumLength > 0) {
      sizedDocument.setMaximumLength(maximumLength);
    }

    return new JTextField(sizedDocument, "", 0);
  }

  private static DoubleField initializeDoubleField(final Property<Double> property) {
    final DoubleField field = new DoubleField((DecimalFormat) cloneFormat((NumberFormat) property.getFormat()));
    if (property.getMinimumValue() != null && property.getMaximumValue() != null) {
      field.setRange(Math.min(property.getMinimumValue(), 0), property.getMaximumValue());
    }

    return field;
  }

  private static BigDecimalField initializeBigDecimalField(final Property<BigDecimal> property) {
    final BigDecimalField field = new BigDecimalField((DecimalFormat) cloneFormat((NumberFormat) property.getFormat()));
    if (property.getMinimumValue() != null && property.getMaximumValue() != null) {
      field.setRange(Math.min(property.getMinimumValue(), 0), property.getMaximumValue());
    }

    return field;
  }

  private static IntegerField initializeIntegerField(final Property<Integer> property) {
    final IntegerField field = new IntegerField(cloneFormat((NumberFormat) property.getFormat()));
    if (property.getMinimumValue() != null && property.getMaximumValue() != null) {
      field.setRange(property.getMinimumValue(), property.getMaximumValue());
    }

    return field;
  }

  private static LongField initializeLongField(final Property<Long> property) {
    final LongField field = new LongField(cloneFormat((NumberFormat) property.getFormat()));
    if (property.getMinimumValue() != null && property.getMaximumValue() != null) {
      field.setRange(property.getMinimumValue(), property.getMaximumValue());
    }

    return field;
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
