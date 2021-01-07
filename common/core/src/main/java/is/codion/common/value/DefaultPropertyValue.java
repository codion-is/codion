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

final class DefaultPropertyValue<V> implements PropertyValue<V> {

  private final EventObserver<V> changeEvent;
  private final String propertyName;
  private final Class<V> valueClass;
  private final Object valueOwner;
  private final Method getMethod;
  private final Set<Validator<V>> validators = new LinkedHashSet<>(0);

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
      this.getMethod = getGetMethod(valueClass, propertyName, valueOwner.getClass());
    }
    catch (final NoSuchMethodException e) {
      throw new IllegalArgumentException("Get method for property " + propertyName + ", type: " + valueClass +
              " not found in class " + valueOwner.getClass().getName(), e);
    }
    try {
      this.setMethod = getSetMethod(valueClass, propertyName, valueOwner.getClass());
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
  public final Optional<V> toOptional() {
    if (isNullable()) {
      return Optional.ofNullable(get());
    }

    return Optional.of(get());
  }

  @Override
  public void set(final V value) {
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
  public V getOrThrow() throws IllegalStateException {
    return getOrThrow("Required property is missing: " + propertyName);
  }

  @Override
  public V getOrThrow(final String message) throws IllegalStateException {
    requireNonNull(message, "message");
    final V value = get();
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
  public boolean is(final V value) {
    return Objects.equals(get(), value);
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
  public void link(final Value<V> originalValue) {
    new ValueLink<>(this, originalValue);
  }

  @Override
  public void link(final ValueObserver<V> originalValueObserver) {
    set(requireNonNull(originalValueObserver, "originalValueObserver").get());
    originalValueObserver.addDataListener(this::set);
  }

  @Override
  public void addValidator(final Validator<V> validator) {
    requireNonNull(validator, "validator").validate(get());
    validators.add(validator);
  }

  @Override
  public Collection<Validator<V>> getValidators() {
    return unmodifiableSet(validators);
  }

  static Method getSetMethod(final Class<?> valueType, final String property, final Class<?> ownerClass) throws NoSuchMethodException {
    if (requireNonNull(property, "property").isEmpty()) {
      throw new IllegalArgumentException("Property must be specified");
    }

    return requireNonNull(ownerClass, "ownerClass").getMethod("set" +
            Character.toUpperCase(property.charAt(0)) + property.substring(1), requireNonNull(valueType, "valueType"));
  }

  static Method getGetMethod(final Class<?> valueType, final String property, final Class<?> ownerClass) throws NoSuchMethodException {
    requireNonNull(valueType, "valueType");
    requireNonNull(property, "property");
    requireNonNull(ownerClass, "ownerClass");
    if (property.length() == 0) {
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

    return ownerClass.getMethod("get" + propertyName);
  }
}
