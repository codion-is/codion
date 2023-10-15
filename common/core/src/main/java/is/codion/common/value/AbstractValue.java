/*
 * Copyright (c) 2019 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
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

import static java.util.Objects.requireNonNull;

/**
 * An abstract {@link Value} implementation handling everything except the value itself.<br><br>
 * The constructor parameter {@code notify} specifies whether this {@link Value} instance should automatically call
 * {@link #notifyListeners()} when the value is changed via {@link AbstractValue#set(Object)}.
 * Some implementations may want to do this manually.
 * @param <T> the value type
 */
public abstract class AbstractValue<T> implements Value<T> {

  private final Event<T> changeEvent = Event.event();
  private final T nullValue;
  private final Notify notify;
  private final Set<Validator<T>> validators = new LinkedHashSet<>(0);
  private final Map<Value<T>, ValueLink<T>> linkedValues = new LinkedHashMap<>(0);
  private final Consumer<T> originalValueListener = new OriginalValueListener();

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
    for (Validator<T> validator : validators) {
      validator.validate(newValue);
    }
    T previousValue = get();
    setValue(newValue);
    if (notify == Notify.WHEN_SET) {
      notifyListeners();
    }
    boolean valueChanged = !Objects.equals(previousValue, newValue);
    if (notify == Notify.WHEN_CHANGED && valueChanged) {
      notifyListeners();
    }

    return valueChanged;
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
  public final boolean nullable() {
    return nullValue == null;
  }

  @Override
  public final void accept(T data) {
    set(data);
  }

  @Override
  public final boolean addListener(Runnable listener) {
    return changeEvent.addListener(listener);
  }

  @Override
  public final boolean removeListener(Runnable listener) {
    return changeEvent.removeListener(listener);
  }

  @Override
  public final boolean addDataListener(Consumer<T> listener) {
    return changeEvent.addDataListener(listener);
  }

  @Override
  public final boolean removeDataListener(Consumer<T> listener) {
    return changeEvent.removeDataListener(listener);
  }

  @Override
  public final boolean addWeakListener(Runnable listener) {
    return changeEvent.addWeakListener(listener);
  }

  @Override
  public final boolean removeWeakListener(Runnable listener) {
    return changeEvent.removeWeakListener(listener);
  }

  @Override
  public final boolean addWeakDataListener(Consumer<T> listener) {
    return changeEvent.addWeakDataListener(listener);
  }

  @Override
  public final boolean removeWeakDataListener(Consumer<T> listener) {
    return changeEvent.removeWeakDataListener(listener);
  }

  @Override
  public final void link(Value<T> originalValue) {
    if (linkedValues.containsKey(requireNonNull(originalValue))) {
      throw new IllegalStateException("Values are already linked");
    }
    linkedValues.put(originalValue, new ValueLink<>(this, originalValue));
  }

  @Override
  public final void unlink(Value<T> originalValue) {
    if (!linkedValues.containsKey(requireNonNull(originalValue))) {
      throw new IllegalStateException("Values are not linked");
    }
    linkedValues.remove(originalValue).unlink();
  }

  @Override
  public final void link(ValueObserver<T> originalValue) {
    set(requireNonNull(originalValue).get());
    originalValue.addDataListener(originalValueListener);
  }

  @Override
  public final void unlink(ValueObserver<T> originalValue) {
    requireNonNull(originalValue).removeDataListener(originalValueListener);
  }

  @Override
  public final boolean addValidator(Validator<T> validator) {
    requireNonNull(validator, "validator").validate(get());
    return validators.add(validator);
  }

  @Override
  public final boolean removeValidator(Validator<T> validator) {
    return validators.remove(requireNonNull(validator));
  }

  @Override
  public final void validate(T value) {
    validators.forEach(validator -> validator.validate(value));
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
    changeEvent.accept(get());
  }

  final Set<Value<T>> linkedValues() {
    return linkedValues.keySet();
  }

  final Collection<Validator<T>> validators() {
    return validators;
  }

  private final class OriginalValueListener implements Consumer<T> {

    @Override
    public void accept(T value) {
      set(value);
    }
  }
}
