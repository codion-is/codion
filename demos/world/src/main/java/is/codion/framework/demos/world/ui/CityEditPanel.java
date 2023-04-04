package is.codion.framework.demos.world.ui;

import is.codion.common.db.exception.DatabaseException;
import is.codion.common.event.EventDataListener;
import is.codion.common.state.State;
import is.codion.framework.demos.world.domain.api.World.City;
import is.codion.framework.demos.world.domain.api.World.Location;
import is.codion.framework.demos.world.model.CityEditModel;
import is.codion.framework.demos.world.model.CityTableModel;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.exception.ValidationException;
import is.codion.swing.common.ui.component.Components;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.ui.EntityEditPanel;
import is.codion.swing.framework.ui.icons.FrameworkIcons;

import org.jxmapviewer.JXMapKit;
import org.jxmapviewer.JXMapViewer;
import org.jxmapviewer.OSMTileFactoryInfo;
import org.jxmapviewer.viewer.DefaultTileFactory;
import org.jxmapviewer.viewer.DefaultWaypoint;
import org.jxmapviewer.viewer.GeoPosition;
import org.jxmapviewer.viewer.Waypoint;
import org.jxmapviewer.viewer.WaypointPainter;
import org.kordamp.ikonli.foundation.Foundation;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.io.IOException;
import java.util.Collection;
import java.util.Objects;
import java.util.Set;

import static is.codion.swing.common.ui.layout.Layouts.borderLayout;
import static is.codion.swing.common.ui.layout.Layouts.gridLayout;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toSet;

public final class CityEditPanel extends EntityEditPanel {

  private static final int MIN_ZOOM = 19;

  private final JXMapKit mapKit;
  private final EventDataListener<Collection<Entity>> displayLocationListener;

  public CityEditPanel(SwingEntityEditModel editModel) {
    super(editModel);
    this.mapKit = null;
    this.displayLocationListener = null;
  }

  CityEditPanel(CityTableModel tableModel) {
    super(tableModel.editModel());
    this.mapKit = createMapKit();
    this.displayLocationListener = new DisplayLocationListener(mapKit.getMainMap());
    tableModel.addDisplayLocationListener(displayLocationListener);
  }

  @Override
  protected void initializeUI() {
    setInitialFocusAttribute(City.COUNTRY_FK);

    createForeignKeyComboBox(City.COUNTRY_FK)
            .preferredWidth(120);
    createTextField(City.NAME);
    createTextField(City.DISTRICT);
    createTextField(City.POPULATION);

    JPanel inputPanel = Components.panel(gridLayout(0, 1))
            .add(createInputPanel(City.COUNTRY_FK))
            .add(createInputPanel(City.NAME))
            .add(createInputPanel(City.DISTRICT))
            .add(createInputPanel(City.POPULATION))
            .build();

    JPanel centerPanel = Components.panel(gridLayout(1, 0))
            .add(inputPanel)
            .build();
    if (mapKit != null) {
      centerPanel.add(mapKit);
    }
    setLayout(borderLayout());
    add(centerPanel, BorderLayout.CENTER);
  }

  @Override
  protected Controls createControlPanelControls() {
    return super.createControlPanelControls()
            .addAt(4, Control.builder(this::setLocation)
                    .enabledState(State.and(activeObserver(),
                            editModel().nullObserver(City.LOCATION),
                            editModel().entityNewObserver().reversedObserver()))
                    .smallIcon(FrameworkIcons.instance().getIcon(Foundation.MAP))
                    .build());
  }

  private void setLocation() throws ValidationException, IOException, DatabaseException {
    ((CityEditModel) editModel()).setLocation();
    displayLocationListener.onEvent(singletonList(editModel().entityCopy()));
  }

  private static JXMapKit createMapKit() {
    JXMapKit mapKit = new JXMapKit();
    mapKit.setTileFactory(new DefaultTileFactory(new OSMTileFactoryInfo()));
    mapKit.setMiniMapVisible(false);
    mapKit.setZoomSliderVisible(false);
    mapKit.setZoomButtonsVisible(false);
    mapKit.getMainMap().setZoom(MIN_ZOOM);
    mapKit.getMainMap().setOverlayPainter(new WaypointPainter<>());

    return mapKit;
  }

  private static final class DisplayLocationListener implements EventDataListener<Collection<Entity>> {

    private static final int SINGLE_WAYPOINT_ZOOM_LEVEL = 15;

    private final JXMapViewer mapViewer;

    private DisplayLocationListener(JXMapViewer mapViewer) {
      this.mapViewer = mapViewer;
    }

    @Override
    public void onEvent(Collection<Entity> cities) {
      SwingUtilities.invokeLater(() -> paintWaypoints(cities));
    }

    private void paintWaypoints(Collection<Entity> cities) {
      paintWaypoints(cities.stream()
              .map(city -> city.get(City.LOCATION))
              .filter(Objects::nonNull)
              .collect(toSet()));
    }

    private void paintWaypoints(Set<Location> positions) {
      Set<GeoPosition> geoPositions = positions.stream()
              .map(DisplayLocationListener::toGeoPosition)
              .collect(toSet());
      WaypointPainter<Waypoint> overlayPainter = (WaypointPainter<Waypoint>) mapViewer.getOverlayPainter();
      overlayPainter.setWaypoints(geoPositions.stream()
              .map(DefaultWaypoint::new)
              .collect(toSet()));
      if (geoPositions.isEmpty()) {
        mapViewer.setZoom(MIN_ZOOM);
        mapViewer.repaint();
      }
      else if (geoPositions.size() == 1) {
        mapViewer.setZoom(0);
        mapViewer.setCenterPosition(geoPositions.iterator().next());
        mapViewer.setZoom(SINGLE_WAYPOINT_ZOOM_LEVEL);
      }
      else {
        mapViewer.setZoom(0);
        mapViewer.zoomToBestFit(geoPositions, 1);
      }
    }

    private static GeoPosition toGeoPosition(Location location) {
      return new GeoPosition(location.latitude(), location.longitude());
    }
  }
}
