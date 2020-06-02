/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity;

import is.codion.framework.domain.attribute.Attribute;

import static java.util.Objects.requireNonNull;

/**
 * Factory class for {@link ValueChange} instances
 */
public final class ValueChanges {

  private ValueChanges() {}

  /**
   * Returns a new {@link ValueChange} instance
   * @param attribute the attribute associated with the value
   * @param currentValue the current value
   * @param previousValue the previous value
   * @return a new {@link ValueChange} instance
   */
  public static ValueChange valueChange(final Attribute<?> attribute, final Object currentValue, final Object previousValue) {
    return valueChange(attribute, currentValue, previousValue, false);
  }

  /**
   * Returns a new {@link ValueChange} instance
   * @param attribute the attribute associated with the value
   * @param currentValue the current value
   * @param previousValue the previous value
   * @param initialization true if the value was being initialized
   * @return a new {@link ValueChange} instance
   */
  public static ValueChange valueChange(final Attribute<?> attribute, final Object currentValue, final Object previousValue,
                                        final boolean initialization) {
    return new DefaultValueChange(attribute, currentValue, previousValue, initialization);
  }

  private static final class DefaultValueChange implements ValueChange {

    /**
     * The Property identifying the changed value
     */
    private final Attribute<?> property;

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
     * @param attribute the attribute associated with the value
     * @param value the new value
     * @param previousValue the previous value
     * @param initialization true if the value is being initialized
     */
    private DefaultValueChange(final Attribute<?> attribute, final Object value, final Object previousValue,
                               final boolean initialization) {
      this.property = requireNonNull(attribute, "attribute");
      this.value = value;
      this.previousValue = previousValue;
      this.initialization = initialization;
    }

    @Override
    public Attribute<?> getAttribute() {
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
