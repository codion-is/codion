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

import is.codion.common.event.Event;
import is.codion.common.observable.Observer;
import is.codion.common.state.ObservableState;
import is.codion.common.state.State;
import is.codion.demos.world.domain.api.World.City;
import is.codion.demos.world.domain.api.World.Country;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.Entity;
import is.codion.swing.common.model.worker.ProgressWorker.ProgressReporter;
import is.codion.swing.common.model.worker.ProgressWorker.ProgressResultTask;
import is.codion.swing.framework.model.SwingEntityTableModel;

import org.jfree.data.general.DefaultPieDataset;
import org.jfree.data.general.PieDataset;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public final class CityTableModel extends SwingEntityTableModel {

	private final DefaultPieDataset<String> chartDataset = new DefaultPieDataset<>();
	private final Event<Collection<Entity>> displayLocationEvent = Event.event();
	private final State cityWithoutLocationSelected = State.state();

	CityTableModel(EntityConnectionProvider connectionProvider) {
		super(new CityEditModel(connectionProvider));
		selection().items().addConsumer(displayLocationEvent);
		selection().items().addConsumer(cities ->
						cityWithoutLocationSelected.set(cities.stream()
										.anyMatch(city -> city.isNull(City.LOCATION))));
		items().visible().addConsumer(this::refreshChartDataset);
	}

	public PieDataset<String> chartDataset() {
		return chartDataset;
	}

	public PopulateLocationTask populateLocationTask() {
		return new PopulateLocationTask();
	}

	public Observer<Collection<Entity>> displayLocations() {
		return displayLocationEvent.observer();
	}

	public ObservableState cityWithoutLocationSelected() {
		return cityWithoutLocationSelected.observable();
	}

	private void refreshChartDataset(Collection<Entity> cities) {
		chartDataset.clear();
		cities.forEach(city -> chartDataset.setValue(city.get(City.NAME), city.get(City.POPULATION)));
	}

	public final class PopulateLocationTask implements ProgressResultTask<Void, String> {

		private final State cancelled = State.state();
		private final Collection<Entity> cities;

		private PopulateLocationTask() {
			cities = selection().items().get().stream()
							.filter(city -> city.isNull(City.LOCATION))
							.toList();
		}

		@Override
		public int maximum() {
			return cities.size();
		}

		public ObservableState cancelled() {
			return cancelled.observable();
		}

		public void cancel() {
			cancelled.set(true);
		}

		@Override
		public Void execute(ProgressReporter<String> progressReporter) throws IOException {
			Collection<Entity> updatedCities = new ArrayList<>();
			CityEditModel editModel = (CityEditModel) editModel();
			Iterator<Entity> citiesIterator = cities.iterator();
			while (citiesIterator.hasNext() && !cancelled.is()) {
				Entity city = citiesIterator.next();
				progressReporter.publish(city.get(City.COUNTRY_FK).get(Country.NAME) + " - " + city.get(City.NAME));
				editModel.populateLocation(city);
				updatedCities.add(city);
				progressReporter.report(updatedCities.size());
				displayLocationEvent.accept(List.of(city));
			}
			displayLocationEvent.accept(selection().items().get());

			return null;
		}
	}
}
