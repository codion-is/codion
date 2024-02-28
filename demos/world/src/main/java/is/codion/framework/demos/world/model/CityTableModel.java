/*
 * Copyright (c) 2023 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.world.model;

import is.codion.common.db.exception.DatabaseException;
import is.codion.common.event.Event;
import is.codion.common.state.State;
import is.codion.common.state.StateObserver;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.demos.world.domain.api.World.City;
import is.codion.framework.demos.world.domain.api.World.Country;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.exception.ValidationException;
import is.codion.swing.common.model.worker.ProgressWorker.ProgressReporter;
import is.codion.swing.framework.model.SwingEntityTableModel;

import org.jfree.data.general.DefaultPieDataset;
import org.jfree.data.general.PieDataset;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.function.Consumer;

import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;

public final class CityTableModel extends SwingEntityTableModel {

  private final DefaultPieDataset<String> chartDataset = new DefaultPieDataset<>();
  private final Event<Collection<Entity>> displayLocationEvent = Event.event();
  private final State citiesWithoutLocationSelected = State.state();

  CityTableModel(EntityConnectionProvider connectionProvider) {
    super(new CityEditModel(connectionProvider));
    selectionModel().addSelectedItemsListener(displayLocationEvent::accept);
    selectionModel().addSelectionListener(this::updateCitiesWithoutLocationSelected);
    refresher().addRefreshListener(this::refreshChartDataset);
  }

  public PieDataset<String> chartDataset() {
    return chartDataset;
  }

  public void addDisplayLocationListener(Consumer<Collection<Entity>> listener) {
    displayLocationEvent.addDataListener(listener);
  }

  public StateObserver citiesWithoutLocationSelected() {
    return citiesWithoutLocationSelected.observer();
  }

  public void populateLocationForSelected(ProgressReporter<String> progressReporter,
                                          StateObserver cancelPopulateLocation)
          throws IOException, DatabaseException, ValidationException {
    Collection<Entity> updatedCities = new ArrayList<>();
    Collection<Entity> selectedCitiesWithoutLocation = selectionModel().getSelectedItems().stream()
            .filter(city -> city.isNull(City.LOCATION))
            .collect(toList());
    CityEditModel editModel = editModel();
    Iterator<Entity> citiesWithoutLocation = selectedCitiesWithoutLocation.iterator();
    while (citiesWithoutLocation.hasNext() && !cancelPopulateLocation.get()) {
      Entity city = citiesWithoutLocation.next();
      progressReporter.publish(city.get(City.COUNTRY_FK).get(Country.NAME) + " - " + city.get(City.NAME));
      editModel.populateLocation(city);
      updatedCities.add(city);
      progressReporter.report(100 * updatedCities.size() / selectedCitiesWithoutLocation.size());
      displayLocationEvent.accept(singletonList(city));
    }
    displayLocationEvent.accept(selectionModel().getSelectedItems());
  }

  private void refreshChartDataset() {
    chartDataset.clear();
    visibleItems().forEach(city ->
            chartDataset.setValue(city.get(City.NAME), city.get(City.POPULATION)));
  }

  private void updateCitiesWithoutLocationSelected() {
    citiesWithoutLocationSelected.set(selectionModel().getSelectedItems().stream()
            .anyMatch(city -> city.isNull(City.LOCATION)));
  }
}
