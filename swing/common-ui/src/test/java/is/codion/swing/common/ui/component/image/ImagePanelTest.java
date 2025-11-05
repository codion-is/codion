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
 * Copyright (c) 2025, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.image;

import is.codion.swing.common.ui.component.image.ImagePanel.ZoomDevice;

import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;

import static java.awt.image.BufferedImage.TYPE_INT_RGB;
import static org.junit.jupiter.api.Assertions.*;

public final class ImagePanelTest {

	@Test
	void builder() {
		BufferedImage image = createTestImage(100, 100);
		ImagePanel panel = ImagePanel.builder()
						.image(image)
						.zoomDevice(ZoomDevice.MOUSE_BUTTON)
						.navigable(false)
						.movable(false)
						.build();

		assertEquals(image, panel.image().get());
		assertEquals(ZoomDevice.MOUSE_BUTTON, panel.zoomDevice().get());
		assertFalse(panel.navigable().is());
		assertFalse(panel.movable().is());
	}

	@Test
	void imageValue() {
		ImagePanel panel = ImagePanel.builder().build();
		assertNull(panel.image().get());

		BufferedImage image = createTestImage(100, 100);
		panel.image().set(image);

		assertEquals(image, panel.image().get());
	}

	@Test
	void zoomDeviceValue() {
		ImagePanel panel = ImagePanel.builder()
						.zoomDevice(ZoomDevice.MOUSE_WHEEL)
						.build();

		assertEquals(ZoomDevice.MOUSE_WHEEL, panel.zoomDevice().get());

		panel.zoomDevice().set(ZoomDevice.MOUSE_BUTTON);
		assertEquals(ZoomDevice.MOUSE_BUTTON, panel.zoomDevice().get());

		panel.zoomDevice().set(ZoomDevice.NONE);
		assertEquals(ZoomDevice.NONE, panel.zoomDevice().get());
	}

	@Test
	void movableState() {
		ImagePanel panel = ImagePanel.builder()
						.movable(true)
						.build();

		assertTrue(panel.movable().is());

		panel.movable().set(false);
		assertFalse(panel.movable().is());
	}

	@Test
	void navigableState() {
		ImagePanel panel = ImagePanel.builder()
						.navigable(true)
						.build();

		assertTrue(panel.navigable().is());

		panel.navigable().set(false);
		assertFalse(panel.navigable().is());
	}

	@Test
	void zoomIncrement() {
		ImagePanel panel = ImagePanel.builder().build();

		// Default value
		assertEquals(0.2, panel.zoomIncrement().getOrThrow(), 0.001);

		panel.zoomIncrement().set(0.5);
		assertEquals(0.5, panel.zoomIncrement().getOrThrow(), 0.001);
	}

	@Test
	void zoomIncrementNegativeThrows() {
		ImagePanel panel = ImagePanel.builder().build();

		assertThrows(IllegalArgumentException.class, () -> panel.zoomIncrement().set(-0.1));
	}

	@Test
	void zoomValue() {
		BufferedImage image = createTestImage(200, 200);
		ImagePanel panel = ImagePanel.builder()
						.image(image)
						.navigable(false)
						.build();

		// Initial zoom is 0 before paint
		assertEquals(0.0, panel.zoom().getOrThrow(), 0.001);

		// Note: zoom().set() requires initialScale > 0, which happens during paint
		// Without painting (headless tests), we can only verify the Value exists
		assertNotNull(panel.zoom());
	}

	@Test
	void zoomValueNegativeThrows() {
		BufferedImage image = createTestImage(200, 200);
		ImagePanel panel = ImagePanel.builder()
						.image(image)
						.navigable(false)
						.build();

		assertThrows(IllegalArgumentException.class, () -> panel.zoom().set(-1.0));
	}

	@Test
	void coordinateConversion() {
		BufferedImage image = createTestImage(100, 100);
		ImagePanel panel = ImagePanel.builder()
						.image(image)
						.navigable(false)
						.build();
		panel.setSize(200, 200);

		// Set zoom to enable coordinate conversion
		panel.zoom().set(1.0);

		// Panel center should map to image coordinates
		Point panelCenter = new Point(100, 100);
		Point2D.Double imagePoint = panel.toImageCoordinates(panelCenter);

		assertNotNull(imagePoint);

		// Convert back
		Point2D.Double backToPanel = panel.toPanelCoordinates(imagePoint);
		assertNotNull(backToPanel);
	}

	@Test
	void isWithinImage() {
		BufferedImage image = createTestImage(100, 100);
		ImagePanel panel = ImagePanel.builder()
						.image(image)
						.navigable(false)
						.build();

		// Without painting/initializing, coordinates aren't properly initialized
		// We can only verify the method doesn't throw
		assertFalse(panel.isWithinImage(new Point(1000, 1000)));
	}

	@Test
	void isWithinImageNoImage() {
		ImagePanel panel = ImagePanel.builder().build();

		assertFalse(panel.isWithinImage(new Point(50, 50)));
	}

	@Test
	void centerImage() {
		BufferedImage image = createTestImage(100, 100);
		ImagePanel panel = ImagePanel.builder()
						.image(image)
						.navigable(false)
						.build();
		panel.setSize(200, 200);

		// Set zoom to enable centering
		panel.zoom().set(1.0);

		// Center on specific point
		panel.centerImage(new Point(50, 50));

		// Should not throw
		assertNotNull(panel.image().get());
	}

	@Test
	void centerImageOnImageCoordinates() {
		BufferedImage image = createTestImage(100, 100);
		ImagePanel panel = ImagePanel.builder()
						.image(image)
						.navigable(false)
						.build();
		panel.setSize(200, 200);

		// Set zoom to enable centering
		panel.zoom().set(1.0);

		// Center on image coordinates
		Point2D.Double imagePoint = new Point2D.Double(25.0, 25.0);
		panel.centerImage(imagePoint);

		// Should not throw
		assertNotNull(panel.image().get());
	}

	@Test
	void resetView() {
		BufferedImage image = createTestImage(100, 100);
		ImagePanel panel = ImagePanel.builder()
						.image(image)
						.navigable(false)
						.build();

		// Initial zoom is 0
		assertEquals(0.0, panel.zoom().get(), 0.001);

		// Reset view resets internal scale to 0
		panel.resetView();

		// After reset, zoom is still 0
		assertEquals(0.0, panel.zoom().get(), 0.001);
	}

	@Test
	void readImageFromFile() {
		// This tests the static utility method
		assertThrows(Exception.class, () -> ImagePanel.readImage("nonexistent.png"));
	}

	@Test
	void componentValueIntegration() {
		BufferedImage image = createTestImage(100, 100);
		ImagePanel panel = ImagePanel.builder()
						.image(image)
						.navigable(false)
						.build();

		// ComponentValue should be linked to image() Value
		assertEquals(image, panel.image().get());
	}

	private static BufferedImage createTestImage(int width, int height) {
		BufferedImage image = new BufferedImage(width, height, TYPE_INT_RGB);
		Graphics2D g2d = image.createGraphics();
		g2d.setColor(Color.BLUE);
		g2d.fillRect(0, 0, width, height);
		g2d.setColor(Color.RED);
		g2d.fillOval(width / 4, height / 4, width / 2, height / 2);
		g2d.dispose();
		return image;
	}
}
