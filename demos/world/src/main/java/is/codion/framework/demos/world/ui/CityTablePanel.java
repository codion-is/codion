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
import is.codion.framework.demos.world.domain.api.World.Country;
import is.codion.framework.demos.world.model.CityTableModel;
import is.codion.framework.demos.world.model.CityTableModel.PopulateLocationTask;
import is.codion.framework.domain.entity.Entity;
import is.codion.swing.common.ui.component.table.FilterTableCellRenderer;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.dialog.Dialogs;
import is.codion.swing.framework.model.SwingEntityTableModel;
import is.codion.swing.framework.ui.EntityTableCellRenderer;
import is.codion.swing.framework.ui.icon.FrameworkIcons;

import org.kordamp.ikonli.foundation.Foundation;

import java.awt.Color;
import java.util.Objects;

final class CityTablePanel extends ChartTablePanel {

	CityTablePanel(CityTableModel tableModel) {
		super(tableModel, tableModel.chartDataset(), "Cities", config -> config
						.configureTable(builder -> builder
										.cellRenderer(City.POPULATION, () -> populationRenderer(tableModel))
										.cellRenderer(City.NAME, () -> nameRenderer(tableModel)))
						.editable(attributes -> attributes.remove(City.LOCATION)));
		configurePopupMenu(config -> config.clear()
						.control(createPopulateLocationControl())
						.separator()
						.defaults());
	}

	private Control createPopulateLocationControl() {
		CityTableModel cityTableModel = tableModel();

		return Control.builder()
						.command(this::populateLocation)
						.name("Populate location")
						.enabled(cityTableModel.citiesWithoutLocationSelected())
						.smallIcon(FrameworkIcons.instance().icon(Foundation.MAP))
						.build();
	}

	private void populateLocation() {
		CityTableModel tableModel = tableModel();
		PopulateLocationTask task = tableModel.populateLocationTask();

		Dialogs.progressWorkerDialog(task)
						.owner(this)
						.title("Populating locations")
						.maximumProgress(task.maximumProgress())
						.stringPainted(true)
						.control(Control.builder()
										.command(task::cancel)
										.name("Cancel")
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

	private static FilterTableCellRenderer populationRenderer(SwingEntityTableModel tableModel) {
		return EntityTableCellRenderer.builder(City.POPULATION, tableModel)
						.foreground((table, row, value) -> {
							if (value > 1_000_000) {
								return Color.YELLOW;
							}

							return null;
						})
						.build();
	}

	private static FilterTableCellRenderer nameRenderer(CityTableModel tableModel) {
		return EntityTableCellRenderer.builder(City.NAME, tableModel)
						.foreground((table, row, value) -> {
							Entity city = (Entity) table.model().items().visible().itemAt(row);
							if (Objects.equals(city.get(City.ID), city.get(City.COUNTRY_FK).get(Country.CAPITAL))) {
								return Color.GREEN;
							}

							return null;
						})
						.build();
	}
}
