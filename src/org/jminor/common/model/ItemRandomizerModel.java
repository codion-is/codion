/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * A default ItemRandomizer implementation.
 */
public class ItemRandomizerModel<T> implements ItemRandomizer<T> {

  /**
   * An Event fired when a weight value has changed
   */
  private final Event evtWeightsChanged = Events.event();

  /**
   * The items contained in this model
   */
  private final List<ItemRandomizer.RandomItem<T>> items = new ArrayList<ItemRandomizer.RandomItem<T>>();

  private final Random random = new Random();

  /**
   * Instantiates a new empty RandomItemModel.
   */
  public ItemRandomizerModel() {
    this(0);
  }

  /**
   * Instantiates a new RandomItemModel with the given items.
   * @param defaultWeight the default weight to assign to each intial item
   * @param items the items to add to this model
   */
  public ItemRandomizerModel(final int defaultWeight, final T... items) {
    if (items != null) {
      for (final T item : items) {
        this.items.add(new DefaultRandomItem<T>(item, defaultWeight));
      }
    }
  }

  /** {@inheritDoc} */
  public void addItem(final T item, final int weight) {
    items.add(new DefaultRandomItem<T>(item, weight));
  }

  /** {@inheritDoc} */
  public void incrementWeight(final T item) {
    getRandomItem(item).incrementWeight();
    evtWeightsChanged.fire();
  }

  /** {@inheritDoc} */
  public void decrementWeight(final T item) {
    getRandomItem(item).decrementWeight();
    evtWeightsChanged.fire();
  }

  /** {@inheritDoc} */
  public void setWeight(final T item, final int weight) {
    getRandomItem(item).setWeight(weight);
    evtWeightsChanged.fire();
  }

  /** {@inheritDoc} */
  public final void addItem(final T item) {
    addItem(item, 0);
  }

  /** {@inheritDoc} */
  public final List<ItemRandomizer.RandomItem<T>> getItems() {
    return items;
  }

  /** {@inheritDoc} */
  public final int getItemCount() {
    return items.size();
  }

  /** {@inheritDoc} */
  public final EventObserver getWeightsObserver() {
    return evtWeightsChanged.getObserver();
  }

  /** {@inheritDoc} */
  public final T getRandomItem() {
    final int totalWeights = getTotalWeights();
    if (totalWeights == 0) {
      throw new IllegalStateException("Can not choose a random item unless total weights exceed 0");
    }

    final int randomNumber = random.nextInt(totalWeights + 1);
    int position = 0;
    for (final ItemRandomizer.RandomItem<T> item : items) {
      position += item.getWeight();
      if (randomNumber <= position && item.getWeight() > 0) {
        return item.getItem();
      }
    }

    throw new IllegalArgumentException("getRandomItem did not find an item");
  }

  /** {@inheritDoc} */
  public final double getWeightRatio(final T item) {
    final int totalWeights = getTotalWeights();
    if (totalWeights == 0) {
      return 0;
    }

    return getWeight(item) / (double) totalWeights;
  }

  /** {@inheritDoc} */
  public final int getWeight(final T item) {
    return getRandomItem(item).getWeight();
  }

  /**
   * Returns the RandomItem associated with <code>item</code>.
   * @param item the item
   * @return the RandomItem
   * @throws RuntimeException in case the item is not found
   */
  protected final ItemRandomizer.RandomItem<T> getRandomItem(final T item) {
    for (final ItemRandomizer.RandomItem<T> randomItem : items) {
      if (randomItem.getItem().equals(item)) {
        return randomItem;
      }
    }

    throw new IllegalArgumentException("Item not found: " + item);
  }

  /**
   * Notifies this model that the item weights have changed.
   */
  protected final void fireWeightsChangedEvent() {
    evtWeightsChanged.fire();
  }

  /**
   * @return a Random instance.
   */
  protected final Random getRandom() {
    return random;
  }

  private int getTotalWeights() {
    int sum = 0;
    for (final ItemRandomizer.RandomItem item : items) {
      sum += item.getWeight();
    }

    return sum;
  }

  /**
   * A class encapsulating an Object item and a integer weight value.
   */
  private static final class DefaultRandomItem<T> implements ItemRandomizer.RandomItem<T> {

    private final T item;
    private int weight = 0;

    /**
     * Instantiates a new RandomItem
     * @param item the item
     * @param weight the random selection weight to assign to this item
     */
    private DefaultRandomItem(final T item, final int weight) {
      if (weight < 0) {
        throw new IllegalStateException("Weight can not be negative");
      }
      this.item = item;
      this.weight = weight;
    }

    /** {@inheritDoc} */
    public int getWeight() {
      return weight;
    }

    /** {@inheritDoc} */
    public T getItem() {
      return item;
    }

  /** {@inheritDoc} */
    @Override
    public String toString() {
      return item.toString();
    }

  /** {@inheritDoc} */
    @Override
    public boolean equals(final Object obj) {
      return obj instanceof ItemRandomizer.RandomItem && (((ItemRandomizer.RandomItem) obj).getItem().equals(item));
    }

  /** {@inheritDoc} */
    @Override
    public int hashCode() {
      return item.hashCode();
    }

    /** {@inheritDoc} */
    public void incrementWeight() {
      weight++;
    }

    /** {@inheritDoc} */
    public void decrementWeight() {
      if (weight == 0) {
        throw new IllegalStateException("Weight can not be negative");
      }

      weight--;
    }

    /** {@inheritDoc} */
    public void setWeight(final int weight) {
      if (weight < 0) {
        throw new IllegalStateException("Weight can not be negative");
      }

      this.weight = weight;
    }
  }
}
