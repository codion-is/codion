/*
 * Copyright (c) 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.world.ui;

import is.codion.framework.demos.world.domain.api.World.City;
import is.codion.framework.demos.world.domain.api.World.Country;
import is.codion.framework.demos.world.model.CountryEditModel;
import is.codion.framework.domain.entity.Entity;
import is.codion.swing.common.ui.component.text.NumberField;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.ui.EntityEditPanel;
import is.codion.swing.framework.ui.component.EntityComboBox;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import static is.codion.swing.common.ui.component.Components.*;
import static is.codion.swing.common.ui.component.button.ButtonPanelBuilder.createEastButtonPanel;
import static is.codion.swing.common.ui.layout.Layouts.gridLayout;

final class CountryEditPanel extends EntityEditPanel {

  CountryEditPanel(SwingEntityEditModel editModel) {
    super(editModel);
  }

  @Override
  protected void initializeUI() {
    setInitialFocusAttribute(Country.CODE);

    createTextField(Country.CODE)
            .columns(6)
            .upperCase(true);
    createTextField(Country.CODE_2)
            .columns(6)
            .upperCase(true);
    createTextField(Country.NAME);
    createTextField(Country.LOCALNAME);
    createItemComboBox(Country.CONTINENT)
            .preferredWidth(120);
    createComboBox(Country.REGION)
            .preferredWidth(120);
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
            .preferredWidth(120)
            .editable(true);
    createTextField(Country.HEADOFSTATE);
    EntityComboBox capitalComboBox = createForeignKeyComboBox(Country.CAPITAL_FK)
            .preferredWidth(120)
            .build();
    //create a panel with a button for adding a new city
    JPanel capitalPanel = createEastButtonPanel(capitalComboBox,
            createInsertControl(capitalComboBox, this::createCapitalEditPanel));
    //add a field displaying the avarage city population for the selected country
    CountryEditModel editModel = editModel();
    NumberField<Double> averageCityPopulationField = doubleField()
            .linkedValue(editModel.averageCityPopulation())
            .maximumFractionDigits(2)
            .groupingUsed(true)
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
    addInputPanel(Country.CAPITAL_FK, capitalPanel);
    addInputPanel(Country.CONTINENT);
    addInputPanel(Country.REGION);
    add(surfaceAreaIndYearPanel);
    add(populationLifeExpectancyPanel);
    add(gnpPanel);
    addInputPanel(Country.GOVERNMENTFORM);
    addInputPanel(Country.HEADOFSTATE);
    add(createInputPanel(label("Avg. city population")
            .horizontalAlignment(SwingConstants.CENTER)
            .build(), averageCityPopulationField));
  }

  private EntityEditPanel createCapitalEditPanel() {
    CityEditPanel capitalEditPanel = new CityEditPanel(new SwingEntityEditModel(City.TYPE, editModel().connectionProvider()));
    Entity country = editModel().entity();
    if (country.primaryKey().isNotNull()) {
      //if a country is selected, then we don't allow it to be changed
      capitalEditPanel.editModel().put(City.COUNTRY_FK, country);
      //initialize the panel components, so we can configure the country component
      capitalEditPanel.initialize();
      //disable the country selection component
      JComponent countryComponent = capitalEditPanel.component(City.COUNTRY_FK);
      countryComponent.setEnabled(false);
      countryComponent.setFocusable(false);
      //and change the initial focus property
      capitalEditPanel.setInitialFocusAttribute(City.NAME);
    }

    return capitalEditPanel;
  }
}
