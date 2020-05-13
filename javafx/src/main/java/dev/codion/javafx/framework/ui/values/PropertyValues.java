/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package dev.codion.javafx.framework.ui.values;

import dev.codion.common.DateParser;
import dev.codion.common.item.Item;
import dev.codion.common.item.Items;
import dev.codion.common.value.AbstractValue;
import dev.codion.common.value.Value;
import dev.codion.framework.domain.entity.Entity;
import dev.codion.framework.model.EntityLookupModel;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.control.SelectionModel;
import javafx.scene.control.SingleSelectionModel;
import javafx.util.StringConverter;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.Format;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.ParsePosition;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.Temporal;
import java.util.List;

import static dev.codion.common.Util.nullOrEmpty;

/**
 * A factory class for {@link Value} instances based on JavaFX models and properties
 */
public final class PropertyValues {

  private PropertyValues() {/**/}

  /**
   * @param selectionModel the selection model
   * @return a {@link Value} based on the selected item in the given selection model
   */
  public static Value selectedItemValue(final SingleSelectionModel<Item> selectionModel) {
    return new SelectedItemValue(selectionModel);
  }

  /**
   * @param booleanProperty the boolean property
   * @return a {@link Value} based on the given boolean property
   */
  public static Value<Boolean> booleanPropertyValue(final BooleanProperty booleanProperty) {
    return new BooleanPropertyValue(booleanProperty);
  }

  /**
   * @param selectionModel the selection model
   * @param <V> the type of the actual value
   * @return a {@link Value} based on the selected item in the given selection model
   */
  public static <V> Value<V> selectedValue(final SingleSelectionModel<V> selectionModel) {
    return new SelectedValue<>(selectionModel);
  }

  /**
   * @param lookupModel the lookup model
   * @return a {@link Value} based on the entities selected in the given lookup model
   */
  public static Value lookupValue(final EntityLookupModel lookupModel) {
    if (lookupModel.getMultipleSelectionEnabledValue().get()) {
      return new EntityLookupMultiValue(lookupModel);
    }
    else {
      return new EntityLookupSingleValue(lookupModel);
    }
  }

  /**
   * @param property the string property
   * @return a {@link StringValue} based on the given string property
   */
  public static StringValue<String> stringPropertyValue(final StringProperty property) {
    return new DefaultStringValue<>(property, new DefaultStringConverter());
  }

  /**
   * @param property the string property
   * @param numberFormat the format to use
   * @return a Integer {@link StringValue} based on the given string property
   */
  public static StringValue<Integer> integerPropertyValue(final StringProperty property, final NumberFormat numberFormat) {
    return new DefaultStringValue<>(property, new IntegerConverter(numberFormat));
  }

  /**
   * @param property the string property
   * @param numberFormat the format to use
   * @return a Long {@link StringValue} based on the given string property
   */
  public static StringValue<Long> longPropertyValue(final StringProperty property, final NumberFormat numberFormat) {
    return new DefaultStringValue<>(property, new LongConverter(numberFormat));
  }

  /**
   * @param property the string property
   * @param numberFormat the format to use
   * @return a Double {@link StringValue} based on the given string property
   */
  public static StringValue<Double> doublePropertyValue(final StringProperty property, final NumberFormat numberFormat) {
    return new DefaultStringValue<>(property, new DoubleConverter(numberFormat));
  }

  /**
   * @param property the string property
   * @param decimalFormat the format to use
   * @return a BigDecimal {@link StringValue} based on the given string property
   */
  public static StringValue<BigDecimal> bigDecimalPropertyValue(final StringProperty property, final DecimalFormat decimalFormat) {
    return new DefaultStringValue<>(property, new BigDecimalConverter(decimalFormat));
  }

  /**
   * @param property the string property
   * @param dateTimeFormatter the formatter to use
   * @return a {@link LocalDate} {@link StringValue} based on the given string property
   */
  public static StringValue<LocalDate> datePropertyValue(final StringProperty property, final DateTimeFormatter dateTimeFormatter) {
    return new DefaultStringValue<>(property, new DateConverter<>(dateTimeFormatter, LocalDate::parse));
  }

  /**
   * @param property the string property
   * @param dateTimeFormatter the formatter to use
   * @return a {@link LocalDateTime} {@link StringValue} based on the given string property
   */
  public static StringValue<LocalDateTime> timestampPropertyValue(final StringProperty property, final DateTimeFormatter dateTimeFormatter) {
    return new DefaultStringValue<>(property, new DateConverter<>(dateTimeFormatter, LocalDateTime::parse));
  }

  /**
   * @param property the string property
   * @param dateTimeFormatter the formatter to use
   * @return a {@link LocalTime} {@link StringValue} based on the given string property
   */
  public static StringValue<LocalTime> timePropertyValue(final StringProperty property, final DateTimeFormatter dateTimeFormatter) {
    return new DefaultStringValue<>(property, new DateConverter<>(dateTimeFormatter, LocalTime::parse));
  }

  /**
   * Parses the given value using the given format
   * @param format the format
   * @param value the value
   * @return the parsed value
   * @throws ParseException in case of an exception
   */
  public static Object parseStrict(final Format format, final String value) throws ParseException {
    final ParsePosition pos = new ParsePosition(0);
    final Object result = format.parseObject(value, pos);
    if (pos.getIndex() < value.length()) {
      throw new ParseException("Failed to parse '" + value + "'", pos.getIndex());
    }

    return result;
  }

  private static final class DefaultStringConverter extends StringConverter<String> {

    @Override
    public String toString(final String object) {
      return object;
    }

    @Override
    public String fromString(final String string) {
      if (string != null && string.isEmpty()) {
        return null;
      }

      return string;
    }
  }

  private static final class IntegerConverter extends StringConverter<Integer> {

    private final NumberFormat numberFormat;

    private IntegerConverter(final NumberFormat numberFormat) {
      this.numberFormat = numberFormat;
    }

    @Override
    public String toString(final Integer value) {
      if (value == null) {
        return "";
      }

      return numberFormat.format(value);
    }

    @Override
    public Integer fromString(final String value) {
      if (nullOrEmpty(value)) {
        return null;
      }
      try {
        final Object number = parseStrict(numberFormat, value);

        return ((Long) number).intValue();
      }
      catch (final ParseException e) {
        return null;
      }
    }
  }

  private static final class LongConverter extends StringConverter<Long> {

    private final NumberFormat numberFormat;

    private LongConverter(final NumberFormat numberFormat) {
      this.numberFormat = numberFormat;
    }

    @Override
    public String toString(final Long value) {
      if (value == null) {
        return "";
      }

      return numberFormat.format(value);
    }

    @Override
    public Long fromString(final String value) {
      if (nullOrEmpty(value)) {
        return null;
      }
      try {
        return (Long) parseStrict(numberFormat, value);
      }
      catch (final ParseException e) {
        return null;
      }
    }
  }

  private static final class DoubleConverter extends StringConverter<Double> {

    private final NumberFormat numberFormat;

    private DoubleConverter(final NumberFormat numberFormat) {
      this.numberFormat = numberFormat;
    }

    @Override
    public String toString(final Double value) {
      if (value == null) {
        return "";
      }

      return numberFormat.format(value);
    }

    @Override
    public Double fromString(final String value) {
      if (nullOrEmpty(value)) {
        return null;
      }
      try {
        final Object number = parseStrict(numberFormat, value);
        if (number instanceof Double) {
          return (Double) number;
        }

        return ((Long) number).doubleValue();
      }
      catch (final ParseException e) {
        return null;
      }
    }
  }

  private static final class BigDecimalConverter extends StringConverter<BigDecimal> {

    private final DecimalFormat numberFormat;

    private BigDecimalConverter(final DecimalFormat numberFormat) {
      this.numberFormat = numberFormat;
      this.numberFormat.setParseBigDecimal(true);
    }

    @Override
    public String toString(final BigDecimal value) {
      if (value == null) {
        return "";
      }

      return numberFormat.format(value);
    }

    @Override
    public BigDecimal fromString(final String value) {
      if (nullOrEmpty(value)) {
        return null;
      }
      try {
        return (BigDecimal) parseStrict(numberFormat, value);
      }
      catch (final ParseException e) {
        return null;
      }
    }
  }

  private static final class DateConverter<T extends Temporal> extends StringConverter<T> {

    private final DateTimeFormatter dateFormatter;
    private final DateParser<T> parser;

    private DateConverter(final DateTimeFormatter dateFormatter, final DateParser<T> parser) {
      this.dateFormatter = dateFormatter;
      this.parser = parser;
    }

    @Override
    public String toString(final T value) {
      if (value != null) {
        return dateFormatter.format(value);
      }
      else {
        return "";
      }
    }
    @Override
    public T fromString(final String string) {
      if (nullOrEmpty(string)) {
        return null;
      }
      try {
        return parser.parse(string, dateFormatter);
      }
      catch (final DateTimeParseException e) {
        return null;
      }
    }
  }

  private static final class DefaultStringValue<V> extends AbstractValue<V> implements StringValue<V> {

    private final StringProperty stringProperty;
    private final StringConverter<V> converter;

    public DefaultStringValue(final StringProperty stringProperty, final StringConverter<V> converter) {
      this.stringProperty = stringProperty;
      this.converter = converter;
      this.stringProperty.addListener((observable, oldValue, newValue) -> notifyValueChange());
    }

    @Override
    public void set(final V v) {
      stringProperty.set(converter.toString(v));
    }

    @Override
    public V get() {
      return converter.fromString(stringProperty.get());
    }

    @Override
    public boolean isNullable() {
      return true;
    }

    @Override
    public StringConverter<V> getConverter() {
      return converter;
    }
  }

  private static final class BooleanPropertyValue extends AbstractValue<Boolean> {

    private final BooleanProperty booleanProperty;

    public BooleanPropertyValue(final BooleanProperty booleanProperty) {
      this.booleanProperty = booleanProperty;
      this.booleanProperty.addListener((observable, oldValue, newValue) -> notifyValueChange());
    }

    @Override
    public void set(final Boolean value) {
      booleanProperty.set(value);
    }

    @Override
    public Boolean get() {
      return booleanProperty.get();
    }

    @Override
    public boolean isNullable() {
      return false;
    }
  }

  private static final class SelectedValue<V> extends AbstractValue<V> {

    private final SingleSelectionModel<V> selectionModel;

    public SelectedValue(final SingleSelectionModel<V> selectionModel) {
      this.selectionModel = selectionModel;
      this.selectionModel.selectedItemProperty().addListener((observable, oldValue, newValue) -> notifyValueChange());
    }

    @Override
    public void set(final V value) {
      selectionModel.select(value);
    }

    @Override
    public V get() {
      return selectionModel.getSelectedItem();
    }

    @Override
    public boolean isNullable() {
      return true;
    }
  }

  private static final class SelectedItemValue<T> extends AbstractValue<T> {

    private final SelectionModel<Item<T>> selectionModel;

    public SelectedItemValue(final SelectionModel<Item<T>> selectionModel) {
      this.selectionModel = selectionModel;
      this.selectionModel.selectedItemProperty().addListener((observable, oldValue, newValue) -> notifyValueChange());
    }

    @Override
    public void set(final T value) {
      selectionModel.select(Items.item(value));
    }

    @Override
    public T get() {
      return selectionModel.getSelectedItem().getValue();
    }

    @Override
    public boolean isNullable() {
      return true;
    }
  }

  private static final class EntityLookupSingleValue extends AbstractValue<Entity> {

    private final EntityLookupModel lookupModel;

    private EntityLookupSingleValue(final EntityLookupModel lookupModel) {
      this.lookupModel = lookupModel;
      this.lookupModel.addSelectedEntitiesListener(selected -> notifyValueChange());
    }

    @Override
    public void set(final Entity value) {
      lookupModel.setSelectedEntity(value);
    }

    @Override
    public Entity get() {
      final List<Entity> selected = lookupModel.getSelectedEntities();

      return selected.isEmpty() ? null : selected.iterator().next();
    }

    @Override
    public boolean isNullable() {
      return true;
    }
  }

  private static final class EntityLookupMultiValue extends AbstractValue<List<Entity>> {

    private final EntityLookupModel lookupModel;

    private EntityLookupMultiValue(final EntityLookupModel lookupModel) {
      this.lookupModel = lookupModel;
      this.lookupModel.addSelectedEntitiesListener(entities -> notifyValueChange());
    }

    @Override
    public void set(final List<Entity> value) {
      lookupModel.setSelectedEntities(value);
    }

    @Override
    public List<Entity> get() {
      return lookupModel.getSelectedEntities();
    }

    @Override
    public boolean isNullable() {
      return false;
    }
  }
}
