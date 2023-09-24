/*
 * Copyright (c) 2023, Björn Darri Sigurðsson. All Rights Reserved.
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
      countryEditModel.setEntity(connectionProvider.connection().selectSingle(
              Country.NAME.equalTo("Afghanistan")));
      assertEquals(583_025, countryEditModel.averageCityPopulation().get());
      countryEditModel.setEntity(null);
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
