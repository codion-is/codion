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
 * Copyright (c) 2023 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.framework.demos.world.model;

import is.codion.common.version.Version;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.swing.framework.model.SwingEntityApplicationModel;
import is.codion.swing.framework.model.SwingEntityModel;

public final class WorldAppModel extends SwingEntityApplicationModel {

	public static final Version VERSION = Version.parsePropertiesFile(WorldAppModel.class, "/version.properties");

	public WorldAppModel(EntityConnectionProvider connectionProvider) {
		super(connectionProvider, VERSION);
		CountryModel countryModel = new CountryModel(connectionProvider);
		SwingEntityModel lookupModel = new SwingEntityModel(new LookupTableModel(connectionProvider));
		ContinentModel continentModel = new ContinentModel(connectionProvider);

		countryModel.tableModel().refresh();
		continentModel.tableModel().refresh();

		addEntityModels(countryModel, lookupModel, continentModel);
	}
}
