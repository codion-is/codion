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
package is.codion.framework.model;

import is.codion.common.model.component.tree.FilterTreeModel;
import is.codion.common.model.component.tree.NodePath;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.EntityConnection.Select;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.ForeignKey;
import is.codion.framework.domain.entity.condition.Condition;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

/**
 * A base class for {@link EntityTreeModel.Builder} implementations. A toolkit builder extends this class, overriding
 * {@link #build()} to build its own model, based on {@link AbstractEntityTreeModel}.
 * @param <B> the builder type
 * @see AbstractEntityTreeModel
 */
public abstract class AbstractEntityTreeModelBuilder<B extends EntityTreeModel.Builder<B>>
				implements EntityTreeModel.Builder<B> {

	final EntityConnection connection;
	final EntityType rootType;
	final NodeConfiguration roots = new NodeConfiguration();
	final Map<ForeignKey, NodeConfiguration> children = new LinkedHashMap<>();
	//the options of the underlying tree model, applied in order, after the defaults
	final List<Consumer<FilterTreeModel.Builder<Entity, ?>>> options = new ArrayList<>();

	//the comparator of the tree, for the roots and children which specify neither a comparator nor an order by
	@Nullable Comparator<Entity> comparator;
	boolean comparatorSet = false;
	//replacing the ordering of siblings entirely
	@Nullable Function<NodePath<Entity>, @Nullable Comparator<Entity>> comparators;

	/**
	 * @param rootType the type of the root entities
	 * @param roots configures the roots
	 * @param connection the connection
	 */
	protected AbstractEntityTreeModelBuilder(EntityType rootType, Consumer<Roots> roots, EntityConnection connection) {
		this.connection = requireNonNull(connection);
		this.rootType = connection.entities().definition(requireNonNull(rootType)).type();
		requireNonNull(roots).accept(this.roots);
	}

	@Override
	public final B children(ForeignKey foreignKey) {
		return children(foreignKey, configuration -> {});
	}

	@Override
	public final B children(ForeignKey foreignKey, Consumer<Children> children) {
		connection.entities().definition(requireNonNull(foreignKey).entityType()).foreignKeys().definition(foreignKey);
		requireNonNull(children);
		for (ForeignKey added : this.children.keySet()) {
			if (added.equals(foreignKey)) {
				throw new IllegalArgumentException("Foreign key " + foreignKey + " has already been added");
			}
			if (added.entityType().equals(foreignKey.entityType()) && added.referencedType().equals(foreignKey.referencedType())) {
				throw new IllegalArgumentException("Foreign key " + foreignKey + " references the same entity type as " + added +
								", an entity would appear twice below a node");
			}
		}
		NodeConfiguration configuration = new NodeConfiguration();
		children.accept(configuration);
		this.children.put(foreignKey, configuration);
		return self();
	}

	@Override
	public final B leaves(Function<List<NodePath<Entity>>, Collection<NodePath<Entity>>> leaves) {
		requireNonNull(leaves);
		options.add(builder -> builder.leaves(leaves));
		return self();
	}

	@Override
	public final B leaf(Predicate<NodePath<Entity>> leaf) {
		requireNonNull(leaf);
		options.add(builder -> builder.leaf(leaf));
		return self();
	}

	@Override
	public final B comparator(@Nullable Comparator<Entity> comparator) {
		this.comparator = comparator;
		this.comparatorSet = true;
		this.comparators = null;
		return self();
	}

	@Override
	public final B comparators(Function<NodePath<Entity>, @Nullable Comparator<Entity>> comparators) {
		this.comparators = requireNonNull(comparators);
		return self();
	}

	@Override
	public final B included(Predicate<NodePath<Entity>> included) {
		requireNonNull(included);
		options.add(builder -> builder.included(included));
		return self();
	}

	@Override
	public final B onLoadException(Consumer<Exception> onLoadException) {
		requireNonNull(onLoadException);
		options.add(builder -> builder.onLoadException(onLoadException));
		return self();
	}

	@Override
	public final B refresh(boolean refresh) {
		options.add(builder -> builder.refresh(refresh));
		return self();
	}

	@Override
	public final B onSelectionChanged(Runnable listener) {
		requireNonNull(listener);
		options.add(builder -> builder.onSelectionChanged(listener));
		return self();
	}

	@Override
	public final B onSelectedItem(Consumer<Entity> item) {
		requireNonNull(item);
		options.add(builder -> builder.onSelectedItem(item));
		return self();
	}

	@Override
	public final B onSelectedItems(Consumer<List<Entity>> items) {
		requireNonNull(items);
		options.add(builder -> builder.onSelectedItems(items));
		return self();
	}

	@Override
	public final B onSelectedPath(Consumer<NodePath<Entity>> path) {
		requireNonNull(path);
		options.add(builder -> builder.onSelectedPath(path));
		return self();
	}

	@Override
	public final B onSelectedPaths(Consumer<List<NodePath<Entity>>> paths) {
		requireNonNull(paths);
		options.add(builder -> builder.onSelectedPaths(paths));
		return self();
	}

	@Override
	public final B onSelectedIndex(Consumer<Integer> index) {
		requireNonNull(index);
		options.add(builder -> builder.onSelectedIndex(index));
		return self();
	}

	@Override
	public final B onSelectedIndexes(Consumer<List<Integer>> indexes) {
		requireNonNull(indexes);
		options.add(builder -> builder.onSelectedIndexes(indexes));
		return self();
	}

	@Override
	public EntityTreeModel build() {
		return new DefaultEntityTreeModel(this);
	}

	/**
	 * @return this builder instance
	 */
	protected final B self() {
		return (B) this;
	}

	/**
	 * The configuration of the roots, or the children via a foreign key.
	 */
	static final class NodeConfiguration implements Roots, Children {

		private static final Consumer<Select.Builder> NO_CONFIGURATION = select -> {};

		@Nullable Supplier<Condition> condition;
		Consumer<Select.Builder> select = NO_CONFIGURATION;
		@Nullable Comparator<Entity> comparator;
		boolean comparatorSet = false;

		@Override
		public NodeConfiguration condition(Supplier<Condition> condition) {
			this.condition = requireNonNull(condition);
			return this;
		}

		@Override
		public NodeConfiguration select(Consumer<Select.Builder> select) {
			this.select = requireNonNull(select);
			return this;
		}

		@Override
		public NodeConfiguration comparator(@Nullable Comparator<Entity> comparator) {
			this.comparator = comparator;
			this.comparatorSet = true;
			return this;
		}
	}
}
