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
 * Copyright (c) 2026, Björn Darri Sigurðsson.
 */
package is.codion.manual.javadoc;

import is.codion.swing.common.ui.component.image.ImagePane;
import is.codion.swing.common.ui.component.image.ImagePane.ZoomDevice;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * The {@link ImagePane} javadoc snippets, each the region of the same name.
 */
final class ImagePaneSnippets {

	void image(BufferedImage bufferedImage, BufferedImage newImage) {
		ImagePane pane = ImagePane.builder() // @start region=image
						.image(bufferedImage)
						.build();

		// Or change the image reactively
		pane.image().set(newImage); // @end
	}

	void zoomDevice() {
		ImagePane pane = ImagePane.builder() // @start region=zoomDevice
						.zoomDevice(ZoomDevice.MOUSE_BUTTON)
						.build();

		// Or change reactively
		pane.zoomDevice().set(ZoomDevice.NONE); // @end
	}

	void zoomIncrement(ImagePane pane) {
		pane.zoomIncrement().set(0.3); // 30% increment // @start region=zoomIncrement @end
	}

	void zoom(ImagePane pane) {
		pane.zoom().set(2.0); // Zoom to 200% // @start region=zoom @end
	}

	void autoResize() {
		ImagePane pane = ImagePane.builder() // @start region=autoResize
						.autoResize(true)
						.build();

		// Or toggle reactively
		pane.autoResize().set(true); // @end
	}

	void navigable() {
		ImagePane pane = ImagePane.builder() // @start region=navigable
						.navigable(true)
						.build();

		// Or toggle reactively
		pane.navigable().set(false); // @end
	}

	void movable(ImagePane pane) {
		pane.movable().set(false); // Disable dragging // @start region=movable @end
	}

	void origin(ImagePane pane) {
		// Move image to show a specific region // @start region=origin
		pane.origin().set(new Point(-200, -100));

		// React to origin changes
		pane.origin().addConsumer(origin ->
						System.out.println("Image origin: " + origin)); // @end
	}

	void usage(MouseEvent mouseEvent) throws IOException {
		BufferedImage image = ImageIO.read(new File("photo.jpg")); // @start region=usage

		ImagePane pane = ImagePane.builder()
						.image(image)
						.zoomDevice(ZoomDevice.MOUSE_WHEEL)
						.autoResize(true)
						.navigable(true)
						.movable(true)
						.build();

		// Coordinate translation
		Point2D.Double imagePoint = pane.coordinates().toImage(mouseEvent.getPoint());

		// Center image on a specific point
		pane.center().onImage(new Point2D.Double(500, 300));

		// Programmatic zoom
		pane.zoom().set(1.5);

		// React to zoom changes
		pane.zoom().addConsumer(zoom ->
						System.out.println("Zoom level: " + zoom)); // @end
	}

	void originUsage(ImagePane pane) {
		// Center image coordinates (500, 300) in the pane, as center().onImage() does // @start region=originUsage
		Point2D.Double panePoint = pane.coordinates().toPane(new Point2D.Double(500, 300));
		Point origin = pane.origin().getOrThrow();
		pane.origin().set(new Point(
						origin.x + pane.getWidth() / 2 - (int) panePoint.x,
						origin.y + pane.getHeight() / 2 - (int) panePoint.y));

		// React to origin changes (e.g., when user drags the image)
		pane.origin().addConsumer(this::updateVisibleRegionIndicator); // @end
	}

	void gridOverlay(BufferedImage image) {
		ImagePane imagePane = ImagePane.builder() // @start region=gridOverlay
						.image(image)
						.overlay((g2d, pane) -> {
							g2d.setColor(new Color(255, 255, 255, 100));
							// Draw grid lines every 100 image pixels
							for (int x = 0; x < image.getWidth(); x += 100) {
								Point2D.Double top = pane.coordinates().toPane(new Point2D.Double(x, 0));
								Point2D.Double bottom = pane.coordinates().toPane(
												new Point2D.Double(x, image.getHeight()));
								g2d.drawLine((int) top.x, (int) top.y, (int) bottom.x, (int) bottom.y);
							}
						})
						.build(); // @end
	}

	void regionOverlay(BufferedImage image) {
		List<Rectangle> taggedRegions = taggedRegions(); // @start region=regionOverlay

		ImagePane imagePane = ImagePane.builder()
						.image(image)
						.overlay((g2d, pane) -> {
							g2d.setColor(new Color(255, 0, 0, 128));
							for (Rectangle region : taggedRegions) {
								// Convert image coordinates to pane coordinates
								Point2D.Double topLeft = pane.coordinates().toPane(
												new Point2D.Double(region.x, region.y));
								Point2D.Double bottomRight = pane.coordinates().toPane(
												new Point2D.Double(region.x + region.width, region.y + region.height));

								int width = (int) (bottomRight.x - topLeft.x);
								int height = (int) (bottomRight.y - topLeft.y);
								g2d.fillRect((int) topLeft.x, (int) topLeft.y, width, height);
							}
						})
						.build(); // @end
	}

	private void updateVisibleRegionIndicator(Point origin) {}

	private static List<Rectangle> taggedRegions() {
		return List.of();
	}
}
