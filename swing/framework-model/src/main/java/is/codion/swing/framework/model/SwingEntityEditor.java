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
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.attribute.ForeignKey;
import is.codion.framework.domain.entity.attribute.ForeignKeyDefinition;
import is.codion.framework.domain.entity.attribute.ValueAttributeDefinition;
import is.codion.framework.model.DefaultEntityEditor;
import is.codion.framework.model.EntityEditModel.EntityEditor;
import is.codion.swing.common.model.component.combobox.FilterComboBoxModel;
import is.codion.swing.framework.model.component.EntityComboBoxModel;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;

/**
 * A Swing implementation of {@link EntityEditor}
 */
public final class SwingEntityEditor extends DefaultEntityEditor {

	private static final NullItemCaption NULL_ITEM_CAPTION = new NullItemCaption();

	private final Map<Attribute<?>, EntityComboBoxModel> foreignKeyComboBoxModels = new HashMap<>();
	private final Map<Attribute<?>, FilterComboBoxModel<?>> columnComboBoxModels = new HashMap<>();

	/**
	 * Instantiates a new {@link SwingEntityEditor}
	 * @param entityType the entity type
	 * @param connectionProvider the connection provider
	 */
	public SwingEntityEditor(EntityType entityType, EntityConnectionProvider connectionProvider) {
		this(entityType, connectionProvider, new DefaultSwingEditorModels());
	}

	/**
	 * Instantiates a new {@link SwingEntityEditor}
	 * @param entityType the entity type
	 * @param connectionProvider the connection provider
	 */
	public SwingEntityEditor(EntityType entityType, EntityConnectionProvider connectionProvider, SwingEditorModels editorModels) {
		super(entityType, connectionProvider, editorModels);
	}

	/**
	 * Creates and refreshes combo box models for the given attributes. Doing this in the model
	 * constructor avoids the models being refreshed when the combo boxes using them are initialized,
	 * which usually happens on the Event Dispatch Thread.
	 * @param attributes the attributes for which to initialize combo box models
	 * @see #createComboBoxModel(Column)
	 * @see #createComboBoxModel(ForeignKey)
	 */
	public void initializeComboBoxModels(Attribute<?>... attributes) {
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
	public void refreshForeignKeyComboBoxModels() {
		synchronized (foreignKeyComboBoxModels) {
			for (EntityComboBoxModel comboBoxModel : foreignKeyComboBoxModels.values()) {
				comboBoxModel.items().refresh();
			}
		}
	}

	/**
	 * Refreshes all column based combobox models
	 */
	public void refreshColumnComboBoxModels() {
		synchronized (columnComboBoxModels) {
			for (FilterComboBoxModel<?> comboBoxModel : columnComboBoxModels.values()) {
				comboBoxModel.items().refresh();
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
	 * @see SwingEditorModels#configure(ForeignKey, EntityComboBoxModel, SwingEntityEditor)
	 */
	public EntityComboBoxModel comboBoxModel(ForeignKey foreignKey) {
		entityDefinition().foreignKeys().definition(foreignKey);
		synchronized (foreignKeyComboBoxModels) {
			// can't use computeIfAbsent() here, since that prevents recursive initialization of interdepending combo
			// box models, createComboBoxModel() may for example call this function
			// see javadoc: must not attempt to update any other mappings of this map
			EntityComboBoxModel comboBoxModel = foreignKeyComboBoxModels.get(foreignKey);
			if (comboBoxModel == null) {
				comboBoxModel = createComboBoxModel(foreignKey);
				editorModels().configure(foreignKey, comboBoxModel, this);
				foreignKeyComboBoxModels.put(foreignKey, comboBoxModel);
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
	 * @see SwingEditorModels#configure(Column, FilterComboBoxModel, SwingEntityEditor)
	 */
	public <T> FilterComboBoxModel<T> comboBoxModel(Column<T> column) {
		entityDefinition().columns().definition(column);
		synchronized (columnComboBoxModels) {
			// can't use computeIfAbsent() here, since that prevents recursive initialization of interdepending combo
			// box models, createComboBoxModel() may for example call this function
			// see javadoc: must not attempt to update any other mappings of this map
			FilterComboBoxModel<T> comboBoxModel = (FilterComboBoxModel<T>) columnComboBoxModels.get(column);
			if (comboBoxModel == null) {
				comboBoxModel = createComboBoxModel(column);
				editorModels().configure(column, comboBoxModel, this);
				columnComboBoxModels.put(column, comboBoxModel);
			}

			return comboBoxModel;
		}
	}

	/**
	 * <p>Creates a new {@link EntityComboBoxModel} for the given foreign key.
	 * @param foreignKey the foreign key for which to create a {@link EntityComboBoxModel}
	 * @return a {@link EntityComboBoxModel} for the given foreign key
	 * @see SwingEditorModels#createComboBoxModel(ForeignKey, SwingEntityEditor)
	 * @see FilterComboBoxModel#NULL_CAPTION
	 * @see EntityComboBoxModel.Builder#nullCaption(String)
	 * @see EntityComboBoxModel.Builder#includeNull(boolean)
	 * @see ValueAttributeDefinition#nullable()
	 * @see EntityComboBoxModel.Builder#attributes(Collection)
	 * @see ForeignKeyDefinition#attributes()
	 */
	public EntityComboBoxModel createComboBoxModel(ForeignKey foreignKey) {
		return editorModels().createComboBoxModel(foreignKey, this);
	}

	/**
	 * Creates a combo box model containing the current values of the given column.
	 * @param column the column
	 * @param <T> the value type
	 * @return a combo box model based on the given column
	 * @see SwingEditorModels#createComboBoxModel(Column, SwingEntityEditor)
	 * @see FilterComboBoxModel#NULL_CAPTION
	 */
	public <T> FilterComboBoxModel<T> createComboBoxModel(Column<T> column) {
		return editorModels().createComboBoxModel(column, this);
	}

	@Override
	protected SwingEditorModels editorModels() {
		return (SwingEditorModels) super.editorModels();
	}

	/**
	 * Provides data models for swing based editor components
	 */
	public interface SwingEditorModels extends EditorModels {

		/**
		 * <p>Creates a new {@link EntityComboBoxModel} for the given foreign key, override to
		 * provide a custom {@link EntityComboBoxModel} implementation.
		 * <p>This default implementation returns a sorted {@link EntityComboBoxModel} using the default
		 * null item caption if the underlying attribute is nullable.
		 * <p>If the foreign key has select attributes defined, those are set in the combo box model.
		 * @param foreignKey the foreign key for which to create a {@link EntityComboBoxModel}
		 * @param editor the editor
		 * @return a {@link EntityComboBoxModel} for the given foreign key
		 * @see FilterComboBoxModel#NULL_CAPTION
		 * @see EntityComboBoxModel.Builder#nullCaption(String)
		 * @see EntityComboBoxModel.Builder#includeNull(boolean)
		 * @see ValueAttributeDefinition#nullable()
		 * @see EntityComboBoxModel.Builder#attributes(Collection)
		 * @see ForeignKeyDefinition#attributes()
		 */
		EntityComboBoxModel createComboBoxModel(ForeignKey foreignKey, SwingEntityEditor editor);

		/**
		 * Creates a combo box model containing the current values of the given column.
		 * This default implementation returns a sorted {@link FilterComboBoxModel} using the default
		 * null item caption if the underlying column is nullable
		 * @param column the column
		 * @param editor the editor
		 * @param <T> the value type
		 * @return a combo box model based on the given column
		 * @see FilterComboBoxModel#NULL_CAPTION
		 */
		<T> FilterComboBoxModel<T> createComboBoxModel(Column<T> column, SwingEntityEditor editor);

		/**
		 * <p>Called when a {@link EntityComboBoxModel} is created by {@link #comboBoxModel(ForeignKey)}.
		 * @param foreignKey the foreign key
		 * @param comboBoxModel the combo box model
		 * @param editor the editor
		 */
		default void configure(ForeignKey foreignKey, EntityComboBoxModel comboBoxModel, SwingEntityEditor editor) {}

		/**
		 * Called when a {@link FilterComboBoxModel} is created by {@link #comboBoxModel(Column)}
		 * @param column the column
		 * @param comboBoxModel the combo box model
		 * @param editor the editor
		 * @param <T> the column type
		 */
		default <T> void configure(Column<T> column, FilterComboBoxModel<T> comboBoxModel, SwingEntityEditor editor) {}
	}

	/**
	 * A default {@link SwingEditorModels} implementation.
	 */
	public static class DefaultSwingEditorModels extends DefaultEditorModels implements SwingEditorModels {

		@Override
		public EntityComboBoxModel createComboBoxModel(ForeignKey foreignKey, SwingEntityEditor editor) {
			ForeignKeyDefinition foreignKeyDefinition = editor.entityDefinition().foreignKeys().definition(foreignKey);

			return EntityComboBoxModel.builder()
							.entityType(foreignKey.referencedType())
							.connectionProvider(editor.connectionProvider())
							.attributes(foreignKeyDefinition.attributes())
							.includeNull(editor.nullable(foreignKey))
							.build();
		}

		@Override
		public <T> FilterComboBoxModel<T> createComboBoxModel(Column<T> column, SwingEntityEditor editor) {
			FilterComboBoxModel.Builder<T> builder = createColumnComboBoxModel(requireNonNull(column), requireNonNull(editor).connectionProvider());
			if (editor.nullable(column)) {
				builder.includeNull(true);
				if (column.type().valueClass().isInterface()) {
					builder.nullItem(ProxyBuilder.of(column.type().valueClass())
									.method("toString", (ProxyBuilder.ProxyMethod<T>) NULL_ITEM_CAPTION)
									.build());
				}
			}

			return builder.build();
		}

		private static <T> FilterComboBoxModel.Builder<T> createColumnComboBoxModel(Column<T> column, EntityConnectionProvider connectionProvider) {
			return column.type().isEnum() ?
							FilterComboBoxModel.builder()
											.items(asList(column.type().valueClass().getEnumConstants())) :
							FilterComboBoxModel.builder()
											.items(new ColumnItems<>(connectionProvider, column));
		}
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

	private static final class NullItemCaption implements ProxyBuilder.ProxyMethod<Object> {

		private final String caption = FilterComboBoxModel.NULL_CAPTION.getOrThrow();

		@Override
		public Object invoke(Parameters<Object> parameters) throws Throwable {
			return caption;
		}
	}
}
