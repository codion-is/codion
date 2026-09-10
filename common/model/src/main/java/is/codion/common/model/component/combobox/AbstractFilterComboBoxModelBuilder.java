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

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

/**
 * A base class for {@link FilterComboBoxModel.Builder} implementations. A toolkit builder extends this class,
 * adding its own options and overriding {@link #build()} to wrap the {@link FilterComboBoxModel} in its own model.
 * @param <T> the item type
 * @param <B> the builder type
 */
public abstract class AbstractFilterComboBoxModelBuilder<T, B extends FilterComboBoxModel.Builder<T, B>>
				implements FilterComboBoxModel.Builder<T, B> {

	final @Nullable Collection<T> items;
	final @Nullable Supplier<Collection<T>> supplier;
	final Collection<Consumer<T>> onSelectedItem = new ArrayList<>(1);

	Predicate<T> validator = new ValidPredicate<>();
	Comparator<T> comparator = (Comparator<T>) DefaultFilterComboBoxModel.DEFAULT_COMPARATOR;
	Function<Object, T> translator = (Function<Object, T>) DefaultFilterComboBoxModel.DEFAULT_SELECTED_ITEM_TRANSLATOR;
	@Nullable Consumer<Exception> onRefreshException;
	boolean filterSelected;
	boolean includeNull;
	@Nullable T nullItem;
	@Nullable T selectItem;
	boolean refresh = false;

	/**
	 * @param items the items
	 */
	protected AbstractFilterComboBoxModelBuilder(Collection<T> items) {
		this.items = requireNonNull(items);
		this.supplier = null;
	}

	/**
	 * @param supplier supplies the items
	 */
	protected AbstractFilterComboBoxModelBuilder(Supplier<Collection<T>> supplier) {
		this.items = null;
		this.supplier = requireNonNull(supplier);
	}

	@Override
	public final B validator(Predicate<T> validator) {
		this.validator = requireNonNull(validator);
		return self();
	}

	@Override
	public final B comparator(@Nullable Comparator<T> comparator) {
		this.comparator = comparator == null ? (Comparator<T>) DefaultFilterComboBoxModel.NULL_COMPARATOR : comparator;
		return self();
	}

	@Override
	public final B includeNull(boolean includeNull) {
		this.includeNull = includeNull;
		if (!includeNull) {
			this.nullItem = null;
		}
		return self();
	}

	@Override
	public final B nullItem(@Nullable T nullItem) {
		this.nullItem = nullItem;
		return includeNull(nullItem != null);
	}

	@Override
	public final B select(@Nullable T item) {
		this.selectItem = item;
		return self();
	}

	@Override
	public final B translator(Function<Object, T> translator) {
		this.translator = requireNonNull(translator);
		return self();
	}

	@Override
	public final B filterSelected(boolean filterSelected) {
		this.filterSelected = filterSelected;
		return self();
	}

	@Override
	public final B onSelectedItem(Consumer<@Nullable T> item) {
		this.onSelectedItem.add(requireNonNull(item));
		return self();
	}

	@Override
	public final B onRefreshException(Consumer<Exception> onRefreshException) {
		this.onRefreshException = requireNonNull(onRefreshException);
		return self();
	}

	@Override
	public final B refresh(boolean refresh) {
		this.refresh = refresh;
		return self();
	}

	@Override
	public FilterComboBoxModel<T> build() {
		return new DefaultFilterComboBoxModel<>(this);
	}

	/**
	 * @return this builder instance
	 */
	protected final B self() {
		return (B) this;
	}

	private static final class ValidPredicate<T> implements Predicate<T> {

		@Override
		public boolean test(T item) {
			return true;
		}
	}
}
