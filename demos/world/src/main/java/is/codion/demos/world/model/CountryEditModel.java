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
 * Copyright (c) 2023 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.demos.world.model;

import is.codion.common.reactive.observer.Observable;
import is.codion.common.reactive.value.Value;
import is.codion.demos.world.domain.api.World.City;
import is.codion.demos.world.domain.api.World.Country;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.Entity;
import is.codion.swing.framework.model.SwingEntityEditModel;

import java.util.Objects;

public final class CountryEditModel extends SwingEntityEditModel {

	private final Value<Double> averageCityPopulation = Value.nullable();

	CountryEditModel(EntityConnectionProvider connectionProvider) {
		super(Country.TYPE, connectionProvider);
		editor().entity().addConsumer(this::setAverageCityPopulation);
		//only show cities for currently selected country
		editor().entity().addConsumer(this::filterCities);
	}

	public Observable<Double> averageCityPopulation() {
		return averageCityPopulation.observable();
	}

	private void setAverageCityPopulation(Entity country) {
		averageCityPopulation.set(country == null ? null :
						connection().execute(Country.AVERAGE_CITY_POPULATION, country.get(Country.CODE)));
	}

	private void filterCities(Entity country) {
		editor().comboBoxModels().get(Country.CAPITAL_FK).filter().predicate().set(city ->
						country != null && Objects.equals(city.get(City.COUNTRY_FK), country));
	}
}
