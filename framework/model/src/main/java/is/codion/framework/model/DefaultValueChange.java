/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.model;

import is.codion.framework.domain.entity.Attribute;

import static java.util.Objects.requireNonNull;

final class DefaultValueChange implements ValueChange {

  /**
   * The attribute associated with the changed value
   */
  private final Attribute<?> attribute;

  /**
   * The new value
   */
  private final Object value;

  /**
   * The old value
   */
  private final Object previousValue;

  /**
   * Instantiates a new DefaultValueChange
   * @param attribute the attribute associated with the value
   * @param value the new value
   * @param previousValue the previous value
   */
  DefaultValueChange(final Attribute<?> attribute, final Object value, final Object previousValue) {
    this.attribute = requireNonNull(attribute, "attribute");
    this.value = value;
    this.previousValue = previousValue;
  }

  @Override
  public Attribute<?> getAttribute() {
    return attribute;
  }

  @Override
  public Object getValue() {
    return value;
  }

  @Override
  public Object getPreviousValue() {
    return previousValue;
  }

  @Override
  public String toString() {
    return attribute + ": " + previousValue + " -> " + value;
  }
}
