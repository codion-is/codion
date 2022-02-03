package is.codion.framework.demos.world.ui;

import is.codion.common.event.EventDataListener;
import is.codion.framework.demos.world.domain.api.World.City;
import is.codion.framework.demos.world.domain.api.World.Location;
import is.codion.framework.demos.world.model.CityTableModel;
import is.codion.framework.domain.entity.Entity;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.ui.EntityEditPanel;

import org.jxmapviewer.JXMapKit;
import org.jxmapviewer.JXMapViewer;
import org.jxmapviewer.OSMTileFactoryInfo;
import org.jxmapviewer.viewer.DefaultTileFactory;
import org.jxmapviewer.viewer.DefaultWaypoint;
import org.jxmapviewer.viewer.GeoPosition;
import org.jxmapviewer.viewer.Waypoint;
import org.jxmapviewer.viewer.WaypointPainter;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.util.Collection;
import java.util.Objects;
import java.util.Set;

import static is.codion.swing.common.ui.layout.Layouts.borderLayout;
import static is.codion.swing.common.ui.layout.Layouts.gridLayout;
import static java.util.stream.Collectors.toSet;
import static javax.swing.BorderFactory.createRaisedBevelBorder;

public final class CityEditPanel extends EntityEditPanel {

  private static final int MAX_ZOOM = 19;

  private final JXMapKit mapKit;

  public CityEditPanel(SwingEntityEditModel editModel) {
    this(editModel, null);
  }

  CityEditPanel(SwingEntityEditModel editModel, final CityTableModel tableModel) {
    super(editModel);
    this.mapKit = tableModel == null ? null : initializeMapKit(tableModel);
  }

  @Override
  protected void initializeUI() {
    setInitialFocusAttribute(City.COUNTRY_FK);

    createForeignKeyComboBox(City.COUNTRY_FK)
            .preferredWidth(120);
    createTextField(City.NAME);
    createTextField(City.DISTRICT);
    createTextField(City.POPULATION);

    JPanel inputPanel = new JPanel(gridLayout(0, 1));
    inputPanel.add(createInputPanel(City.COUNTRY_FK));
    inputPanel.add(createInputPanel(City.NAME));
    inputPanel.add(createInputPanel(City.DISTRICT));
    inputPanel.add(createInputPanel(City.POPULATION));

    JPanel inputBasePanel = new JPanel();
    if (mapKit == null) {
      inputBasePanel.setLayout(borderLayout());
      inputBasePanel.add(inputPanel, BorderLayout.NORTH);
    }
    else {
      inputBasePanel.setLayout(gridLayout(1, 2));
      inputBasePanel.add(inputPanel);
      inputBasePanel.add(mapKit, BorderLayout.CENTER);
    }
    setLayout(borderLayout());
    add(inputBasePanel, BorderLayout.CENTER);
  }

  private static JXMapKit initializeMapKit(final CityTableModel tableModel) {
    JXMapKit mapKit = new JXMapKit();
    mapKit.setTileFactory(new DefaultTileFactory(new OSMTileFactoryInfo()));
    mapKit.setMiniMapVisible(false);
    mapKit.setZoomSliderVisible(false);
    mapKit.setBorder(createRaisedBevelBorder());
    mapKit.getMainMap().setZoom(MAX_ZOOM);
    mapKit.getMainMap().setOverlayPainter(new WaypointPainter<>());

    tableModel.addDisplayLocationListener(new LocationListener(mapKit.getMainMap()));

    return mapKit;
  }

  private static final class LocationListener implements EventDataListener<Collection<Entity>> {

    private static final int SINGLE_WAYPOINT_ZOOM_LEVEL = 15;

    private final JXMapViewer mapViewer;

    private LocationListener(JXMapViewer mapViewer) {
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
              .map(LocationListener::toGeoPosition)
              .collect(toSet());
      WaypointPainter<Waypoint> overlayPainter = (WaypointPainter<Waypoint>) mapViewer.getOverlayPainter();
      overlayPainter.setWaypoints(geoPositions.stream()
              .map(DefaultWaypoint::new)
              .collect(toSet()));
      if (geoPositions.isEmpty()) {
        mapViewer.setZoom(MAX_ZOOM);
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

    private static GeoPosition toGeoPosition(final Location location) {
      return new GeoPosition(location.latitude(), location.longitude());
    }
  }
}
