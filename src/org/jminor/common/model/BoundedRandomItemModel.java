/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model;

/**
 * A RandomItemModel with the added constraint that the total item weights can not exceed a defined maximum.
 * When the weight of one item is incremented the weight of another is decremented in a round robin kind of fashion
 * and when a item weight is decremented the weight of another is incremented.
 * User: Björn Darri
 * Date: 6.4.2010
 * Time: 21:26:00
 */
public class BoundedRandomItemModel extends RandomItemModel {

  private final int weightBounds;
  private RandomItem lastAffected;

  /**
   * Instantiates a new BoundedRandomItemModel with a default bounded weight of 100.
   * @param items the items
   */
  public BoundedRandomItemModel(final Object... items) {
    this(100, items);
  }

  /**
   * Instantiates a new BoundedRandomItemModel with the given bounded weight.
   * @param boundedWeight the maximum total weight
   * @param items the items
   */
  public BoundedRandomItemModel(final int boundedWeight, final Object... items) {
    if (boundedWeight <= 0)
      throw new IllegalArgumentException("Bounded weight must be a positive integer");
    if (items == null || items.length == 0)
      throw new IllegalArgumentException("Items must not be null or empty");

    this.weightBounds = boundedWeight;
    initializeItems(items);
    lastAffected = this.items.get(0);
  }

  public int getWeightBounds() {
    return weightBounds;
  }

  @Override
  public void increment(final Object item) {
    synchronized (items) {
      final RandomItem randomItem = getRandomItem(item);
      if (randomItem.getWeight() >= weightBounds)
        throw new RuntimeException("Maximum weight reached");

      decrementWeight(randomItem);
      getRandomItem(item).increment();
    }
    evtWeightsChanged.fire();
  }

  @Override
  public void decrement(final Object item) {
    synchronized (items) {
      final RandomItem randomItem = getRandomItem(item);
      if (randomItem.getWeight() == 0)
        throw new RuntimeException("No weight to shed");

      incrementWeight(randomItem);
      randomItem.decrement();
    }
    evtWeightsChanged.fire();
  }

  @Override
  public void setWeight(final Object item, final int weight) {
    throw new RuntimeException("setWeigth is not implemented in the " + getClass().getSimpleName());
  }

  @Override
  public void addItem(final Object item, final int weight) {
    throw new RuntimeException("addItem is not implemented in the BoundedRandomItemModel " + getClass().getSimpleName());
  }

  protected void initializeItems(final Object... items) {
    final int rest = weightBounds % items.length;
    final int amountEach = weightBounds / items.length;
    for (int i = 0; i < items.length; i++)
      super.addItem(items[i], i < items.length - 1 ? amountEach : amountEach + rest);
  }

  private void incrementWeight(final RandomItem exclude) {
    lastAffected = getNextItem(exclude, false);
    lastAffected.increment();
  }

  private void decrementWeight(final RandomItem exclude) {
    lastAffected = getNextItem(exclude, true);
    lastAffected.decrement();
  }

  private RandomItem getNextItem(final RandomItem exclude, final boolean nonEmpty) {
    int index = items.indexOf(lastAffected);
    RandomItem item = null;
    while (item == null || item == exclude || (nonEmpty ? item.getWeight() == 0 : item.getWeight() == weightBounds)) {
      if (index == 0)
        index = items.size() - 1;
      else
        index--;

      item = items.get(index);
    }

    return item;
  }
}
