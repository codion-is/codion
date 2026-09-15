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
import is.codion.framework.db.EntityConnection;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.attribute.ForeignKey;
import is.codion.framework.domain.entity.condition.Condition;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static is.codion.common.utilities.Operator.*;
import static is.codion.framework.model.PersistenceEvents.persistenceEvents;
import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toCollection;

final class DefaultForeignKeyConditionModel implements ForeignKeyConditionModel {

	private static final List<Operator> OPERATORS = unmodifiableList(asList(EQUAL, NOT_EQUAL, IN, NOT_IN));

	private final ForeignKey foreignKey;
	private final ConditionModel<Entity> condition;
	private final DefaultModels models;
	// strong references, the persistence events hold their consumers weakly
	private final Consumer<Map<Entity, Entity>> updateListener = new UpdateListener();
	private final Consumer<Collection<Entity>> deleteListener = new DeleteListener();

	private DefaultForeignKeyConditionModel(DefaultBuilder builder) {
		foreignKey = builder.foreignKey;
		models = new DefaultModels(builder.comboBoxModel, builder.searchModel);
		condition = ConditionModel.builder()
						.valueClass(Entity.class)
						// the operator before the operators, which must contain it
						.operator(builder.operator == null ? builder.operators.get(0) : builder.operator)
						.operators(builder.operators)
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
	public Models models() {
		return models;
	}

	/**
	 * @param foreignKey the foreign key
	 * @param connection the connection
	 * @return the default combo box model for the given foreign key, of the referenced entities, including null
	 */
	static EntityComboBoxModel comboBoxModel(ForeignKey foreignKey, EntityConnection connection) {
		return EntityComboBoxModel.builder()
						.entityType(requireNonNull(foreignKey).referencedType())
						.connection(requireNonNull(connection))
						.includeNull(true)
						.build();
	}

	/**
	 * @param foreignKey the foreign key
	 * @param connection the connection
	 * @return the default search model for the given foreign key, of the referenced entities
	 */
	static EntitySearchModel searchModel(ForeignKey foreignKey, EntityConnection connection) {
		return EntitySearchModel.builder()
						.entityType(requireNonNull(foreignKey).referencedType())
						.connection(requireNonNull(connection))
						.build();
	}

	static final class DefaultBuilder implements Builder {

		static final ForeignKeyStep FOREIGN_KEY_STEP = new DefaultForeignKeyStep();

		private final ForeignKey foreignKey;

		private Supplier<EntityComboBoxModel> comboBoxModel;
		private Supplier<EntitySearchModel> searchModel;
		private List<Operator> operators = OPERATORS;
		private @Nullable Operator operator;
		private @Nullable String caption;

		private DefaultBuilder(ForeignKey foreignKey, EntityConnection connection) {
			this.foreignKey = foreignKey;
			this.comboBoxModel = () -> DefaultForeignKeyConditionModel.comboBoxModel(foreignKey, connection);
			this.searchModel = () -> DefaultForeignKeyConditionModel.searchModel(foreignKey, connection);
		}

		@Override
		public Builder comboBoxModel(Supplier<EntityComboBoxModel> comboBoxModel) {
			this.comboBoxModel = requireNonNull(comboBoxModel);
			return this;
		}

		@Override
		public Builder searchModel(Supplier<EntitySearchModel> searchModel) {
			this.searchModel = requireNonNull(searchModel);
			return this;
		}

		@Override
		public Builder operators(List<Operator> operators) {
			if (requireNonNull(operators).isEmpty()) {
				throw new IllegalArgumentException("No operators specified");
			}
			if (!OPERATORS.containsAll(operators)) {
				throw new IllegalArgumentException("Operators not supported by a foreign key condition: " + operators);
			}
			this.operators = unmodifiableList(new ArrayList<>(operators));
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
			if (operator != null && !operators.contains(operator)) {
				throw new IllegalArgumentException("Operator " + operator + " is not one of the available operators: " + operators);
			}

			return new DefaultForeignKeyConditionModel(this);
		}

		private static final class DefaultForeignKeyStep implements ForeignKeyStep {

			@Override
			public ConnectionStep foreignKey(ForeignKey foreignKey) {
				return new DefaultConnectionStep(requireNonNull(foreignKey));
			}
		}

		private static final class DefaultConnectionStep implements ConnectionStep {

			private final ForeignKey foreignKey;

			private DefaultConnectionStep(ForeignKey foreignKey) {
				this.foreignKey = foreignKey;
			}

			@Override
			public Builder connection(EntityConnection connection) {
				return new DefaultBuilder(foreignKey, requireNonNull(connection));
			}
		}
	}

	private static final class DefaultModels implements Models {

		private final Supplier<EntityComboBoxModel> comboBoxModel;
		private final Supplier<EntitySearchModel> searchModel;
		private final Value<Supplier<Condition>> condition = Value.nullable();
		private final DefaultOperand equal = new DefaultOperand();
		private final DefaultOperand in = new DefaultOperand();

		private DefaultModels(Supplier<EntityComboBoxModel> comboBoxModel, Supplier<EntitySearchModel> searchModel) {
			this.comboBoxModel = comboBoxModel;
			this.searchModel = searchModel;
		}

		@Override
		public Operand equal() {
			return equal;
		}

		@Override
		public Operand in() {
			return in;
		}

		@Override
		public Value<Supplier<Condition>> condition() {
			return condition;
		}

		/**
		 * Applies the shared condition to the given model condition, now if set, and whenever set from now on
		 */
		private void restrict(Value<Supplier<Condition>> modelCondition) {
			condition.optional().ifPresent(modelCondition::set);
			condition.addConsumer(modelCondition::set);
		}

		private final class DefaultOperand implements Operand {

			private @Nullable EntityComboBoxModel comboBoxModel;
			private @Nullable EntitySearchModel searchModel;

			@Override
			public EntityComboBoxModel comboBoxModel() {
				synchronized (DefaultModels.this) {
					if (comboBoxModel == null) {
						EntityComboBoxModel created = requireNonNull(DefaultModels.this.comboBoxModel.get());
						if (created == other().comboBoxModel) {
							throw new IllegalStateException("The EQUAL and IN operands can not share a combo box model: " + created);
						}
						restrict(created.condition());
						comboBoxModel = created;
					}

					return comboBoxModel;
				}
			}

			@Override
			public EntitySearchModel searchModel() {
				synchronized (DefaultModels.this) {
					if (searchModel == null) {
						EntitySearchModel created = requireNonNull(DefaultModels.this.searchModel.get());
						if (created == other().searchModel) {
							throw new IllegalStateException("The EQUAL and IN operands can not share a search model: " + created);
						}
						restrict(created.condition());
						searchModel = created;
					}

					return searchModel;
				}
			}

			private DefaultOperand other() {
				return this == equal ? in : equal;
			}
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
