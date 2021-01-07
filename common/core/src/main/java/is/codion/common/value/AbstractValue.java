/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.value;

import is.codion.common.event.Event;
import is.codion.common.event.EventDataListener;
import is.codion.common.event.EventListener;
import is.codion.common.event.Events;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static java.util.Collections.unmodifiableSet;
import static java.util.Objects.requireNonNull;

/**
 * A base Value implementation handling everything except the value itself.
 * @param <V> the value type
 */
public abstract class AbstractValue<V> implements Value<V> {

  public enum NotifyOnSet {
    YES, NO
  }

  private final Event<V> changeEvent = Events.event();
  private final V nullValue;
  private final boolean notifyOnSet;
  private final Set<Validator<V>> validators = new LinkedHashSet<>(0);

  public AbstractValue() {
    this(null);
  }

  public AbstractValue(final V nullValue) {
    this(nullValue, NotifyOnSet.NO);
  }

  public AbstractValue(final V nullValue, final NotifyOnSet notifyOnSet) {
    this.nullValue = nullValue;
    this.notifyOnSet = notifyOnSet == NotifyOnSet.YES;
  }

  @Override
  public final void set(final V value) {
    final V actualValue = value == null ? nullValue : value;
    validators.forEach(validator -> validator.validate(actualValue));
    if (!Objects.equals(get(), actualValue)) {
      doSet(actualValue);
      if (notifyOnSet) {
        notifyValueChange();
      }
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
  public final boolean is(final V value) {
    return Objects.equals(get(), value);
  }

  @Override
  public final void addListener(final EventListener listener) {
    changeEvent.addListener(listener);
  }

  @Override
  public final void removeListener(final EventListener listener) {
    changeEvent.removeListener(listener);
  }

  @Override
  public final void addDataListener(final EventDataListener<V> listener) {
    changeEvent.addDataListener(listener);
  }

  @Override
  public final void removeDataListener(final EventDataListener<V> listener) {
    changeEvent.removeDataListener(listener);
  }

  @Override
  public final void link(final Value<V> originalValue) {
    new ValueLink<>(this, originalValue);
  }

  @Override
  public final void link(final ValueObserver<V> originalValueObserver) {
    set(requireNonNull(originalValueObserver, "originalValueObserver").get());
    originalValueObserver.addDataListener(this::set);
  }

  @Override
  public final void addValidator(final Validator<V> validator) {
    requireNonNull(validator, "validator").validate(get());
    validators.add(validator);
  }

  @Override
  public final Collection<Validator<V>> getValidators() {
    return unmodifiableSet(validators);
  }

  /**
   * Sets the actual internal value.
   * @param value the value
   */
  protected abstract void doSet(final V value);

  /**
   * Fires the change event for this value, using the current value, indicating that
   * the underlying value has changed or at least that it may have changed
   */
  protected final void notifyValueChange() {
    changeEvent.onEvent(get());
  }
}
