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
package is.codion.framework.demos.world.ui;

import is.codion.framework.demos.world.domain.api.World.City;
import is.codion.framework.demos.world.domain.api.World.CountryLanguage;
import is.codion.framework.demos.world.model.CountryModel;
import is.codion.swing.framework.model.SwingEntityModel;
import is.codion.swing.framework.ui.EntityPanel;

import static is.codion.swing.framework.ui.TabbedPanelLayout.splitPaneResizeWeight;

final class CountryPanel extends EntityPanel {

  CountryPanel(CountryModel countryModel) {
    super(countryModel,
            new CountryEditPanel(countryModel.editModel()),
            new CountryTablePanel(countryModel.tableModel()),
            splitPaneResizeWeight(0.7));

    SwingEntityModel cityModel = countryModel.detailModel(City.TYPE);
    EntityPanel cityPanel = new EntityPanel(cityModel,
            new CityEditPanel(cityModel.tableModel()),
            new CityTablePanel(cityModel.tableModel()));

    SwingEntityModel countryLanguageModel = countryModel.detailModel(CountryLanguage.TYPE);
    EntityPanel countryLanguagePanel = new EntityPanel(countryLanguageModel,
            new CountryLanguageEditPanel(countryLanguageModel.editModel()),
            new CountryLanguageTablePanel(countryLanguageModel.tableModel()));

    addDetailPanels(cityPanel, countryLanguagePanel);
  }
}
