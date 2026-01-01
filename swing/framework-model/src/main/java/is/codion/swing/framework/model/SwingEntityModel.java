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
import is.codion.framework.model.AbstractEntityModel;

import static java.util.Objects.requireNonNull;

/**
 * A Swing implementation of {@link is.codion.framework.model.EntityModel}
 */
public class SwingEntityModel extends AbstractEntityModel<SwingEntityModel, SwingEntityEditModel, SwingEntityTableModel> {

	/**
	 * Instantiates a new SwingEntityModel with default SwingEntityEditModel and SwingEntityTableModel implementations.
	 * @param entityType the type of the entity to base this SwingEntityModel on
	 * @param connectionProvider a EntityConnectionProvider
	 */
	public SwingEntityModel(EntityType entityType, EntityConnectionProvider connectionProvider) {
		this(new SwingEntityEditModel(requireNonNull(entityType), requireNonNull(connectionProvider)));
	}

	/**
	 * Instantiates a new SwingEntityModel, with a default {@link SwingEntityTableModel}
	 * @param editModel the edit model
	 */
	public SwingEntityModel(SwingEntityEditModel editModel) {
		super(new SwingEntityTableModel(editModel));
	}

	/**
	 * Instantiates a new SwingEntityModel
	 * @param tableModel the table model
	 */
	public SwingEntityModel(SwingEntityTableModel tableModel) {
		super(tableModel);
	}
}
