package is.codion.framework.demos.world.ui;

import is.codion.common.event.EventDataListener;
import is.codion.framework.demos.world.domain.api.World.City;
import is.codion.framework.model.ValueChange;
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
import java.awt.Dimension;

import static is.codion.swing.common.ui.Components.setPreferredWidth;
import static is.codion.swing.common.ui.layout.Layouts.borderLayout;
import static is.codion.swing.common.ui.layout.Layouts.gridLayout;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static javax.swing.BorderFactory.createRaisedBevelBorder;

public final class CityEditPanel extends EntityEditPanel {

  public CityEditPanel(SwingEntityEditModel editModel) {
    super(editModel);
  }

  @Override
  protected void initializeUI() {
    setInitialFocusAttribute(City.COUNTRY_FK);

    setPreferredWidth(createForeignKeyComboBox(City.COUNTRY_FK), 120);
    createTextField(City.NAME).setColumns(12);
    createTextField(City.DISTRICT).setColumns(12);
    createTextField(City.POPULATION);

    JPanel inputPanel = new JPanel(gridLayout(4, 1));
    inputPanel.add(createInputPanel(City.COUNTRY_FK));
    inputPanel.add(createInputPanel(City.NAME));
    inputPanel.add(createInputPanel(City.DISTRICT));
    inputPanel.add(createInputPanel(City.POPULATION));

    JPanel inputBasePanel = new JPanel(borderLayout());
    inputBasePanel.add(inputPanel, BorderLayout.NORTH);

    setLayout(borderLayout());
    add(inputBasePanel, BorderLayout.WEST);
    add(initializeMapKit(), BorderLayout.CENTER);
  }

  private JXMapKit initializeMapKit() {
    JXMapKit mapKit = new JXMapKit();
    mapKit.setPreferredSize(new Dimension(300, 300));
    mapKit.setTileFactory(new DefaultTileFactory(new OSMTileFactoryInfo()));
    mapKit.setMiniMapVisible(false);
    mapKit.setZoomSliderVisible(false);
    mapKit.setBorder(createRaisedBevelBorder());
    mapKit.getMainMap().setZoom(14);
    mapKit.getMainMap().setOverlayPainter(new WaypointPainter<>());

    getEditModel().addValueListener(City.LOCATION, new LocationListener(mapKit.getMainMap()));

    return mapKit;
  }

  private static final class LocationListener implements EventDataListener<ValueChange<GeoPosition>> {

    private final JXMapViewer mapViewer;

    private LocationListener(final JXMapViewer mapViewer) {
      this.mapViewer = mapViewer;
    }

    @Override
    public void onEvent(final ValueChange<GeoPosition> locationChange) {
      final WaypointPainter<Waypoint> overlayPainter = (WaypointPainter<Waypoint>) mapViewer.getOverlayPainter();
      if (locationChange.getValue() != null) {
        final GeoPosition position = locationChange.getValue();
        overlayPainter.setWaypoints(singleton((new DefaultWaypoint(position.getLatitude(), position.getLongitude()))));
        mapViewer.setCenterPosition(position);
      }
      else {
        overlayPainter.setWaypoints(emptySet());
        mapViewer.repaint();
      }
    }
  }
}
