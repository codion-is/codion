/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.value;

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
   * @param oneWay if true then this link will be uni-directional, that is, changes in
   * the linked value do not trigger a change in the original value
   */
  ValueLink(final Value<V> originalValue, final Value<V> linkedValue, final boolean oneWay) {
    requireNonNull(originalValue, "originalValue");
    requireNonNull(linkedValue, "linkedValue");
    linkedValue.set(originalValue.get());
    bindEvents(originalValue, linkedValue, oneWay);
  }

  private void bindEvents(final Value<V> originalValue, final Value<V> linkedValue, final boolean oneWay) {
    originalValue.addListener(() -> updateLinkedValue(originalValue, linkedValue));
    if (!oneWay) {
      linkedValue.addListener(() -> updateOriginalValue(originalValue, linkedValue));
    }
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
