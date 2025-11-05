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
 * Copyright (c) 2023 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.demos.world.model;

import is.codion.common.reactive.observer.Observable;
import is.codion.common.reactive.value.Value;
import is.codion.demos.world.domain.api.World.City;
import is.codion.demos.world.domain.api.World.Country;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.attribute.ForeignKey;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.model.component.EntityComboBoxModel;

import java.util.Objects;

public final class CountryEditModel extends SwingEntityEditModel {

	private final Value<Double> averageCityPopulation = Value.nullable();

	CountryEditModel(EntityConnectionProvider connectionProvider) {
		super(Country.TYPE, connectionProvider);
		initializeComboBoxModels(Country.CAPITAL_FK);
		editor().addConsumer(country ->
						averageCityPopulation.set(averageCityPopulation(country)));
	}

	@Override
	protected void configure(ForeignKey foreignKey, EntityComboBoxModel comboBoxModel) {
		if (foreignKey.equals(Country.CAPITAL_FK)) {
			//only show cities for currently selected country
			editor().addConsumer(country ->
							comboBoxModel.filter().predicate().set(city ->
											country != null && Objects.equals(city.get(City.COUNTRY_FK), country)));
		}
	}

	public Observable<Double> averageCityPopulation() {
		return averageCityPopulation.observable();
	}

	private Double averageCityPopulation(Entity country) {
		return country == null ? null :
						connection().execute(Country.AVERAGE_CITY_POPULATION, country.get(Country.CODE));
	}
}
