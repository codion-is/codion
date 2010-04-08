/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model;

import java.util.ArrayList;
import java.util.Collection;
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
 */
public class RandomItemModel {

  /**
   * An Event fired when a weight value has changed
   */
  protected final Event evtWeightsChanged = new Event();

  /**
   * The items contained in this model
   */
  protected final List<RandomItem> items = new ArrayList<RandomItem>();

  private final Random random = new Random();

  public RandomItemModel() {
    this(0);
  }

  /**
   * Instantiates a new RandomItemModel with the given items.
   * @param defaultWeight the default weight to assign to each intial item
   * @param items the items to add to this model
   */
  public RandomItemModel(final int defaultWeight, final Object... items) {
    if (items != null) {
      for (final Object item : items)
        addItem(item, defaultWeight);
    }
  }

  /**
   * Adds the given item to this model with default weight of 0.
   * @param item the item to add
   */
  public void addItem(final Object item) {
    addItem(item, 0);
  }

  /**
   * Adds the given item to this model with the given weight value.
   * @param item the item to add
   * @param weight the initial weight to assign to the item
   */
  public void addItem(final Object item, final int weight) {
    items.add(new RandomItem(item, weight));
  }

  /**
   * @return the items in this model.
   */
  public Collection<RandomItem> getItems() {
    return items;
  }

  /**
   * @return a Random instance.
   */
  public Random getRandom() {
    return random;
  }

  /**
   * @return the number of items in this model.
   */
  public int getItemCount() {
    return getItems().size();
  }

  /**
   * @return an Event which fires each time a weight has been changed.
   */
  public Event eventWeightsChanged() {
    return evtWeightsChanged;
  }

  /**
   * Fetches a random item from this model based on the item weights.
   * @return a randomly chosen item.
   */
  public Object getRandomItem() {
    final int totalWeights = getTotalWeights();
    if (totalWeights == 0)
      throw new RuntimeException("Can not choose a random item unless total weights exceed 0");

    final int random = getRandom().nextInt(totalWeights + 1);
    int position = 0;
    for (final RandomItem item : items) {
      position += item.getWeight();
      if (random <= position && item.getWeight() > 0)
        return item.getItem();
    }

    throw new RuntimeException("getRandomItem did not find an item");
  }

  /**
   * Returns this items share in the total weights as a floating point number between 0 and 1
   * @param item the item
   * @return the ratio of the total weights held by the given item
   */
  public double getWeightRatio(final Object item) {
    final int totalWeights = getTotalWeights();
    if (totalWeights == 0)
      return 0;

    return getWeight(item) / (double) totalWeights;
  }

  /**
   * Increments the weight of the given item by one
   * @param item the item
   */
  public void increment(final Object item) {
    synchronized (items) {
      final RandomItem randomItem = getRandomItem(item);
      if (randomItem != null)
        randomItem.increment();
    }
    eventWeightsChanged().fire();
  }

  /**
   * Decrements the weight of the given item by one
   * @param item the item
   * @throws IllegalStateException in case the weight is 0
   */
  public void decrement(final Object item) {
    synchronized (items) {
      final RandomItem randomItem = getRandomItem(item);
      if (randomItem != null)
        randomItem.decrement();
    }
    eventWeightsChanged().fire();
  }

  /**
   * Sets the weight of the given item
   * @param item the item
   * @param weight the value
   */
  public void setWeight(final Object item, final int weight) {
    getRandomItem(item).setWeight(weight);
    eventWeightsChanged().fire();
  }

  /**
   * Returns the weight of the given item.
   * @param item the item
   * @return the item weight
   */
  public int getWeight(final Object item) {
    synchronized (items) {
      final RandomItem randomItem = getRandomItem(item);
      if (randomItem != null)
        return randomItem.getWeight();
    }

    return 0;
  }

  /**
   * Returns the RandomItem associated with <code>item</code>.
   * @param item the item
   * @return the RandomItem
   */
  protected RandomItem getRandomItem(final Object item) {
    for (final RandomItem randomItem : items)
      if (randomItem.getItem().equals(item))
        return randomItem;

    return null;
  }

  private int getTotalWeights() {
    int sum = 0;
    synchronized (items) {
      for (final RandomItem item : items)
        sum += item.getWeight();
    }

    return sum;
  }

  /**
   * A class encapsulating an Object item and a integer weight value.
   */
  public static class RandomItem {

    private final Object item;
    private int weight = 0;

    public RandomItem(final Object item, final int weight) {
      this.item = item;
      this.weight = weight;
    }

    public int getWeight() {
      return weight;
    }

    public Object getItem() {
      return item;
    }

    @Override
    public String toString() {
      return item.toString();
    }

    @Override
    public boolean equals(final Object obj) {
      return obj instanceof RandomItem && (((RandomItem) obj).getItem().equals(getItem()));
    }

    void increment() {
      weight++;
    }

    void decrement() {
      if (weight == 0)
        throw new IllegalStateException("Weight can not be negative");

      weight--;
    }

    void setWeight(final int weight) {
      if (weight < 0)
        throw new IllegalStateException("Weight can not be negative");

      this.weight = weight;
    }
  }
}
