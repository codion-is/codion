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

import is.codion.common.model.component.tree.NodePath;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.EntityConnection.Select;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.attribute.ForeignKey;
import is.codion.framework.domain.entity.attribute.ForeignKey.Reference;
import is.codion.framework.domain.entity.condition.Condition;
import is.codion.framework.model.AbstractEntityTreeModelBuilder.NodeConfiguration;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import static is.codion.framework.domain.entity.condition.Condition.and;
import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

/**
 * The structure of an entity tree, given by its root type and foreign keys: the queries selecting the roots, the
 * children and the leaves, and the order of siblings.
 */
final class EntityTreeStructure {

	private final EntityConnection connection;
	private final Entities entities;
	private final EntityType rootType;
	private final NodeConfiguration roots;
	private final Map<ForeignKey, NodeConfiguration> children = new LinkedHashMap<>();
	//the foreign keys referencing each entity type, the children of its nodes
	private final Map<EntityType, List<ForeignKey>> referencing = new HashMap<>();
	//the foreign keys of the root type referencing the root type, null for the roots
	private final List<ForeignKey> selfReferencing = new ArrayList<>();
	//the entity types, the root type first, followed by the others in the order configured
	private final List<EntityType> types = new ArrayList<>();
	//the comparator for the roots, null for the query order
	private final @Nullable Comparator<Entity> rootsComparator;
	//the comparator for the children of the nodes of each entity type, a null value for the query order
	private final Map<EntityType, @Nullable Comparator<Entity>> childrenComparators = new HashMap<>();

	EntityTreeStructure(AbstractEntityTreeModelBuilder<?> builder) {
		this.connection = builder.connection;
		this.entities = connection.entities();
		this.rootType = builder.rootType;
		this.roots = builder.roots;
		builder.children.forEach((foreignKey, configuration) -> {
			this.children.put(foreignKey, configuration);
			referencing.computeIfAbsent(foreignKey.referencedType(), type -> new ArrayList<>()).add(foreignKey);
			if (foreignKey.entityType().equals(rootType) && foreignKey.referencedType().equals(rootType)) {
				selfReferencing.add(foreignKey);
			}
		});
		this.types.addAll(reachable(rootType, children.keySet()));
		for (ForeignKey foreignKey : children.keySet()) {
			if (!types.contains(foreignKey.referencedType())) {
				throw new IllegalArgumentException("Foreign key " + foreignKey + " references " + foreignKey.referencedType() +
								", which can not be reached from the root type " + rootType + " via the other foreign keys");
			}
		}
		this.rootsComparator = comparator(roots, rootType, builder);
		Map<ForeignKey, @Nullable Comparator<Entity>> comparators = new HashMap<>();
		children.forEach((foreignKey, configuration) ->
						comparators.put(foreignKey, comparator(configuration, foreignKey.entityType(), builder)));
		referencing.forEach((entityType, foreignKeys) ->
						childrenComparators.put(entityType, comparator(foreignKeys, comparators)));
	}

	/**
	 * @return the entity types in this tree
	 */
	List<EntityType> types() {
		return types;
	}

	/**
	 * @return the roots, by default the ones whose self-referencing foreign keys are null
	 */
	List<Entity> roots() {
		return connection.select(select(roots, roots.condition == null ?
						defaultRootsCondition() : condition(rootType, roots.condition)));
	}

	/**
	 * @param path the path of the parent
	 * @return the entities referencing the parent via the foreign keys referencing its type, except the ones above
	 * it on the path, since a cycle in the data would allow descending forever
	 */
	List<Entity> children(NodePath<Entity> path) {
		Entity parent = path.item();
		List<Entity> children = new ArrayList<>();
		for (ForeignKey foreignKey : referencing.getOrDefault(parent.type(), emptyList())) {
			children.addAll(connection.select(select(this.children.get(foreignKey), condition(foreignKey, foreignKey.equalTo(parent)))));
		}
		children.removeIf(path.items()::contains);

		return children;
	}

	/**
	 * The default leaves function. A node of an entity type no foreign key references is a leaf, for the others a
	 * single query per foreign key referencing their type finds which of them are referenced.
	 * @param paths the paths of nodes with the same parent
	 * @return the paths of the nodes which are leaves
	 */
	Collection<NodePath<Entity>> leaves(List<NodePath<Entity>> paths) {
		Map<EntityType, List<NodePath<Entity>>> byType = new LinkedHashMap<>();
		paths.forEach(path -> byType.computeIfAbsent(path.item().type(), type -> new ArrayList<>()).add(path));
		List<NodePath<Entity>> leaves = new ArrayList<>();
		byType.forEach((type, typePaths) -> {
			List<ForeignKey> foreignKeys = referencing.getOrDefault(type, emptyList());
			if (foreignKeys.isEmpty()) {
				leaves.addAll(typePaths);
			}
			else {
				List<Entity> parents = typePaths.stream()
								.map(NodePath::item)
								.distinct()
								.collect(toList());
				Map<ForeignKey, Set<List<Object>>> referenced = new HashMap<>();
				foreignKeys.forEach(foreignKey -> referenced.put(foreignKey, referenced(foreignKey, parents)));
				typePaths.stream()
								.filter(path -> foreignKeys.stream()
												.noneMatch(foreignKey -> referenced.get(foreignKey).contains(foreignValues(path.item(), foreignKey))))
								.forEach(leaves::add);
			}
		});

		return leaves;
	}

	/**
	 * @param parent the path of the parent
	 * @return the comparator for the children of the given parent, null for the query order
	 */
	@Nullable Comparator<Entity> comparator(NodePath<Entity> parent) {
		return parent.root() ? rootsComparator : childrenComparators.get(parent.item().type());
	}

	/**
	 * @return the entity types reachable from the root type via the given foreign keys, the root type first, the types
	 * referencing the same entity type in the order their foreign keys were configured
	 */
	private static Set<EntityType> reachable(EntityType rootType, Collection<ForeignKey> foreignKeys) {
		Set<EntityType> reachable = new LinkedHashSet<>();
		reachable.add(rootType);
		boolean added;
		do {
			added = false;
			for (ForeignKey foreignKey : foreignKeys) {
				if (reachable.contains(foreignKey.referencedType()) && reachable.add(foreignKey.entityType())) {
					added = true;
				}
			}
		}
		while (added);

		return reachable;
	}

	private Condition defaultRootsCondition() {
		if (selfReferencing.isEmpty()) {
			return Condition.all(rootType);
		}

		return and(selfReferencing.stream()
						.map(ForeignKey::isNull)
						.collect(toList()));
	}

	/**
	 * @return the values of the columns referenced via the given foreign key, of the given parents which are referenced
	 */
	private Set<List<Object>> referenced(ForeignKey foreignKey, List<Entity> parents) {
		Select query = select(children.get(foreignKey), condition(foreignKey, foreignKey.in(parents)));
		List<Reference<?>> references = foreignKey.references();
		if (references.size() == 1) {
			return values(references.get(0).column(), conditions(query).build());
		}
		Set<List<Object>> referenced = new HashSet<>();
		for (Entity entity : connection.select(conditions(query)
						.attributes(references.stream()
										.map(Reference::column)
										.collect(toList()))
						.build())) {
			List<Object> values = references.stream()
							.map(reference -> (Object) entity.get(reference.column()))
							.collect(toList());
			if (!values.contains(null)) {
				referenced.add(values);
			}
		}

		return referenced;
	}

	private <T> Set<List<Object>> values(Column<T> column, Select select) {
		Set<List<Object>> values = new HashSet<>();
		connection.select(column, select).forEach(value -> values.add(Collections.<Object>singletonList(value)));

		return values;
	}

	private static List<Object> foreignValues(Entity entity, ForeignKey foreignKey) {
		return foreignKey.references().stream()
						.map(reference -> (Object) entity.get(reference.foreign()))
						.collect(toList());
	}

	/**
	 * @return the foreign key condition, combined with the condition of its query, if any
	 */
	private Condition condition(ForeignKey foreignKey, Condition condition) {
		Supplier<Condition> supplier = children.get(foreignKey).condition;

		return supplier == null ? condition : and(condition, condition(foreignKey.entityType(), supplier));
	}

	/**
	 * The first of: the comparator of the roots or the foreign key, the query order in case its select specifies an
	 * order by, the comparator of the tree, and the comparator of the entity type.
	 * @return the comparator for the entities the given configuration selects, null for the query order
	 */
	private @Nullable Comparator<Entity> comparator(NodeConfiguration configuration, EntityType entityType,
																									AbstractEntityTreeModelBuilder<?> builder) {
		if (configuration.comparatorSet) {
			return configuration.comparator;
		}
		if (ordered(configuration, entityType)) {
			return null;
		}
		if (builder.comparatorSet) {
			return builder.comparator;
		}

		return entities.definition(entityType).comparator();
	}

	/**
	 * @return true if the select of the given configuration specifies an order by, the select configured once to find out
	 */
	private static boolean ordered(NodeConfiguration configuration, EntityType entityType) {
		Select.Builder select = Select.where(Condition.all(entityType));
		configuration.select.accept(select);

		return select.build().orderBy().isPresent();
	}

	/**
	 * @param foreignKeys the foreign keys referencing an entity type, in the order configured
	 * @param comparators the comparator of each foreign key
	 * @return the comparator for the children of a node of the entity type, by type, in the order the foreign keys were
	 * configured, then by the comparator of each foreign key, null in case all keep the query order
	 */
	private static @Nullable Comparator<Entity> comparator(List<ForeignKey> foreignKeys,
																												 Map<ForeignKey, @Nullable Comparator<Entity>> comparators) {
		if (foreignKeys.size() == 1) {
			return comparators.get(foreignKeys.get(0));
		}
		if (foreignKeys.stream().allMatch(foreignKey -> comparators.get(foreignKey) == null)) {
			//the children queries run in the order configured
			return null;
		}
		List<EntityType> types = foreignKeys.stream()
						.map(ForeignKey::entityType)
						.collect(toList());
		Map<EntityType, @Nullable Comparator<Entity>> byType = new HashMap<>();
		foreignKeys.forEach(foreignKey -> byType.put(foreignKey.entityType(), comparators.get(foreignKey)));

		return (entity, other) -> {
			int byRank = Integer.compare(types.indexOf(entity.type()), types.indexOf(other.type()));
			if (byRank != 0) {
				return byRank;
			}
			Comparator<Entity> comparator = byType.get(entity.type());

			return comparator == null ? 0 : comparator.compare(entity, other);
		};
	}

	/**
	 * @return the condition supplied
	 * @throws IllegalArgumentException in case the condition is for another entity type
	 */
	private static Condition condition(EntityType entityType, Supplier<Condition> supplier) {
		Condition condition = requireNonNull(supplier.get(), "The condition supplier returned null");
		if (!condition.entityType().equals(entityType)) {
			throw new IllegalArgumentException("A condition for " + entityType + " expected, got: " + condition.entityType());
		}

		return condition;
	}

	/**
	 * @return the select of the given query, with the given where condition
	 */
	private static Select select(NodeConfiguration configuration, Condition where) {
		Select.Builder builder = Select.where(where);
		configuration.select.accept(builder);

		return builder.build();
	}

	/**
	 * @return a select builder with the conditions and timeout of the given query, without its order by, which a
	 * query selecting the distinct values of a column may not contain
	 */
	private static Select.Builder conditions(Select query) {
		Select.Builder builder = Select.where(query.where())
						.having(query.having());
		query.timeout().ifPresent(builder::timeout);

		return builder;
	}
}
