/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.value;

import is.codion.common.event.EventDataListener;
import is.codion.common.event.EventListener;
import is.codion.common.event.EventObserver;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static is.codion.common.Util.nullOrEmpty;
import static java.util.Collections.unmodifiableSet;
import static java.util.Objects.requireNonNull;

final class DefaultPropertyValue<T> implements PropertyValue<T> {

  private final EventObserver<T> changeEvent;
  private final String propertyName;
  private final Class<T> valueClass;
  private final Object valueOwner;
  private final Method getMethod;
  private final Method setMethod;
  private final Set<Validator<T>> validators = new LinkedHashSet<>(0);
  private final Set<Value<T>> linkedValues = new LinkedHashSet<>();

  private ValueObserver<T> observer;

  DefaultPropertyValue(final Object valueOwner, final String propertyName, final Class<T> valueClass,
                       final EventObserver<T> changeObserver) {
    if (nullOrEmpty(propertyName)) {
      throw new IllegalArgumentException("propertyName is null or an empty string");
    }
    this.propertyName = propertyName;
    this.valueClass = requireNonNull(valueClass, "valueClass");
    this.valueOwner = requireNonNull(valueOwner, "valueOwner");
    this.changeEvent = requireNonNull(changeObserver);
    this.getMethod = getGetMethod(valueClass, propertyName, valueOwner.getClass());
    this.setMethod = getSetMethod(valueClass, propertyName, valueOwner.getClass());
  }

  @Override
  public T get() {
    try {
      return (T) getMethod.invoke(valueOwner);
    }
    catch (final RuntimeException re) {
      throw re;
    }
    catch (final Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public ValueObserver<T> getObserver() {
    synchronized (changeEvent) {
      if (observer == null) {
        observer = new DefaultValueObserver<>(this);
      }

      return observer;
    }
  }

  @Override
  public Optional<T> toOptional() {
    if (isNullable()) {
      return Optional.ofNullable(get());
    }

    return Optional.of(get());
  }

  @Override
  public void set(final T value) {
    if (setMethod == null) {
      throw new IllegalStateException("Set method for property not found: " + propertyName);
    }
    validators.forEach(validator -> validator.validate(value));
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
  public T getOrThrow() throws IllegalStateException {
    return getOrThrow("Value of " + propertyName + " is null");
  }

  @Override
  public T getOrThrow(final String message) throws IllegalStateException {
    requireNonNull(message, "message");
    final T value = get();
    if (value == null) {
      throw new IllegalStateException(message);
    }

    return value;
  }

  @Override
  public boolean isNull() {
    return isNullable() && get() == null;
  }

  @Override
  public boolean isNotNull() {
    return !isNull();
  }

  @Override
  public boolean isNullable() {
    return !valueClass.isPrimitive();
  }

  @Override
  public boolean equalTo(final T value) {
    return Objects.equals(get(), value);
  }

  @Override
  public void onEvent(final T data) {
    set(data);
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
  public void addDataListener(final EventDataListener<T> listener) {
    changeEvent.addDataListener(listener);
  }

  @Override
  public void removeDataListener(final EventDataListener<T> listener) {
    changeEvent.removeDataListener(listener);
  }

  @Override
  public void link(final Value<T> originalValue) {
    if (linkedValues.contains(requireNonNull(originalValue, "originalValue"))) {
      throw new IllegalArgumentException("Values are already linked");
    }
    new ValueLink<>(this, originalValue);
    linkedValues.add(originalValue);
  }

  @Override
  public void link(final ValueObserver<T> originalValueObserver) {
    set(requireNonNull(originalValueObserver, "originalValueObserver").get());
    originalValueObserver.addDataListener(this::set);
  }

  @Override
  public Set<Value<T>> getLinkedValues() {
    return unmodifiableSet(linkedValues);
  }

  @Override
  public void addValidator(final Validator<T> validator) {
    requireNonNull(validator, "validator").validate(get());
    validators.add(validator);
  }

  @Override
  public Collection<Validator<T>> getValidators() {
    return unmodifiableSet(validators);
  }

  static Method getSetMethod(final Class<?> valueType, final String property, final Class<?> ownerClass) {
    if (requireNonNull(property, "property").isEmpty()) {
      throw new IllegalArgumentException("Property must be specified");
    }

    try {
      return requireNonNull(ownerClass, "ownerClass").getMethod("set" +
              Character.toUpperCase(property.charAt(0)) + property.substring(1), requireNonNull(valueType, "valueType"));
    }
    catch (final NoSuchMethodException e) {
      return null;
    }
  }

  static Method getGetMethod(final Class<?> valueType, final String property, final Class<?> ownerClass) {
    requireNonNull(valueType, "valueType");
    requireNonNull(property, "property");
    requireNonNull(ownerClass, "ownerClass");
    if (property.isEmpty()) {
      throw new IllegalArgumentException("Property must be specified");
    }
    final String propertyName = Character.toUpperCase(property.charAt(0)) + property.substring(1);
    if (valueType.equals(boolean.class) || valueType.equals(Boolean.class)) {
      try {
        return ownerClass.getMethod("is" + propertyName);
      }
      catch (final NoSuchMethodException ignored) {/*ignored*/}
      try {
        return ownerClass.getMethod(propertyName.substring(0, 1).toLowerCase() + propertyName.substring(1));
      }
      catch (final NoSuchMethodException ignored) {/*ignored*/}
    }

    try {
      return ownerClass.getMethod("get" + propertyName);
    }
    catch (final NoSuchMethodException e) {
      throw new IllegalArgumentException("Get method for property " + propertyName + ", type: " + valueType +
              " not found in class " + ownerClass.getName(), e);
    }

  }
}
