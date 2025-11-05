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

import is.codion.common.model.condition.ConditionModel;
import is.codion.common.observer.Observer;
import is.codion.common.state.State;
import is.codion.common.utilities.Operator;
import is.codion.common.value.Value;
import is.codion.common.value.ValueSet;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.model.EntitySearchModel;
import is.codion.swing.framework.model.component.EntityComboBoxModel;

import org.jspecify.annotations.Nullable;

import java.text.Format;
import java.util.List;
import java.util.Optional;

import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;

final class DefaultSwingForeignKeyConditionModel implements SwingForeignKeyConditionModel {

	private static final String EQUAL_SEARCH_MODEL_MESSAGE = "SwingForeignKeyCondition uses a EntityComboBoxModel for the EQUAL operand";

	private final ConditionModel<Entity> condition;
	private final @Nullable EntityComboBoxModel equalComboBoxModel;
	private final @Nullable EntitySearchModel inSearchModel;

	private DefaultSwingForeignKeyConditionModel(DefaultBuilder builder) {
		equalComboBoxModel = builder.equalComboBoxModel;
		inSearchModel = builder.inSearchModel;
		condition = ConditionModel.builder()
						.valueClass(Entity.class)
						.operators(builder.operators())
						.operator(defaultOperator(builder))
						.operands(new ForeignKeyOperands())
						.build();
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
	public SetCondition<Entity> set() {
		return condition.set();
	}

	@Override
	public boolean accepts(Comparable<Entity> value) {
		return condition.accepts(value);
	}

	@Override
	public Observer<?> changed() {
		return condition.changed();
	}

	@Override
	public EntityComboBoxModel equalComboBoxModel() {
		if (equalComboBoxModel == null) {
			throw new IllegalStateException("No EntityComboBoxModel is available for the EQUAL operand");
		}

		return equalComboBoxModel;
	}

	@Override
	public EntitySearchModel equalSearchModel() {
		throw new UnsupportedOperationException(EQUAL_SEARCH_MODEL_MESSAGE);
	}

	@Override
	public EntitySearchModel inSearchModel() {
		if (inSearchModel == null) {
			throw new IllegalStateException("No EntitySearchModel available for the IN operand");
		}

		return inSearchModel;
	}

	private static Operator defaultOperator(DefaultBuilder builder) {
		if (builder.inSearchModel == null) {
			return Operator.EQUAL;
		}

		boolean searchable = !builder.inSearchModel.entityDefinition().columns().searchable().isEmpty();

		return searchable ? Operator.IN : Operator.EQUAL;
	}

	static final class DefaultBuilder implements Builder {

		private @Nullable EntityComboBoxModel equalComboBoxModel;
		private @Nullable EntitySearchModel inSearchModel;

		@Override
		public Builder equalComboBoxModel(EntityComboBoxModel equalComboBoxModel) {
			this.equalComboBoxModel = requireNonNull(equalComboBoxModel);
			return this;
		}

		@Override
		public Builder equalSearchModel(EntitySearchModel equalSearchModel) {
			throw new UnsupportedOperationException(EQUAL_SEARCH_MODEL_MESSAGE);
		}

		@Override
		public Builder inSearchModel(EntitySearchModel inSearchModel) {
			this.inSearchModel = requireNonNull(inSearchModel);
			return this;
		}

		@Override
		public SwingForeignKeyConditionModel build() {
			return new DefaultSwingForeignKeyConditionModel(this);
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

	private final class ForeignKeyOperands implements Operands<Entity> {

		@Override
		public Value<Entity> equal() {
			return equalComboBoxModel == null ? Operands.super.equal() : equalComboBoxModel.selection().item();
		}

		@Override
		public ValueSet<Entity> in() {
			return inSearchModel == null ? Operands.super.in() : inSearchModel.selection().entities();
		}
	}
}
