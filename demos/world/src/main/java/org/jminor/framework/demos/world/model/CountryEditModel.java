package org.jminor.framework.demos.world.model;

import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.demos.world.domain.World;
import org.jminor.framework.domain.property.ForeignKeyProperty;
import org.jminor.swing.framework.model.SwingEntityComboBoxModel;
import org.jminor.swing.framework.model.SwingEntityEditModel;

import java.util.Objects;

public final class CountryEditModel extends SwingEntityEditModel {

  public CountryEditModel(EntityConnectionProvider connectionProvider) {
    super(World.T_COUNTRY, connectionProvider);
  }

  @Override
  public SwingEntityComboBoxModel createForeignKeyComboBoxModel(ForeignKeyProperty foreignKeyProperty) {
    SwingEntityComboBoxModel comboBoxModel = super.createForeignKeyComboBoxModel(foreignKeyProperty);
    if (foreignKeyProperty.is(World.COUNTRY_CAPITAL_FK)) {
      //only show cities for currently selected country
      addEntitySetListener(selectedCountry -> comboBoxModel.setIncludeCondition(
              city -> selectedCountry != null &&
                      Objects.equals(selectedCountry.get(World.COUNTRY_CODE),
                              city.get(World.CITY_COUNTRYCODE))));
    }

    return comboBoxModel;
  }
}
