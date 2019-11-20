/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.value;

import org.jminor.common.Util;
import org.jminor.common.event.EventObserver;

import java.lang.reflect.Method;

import static java.util.Objects.requireNonNull;
import static org.jminor.common.Util.nullOrEmpty;

final class DefaultPropertyValue<V> extends AbstractObservableValue<V> implements PropertyValue<V> {

  private final EventObserver<V> changeEvent;
  private final String propertyName;
  private final Object valueOwner;
  private final Method getMethod;
  private Method setMethod;

  DefaultPropertyValue(final Object valueOwner, final String propertyName, final Class valueClass,
                       final EventObserver<V> changeEvent) {
    if (nullOrEmpty(propertyName)) {
      throw new IllegalArgumentException("propertyName is null or an empty string");
    }
    this.propertyName = propertyName;
    try {
      this.valueOwner = requireNonNull(valueOwner, "valueOwner");
      this.changeEvent = requireNonNull(changeEvent);
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
      throw new IllegalStateException("Set method for property not found: " + getMethod.getName());
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
  public String getProperty() {
    return propertyName;
  }

  @Override
  public boolean isNullable() {
    return true;
  }

  @Override
  public EventObserver<V> getChangeObserver() {
    return changeEvent;
  }
}
