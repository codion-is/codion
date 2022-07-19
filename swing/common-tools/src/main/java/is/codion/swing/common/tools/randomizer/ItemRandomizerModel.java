/*
 * Copyright (c) 2010 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.tools.randomizer;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * A default ItemRandomizer implementation.
 * @param <T> the type returned by this randomizer
 */
public class ItemRandomizerModel<T> implements ItemRandomizer<T> {

  /**
   * The items contained in this model
   */
  private final List<ItemRandomizer.RandomItem<T>> items = new ArrayList<>();

  /**
   * The Random instance used by this randomizer
   */
  private final Random random = new Random();

  @Override
  public void addItem(T item, int weight) {
    items.add(new DefaultRandomItem<>(item, weight));
  }

  @Override
  public void incrementWeight(T item) {
    getRandomItem(item).incrementWeight();
  }

  @Override
  public void decrementWeight(T item) {
    getRandomItem(item).decrementWeight();
  }

  @Override
  public void setWeight(T item, int weight) {
    getRandomItem(item).setWeight(weight);
  }

  @Override
  public final boolean isItemEnabled(T item) {
    return getRandomItem(item).isEnabled();
  }

  @Override
  public final void setItemEnabled(T item, boolean enabled) {
    getRandomItem(item).setEnabled(enabled);
  }

  @Override
  public final void addItem(T item) {
    addItem(item, 0);
  }

  @Override
  public final List<ItemRandomizer.RandomItem<T>> getItems() {
    return items;
  }

  @Override
  public final int getItemCount() {
    return items.size();
  }

  @Override
  public final T getRandomItem() {
    int totalWeights = getTotalWeights();
    if (totalWeights == 0) {
      throw new IllegalStateException("Can not choose a random item unless total weights exceed 0");
    }

    int randomNumber = random.nextInt(totalWeights + 1);
    int position = 0;
    for (ItemRandomizer.RandomItem<T> item : items) {
      position += item.getWeight();
      if (randomNumber <= position && item.getWeight() > 0) {
        return item.getItem();
      }
    }

    throw new IllegalStateException("getRandomItem() did not find an item");
  }

  @Override
  public final double getWeightRatio(T item) {
    int totalWeights = getTotalWeights();
    if (totalWeights == 0) {
      return 0;
    }

    return getWeight(item) / (double) totalWeights;
  }

  @Override
  public final int getWeight(T item) {
    return getRandomItem(item).getWeight();
  }

  /**
   * Returns the RandomItem associated with {@code item}.
   * @param item the item
   * @return the RandomItem
   * @throws RuntimeException in case the item is not found
   */
  protected final ItemRandomizer.RandomItem<T> getRandomItem(T item) {
    for (ItemRandomizer.RandomItem<T> randomItem : items) {
      if (randomItem.getItem().equals(item)) {
        return randomItem;
      }
    }

    throw new IllegalArgumentException("Item not found: " + item + ": " + item.getClass());
  }

  private int getTotalWeights() {
    return items.stream()
            .mapToInt(RandomItem::getWeight)
            .sum();
  }

  /**
   * A class encapsulating an Object item and an integer weight value.
   */
  private static final class DefaultRandomItem<T> implements ItemRandomizer.RandomItem<T> {

    private static final String WEIGHT_CAN_NOT_BE_NEGATIVE = "Weight can not be negative";

    private final T item;
    private int weight;
    private boolean enabled = true;

    /**
     * Instantiates a new RandomItem
     * @param item the item
     * @param weight the random selection weight to assign to this item
     */
    private DefaultRandomItem(T item, int weight) {
      if (weight < 0) {
        throw new IllegalArgumentException(WEIGHT_CAN_NOT_BE_NEGATIVE);
      }
      this.item = item;
      this.weight = weight;
    }

    @Override
    public int getWeight() {
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
    public T getItem() {
      return item;
    }

    @Override
    public String toString() {
      return item.toString();
    }

    @Override
    public boolean equals(Object obj) {
      return obj instanceof ItemRandomizer.RandomItem && (((ItemRandomizer.RandomItem<T>) obj).getItem().equals(item));
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
}
