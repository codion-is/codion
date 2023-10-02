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
import is.codion.common.user.User;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.local.LocalEntityConnectionProvider;
import is.codion.framework.demos.world.domain.WorldImpl;
import is.codion.framework.demos.world.domain.api.World.Country;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class CountryEditModelTest {

  private static final User UNIT_TEST_USER =
          User.parse(System.getProperty("codion.test.user", "scott:tiger"));

  @Test
  void averageCityPopulation() throws DatabaseException {
    try (EntityConnectionProvider connectionProvider = createConnectionProvider()) {
      CountryEditModel countryEditModel = new CountryEditModel(connectionProvider);
      countryEditModel.set(connectionProvider.connection().selectSingle(
              Country.NAME.equalTo("Afghanistan")));
      assertEquals(583_025, countryEditModel.averageCityPopulation().get());
      countryEditModel.set(null);
      assertNull(countryEditModel.averageCityPopulation().get());
    }
  }

  private static EntityConnectionProvider createConnectionProvider() {
    return LocalEntityConnectionProvider.builder()
            .domain(new WorldImpl())
            .user(UNIT_TEST_USER)
            .build();
  }
}
