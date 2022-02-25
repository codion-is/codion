/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.javafx.framework.ui.values;

import is.codion.common.DateTimeParser;
import is.codion.common.item.Item;
import is.codion.common.value.AbstractValue;
import is.codion.common.value.Value;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.model.EntitySearchModel;

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

import static is.codion.common.Util.nullOrEmpty;

/**
 * A factory class for {@link Value} instances based on JavaFX models and properties
 */
public final class PropertyValues {

  private PropertyValues() {/**/}

  /**
   * @param selectionModel the selection model
   * @param <T> the value type
   * @return a {@link Value} based on the selected item in the given selection model
   */
  public static <T> Value<T> selectedItemValue(final SingleSelectionModel<Item<T>> selectionModel) {
    return new SelectedItemValue<>(selectionModel);
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
   * @param <T> the type of the actual value
   * @return a {@link Value} based on the selected item in the given selection model
   */
  public static <T> Value<T> selectedValue(final SingleSelectionModel<T> selectionModel) {
    return new SelectedValue<>(selectionModel);
  }

  /**
   * @param searchModel the search model
   * @return a {@link Value} based on the entities selected in the given search model
   * @throws IllegalArgumentException in case the search model has multiple selection enabled
   */
  public static Value<Entity> singleSearchValue(final EntitySearchModel searchModel) {
    if (searchModel.getMultipleSelectionEnabledValue().get()) {
      throw new IllegalArgumentException("Multiple item selection is enabled for search model");
    }

    return new EntitySearchSingleValue(searchModel);
  }

  /**
   * @param searchModel the search model
   * @return a {@link Value} based on the entities selected in the given search model
   */
  public static Value<List<Entity>> multipleSearchValue(final EntitySearchModel searchModel) {
    return new EntitySearchMultiValue(searchModel);
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
   * @return an Integer {@link StringValue} based on the given string property
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
    ParsePosition pos = new ParsePosition(0);
    Object result = format.parseObject(value, pos);
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
        Object number = parseStrict(numberFormat, value);

        return ((Long) number).intValue();
      }
      catch (ParseException e) {
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
      catch (ParseException e) {
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
        Object number = parseStrict(numberFormat, value);
        if (number instanceof Double) {
          return (Double) number;
        }

        return ((Long) number).doubleValue();
      }
      catch (ParseException e) {
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
      catch (ParseException e) {
        return null;
      }
    }
  }

  private static final class DateConverter<T extends Temporal> extends StringConverter<T> {

    private final DateTimeFormatter dateFormatter;
    private final DateTimeParser<T> parser;

    private DateConverter(final DateTimeFormatter dateFormatter, final DateTimeParser<T> parser) {
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
      catch (DateTimeParseException e) {
        return null;
      }
    }
  }

  private static final class DefaultStringValue<T> extends AbstractValue<T> implements StringValue<T> {

    private final StringProperty stringProperty;
    private final StringConverter<T> converter;

    public DefaultStringValue(final StringProperty stringProperty, final StringConverter<T> converter) {
      this.stringProperty = stringProperty;
      this.converter = converter;
      this.stringProperty.addListener((observable, oldValue, newValue) -> notifyValueChange());
    }

    @Override
    public T get() {
      return converter.fromString(stringProperty.get());
    }

    @Override
    public StringConverter<T> getConverter() {
      return converter;
    }

    @Override
    protected void setValue(final T value) {
      stringProperty.set(converter.toString(value));
    }
  }

  private static final class BooleanPropertyValue extends AbstractValue<Boolean> {

    private final BooleanProperty booleanProperty;

    public BooleanPropertyValue(final BooleanProperty booleanProperty) {
      this.booleanProperty = booleanProperty;
      this.booleanProperty.addListener((observable, oldValue, newValue) -> notifyValueChange());
    }

    @Override
    public Boolean get() {
      return booleanProperty.get();
    }

    @Override
    protected void setValue(final Boolean value) {
      booleanProperty.set(value);
    }
  }

  private static final class SelectedValue<T> extends AbstractValue<T> {

    private final SingleSelectionModel<T> selectionModel;

    public SelectedValue(final SingleSelectionModel<T> selectionModel) {
      this.selectionModel = selectionModel;
      this.selectionModel.selectedItemProperty().addListener((observable, oldValue, newValue) -> notifyValueChange());
    }

    @Override
    public T get() {
      return selectionModel.getSelectedItem();
    }

    @Override
    protected void setValue(final T value) {
      selectionModel.select(value);
    }
  }

  private static final class SelectedItemValue<T> extends AbstractValue<T> {

    private final SelectionModel<Item<T>> selectionModel;

    public SelectedItemValue(final SelectionModel<Item<T>> selectionModel) {
      this.selectionModel = selectionModel;
      this.selectionModel.selectedItemProperty().addListener((observable, oldValue, newValue) -> notifyValueChange());
    }

    @Override
    public T get() {
      Item<T> selectedItem = selectionModel.getSelectedItem();
      if (selectedItem == null) {
        return null;
      }

      return selectedItem.getValue();
    }

    @Override
    protected void setValue(final T value) {
      selectionModel.select(Item.item(value));
    }
  }

  private static final class EntitySearchSingleValue extends AbstractValue<Entity> {

    private final EntitySearchModel searchModel;

    private EntitySearchSingleValue(final EntitySearchModel searchModel) {
      this.searchModel = searchModel;
      this.searchModel.addSelectedEntitiesListener(selected -> notifyValueChange());
    }

    @Override
    public Entity get() {
      List<Entity> selected = searchModel.getSelectedEntities();

      return selected.isEmpty() ? null : selected.iterator().next();
    }

    @Override
    protected void setValue(final Entity value) {
      searchModel.setSelectedEntity(value);
    }
  }

  private static final class EntitySearchMultiValue extends AbstractValue<List<Entity>> {

    private final EntitySearchModel searchModel;

    private EntitySearchMultiValue(final EntitySearchModel searchModel) {
      this.searchModel = searchModel;
      this.searchModel.addSelectedEntitiesListener(entities -> notifyValueChange());
    }

    @Override
    public List<Entity> get() {
      return searchModel.getSelectedEntities();
    }

    @Override
    protected void setValue(final List<Entity> value) {
      searchModel.setSelectedEntities(value);
    }
  }
}
