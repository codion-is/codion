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
 * Copyright (c) 2024, Björn Darri Sigurðsson.
 */
package is.codion.swing.framework.model;

import is.codion.common.Operator;
import is.codion.common.event.EventObserver;
import is.codion.common.model.table.ColumnConditionModel;
import is.codion.common.state.State;
import is.codion.common.value.Value;
import is.codion.common.value.ValueSet;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.ForeignKey;
import is.codion.framework.model.EntitySearchModel;
import is.codion.swing.framework.model.component.EntityComboBoxModel;

import java.text.Format;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;

/**
 * A condition model using a {@link EntityComboBoxModel} for the {@link #equalValue()}
 * and a {@link EntitySearchModel} for the {@link #inValues()}.
 * @see #builder(ForeignKey)
 */
public final class SwingForeignKeyConditionModel implements ColumnConditionModel<Attribute<?>, Entity> {

	private final ColumnConditionModel<ForeignKey, Entity> conditionModel;
	private final EntityComboBoxModel equalComboBoxModel;
	private final EntitySearchModel inSearchModel;

	private boolean updatingModel = false;

	private SwingForeignKeyConditionModel(DefaultBuilder builder) {
		this.conditionModel = ColumnConditionModel.builder(builder.foreignKey, Entity.class)
						.operators(builder.operators())
						.operator(defaultOperator(builder))
						.build();
		this.inSearchModel = builder.inSearchModel;
		this.equalComboBoxModel = builder.equalComboBoxModel;
		bindEvents();
	}

	/**
	 * @return the combo box model controlling the equal value
	 * @throws IllegalStateException in case no such model is available
	 */
	public EntityComboBoxModel equalComboBoxModel() {
		if (equalComboBoxModel == null) {
			throw new IllegalStateException("equalComboBoxModel is not available");
		}

		return equalComboBoxModel;
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
	public char wildcard() {
		return conditionModel.wildcard();
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
	 * @return a new {@link SwingForeignKeyConditionModel.Builder}
	 */
	public static SwingForeignKeyConditionModel.Builder builder(ForeignKey foreignKey) {
		return new DefaultBuilder(foreignKey);
	}

	private static Operator defaultOperator(DefaultBuilder builder) {
		if (builder.inSearchModel == null) {
			return Operator.EQUAL;
		}

		boolean searchable = !builder.inSearchModel.connectionProvider().entities()
						.definition(builder.inSearchModel.entityType()).columns().searchable().isEmpty();

		return searchable ? Operator.IN : Operator.EQUAL;
	}

	/**
	 * A builder for a {@link SwingForeignKeyConditionModel}
	 */
	public interface Builder {

		/**
		 * @param equalComboBoxModel the combo box model to use for the EQUAl condition
		 * @return this builder
		 */
		Builder includeEqualOperators(EntityComboBoxModel equalComboBoxModel);

		/**
		 * @param inSearchModel the search model to use for the IN condition
		 * @return this builder
		 */
		Builder includeInOperators(EntitySearchModel inSearchModel);

		/**
		 * @return a new {@link SwingForeignKeyConditionModel} instance
		 */
		SwingForeignKeyConditionModel build();
	}

	private void bindEvents() {
		if (equalComboBoxModel != null) {
			equalComboBoxModel.selectionEvent().addConsumer(new SetEqualValue());
			equalValue().addConsumer(new SelectEqualValue());
			equalComboBoxModel.refresher().refreshEvent().addListener(() -> equalComboBoxModel.setSelectedItem(getEqualValue()));
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

	private final class SelectEqualValue implements Consumer<Entity> {

		@Override
		public void accept(Entity equalValue) {
			updatingModel = true;
			try {
				equalComboBoxModel.setSelectedItem(equalValue);
			}
			finally {
				updatingModel = false;
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

		private EntityComboBoxModel equalComboBoxModel;
		private EntitySearchModel inSearchModel;

		private DefaultBuilder(ForeignKey foreignKey) {
			this.foreignKey = requireNonNull(foreignKey);
		}

		@Override
		public Builder includeEqualOperators(EntityComboBoxModel equalComboBoxModel) {
			this.equalComboBoxModel = requireNonNull(equalComboBoxModel);
			return this;
		}

		@Override
		public Builder includeInOperators(EntitySearchModel inSearchModel) {
			this.inSearchModel = requireNonNull(inSearchModel);
			return this;
		}

		@Override
		public SwingForeignKeyConditionModel build() {
			return new SwingForeignKeyConditionModel(this);
		}

		private List<Operator> operators() {
			if (equalComboBoxModel == null && inSearchModel == null) {
				throw new IllegalStateException("You must include either the EQUAL or IN operator or both");
			}
			if (equalComboBoxModel != null && inSearchModel != null) {
				return asList(Operator.EQUAL, Operator.NOT_EQUAL, Operator.IN, Operator.NOT_IN);
			}
			if (equalComboBoxModel != null) {
				return asList(Operator.EQUAL, Operator.NOT_EQUAL);
			}

			return asList(Operator.IN, Operator.NOT_IN);
		}
	}
}
