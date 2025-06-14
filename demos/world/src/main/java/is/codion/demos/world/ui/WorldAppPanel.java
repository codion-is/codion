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

import is.codion.common.model.CancelException;
import is.codion.common.user.User;
import is.codion.demos.world.domain.api.World;
import is.codion.demos.world.domain.api.World.Continent;
import is.codion.demos.world.domain.api.World.Country;
import is.codion.demos.world.domain.api.World.Lookup;
import is.codion.demos.world.model.ContinentModel;
import is.codion.demos.world.model.CountryModel;
import is.codion.demos.world.model.WorldAppModel;
import is.codion.plugin.flatlaf.intellij.themes.monokaipro.MonokaiPro;
import is.codion.swing.common.ui.component.table.FilterTableCellRenderer;
import is.codion.swing.framework.model.SwingEntityModel;
import is.codion.swing.framework.ui.EntityApplicationPanel;
import is.codion.swing.framework.ui.EntityPanel;
import is.codion.swing.framework.ui.ReferentialIntegrityErrorHandling;
import is.codion.swing.framework.ui.icon.FrameworkIcons;

import org.kordamp.ikonli.foundation.Foundation;

import javax.swing.SwingConstants;
import java.util.List;
import java.util.Locale;

import static java.util.Collections.emptyList;

public final class WorldAppPanel extends EntityApplicationPanel<WorldAppModel> {

	static {
		FrameworkIcons.instance().add(Foundation.MAP, Foundation.PAGE_EXPORT, Foundation.PAGE_ADD, Foundation.CHECK);
	}

	public WorldAppPanel(WorldAppModel applicationModel) {
		super(applicationModel, createPanels(applicationModel), emptyList());
	}

	// tag::initializeEntityPanels[]
	private static List<EntityPanel> createPanels(WorldAppModel applicationModel) {
		CountryModel countryModel = (CountryModel)
						applicationModel.entityModels().get(Country.TYPE);
		CountryPanel countryPanel = new CountryPanel(countryModel);

		ContinentModel continentModel = (ContinentModel)
						applicationModel.entityModels().get(Continent.TYPE);
		ContinentPanel continentPanel = new ContinentPanel(continentModel);

		SwingEntityModel lookupModel = applicationModel.entityModels().get(Lookup.TYPE);
		EntityPanel lookupPanel = new EntityPanel(lookupModel,
						new LookupTablePanel(lookupModel.tableModel()));

		return List.of(countryPanel, continentPanel, lookupPanel);
	}
	// end::initializeEntityPanels[]

	public static void main(String[] args) throws CancelException {
		Locale.setDefault(new Locale("en", "EN"));
		EntityPanel.Config.TOOLBAR_CONTROLS.set(true);
		FilterTableCellRenderer.NUMERICAL_HORIZONTAL_ALIGNMENT.set(SwingConstants.CENTER);
		ReferentialIntegrityErrorHandling.REFERENTIAL_INTEGRITY_ERROR_HANDLING
						.set(ReferentialIntegrityErrorHandling.DISPLAY_DEPENDENCIES);
		EntityApplicationPanel.builder(WorldAppModel.class, WorldAppPanel.class)
						.domain(World.DOMAIN)
						.applicationName("World")
						.applicationVersion(WorldAppModel.VERSION)
						.defaultLookAndFeel(MonokaiPro.class)
						.defaultUser(User.parse("scott:tiger"))
						.start();
	}
}
