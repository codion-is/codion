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
 * Copyright (c) 2010 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.common.model.randomizer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import static java.util.Objects.requireNonNull;

/**
 * User: Björn Darri<br>
 * Date: 6.4.2010<br>
 * Time: 21:26:00<br>
 * @param <T> the type of item this random item model returns
 */
final class BoundedItemRandomizer<T> extends DefaultItemRandomizer<T> {

  private final Object lock = new Object();
  private final int maximumTotalWeight;
  private RandomItem<T> lastAffected;

  BoundedItemRandomizer(Collection<T> items, int maximumTotalWeight) {
    super(initializeItems(items, maximumTotalWeight));
    this.maximumTotalWeight = maximumTotalWeight;
    this.lastAffected = items().get(0);
  }

  @Override
  public void incrementWeight(T item) {
    synchronized (lock) {
      RandomItem<T> randomItem = randomItem(item);
      if (randomItem.weight() >= maximumTotalWeight) {
        throw new IllegalStateException("Maximum weight reached");
      }

      decrementWeight(randomItem);
      randomItem(item).incrementWeight();
    }
  }

  @Override
  public void decrementWeight(T item) {
    synchronized (lock) {
      RandomItem<T> randomItem = randomItem(item);
      if (randomItem.weight() == 0) {
        throw new IllegalStateException("No weight to shed");
      }

      incrementWeight(randomItem);
      randomItem.decrementWeight();
    }
  }

  @Override
  public void setWeight(T item, int weight) {
    throw new UnsupportedOperationException("setWeight is not implemented in " + getClass().getSimpleName());
  }

  private static <T> Collection<RandomItem<T>> initializeItems(Collection<T> items, int weightBounds) {
    if (weightBounds <= 0) {
      throw new IllegalArgumentException("Bounded weight must be a positive integer");
    }
    if (requireNonNull(items, "items").isEmpty()) {
      throw new IllegalArgumentException("Items must not be empty");
    }
    int rest = weightBounds % items.size();
    int amountEach = weightBounds / items.size();
    Collection<RandomItem<T>> randomItems = new ArrayList<>(items.size());
    Iterator<T> itemIterator = items.iterator();
    int i = 0;
    while (itemIterator.hasNext()) {
      randomItems.add(RandomItem.randomItem(itemIterator.next(), i++ < items.size() - 1 ? amountEach : amountEach + rest));
    }

    return randomItems;
  }

  private void incrementWeight(RandomItem<?> exclude) {
    lastAffected = nextItem(exclude, false);
    lastAffected.incrementWeight();
  }

  private void decrementWeight(RandomItem<?> exclude) {
    lastAffected = nextItem(exclude, true);
    lastAffected.decrementWeight();
  }

  private RandomItem<T> nextItem(RandomItem<?> exclude, boolean nonEmpty) {
    int index = items().indexOf(lastAffected);
    RandomItem<T> item = null;
    while (item == null || item.equals(exclude) || (nonEmpty ? item.weight() == 0 : item.weight() == maximumTotalWeight)) {
      if (index == 0) {
        index = items().size() - 1;
      }
      else {
        index--;
      }

      item = items().get(index);
    }

    return item;
  }
}
