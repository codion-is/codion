package org.jminor.framework.demos.world.ui;

import org.jminor.framework.demos.world.domain.World;
import org.jminor.framework.demos.world.model.CountryEditModel;
import org.jminor.framework.domain.entity.Entity;
import org.jminor.swing.framework.ui.EntityComboBox;
import org.jminor.swing.framework.ui.EntityEditPanel;
import org.jminor.swing.framework.ui.EntityPanelProvider;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.GridLayout;

import static org.jminor.swing.common.ui.Components.createEastButtonPanel;
import static org.jminor.swing.common.ui.Components.setPreferredWidth;
import static org.jminor.swing.common.ui.textfield.TextFields.makeUpperCase;

public final class CountryEditPanel extends EntityEditPanel {

  public CountryEditPanel(CountryEditModel editModel) {
    super(editModel);
  }

  @Override
  protected void initializeUI() {
    setInitialFocusProperty(World.COUNTRY_CODE);

    makeUpperCase(createTextField(World.COUNTRY_CODE)).setColumns(12);
    makeUpperCase(createTextField(World.COUNTRY_CODE2)).setColumns(12);
    createTextField(World.COUNTRY_NAME).setColumns(12);
    setPreferredWidth(createValueListComboBox(World.COUNTRY_CONTINENT), 120);
    setPreferredWidth(createPropertyComboBox(World.COUNTRY_REGION), 120);
    createTextField(World.COUNTRY_SURFACEAREA);
    createTextField(World.COUNTRY_INDEPYEAR);
    createTextField(World.COUNTRY_POPULATION);
    createTextField(World.COUNTRY_LIFEEXPECTANCY);
    createTextField(World.COUNTRY_GNP);
    createTextField(World.COUNTRY_GNPOLD);
    createTextField(World.COUNTRY_LOCALNAME).setColumns(12);
    setPreferredWidth(createPropertyComboBox(World.COUNTRY_GOVERNMENTFORM, null, true), 120);
    createTextField(World.COUNTRY_HEADOFSTATE).setColumns(12);
    EntityComboBox capitalComboBox =
            setPreferredWidth(createForeignKeyComboBox(World.COUNTRY_CAPITAL_FK), 120);
    //create a panel with a button for adding a new city
    JPanel capitalPanel = createEastButtonPanel(capitalComboBox,
            createEditPanelAction(capitalComboBox, new CityPanelProvider()), false);

    setLayout(new GridLayout(4, 5, 5, 5));

    addPropertyPanel(World.COUNTRY_CODE);
    addPropertyPanel(World.COUNTRY_CODE2);
    addPropertyPanel(World.COUNTRY_NAME);
    addPropertyPanel(World.COUNTRY_CONTINENT);
    addPropertyPanel(World.COUNTRY_REGION);
    addPropertyPanel(World.COUNTRY_SURFACEAREA);
    addPropertyPanel(World.COUNTRY_INDEPYEAR);
    addPropertyPanel(World.COUNTRY_POPULATION);
    addPropertyPanel(World.COUNTRY_LIFEEXPECTANCY);
    addPropertyPanel(World.COUNTRY_GNP);
    addPropertyPanel(World.COUNTRY_GNPOLD);
    addPropertyPanel(World.COUNTRY_LOCALNAME);
    addPropertyPanel(World.COUNTRY_GOVERNMENTFORM);
    addPropertyPanel(World.COUNTRY_HEADOFSTATE);
    add(createPropertyPanel(World.COUNTRY_CAPITAL_FK, capitalPanel));
  }

  /** A EntityPanelProvider for adding a new city */
  private final class CityPanelProvider extends EntityPanelProvider {

    public CityPanelProvider() {
      super(World.T_CITY);
      setEditPanelClass(CityEditPanel.class);
    }

    @Override
    protected void configureEditPanel(EntityEditPanel editPanel) {
      //set the country to the one selected in the CountryEditPanel
      Entity country = CountryEditPanel.this.getEditModel().getEntityCopy();
      if (country.getKey().isNotNull()) {
        //if a country is selected, then we don't allow it to be changed
        editPanel.getEditModel().put(World.CITY_COUNTRY_FK, country);
        //initialize the panel components, so we can configure the country component
        editPanel.initializePanel();
        //disable the country selection component
        JComponent countryComponent = editPanel.getComponent(World.CITY_COUNTRY_FK);
        countryComponent.setEnabled(false);
        countryComponent.setFocusable(false);
        //and change the initial focus property
        editPanel.setInitialFocusProperty(World.CITY_NAME);
      }
    }
  }
}
