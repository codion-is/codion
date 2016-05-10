/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model.tools;

import org.jminor.common.Util;

import java.util.Collection;
import java.util.Iterator;

/**
 * A ItemRandomizer with the added constraint that the total item weights can not exceed a defined maximum.
 * When the weight of one item is incremented the weight of another is decremented in a round robin kind of fashion
 * and when a item weight is decremented the weight of another is incremented.<br>
 * User: Björn Darri<br>
 * Date: 6.4.2010<br>
 * Time: 21:26:00<br>
 * @param <T> the type of item this random item model returns
 */
public final class BoundedItemRandomizerModel<T> extends ItemRandomizerModel<T> {

  private static final int DEFAULT_BOUNDED_WEIGHT = 100;

  private final Object lock = new Object();
  private final int weightBounds;
  private RandomItem<T> lastAffected;

  /**
   * Instantiates a new BoundedRandomItemModel with a default bounded weight of 100.
   * @param items the items
   */
  public BoundedItemRandomizerModel(final Collection<T> items) {
    this(DEFAULT_BOUNDED_WEIGHT, items);
  }

  /**
   * Instantiates a new BoundedRandomItemModel with the given bounded weight.
   * @param boundedWeight the maximum total weight
   * @param items the items
   */
  public BoundedItemRandomizerModel(final int boundedWeight, final Collection<T> items) {
    if (boundedWeight <= 0) {
      throw new IllegalArgumentException("Bounded weight must be a positive integer");
    }
    Util.rejectNullValue(items, "items");
    if (items.isEmpty()) {
      throw new IllegalArgumentException("Items must not be empty");
    }

    this.weightBounds = boundedWeight;
    initializeItems(items);
    lastAffected = getItems().get(0);
  }

  public int getWeightBounds() {
    return weightBounds;
  }

  /** {@inheritDoc} */
  @Override
  public void incrementWeight(final T item) {
    synchronized (lock) {
      final RandomItem randomItem = getRandomItem(item);
      if (randomItem.getWeight() >= weightBounds) {
        throw new IllegalStateException("Maximum weight reached");
      }

      decrementWeight(randomItem);
      getRandomItem(item).incrementWeight();
    }
    fireWeightsChangedEvent();
  }

  /** {@inheritDoc} */
  @Override
  public void decrementWeight(final T item) {
    synchronized (lock) {
      final RandomItem<T> randomItem = getRandomItem(item);
      if (randomItem.getWeight() == 0) {
        throw new IllegalStateException("No weight to shed");
      }

      incrementWeight(randomItem);
      randomItem.decrementWeight();
    }
    fireWeightsChangedEvent();
  }

  /** {@inheritDoc} */
  @Override
  public void setWeight(final T item, final int weight) {
    throw new UnsupportedOperationException("setWeight is not implemented in " + getClass().getSimpleName());
  }

  /** {@inheritDoc} */
  @Override
  public void addItem(final T item, final int weight) {
    throw new UnsupportedOperationException("addItem is not implemented in " + getClass().getSimpleName());
  }

  private void initializeItems(final Collection<T> items) {
    final int rest = weightBounds % items.size();
    final int amountEach = weightBounds / items.size();
    final Iterator<T> itemIterator = items.iterator();
    int i = 0;
    while (itemIterator.hasNext()) {
      super.addItem(itemIterator.next(), i++ < items.size() - 1 ?  amountEach : amountEach + rest);
    }
  }

  private void incrementWeight(final RandomItem exclude) {
    lastAffected = getNextItem(exclude, false);
    lastAffected.incrementWeight();
  }

  private void decrementWeight(final RandomItem exclude) {
    lastAffected = getNextItem(exclude, true);
    lastAffected.decrementWeight();
  }

  private RandomItem<T> getNextItem(final RandomItem exclude, final boolean nonEmpty) {
    int index = getItems().indexOf(lastAffected);
    RandomItem<T> item = null;
    while (item == null || item.equals(exclude) || (nonEmpty ? item.getWeight() == 0 : item.getWeight() == weightBounds)) {
      if (index == 0) {
        index = getItems().size() - 1;
      }
      else {
        index--;
      }

      item = getItems().get(index);
    }

    return item;
  }
}
