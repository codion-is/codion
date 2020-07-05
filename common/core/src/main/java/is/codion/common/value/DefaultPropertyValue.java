/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.value;

import is.codion.common.Util;
import is.codion.common.event.EventDataListener;
import is.codion.common.event.EventListener;
import is.codion.common.event.EventObserver;

import java.lang.reflect.Method;

import static is.codion.common.Util.nullOrEmpty;
import static java.util.Objects.requireNonNull;

final class DefaultPropertyValue<V> implements PropertyValue<V> {

  private final EventObserver<V> changeEvent;
  private final String propertyName;
  private final Class<V> valueClass;
  private final Object valueOwner;
  private final Method getMethod;
  private Method setMethod;

  DefaultPropertyValue(final Object valueOwner, final String propertyName, final Class<V> valueClass,
                       final EventObserver<V> changeObserver) {
    if (nullOrEmpty(propertyName)) {
      throw new IllegalArgumentException("propertyName is null or an empty string");
    }
    this.propertyName = propertyName;
    this.valueClass = valueClass;
    try {
      this.valueOwner = requireNonNull(valueOwner, "valueOwner");
      this.changeEvent = requireNonNull(changeObserver);
      this.getMethod = Util.getGetMethod(valueClass, propertyName, valueOwner);
    }
    catch (final NoSuchMethodException e) {
      throw new IllegalArgumentException("Get method for property " + propertyName + ", type: " + valueClass +
              " not found in class " + valueOwner.getClass().getName(), e);
    }
    try {
      this.setMethod = Util.getSetMethod(valueClass, propertyName, valueOwner);
    }
    catch (final NoSuchMethodException ignored) {/*ignored*/
      this.setMethod = null;
    }
  }

  @Override
  public V get() {
    try {
      return (V) getMethod.invoke(valueOwner);
    }
    catch (final RuntimeException re) {
      throw re;
    }
    catch (final Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void set(final V value) {
    if (setMethod == null) {
      throw new IllegalStateException("Set method for property not found: " + propertyName);
    }
    try {
      setMethod.invoke(valueOwner, value);
    }
    catch (final RuntimeException re) {
      throw re;
    }
    catch (final Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public String getPropertyName() {
    return propertyName;
  }

  @Override
  public boolean isNullable() {
    return !valueClass.isPrimitive();
  }

  @Override
  public void addListener(final EventListener listener) {
    changeEvent.addListener(listener);
  }

  @Override
  public void removeListener(final EventListener listener) {
    changeEvent.removeListener(listener);
  }

  @Override
  public void addDataListener(final EventDataListener<V> listener) {
    changeEvent.addDataListener(listener);
  }

  @Override
  public void removeDataListener(final EventDataListener<V> listener) {
    changeEvent.removeDataListener(listener);
  }

  @Override
  public void link(final Value<V> linkedValue) {
    new ValueLink<>(this, linkedValue);
  }
}
