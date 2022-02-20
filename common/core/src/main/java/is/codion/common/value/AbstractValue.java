/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.value;

import is.codion.common.event.Event;
import is.codion.common.event.EventDataListener;
import is.codion.common.event.EventListener;
import is.codion.common.event.EventObserver;

import java.util.Collection;
import java.util.LinkedHashSet;
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
  private final Set<Value<T>> linkedValues = new LinkedHashSet<>();

  private ValueObserver<T> observer;

  protected AbstractValue() {
    this(null);
  }

  protected AbstractValue(final T nullValue) {
    this(nullValue, NotifyOnSet.NO);
  }

  protected AbstractValue(final T nullValue, final NotifyOnSet notifyOnSet) {
    this.nullValue = nullValue;
    this.notifyOnSet = notifyOnSet == NotifyOnSet.YES;
  }

  @Override
  public final void set(final T value) {
    final T actualValue = value == null ? nullValue : value;
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
  public final boolean equalTo(final T value) {
    return Objects.equals(get(), value);
  }

  @Override
  public final void onEvent(final T data) {
    set(data);
  }

  @Override
  public final void addListener(final EventListener listener) {
    getChangeObserver().addListener(listener);
  }

  @Override
  public final void removeListener(final EventListener listener) {
    getChangeObserver().removeListener(listener);
  }

  @Override
  public final void addDataListener(final EventDataListener<T> listener) {
    getChangeObserver().addDataListener(listener);
  }

  @Override
  public final void removeDataListener(final EventDataListener<T> listener) {
    getChangeObserver().removeDataListener(listener);
  }

  @Override
  public final void link(final Value<T> originalValue) {
    if (linkedValues.contains(requireNonNull(originalValue, "originalValue"))) {
      throw new IllegalArgumentException("Values are already linked");
    }
    new ValueLink<>(this, originalValue);
    linkedValues.add(originalValue);
  }

  @Override
  public final void link(final ValueObserver<T> originalValueObserver) {
    set(requireNonNull(originalValueObserver, "originalValueObserver").get());
    originalValueObserver.addDataListener(this::set);
  }

  @Override
  public final Set<Value<T>> getLinkedValues() {
    return unmodifiableSet(linkedValues);
  }

  @Override
  public final void addValidator(final Validator<T> validator) {
    requireNonNull(validator, "validator").validate(get());
    validators.add(validator);
  }

  @Override
  public final Collection<Validator<T>> getValidators() {
    return unmodifiableSet(validators);
  }

  /**
   * Sets the actual internal value.
   * @param value the value
   */
  protected abstract void setValue(final T value);

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
