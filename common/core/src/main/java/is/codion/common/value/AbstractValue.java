/*
 * Copyright (c) 2019 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.value;

import is.codion.common.event.Event;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

import static is.codion.common.value.Value.Notify.WHEN_CHANGED;
import static is.codion.common.value.Value.Notify.WHEN_SET;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Objects.requireNonNull;

/**
 * An abstract {@link Value} implementation handling everything except the value itself.<br><br>
 * The constructor parameter {@code notify} specifies whether this {@link Value} instance should automatically call
 * {@link #notifyListeners()} when the value is set or changed via {@link AbstractValue#set(Object)}.
 * Some implementations may want to handle this manually.
 * @param <T> the value type
 */
public abstract class AbstractValue<T> implements Value<T> {

  private final T nullValue;
  private final Notify notify;

  private Event<T> changeEvent;
  private Set<Validator<T>> validators;
  private Map<Value<T>, ValueLink<T>> linkedValues;
  private Consumer<T> originalValueListener;
  private ValueObserver<T> observer;

  protected AbstractValue() {
    this(null);
  }

  /**
   * Creates an {@link AbstractValue} instance, which does not notify listeners.
   * @param nullValue the value to use instead of null
   */
  protected AbstractValue(T nullValue) {
    this.nullValue = nullValue;
    this.notify = null;
  }

  /**
   * Creates an {@link AbstractValue} instance.
   * @param nullValue the value to use instead of null
   * @param notify specifies when to notify listeners
   */
  protected AbstractValue(T nullValue, Notify notify) {
    this.nullValue = nullValue;
    this.notify = requireNonNull(notify);
  }

  @Override
  public final boolean set(T value) {
    T newValue = value == null ? nullValue : value;
    for (Validator<T> validator : validators()) {
      validator.validate(newValue);
    }
    T previousValue = get();
    setValue(newValue);

    return notifyListeners(!Objects.equals(previousValue, newValue));
  }

  @Override
  public final boolean map(Function<T, T> mapper) {
    return set(requireNonNull(mapper).apply(get()));
  }

  @Override
  public synchronized final ValueObserver<T> observer() {
    if (observer == null) {
      observer = new DefaultValueObserver<>(this);
    }

    return observer;
  }

  @Override
  public final boolean nullable() {
    return nullValue == null;
  }

  @Override
  public final void accept(T data) {
    set(data);
  }

  @Override
  public final boolean addListener(Runnable listener) {
    return changeEvent().addListener(listener);
  }

  @Override
  public final boolean removeListener(Runnable listener) {
    if (changeEvent != null) {
      return changeEvent.removeListener(listener);
    }

    return false;
  }

  @Override
  public final boolean addDataListener(Consumer<T> listener) {
    return changeEvent().addDataListener(listener);
  }

  @Override
  public final boolean removeDataListener(Consumer<T> listener) {
    if (changeEvent != null) {
      return changeEvent.removeDataListener(listener);
    }

    return false;
  }

  @Override
  public final boolean addWeakListener(Runnable listener) {
    return changeEvent().addWeakListener(listener);
  }

  @Override
  public final boolean removeWeakListener(Runnable listener) {
    if (changeEvent != null) {
      return changeEvent.removeWeakListener(listener);
    }

    return false;
  }

  @Override
  public final boolean addWeakDataListener(Consumer<T> listener) {
    return changeEvent().addWeakDataListener(listener);
  }

  @Override
  public final boolean removeWeakDataListener(Consumer<T> listener) {
    if (changeEvent != null) {
      return changeEvent.removeWeakDataListener(listener);
    }

    return false;
  }

  @Override
  public final void link(Value<T> originalValue) {
    requireNonNull(originalValue);
    if (linkedValues == null) {
      linkedValues = new LinkedHashMap<>(1);
    }
    if (linkedValues.containsKey(originalValue)) {
      throw new IllegalStateException("Values are already linked");
    }
    linkedValues.put(originalValue, new ValueLink<>(this, originalValue));
  }

  @Override
  public final void unlink(Value<T> originalValue) {
    requireNonNull(originalValue);
    if (linkedValues != null) {
      if (!linkedValues.containsKey(originalValue)) {
        throw new IllegalStateException("Values are not linked");
      }
      linkedValues.remove(originalValue).unlink();
    }
  }

  @Override
  public final void link(ValueObserver<T> originalValue) {
    requireNonNull(originalValue);
    if (originalValueListener == null) {
      originalValueListener = new OriginalValueListener();
    }
    set(originalValue.get());
    originalValue.addDataListener(originalValueListener);
  }

  @Override
  public final void unlink(ValueObserver<T> originalValue) {
    requireNonNull(originalValue);
    if (originalValueListener != null) {
      originalValue.removeDataListener(originalValueListener);
    }
  }

  @Override
  public final boolean addValidator(Validator<T> validator) {
    requireNonNull(validator, "validator").validate(get());
    if (validators == null) {
      validators = new LinkedHashSet<>(1);
    }

    return validators.add(validator);
  }

  @Override
  public final boolean removeValidator(Validator<T> validator) {
    requireNonNull(validator, "validator");
    if (validators != null) {
      return validators.remove(validator);
    }

    return false;
  }

  @Override
  public final void validate(T value) {
    validators().forEach(validator -> validator.validate(value));
  }

  /**
   * Sets the actual internal value.
   * @param value the value
   */
  protected abstract void setValue(T value);

  /**
   * Notifies listeners that the underlying value has changed or at least that it may have changed
   */
  protected final void notifyListeners() {
    if (changeEvent != null) {
      changeEvent.accept(get());
    }
  }

  final Set<Value<T>> linkedValues() {
    return linkedValues == null ? emptySet() : linkedValues.keySet();
  }

  final Collection<Validator<T>> validators() {
    return validators == null ? emptyList() : validators;
  }

  private boolean notifyListeners(boolean changed) {
    if (notify == WHEN_SET || (notify == WHEN_CHANGED && changed)){
      notifyListeners();
    }

    return changed;
  }

  private synchronized Event<T> changeEvent() {
    if (changeEvent == null) {
      changeEvent = Event.event();
    }

    return changeEvent;
  }

  private final class OriginalValueListener implements Consumer<T> {

    @Override
    public void accept(T value) {
      set(value);
    }
  }
}
