/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.value;

import java.util.Set;

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
    preventLinkCycle(linkedValue, originalValue);
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
    final LinkedValidator<V> originalValidators = new LinkedValidator<>(originalValue);
    final LinkedValidator<V> linkedValidators = new LinkedValidator<>(linkedValue);
    originalValidators.excluded = linkedValidators;
    linkedValidators.excluded = originalValidators;
    linkedValue.addValidator(originalValidators);
    originalValue.addValidator(linkedValidators);
  }

  private static <V> void preventLinkCycle(final Value<V> linkedValue, final Value<V> originalValue) {
    if (originalValue == linkedValue) {
      throw new IllegalArgumentException("A Value can not be linked to itself");
    }
    final Set<Value<V>> linkedValues = originalValue.getLinkedValues();
    if (linkedValues.contains(linkedValue)) {
      throw new IllegalStateException("Cyclical value link detected");
    }
    linkedValues.forEach(value -> preventLinkCycle(value, originalValue));
  }

  private static final class LinkedValidator<V> implements Value.Validator<V> {

    private final Value<V> linkedValue;

    private Value.Validator<V> excluded;

    private LinkedValidator(final Value<V> linkedValue) {
      this.linkedValue = linkedValue;
    }

    @Override
    public void validate(final V value) throws IllegalArgumentException {
      linkedValue.getValidators().stream()
              .filter(validator -> validator != excluded)
              .forEach(validator -> validator.validate(value));
    }
  }
}
