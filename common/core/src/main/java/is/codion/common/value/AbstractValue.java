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

import static java.util.Collections.unmodifiableSet;
import static java.util.Objects.requireNonNull;

/**
 * An abstract {@link Value} implementation handling everything except the value itself.<br><br>
 * The constructor parameter {@code notifyOnSet} specifies whether this {@link Value} instance should automatically call
 * {@link #notifyValueChange()} when the value is changed via {@link AbstractValue#set(Object)}.
 * Some implementations may want to do this manually.
 * @param <T> the value type
 */
public abstract class AbstractValue<T> implements Value<T> {

  private final Event<T> changeEvent = Event.event();
  private final T nullValue;
  private final boolean notifyValueChange;
  private final Set<Validator<T>> validators = new LinkedHashSet<>(0);
  private final Map<Value<T>, ValueLink<T>> linkedValues = new LinkedHashMap<>(0);
  private final Consumer<T> originalValueListener = new OriginalValueListener();

  private ValueObserver<T> observer;

  protected AbstractValue() {
    this(null);
  }

  /**
   * Creates an {@link AbstractValue} instance, which does not notify on set.
   * @param nullValue the value to use instead of null
   */
  protected AbstractValue(T nullValue) {
    this(nullValue, false);
  }

  /**
   * Creates an {@link AbstractValue} instance.
   * @param nullValue the value to use instead of null
   * @param notifyValueChange specifies whether to automatically call {@link #notifyValueChange()} when the value is changed via {@link #set(Object)}
   */
  protected AbstractValue(T nullValue, boolean notifyValueChange) {
    this.nullValue = nullValue;
    this.notifyValueChange = notifyValueChange;
  }

  @Override
  public final void set(T value) {
    T newValue = value == null ? nullValue : value;
    for (Validator<T> validator : validators) {
      validator.validate(newValue);
    }
    T previousValue = get();
    setValue(newValue);
    if (notifyValueChange && !Objects.equals(previousValue, newValue)) {
      notifyValueChange();
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
  public final boolean nullable() {
    return nullValue == null;
  }

  @Override
  public final void accept(T data) {
    set(data);
  }

  @Override
  public final void addListener(Runnable listener) {
    changeEvent.addListener(listener);
  }

  @Override
  public final void removeListener(Runnable listener) {
    changeEvent.removeListener(listener);
  }

  @Override
  public final void addDataListener(Consumer<T> listener) {
    changeEvent.addDataListener(listener);
  }

  @Override
  public final void removeDataListener(Consumer<T> listener) {
    changeEvent.removeDataListener(listener);
  }

  @Override
  public final void addWeakListener(Runnable listener) {
    changeEvent.addWeakListener(listener);
  }

  @Override
  public final void removeWeakListener(Runnable listener) {
    changeEvent.removeWeakListener(listener);
  }

  @Override
  public final void addWeakDataListener(Consumer<T> listener) {
    changeEvent.addWeakDataListener(listener);
  }

  @Override
  public final void removeWeakDataListener(Consumer<T> listener) {
    changeEvent.removeWeakDataListener(listener);
  }

  @Override
  public final void link(Value<T> originalValue) {
    if (linkedValues.containsKey(requireNonNull(originalValue, "originalValue"))) {
      throw new IllegalStateException("Values are already linked");
    }
    linkedValues.put(originalValue, new ValueLink<>(this, originalValue));
  }

  @Override
  public final void unlink(Value<T> originalValue) {
    if (!linkedValues.containsKey(requireNonNull(originalValue, "originalValue"))) {
      throw new IllegalStateException("Values are not linked");
    }
    linkedValues.remove(originalValue).unlink();
  }

  @Override
  public final void link(ValueObserver<T> originalValue) {
    set(requireNonNull(originalValue, "originalValue").get());
    originalValue.addDataListener(originalValueListener);
  }

  @Override
  public final void unlink(ValueObserver<T> originalValue) {
    requireNonNull(originalValue).removeDataListener(originalValueListener);
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
   * Triggers the change event for this value, using the current value, indicating that
   * the underlying value has changed or at least that it may have changed
   */
  protected final void notifyValueChange() {
    changeEvent.accept(get());
  }

  private final class OriginalValueListener implements Consumer<T> {

    @Override
    public void accept(T value) {
      set(value);
    }
  }
}
