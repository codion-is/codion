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
 * Copyright (c) 2023 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.demos.world.model;

import is.codion.common.user.User;
import is.codion.common.value.Value;
import is.codion.demos.world.domain.WorldImpl;
import is.codion.demos.world.domain.api.World.City;
import is.codion.demos.world.domain.api.World.Country;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.local.LocalEntityConnectionProvider;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.swing.common.model.worker.ProgressWorker.ProgressReporter;

import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRField;
import net.sf.jasperreports.engine.base.JRBaseField;
import org.junit.jupiter.api.Test;

import java.util.List;

import static is.codion.framework.db.EntityConnection.Select.where;
import static is.codion.framework.domain.entity.OrderBy.ascending;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public final class CountryReportDataSourceTest {

	private static final User UNIT_TEST_USER =
					User.parse(System.getProperty("codion.test.user", "scott:tiger"));

	@Test
	void iterate() throws JRException {
		try (EntityConnectionProvider connectionProvider = createConnectionProvider()) {
			Value<Integer> progressCounter = Value.nullable();
			Value<String> publishedValue = Value.nullable();
			ProgressReporter<String> progressReporter = new ProgressReporter<>() {
				@Override
				public void report(int progress) {
					progressCounter.set(progress);
				}

				@Override
				public void publish(String... chunks) {
					publishedValue.set(chunks[0]);
				}
			};

			EntityConnection connection = connectionProvider.connection();
			List<Entity> countries =
							connection.select(where(Country.NAME.in("Denmark", "Iceland"))
											.orderBy(ascending(Country.NAME))
											.build());
			CountryReportDataSource countryReportDataSource = new CountryReportDataSource(countries.iterator(), connection, progressReporter);
			assertThrows(IllegalStateException.class, countryReportDataSource::cityDataSource);

			countryReportDataSource.next();
			assertEquals("Denmark", countryReportDataSource.getFieldValue(field(Country.NAME)));
			assertEquals("Europe", countryReportDataSource.getFieldValue(field(Country.CONTINENT)));
			assertEquals("Nordic Countries", countryReportDataSource.getFieldValue(field(Country.REGION)));
			assertEquals(43094d, countryReportDataSource.getFieldValue(field(Country.SURFACEAREA)));
			assertEquals(5330000, countryReportDataSource.getFieldValue(field(Country.POPULATION)));
			assertThrows(JRException.class, () -> countryReportDataSource.getFieldValue(field(City.LOCATION)));

			JRDataSource denmarkCityDataSource = countryReportDataSource.cityDataSource();
			denmarkCityDataSource.next();
			assertEquals("København", denmarkCityDataSource.getFieldValue(field(City.NAME)));
			assertEquals(495699, denmarkCityDataSource.getFieldValue(field(City.POPULATION)));
			assertThrows(JRException.class, () -> denmarkCityDataSource.getFieldValue(field(Country.REGION)));
			denmarkCityDataSource.next();
			assertEquals("Århus", denmarkCityDataSource.getFieldValue(field(City.NAME)));

			countryReportDataSource.next();
			assertEquals("Iceland", countryReportDataSource.getFieldValue(field(Country.NAME)));

			JRDataSource icelandCityDataSource = countryReportDataSource.cityDataSource();
			icelandCityDataSource.next();
			assertEquals("Reykjavík", icelandCityDataSource.getFieldValue(field(City.NAME)));

			assertEquals(2, progressCounter.get());
			assertEquals("Iceland", publishedValue.get());
		}
	}

	private static EntityConnectionProvider createConnectionProvider() {
		return LocalEntityConnectionProvider.builder()
						.domain(new WorldImpl())
						.user(UNIT_TEST_USER)
						.build();
	}

	private static JRField field(Attribute<?> attribute) {
		return new TestField(attribute.name());
	}

	private static final class TestField extends JRBaseField {

		private TestField(String name) {
			this.name = name;
		}
	}
}
