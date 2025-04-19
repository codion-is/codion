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
import is.codion.demos.world.domain.api.World.Country;
import is.codion.demos.world.model.CityTableModel;
import is.codion.demos.world.model.CityTableModel.PopulateLocationTask;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.dialog.Dialogs;
import is.codion.swing.framework.ui.EntityTableCellRenderer;
import is.codion.swing.framework.ui.icon.FrameworkIcons;

import org.kordamp.ikonli.foundation.Foundation;

import java.awt.Color;
import java.util.Objects;

final class CityTablePanel extends ChartTablePanel {

	CityTablePanel(CityTableModel tableModel) {
		super(tableModel, tableModel.chartDataset(), "Cities", config -> config
						.cellRenderer(City.POPULATION, EntityTableCellRenderer.builder(City.POPULATION, tableModel)
										.foreground((table, city, attribute, population) ->
														population > 1_000_000 ? Color.YELLOW : null)
										.build())
						.cellRenderer(City.NAME, EntityTableCellRenderer.builder(City.NAME, tableModel)
										.foreground((table, city, attribute, name) ->
														Objects.equals(city.get(City.ID), city.get(City.COUNTRY_FK).get(Country.CAPITAL)) ? Color.GREEN : null)
										.build())
						.editable(attributes -> attributes.remove(City.LOCATION)));
		configurePopupMenu(layout -> layout.clear()
						.control(createPopulateLocationControl())
						.separator()
						.defaults());
	}

	private Control createPopulateLocationControl() {
		CityTableModel cityTableModel = (CityTableModel) tableModel();

		return Control.builder()
						.command(this::populateLocation)
						.caption("Populate location")
						.enabled(cityTableModel.cityWithoutLocationSelected())
						.smallIcon(FrameworkIcons.instance().get(Foundation.MAP))
						.build();
	}

	private void populateLocation() {
		CityTableModel tableModel = (CityTableModel) tableModel();
		PopulateLocationTask task = tableModel.populateLocationTask();

		Dialogs.progressWorkerDialog(task)
						.owner(this)
						.title("Populating locations")
						.maximumProgress(task.maximumProgress())
						.stringPainted(true)
						.control(Control.builder()
										.command(task::cancel)
										.caption("Cancel")
										.enabled(task.cancelled().not()))
						.onException(this::displayPopulateException)
						.execute();
	}

	private void displayPopulateException(Exception exception) {
		Dialogs.exceptionDialog()
						.owner(this)
						.title("Unable to populate location")
						.show(exception);
	}
}
