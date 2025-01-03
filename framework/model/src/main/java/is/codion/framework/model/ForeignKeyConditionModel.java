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
package is.codion.framework.model;

import is.codion.common.Operator;
import is.codion.common.model.condition.ConditionModel;
import is.codion.common.observable.Observer;
import is.codion.common.state.State;
import is.codion.common.value.Value;
import is.codion.framework.domain.entity.Entity;

import java.text.Format;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;

/**
 * A default foreign key condition model using {@link EntitySearchModel} for
 * both the {@link Operator#EQUAL} and {@link Operator#IN} operands.
 * @see #builder()
 */
public final class ForeignKeyConditionModel implements ConditionModel<Entity> {

	private final ConditionModel<Entity> condition;
	private final EntitySearchModel equalSearchModel;
	private final EntitySearchModel inSearchModel;

	private boolean updatingModel = false;

	private ForeignKeyConditionModel(DefaultBuilder builder) {
		this.condition = ConditionModel.builder(Entity.class)
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
	public State caseSensitive() {
		return condition.caseSensitive();
	}

	@Override
	public Optional<Format> format() {
		return condition.format();
	}

	@Override
	public Optional<String> dateTimePattern() {
		return condition.dateTimePattern();
	}

	@Override
	public Value<Wildcard> wildcard() {
		return condition.wildcard();
	}

	@Override
	public State autoEnable() {
		return condition.autoEnable();
	}

	@Override
	public State locked() {
		return condition.locked();
	}

	@Override
	public Class<Entity> valueClass() {
		return condition.valueClass();
	}

	@Override
	public List<Operator> operators() {
		return condition.operators();
	}

	@Override
	public State enabled() {
		return condition.enabled();
	}

	@Override
	public void clear() {
		condition.clear();
	}

	@Override
	public Value<Operator> operator() {
		return condition.operator();
	}

	@Override
	public Operands<Entity> operands() {
		return condition.operands();
	}

	@Override
	public boolean accepts(Comparable<Entity> value) {
		return condition.accepts(value);
	}

	@Override
	public Observer<?> changed() {
		return condition.changed();
	}

	/**
	 * @return a new {@link ForeignKeyConditionModel.Builder}
	 */
	public static Builder builder() {
		return new DefaultBuilder();
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
			equalSearchModel.selection().entity().addConsumer(new SetEqualOperand());
			operands().equal().addConsumer(new SelectEqualOperand());
		}
		if (inSearchModel != null) {
			inSearchModel.selection().entities().addConsumer(new SetInOperands());
			operands().in().addConsumer(new SelectInOperands());
		}
	}

	private final class SetEqualOperand implements Consumer<Entity> {

		@Override
		public void accept(Entity selectedEntity) {
			if (!updatingModel) {
				operands().equal().set(selectedEntity);
			}
		}
	}

	private final class SetInOperands implements Consumer<Set<Entity>> {

		@Override
		public void accept(Set<Entity> selectedEntities) {
			if (!updatingModel) {
				operands().in().set(selectedEntities);
			}
		}
	}

	private final class SelectEqualOperand implements Consumer<Entity> {

		@Override
		public void accept(Entity equalOperand) {
			updatingModel = true;
			try {
				equalSearchModel.selection().entity().set(equalOperand);
			}
			finally {
				updatingModel = false;
			}
		}
	}

	private final class SelectInOperands implements Consumer<Set<Entity>> {

		@Override
		public void accept(Set<Entity> inOperands) {
			updatingModel = true;
			try {
				inSearchModel.selection().entities().set(inOperands);
			}
			finally {
				updatingModel = false;
			}
		}
	}

	private static final class DefaultBuilder implements Builder {

 		private EntitySearchModel equalSearchModel;
		private EntitySearchModel inSearchModel;

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
				throw new IllegalStateException("Neither EQUAL nor IN operator specified");
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
