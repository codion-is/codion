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
 * Copyright (c) 2016 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.swing.framework.model;

import is.codion.common.version.Version;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.model.DefaultEntityApplicationModel;

/**
 * A Swing implementation of {@link is.codion.framework.model.EntityApplicationModel}
 */
public class SwingEntityApplicationModel
				extends DefaultEntityApplicationModel<SwingEntityModel, SwingEntityEditModel, SwingEntityTableModel> {

	/**
	 * Instantiates a new {@link SwingEntityApplicationModel}
	 * @param connectionProvider the connection provider
	 */
	public SwingEntityApplicationModel(EntityConnectionProvider connectionProvider) {
		this(connectionProvider, null);
	}

	/**
	 * Instantiates a new {@link SwingEntityApplicationModel}
	 * @param connectionProvider the connection provider
	 * @param version the application version
	 */
	public SwingEntityApplicationModel(EntityConnectionProvider connectionProvider, Version version) {
		super(connectionProvider, version);
	}
}
