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
package is.codion.framework.demos.world.model;

import is.codion.common.db.exception.DatabaseException;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.demos.world.domain.api.World.City;
import is.codion.framework.demos.world.domain.api.World.Country;
import is.codion.framework.demos.world.domain.api.World.Location;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.exception.ValidationException;
import is.codion.swing.framework.model.SwingEntityEditModel;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Optional;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.joining;

public final class CityEditModel extends SwingEntityEditModel {

	private static final String OPENSTREETMAP_ORG_SEARCH = "https://nominatim.openstreetmap.org/search?q=";

	public CityEditModel(EntityConnectionProvider connectionProvider) {
		super(City.TYPE, connectionProvider);
		initializeComboBoxModels(City.COUNTRY_FK);
	}

	public void populateLocation() throws IOException, DatabaseException, ValidationException {
		Location location = lookupLocation(entity())
						.orElseThrow(() -> new RuntimeException("Location not found for city: " + entity()));
		put(City.LOCATION, location);
		if (modified().get()) {
			update();
		}
	}

	void populateLocation(Entity city) throws IOException, DatabaseException, ValidationException {
		lookupLocation(city).ifPresent(location -> city.put(City.LOCATION, location));
		if (city.modified()) {
			update(singletonList(city));
		}
	}

	private static Optional<Location> lookupLocation(Entity city) throws IOException {
		JSONArray jsonArray = toJSONArray(new URL(OPENSTREETMAP_ORG_SEARCH +
						URLEncoder.encode(city.get(City.NAME), UTF_8.name()) + "," +
						URLEncoder.encode(city.get(City.COUNTRY_FK).get(Country.NAME), UTF_8.name()) + "&format=json"));
		if (!jsonArray.isEmpty()) {
			JSONObject cityInformation = (JSONObject) jsonArray.get(0);

			return Optional.of(new Location(cityInformation.getDouble("lat"), cityInformation.getDouble("lon")));
		}

		return Optional.empty();
	}

	private static JSONArray toJSONArray(URL url) throws IOException {
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(url.openConnection().getInputStream(), UTF_8))) {
			return new JSONArray(reader.lines().collect(joining()));
		}
	}
}
