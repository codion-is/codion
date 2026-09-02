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
 * Copyright (c) 2013 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.common.model.component.combobox;

import is.codion.common.model.component.combobox.FilterComboBoxModel.ItemComboBoxModelBuilder;
import is.codion.common.utilities.item.Item;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import static java.util.Objects.requireNonNull;

/**
 * A base class for {@link ItemComboBoxModelBuilder} implementations. A toolkit builder extends this class,
 * adding its own options and overriding {@link #build()} to wrap the {@link FilterComboBoxModel} in its own model.
 * @param <T> the item type
 * @param <B> the builder type
 */
public abstract class AbstractItemComboBoxModelBuilder<T, B extends ItemComboBoxModelBuilder<T, B>>
				implements ItemComboBoxModelBuilder<T, B> {

	private final List<Item<T>> items;
	private final @Nullable Item<T> nullItem;

	private boolean sorted = false;
	private @Nullable Comparator<Item<T>> comparator;
	private @Nullable Item<T> selected;

	/**
	 * @param items the items, a null item, if any, is used as the null item
	 */
	protected AbstractItemComboBoxModelBuilder(List<Item<T>> items) {
		this.items = new ArrayList<>(requireNonNull(items));
		int indexOfNullItem = this.items.indexOf(Item.item(null));
		this.nullItem = indexOfNullItem >= 0 ? this.items.remove(indexOfNullItem) : null;
	}

	@Override
	public final B sorted(boolean sorted) {
		this.sorted = sorted;
		if (!sorted) {
			this.comparator = null;
		}
		return self();
	}

	@Override
	public final B sorted(Comparator<Item<T>> comparator) {
		this.sorted = true;
		this.comparator = requireNonNull(comparator);
		return self();
	}

	@Override
	public final B selected(@Nullable T selected) {
		return selected(Item.item(selected));
	}

	@Override
	public final B selected(Item<T> selected) {
		requireNonNull(selected);
		if (!items.contains(selected) && !Objects.equals(selected, nullItem)) {
			throw new IllegalArgumentException("Model does not contain item: " + selected);
		}
		this.selected = selected;
		return self();
	}

	@Override
	public FilterComboBoxModel<Item<T>> build() {
		FilterComboBoxModel.Builder<Item<T>, ?> builder = DefaultFilterComboBoxModel.builder(items)
						.translator(new DefaultFilterComboBoxModel.SelectedItemTranslator<>(items))
						.nullItem(nullItem);
		if (!sorted) {
			builder.comparator(null);
		}
		if (comparator != null) {
			builder.comparator(comparator);
		}
		FilterComboBoxModel<Item<T>> comboBoxModel = builder.build();
		if (selected != null) {
			comboBoxModel.selection().item().set(selected);
		}

		return comboBoxModel;
	}

	/**
	 * @return this builder instance
	 */
	protected final B self() {
		return (B) this;
	}
}
