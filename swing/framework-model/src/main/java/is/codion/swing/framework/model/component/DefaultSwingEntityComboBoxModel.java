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
import is.codion.framework.db.EntityConnection;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.ForeignKey;
import is.codion.framework.domain.entity.condition.Condition;
import is.codion.framework.model.AbstractEntityComboBoxModelBuilder;
import is.codion.framework.model.EntityComboBoxModel;
import is.codion.swing.common.model.component.combobox.SwingFilterComboBoxModel;

import org.jspecify.annotations.Nullable;

import javax.swing.event.ListDataListener;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

/**
 * The Swing {@code ComboBoxModel} coat over a {@link EntityComboBoxModel}: the entity logic lives in the wrapped
 * model, the {@code ComboBoxModel}/{@code ListModel} surface comes from a {@link SwingFilterComboBoxModel} wrapping the
 * same model ({@link SwingFilterComboBoxModel#model(is.codion.common.model.component.combobox.FilterComboBoxModel)}).
 */
final class DefaultSwingEntityComboBoxModel implements SwingEntityComboBoxModel {

	private final EntityComboBoxModel model;
	private final SwingFilterComboBoxModel<Entity> coat;

	private DefaultSwingEntityComboBoxModel(EntityComboBoxModel model) {
		this.model = model;
		this.coat = SwingFilterComboBoxModel.model(model);
	}

	@Override
	public String toString() {
		return model.toString();
	}

	// Entity-specific surface -> the wrapped entity model

	@Override
	public EntityConnection connection() {
		return model.connection();
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
	public @Nullable Entity selectedItem() {
		return coat.selectedItem();
	}

	@Override
	public <V> Value<V> selector(ItemFinder<Entity, V> itemFinder) {
		return coat.selector(itemFinder);
	}

	@Override
	public @Nullable Object getSelectedItem() {
		return selectedItem();
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

	static final class DefaultBuilder extends AbstractEntityComboBoxModelBuilder<Builder> implements Builder {

		static final Builder.EntityTypeStep ENTITY_TYPE = new DefaultEntityTypeStep();

		private DefaultBuilder(EntityType entityType, EntityConnection connection) {
			super(entityType, connection);
		}

		private DefaultBuilder(ForeignKey foreignKey, EntityConnection connection) {
			super(foreignKey, connection);
		}

		@Override
		public SwingEntityComboBoxModel build() {
			return new DefaultSwingEntityComboBoxModel(super.build());
		}
	}

	private static final class DefaultEntityTypeStep implements Builder.EntityTypeStep {

		@Override
		public Builder.ConnectionStep entityType(EntityType entityType) {
			return new DefaultConnectionStep(requireNonNull(entityType), null);
		}

		@Override
		public Builder.ConnectionStep foreignKey(ForeignKey foreignKey) {
			return new DefaultConnectionStep(requireNonNull(foreignKey).referencedType(), foreignKey);
		}
	}

	private static final class DefaultConnectionStep implements Builder.ConnectionStep {

		private final EntityType entityType;
		private final @Nullable ForeignKey foreignKey;

		private DefaultConnectionStep(EntityType entityType, @Nullable ForeignKey foreignKey) {
			this.entityType = entityType;
			this.foreignKey = foreignKey;
		}

		@Override
		public Builder connection(EntityConnection connection) {
			return foreignKey == null ? new DefaultBuilder(entityType, connection) : new DefaultBuilder(foreignKey, connection);
		}
	}
}
