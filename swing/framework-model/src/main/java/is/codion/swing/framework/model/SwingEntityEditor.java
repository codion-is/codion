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
 * Copyright (c) 2026, Björn Darri Sigurðsson.
 */
package is.codion.swing.framework.model;

import is.codion.common.utilities.proxy.ProxyBuilder;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.attribute.ForeignKey;
import is.codion.framework.domain.entity.attribute.ForeignKeyDefinition;
import is.codion.framework.domain.entity.attribute.ValueAttributeDefinition;
import is.codion.framework.model.DefaultEntityEditor;
import is.codion.swing.common.model.component.combobox.FilterComboBoxModel;
import is.codion.swing.framework.model.component.EntityComboBoxModel;

import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static java.util.Collections.unmodifiableMap;
import static java.util.Objects.requireNonNull;

/**
 * A Swing extension of {@link DefaultEntityEditor}
 */
public final class SwingEntityEditor extends DefaultEntityEditor<SwingEntityModel,
				SwingEntityEditModel, SwingEntityTableModel, SwingEntityEditor> {

	private static final String NULL_ITEM_CAPTION = FilterComboBoxModel.NULL_CAPTION.getOrThrow();
	private static final ProxyBuilder.ProxyMethod<Object> NULL_ITEM_TO_STRING = parameters -> NULL_ITEM_CAPTION;

	private final ComboBoxModels comboBoxModels = new ComboBoxModels();

	/**
	 * Instantiates a new {@link SwingEntityEditor}
	 * @param entityType the entity type
	 * @param connectionProvider the connection provider
	 */
	public SwingEntityEditor(EntityType entityType, EntityConnectionProvider connectionProvider) {
		this(entityType, connectionProvider, new SwingComponentModels() {});
	}

	/**
	 * Instantiates a new {@link SwingEntityEditor}
	 * @param entityType the entity type
	 * @param connectionProvider the connection provider
	 * @param componentModels the component models
	 */
	public SwingEntityEditor(EntityType entityType, EntityConnectionProvider connectionProvider,
													 SwingComponentModels componentModels) {
		super(entityType, connectionProvider, componentModels);
	}

	/**
	 * @return the {@link ComboBoxModels} instance
	 */
	public ComboBoxModels comboBoxModels() {
		return comboBoxModels;
	}

	@Override
	protected SwingComponentModels componentModels() {
		return (SwingComponentModels) super.componentModels();
	}

	/**
	 * Manages the combo box models used by a {@link SwingEntityEditor}.
	 */
	public final class ComboBoxModels {

		private final Map<ForeignKey, EntityComboBoxModel> foreignKeyComboBoxModels = new HashMap<>();
		private final Map<Column<?>, FilterComboBoxModel<?>> columnComboBoxModels = new HashMap<>();

		private ComboBoxModels() {}

		/**
		 * Returns the foreign key based combo box models initialized via {@link #get(ForeignKey)}.
		 * @return an unmodifiable view of the foreign key based combo box models
		 */
		public Map<ForeignKey, EntityComboBoxModel> foreignKey() {
			synchronized (foreignKeyComboBoxModels) {
				return unmodifiableMap(foreignKeyComboBoxModels);
			}
		}

		/**
		 * Returns the column based combo box models initialized via {@link #get(Column)}.
		 * @return an unmodifiable view of the column based combo box models
		 */
		public Map<Column<?>, FilterComboBoxModel<?>> column() {
			synchronized (columnComboBoxModels) {
				return unmodifiableMap(columnComboBoxModels);
			}
		}

		/**
		 * Creates and refreshes combo box models for the given attributes. Doing this in a edit model
		 * constructor avoids the models being refreshed when the combo boxes using them are initialized
		 * @param attributes the attributes for which to initialize combo box models
		 * @see #create(Column)
		 * @see #create(ForeignKey)
		 */
		public void initialize(Attribute<?>... attributes) {
			for (Attribute<?> attribute : requireNonNull(attributes)) {
				if (attribute instanceof ForeignKey) {
					get((ForeignKey) attribute).items().refresh();
				}
				else if (attribute instanceof Column<?>) {
					get((Column<?>) attribute).items().refresh();
				}
			}
		}

		/**
		 * <p>Returns the {@link EntityComboBoxModel} associated with the given foreign key.
		 * If no such combo box model exists, one is created by calling {@link #create(ForeignKey)}.
		 * <p>This method always returns the same {@link EntityComboBoxModel} instance, once one has been created.
		 * @param foreignKey the foreign key
		 * @return the {@link EntityComboBoxModel} associated with the given foreign key
		 * @see ComboBoxModels#create(ForeignKey)
		 */
		public EntityComboBoxModel get(ForeignKey foreignKey) {
			entityDefinition().foreignKeys().definition(foreignKey);
			synchronized (foreignKeyComboBoxModels) {
				// can't use computeIfAbsent() here, since that prevents recursive initialization of interdepending combo
				// box models, create() may for example call this function
				// see javadoc: must not attempt to update any other mappings of this map
				EntityComboBoxModel comboBoxModel = foreignKeyComboBoxModels.get(foreignKey);
				if (comboBoxModel == null) {
					comboBoxModel = create(requireNonNull(foreignKey));
					foreignKeyComboBoxModels.put(foreignKey, comboBoxModel);
				}

				return comboBoxModel;
			}
		}

		/**
		 * <p>Returns the {@link FilterComboBoxModel} associated with the given column.
		 * If no such combo box model exists, one is created by calling {@link #create(Column)}.
		 * <p>This method always returns the same {@link FilterComboBoxModel} instance, once one has been created.
		 * @param column the column
		 * @param <T> the value type
		 * @return the {@link FilterComboBoxModel} associated with the given column
		 * @see #create(Column)
		 */
		public <T> FilterComboBoxModel<T> get(Column<T> column) {
			entityDefinition().columns().definition(column);
			synchronized (columnComboBoxModels) {
				// can't use computeIfAbsent() here, since that prevents recursive initialization of interdepending combo
				// box models, create() may for example call this function
				// see javadoc: must not attempt to update any other mappings of this map
				FilterComboBoxModel<T> comboBoxModel = (FilterComboBoxModel<T>) columnComboBoxModels.get(column);
				if (comboBoxModel == null) {
					comboBoxModel = create(requireNonNull(column));
					columnComboBoxModels.put(column, comboBoxModel);
				}

				return comboBoxModel;
			}
		}

		/**
		 * <p>Creates a new {@link EntityComboBoxModel} for the given foreign key.
		 * @param foreignKey the foreign key for which to create a {@link EntityComboBoxModel}
		 * @return a {@link EntityComboBoxModel} for the given foreign key
		 * @see SwingComponentModels#comboBoxModel(ForeignKey, EntityConnectionProvider)
		 * @see FilterComboBoxModel#NULL_CAPTION
		 * @see EntityComboBoxModel.Builder#nullCaption(String)
		 * @see EntityComboBoxModel.Builder#includeNull(boolean)
		 * @see ValueAttributeDefinition#nullable()
		 * @see EntityComboBoxModel.Builder#attributes(Collection)
		 * @see ForeignKeyDefinition#attributes()
		 */
		public EntityComboBoxModel create(ForeignKey foreignKey) {
			return componentModels().comboBoxModel(requireNonNull(foreignKey), connectionProvider());
		}

		/**
		 * Creates a combo box model containing the current values of the given column.
		 * @param column the column
		 * @param <T> the value type
		 * @return a combo box model based on the given column
		 * @see FilterComboBoxModel#NULL_CAPTION
		 */
		public <T> FilterComboBoxModel<T> create(Column<T> column) {
			return componentModels().comboBoxModel(requireNonNull(column), connectionProvider());
		}
	}

	/**
	 * <p>A {@link SwingComponentModels} extension providing foreign key based
	 * {@link EntityComboBoxModel} and column based {@link FilterComboBoxModel}.
	 * <p>Override to customize combo box model creation.
	 */
	public interface SwingComponentModels extends ComponentModels<SwingEntityModel, SwingEntityEditModel, SwingEntityTableModel, SwingEntityEditor> {

		/**
		 * <p>Creates a new {@link EntityComboBoxModel} for the given foreign key, override to
		 * provide a custom {@link EntityComboBoxModel} implementation.
		 * <p>This default implementation returns a sorted {@link EntityComboBoxModel} using the default
		 * null item caption if the underlying attribute is nullable.
		 * <p>If the foreign key has select attributes defined, those are set in the combo box model.
		 * @param foreignKey the foreign key for which to create a {@link EntityComboBoxModel}
		 * @param connectionProvider the connection provider
		 * @return a {@link EntityComboBoxModel} for the given foreign key
		 * @see FilterComboBoxModel#NULL_CAPTION
		 * @see EntityComboBoxModel.Builder#nullCaption(String)
		 * @see EntityComboBoxModel.Builder#includeNull(boolean)
		 * @see ValueAttributeDefinition#nullable()
		 * @see EntityComboBoxModel.Builder#attributes(Collection)
		 * @see ForeignKeyDefinition#attributes()
		 */
		default EntityComboBoxModel comboBoxModel(ForeignKey foreignKey, EntityConnectionProvider connectionProvider) {
			return EntityComboBoxModel.builder()
							.foreignKey(foreignKey)
							.connectionProvider(requireNonNull(requireNonNull(connectionProvider)))
							.build();
		}

		/**
		 * Creates a combo box model containing the current values of the given column.
		 * This default implementation returns a sorted {@link FilterComboBoxModel} using the default
		 * null item caption if the underlying column is nullable
		 * @param column the column
		 * @param connectionProvider the connection provider
		 * @param <T> the value type
		 * @return a combo box model based on the given column
		 * @see FilterComboBoxModel#NULL_CAPTION
		 */
		default <T> FilterComboBoxModel<T> comboBoxModel(Column<T> column, EntityConnectionProvider connectionProvider) {
			EntityDefinition entityDefinition = requireNonNull(connectionProvider).entities()
							.definition(requireNonNull(column).entityType());
			boolean nullable = entityDefinition.columns().definition(column).nullable();

			return FilterComboBoxModel.builder()
							.items(() -> connectionProvider.connection().select(column))
							.nullItem(nullable ? createNullItem(column) : null)
							.includeNull(nullable)
							.build();
		}
	}

	static <T> @Nullable T createNullItem(Column<T> column) {
		return column.type().valueClass().isInterface() ? ProxyBuilder.of(column.type().valueClass())
						.method("toString", (ProxyBuilder.ProxyMethod<T>) NULL_ITEM_TO_STRING)
						.build() : null;
	}
}
