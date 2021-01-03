/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.value;

import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.requireNonNull;

/**
 * A class for linking two values.
 * @param <V> the type of the value
 */
final class ValueLink<V> {

  private final Value<V> linkedValue;
  private final Value<V> originalValue;

  /**
   * True while the linked value is being updated
   */
  private boolean isUpdatingLinked = false;

  /**
   * True while the original value is being updated
   */
  private boolean isUpdatingOriginal = false;

  /**
   * Instantiates a new ValueLink
   * @param linkedValue the value to link to the original value
   * @param originalValue the original value
   */
  ValueLink(final Value<V> linkedValue, final Value<V> originalValue) {
    if (requireNonNull(originalValue, "originalValue") == requireNonNull(linkedValue, "linkedValue")) {
      throw new IllegalArgumentException("A Value can not be linked to itself");
    }
    this.linkedValue = linkedValue;
    this.originalValue = originalValue;
    linkedValue.set(originalValue.get());
    originalValue.addListener(this::updateLinkedValue);
    linkedValue.addListener(this::updateOriginalValue);
    combineValidators(linkedValue, originalValue);
  }

  private void updateOriginalValue() {
    if (!isUpdatingLinked) {
      try {
        isUpdatingOriginal = true;
        try {
          originalValue.set(linkedValue.get());
        }
        catch (final IllegalArgumentException e) {
          linkedValue.set(originalValue.get());
          throw e;
        }
      }
      finally {
        isUpdatingOriginal = false;
      }
    }
  }

  private void updateLinkedValue() {
    if (!isUpdatingOriginal) {
      try {
        isUpdatingLinked = true;
        try {
          linkedValue.set(originalValue.get());
        }
        catch (final IllegalArgumentException e) {
          originalValue.set(linkedValue.get());
          throw e;
        }
      }
      finally {
        isUpdatingLinked = false;
      }
    }
  }

  private void combineValidators(final Value<V> linkedValue, final Value<V> originalValue) {
    try {
      final List<Value.Validator<V>> originalValidators = new ArrayList<>(originalValue.getValidators());
      final List<Value.Validator<V>> linkedValidators = new ArrayList<>(linkedValue.getValidators());
      originalValidators.forEach(linkedValue::addValidator);
      linkedValidators.forEach(originalValue::addValidator);
    }
    catch (final UnsupportedOperationException e) {/*Not supported, StateValue for example*/}
  }
}
