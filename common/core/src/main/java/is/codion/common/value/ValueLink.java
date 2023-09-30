/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2019 - 2023, Björn Darri Sigurðsson.
 */
package is.codion.common.value;

import java.util.Set;

/**
 * A class for linking two values.
 * @param <T> the type of the value
 */
final class ValueLink<T> {

  private final Value<T> linkedValue;
  private final Value<T> originalValue;

  private final Runnable updateLinkedValue = new UpdateLinkedValue();
  private final Runnable updateOriginalValue = new UpdateOriginalValue();

  private final LinkedValidator<T> linkedValidator;
  private final LinkedValidator<T> originalValidator;

  /**
   * True while the linked value is being updated
   */
  private boolean updatingLinked = false;

  /**
   * True while the original value is being updated
   */
  private boolean updatingOriginal = false;

  /**
   * Creates a new ValueLink
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
    originalValue.addListener(updateLinkedValue);
    linkedValue.addListener(updateOriginalValue);
    originalValue.addValidator(linkedValidator);
    linkedValue.addValidator(originalValidator);
  }

  void unlink() {
    linkedValue.removeListener(updateOriginalValue);
    originalValue.removeListener(updateLinkedValue);
    linkedValue.removeValidator(originalValidator);
    originalValue.removeValidator(linkedValidator);
  }

  private static <T> void preventLinkCycle(Value<T> linkedValue, Value<T> originalValue) {
    if (originalValue == linkedValue) {
      throw new IllegalArgumentException("A Value can not be linked to itself");
    }
    Set<Value<T>> linkedValues = originalValue.linkedValues();
    if (linkedValues.contains(linkedValue)) {
      throw new IllegalStateException("Cyclical value link detected");
    }
    linkedValues.forEach(value -> preventLinkCycle(value, originalValue));
  }

  private final class UpdateLinkedValue implements Runnable {

    @Override
    public void run() {
      updateLinkedValue();
    }

    private void updateLinkedValue() {
      if (!updatingOriginal) {
        try {
          updatingLinked = true;
          try {
            linkedValue.set(originalValue.get());
          }
          catch (RuntimeException e) {
            originalValue.set(linkedValue.get());
            throw e;
          }
        }
        finally {
          updatingLinked = false;
        }
      }
    }
  }

  private final class UpdateOriginalValue implements Runnable {

    @Override
    public void run() {
      updateOriginalValue();
    }

    private void updateOriginalValue() {
      if (!updatingLinked) {
        try {
          updatingOriginal = true;
          try {
            originalValue.set(linkedValue.get());
          }
          catch (RuntimeException e) {
            linkedValue.set(originalValue.get());
            throw e;
          }
        }
        finally {
          updatingOriginal = false;
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
      linkedValue.validators().stream()
              .filter(validator -> validator != excluded)
              .forEach(validator -> validator.validate(value));
    }
  }
}
