/*
 * Copyright (c) 2010 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.model.randomizer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;

/**
 * A default {@link ItemRandomizer} implementation.
 * @param <T> the type returned by this randomizer
 */
class DefaultItemRandomizer<T> implements ItemRandomizer<T> {

  private final List<RandomItem<T>> items;
  private final Random random = new Random();

  DefaultItemRandomizer(Collection<RandomItem<T>> items) {
    this.items = unmodifiableList(new ArrayList<>(requireNonNull(items)));
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
  public final List<RandomItem<T>> items() {
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
    for (RandomItem<T> item : items) {
      position += item.weight();
      if (randomNumber <= position && item.weight() > 0) {
        return item.item();
      }
    }

    throw new IllegalStateException("randomItem() did not find an item");
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
  protected final RandomItem<T> randomItem(T item) {
    requireNonNull(item);
    for (RandomItem<T> randomItem : items) {
      if (randomItem.item() == item) {
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
}
