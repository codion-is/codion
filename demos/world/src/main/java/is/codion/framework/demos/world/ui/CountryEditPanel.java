package is.codion.framework.demos.world.ui;

import is.codion.framework.demos.world.domain.api.World.City;
import is.codion.framework.demos.world.domain.api.World.Country;
import is.codion.framework.demos.world.model.CountryEditModel;
import is.codion.framework.domain.entity.Entity;
import is.codion.swing.common.ui.component.Components;
import is.codion.swing.common.ui.component.textfield.DoubleField;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.ui.EntityComboBox;
import is.codion.swing.framework.ui.EntityEditPanel;
import is.codion.swing.framework.ui.EntityPanel;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import static is.codion.swing.common.ui.component.Components.label;
import static is.codion.swing.common.ui.component.Components.panel;
import static is.codion.swing.common.ui.component.panel.Panels.createEastButtonPanel;
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
    createAttributeComboBox(Country.REGION)
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
    createAttributeComboBox(Country.GOVERNMENTFORM)
            .preferredWidth(120)
            .editable(true);
    createTextField(Country.HEADOFSTATE);
    EntityComboBox capitalComboBox = createForeignKeyComboBox(Country.CAPITAL_FK)
            .preferredWidth(120)
            .build();
    //create a panel with a button for adding a new city
    JPanel capitalPanel = createEastButtonPanel(capitalComboBox, EntityPanel.builder(City.TYPE)
            .editPanelClass(CityEditPanel.class)
            .editPanelInitializer(this::initializeCapitalEditPanel)
            .createEditPanelAction(capitalComboBox));
    //add a field displaying the avarage city population for the selected country
    CountryEditModel editModel = (CountryEditModel) getEditModel();
    DoubleField averageCityPopulationField = Components.doubleField()
            .linkedValueObserver(editModel.getAverageCityPopulationValue())
            .maximumFractionDigits(2)
            .groupingUsed(true)
            .horizontalAlignment(SwingConstants.CENTER)
            .focusable(false)
            .editable(false)
            .build();

    JPanel codePanel = panel(gridLayout(1, 2))
            .add(createInputPanel(Country.CODE))
            .add(createInputPanel(Country.CODE_2))
            .build();

    JPanel gnpPanel = panel(gridLayout(1, 2))
            .add(createInputPanel(Country.GNP))
            .add(createInputPanel(Country.GNPOLD))
            .build();

    JPanel surfaceAreaIndYear = panel(gridLayout(1, 2))
            .add(createInputPanel(Country.SURFACEAREA))
            .add(createInputPanel(Country.INDEPYEAR))
            .build();

    JPanel populationLifeExpectancyPanel = panel(gridLayout(1, 2))
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
    add(surfaceAreaIndYear);
    add(populationLifeExpectancyPanel);
    add(gnpPanel);
    addInputPanel(Country.GOVERNMENTFORM);
    addInputPanel(Country.HEADOFSTATE);
    add(createInputPanel(label("Avg. city population")
            .horizontalAlignment(SwingConstants.CENTER)
            .build(), averageCityPopulationField));
  }

  private void initializeCapitalEditPanel(EntityEditPanel capitalEditPanel) {
    //set the country to the one selected in the CountryEditPanel
    Entity country = getEditModel().getEntityCopy();
    if (country.getPrimaryKey().isNotNull()) {
      //if a country is selected, then we don't allow it to be changed
      capitalEditPanel.getEditModel().put(City.COUNTRY_FK, country);
      //initialize the panel components, so we can configure the country component
      capitalEditPanel.initializePanel();
      //disable the country selection component
      JComponent countryComponent = capitalEditPanel.getComponent(City.COUNTRY_FK);
      countryComponent.setEnabled(false);
      countryComponent.setFocusable(false);
      //and change the initial focus property
      capitalEditPanel.setInitialFocusAttribute(City.NAME);
    }
  }
}
