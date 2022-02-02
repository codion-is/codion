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
import java.awt.BorderLayout;
import java.util.Collection;
import java.util.Objects;
import java.util.Set;

import static is.codion.swing.common.ui.layout.Layouts.borderLayout;
import static is.codion.swing.common.ui.layout.Layouts.gridLayout;
import static java.util.stream.Collectors.toSet;
import static javax.swing.BorderFactory.createRaisedBevelBorder;

public final class CityEditPanel extends EntityEditPanel {

  private final CityTableModel tableModel;

  public CityEditPanel(SwingEntityEditModel editModel) {
    this(editModel, null);
  }

  CityEditPanel(SwingEntityEditModel editModel, final CityTableModel tableModel) {
    super(editModel);
    this.tableModel = tableModel;
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
    if (tableModel == null) {
      inputBasePanel.setLayout(borderLayout());
      inputBasePanel.add(inputPanel, BorderLayout.NORTH);
    }
    else {
      inputBasePanel.setLayout(gridLayout(1, 2));
      inputBasePanel.add(inputPanel);
      inputBasePanel.add(initializeMapKit(), BorderLayout.CENTER);
    }
    setLayout(borderLayout());
    add(inputBasePanel, BorderLayout.CENTER);
  }

  private JXMapKit initializeMapKit() {
    JXMapKit mapKit = new JXMapKit();
    mapKit.setTileFactory(new DefaultTileFactory(new OSMTileFactoryInfo()));
    mapKit.setMiniMapVisible(false);
    mapKit.setZoomSliderVisible(false);
    mapKit.setBorder(createRaisedBevelBorder());
    mapKit.getMainMap().setZoom(19);
    mapKit.getMainMap().setOverlayPainter(new WaypointPainter<>());

    tableModel.addDisplayLocationListener(new LocationListener(mapKit.getMainMap()));

    return mapKit;
  }

  private static final class LocationListener implements EventDataListener<Collection<Entity>> {

    private final JXMapViewer mapViewer;

    private LocationListener(JXMapViewer mapViewer) {
      this.mapViewer = mapViewer;
    }

    @Override
    public void onEvent(Collection<Entity> cities) {
      paintWaypoints(cities.stream()
              .map(city -> city.get(City.LOCATION))
              .filter(Objects::nonNull)
              .map(LocationListener::toGeoPosition)
              .collect(toSet()));
    }

    private void paintWaypoints(Set<GeoPosition> positions) {
      WaypointPainter<Waypoint> overlayPainter = (WaypointPainter<Waypoint>) mapViewer.getOverlayPainter();
      overlayPainter.setWaypoints(positions.stream()
              .map(position -> new DefaultWaypoint(position.getLatitude(), position.getLongitude()))
              .collect(toSet()));
      if (positions.isEmpty()) {
        mapViewer.repaint();
      }
      else if (positions.size() == 1) {
        mapViewer.setCenterPosition(positions.iterator().next());
        mapViewer.setZoom(10);
      }
      else {
        mapViewer.setZoom(1);
        mapViewer.zoomToBestFit(positions, .9);
      }
    }

    private static GeoPosition toGeoPosition(final Location location) {
      return new GeoPosition(location.latitude(), location.longitude());
    }
  }
}
