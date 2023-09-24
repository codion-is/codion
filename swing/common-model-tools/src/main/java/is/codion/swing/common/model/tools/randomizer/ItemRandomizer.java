/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2010 - 2023, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.model.tools.randomizer;

import java.util.Collection;

/**
 * ItemRandomizer provides a way to randomly choose an item based on a weight value.
 *
 * <pre>
 * Object one = new Object();
 * Object two = new Object();
 * Object three = new Object();
 *
 * ItemRandomizer model = ItemRandomizer.itemRandomizer(Arrays.asList(one, two, three));
 *
 * model.setWeight(one, 10);
 * model.setWeight(two, 60);
 * model.setWeight(three, 30);
 *
 * //10% chance of getting 'one', 60% chance of getting 'two' and 30% chance of getting 'three'.
 * Object random = model.randomItem();
 * </pre>
 * For instances use the following factory functions: {@link #itemRandomizer(Collection)},
 * {@link #boundedItemRandomizer(Collection)}, {@link #boundedItemRandomizer(Collection, int)}
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
   * Instantiates a new {@link ItemRandomizer}.
   * @param <T> the item type
   * @param items the items to randomize
   * @return a new {@link ItemRandomizer}
   */
  static <T> ItemRandomizer<T> itemRandomizer(Collection<RandomItem<T>> items) {
    return new DefaultItemRandomizer<>(items);
  }

  /**
   * Instantiates a new {@link ItemRandomizer} with the added constraint that the total item weights can not exceed a defined maximum.
   * When the weight of one item is incremented the weight of another is decremented in a round-robin kind of fashion
   * and when an item weight is decremented the weight of another is incremented.<br>
   * Instantiates a new {@link ItemRandomizer} with the maximum total weights as 100.
   * @param <T> the item type
   * @param items the items
   * @return a new {@link ItemRandomizer}
   */
  static <T> ItemRandomizer<T> boundedItemRandomizer(Collection<T> items) {
    return new BoundedItemRandomizer<>(items, 100);
  }

  /**
   * Instantiates a new {@link ItemRandomizer} with the added constraint that the total item weights can not exceed a defined maximum.
   * When the weight of one item is incremented the weight of another is decremented in a round-robin kind of fashion
   * and when an item weight is decremented the weight of another is incremented.<br>
   * @param <T> the item type
   * @param items the items
   * @param maximumTotalWeights the maximum total weights
   * @return a new {@link ItemRandomizer}
   */
  static <T> ItemRandomizer<T> boundedItemRandomizer(Collection<T> items, int maximumTotalWeights) {
    return new BoundedItemRandomizer<>(items, maximumTotalWeights);
  }

  /**
   * Wraps an item for usage in the ItemRandomizer.
   * For instances use the {@link RandomItem#randomItem(Object, int)} factory method.
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

    /**
     * Instantiates a new {@link RandomItem} instance.
     * @param item the item
     * @param weight the random selection weight to assign to this item
     * @param <T> the item type
     * @return a new {@link RandomItem} instance.
     */
    static <T> RandomItem<T> randomItem(T item, int weight) {
      return new DefaultRandomItem<>(item, weight);
    }
  }
}
