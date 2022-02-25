/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.model;

import is.codion.framework.domain.entity.Attribute;

import static java.util.Objects.requireNonNull;

final class DefaultValueChange<T> implements ValueChange<T> {

  /**
   * The attribute associated with the changed value
   */
  private final Attribute<T> attribute;

  /**
   * The new value
   */
  private final T value;

  /**
   * The old value
   */
  private final T previousValue;

  /**
   * Instantiates a new DefaultValueChange
   * @param attribute the attribute associated with the value
   * @param value the new value
   * @param previousValue the previous value
   */
  DefaultValueChange(Attribute<T> attribute, T value, T previousValue) {
    this.attribute = requireNonNull(attribute, "attribute");
    this.value = value;
    this.previousValue = previousValue;
  }

  @Override
  public Attribute<T> getAttribute() {
    return attribute;
  }

  @Override
  public T getValue() {
    return value;
  }

  @Override
  public T getPreviousValue() {
    return previousValue;
  }

  @Override
  public String toString() {
    return attribute + ": " + previousValue + " -> " + value;
  }
}
