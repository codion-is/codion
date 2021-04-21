package is.codion.framework.demos.world.model;

import is.codion.common.db.exception.DatabaseException;
import is.codion.common.event.EventDataListener;
import is.codion.common.state.State;
import is.codion.common.state.StateObserver;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.demos.world.domain.api.World.City;
import is.codion.framework.demos.world.domain.api.World.Country;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.exception.ValidationException;
import is.codion.swing.framework.model.SwingEntityTableModel;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jxmapviewer.viewer.GeoPosition;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.joining;

public final class CityTableModel extends SwingEntityTableModel {

  public static final String OPENSTREETMAP_ORG_SEARCH = "https://nominatim.openstreetmap.org/search/";

  private final State locationUpdateCancelledState = State.state();

  public CityTableModel(final EntityConnectionProvider connectionProvider) {
    super(City.TYPE, connectionProvider);
  }

  public List<Entity> updateLocationForSelected(EventDataListener<Integer> progressListener)
          throws IOException, DatabaseException, ValidationException {
    List<Entity> updatedCities = new ArrayList<>();
    locationUpdateCancelledState.set(false);
    List<Entity> selectedCities = getSelectionModel().getSelectedItems();
    for (Entity city : selectedCities) {
      if (!locationUpdateCancelledState.get()) {
        updateLocation(city);
        updatedCities.add(city);
        progressListener.onEvent(100 * updatedCities.size() / selectedCities.size());
      }
    }

    return updatedCities;
  }

  public void cancelLocationUpdate() {
    locationUpdateCancelledState.set(true);
  }

  public StateObserver getLocationUpdateCancelledObserver() {
    return locationUpdateCancelledState.getObserver();
  }

  private void updateLocation(Entity city) throws IOException, DatabaseException, ValidationException {
    JSONArray jsonArray = toJSONArray(new URL(OPENSTREETMAP_ORG_SEARCH +
            URLEncoder.encode(city.get(City.NAME), UTF_8.name()) + "," +
            URLEncoder.encode(city.getForeignKey(City.COUNTRY_FK).get(Country.NAME), UTF_8.name()) + "?format=json"));

    if (jsonArray.length() > 0) {
      updateLocation(city, (JSONObject) jsonArray.get(0));
    }
  }

  private void updateLocation(Entity city, JSONObject cityInformation) throws DatabaseException, ValidationException {
    city.put(City.LOCATION, new GeoPosition(cityInformation.getDouble("lat"), cityInformation.getDouble("lon")));
    getEditModel().update(singletonList(city)).get(0);
  }

  private static JSONArray toJSONArray(URL url) throws IOException {
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(url.openConnection().getInputStream(), UTF_8))) {
      return new JSONArray(reader.lines().collect(joining()));
    }
  }
}
