/*
 * Copyright (c) 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.tools.randomizer;

/**
 * A class encapsulating an Object item and an integer weight value.
 */
final class DefaultRandomItem<T> implements ItemRandomizer.RandomItem<T> {

  private static final String WEIGHT_CAN_NOT_BE_NEGATIVE = "Weight can not be negative";

  private final T item;
  private int weight;
  private boolean enabled = true;

  /**
   * Instantiates a new RandomItem
   * @param item the item
   * @param weight the random selection weight to assign to this item
   */
  DefaultRandomItem(T item, int weight) {
    if (weight < 0) {
      throw new IllegalArgumentException(WEIGHT_CAN_NOT_BE_NEGATIVE);
    }
    this.item = item;
    this.weight = weight;
  }

  @Override
  public int weight() {
    return enabled ? weight : 0;
  }

  @Override
  public boolean isEnabled() {
    return enabled;
  }

  @Override
  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  @Override
  public T item() {
    return item;
  }

  @Override
  public String toString() {
    return item.toString();
  }

  @Override
  public boolean equals(Object obj) {
    return obj instanceof ItemRandomizer.RandomItem && (((ItemRandomizer.RandomItem<T>) obj).item().equals(item));
  }

  @Override
  public int hashCode() {
    return item.hashCode();
  }

  @Override
  public void incrementWeight() {
    weight++;
  }

  @Override
  public void decrementWeight() {
    if (weight == 0) {
      throw new IllegalStateException(WEIGHT_CAN_NOT_BE_NEGATIVE);
    }

    weight--;
  }

  @Override
  public void setWeight(int weight) {
    if (weight < 0) {
      throw new IllegalArgumentException(WEIGHT_CAN_NOT_BE_NEGATIVE);
    }

    this.weight = weight;
  }
}
