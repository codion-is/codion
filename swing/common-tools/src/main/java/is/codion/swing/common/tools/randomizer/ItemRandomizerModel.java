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
    randomItem(item).incrementWeight();
  }

  @Override
  public void decrementWeight(T item) {
    randomItem(item).decrementWeight();
  }

  @Override
  public void setWeight(T item, int weight) {
    randomItem(item).setWeight(weight);
  }

  @Override
  public final boolean isItemEnabled(T item) {
    return randomItem(item).isEnabled();
  }

  @Override
  public final void setItemEnabled(T item, boolean enabled) {
    randomItem(item).setEnabled(enabled);
  }

  @Override
  public final void addItem(T item) {
    addItem(item, 0);
  }

  @Override
  public final List<ItemRandomizer.RandomItem<T>> items() {
    return items;
  }

  @Override
  public final int itemCount() {
    return items.size();
  }

  @Override
  public final T randomItem() {
    int totalWeights = totalWeights();
    if (totalWeights == 0) {
      throw new IllegalStateException("Can not choose a random item unless total weights exceed 0");
    }

    int randomNumber = random.nextInt(totalWeights + 1);
    int position = 0;
    for (ItemRandomizer.RandomItem<T> item : items) {
      position += item.weight();
      if (randomNumber <= position && item.weight() > 0) {
        return item.item();
      }
    }

    throw new IllegalStateException("getRandomItem() did not find an item");
  }

  @Override
  public final double weightRatio(T item) {
    int totalWeights = totalWeights();
    if (totalWeights == 0) {
      return 0;
    }

    return weight(item) / (double) totalWeights;
  }

  @Override
  public final int weight(T item) {
    return randomItem(item).weight();
  }

  /**
   * Returns the RandomItem associated with {@code item}.
   * @param item the item
   * @return the RandomItem
   * @throws RuntimeException in case the item is not found
   */
  protected final ItemRandomizer.RandomItem<T> randomItem(T item) {
    for (ItemRandomizer.RandomItem<T> randomItem : items) {
      if (randomItem.item().equals(item)) {
        return randomItem;
      }
    }

    throw new IllegalArgumentException("Item not found: " + item + ": " + item.getClass());
  }

  private int totalWeights() {
    return items.stream()
            .mapToInt(RandomItem::weight)
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
    public int weight() {
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
    public T item() {
      return item;
    }

    @Override
    public String toString() {
      return item.toString();
    }

    @Override
    public boolean equals(Object obj) {
      return obj instanceof ItemRandomizer.RandomItem && (((ItemRandomizer.RandomItem<T>) obj).item().equals(item));
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
