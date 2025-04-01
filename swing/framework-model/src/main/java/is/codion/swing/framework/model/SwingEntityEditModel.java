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
 * Copyright (c) 2016 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.swing.framework.model;

import is.codion.common.proxy.ProxyBuilder;
import is.codion.common.proxy.ProxyBuilder.ProxyMethod;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.AttributeDefinition;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.attribute.ForeignKey;
import is.codion.framework.domain.entity.attribute.ForeignKeyDefinition;
import is.codion.framework.model.AbstractEntityEditModel;
import is.codion.framework.model.EntityEditModel;
import is.codion.swing.common.model.component.combobox.FilterComboBoxModel;
import is.codion.swing.framework.model.component.EntityComboBoxModel;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;

/**
 * A Swing implementation of {@link EntityEditModel}.
 */
public class SwingEntityEditModel extends AbstractEntityEditModel {

	private static final NullItemCaption NULL_ITEM_CAPTION = new NullItemCaption();

	private final Map<Attribute<?>, FilterComboBoxModel<?>> comboBoxModels = new HashMap<>();

	/**
	 * Instantiates a new {@link SwingEntityEditModel} based on the given entity type.
	 * @param entityType the type of the entity to base this {@link SwingEntityEditModel} on
	 * @param connectionProvider the {@link EntityConnectionProvider} instance
	 */
	public SwingEntityEditModel(EntityType entityType, EntityConnectionProvider connectionProvider) {
		super(entityType, requireNonNull(connectionProvider));
	}

	/**
	 * Creates and refreshes combo box models for the given attributes. Doing this in the model
	 * constructor avoids the models being refreshed when the combo boxes using them are initialized.
	 * @param attributes the attributes for which to initialize combo box models
	 * @see #createComboBoxModel(Column)
	 * @see #createComboBoxModel(ForeignKey)
	 */
	public final void initializeComboBoxModels(Attribute<?>... attributes) {
		requireNonNull(attributes);
		for (Attribute<?> attribute : attributes) {
			if (attribute instanceof ForeignKey) {
				comboBoxModel((ForeignKey) attribute).items().refresh();
			}
			else if (attribute instanceof Column<?>) {
				comboBoxModel((Column<?>) attribute).items().refresh();
			}
		}
	}

	/**
	 * Refreshes all foreign key combobox models
	 */
	public final void refreshForeignKeyComboBoxModels() {
		synchronized (comboBoxModels) {
			for (FilterComboBoxModel<?> comboBoxModel : comboBoxModels.values()) {
				if (comboBoxModel instanceof EntityComboBoxModel) {
					comboBoxModel.items().refresh();
				}
			}
		}
	}

	/**
	 * Refreshes all combobox models
	 */
	public final void refreshComboBoxModels() {
		synchronized (comboBoxModels) {
			for (FilterComboBoxModel<?> comboBoxModel : comboBoxModels.values()) {
				comboBoxModel.items().refresh();
			}
		}
	}

	/**
	 * Clears all combobox models
	 */
	public final void clearComboBoxModels() {
		synchronized (comboBoxModels) {
			for (FilterComboBoxModel<?> comboBoxModel : comboBoxModels.values()) {
				comboBoxModel.items().clear();
			}
		}
	}

	/**
	 * <p>Returns the {@link EntityComboBoxModel} associated with the given foreign key.
	 * If no such combo box model exists, one is created by calling {@link #createComboBoxModel(ForeignKey)}.
	 * <p>This method always returns the same {@link EntityComboBoxModel} instance, once one has been created.
	 * @param foreignKey the foreign key
	 * @return the {@link EntityComboBoxModel} associated with the given foreign key
	 * @see #createComboBoxModel(ForeignKey)
	 * @see #configureComboBoxModel(ForeignKey, EntityComboBoxModel)
	 */
	public final EntityComboBoxModel comboBoxModel(ForeignKey foreignKey) {
		entityDefinition().foreignKeys().definition(foreignKey);
		synchronized (comboBoxModels) {
			// can't use computeIfAbsent() here, since that prevents recursive initialization of interdepending combo
			// box models, createComboBoxModel() may for example call this function
			// see javadoc: must not attempt to update any other mappings of this map
			EntityComboBoxModel comboBoxModel = (EntityComboBoxModel) comboBoxModels.get(foreignKey);
			if (comboBoxModel == null) {
				comboBoxModel = createComboBoxModel(foreignKey);
				configureComboBoxModel(foreignKey, comboBoxModel);
				comboBoxModels.put(foreignKey, comboBoxModel);
			}

			return comboBoxModel;
		}
	}

	/**
	 * <p>Returns the {@link FilterComboBoxModel} associated with the given column.
	 * If no such combo box model exists, one is created by calling {@link #createComboBoxModel(Column)}.
	 * <p>This method always returns the same {@link FilterComboBoxModel} instance, once one has been created.
	 * @param column the column
	 * @param <T> the value type
	 * @return the {@link FilterComboBoxModel} associated with the given column
	 * @see #createComboBoxModel(Column)
	 * @see #configureComboBoxModel(Column, FilterComboBoxModel)
	 */
	public final <T> FilterComboBoxModel<T> comboBoxModel(Column<T> column) {
		entityDefinition().columns().definition(column);
		synchronized (comboBoxModels) {
			// can't use computeIfAbsent() here, since that prevents recursive initialization of interdepending combo
			// box models, createComboBoxModel() may for example call this function
			// see javadoc: must not attempt to update any other mappings of this map
			FilterComboBoxModel<T> comboBoxModel = (FilterComboBoxModel<T>) comboBoxModels.get(column);
			if (comboBoxModel == null) {
				comboBoxModel = createComboBoxModel(column);
				configureComboBoxModel(column, comboBoxModel);
				comboBoxModels.put(column, comboBoxModel);
			}

			return comboBoxModel;
		}
	}

	/**
	 * <p>Creates a new {@link EntityComboBoxModel} for the given foreign key, override to
	 * provide a custom {@link EntityComboBoxModel} implementation.
	 * <p>This default implementation returns a sorted {@link EntityComboBoxModel} using the default
	 * null item caption if the underlying attribute is nullable.
	 * <p>If the foreign key has select attributes defined, those are set in the combo box model.
	 * @param foreignKey the foreign key for which to create a {@link EntityComboBoxModel}
	 * @return a {@link EntityComboBoxModel} for the given foreign key
	 * @see FilterComboBoxModel#NULL_CAPTION
	 * @see EntityComboBoxModel.Builder#nullCaption(String)
	 * @see EntityComboBoxModel.Builder#includeNull(boolean)
	 * @see AttributeDefinition#nullable()
	 * @see EntityComboBoxModel.Builder#attributes(Collection)
	 * @see ForeignKeyDefinition#attributes()
	 */
	public EntityComboBoxModel createComboBoxModel(ForeignKey foreignKey) {
		ForeignKeyDefinition foreignKeyDefinition = entityDefinition().foreignKeys().definition(foreignKey);

		return EntityComboBoxModel.builder(foreignKey.referencedType(), connectionProvider())
						.attributes(foreignKeyDefinition.attributes())
						.includeNull(editor().nullable(foreignKey))
						.build();
	}

	/**
	 * Creates a combo box model containing the current values of the given column.
	 * This default implementation returns a sorted {@link FilterComboBoxModel} using the default
	 * null item caption if the underlying column is nullable
	 * @param column the column
	 * @param <T> the value type
	 * @return a combo box model based on the given column
	 * @see FilterComboBoxModel#NULL_CAPTION
	 */
	public <T> FilterComboBoxModel<T> createComboBoxModel(Column<T> column) {
		FilterComboBoxModel.Builder<T> builder = createColumnComboBoxModel(requireNonNull(column));
		if (editor().nullable(column)) {
			builder.includeNull(true);
			if (column.type().valueClass().isInterface()) {
				builder.nullItem(ProxyBuilder.builder(column.type().valueClass())
								.method("toString", (ProxyMethod<T>) NULL_ITEM_CAPTION)
								.build());
			}
		}
		FilterComboBoxModel<T> comboBoxModel = builder.build();
		afterInsertUpdateOrDelete().addListener(comboBoxModel.items()::refresh);

		return comboBoxModel;
	}

	/**
	 * <p>Called when a {@link EntityComboBoxModel} is created in {@link #comboBoxModel(ForeignKey)}.
	 * @param foreignKey the foreign key
	 * @param comboBoxModel the combo box model
	 */
	protected void configureComboBoxModel(ForeignKey foreignKey, EntityComboBoxModel comboBoxModel) {}

	/**
	 * Called when a {@link FilterComboBoxModel} is created in {@link #comboBoxModel(Column)}
	 * @param column the column
	 * @param comboBoxModel the combo box model
	 * @param <T> the column type
	 */
	protected <T> void configureComboBoxModel(Column<T> column, FilterComboBoxModel<T> comboBoxModel) {}

	private <T> FilterComboBoxModel.Builder<T> createColumnComboBoxModel(Column<T> column) {
		return column.type().isEnum() ?
						FilterComboBoxModel.builder(asList(column.type().valueClass().getEnumConstants())) :
						FilterComboBoxModel.builder(new ColumnItems<>(connectionProvider(), column));
	}

	private static final class ColumnItems<T> implements Supplier<Collection<T>> {

		private final EntityConnectionProvider connectionProvider;
		private final Column<T> column;

		private ColumnItems(EntityConnectionProvider connectionProvider, Column<T> column) {
			this.connectionProvider = connectionProvider;
			this.column = column;
		}

		@Override
		public Collection<T> get() {
			return connectionProvider.connection().select(column);
		}
	}

	private static final class NullItemCaption implements ProxyMethod<Object> {

		private final String caption = FilterComboBoxModel.NULL_CAPTION.get();

		@Override
		public Object invoke(Parameters<Object> parameters) throws Throwable {
			return caption;
		}
	}
}
