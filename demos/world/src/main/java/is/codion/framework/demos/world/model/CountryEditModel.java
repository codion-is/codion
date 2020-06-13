package is.codion.framework.demos.world.model;

import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.demos.world.domain.api.World.City;
import is.codion.framework.demos.world.domain.api.World.Country;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.Entity;
import is.codion.swing.framework.model.SwingEntityComboBoxModel;
import is.codion.swing.framework.model.SwingEntityEditModel;

import java.util.Objects;

public final class CountryEditModel extends SwingEntityEditModel {

  public CountryEditModel(EntityConnectionProvider connectionProvider) {
    super(Country.TYPE, connectionProvider);
  }

  @Override
  public SwingEntityComboBoxModel createForeignKeyComboBoxModel(Attribute<Entity> foreignKeyAttribute) {
    SwingEntityComboBoxModel comboBoxModel = super.createForeignKeyComboBoxModel(foreignKeyAttribute);
    if (foreignKeyAttribute.equals(Country.CAPITAL_FK)) {
      //only show cities for currently selected country
      addEntitySetListener(selectedCountry -> comboBoxModel.setIncludeCondition(
              city -> selectedCountry != null &&
                      Objects.equals(selectedCountry.get(Country.CODE),
                              city.get(City.COUNTRY_CODE))));
    }

    return comboBoxModel;
  }
}
