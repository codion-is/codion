/*
 * Copyright (c) 2019 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.value;

import is.codion.common.Primitives;
import is.codion.common.event.EventObserver;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Optional;

import static is.codion.common.Util.nullOrEmpty;
import static java.util.Objects.requireNonNull;

final class DefaultPropertyValue<T> extends AbstractValue<T> {

  private final EventObserver<T> changeObserver;
  private final String propertyName;
  private final Object valueOwner;
  private final Method getMethod;
  private final Method setMethod;

  DefaultPropertyValue(Object valueOwner, String propertyName, Class<T> valueClass,
                       EventObserver<T> changeObserver) {
    super(requireNonNull(valueClass).isPrimitive() ? Primitives.defaultValue(valueClass) : null);
    if (nullOrEmpty(propertyName)) {
      throw new IllegalArgumentException("propertyName is null or an empty string");
    }
    this.changeObserver = requireNonNull(changeObserver);
    this.propertyName = propertyName;
    this.valueOwner = requireNonNull(valueOwner, "valueOwner");
    this.getMethod = getGetMethod(valueClass, propertyName, valueOwner.getClass());
    this.setMethod = getSetMethod(valueClass, propertyName, valueOwner.getClass()).orElse(null);
  }

  @Override
  public T get() {
    try {
      return (T) getMethod.invoke(valueOwner);
    }
    catch (InvocationTargetException e) {
      Throwable cause = e.getCause();
      if (cause instanceof RuntimeException) {
        throw (RuntimeException) cause;
      }

      throw new RuntimeException(cause);
    }
    catch (RuntimeException re) {
      throw re;
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  protected void setValue(T value) {
    if (setMethod == null) {
      throw new IllegalStateException("Set method for property not found: " + propertyName);
    }
    try {
      setMethod.invoke(valueOwner, value);
    }
    catch (InvocationTargetException e) {
      Throwable cause = e.getCause();
      if (cause instanceof RuntimeException) {
        throw (RuntimeException) cause;
      }

      throw new RuntimeException(cause);
    }
    catch (RuntimeException re) {
      throw re;
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  protected EventObserver<T> changeObserver() {
    return changeObserver;
  }

  static Optional<Method> getSetMethod(Class<?> valueType, String property, Class<?> ownerClass) {
    requireNonNull(ownerClass, "ownerClass");
    requireNonNull(valueType, "valueType");
    if (requireNonNull(property, "property").isEmpty()) {
      throw new IllegalArgumentException("Property must be specified");
    }

    try {
      String propertyName = Character.toUpperCase(property.charAt(0)) + property.substring(1);

      return Optional.of(ownerClass.getMethod("set" + propertyName, valueType));
    }
    catch (NoSuchMethodException e) {
      return Optional.empty();
    }
  }

  static Method getGetMethod(Class<?> valueType, String property, Class<?> ownerClass) {
    requireNonNull(valueType, "valueType");
    requireNonNull(property, "property");
    requireNonNull(ownerClass, "ownerClass");
    if (property.isEmpty()) {
      throw new IllegalArgumentException("Property must be specified");
    }
    String propertyName = Character.toUpperCase(property.charAt(0)) + property.substring(1);
    if (valueType.equals(boolean.class) || valueType.equals(Boolean.class)) {
      try {
        return ownerClass.getMethod("is" + propertyName);
      }
      catch (NoSuchMethodException ignored) {/*ignored*/}
      try {
        return ownerClass.getMethod(propertyName.substring(0, 1).toLowerCase() + propertyName.substring(1));
      }
      catch (NoSuchMethodException ignored) {/*ignored*/}
    }

    try {
      return ownerClass.getMethod("get" + propertyName);
    }
    catch (NoSuchMethodException e) {
      throw new IllegalArgumentException("Get method for property " + propertyName + ", type: " + valueType +
              " not found in class " + ownerClass.getName(), e);
    }
  }
}
