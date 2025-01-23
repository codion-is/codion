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
 * Copyright (c) 2023 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.framework.domain.entity.condition;

import is.codion.common.Conjunction;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.Column;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static is.codion.common.Operator.EQUAL;
import static is.codion.framework.domain.entity.condition.DefaultForeignKeyConditionFactory.compositeEqualCondition;
import static java.util.Objects.requireNonNull;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

/**
 * <p>Specifies a query condition.</p>
 * {@link Condition.All} indicates no condition, as in, all entities should be returned.
 * @see #all(EntityType)
 * @see #key(Entity.Key)
 * @see #keys(Collection)
 * @see #and(Condition...)
 * @see #and(Collection)
 * @see #or(Condition...)
 * @see #or(Collection)
 * @see #combination(Conjunction, Condition...)
 * @see #combination(Conjunction, Condition...)
 */
public interface Condition {

	/**
	 * @return the entity type
	 */
	EntityType entityType();

	/**
	 * @return a list of the values this condition is based on, in the order they appear
	 * in the condition clause. An empty list is returned in case no values are specified.
	 */
	List<?> values();

	/**
	 * @return a list of the columns this condition is based on, in the same
	 * order as their respective values appear in the condition clause.
	 * An empty list is returned in case no values are specified.
	 */
	List<Column<?>> columns();

	/**
	 * Returns a string representing this condition, e.g. "column = ?" or "col1 is not null and col2 in (?, ?)".
	 * @param definition the entity definition
	 * @return a condition string
	 */
	String toString(EntityDefinition definition);

	/**
	 * A condition specifying all entities of a given type, a no-condition.
	 */
	interface All extends Condition {}

	/**
	 * An interface encapsulating a combination of Condition instances,
	 * that should be either AND'ed or OR'ed together in a query context
	 */
	interface Combination extends Condition {

		/**
		 * @return the condition comprising this Combination
		 */
		Collection<Condition> conditions();

		/**
		 * @return the conjunction
		 */
		Conjunction conjunction();
	}

	/**
	 * @param entityType the entity type
	 * @return a Condition specifying all entities of the given type
	 */
	static Condition all(EntityType entityType) {
		return new DefaultAllCondition(entityType);
	}

	/**
	 * Creates a {@link Condition} based on the given key
	 * @param key the key
	 * @return a condition based on the given key
	 */
	static Condition key(Entity.Key key) {
		if (requireNonNull(key).columns().size() > 1) {
			Map<Column<?>, Column<?>> columnMap = key.columns().stream()
							.collect(toMap(identity(), identity()));
			Map<Column<?>, Object> valueMap = new HashMap<>();
			key.columns().forEach(column -> valueMap.put(column, key.get(column)));

			return compositeEqualCondition(columnMap, EQUAL, valueMap);
		}

		return key.column().equalTo(key.get());
	}

	/**
	 * Creates a {@link Condition} based on the given keys.
	 * @param keys the keys
	 * @return a condition based on the given keys
	 * @throws IllegalArgumentException in case {@code keys} is empty or if it contains keys from multiple entity types
	 */
	static Condition keys(Collection<Entity.Key> keys) {
		if (requireNonNull(keys).isEmpty()) {
			throw new IllegalArgumentException("No keys specified for key condition");
		}
		Set<EntityType> entityTypes = keys.stream()
						.map(Entity.Key::type)
						.collect(Collectors.toSet());
		if (entityTypes.size() > 1) {
			throw new IllegalArgumentException("Multiple entity types found among keys");
		}
		Entity.Key firstKey = (keys instanceof List) ? ((List<Entity.Key>) keys).get(0) : keys.iterator().next();
		if (firstKey.columns().size() > 1) {
			return compositeEqualCondition(firstKey, keys);
		}

		return firstKey.column().in(Entity.values(keys));
	}

	/**
	 * Returns a new {@link Combination} instance, combining the given conditions using the AND conjunction.
	 * @param conditions the conditions to combine
	 * @return a new conditions combination
	 */
	static Combination and(Condition... conditions) {
		return and(Arrays.asList(conditions));
	}

	/**
	 * Returns a new {@link Combination} instance, combining the given conditions using the AND conjunction.
	 * @param conditions the conditions to combine
	 * @return a new conditions combination
	 */
	static Combination and(Collection<Condition> conditions) {
		return combination(Conjunction.AND, conditions);
	}

	/**
	 * Returns a new {@link Combination} instance, combining the given conditions using the OR conjunction.
	 * @param conditions the conditions to combine
	 * @return a new conditions combination
	 */
	static Combination or(Condition... conditions) {
		return or(Arrays.asList(conditions));
	}

	/**
	 * Returns a new {@link Combination} instance, combining the given conditions using the OR conjunction.
	 * @param conditions the conditions to combine
	 * @return a new conditions combination
	 */
	static Combination or(Collection<Condition> conditions) {
		return combination(Conjunction.OR, conditions);
	}

	/**
	 * Initializes a new {@link Combination} instance
	 * @param conjunction the Conjunction to use
	 * @param conditions the conditions to combine
	 * @return a new {@link Combination} instance
	 * @throws IllegalArgumentException in case {@code conditions} is empty
	 */
	static Combination combination(Conjunction conjunction, Condition... conditions) {
		return combination(conjunction, Arrays.asList(requireNonNull(conditions)));
	}

	/**
	 * Initializes a new {@link Combination} instance
	 * @param conjunction the Conjunction to use
	 * @param conditions the conditions to combine
	 * @return a new {@link Combination} instance
	 * @throws IllegalArgumentException in case {@code conditions} is empty
	 */
	static Combination combination(Conjunction conjunction, Collection<Condition> conditions) {
		return new DefaultConditionCombination(conjunction, new ArrayList<>(requireNonNull(conditions)));
	}
}
