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

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * <p>A {@link FilterTreeModel} of entities, arranged by foreign keys: the children of a node are the entities
 * referencing it via the configured foreign keys, selected when the node is loaded.
 * <p>One foreign key referencing its own entity type, {@code Employee.REPORTSTO_FK} for example, applies at every
 * level, so the depth comes from the data, while foreign keys referencing other types form a fixed number of levels,
 * {@code Album.ARTIST_FK} and {@code Track.ALBUM_FK} under artists for example. The two may be mixed: the employees by
 * their manager, each with their customers.
 * <p>The roots are the entities of the root type, by default the ones whose self-referencing foreign keys, if any are
 * configured, are null, the top of the hierarchy.
 * <p>The roots, and the children via each foreign key, can be configured with a condition, the {@link Select} they are
 * selected with, and a comparator, see {@link Builder.RootsStep#roots(EntityType, Consumer)} and
 * {@link Builder#children(ForeignKey, Consumer)}.
 * <p>By default, whether a node is a leaf is decided when its parent is loaded, along with its siblings: a node of an
 * entity type no configured foreign key references is always a leaf, while for the others a single query per foreign
 * key finds which of the siblings are referenced, see {@link Builder#leaves(Function)}.
 * <p>Siblings of different entity types are ordered by type, in the order their foreign keys were configured. Siblings
 * of one type are ordered by the first of: the comparator of the roots or the foreign key, the order by of their
 * select, keeping the query order, the comparator of the tree, see {@link Builder#comparator(Comparator)}, and the
 * {@link is.codion.framework.domain.entity.EntityDefinition#comparator()} of the entity type.
 * <p>The model updates on refresh, {@link Nodes#refresh(NodePath)} reloading a node exactly, while
 * {@link Nodes#add(NodePath, Collection)}, {@link Nodes#replace(NodePath, Object)} and {@link Nodes#remove(Collection)}
 * apply edits in memory.
 * @see #builder()
 */
public interface EntityTreeModel extends FilterTreeModel<Entity> {

	/**
	 * @return the connection used by this tree model
	 */
	EntityConnection connection();

	/**
	 * @return a {@link Builder.RootsStep} instance
	 */
	static Builder.RootsStep builder() {
		return DefaultEntityTreeModel.DefaultBuilder.ROOTS;
	}

	/**
	 * Builds a {@link EntityTreeModel}.
	 * @param <B> the builder type
	 */
	interface Builder<B extends Builder<B>> extends FilterTreeModel.Builder<Entity, B> {

		/**
		 * Provides a {@link ConnectionStep}
		 */
		interface RootsStep {

			/**
			 * @param entityType the type of the root entities
			 * @return a {@link ConnectionStep}
			 */
			ConnectionStep roots(EntityType entityType);

			/**
			 * <p>Configures the query selecting the roots. A condition replaces the default one, the self-referencing
			 * foreign keys being null, which it may include if needed.
			 * {@snippet class = "is.codion.demos.chinook.javadoc.EntityTreeModelSnippets" region = "roots" :
			 * EntityTreeModel.builder() // @start region=roots
			 *         .roots(Employee.TYPE, roots -> roots
			 *                 .condition(() -> Employee.LASTNAME.equalTo("Edwards"))
			 *                 .select(select -> select.orderBy(ascending(Employee.LASTNAME)))); // @end}
			 * @param entityType the type of the root entities
			 * @param roots configures the query selecting the roots
			 * @return a {@link ConnectionStep}
			 * @see Roots
			 */
			ConnectionStep roots(EntityType entityType, Consumer<Roots> roots);
		}

		/**
		 * Provides a {@link Builder}
		 */
		interface ConnectionStep {

			/**
			 * @param connection the connection to use
			 * @return a {@link Builder}
			 */
			Builder<?> connection(EntityConnection connection);
		}

		/**
		 * Adds the entities referencing a node via the given foreign key as its children, for the nodes of the
		 * referenced entity type. A foreign key referencing its own entity type applies at every level.
		 * @param foreignKey the foreign key
		 * @return this builder instance
		 * @throws IllegalArgumentException in case the foreign key has already been added, or another foreign key of
		 * the same entity type referencing the same entity type, since an entity would then appear twice below a node
		 * @see #build()
		 */
		B children(ForeignKey foreignKey);

		/**
		 * <p>Adds the entities referencing a node via the given foreign key as its children, see
		 * {@link #children(ForeignKey)}, configuring the query selecting them. A condition is combined with the
		 * foreign key condition, and applies when finding which nodes have children as well.
		 * {@snippet class = "is.codion.demos.chinook.javadoc.EntityTreeModelSnippets" region = "children" :
		 * builder.children(Employee.REPORTSTO_FK, children -> children // @start region=children
		 *         .condition(() -> Employee.EMAIL.isNotNull())
		 *         .select(select -> select.referenceDepth(0))); // @end}
		 * @param foreignKey the foreign key
		 * @param children configures the query selecting the children
		 * @return this builder instance
		 * @throws IllegalArgumentException in case the foreign key has already been added, or another foreign key of
		 * the same entity type referencing the same entity type
		 * @see Children
		 */
		B children(ForeignKey foreignKey, Consumer<Children> children);

		/**
		 * <p>Replaces the default leaf detection, which queries for the siblings referenced via the configured foreign
		 * keys, a node of an entity type no configured foreign key references always being a leaf.
		 * <p>{@inheritDoc}
		 * @param leaves the leaves function
		 * @return this builder instance
		 */
		@Override
		B leaves(Function<List<NodePath<Entity>>, Collection<NodePath<Entity>>> leaves);

		/**
		 * <p>Replaces the default leaf detection, see {@link #leaves(Function)}, with a function deciding node by node,
		 * based on a domain attribute for example.
		 * <p>{@inheritDoc}
		 * @param leaf the leaf function
		 * @return this builder instance
		 */
		@Override
		B leaf(Predicate<NodePath<Entity>> leaf);

		/**
		 * <p>Specifies the comparator for the roots, and the children via each foreign key, which specify neither a
		 * comparator nor an order by, in place of the comparator of their entity type, null for the query order.
		 * Siblings of different entity types are ordered by type, in the order their foreign keys were configured.
		 * <p>Replaces the comparators specified via {@link #comparators(Function)}, the last one set winning.
		 * @param comparator the comparator, null for the query order
		 * @return this builder instance
		 * @see Roots#comparator(Comparator)
		 * @see Children#comparator(Comparator)
		 */
		@Override
		B comparator(@Nullable Comparator<Entity> comparator);

		/**
		 * <p>Replaces the ordering of siblings entirely, the roots, the foreign key comparators and order bys, and the
		 * entity type comparators.
		 * <p>{@inheritDoc}
		 * @param comparators provides the comparator for the children of a parent, given its path
		 * @return this builder instance
		 */
		@Override
		B comparators(Function<NodePath<Entity>, @Nullable Comparator<Entity>> comparators);

		/**
		 * @return a new {@link EntityTreeModel} instance
		 * @throws IllegalArgumentException in case a foreign key references an entity type which is neither the root
		 * type nor the type of another foreign key, so it can never apply
		 */
		@Override
		EntityTreeModel build();

		/**
		 * <p>Configures the roots: their condition, the {@link Select} they are selected with and their comparator.
		 * <p>A limit or an offset leaves roots out.
		 */
		interface Roots {

			/**
			 * Specifies the roots condition, replacing the default one, the self-referencing foreign keys being null,
			 * which it may include if needed.
			 * @param condition supplies the condition, called each time the roots are selected
			 * @return this roots instance
			 */
			Roots condition(Supplier<Condition> condition);

			/**
			 * Configures the {@link Select} the roots are selected with, the order by, the attributes or the reference
			 * depth for example, its where condition already set.
			 * <p>Called each time the roots are selected, and once when the model is built, to find out whether it
			 * specifies an order by.
			 * @param select configures the select builder
			 * @return this roots instance
			 */
			Roots select(Consumer<Select.Builder> select);

			/**
			 * Specifies the comparator for the roots, in place of the order by of their select, the comparator of the
			 * tree and the comparator of the entity type.
			 * @param comparator the comparator, null for the query order
			 * @return this roots instance
			 * @see Builder#comparator(Comparator)
			 */
			Roots comparator(@Nullable Comparator<Entity> comparator);
		}

		/**
		 * <p>Configures the children via a foreign key: their condition, combined with the foreign key condition owned
		 * by the tree, the {@link Select} they are selected with and their comparator.
		 * <p>A limit or an offset leaves children out.
		 */
		interface Children {

			/**
			 * Specifies a condition the children must satisfy, combined with the foreign key condition, applying when
			 * finding which nodes have children as well.
			 * @param condition supplies the condition, called each time the children are selected
			 * @return this children instance
			 */
			Children condition(Supplier<Condition> condition);

			/**
			 * Configures the {@link Select} the children are selected with, the order by, the attributes or the reference
			 * depth for example, its where condition already set. Its order by does not apply when finding which nodes
			 * have children.
			 * <p>Called each time the children are selected, and once when the model is built, to find out whether it
			 * specifies an order by.
			 * @param select configures the select builder
			 * @return this children instance
			 */
			Children select(Consumer<Select.Builder> select);

			/**
			 * Specifies the comparator for the children, in place of the order by of their select, the comparator of the
			 * tree and the comparator of the entity type.
			 * @param comparator the comparator, null for the query order
			 * @return this children instance
			 * @see Builder#comparator(Comparator)
			 */
			Children comparator(@Nullable Comparator<Entity> comparator);
		}
	}
}
