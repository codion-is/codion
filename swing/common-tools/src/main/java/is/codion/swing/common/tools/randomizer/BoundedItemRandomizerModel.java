/*
 * Copyright (c) 2010 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.tools.randomizer;

import java.util.Collection;
import java.util.Iterator;

import static java.util.Objects.requireNonNull;

/**
 * A ItemRandomizer with the added constraint that the total item weights can not exceed a defined maximum.
 * When the weight of one item is incremented the weight of another is decremented in a round-robin kind of fashion
 * and when an item weight is decremented the weight of another is incremented.<br>
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
  public BoundedItemRandomizerModel(Collection<T> items) {
    this(DEFAULT_BOUNDED_WEIGHT, items);
  }

  /**
   * Instantiates a new BoundedRandomItemModel with the given bounded weight.
   * @param boundedWeight the maximum total weight
   * @param items the items
   */
  public BoundedItemRandomizerModel(int boundedWeight, Collection<T> items) {
    if (boundedWeight <= 0) {
      throw new IllegalArgumentException("Bounded weight must be a positive integer");
    }
    if (requireNonNull(items, "items").isEmpty()) {
      throw new IllegalArgumentException("Items must not be empty");
    }

    this.weightBounds = boundedWeight;
    initializeItems(items);
    lastAffected = items().get(0);
  }

  public int weightBounds() {
    return weightBounds;
  }

  @Override
  public void incrementWeight(T item) {
    synchronized (lock) {
      RandomItem<T> randomItem = randomItem(item);
      if (randomItem.weight() >= weightBounds) {
        throw new IllegalStateException("Maximum weight reached");
      }

      decrementWeight(randomItem);
      randomItem(item).incrementWeight();
    }
  }

  @Override
  public void decrementWeight(T item) {
    synchronized (lock) {
      RandomItem<T> randomItem = randomItem(item);
      if (randomItem.weight() == 0) {
        throw new IllegalStateException("No weight to shed");
      }

      incrementWeight(randomItem);
      randomItem.decrementWeight();
    }
  }

  @Override
  public void setWeight(T item, int weight) {
    throw new UnsupportedOperationException("setWeight is not implemented in " + getClass().getSimpleName());
  }

  @Override
  public void addItem(T item, int weight) {
    throw new UnsupportedOperationException("addItem is not implemented in " + getClass().getSimpleName());
  }

  private void initializeItems(Collection<T> items) {
    int rest = weightBounds % items.size();
    int amountEach = weightBounds / items.size();
    Iterator<T> itemIterator = items.iterator();
    int i = 0;
    while (itemIterator.hasNext()) {
      super.addItem(itemIterator.next(), i++ < items.size() - 1 ? amountEach : amountEach + rest);
    }
  }

  private void incrementWeight(RandomItem<?> exclude) {
    lastAffected = nextItem(exclude, false);
    lastAffected.incrementWeight();
  }

  private void decrementWeight(RandomItem<?> exclude) {
    lastAffected = nextItem(exclude, true);
    lastAffected.decrementWeight();
  }

  private RandomItem<T> nextItem(RandomItem<?> exclude, boolean nonEmpty) {
    int index = items().indexOf(lastAffected);
    RandomItem<T> item = null;
    while (item == null || item.equals(exclude) || (nonEmpty ? item.weight() == 0 : item.weight() == weightBounds)) {
      if (index == 0) {
        index = items().size() - 1;
      }
      else {
        index--;
      }

      item = items().get(index);
    }

    return item;
  }
}
