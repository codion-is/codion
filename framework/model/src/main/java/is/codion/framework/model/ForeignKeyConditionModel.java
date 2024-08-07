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
 * Copyright (c) 2023 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.framework.model;

import is.codion.common.Operator;
import is.codion.common.event.EventObserver;
import is.codion.common.model.table.ColumnConditionModel;
import is.codion.common.state.State;
import is.codion.common.value.Value;
import is.codion.common.value.ValueSet;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.ForeignKey;

import java.text.Format;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;

/**
 * A default foreign key condition model using {@link EntitySearchModel} for
 * both the {@link #equalValue()} and {@link #inValues()}.
 * @see #builder(ForeignKey)
 */
public final class ForeignKeyConditionModel implements ColumnConditionModel<Attribute<?>, Entity> {

	private final ColumnConditionModel<ForeignKey, Entity> conditionModel;
	private final EntitySearchModel equalSearchModel;
	private final EntitySearchModel inSearchModel;

	private boolean updatingModel = false;

	private ForeignKeyConditionModel(DefaultBuilder builder) {
		this.conditionModel = ColumnConditionModel.builder(builder.foreignKey, Entity.class)
						.operators(builder.operators())
						.operator(builder.inSearchModel == null ? Operator.EQUAL : Operator.IN)
						.build();
		this.equalSearchModel = builder.equalSearchModel;
		this.inSearchModel = builder.inSearchModel;
		bindEvents();
	}

	/**
	 * @return the combo box model controlling the equal value
	 * @throws IllegalStateException in case no such model is available
	 */
	public EntitySearchModel equalSearchModel() {
		if (equalSearchModel == null) {
			throw new IllegalStateException("equalSearchModel is not available");
		}

		return equalSearchModel;
	}

	/**
	 * @return the search model controlling the in values
	 * @throws IllegalStateException in case no such model is available
	 */
	public EntitySearchModel inSearchModel() {
		if (inSearchModel == null) {
			throw new IllegalStateException("inSearchModel is not available");
		}

		return inSearchModel;
	}

	@Override
	public Attribute<?> columnIdentifier() {
		return conditionModel.columnIdentifier();
	}

	@Override
	public State caseSensitive() {
		return conditionModel.caseSensitive();
	}

	@Override
	public Optional<Format> format() {
		return conditionModel.format();
	}

	@Override
	public String dateTimePattern() {
		return conditionModel.dateTimePattern();
	}

	@Override
	public Value<AutomaticWildcard> automaticWildcard() {
		return conditionModel.automaticWildcard();
	}

	@Override
	public State autoEnable() {
		return conditionModel.autoEnable();
	}

	@Override
	public State locked() {
		return conditionModel.locked();
	}

	@Override
	public Class<Entity> columnClass() {
		return conditionModel.columnClass();
	}

	@Override
	public void setEqualValue(Entity value) {
		conditionModel.setEqualValue(value);
	}

	@Override
	public Entity getEqualValue() {
		return conditionModel.getEqualValue();
	}

	@Override
	public void setInValues(Collection<Entity> values) {
		conditionModel.setInValues(values);
	}

	@Override
	public Collection<Entity> getInValues() {
		return conditionModel.getInValues();
	}

	@Override
	public void setUpperBound(Entity upper) {
		conditionModel.setUpperBound(upper);
	}

	@Override
	public Entity getUpperBound() {
		return conditionModel.getUpperBound();
	}

	@Override
	public void setLowerBound(Entity value) {
		conditionModel.setLowerBound(value);
	}

	@Override
	public Entity getLowerBound() {
		return conditionModel.getLowerBound();
	}

	@Override
	public List<Operator> operators() {
		return conditionModel.operators();
	}

	@Override
	public State enabled() {
		return conditionModel.enabled();
	}

	@Override
	public void clear() {
		conditionModel.clear();
	}

	@Override
	public Value<Entity> equalValue() {
		return conditionModel.equalValue();
	}

	@Override
	public ValueSet<Entity> inValues() {
		return conditionModel.inValues();
	}

	@Override
	public Value<Entity> upperBoundValue() {
		return conditionModel.upperBoundValue();
	}

	@Override
	public Value<Entity> lowerBoundValue() {
		return conditionModel.lowerBoundValue();
	}

	@Override
	public Value<Operator> operator() {
		return conditionModel.operator();
	}

	@Override
	public boolean accepts(Comparable<Entity> columnValue) {
		return conditionModel.accepts(columnValue);
	}

	@Override
	public EventObserver<?> conditionChangedEvent() {
		return conditionModel.conditionChangedEvent();
	}

	/**
	 * @param foreignKey the foreign key
	 * @return a new {@link ForeignKeyConditionModel.Builder}
	 */
	public static Builder builder(ForeignKey foreignKey) {
		return new DefaultBuilder(foreignKey);
	}

	/**
	 * A builder for a {@link ForeignKeyConditionModel}
	 */
	public interface Builder {

		/**
		 * @param equalSearchModel the search model to use for the EQUAl condition
		 * @return this builder
		 */
		Builder includeEqualOperators(EntitySearchModel equalSearchModel);

		/**
		 * @param inSearchModel the search model to use for the IN condition
		 * @return this builder
		 */
		Builder includeInOperators(EntitySearchModel inSearchModel);

		/**
		 * @return a new {@link ForeignKeyConditionModel} instance
		 */
		ForeignKeyConditionModel build();
	}

	private void bindEvents() {
		if (equalSearchModel != null) {
			equalSearchModel.entity().addConsumer(new SetEqualValue());
			equalValue().addConsumer(new SelectEqualValue());
		}
		if (inSearchModel != null) {
			inSearchModel.entities().addConsumer(new SetInValues());
			inValues().addConsumer(new SelectInValues());
		}
	}

	private final class SetEqualValue implements Consumer<Entity> {

		@Override
		public void accept(Entity selectedEntity) {
			if (!updatingModel) {
				setEqualValue(selectedEntity);
			}
		}
	}

	private final class SetInValues implements Consumer<Set<Entity>> {

		@Override
		public void accept(Set<Entity> selectedEntities) {
			if (!updatingModel) {
				setInValues(selectedEntities);
			}
		}
	}

	private final class SelectEqualValue implements Consumer<Entity> {

		@Override
		public void accept(Entity equalValue) {
			updatingModel = true;
			try {
				equalSearchModel.entity().set(equalValue);
			}
			finally {
				updatingModel = false;
			}
		}
	}

	private final class SelectInValues implements Consumer<Set<Entity>> {

		@Override
		public void accept(Set<Entity> inValues) {
			updatingModel = true;
			try {
				inSearchModel.entities().set(inValues);
			}
			finally {
				updatingModel = false;
			}
		}
	}

	private static final class DefaultBuilder implements Builder {

		private final ForeignKey foreignKey;

		private EntitySearchModel equalSearchModel;
		private EntitySearchModel inSearchModel;

		private DefaultBuilder(ForeignKey foreignKey) {
			this.foreignKey = requireNonNull(foreignKey);
		}

		@Override
		public Builder includeEqualOperators(EntitySearchModel equalSearchModel) {
			this.equalSearchModel = requireNonNull(equalSearchModel);
			return this;
		}

		@Override
		public Builder includeInOperators(EntitySearchModel inSearchModel) {
			this.inSearchModel = requireNonNull(inSearchModel);
			return this;
		}

		@Override
		public ForeignKeyConditionModel build() {
			return new ForeignKeyConditionModel(this);
		}

		private List<Operator> operators() {
			if (equalSearchModel == null && inSearchModel == null) {
				throw new IllegalStateException("You must specify either an equalSearchModel or an inSearchModel");
			}
			if (equalSearchModel != null && inSearchModel != null) {
				return asList(Operator.EQUAL, Operator.NOT_EQUAL, Operator.IN, Operator.NOT_IN);
			}
			if (equalSearchModel != null) {
				return asList(Operator.EQUAL, Operator.NOT_EQUAL);
			}

			return asList(Operator.IN, Operator.NOT_IN);
		}
	}
}
