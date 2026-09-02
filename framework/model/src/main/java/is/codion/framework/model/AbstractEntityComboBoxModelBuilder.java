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
 * Copyright (c) 2013 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.framework.model;

import is.codion.common.model.component.combobox.FilterComboBoxModel;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityDefinition.ForeignKeys;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.OrderBy;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.ForeignKey;
import is.codion.framework.domain.entity.condition.Condition;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static is.codion.framework.model.EntityComboBoxModel.PERSISTENCE_AWARE;
import static java.util.Objects.requireNonNull;

/**
 * A base class for {@link EntityComboBoxModel.Builder} implementations. A toolkit builder extends this class,
 * adding its own options and overriding {@link #build()} to wrap the {@link EntityComboBoxModel} in its own model.
 * @param <B> the builder type
 */
public abstract class AbstractEntityComboBoxModelBuilder<B extends EntityComboBoxModel.Builder<B>>
				implements EntityComboBoxModel.Builder<B> {

	final DefaultEntityComboBoxModel.EntityItems items;
	final EntityDefinition entityDefinition;
	final Map<ForeignKey, EntityComboBoxModel> filterLinks = new HashMap<>();

	@Nullable Comparator<Entity> comparator;
	boolean persistenceAware = PERSISTENCE_AWARE.getOrThrow();
	boolean filterSelected = false;
	@Nullable Entity selectEntity;
	@Nullable Entity nullItem;
	@Nullable Consumer<@Nullable Entity> onSelectedItem;
	boolean refresh = false;

	/**
	 * @param entityType the type of the entity the combo box model should represent
	 * @param connection the connection
	 */
	protected AbstractEntityComboBoxModelBuilder(EntityType entityType, EntityConnection connection) {
		this.entityDefinition = requireNonNull(connection).entities().definition(requireNonNull(entityType));
		this.items = new DefaultEntityComboBoxModel.EntityItems(entityDefinition, connection);
		this.comparator = entityDefinition.comparator();
	}

	/**
	 * Configures the builder according to the given foreign key, including null if it is nullable and
	 * specifying the attributes to include if defined.
	 * @param foreignKey the foreign key which referenced entity type the combo box model should represent
	 * @param connection the connection
	 * @see EntityComboBoxModel.Builder.EntityTypeStep#foreignKey(ForeignKey)
	 */
	protected AbstractEntityComboBoxModelBuilder(ForeignKey foreignKey, EntityConnection connection) {
		this(requireNonNull(foreignKey).referencedType(), connection);
		ForeignKeys foreignKeys = connection.entities().definition(foreignKey.entityType()).foreignKeys();
		includeNull(foreignKeys.nullable(foreignKey));
		attributes(foreignKeys.definition(foreignKey).attributes());
	}

	@Override
	public final B orderBy(@Nullable OrderBy orderBy) {
		items.orderBy = orderBy;
		return self();
	}

	@Override
	public final B comparator(@Nullable Comparator<Entity> comparator) {
		this.comparator = comparator;
		return self();
	}

	@Override
	public final B condition(@Nullable Supplier<Condition> condition) {
		items.condition.set(condition);
		return self();
	}

	@Override
	public final B attributes(Collection<Attribute<?>> attributes) {
		for (Attribute<?> attribute : requireNonNull(attributes)) {
			if (!attribute.entityType().equals(items.entityDefinition.type())) {
				throw new IllegalArgumentException("Attribute " + attribute + " is not part of entity: " + items.entityDefinition.type());
			}
		}
		items.attributes = new ArrayList<>(attributes);
		return self();
	}

	@Override
	public final B includeNull(boolean includeNull) {
		return nullCaption(includeNull ? FilterComboBoxModel.NULL_CAPTION.getOrThrow() : null);
	}

	@Override
	public final B nullCaption(@Nullable String nullCaption) {
		this.nullItem = nullCaption == null ? null : entityDefinition.entity(nullCaption);
		return self();
	}

	@Override
	public final B select(@Nullable Entity entity) {
		this.selectEntity = entity;
		return self();
	}

	@Override
	public final B persistenceAware(boolean persistenceAware) {
		this.persistenceAware = persistenceAware;
		return self();
	}

	@Override
	public final B filterSelected(boolean filterSelected) {
		this.filterSelected = filterSelected;
		return self();
	}

	@Override
	public final B filter(ForeignKey foreignKey, EntityComboBoxModel filterModel) {
		entityDefinition.foreignKeys().definition(foreignKey);
		DefaultEntityComboBoxModel.validateLink(foreignKey, filterModel);
		filterLinks.put(foreignKey, filterModel);
		return self();
	}

	@Override
	public final B onSelectedItem(Consumer<@Nullable Entity> item) {
		this.onSelectedItem = requireNonNull(item);
		return self();
	}

	@Override
	public final B refresh(boolean refresh) {
		this.refresh = refresh;
		return self();
	}

	@Override
	public EntityComboBoxModel build() {
		return new DefaultEntityComboBoxModel(this);
	}

	/**
	 * @return this builder instance
	 */
	protected final B self() {
		return (B) this;
	}
}
