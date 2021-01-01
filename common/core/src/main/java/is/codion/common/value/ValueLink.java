/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.value;

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
  }

  private void updateOriginalValue() {
    if (!isUpdatingLinked) {
      try {
        isUpdatingOriginal = true;
        originalValue.set(linkedValue.get());
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
        linkedValue.set(originalValue.get());
      }
      finally {
        isUpdatingLinked = false;
      }
    }
  }
}
