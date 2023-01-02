/*
 * Copyright (c) 2010 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.tools.randomizer;

import java.util.Collection;

/**
 * ItemRandomizer provides a way to randomly choose an item based on a weight value.
 *
 * <pre>
 * Object one = new Object();
 * Object two = new Object();
 * Object three = new Object();
 *
 * ItemRandomizer model = ...
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

public interface ItemRandomizer<T> {

  /**
   * @return the number of items in this model.
   */
  int itemCount();

  /**
   * @return the items in this model.
   */
  Collection<RandomItem<T>> items();

  /**
   * Returns the weight of the given item.
   * @param item the item
   * @return the item weight
   */
  int weight(T item);

  /**
   * Sets the weight of the given item
   * @param item the item
   * @param weight the value
   */
  void setWeight(T item, int weight);

  /**
   * Adds the given item to this model with default weight of 0.
   * @param item the item to add
   */
  void addItem(T item);

  /**
   * Adds the given item to this model with the given weight value.
   * @param item the item to add
   * @param weight the initial weight to assign to the item
   */
  void addItem(T item, int weight);

  /**
   * Fetches a random item from this model based on the item weights.
   * @return a randomly chosen item.
   */
  T randomItem();

  /**
   * Returns this items share in the total weights as a floating point number between 0 and 1
   * @param item the item
   * @return the ratio of the total weights held by the given item
   */
  double weightRatio(T item);

  /**
   * Increments the weight of the given item by one
   * @param item the item
   */
  void incrementWeight(T item);

  /**
   * Decrements the weight of the given item by one
   * @param item the item
   * @throws IllegalStateException in case the weight is 0
   */
  void decrementWeight(T item);

  /**
   * @param item the item
   * @return true if the item is enabled
   */
  boolean isItemEnabled(T item);

  /**
   * @param item the item
   * @param enabled true if the item should be enabled
   */
  void setItemEnabled(T item, boolean enabled);

  /**
   * Wraps an item for usage in the ItemRandomizer
   * @param <T> the type being wrapped
   */
  interface RandomItem<T> {

    /**
     * Increments the weight value assigned to this random item
     */
    void incrementWeight();

    /**
     * Decrements the weight value assigned to this random item
     */
    void decrementWeight();

    /**
     * @param weight the random weight assigned to this item
     */
    void setWeight(int weight);

    /**
     * @return the random weight assigned to this item
     */
    int weight();

    /**
     * @return true if this item is enabled
     */
    boolean isEnabled();

    /**
     * @param enabled true if this item should be enabled
     */
    void setEnabled(boolean enabled);

    /**
     * @return the item this random item represents
     */
    T item();
  }
}
