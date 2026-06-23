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
 * Copyright (c) 2024 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.swing.framework.model.component;

import is.codion.common.model.selection.SingleSelection;
import is.codion.common.reactive.value.Value;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.OrderBy;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.ForeignKey;
import is.codion.framework.domain.entity.condition.Condition;
import is.codion.framework.model.EntityComboBoxModel;
import is.codion.swing.common.model.component.combobox.SwingComboBoxModel;

import org.jspecify.annotations.Nullable;

import javax.swing.event.ListDataListener;
import java.util.Collection;
import java.util.Comparator;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * The Swing {@code ComboBoxModel} coat over a {@link EntityComboBoxModel}: the entity logic lives in the wrapped
 * model, the {@code ComboBoxModel}/{@code ListModel} surface comes from a {@link SwingComboBoxModel} wrapping the
 * same model ({@link SwingComboBoxModel#model(is.codion.common.model.component.combobox.FilterComboBoxModel)}).
 */
final class DefaultSwingEntityComboBoxModel implements SwingEntityComboBoxModel {

	private final EntityComboBoxModel model;
	private final SwingComboBoxModel<Entity> coat;

	private DefaultSwingEntityComboBoxModel(EntityComboBoxModel model) {
		this.model = model;
		this.coat = SwingComboBoxModel.model(model);
	}

	@Override
	public String toString() {
		return model.toString();
	}

	// Entity-specific surface -> the wrapped entity model

	@Override
	public EntityConnectionProvider connectionProvider() {
		return model.connectionProvider();
	}

	@Override
	public EntityDefinition entityDefinition() {
		return model.entityDefinition();
	}

	@Override
	public void select(Entity.Key primaryKey) {
		model.select(primaryKey);
	}

	@Override
	public Value<Supplier<Condition>> condition() {
		return model.condition();
	}

	@Override
	public Filter filter() {
		return model.filter();
	}

	@Override
	public <T> Value<T> selector(Attribute<T> attribute) {
		return model.selector(attribute);
	}

	// Combo box model + Swing coat -> the coat (which delegates to the wrapped model)

	@Override
	public ComboBoxItems<Entity> items() {
		return coat.items();
	}

	@Override
	public SingleSelection<Entity> selection() {
		return coat.selection();
	}

	@Override
	public Sort<Entity> sort() {
		return coat.sort();
	}

	@Override
	public @Nullable Entity getSelectedItem() {
		return coat.getSelectedItem();
	}

	@Override
	public <V> Value<V> selector(ItemFinder<Entity, V> itemFinder) {
		return coat.selector(itemFinder);
	}

	@Override
	public void setSelectedItem(@Nullable Object item) {
		coat.setSelectedItem(item);
	}

	@Override
	public int getSize() {
		return coat.getSize();
	}

	@Override
	public @Nullable Entity getElementAt(int index) {
		return coat.getElementAt(index);
	}

	@Override
	public void addListDataListener(ListDataListener listener) {
		coat.addListDataListener(listener);
	}

	@Override
	public void removeListDataListener(ListDataListener listener) {
		coat.removeListDataListener(listener);
	}

	static final class DefaultBuilder implements Builder {

		static final Builder.EntityTypeStep ENTITY_TYPE = new DefaultEntityTypeStep();

		private final EntityComboBoxModel.Builder builder;

		private DefaultBuilder(EntityComboBoxModel.Builder builder) {
			this.builder = builder;
		}

		@Override
		public Builder orderBy(@Nullable OrderBy orderBy) {
			builder.orderBy(orderBy);
			return this;
		}

		@Override
		public Builder comparator(@Nullable Comparator<Entity> comparator) {
			builder.comparator(comparator);
			return this;
		}

		@Override
		public Builder condition(@Nullable Supplier<Condition> condition) {
			builder.condition(condition);
			return this;
		}

		@Override
		public Builder attributes(Collection<Attribute<?>> attributes) {
			builder.attributes(attributes);
			return this;
		}

		@Override
		public Builder includeNull(boolean includeNull) {
			builder.includeNull(includeNull);
			return this;
		}

		@Override
		public Builder nullCaption(@Nullable String nullCaption) {
			builder.nullCaption(nullCaption);
			return this;
		}

		@Override
		public Builder select(@Nullable Entity entity) {
			builder.select(entity);
			return this;
		}

		@Override
		public Builder persistenceAware(boolean persistenceAware) {
			builder.persistenceAware(persistenceAware);
			return this;
		}

		@Override
		public Builder filterSelected(boolean filterSelected) {
			builder.filterSelected(filterSelected);
			return this;
		}

		@Override
		public Builder filter(ForeignKey foreignKey, SwingEntityComboBoxModel filterModel) {
			builder.filter(foreignKey, filterModel);
			return this;
		}

		@Override
		public Builder onItemSelected(Consumer<@Nullable Entity> item) {
			builder.onItemSelected(item);
			return this;
		}

		@Override
		public Builder refresh(boolean refresh) {
			builder.refresh(refresh);
			return this;
		}

		@Override
		public SwingEntityComboBoxModel build() {
			return new DefaultSwingEntityComboBoxModel(builder.build());
		}
	}

	private static final class DefaultEntityTypeStep implements Builder.EntityTypeStep {

		@Override
		public Builder.ConnectionProviderStep entityType(EntityType entityType) {
			return new DefaultConnectionProviderStep(EntityComboBoxModel.builder().entityType(entityType));
		}

		@Override
		public Builder.ConnectionProviderStep foreignKey(ForeignKey foreignKey) {
			return new DefaultConnectionProviderStep(EntityComboBoxModel.builder().foreignKey(foreignKey));
		}
	}

	private static final class DefaultConnectionProviderStep implements Builder.ConnectionProviderStep {

		private final EntityComboBoxModel.Builder.ConnectionProviderStep step;

		private DefaultConnectionProviderStep(EntityComboBoxModel.Builder.ConnectionProviderStep step) {
			this.step = step;
		}

		@Override
		public Builder connectionProvider(EntityConnectionProvider connectionProvider) {
			return new DefaultBuilder(step.connectionProvider(connectionProvider));
		}
	}
}
