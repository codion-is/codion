package is.codion.framework.demos.world.ui;

import is.codion.framework.demos.world.domain.api.World.City;
import is.codion.framework.demos.world.domain.api.World.Country;
import is.codion.framework.demos.world.model.CountryEditModel;
import is.codion.framework.domain.entity.Entity;
import is.codion.swing.common.ui.textfield.DoubleField;
import is.codion.swing.common.ui.value.ComponentValue;
import is.codion.swing.common.ui.value.ComponentValues;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.ui.EntityComboBox;
import is.codion.swing.framework.ui.EntityEditPanel;
import is.codion.swing.framework.ui.EntityPanel;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import static is.codion.swing.common.ui.Components.createEastButtonPanel;
import static is.codion.swing.common.ui.layout.Layouts.gridLayout;

public final class CountryEditPanel extends EntityEditPanel {

  public CountryEditPanel(SwingEntityEditModel editModel) {
    super(editModel);
  }

  @Override
  protected void initializeUI() {
    setInitialFocusAttribute(Country.CODE);

    textField(Country.CODE).upperCase().columns(12);
    textField(Country.CODE_2).upperCase().columns(12);
    textField(Country.NAME).columns(12);
    valueListComboBox(Country.CONTINENT).preferredWidth(120);
    attributeComboBox(Country.REGION).preferredWidth(120);
    textField(Country.SURFACEAREA);
    textField(Country.INDEPYEAR);
    textField(Country.POPULATION);
    textField(Country.LIFE_EXPECTANCY);
    textField(Country.GNP);
    textField(Country.GNPOLD);
    textField(Country.LOCALNAME).columns(12);
    attributeComboBox(Country.GOVERNMENTFORM).preferredWidth(120).editable(true);
    textField(Country.HEADOFSTATE).columns(12);
    EntityComboBox capitalComboBox =
            foreignKeyComboBox(Country.CAPITAL_FK)
                    .preferredWidth(120)
                    .build();
    //create a panel with a button for adding a new city
    JPanel capitalPanel = createEastButtonPanel(capitalComboBox,
            EntityPanel.builder(City.TYPE)
                    .editPanelClass(CityEditPanel.class)
                    .editPanelInitializer(this::initializeCapitalEditPanel)
                    .createEditPanelAction(capitalComboBox));
    //add a field displaying the avarage city population for the selected country
    DoubleField averageCityPopulationField = new DoubleField();
    averageCityPopulationField.setEditable(false);
    averageCityPopulationField.setFocusable(false);
    ComponentValue<Double, DoubleField> averageCityPopulationFieldValue =
            ComponentValues.doubleField(averageCityPopulationField);
    averageCityPopulationFieldValue.link(((CountryEditModel) getEditModel()).getAvarageCityPopulationValue());

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
