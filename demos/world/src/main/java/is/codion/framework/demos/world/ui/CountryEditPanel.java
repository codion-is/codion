package is.codion.framework.demos.world.ui;

import is.codion.framework.demos.world.domain.api.World.City;
import is.codion.framework.demos.world.domain.api.World.Country;
import is.codion.framework.demos.world.model.CountryEditModel;
import is.codion.framework.domain.entity.Entity;
import is.codion.swing.common.ui.textfield.DoubleField;
import is.codion.swing.common.ui.value.ComponentValue;
import is.codion.swing.common.ui.value.NumericalValues;
import is.codion.swing.framework.ui.EntityComboBox;
import is.codion.swing.framework.ui.EntityEditPanel;
import is.codion.swing.framework.ui.EntityInputComponents.Editable;
import is.codion.swing.framework.ui.EntityPanelBuilder;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import static is.codion.swing.common.ui.Components.createEastButtonPanel;
import static is.codion.swing.common.ui.Components.setPreferredWidth;
import static is.codion.swing.common.ui.layout.Layouts.gridLayout;
import static is.codion.swing.common.ui.textfield.TextFields.upperCase;

public final class CountryEditPanel extends EntityEditPanel {

  public CountryEditPanel(CountryEditModel editModel) {
    super(editModel);
  }

  @Override
  protected void initializeUI() {
    setInitialFocusAttribute(Country.CODE);

    upperCase(createTextField(Country.CODE)).setColumns(12);
    upperCase(createTextField(Country.CODE_2)).setColumns(12);
    createTextField(Country.NAME).setColumns(12);
    setPreferredWidth(createValueListComboBox(Country.CONTINENT), 120);
    setPreferredWidth(createAttributeComboBox(Country.REGION), 120);
    createTextField(Country.SURFACEAREA);
    createTextField(Country.INDEPYEAR);
    createTextField(Country.POPULATION);
    createTextField(Country.LIFE_EXPECTANCY);
    createTextField(Country.GNP);
    createTextField(Country.GNPOLD);
    createTextField(Country.LOCALNAME).setColumns(12);
    setPreferredWidth(createAttributeComboBox(Country.GOVERNMENTFORM, null, Editable.YES), 120);
    createTextField(Country.HEADOFSTATE).setColumns(12);
    EntityComboBox capitalComboBox =
            setPreferredWidth(createForeignKeyComboBox(Country.CAPITAL_FK), 120);
    //create a panel with a button for adding a new city
    JPanel capitalPanel = createEastButtonPanel(capitalComboBox,
            new CityPanelBuilder().createEditPanelAction(capitalComboBox));
    //add a field displaying the avarage city population for the selected country
    ComponentValue<Double, DoubleField> averageCityPopulationFieldValue = NumericalValues.doubleValue();
    final DoubleField averageCityPopulationField = averageCityPopulationFieldValue.getComponent();
    averageCityPopulationField.setEditable(false);
    averageCityPopulationField.setFocusable(false);
    ((CountryEditModel) getEditModel()).getAvarageCityPopulationValue().link(averageCityPopulationFieldValue);

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
    add(createInputPanel(Country.CAPITAL_FK, capitalPanel));
    add(createInputPanel(new JLabel("Avg. city population"), averageCityPopulationField));
  }

  /** A EntityPanelBuilder for adding a new city */
  private final class CityPanelBuilder extends EntityPanelBuilder {

    public CityPanelBuilder() {
      super(City.TYPE);
      editPanelClass(CityEditPanel.class);
    }

    @Override
    protected void configureEditPanel(EntityEditPanel editPanel) {
      //set the country to the one selected in the CountryEditPanel
      Entity country = CountryEditPanel.this.getEditModel().getEntityCopy();
      if (country.getPrimaryKey().isNotNull()) {
        //if a country is selected, then we don't allow it to be changed
        editPanel.getEditModel().put(City.COUNTRY_FK, country);
        //initialize the panel components, so we can configure the country component
        editPanel.initializePanel();
        //disable the country selection component
        JComponent countryComponent = editPanel.getComponent(City.COUNTRY_FK);
        countryComponent.setEnabled(false);
        countryComponent.setFocusable(false);
        //and change the initial focus property
        editPanel.setInitialFocusAttribute(City.NAME);
      }
    }
  }
}
