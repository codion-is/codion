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
 * Copyright (c) 2016 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.swing.framework.model;

import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.model.DefaultEntityEditModel;
import is.codion.framework.model.EntityEditModel;
import is.codion.swing.framework.model.SwingEntityEditor.DefaultSwingEditorModels;
import is.codion.swing.framework.model.SwingEntityEditor.SwingEditorModels;

/**
 * A Swing implementation of {@link EntityEditModel}.
 */
public class SwingEntityEditModel extends DefaultEntityEditModel {

	/**
	 * Instantiates a new {@link SwingEntityEditModel} based on the given entity type.
	 * @param entityType the type of the entity to base this {@link SwingEntityEditModel} on
	 * @param connectionProvider the {@link EntityConnectionProvider} instance
	 */
	public SwingEntityEditModel(EntityType entityType, EntityConnectionProvider connectionProvider) {
		this(entityType, connectionProvider, new DefaultSwingEditorModels());
	}

	/**
	 * Instantiates a new {@link SwingEntityEditModel} based on the given entity type.
	 * @param entityType the type of the entity to base this {@link SwingEntityEditModel} on
	 * @param connectionProvider the {@link EntityConnectionProvider} instance
	 * @param editorModels the editor models
	 */
	public SwingEntityEditModel(EntityType entityType, EntityConnectionProvider connectionProvider, SwingEditorModels editorModels) {
		super(new SwingEntityEditor(entityType, connectionProvider, editorModels));
		afterInsertUpdateOrDelete().addListener(editor().comboBoxModels()::refreshColumnComboBoxModels);
	}

	@Override
	public final SwingEntityEditor editor() {
		return (SwingEntityEditor) super.editor();
	}
}
