/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * RandomItemModel provides a way for randomly choosing an item based on a weight value.
 *
 * <pre>
 * Object one = new Object();
 * Object two = new Object();
 * Object three = new Object();
 *
 * RandomItemModel model = new RandomItemModel(one, two, three);
 *
 * model.setWeight(one, 10);
 * model.setWeight(two, 60);
 * model.setWeight(three, 30);
 *
 * //10% chance of getting 'one', 60% chance of getting 'two' and 30% chance of getting 'three'.
 * Object random = model.getRandomItem();
 * </pre>
 * @param <T> the type of item this random item model returns
 */
public class RandomItemModel<T> {

  /**
   * An Event fired when a weight value has changed
   */
  private final Event evtWeightsChanged = Events.event();

  /**
   * The items contained in this model
   */
  private final List<RandomItem<T>> items = new ArrayList<RandomItem<T>>();

  private final Random random = new Random();

  /**
   * Instantiates a new empty RandomItemModel.
   */
  public RandomItemModel() {
    this(0);
  }

  /**
   * Instantiates a new RandomItemModel with the given items.
   * @param defaultWeight the default weight to assign to each intial item
   * @param items the items to add to this model
   */
  public RandomItemModel(final int defaultWeight, final T... items) {
    if (items != null) {
      for (final T item : items) {
        this.items.add(new RandomItem<T>(item, defaultWeight));
      }
    }
  }

  /**
   * Adds the given item to this model with the given weight value.
   * @param item the item to add
   * @param weight the initial weight to assign to the item
   */
  public void addItem(final T item, final int weight) {
    items.add(new RandomItem<T>(item, weight));
  }

  /**
   * Increments the weight of the given item by one
   * @param item the item
   */
  public void increment(final T item) {
    getRandomItem(item).increment();
    evtWeightsChanged.fire();
  }

  /**
   * Decrements the weight of the given item by one
   * @param item the item
   * @throws IllegalStateException in case the weight is 0
   */
  public void decrement(final T item) {
    getRandomItem(item).decrement();
    evtWeightsChanged.fire();
  }

  /**
   * Sets the weight of the given item
   * @param item the item
   * @param weight the value
   */
  public void setWeight(final T item, final int weight) {
    getRandomItem(item).setWeight(weight);
    evtWeightsChanged.fire();
  }

  /**
   * Adds the given item to this model with default weight of 0.
   * @param item the item to add
   */
  public final void addItem(final T item) {
    addItem(item, 0);
  }

  /**
   * @return the items in this model.
   */
  public final List<RandomItem<T>> getItems() {
    return items;
  }

  /**
   * @return a Random instance.
   */
  public final Random getRandom() {
    return random;
  }

  /**
   * @return the number of items in this model.
   */
  public final int getItemCount() {
    return items.size();
  }

  /**
   * @return an Event which fires each time a weight has been changed.
   */
  public final EventObserver getWeightsObserver() {
    return evtWeightsChanged.getObserver();
  }

  /**
   * Fetches a random item from this model based on the item weights.
   * @return a randomly chosen item.
   */
  public final T getRandomItem() {
    final int totalWeights = getTotalWeights();
    if (totalWeights == 0) {
      throw new RuntimeException("Can not choose a random item unless total weights exceed 0");
    }

    final int randomNumber = random.nextInt(totalWeights + 1);
    int position = 0;
    for (final RandomItem<T> item : items) {
      position += item.getWeight();
      if (randomNumber <= position && item.getWeight() > 0) {
        return item.getItem();
      }
    }

    throw new RuntimeException("getRandomItem did not find an item");
  }

  /**
   * Returns this items share in the total weights as a floating point number between 0 and 1
   * @param item the item
   * @return the ratio of the total weights held by the given item
   */
  public final double getWeightRatio(final T item) {
    final int totalWeights = getTotalWeights();
    if (totalWeights == 0) {
      return 0;
    }

    return getWeight(item) / (double) totalWeights;
  }

  /**
   * Returns the weight of the given item.
   * @param item the item
   * @return the item weight
   */
  public final int getWeight(final T item) {
    return getRandomItem(item).getWeight();
  }

  /**
   * Returns the RandomItem associated with <code>item</code>.
   * @param item the item
   * @return the RandomItem
   * @throws RuntimeException in case the item is not found
   */
  protected final RandomItem<T> getRandomItem(final T item) {
    for (final RandomItem<T> randomItem : items) {
      if (randomItem.getItem().equals(item)) {
        return randomItem;
      }
    }

    throw new RuntimeException("Item not found: " + item);
  }

  /**
   * Notifies this model that the item weights have changed.
   */
  protected final void fireWeightsChangedEvent() {
    evtWeightsChanged.fire();
  }

  private int getTotalWeights() {
    int sum = 0;
    for (final RandomItem item : items) {
      sum += item.getWeight();
    }

    return sum;
  }

  /**
   * A class encapsulating an Object item and a integer weight value.
   */
  public static final class RandomItem<T> {

    private final T item;
    private int weight = 0;

    /**
     * Instantiates a new RandomItem
     * @param item the item
     * @param weight the random selection weight to assign to this item
     */
    public RandomItem(final T item, final int weight) {
      if (weight < 0) {
        throw new IllegalStateException("Weight can not be negative");
      }
      this.item = item;
      this.weight = weight;
    }

    /**
     * @return the random weight assigned to this item
     */
    public int getWeight() {
      return weight;
    }

    /**
     * @return the item this random item represents
     */
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
      return obj instanceof RandomItem && (((RandomItem) obj).item.equals(item));
    }

  /** {@inheritDoc} */
    @Override
    public int hashCode() {
      return item.hashCode();
    }

    void increment() {
      weight++;
    }

    void decrement() {
      if (weight == 0) {
        throw new IllegalStateException("Weight can not be negative");
      }

      weight--;
    }

    private void setWeight(final int weight) {
      if (weight < 0) {
        throw new IllegalStateException("Weight can not be negative");
      }

      this.weight = weight;
    }
  }
}
