/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model.tools;

import org.jminor.common.model.EventObserver;

import java.io.Serializable;
import java.util.Collection;

/**
 * ItemRandomizer provides a way for randomly choosing an item based on a weight value.
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
  int getItemCount();

  /**
   * @return the items in this model.
   */
  Collection<RandomItem<T>> getItems();

  /**
   * Returns the weight of the given item.
   * @param item the item
   * @return the item weight
   */
  int getWeight(T item);

  /**
   * @return an EventObserver which fires each time a weight has been changed.
   */
  EventObserver<Integer> getWeightsObserver();

  /**
   * @return an EventObserver which fires each time the enabled status of an item has been changed.
   */
  EventObserver<Boolean> getEnabledObserver();

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
  void addItem(final T item);

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
  T getRandomItem();

  /**
   * Returns this items share in the total weights as a floating point number between 0 and 1
   * @param item the item
   * @return the ratio of the total weights held by the given item
   */
  double getWeightRatio(final T item);

  /**
   * Increments the weight of the given item by one
   * @param item the item
   */
  void incrementWeight(final T item);

  /**
   * Decrements the weight of the given item by one
   * @param item the item
   * @throws IllegalStateException in case the weight is 0
   */
  void decrementWeight(final T item);

  /**
   * @param item the item
   * @return true if the item is enabled
   */
  boolean isItemEnabled(final T item);

  /**
   * @param item the item
   * @param value true if the item should be enabled
   */
  void setItemEnabled(final T item, final boolean value);

  /**
   * Wraps an item for usage in the ItemRandomizer
   * @param <T> the type being wrapped
   */
  interface RandomItem<T> extends Serializable {

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
    int getWeight();

    /**
     * @return true if this item is enabled
     */
    boolean isEnabled();

    /**
     * @param value true if this item should be enabled
     */
    void setEnabled(final boolean value);

    /**
     * @return the item this random item represents
     */
    T getItem();
  }
}
