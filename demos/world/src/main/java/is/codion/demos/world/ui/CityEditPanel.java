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
 * Copyright (c) 2023 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.demos.world.ui;

import is.codion.common.reactive.state.State;
import is.codion.demos.world.domain.api.World.City;
import is.codion.demos.world.model.CityEditModel;
import is.codion.demos.world.model.CityTableModel;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.exception.EntityValidationException;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.ui.EntityEditPanel;
import is.codion.swing.framework.ui.icon.FrameworkIcons;

import org.jxmapviewer.JXMapKit;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static is.codion.swing.common.ui.component.Components.gridLayoutPanel;
import static is.codion.swing.common.ui.layout.Layouts.borderLayout;
import static java.util.stream.Collectors.toSet;

public final class CityEditPanel extends EntityEditPanel {

	private final JXMapKit mapKit;

	public CityEditPanel(SwingEntityEditModel editModel) {
		super(editModel);
		this.mapKit = null;
	}

	CityEditPanel(CityTableModel tableModel) {
		super(tableModel.editModel());
		this.mapKit = Maps.createMapKit();
		tableModel.displayLocations().addConsumer(this::displayLocations);
		configureControls(config -> config
						.control(Control.builder()
										.command(this::populateLocation)
										.enabled(State.and(active(),
														editor().value(City.LOCATION).present().not(),
														editor().exists()))
										.icon(FrameworkIcons.instance().get("map"))));
	}

	@Override
	protected void initializeUI() {
		create().comboBox(City.COUNTRY_FK)
						.preferredWidth(120);
		create().textField(City.NAME);
		create().textField(City.DISTRICT);
		create().textField(City.POPULATION);

		JPanel inputPanel = gridLayoutPanel(0, 1)
						.add(create().inputPanel(City.COUNTRY_FK))
						.add(create().inputPanel(City.NAME))
						.add(create().inputPanel(City.DISTRICT))
						.add(create().inputPanel(City.POPULATION))
						.build();

		JPanel centerPanel = gridLayoutPanel(1, 0)
						.add(inputPanel)
						.build();
		if (mapKit != null) {
			centerPanel.add(mapKit);
		}
		setLayout(borderLayout());
		add(centerPanel, BorderLayout.CENTER);
	}

	void disableCountryInput() {
		JComponent countryComponent = component(City.COUNTRY_FK).get();
		countryComponent.setEnabled(false);
		countryComponent.setFocusable(false);
		focus().initial().set(City.NAME);
	}

	private void populateLocation() throws IOException, EntityValidationException {
		CityEditModel editModel = (CityEditModel) editModel();
		editModel.populateLocation();
		displayLocations(List.of(editModel.editor().entity().get()));
	}

	private void displayLocations(Collection<Entity> cities) {
		Maps.paintWaypoints(cities.stream()
						.map(city -> city.optional(City.LOCATION))
						.flatMap(Optional::stream)
						.collect(toSet()), mapKit.getMainMap());
	}
}
