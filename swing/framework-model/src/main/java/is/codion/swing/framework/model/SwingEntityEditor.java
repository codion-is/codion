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

import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.attribute.ForeignKey;
import is.codion.framework.model.AbstractEntityEditor;
import is.codion.swing.common.model.component.combobox.SwingFilterComboBoxModel;
import is.codion.swing.framework.model.component.SwingEntityComboBoxModel;


import java.util.Map;

import static java.util.Objects.requireNonNull;

/**
 * A Swing {@link AbstractEntityEditor} implementation.
 */
public final class SwingEntityEditor extends AbstractEntityEditor<SwingEntityEditor> {

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
		super(entityType, connectionProvider, componentModels, DefaultSwingComboBoxModels::new);
	}

	@Override
	public SwingComboBoxModels comboBoxModels() {
		return (SwingComboBoxModels) super.comboBoxModels();
	}

	@Override
	public SwingEntityEditor create(EntityType entityType) {
		return create(entityType, new SwingComponentModels() {});
	}

	@Override
	public SwingEntityEditor create(EntityType entityType, ComponentModels componentModels) {
		// The cast is safe by construction: a SwingEntityEditor only ever receives a SwingComponentModels, supplied
		// by the application's own (single-platform) binding (e.g. via a shared edit config's componentModels() hook).
		return new SwingEntityEditor(entityType, connectionProvider(), (SwingComponentModels) componentModels);
	}

	@Override
	protected SwingComponentModels componentModels() {
		return (SwingComponentModels) super.componentModels();
	}

	/**
	 * Manages the combo box models used by a {@link SwingEntityEditor}.
	 */
	public interface SwingComboBoxModels extends ComboBoxModels {

		@Override
		Map<ForeignKey, SwingEntityComboBoxModel> foreignKey();

		@Override
		Map<Column<?>, SwingFilterComboBoxModel<?>> column();

		@Override
		SwingEntityComboBoxModel get(ForeignKey foreignKey);

		@Override
		<T> SwingFilterComboBoxModel<T> get(Column<T> column);

		@Override
		SwingEntityComboBoxModel create(ForeignKey foreignKey);

		@Override
		<T> SwingFilterComboBoxModel<T> create(Column<T> column);
	}

	private static final class DefaultSwingComboBoxModels
					extends DefaultComboBoxModels<SwingEntityComboBoxModel, SwingFilterComboBoxModel<?>> implements SwingComboBoxModels {

		private DefaultSwingComboBoxModels(AbstractEntityEditor<?> editor) {
			super(editor);
		}

		@Override
		public <T> SwingFilterComboBoxModel<T> get(Column<T> column) {
			return (SwingFilterComboBoxModel<T>) super.get(column);
		}

		@Override
		public <T> SwingFilterComboBoxModel<T> create(Column<T> column) {
			return (SwingFilterComboBoxModel<T>) super.create(column);
		}
	}

	/**
	 * <p>A {@link ComponentModels} extension providing foreign key based
	 * {@link SwingEntityComboBoxModel} and column based {@link SwingFilterComboBoxModel}.
	 * <p>Override to customize combo box model creation.
	 */
	public interface SwingComponentModels extends ComponentModels {

		@Override
		default SwingEntityComboBoxModel comboBoxModel(ForeignKey foreignKey, EntityConnectionProvider connectionProvider) {
			return SwingEntityComboBoxModel.builder()
							.foreignKey(foreignKey)
							.connectionProvider(requireNonNull(connectionProvider))
							.build();
		}

		@Override
		default <T> SwingFilterComboBoxModel<T> comboBoxModel(Column<T> column, EntityConnectionProvider connectionProvider) {
			EntityDefinition entityDefinition = requireNonNull(connectionProvider).entities()
							.definition(requireNonNull(column).entityType());
			boolean nullable = entityDefinition.columns().definition(column).nullable();

			return SwingFilterComboBoxModel.builder()
							.items(() -> connectionProvider.connection().select(column))
							.nullItem(nullable ? ComponentModels.createNullItem(column) : null)
							.includeNull(nullable)
							.build();
		}
	}
}
