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

import is.codion.demos.world.domain.api.World.City;
import is.codion.demos.world.domain.api.World.Country;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.domain.entity.Entity;
import is.codion.plugin.jasperreports.JasperReportsDataSource;
import is.codion.swing.common.model.worker.ProgressWorker.ProgressReporter;

import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRField;

import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import static is.codion.framework.db.EntityConnection.Select.where;
import static is.codion.framework.domain.entity.OrderBy.descending;

public final class CountryReportDataSource extends JasperReportsDataSource<Entity> {

	private final EntityConnection connection;

	CountryReportDataSource(Iterator<Entity> countryIterator,
													EntityConnection connection,
													ProgressReporter<String> progressReporter) {
		super(countryIterator, new CountryValueProvider(),
						new CountryReportProgressReporter(progressReporter));
		this.connection = connection;
	}

	/* See usage in src/main/reports/country_report.jrxml, subreport element */
	public JRDataSource cityDataSource() {
		Entity country = currentItem();
		Collection<Entity> largestCities =
						connection.select(where(City.COUNTRY_FK.equalTo(country))
										.attributes(City.NAME, City.POPULATION)
										.orderBy(descending(City.POPULATION))
										.limit(5)
										.build());

		return new JasperReportsDataSource<>(largestCities.iterator(), new CityValueProvider());
	}

	private static final class CountryValueProvider implements BiFunction<Entity, JRField, Object> {

		private static final String NAME = "name";
		private static final String CONTINENT = "continent";
		private static final String REGION = "region";
		private static final String SURFACEAREA = "surfacearea";
		private static final String POPULATION = "population";

		@Override
		public Object apply(Entity country, JRField field) {
			return switch (field.getName()) {
				case NAME -> country.get(Country.NAME);
				case CONTINENT -> country.get(Country.CONTINENT);
				case REGION -> country.get(Country.REGION);
				case SURFACEAREA -> country.get(Country.SURFACEAREA);
				case POPULATION -> country.get(Country.POPULATION);
				default -> throw new IllegalArgumentException("Unknown field: " + field.getName());
			};
		}
	}

	private static final class CityValueProvider implements BiFunction<Entity, JRField, Object> {

		private static final String NAME = "name";
		private static final String POPULATION = "population";

		@Override
		public Object apply(Entity city, JRField field) {
			return switch (field.getName()) {
				case NAME -> city.get(City.NAME);
				case POPULATION -> city.get(City.POPULATION);
				default -> throw new IllegalArgumentException("Unknown field: " + field.getName());
			};
		}
	}

	private static final class CountryReportProgressReporter implements Consumer<Entity> {

		private final AtomicInteger counter = new AtomicInteger();
		private final ProgressReporter<String> progressReporter;

		private CountryReportProgressReporter(ProgressReporter<String> progressReporter) {
			this.progressReporter = progressReporter;
		}

		@Override
		public void accept(Entity country) {
			progressReporter.publish(country.get(Country.NAME));
			progressReporter.report(counter.incrementAndGet());
		}
	}
}
