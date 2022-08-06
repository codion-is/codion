package is.codion.framework.demos.world.model;

import is.codion.common.db.exception.DatabaseException;
import is.codion.common.event.Event;
import is.codion.common.event.EventDataListener;
import is.codion.common.state.State;
import is.codion.common.state.StateObserver;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.demos.world.domain.api.World.City;
import is.codion.framework.demos.world.domain.api.World.Location;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.exception.ValidationException;
import is.codion.swing.common.model.worker.ProgressWorker.ProgressReporter;
import is.codion.swing.framework.model.SwingEntityTableModel;

import org.jfree.data.general.DefaultPieDataset;
import org.jfree.data.general.PieDataset;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

public final class CityTableModel extends SwingEntityTableModel {

  public static final String OPENSTREETMAP_ORG_SEARCH = "https://nominatim.openstreetmap.org/search/";

  private final DefaultPieDataset<String> chartDataset = new DefaultPieDataset<>();
  private final Event<Collection<Entity>> displayLocationEvent = Event.event();
  private final State citiesWithoutLocationSelectedState = State.state();

  CityTableModel(EntityConnectionProvider connectionProvider) {
    super(City.TYPE, connectionProvider);
    selectionModel().addSelectedItemsListener(displayLocationEvent::onEvent);
    selectionModel().addSelectionChangedListener(this::updateCitiesWithoutLocationSelected);
    addRefreshListener(this::refreshChartDataset);
  }

  public PieDataset<String> chartDataset() {
    return chartDataset;
  }

  public void addDisplayLocationListener(EventDataListener<Collection<Entity>> listener) {
    displayLocationEvent.addDataListener(listener);
  }

  public StateObserver citiesWithoutLocationSelectedObserver() {
    return citiesWithoutLocationSelectedState.observer();
  }

  public void fetchLocationForSelected(ProgressReporter<String> progressReporter,
                                       StateObserver cancelFetchLocationObserver)
          throws IOException, DatabaseException, ValidationException {
    Collection<Entity> updatedCities = new ArrayList<>();
    Collection<City> selectedCitiesWithoutLocation = selectionModel().getSelectedItems().stream()
            .filter(city -> city.isNull(City.LOCATION))
            .map(city -> city.castTo(City.class))
            .collect(toList());
    Iterator<City> citiesWithoutLocation = selectedCitiesWithoutLocation.iterator();
    while (citiesWithoutLocation.hasNext() && !cancelFetchLocationObserver.get()) {
      City city = citiesWithoutLocation.next();
      progressReporter.publish(city.country().name() + " - " + city.name());
      fetchLocation(city);
      updatedCities.add(city);
      progressReporter.setProgress(100 * updatedCities.size() / selectedCitiesWithoutLocation.size());
      displayLocationEvent.onEvent(singletonList(city));
    }
    displayLocationEvent.onEvent(selectionModel().getSelectedItems());
  }

  private void fetchLocation(City city) throws IOException, DatabaseException, ValidationException {
    JSONArray jsonArray = toJSONArray(new URL(OPENSTREETMAP_ORG_SEARCH +
            URLEncoder.encode(city.name(), UTF_8.name()) + "," +
            URLEncoder.encode(city.country().name(), UTF_8.name()) + "?format=json"));

    if (jsonArray.length() > 0) {
      fetchLocation(city, (JSONObject) jsonArray.get(0));
    }
  }

  private void fetchLocation(City city, JSONObject cityInformation) throws DatabaseException, ValidationException {
    city.location(new Location(cityInformation.getDouble("lat"), cityInformation.getDouble("lon")));
    editModel().update(singletonList(city));
  }

  private static JSONArray toJSONArray(URL url) throws IOException {
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(url.openConnection().getInputStream(), UTF_8))) {
      return new JSONArray(reader.lines().collect(joining()));
    }
  }

  private void refreshChartDataset() {
    chartDataset.clear();
    Entity.castTo(City.class, visibleItems()).forEach(city ->
            chartDataset.setValue(city.name(), city.population()));
  }

  private void updateCitiesWithoutLocationSelected() {
    citiesWithoutLocationSelectedState.set(selectionModel().getSelectedItems().stream()
            .anyMatch(city -> city.isNull(City.LOCATION)));
  }
}
