/*
 * Copyright (c) 2019 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.value;

import is.codion.common.event.Event;
import is.codion.common.event.EventDataListener;
import is.codion.common.event.EventListener;
import is.codion.common.event.EventObserver;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static java.util.Collections.unmodifiableSet;
import static java.util.Objects.requireNonNull;

/**
 * An abstract Value implementation handling everything except the value itself.<br><br>
 * The constructor parameter {@code notifyOnSet} specifies whether this Value instance should automatically call
 * {@link #notifyValueChange()} when the value is changed via {@link AbstractValue#set(Object)}.
 * Some implementations may want to do this manually.
 * @param <T> the value type
 */
public abstract class AbstractValue<T> implements Value<T> {

  private final Event<T> changeEvent = Event.event();
  private final T nullValue;
  private final boolean notifyOnSet;
  private final Set<Validator<T>> validators = new LinkedHashSet<>(0);
  private final Map<Value<T>, ValueLink<T>> linkedValues = new LinkedHashMap<>(0);
  private final EventDataListener<T> originalValueListener = this::set;

  private ValueObserver<T> observer;

  protected AbstractValue() {
    this(null);
  }

  /**
   * Instantiates an {@link AbstractValue} instance, which does not notify on set.
   * @param nullValue the value to use instead of null
   */
  protected AbstractValue(T nullValue) {
    this(nullValue, false);
  }

  /**
   * Instantiates an {@link AbstractValue} instance.
   * @param nullValue the value to use instead of null
   * @param notifyOnSet specifies whether to automatically call {@link #notifyValueChange()} when the value is changed via {@link #set(Object)}
   */
  protected AbstractValue(T nullValue, boolean notifyOnSet) {
    this.nullValue = nullValue;
    this.notifyOnSet = notifyOnSet;
  }

  @Override
  public final void set(T value) {
    T newValue = value == null ? nullValue : value;
    if (!Objects.equals(get(), newValue)) {
      validators.forEach(validator -> validator.validate(newValue));
      setValue(newValue);
      if (notifyOnSet) {
        notifyValueChange();
      }
    }
  }

  @Override
  public final ValueObserver<T> observer() {
    synchronized (changeEvent) {
      if (observer == null) {
        observer = new DefaultValueObserver<>(this);
      }

      return observer;
    }
  }

  @Override
  public final boolean isNullable() {
    return nullValue == null;
  }

  @Override
  public final void onEvent(T data) {
    set(data);
  }

  @Override
  public final void addListener(EventListener listener) {
    changeObserver().addListener(listener);
  }

  @Override
  public final void removeListener(EventListener listener) {
    changeObserver().removeListener(listener);
  }

  @Override
  public final void addDataListener(EventDataListener<T> listener) {
    changeObserver().addDataListener(listener);
  }

  @Override
  public final void removeDataListener(EventDataListener<T> listener) {
    changeObserver().removeDataListener(listener);
  }

  @Override
  public final void link(Value<T> originalValue) {
    if (linkedValues.containsKey(requireNonNull(originalValue, "originalValue"))) {
      throw new IllegalArgumentException("Values are already linked");
    }
    linkedValues.put(originalValue, new ValueLink<>(this, originalValue));
  }

  @Override
  public final void unlink(Value<T> originalValue) {
    if (!linkedValues.containsKey(requireNonNull(originalValue, "originalValue"))) {
      throw new IllegalArgumentException("Values are not linked");
    }
    linkedValues.remove(originalValue).unlink();
  }

  @Override
  public final void link(ValueObserver<T> originalValueObserver) {
    set(requireNonNull(originalValueObserver, "originalValueObserver").get());
    originalValueObserver.addDataListener(originalValueListener);
  }

  @Override
  public final void unlink(ValueObserver<T> originalValueObserver) {
    requireNonNull(originalValueObserver).removeDataListener(originalValueListener);
  }

  @Override
  public final Set<Value<T>> linkedValues() {
    return unmodifiableSet(linkedValues.keySet());
  }

  @Override
  public final void addValidator(Validator<T> validator) {
    requireNonNull(validator, "validator").validate(get());
    validators.add(validator);
  }

  @Override
  public final void removeValidator(Validator<T> validator) {
    validators.remove(requireNonNull(validator));
  }

  @Override
  public final Collection<Validator<T>> validators() {
    return unmodifiableSet(validators);
  }

  /**
   * Sets the actual internal value.
   * @param value the value
   */
  protected abstract void setValue(T value);

  /**
   * @return the change observer to use when adding listeners to this value
   */
  protected EventObserver<T> changeObserver() {
    return changeEvent.observer();
  }

  /**
   * Fires the change event for this value, using the current value, indicating that
   * the underlying value has changed or at least that it may have changed
   */
  protected final void notifyValueChange() {
    changeEvent.onEvent(get());
  }
}
