/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.value;

import is.codion.common.event.EventListener;

import java.util.Set;

/**
 * A class for linking two values.
 * @param <T> the type of the value
 */
final class ValueLink<T> {

  private final Value<T> linkedValue;
  private final Value<T> originalValue;

  private final EventListener updateLinkedValueListener = new UpdateLinkedValueListener();
  private final EventListener updateOriginalValueListener = new UpdateOriginalValueListener();

  private final LinkedValidator<T> linkedValidator;
  private final LinkedValidator<T> originalValidator;

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
  ValueLink(Value<T> linkedValue, Value<T> originalValue) {
    preventLinkCycle(linkedValue, originalValue);
    this.linkedValue = linkedValue;
    this.originalValue = originalValue;
    this.linkedValidator = new LinkedValidator<>(linkedValue);
    this.originalValidator = new LinkedValidator<>(originalValue);
    this.linkedValidator.excluded = originalValidator;
    this.originalValidator.excluded = linkedValidator;
    linkedValue.set(originalValue.get());
    originalValue.addListener(updateLinkedValueListener);
    linkedValue.addListener(updateOriginalValueListener);
    originalValue.addValidator(linkedValidator);
    linkedValue.addValidator(originalValidator);
  }

  void unlink() {
    linkedValue.removeListener(updateOriginalValueListener);
    originalValue.removeListener(updateLinkedValueListener);
    linkedValue.removeValidator(originalValidator);
    originalValue.removeValidator(linkedValidator);
  }

  private static <T> void preventLinkCycle(Value<T> linkedValue, Value<T> originalValue) {
    if (originalValue == linkedValue) {
      throw new IllegalArgumentException("A Value can not be linked to itself");
    }
    Set<Value<T>> linkedValues = originalValue.getLinkedValues();
    if (linkedValues.contains(linkedValue)) {
      throw new IllegalStateException("Cyclical value link detected");
    }
    linkedValues.forEach(value -> preventLinkCycle(value, originalValue));
  }

  private final class UpdateLinkedValueListener implements EventListener {

    @Override
    public void onEvent() {
      updateLinkedValue();
    }

    private void updateLinkedValue() {
      if (!isUpdatingOriginal) {
        try {
          isUpdatingLinked = true;
          try {
            linkedValue.set(originalValue.get());
          }
          catch (IllegalArgumentException e) {
            originalValue.set(linkedValue.get());
            throw e;
          }
        }
        finally {
          isUpdatingLinked = false;
        }
      }
    }
  }

  private final class UpdateOriginalValueListener implements EventListener {

    @Override
    public void onEvent() {
      updateOriginalValue();
    }

    private void updateOriginalValue() {
      if (!isUpdatingLinked) {
        try {
          isUpdatingOriginal = true;
          try {
            originalValue.set(linkedValue.get());
          }
          catch (IllegalArgumentException e) {
            linkedValue.set(originalValue.get());
            throw e;
          }
        }
        finally {
          isUpdatingOriginal = false;
        }
      }
    }
  }

  private static final class LinkedValidator<T> implements Value.Validator<T> {

    private final Value<T> linkedValue;

    private Value.Validator<T> excluded;

    private LinkedValidator(Value<T> linkedValue) {
      this.linkedValue = linkedValue;
    }

    @Override
    public void validate(T value) throws IllegalArgumentException {
      linkedValue.getValidators().stream()
              .filter(validator -> validator != excluded)
              .forEach(validator -> validator.validate(value));
    }
  }
}
