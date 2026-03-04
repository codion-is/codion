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

import is.codion.common.reactive.event.Event;
import is.codion.common.reactive.observer.Observer;
import is.codion.common.reactive.state.ObservableState;
import is.codion.common.reactive.state.State;
import is.codion.demos.world.domain.api.World.City;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.exception.EntityValidationException;
import is.codion.swing.common.model.worker.ProgressWorker.ProgressReporter;
import is.codion.swing.common.model.worker.ProgressWorker.ProgressResultTask;
import is.codion.swing.framework.model.SwingEntityTableModel;

import org.jfree.data.general.DefaultPieDataset;
import org.jfree.data.general.PieDataset;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

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
		items().included().addConsumer(this::refreshChartDataset);
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

	public final class PopulateLocationTask implements ProgressResultTask<Collection<Entity>, Entity> {

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

		public State cancelled() {
			return cancelled;
		}

		public void publish(Collection<Entity> cities) {
			cities.forEach(city -> items().replace(city, city));
			displayLocationEvent.accept(cities);
		}

		public void result(Collection<Entity> cities) {
			displayLocationEvent.accept(cities);
		}

		@Override
		public Collection<Entity> execute(ProgressReporter<Entity> progressReporter) throws IOException, EntityValidationException {
			Collection<Entity> updatedCities = new ArrayList<>();
			Iterator<Entity> citiesIterator = cities.iterator();
			EntityConnection connection = editModel().connection();
			while (citiesIterator.hasNext() && !cancelled.is()) {
				Entity city = CityEditModel.populateLocation(citiesIterator.next(), connection);
				updatedCities.add(city);
				progressReporter.publish(city);
				progressReporter.report(updatedCities.size());
			}

			return updatedCities;
		}
	}
}
