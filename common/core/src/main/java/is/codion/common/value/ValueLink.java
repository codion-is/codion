/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.value;

import static java.util.Objects.requireNonNull;

/**
 * A class for linking two values.
 * @param <V> the type of the value
 */
final class ValueLink<V> {

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
   * @param originalValue the original value
   * @param linkedValue the value to link to the original value
   */
  ValueLink(final Value<V> originalValue, final Value<V> linkedValue) {
    if (requireNonNull(originalValue, "originalValue") == requireNonNull(linkedValue, "linkedValue")) {
      throw new IllegalArgumentException("A Value can not be linked to itself");
    }
    linkedValue.set(originalValue.get());
    bindEvents(originalValue, linkedValue);
  }

  private void bindEvents(final Value<V> originalValue, final Value<V> linkedValue) {
    originalValue.addListener(() -> updateLinkedValue(originalValue, linkedValue));
    linkedValue.addListener(() -> updateOriginalValue(originalValue, linkedValue));
  }

  private void updateOriginalValue(final Value<V> originalValue, final Value<V> linkedValue) {
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

  private void updateLinkedValue(final Value<V> originalValue, final Value<V> linkedValue) {
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
