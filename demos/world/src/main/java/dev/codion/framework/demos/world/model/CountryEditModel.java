package dev.codion.framework.demos.world.model;

import dev.codion.framework.db.EntityConnectionProvider;
import dev.codion.framework.demos.world.domain.World;
import dev.codion.framework.domain.property.ForeignKeyProperty;
import dev.codion.swing.framework.model.SwingEntityComboBoxModel;
import dev.codion.swing.framework.model.SwingEntityEditModel;

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
                              city.get(World.CITY_COUNTRY_CODE))));
    }

    return comboBoxModel;
  }
}
