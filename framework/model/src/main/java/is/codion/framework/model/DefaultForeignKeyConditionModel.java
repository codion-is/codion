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
 * Copyright (c) 2023 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.framework.model;

import is.codion.common.model.condition.ConditionModel;
import is.codion.common.reactive.observer.Observer;
import is.codion.common.reactive.value.Value;
import is.codion.common.reactive.value.ValueSet;
import is.codion.common.utilities.Operator;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.attribute.ForeignKey;

import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import static is.codion.framework.model.PersistenceEvents.persistenceEvents;
import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toCollection;

final class DefaultForeignKeyConditionModel implements ForeignKeyConditionModel {

	private final ForeignKey foreignKey;
	private final ConditionModel<Entity> condition;
	private final @Nullable EntitySearchModel equalSearchModel;
	private final @Nullable EntityComboBoxModel equalComboBoxModel;
	private final @Nullable EntitySearchModel inSearchModel;
	// strong references, the persistence events hold their consumers weakly
	private final Consumer<Map<Entity, Entity>> updateListener = new UpdateListener();
	private final Consumer<Collection<Entity>> deleteListener = new DeleteListener();

	private DefaultForeignKeyConditionModel(DefaultBuilder builder) {
		foreignKey = builder.foreignKey;
		equalSearchModel = builder.equalSearchModel;
		equalComboBoxModel = builder.equalComboBoxModel;
		inSearchModel = builder.inSearchModel;
		List<Operator> operators = builder.operators();
		condition = ConditionModel.builder()
						.valueClass(Entity.class)
						// the operator before the operators, which must contain it, the default EQUAL being absent without an EQUAL operand
						.operator(builder.operator == null ? builder.defaultOperator(operators) : builder.operator)
						.operators(operators)
						.caption(builder.caption)
						.build();
		PersistenceEvents persistenceEvents = persistenceEvents(foreignKey.referencedType());
		persistenceEvents.updated().addWeakConsumer(updateListener);
		persistenceEvents.deleted().addWeakConsumer(deleteListener);
	}

	@Override
	public ForeignKey attribute() {
		return foreignKey;
	}

	@Override
	public ConditionModel<Entity> condition() {
		return condition;
	}

	@Override
	public Observer<?> changed() {
		return condition.changed();
	}

	@Override
	public Optional<String> caption() {
		return condition.caption();
	}

	@Override
	public Optional<EntitySearchModel> equalSearchModel() {
		return Optional.ofNullable(equalSearchModel);
	}

	@Override
	public Optional<EntityComboBoxModel> equalComboBoxModel() {
		return Optional.ofNullable(equalComboBoxModel);
	}

	@Override
	public Optional<EntitySearchModel> inSearchModel() {
		return Optional.ofNullable(inSearchModel);
	}

	static final class DefaultBuilder implements Builder {

		private final ForeignKey foreignKey;

		private @Nullable EntitySearchModel equalSearchModel;
		private @Nullable EntityComboBoxModel equalComboBoxModel;
		private @Nullable EntitySearchModel inSearchModel;
		private @Nullable Operator operator;
		private @Nullable String caption;

		DefaultBuilder(ForeignKey foreignKey) {
			this.foreignKey = foreignKey;
		}

		@Override
		public Builder equalSearchModel(EntitySearchModel equalSearchModel) {
			this.equalSearchModel = requireNonNull(equalSearchModel);
			return this;
		}

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
		public Builder operator(Operator operator) {
			this.operator = requireNonNull(operator);
			return this;
		}

		@Override
		public Builder caption(@Nullable String caption) {
			this.caption = caption;
			return this;
		}

		@Override
		public ForeignKeyConditionModel build() {
			return new DefaultForeignKeyConditionModel(this);
		}

		/**
		 * The default operator hinges on how the EQUAL operand is selected: a combo box is the most intuitive way to pick a
		 * single item, so it defaults to {@link Operator#EQUAL}. Without a combo box, selection happens via a search field,
		 * which is no simpler single- than multi-select, so the more powerful {@link Operator#IN} is preferred when available.
		 */
		private Operator defaultOperator(List<Operator> operators) {
			if (equalComboBoxModel != null) {
				return Operator.EQUAL;
			}
			if (inSearchModel != null) {
				return Operator.IN;
			}

			return operators.get(0);
		}

		private List<Operator> operators() {
			if (equalSearchModel != null && equalComboBoxModel != null) {
				throw new IllegalStateException("The EQUAL operand can not be based on both a search model and a combo box model");
			}
			boolean equal = equalSearchModel != null || equalComboBoxModel != null;
			if (!equal && inSearchModel == null) {
				throw new IllegalStateException("Neither EQUAL nor IN operator specified");
			}
			if (equal && inSearchModel != null) {
				return asList(Operator.EQUAL, Operator.NOT_EQUAL, Operator.IN, Operator.NOT_IN);
			}
			if (equal) {
				return asList(Operator.EQUAL, Operator.NOT_EQUAL);
			}

			return asList(Operator.IN, Operator.NOT_IN);
		}
	}

	/**
	 * Replaces updated entities in the operands with their updated state, the primary key matched on
	 * its original value, in case the update modified it.
	 */
	private final class UpdateListener implements Consumer<Map<Entity, Entity>> {

		@Override
		public void accept(Map<Entity, Entity> updated) {
			Map<Entity.Key, Entity> updatedByKey = new HashMap<>(updated.size());
			updated.forEach((beforeUpdate, afterUpdate) -> updatedByKey.put(beforeUpdate.originalPrimaryKey(), afterUpdate));
			Value<Entity> equal = condition.operands().equal();
			Entity equalOperand = equal.get();
			if (equalOperand != null && updatedByKey.containsKey(equalOperand.primaryKey())) {
				equal.set(updatedByKey.get(equalOperand.primaryKey()));
			}
			ValueSet<Entity> in = condition.operands().in();
			Set<Entity> inOperands = in.get();
			if (inOperands.stream().anyMatch(entity -> updatedByKey.containsKey(entity.primaryKey()))) {
				// a single set(), a remove followed by an add would leave the operand transiently without the updated entities
				in.set(inOperands.stream()
								.map(entity -> updatedByKey.getOrDefault(entity.primaryKey(), entity))
								.collect(toCollection(LinkedHashSet::new)));
			}
		}
	}

	/**
	 * Removes deleted entities from the operands, only touching an operand containing one,
	 * since setting an operand notifies its listeners regardless of whether it changed.
	 */
	private final class DeleteListener implements Consumer<Collection<Entity>> {

		@Override
		public void accept(Collection<Entity> deleted) {
			Value<Entity> equal = condition.operands().equal();
			Entity equalOperand = equal.get();
			if (equalOperand != null && deleted.contains(equalOperand)) {
				equal.clear();
			}
			ValueSet<Entity> in = condition.operands().in();
			Set<Entity> inOperands = in.get();
			if (deleted.stream().anyMatch(inOperands::contains)) {
				in.removeAll(deleted);
			}
		}
	}
}
