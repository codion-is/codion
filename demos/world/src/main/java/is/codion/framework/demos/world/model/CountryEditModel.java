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
 * Copyright (c) 2023, Björn Darri Sigurðsson.
 */
package is.codion.framework.demos.world.model;

import is.codion.common.db.exception.DatabaseException;
import is.codion.common.value.Value;
import is.codion.common.value.ValueObserver;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.demos.world.domain.api.World.City;
import is.codion.framework.demos.world.domain.api.World.Country;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.attribute.ForeignKey;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.model.component.EntityComboBoxModel;

public final class CountryEditModel extends SwingEntityEditModel {

  private final Value<Double> averageCityPopulation = Value.value();

  CountryEditModel(EntityConnectionProvider connectionProvider) {
    super(Country.TYPE, connectionProvider);
    initializeComboBoxModels(Country.CAPITAL_FK);
    addEntityListener(country -> averageCityPopulation.set(averageCityPopulation(country)));
  }

  @Override
  public EntityComboBoxModel createForeignKeyComboBoxModel(ForeignKey foreignKey) {
    EntityComboBoxModel comboBoxModel = super.createForeignKeyComboBoxModel(foreignKey);
    if (foreignKey.equals(Country.CAPITAL_FK)) {
      //only show cities for currently selected country
      addEntityListener(country ->
              comboBoxModel.includeCondition().set(cityEntity ->
                      cityEntity.castTo(City.class)
                              .isInCountry(country)));
    }

    return comboBoxModel;
  }

  public ValueObserver<Double> averageCityPopulation() {
    return averageCityPopulation.observer();
  }

  private Double averageCityPopulation(Entity country) {
    if (country == null) {
      return null;
    }
    try {
      return connectionProvider().connection().execute(Country.AVERAGE_CITY_POPULATION, country.get(Country.CODE));
    }
    catch (DatabaseException e) {
      throw new RuntimeException(e);
    }
  }
}
