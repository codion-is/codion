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
 * Copyright (c) 2024 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.swing.framework.model;

import is.codion.common.Operator;
import is.codion.common.model.condition.ConditionModel;
import is.codion.common.observable.Observer;
import is.codion.common.state.State;
import is.codion.common.value.Value;
import is.codion.common.value.ValueSet;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.model.EntitySearchModel;
import is.codion.swing.framework.model.component.EntityComboBoxModel;

import java.text.Format;
import java.util.List;
import java.util.Optional;

import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;

/**
 * A condition model using a {@link EntityComboBoxModel} for the {@link Operator#EQUAL} operand
 * and a {@link EntitySearchModel} for the {@link Operator#IN} operands.
 * @see #builder()
 */
public final class SwingForeignKeyConditionModel implements ConditionModel<Entity> {

	private final ConditionModel<Entity> condition;
	private final SwingForeignKeyOperands operands;

	private SwingForeignKeyConditionModel(DefaultBuilder builder) {
		this.condition = ConditionModel.builder(Entity.class)
						.operators(builder.operators())
						.operator(defaultOperator(builder))
						.build();
		this.operands = new SwingForeignKeyOperands(condition, builder.equalComboBoxModel, builder.inSearchModel);
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
	public SwingForeignKeyOperands operands() {
		return operands;
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
	 * @return a new {@link SwingForeignKeyConditionModel.Builder}
	 */
	public static SwingForeignKeyConditionModel.Builder builder() {
		return new DefaultBuilder();
	}

	private static Operator defaultOperator(DefaultBuilder builder) {
		if (builder.inSearchModel == null) {
			return Operator.EQUAL;
		}

		boolean searchable = !builder.inSearchModel.entityDefinition().columns().searchable().isEmpty();

		return searchable ? Operator.IN : Operator.EQUAL;
	}

	/**
	 * A builder for a {@link SwingForeignKeyConditionModel}
	 */
	public interface Builder {

		/**
		 * @param equalComboBoxModel the combo box model to use for the EQUAl operator
		 * @return this builder
		 */
		Builder equalComboBoxModel(EntityComboBoxModel equalComboBoxModel);

		/**
		 * @param inSearchModel the search model to use for the IN operator
		 * @return this builder
		 */
		Builder inSearchModel(EntitySearchModel inSearchModel);

		/**
		 * @return a new {@link SwingForeignKeyConditionModel} instance
		 */
		SwingForeignKeyConditionModel build();
	}

	/**
	 * Provides access to the operands and related data models
	 */
	public static final class SwingForeignKeyOperands implements Operands<Entity> {

		private final ConditionModel<Entity> condition;
		private final EntityComboBoxModel equalComboBoxModel;
		private final EntitySearchModel inSearchModel;

		private SwingForeignKeyOperands(ConditionModel<Entity> condition,
																		EntityComboBoxModel equalComboBoxModel,
																		EntitySearchModel inSearchModel) {
			this.condition = requireNonNull(condition);
			this.equalComboBoxModel = equalComboBoxModel;
			this.inSearchModel = inSearchModel;
		}

		/**
		 * @return an {@link EntityComboBoxModel} to use for the EQUAL operand
		 * @throws IllegalStateException in case no such model is available
		 */
		public EntityComboBoxModel equalComboBoxModel() {
			if (equalComboBoxModel == null) {
				throw new IllegalStateException("No EntityComboBoxModel is available for the EQUAL operand");
			}

			return equalComboBoxModel;
		}

		/**
		 * @return a {@link EntitySearchModel} to use for the IN operand
		 * @throws IllegalStateException in case no such model is available
		 */
		public EntitySearchModel inSearchModel() {
			if (inSearchModel == null) {
				throw new IllegalStateException("No EntitySearchModel available for the IN operand");
			}

			return inSearchModel;
		}

		@Override
		public Value<Entity> equal() {
			return condition.operands().equal();
		}

		@Override
		public ValueSet<Entity> in() {
			return condition.operands().in();
		}

		@Override
		public Value<Entity> upper() {
			return condition.operands().upper();
		}

		@Override
		public Value<Entity> lower() {
			return condition.operands().lower();
		}
	}

	private static final class DefaultBuilder implements Builder {

		private EntityComboBoxModel equalComboBoxModel;
		private EntitySearchModel inSearchModel;

		@Override
		public Builder equalComboBoxModel(EntityComboBoxModel equalComboBoxModel) {
			this.equalComboBoxModel = requireNonNull(equalComboBoxModel);
			return this;
		}

		@Override
		public Builder inSearchModel(EntitySearchModel inSearchModel) {
			this.inSearchModel = requireNonNull(inSearchModel);
			return this;
		}

		@Override
		public SwingForeignKeyConditionModel build() {
			return new SwingForeignKeyConditionModel(this);
		}

		private List<Operator> operators() {
			if (equalComboBoxModel == null && inSearchModel == null) {
				throw new IllegalStateException("Neither EQUAL nor IN operator specified");
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
