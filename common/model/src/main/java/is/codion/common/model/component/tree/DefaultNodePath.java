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
 * Copyright (c) 2026, Björn Darri Sigurðsson.
 */
package is.codion.common.model.component.tree;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;

final class DefaultNodePath<T> implements NodePath<T> {

	private static final DefaultNodePath<?> ROOT = new DefaultNodePath<>(new Object[0]);

	private final Object[] items;
	private final int hashCode;

	private DefaultNodePath(Object[] items) {
		this.items = items;
		this.hashCode = Arrays.hashCode(items);
	}

	@Override
	public T item() {
		if (items.length == 0) {
			throw new IllegalStateException("The root path has no item");
		}

		return (T) items[items.length - 1];
	}

	@Override
	public List<T> items() {
		return unmodifiableList((List<T>) Arrays.asList(items));
	}

	@Override
	public NodePath<T> parent() {
		if (items.length == 0) {
			throw new IllegalStateException("The root path has no parent");
		}
		if (items.length == 1) {
			return rootPath();
		}

		return new DefaultNodePath<>(Arrays.copyOf(items, items.length - 1));
	}

	@Override
	public NodePath<T> child(T item) {
		Object[] childItems = Arrays.copyOf(items, items.length + 1);
		childItems[items.length] = requireNonNull(item);

		return new DefaultNodePath<>(childItems);
	}

	@Override
	public int depth() {
		return items.length;
	}

	@Override
	public boolean root() {
		return items.length == 0;
	}

	@Override
	public boolean contains(NodePath<T> path) {
		if (requireNonNull(path).depth() < items.length) {
			return false;
		}
		List<T> otherItems = path.items();
		for (int i = 0; i < items.length; i++) {
			if (!items[i].equals(otherItems.get(i))) {
				return false;
			}
		}

		return true;
	}

	@Override
	public boolean equals(@Nullable Object object) {
		if (this == object) {
			return true;
		}
		if (!(object instanceof DefaultNodePath)) {
			return false;
		}
		DefaultNodePath<?> other = (DefaultNodePath<?>) object;

		return hashCode == other.hashCode && Arrays.equals(items, other.items);
	}

	@Override
	public int hashCode() {
		return hashCode;
	}

	@Override
	public String toString() {
		return Arrays.stream(items)
						.map(String::valueOf)
						.collect(joining(" / "));
	}

	static <T> DefaultNodePath<T> rootPath() {
		return (DefaultNodePath<T>) ROOT;
	}

	static <T> NodePath<T> nodePath(List<T> items) {
		if (items.isEmpty()) {
			return rootPath();
		}
		List<T> copy = new ArrayList<>(items);
		for (T item : copy) {
			requireNonNull(item);
		}

		return new DefaultNodePath<>(copy.toArray());
	}
}
