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

public final class PropertyValues {

  private PropertyValues() {/**/}

  public static <V> Value<V> selectedItemValue(final SelectionModel<V> selectionModel) {
    return new SelectedItemValue<V>(selectionModel);
  }

  public static StringValue<String> stringPropertyValue(final StringProperty property) {
    return new DefaultStringValue<String>(property, new StringConverter<String>() {
      @Override
      public String toString(final String value) {
        return value;
      }

      @Override
      public String fromString(final String value) {
        return value;
      }
    });
  }

  public static StringValue<Integer> integerPropertyValue(final StringProperty property) {
    return new IntegerValue(property);
  }

  public static StringValue<Double> doublePropertyValue(final StringProperty property) {
    return new DoubleValue(property);
  }

  private static final class IntegerValue extends DefaultStringValue<Integer> {
    public IntegerValue(final StringProperty property) {
      super(property, new StringConverter<Integer>() {
        @Override
        public String toString(final Integer value) {
          return value == null ? "" : value.toString();
        }

        @Override
        public Integer fromString(final String value) {
          return Util.nullOrEmpty(value) ? null : Integer.parseInt(value);
        }
      });
    }
  }

  private static final class DoubleValue extends DefaultStringValue<Double> {
    public DoubleValue(final StringProperty property) {
      super(property, new StringConverter<Double>() {
        @Override
        public String toString(final Double value) {
          return value == null ? "" : value.toString();
        }

        @Override
        public Double fromString(final String value) {
          return Util.nullOrEmpty(value) ? null : Double.parseDouble(value);
        }
      });
    }
  }

  private static class DefaultStringValue<V> implements StringValue<V> {

    private final StringProperty property;
    private final StringConverter<V> converter;
    private final Event changeEvent = Events.event();

    public DefaultStringValue(final StringProperty property, final StringConverter<V> converter) {
      this.property = property;
      this.converter = converter;
      this.property.addListener(new ChangeListener<Object>() {
        @Override
        public void changed(final ObservableValue<? extends Object> observable, final Object oldValue, final Object newValue) {
          changeEvent.fire(newValue);
        }
      });
    }

    @Override
    public final void set(final V v) {
      property.set(converter.toString(v));
    }

    @Override
    public final V get() {
      return converter.fromString(property.get());
    }

    @Override
    public final EventObserver<V> getObserver() {
      return changeEvent.getObserver();
    }

    @Override
    public StringConverter<V> getConverter() {
      return converter;
    }

    protected final StringProperty getProperty() {
      return property;
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
