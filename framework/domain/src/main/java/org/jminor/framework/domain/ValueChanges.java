/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.domain;

import org.jminor.framework.domain.property.Property;

import static java.util.Objects.requireNonNull;

/**
 * Factory class for {@link Entity.ValueChange} instances
 */
public final class ValueChanges {

  private ValueChanges() {}

  /**
   * Returns a new {@link Entity.ValueChange} instance
   * @param property the Property associated with the value
   * @param currentValue the current value
   * @param previousValue the previous value
   * @return a new {@link Entity.ValueChange} instance
   */
  public static Entity.ValueChange valueChange(final Property property, final Object currentValue, final Object previousValue) {
    return valueChange(property, currentValue, previousValue, false);
  }

  /**
   * Returns a new {@link Entity.ValueChange} instance
   * @param property the Property associated with the value
   * @param currentValue the current value
   * @param previousValue the previous value
   * @param initialization true if the value was being initialized
   * @return a new {@link Entity.ValueChange} instance
   */
  public static Entity.ValueChange valueChange(final Property property, final Object currentValue, final Object previousValue,
                                               final boolean initialization) {
    return new DefaultValueChange(property, currentValue, previousValue, initialization);
  }

  private static final class DefaultValueChange implements Entity.ValueChange {

    /**
     * The Property identifying the changed value
     */
    private final Property property;

    /**
     * The new value
     */
    private final Object value;

    /**
     * The old value
     */
    private final Object previousValue;

    /**
     * True if this value change indicates an initialization, that is, a value was not present before this value change
     */
    private final boolean initialization;

    /**
     * Instantiates a new DefaultValueChange
     * @param property the Property associated with the value
     * @param value the new value
     * @param previousValue the previous value
     * @param initialization true if the value is being initialized
     */
    private DefaultValueChange(final Property property, final Object value, final Object previousValue,
                               final boolean initialization) {
      this.property = requireNonNull(property, "key");
      this.value = value;
      this.previousValue = previousValue;
      this.initialization = initialization;
    }

    @Override
    public Property getProperty() {
      return property;
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
    public boolean isInitialization() {
      return initialization;
    }

    @Override
    public String toString() {
      if (initialization) {
        return property + ": " + value;
      }
      else {
        return property + ": " + previousValue + " -> " + value;
      }
    }
  }
}
