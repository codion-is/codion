/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
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
import java.util.Optional;
import java.util.Set;

import static java.util.Collections.unmodifiableSet;
import static java.util.Objects.requireNonNull;

/**
 * A base Value implementation handling everything except the value itself.
 * @param <T> the value type
 */
public abstract class AbstractValue<T> implements Value<T> {

  public enum NotifyOnSet {
    YES, NO
  }

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

  protected AbstractValue(T nullValue) {
    this(nullValue, NotifyOnSet.NO);
  }

  protected AbstractValue(T nullValue, NotifyOnSet notifyOnSet) {
    this.nullValue = nullValue;
    this.notifyOnSet = notifyOnSet == NotifyOnSet.YES;
  }

  @Override
  public final void set(T value) {
    T actualValue = value == null ? nullValue : value;
    validators.forEach(validator -> validator.validate(actualValue));
    if (!Objects.equals(get(), actualValue)) {
      setValue(actualValue);
      if (notifyOnSet) {
        notifyValueChange();
      }
    }
  }

  @Override
  public final ValueObserver<T> getObserver() {
    synchronized (changeEvent) {
      if (observer == null) {
        observer = new DefaultValueObserver<>(this);
      }

      return observer;
    }
  }

  @Override
  public final Optional<T> toOptional() {
    if (isNullable()) {
      return Optional.ofNullable(get());
    }

    return Optional.of(get());
  }

  @Override
  public final boolean isNullable() {
    return nullValue == null;
  }

  @Override
  public final boolean isNull() {
    return get() == null;
  }

  @Override
  public final boolean isNotNull() {
    return !isNull();
  }

  @Override
  public final boolean equalTo(T value) {
    return Objects.equals(get(), value);
  }

  @Override
  public final void onEvent(T data) {
    set(data);
  }

  @Override
  public final void addListener(EventListener listener) {
    getChangeObserver().addListener(listener);
  }

  @Override
  public final void removeListener(EventListener listener) {
    getChangeObserver().removeListener(listener);
  }

  @Override
  public final void addDataListener(EventDataListener<T> listener) {
    getChangeObserver().addDataListener(listener);
  }

  @Override
  public final void removeDataListener(EventDataListener<T> listener) {
    getChangeObserver().removeDataListener(listener);
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
  public final Set<Value<T>> getLinkedValues() {
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
  public final Collection<Validator<T>> getValidators() {
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
  protected EventObserver<T> getChangeObserver() {
    return changeEvent.getObserver();
  }

  /**
   * Fires the change event for this value, using the current value, indicating that
   * the underlying value has changed or at least that it may have changed
   */
  protected final void notifyValueChange() {
    changeEvent.onEvent(get());
  }
}
