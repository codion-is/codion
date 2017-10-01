/*
 * Copyright (c) 2004 - 2017, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.tools;

import org.jminor.common.Event;
import org.jminor.common.EventObserver;
import org.jminor.common.Events;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * A default ItemRandomizer implementation.
 * @param <T> the type returned by this randomizer
 */
public class ItemRandomizerModel<T> implements ItemRandomizer<T> {

  /**
   * An Event fired when a weight value has changed
   */
  private final Event<Integer> weightsChangedEvent = Events.event();

  /**
   * An Event fired when the enabled status of an item has changed
   */
  private final Event<Boolean> enabledChangedEvent = Events.event();

  /**
   * The items contained in this model
   */
  private final List<ItemRandomizer.RandomItem<T>> items = new ArrayList<>();

  /**
   * The Random instance used by this randomizer
   */
  private final Random random = new Random();

  /**
   * Instantiates a new empty RandomItemModel.
   */
  public ItemRandomizerModel() {
    this(0);
  }

  /**
   * Instantiates a new RandomItemModel with the given items.
   * @param defaultWeight the default weight to assign to each initial item
   * @param items the items to add to this model
   */
  public ItemRandomizerModel(final int defaultWeight, final T... items) {
    if (items != null) {
      for (final T item : items) {
        this.items.add(new DefaultRandomItem<>(item, defaultWeight));
      }
    }
  }

  /** {@inheritDoc} */
  @Override
  public void addItem(final T item, final int weight) {
    items.add(new DefaultRandomItem<>(item, weight));
  }

  /** {@inheritDoc} */
  @Override
  public void incrementWeight(final T item) {
    final RandomItem randomItem = getRandomItem(item);
    randomItem.incrementWeight();
    weightsChangedEvent.fire(randomItem.getWeight());
  }

  /** {@inheritDoc} */
  @Override
  public void decrementWeight(final T item) {
    final RandomItem randomItem = getRandomItem(item);
    randomItem.decrementWeight();
    weightsChangedEvent.fire(randomItem.getWeight());
  }

  /** {@inheritDoc} */
  @Override
  public void setWeight(final T item, final int weight) {
    getRandomItem(item).setWeight(weight);
    weightsChangedEvent.fire(weight);
  }

  /** {@inheritDoc} */
  @Override
  public boolean isItemEnabled(final T item) {
    return getRandomItem(item).isEnabled();
  }

  /** {@inheritDoc} */
  @Override
  public void setItemEnabled(final T item, final boolean value) {
    getRandomItem(item).setEnabled(value);
    enabledChangedEvent.fire(value);
  }

  /** {@inheritDoc} */
  @Override
  public final void addItem(final T item) {
    addItem(item, 0);
  }

  /** {@inheritDoc} */
  @Override
  public final List<ItemRandomizer.RandomItem<T>> getItems() {
    return items;
  }

  /** {@inheritDoc} */
  @Override
  public final int getItemCount() {
    return items.size();
  }

  /** {@inheritDoc} */
  @Override
  public final EventObserver<Integer> getWeightsObserver() {
    return weightsChangedEvent.getObserver();
  }

  /** {@inheritDoc} */
  @Override
  public final EventObserver<Boolean> getEnabledObserver() {
    return enabledChangedEvent.getObserver();
  }

  /** {@inheritDoc} */
  @Override
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
  @Override
  public final double getWeightRatio(final T item) {
    final int totalWeights = getTotalWeights();
    if (totalWeights == 0) {
      return 0;
    }

    return getWeight(item) / (double) totalWeights;
  }

  /** {@inheritDoc} */
  @Override
  public final int getWeight(final T item) {
    return getRandomItem(item).getWeight();
  }

  /**
   * Returns the RandomItem associated with {@code item}.
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

    throw new IllegalArgumentException("Item not found: " + item + ": " + item.getClass());
  }

  /**
   * Notifies this model that the item weights have changed.
   */
  protected final void fireWeightsChangedEvent() {
    weightsChangedEvent.fire();
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

    private static final String WEIGHT_CAN_NOT_BE_NEGATIVE = "Weight can not be negative";

    private final T item;
    private int weight = 0;
    private boolean enabled = true;

    /**
     * Instantiates a new RandomItem
     * @param item the item
     * @param weight the random selection weight to assign to this item
     */
    private DefaultRandomItem(final T item, final int weight) {
      if (weight < 0) {
        throw new IllegalStateException(WEIGHT_CAN_NOT_BE_NEGATIVE);
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
    public void setEnabled(final boolean value) {
      this.enabled = value;
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
    public boolean equals(final Object obj) {
      return obj instanceof ItemRandomizer.RandomItem && (((ItemRandomizer.RandomItem) obj).getItem().equals(item));
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
    public void setWeight(final int weight) {
      if (weight < 0) {
        throw new IllegalStateException(WEIGHT_CAN_NOT_BE_NEGATIVE);
      }

      this.weight = weight;
    }
  }
}
