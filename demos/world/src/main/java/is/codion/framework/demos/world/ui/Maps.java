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
package is.codion.framework.demos.world.ui;

import is.codion.framework.demos.world.domain.api.World.Location;

import org.jxmapviewer.JXMapKit;
import org.jxmapviewer.JXMapViewer;
import org.jxmapviewer.OSMTileFactoryInfo;
import org.jxmapviewer.viewer.DefaultTileFactory;
import org.jxmapviewer.viewer.DefaultWaypoint;
import org.jxmapviewer.viewer.GeoPosition;
import org.jxmapviewer.viewer.Waypoint;
import org.jxmapviewer.viewer.WaypointPainter;

import javax.swing.SwingUtilities;
import java.util.Set;

import static java.util.stream.Collectors.toSet;

final class Maps {

  private static final int MIN_ZOOM = 19;
  private static final int SINGLE_WAYPOINT_ZOOM_LEVEL = 15;

  private Maps() {}

  static JXMapKit createMapKit() {
    JXMapKit mapKit = new JXMapKit();
    mapKit.setTileFactory(new DefaultTileFactory(new OSMTileFactoryInfo()));
    mapKit.setMiniMapVisible(false);
    mapKit.setZoomSliderVisible(false);
    mapKit.setZoomButtonsVisible(false);
    mapKit.getMainMap().setZoom(MIN_ZOOM);
    mapKit.getMainMap().setOverlayPainter(new WaypointPainter<>());

    return mapKit;
  }

  static void paintWaypoints(Set<Location> positions, JXMapViewer mapViewer) {
    Set<GeoPosition> geoPositions = positions.stream()
            .map(location -> new GeoPosition(location.latitude(), location.longitude()))
            .collect(toSet());
    WaypointPainter<Waypoint> overlayPainter = (WaypointPainter<Waypoint>) mapViewer.getOverlayPainter();
    overlayPainter.setWaypoints(geoPositions.stream()
            .map(DefaultWaypoint::new)
            .collect(toSet()));
    SwingUtilities.invokeLater(() -> {
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
    });
  }
}
