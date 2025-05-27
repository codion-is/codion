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
 * Copyright (c) 2010 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.framework.model;

import is.codion.common.model.condition.ConditionModel;
import is.codion.common.model.condition.ConditionModel.Operands;
import is.codion.common.value.Value;
import is.codion.common.value.Value.Notify;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.attribute.ColumnDefinition;
import is.codion.framework.domain.entity.attribute.ForeignKey;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.util.Collections.unmodifiableMap;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toMap;

/**
 * A default {@link ConditionModel} factory for Entity based condition models.
 */
public class EntityConditionModelFactory implements Supplier<Map<Attribute<?>, ConditionModel<?>>> {

	private final EntityType entityType;
	private final EntityConnectionProvider connectionProvider;

	/**
	 * Instantiates a new {@link EntityConditionModelFactory}.
	 * @param entityType the entity type
	 * @param connectionProvider the connection provider
	 */
	public EntityConditionModelFactory(EntityType entityType, EntityConnectionProvider connectionProvider) {
		this.entityType = requireNonNull(entityType);
		this.connectionProvider = requireNonNull(connectionProvider);
	}

	@Override
	public final Map<Attribute<?>, ConditionModel<?>> get() {
		Map<Attribute<?>, ConditionModel<?>> models = new HashMap<>();
		models.putAll(definition().columns().get().stream()
						.filter(this::include)
						.collect(toMap(Function.identity(), this::conditionModel)));
		models.putAll(definition().foreignKeys().get().stream()
						.filter(this::include)
						.collect(toMap(Function.identity(), this::conditionModel)));

		return unmodifiableMap(models);
	}

	/**
	 * @param column the column
	 * @return true if a condition model should be included for the given column
	 */
	protected boolean include(Column<?> column) {
		return true;
	}

	/**
	 * @param foreignKey the foreign key
	 * @return true if a condition model should be included for the given foreign key
	 */
	protected boolean include(ForeignKey foreignKey) {
		return true;
	}

	/**
	 * Only called if {@link #include(Column)} returns true
	 * @param column the column
	 * @param <T> the column type
	 * @return a {@link ConditionModel} based on the given column
	 */
	protected <T> ConditionModel<T> conditionModel(Column<T> column) {
		ColumnDefinition<T> definition = definition().columns().definition(column);

		return ConditionModel.builder(column.type().valueClass())
						.format(definition.format().orElse(null))
						.dateTimePattern(definition.dateTimePattern().orElse(null))
						.operands(new ColumnOperands<>(definition))
						.build();
	}

	/**
	 * Only called if {@link #include(ForeignKey)} returns true
	 * @param foreignKey the foreign key
	 * @return a {@link ForeignKeyConditionModel} based on the given foreign key
	 */
	protected ForeignKeyConditionModel conditionModel(ForeignKey foreignKey) {
		return ForeignKeyConditionModel.builder()
						.equalSearchModel(createEqualSearchModel(foreignKey))
						.inSearchModel(createInSearchModel(foreignKey))
						.build();
	}

	/**
	 * @param foreignKey the foreign key
	 * @return a search model to use for the equal value
	 */
	protected EntitySearchModel createEqualSearchModel(ForeignKey foreignKey) {
		return EntitySearchModel.builder(requireNonNull(foreignKey).referencedType(), connectionProvider)
						.singleSelection(true)
						.build();
	}

	/**
	 * @param foreignKey the foreign key
	 * @return a search model to use for the in values
	 */
	protected EntitySearchModel createInSearchModel(ForeignKey foreignKey) {
		return EntitySearchModel.builder(requireNonNull(foreignKey).referencedType(), connectionProvider).build();
	}

	/**
	 * @return the underlying connection provider
	 */
	protected final EntityConnectionProvider connectionProvider() {
		return connectionProvider;
	}

	/**
	 * @return the underlying {@link EntityDefinition}
	 */
	protected final EntityDefinition definition() {
		return definition(entityType);
	}

	/**
	 * @param entityType the entity type
	 * @return the definition of the given type
	 */
	protected final EntityDefinition definition(EntityType entityType) {
		return connectionProvider.entities().definition(entityType);
	}

	private static final class ColumnOperands<T> implements Operands<T> {

		private final ColumnDefinition<T> definition;

		private ColumnOperands(ColumnDefinition<T> definition) {
			this.definition = definition;
		}

		@Override
		public Value<T> equal() {
			if (definition.attribute().type().isBoolean() && !definition.nullable()) {
				return (Value<T>) Value.builder()
								.nonNull(false)
								.notify(Notify.SET)
								.build();
			}

			return Operands.super.equal();
		}
	}
}
