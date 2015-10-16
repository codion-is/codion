/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.javafx.framework.ui.values;

import org.jminor.common.model.Event;
import org.jminor.common.model.EventObserver;
import org.jminor.common.model.Events;
import org.jminor.common.model.Util;
import org.jminor.common.model.Value;

import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.SelectionModel;
import javafx.util.StringConverter;

import java.text.Format;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public final class PropertyValues {

  private PropertyValues() {/**/}

  public static <V> Value<V> selectedItemValue(final SelectionModel<V> selectionModel) {
    return new SelectedItemValue<V>(selectionModel);
  }

  public static StringValue<String> stringPropertyValue(final StringProperty property) {
    return new DefaultStringValue<String>(property, new DefaultStringConverter());
  }

  public static StringValue<Integer> integerPropertyValue(final StringProperty property, final NumberFormat numberFormat) {
    return new DefaultStringValue<Integer>(property, new IntegerConverter(numberFormat));
  }

  public static StringValue<Double> doublePropertyValue(final StringProperty property, final NumberFormat numberFormat) {
    return new DefaultStringValue<Double>(property, new DoubleConverter(numberFormat));
  }

  public static StringValue<LocalDate> datePropertyValue(final StringProperty property,
                                                         final SimpleDateFormat dateFormat) {
    return new DefaultStringValue<LocalDate>(property, new DateConverter(dateFormat));
  }

  public static Object parseStrict(final Format format, final String value) throws ParseException {
    final ParsePosition pos = new ParsePosition(0);
    final Object result = format.parseObject(value, pos);
    if(pos.getIndex() < value.length()) {
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
      if (Util.nullOrEmpty(value)) {
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
      if (Util.nullOrEmpty(value)) {
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

  private static class DateConverter extends StringConverter<LocalDate> {

    private final DateTimeFormatter dateFormatter;

    private DateConverter(final SimpleDateFormat dateFormat) {
      this.dateFormatter = DateTimeFormatter.ofPattern(dateFormat.toPattern());
    }

    @Override
    public String toString(final LocalDate date) {
      if (date != null) {
        return dateFormatter.format(date);
      }
      else {
        return "";
      }
    }
    @Override
    public LocalDate fromString(final String string) {
      if (Util.nullOrEmpty(string)) {
        return null;
      }
      try {
        return LocalDate.parse(string, dateFormatter);
      }
      catch (final DateTimeParseException e) {
        return null;
      }
    }
  }

  private static class DefaultStringValue<V> implements StringValue<V> {

    private final StringProperty stringProperty;
    private final StringConverter<V> converter;
    private final Event changeEvent = Events.event();

    public DefaultStringValue(final StringProperty stringProperty, final StringConverter<V> converter) {
      this.stringProperty = stringProperty;
      this.converter = converter;
      this.stringProperty.addListener(new ChangeListener<Object>() {
        @Override
        public void changed(final ObservableValue<? extends Object> observable, final Object oldValue, final Object newValue) {
          changeEvent.fire(newValue);
        }
      });
    }

    @Override
    public final void set(final V v) {
      stringProperty.set(converter.toString(v));
    }

    @Override
    public final V get() {
      return converter.fromString(stringProperty.get());
    }

    @Override
    public final EventObserver<V> getObserver() {
      return changeEvent.getObserver();
    }

    @Override
    public StringConverter<V> getConverter() {
      return converter;
    }

    protected final StringProperty getStringProperty() {
      return stringProperty;
    }
  }

  private static class SelectedItemValue<V> implements Value<V> {

    private final SelectionModel<V> selectionModel;
    private final Event<V> changeEvent = Events.event();

    public SelectedItemValue(final SelectionModel<V> selectionModel) {
      this.selectionModel = selectionModel;
      this.selectionModel.selectedItemProperty().addListener(new ChangeListener<V>() {
        @Override
        public void changed(final ObservableValue<? extends V> observable, final V oldValue, final V newValue) {
          changeEvent.fire(newValue);
        }
      });
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
    public EventObserver<V> getObserver() {
      return changeEvent.getObserver();
    }
  }
}
