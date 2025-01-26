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
 * Copyright (c) 2023 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.demos.world.ui;

import is.codion.demos.world.domain.api.World.City;
import is.codion.demos.world.domain.api.World.CountryLanguage;
import is.codion.demos.world.model.CountryModel;
import is.codion.swing.framework.model.SwingEntityModel;
import is.codion.swing.framework.ui.EntityPanel;
import is.codion.swing.framework.ui.TabbedDetailLayout;

final class CountryPanel extends EntityPanel {

	CountryPanel(CountryModel countryModel) {
		super(countryModel,
						new CountryEditPanel(countryModel.editModel(),
										countryModel.detailModels().get(City.TYPE).tableModel().items()),
						new CountryTablePanel(countryModel.tableModel()),
						config -> config.detailLayout(entityPanel -> TabbedDetailLayout.builder(entityPanel)
										.splitPaneResizeWeight(0.7)
										.build()));

		SwingEntityModel cityModel = countryModel.detailModels().get(City.TYPE);
		EntityPanel cityPanel = new EntityPanel(cityModel,
						new CityEditPanel(cityModel.tableModel()),
						new CityTablePanel(cityModel.tableModel()));

		SwingEntityModel countryLanguageModel = countryModel.detailModels().get(CountryLanguage.TYPE);
		EntityPanel countryLanguagePanel = new EntityPanel(countryLanguageModel,
						new CountryLanguageEditPanel(countryLanguageModel.editModel()),
						new CountryLanguageTablePanel(countryLanguageModel.tableModel()));

		detailPanels().add(cityPanel, countryLanguagePanel);
	}
}
