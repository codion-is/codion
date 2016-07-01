/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.javafx.framework.ui.values;

import org.jminor.common.Event;
import org.jminor.common.EventObserver;
import org.jminor.common.Events;
import org.jminor.common.Item;
import org.jminor.common.Util;
import org.jminor.common.Value;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.model.EntityLookupModel;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.SelectionModel;
import javafx.scene.control.SingleSelectionModel;
import javafx.util.StringConverter;

import java.text.Format;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Collection;

public final class PropertyValues {

  private PropertyValues() {/**/}

  public static Value selectedItemValue(final SingleSelectionModel<Item> selectionModel) {
    return new SelectedItemValue(selectionModel);
  }

  public static Value<Boolean> booleanPropertyValue(final BooleanProperty booleanProperty) {
    return new BooleanPropertyValue(booleanProperty);
  }

  public static <V> Value<V> selectedValue(final SingleSelectionModel<V> selectionModel) {
    return new SelectedValue<>(selectionModel);
  }

  public static Value lookupValue(final EntityLookupModel model) {
    if (model.getMultipleSelectionAllowedValue().get()) {
      return new EntityLookupMultiValue(model);
    }
    else {
      return new EntityLookupSingleValue(model);
    }
  }

  public static StringValue<String> stringPropertyValue(final StringProperty property) {
    return new DefaultStringValue<>(property, new DefaultStringConverter());
  }

  public static StringValue<Integer> integerPropertyValue(final StringProperty property, final NumberFormat numberFormat) {
    return new DefaultStringValue<>(property, new IntegerConverter(numberFormat));
  }

  public static StringValue<Long> longPropertyValue(final StringProperty property, final NumberFormat numberFormat) {
    return new DefaultStringValue<>(property, new LongConverter(numberFormat));
  }

  public static StringValue<Double> doublePropertyValue(final StringProperty property, final NumberFormat numberFormat) {
    return new DefaultStringValue<>(property, new DoubleConverter(numberFormat));
  }

  public static StringValue<LocalDate> datePropertyValue(final StringProperty property, final SimpleDateFormat dateFormat) {
    return new DefaultStringValue<>(property, new DateConverter(dateFormat));
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
      if (Util.nullOrEmpty(value)) {
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

  private static final class DateConverter extends StringConverter<LocalDate> {

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

  private static final class DefaultStringValue<V> implements StringValue<V> {

    private final StringProperty stringProperty;
    private final StringConverter<V> converter;
    private final Event<V> changeEvent = Events.event();

    public DefaultStringValue(final StringProperty stringProperty, final StringConverter<V> converter) {
      this.stringProperty = stringProperty;
      this.converter = converter;
      this.stringProperty.addListener(new ChangeListener<Object>() {
        @Override
        public void changed(final ObservableValue<? extends Object> observable, final Object oldValue, final Object newValue) {
          changeEvent.fire((V) newValue);
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

  private static final class BooleanPropertyValue implements Value<Boolean> {

    private final BooleanProperty booleanProperty;
    private final Event<Boolean> changeEvent = Events.event();

    public BooleanPropertyValue(final BooleanProperty booleanProperty) {
      this.booleanProperty = booleanProperty;
      this.booleanProperty.addListener((observable, oldValue, newValue) -> changeEvent.fire(newValue));
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
    public EventObserver<Boolean> getObserver() {
      return changeEvent.getObserver();
    }
  }

  private static final class SelectedValue<V> implements Value<V> {

    private final SingleSelectionModel<V> selectionModel;
    private final Event<V> changeEvent = Events.event();

    public SelectedValue(final SingleSelectionModel<V> selectionModel) {
      this.selectionModel = selectionModel;
      this.selectionModel.selectedItemProperty().addListener((observable, oldValue, newValue) -> {
        changeEvent.fire(newValue);
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

  private static final class SelectedItemValue implements Value {

    private final Event changeEvent = Events.event();
    private final SelectionModel<Item> selectionModel;

    public SelectedItemValue(final SelectionModel<Item> selectionModel) {
      this.selectionModel = selectionModel;
      selectionModel.selectedItemProperty().addListener((observable, oldValue, newValue) ->
              changeEvent.fire(newValue.getItem()));
    }

    @Override
    public void set(final Object value) {
      selectionModel.select(new Item(value));
    }

    @Override
    public Object get() {
      return selectionModel.getSelectedItem().getItem();
    }

    @Override
    public EventObserver getObserver() {
      return changeEvent.getObserver();
    }
  }

  private static final class EntityLookupSingleValue implements Value<Entity> {

    private final EntityLookupModel lookupModel;
    private final Event<Entity> selectionListener = Events.event();

    private EntityLookupSingleValue(final EntityLookupModel lookupModel) {
      this.lookupModel = lookupModel;
      this.lookupModel.addSelectedEntitiesListener(selected -> selectionListener.fire(selected.isEmpty() ? null : selected.iterator().next()));
    }

    @Override
    public void set(final Entity value) {
      lookupModel.setSelectedEntity(value);
    }

    @Override
    public Entity get() {
      final Collection<Entity> selected = lookupModel.getSelectedEntities();

      return selected.isEmpty() ? null : selected.iterator().next();
    }

    @Override
    public EventObserver<Entity> getObserver() {
      return selectionListener.getObserver();
    }
  }

  private static final class EntityLookupMultiValue implements Value<Collection<Entity>> {

    private final EntityLookupModel lookupModel;
    private final Event<Collection<Entity>> selectionListener = Events.event();

    private EntityLookupMultiValue(final EntityLookupModel lookupModel) {
      this.lookupModel = lookupModel;
      this.lookupModel.addSelectedEntitiesListener(selectionListener);
    }

    @Override
    public void set(final Collection<Entity> value) {
      lookupModel.setSelectedEntities(value);
    }

    @Override
    public Collection<Entity> get() {
      return lookupModel.getSelectedEntities();
    }

    @Override
    public EventObserver<Collection<Entity>> getObserver() {
      return selectionListener.getObserver();
    }
  }
}
