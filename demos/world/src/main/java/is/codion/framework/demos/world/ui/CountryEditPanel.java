package is.codion.framework.demos.world.ui;

import is.codion.framework.demos.world.domain.api.World.City;
import is.codion.framework.demos.world.domain.api.World.Country;
import is.codion.framework.demos.world.model.CountryEditModel;
import is.codion.framework.domain.entity.Entity;
import is.codion.swing.common.ui.component.Components;
import is.codion.swing.common.ui.textfield.DoubleField;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.ui.EntityComboBox;
import is.codion.swing.framework.ui.EntityEditPanel;
import is.codion.swing.framework.ui.EntityPanel;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import static is.codion.swing.common.ui.layout.Layouts.gridLayout;
import static is.codion.swing.common.ui.panel.Panels.createEastButtonPanel;

final class CountryEditPanel extends EntityEditPanel {

  CountryEditPanel(SwingEntityEditModel editModel) {
    super(editModel);
  }

  @Override
  protected void initializeUI() {
    setInitialFocusAttribute(Country.CODE);

    createTextField(Country.CODE)
            .upperCase(true);
    createTextField(Country.CODE_2)
            .upperCase(true);
    createTextField(Country.NAME);
    createItemComboBox(Country.CONTINENT)
            .preferredWidth(120);
    createAttributeComboBox(Country.REGION)
            .preferredWidth(120);
    createTextField(Country.SURFACEAREA);
    createTextField(Country.INDEPYEAR);
    createTextField(Country.POPULATION);
    createTextField(Country.LIFE_EXPECTANCY);
    createTextField(Country.GNP);
    createTextField(Country.GNPOLD);
    createTextField(Country.LOCALNAME);
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
            .focusable(false)
            .editable(false)
            .build();

    setLayout(gridLayout(4, 5));

    addInputPanel(Country.CODE);
    addInputPanel(Country.CODE_2);
    addInputPanel(Country.NAME);
    addInputPanel(Country.CONTINENT);
    addInputPanel(Country.REGION);
    addInputPanel(Country.SURFACEAREA);
    addInputPanel(Country.INDEPYEAR);
    addInputPanel(Country.POPULATION);
    addInputPanel(Country.LIFE_EXPECTANCY);
    addInputPanel(Country.GNP);
    addInputPanel(Country.GNPOLD);
    addInputPanel(Country.LOCALNAME);
    addInputPanel(Country.GOVERNMENTFORM);
    addInputPanel(Country.HEADOFSTATE);
    addInputPanel(Country.CAPITAL_FK, capitalPanel);
    add(createInputPanel(new JLabel("Avg. city population"), averageCityPopulationField));
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
