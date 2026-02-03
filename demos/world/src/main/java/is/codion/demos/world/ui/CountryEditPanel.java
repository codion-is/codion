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

import is.codion.common.model.filter.FilterModel.Items;
import is.codion.demos.world.domain.api.World.City;
import is.codion.demos.world.domain.api.World.Country;
import is.codion.demos.world.model.CountryEditModel;
import is.codion.framework.domain.entity.Entity;
import is.codion.swing.common.ui.component.image.ImagePane;
import is.codion.swing.common.ui.component.text.NumberField;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.dialog.Dialogs;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.ui.EntityEditPanel;

import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.io.IOException;
import java.nio.file.Files;

import static is.codion.common.reactive.state.State.present;
import static is.codion.swing.common.ui.component.Components.*;
import static is.codion.swing.common.ui.layout.Layouts.gridLayout;
import static javax.swing.BorderFactory.createEtchedBorder;

final class CountryEditPanel extends EntityEditPanel {

	private static final int PREFERRED_COMBO_BOX_WIDTH = 120;

	private final Items<Entity> cityTableItems;

	CountryEditPanel(SwingEntityEditModel editModel, Items<Entity> cityTableItems) {
		super(editModel);
		this.cityTableItems = cityTableItems;
	}

	@Override
	protected void initializeUI() {
		createTextField(Country.CODE)
						.columns(6)
						.upperCase(true);
		createTextField(Country.CODE_2)
						.columns(6)
						.upperCase(true);
		createTextField(Country.NAME);
		createTextField(Country.LOCALNAME);
		createItemComboBox(Country.CONTINENT)
						.preferredWidth(PREFERRED_COMBO_BOX_WIDTH);
		createComboBox(Country.REGION)
						.preferredWidth(PREFERRED_COMBO_BOX_WIDTH);
		createTextField(Country.SURFACEAREA)
						.columns(5);
		createTextField(Country.INDEPYEAR)
						.columns(5);
		createTextField(Country.POPULATION)
						.columns(5);
		createTextField(Country.LIFE_EXPECTANCY)
						.columns(5);
		createTextField(Country.GNP)
						.columns(6);
		createTextField(Country.GNPOLD)
						.columns(6);
		createComboBox(Country.GOVERNMENTFORM)
						.preferredWidth(PREFERRED_COMBO_BOX_WIDTH)
						.editable(true);
		createTextField(Country.HEADOFSTATE);
		//create a panel with a button for adding a new city
		createComboBoxPanel(Country.CAPITAL_FK, this::createCapitalEditPanel)
						.preferredComboBoxWidth(PREFERRED_COMBO_BOX_WIDTH)
						.includeAddButton(true);
		component(Country.FLAG).set(ImagePane.builder())
						.preferredWidth(50)
						.border(createEtchedBorder());
		//add a field displaying the avarage city population for the selected country
		CountryEditModel editModel = (CountryEditModel) editModel();
		NumberField<Double> averageCityPopulationField = doubleField()
						.link(editModel.averageCityPopulation())
						.fractionDigits(2)
						.grouping(true)
						.horizontalAlignment(SwingConstants.CENTER)
						.focusable(false)
						.editable(false)
						.build();

		JPanel codePanel = gridLayoutPanel(1, 2)
						.add(createInputPanel(Country.CODE))
						.add(createInputPanel(Country.CODE_2))
						.build();

		JPanel gnpPanel = gridLayoutPanel(1, 2)
						.add(createInputPanel(Country.GNP))
						.add(createInputPanel(Country.GNPOLD))
						.build();

		JPanel surfaceAreaIndYearPanel = gridLayoutPanel(1, 2)
						.add(createInputPanel(Country.SURFACEAREA))
						.add(createInputPanel(Country.INDEPYEAR))
						.build();

		JPanel populationLifeExpectancyPanel = gridLayoutPanel(1, 2)
						.add(createInputPanel(Country.POPULATION))
						.add(createInputPanel(Country.LIFE_EXPECTANCY))
						.build();

		setLayout(gridLayout(4, 5));

		add(codePanel);
		addInputPanel(Country.NAME);
		addInputPanel(Country.LOCALNAME);
		addInputPanel(Country.CAPITAL_FK);
		addInputPanel(Country.CONTINENT);
		addInputPanel(Country.REGION);
		add(surfaceAreaIndYearPanel);
		add(populationLifeExpectancyPanel);
		add(gnpPanel);
		addInputPanel(Country.GOVERNMENTFORM);
		addInputPanel(Country.HEADOFSTATE);
		add(borderLayoutPanel()
						.west(createInputPanel(Country.FLAG)
										.component(borderLayoutPanel()
														.layout(new BorderLayout())
														.center(component(Country.FLAG).get())
														.east(panel()
																		.layout(new GridLayout(1, 2))
																		.add(button()
																						.control(createAddFlagControl())
																						.transferFocusOnEnter(true))
																		.add(button()
																						.control(createRemoveFlagControl())
																						.transferFocusOnEnter(true)))))
						.center(borderLayoutPanel()
										.north(label("Avg. city pop.")
														.horizontalAlignment(SwingConstants.CENTER))
										.center(averageCityPopulationField)));
	}

	private Control createAddFlagControl() {
		return Control.builder()
						.command(this::addFlag)
						.caption("+")
						.build();
	}

	private Control createRemoveFlagControl() {
		return Control.builder()
						.command(this::removeFlag)
						.enabled(present(editModel().editor().value(Country.FLAG)))
						.caption("-")
						.build();
	}

	private void addFlag() throws IOException {
		editModel().editor().value(Country.FLAG).set(Files.readAllBytes(Dialogs.select()
						.files()
						.owner(this)
						.title("Select flag")
						.filter(new FileNameExtensionFilter("PNG image files", "png"))
						.selectFile()
						.toPath()));
	}

	private void removeFlag() {
		editModel().editor().value(Country.FLAG).clear();
	}

	private EntityEditPanel createCapitalEditPanel() {
		SwingEntityEditModel cityEditModel = new SwingEntityEditModel(City.TYPE, editModel().connectionProvider());
		CityEditPanel capitalEditPanel = new CityEditPanel(cityEditModel);
		if (editModel().editor().exists().is()) {
			//add the city to the table model items when a new city is inserted
			cityEditModel.afterInsert().addConsumer(cityTableItems::add);
			//if an existing country is selected, then we assume we are adding a city in that country
			capitalEditPanel.editModel().editor().value(City.COUNTRY_FK).set(editModel().editor().get());
			//initialize the panel components, so we can configure the country component
			capitalEditPanel.initialize();
			//disables the country selection component and changes the initial focus property
			capitalEditPanel.disableCountryInput();
		}

		return capitalEditPanel;
	}
}
