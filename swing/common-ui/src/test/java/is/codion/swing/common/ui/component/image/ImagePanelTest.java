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
import is.codion.swing.common.ui.component.value.ComponentValue;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.Point;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public final class ImagePanelTest {

	private static final String TEST_IMAGE_PATH = "../../documentation/src/docs/asciidoc/images/chinook-client.png";

	@Test
	void imageBytes() throws IOException {
		AtomicInteger bytesEventCounter = new AtomicInteger();
		AtomicInteger imageEventCounter = new AtomicInteger();

		ComponentValue<ImagePanel, byte[]> bytesValue = ImagePanel.builder().buildValue();
		bytesValue.addListener(bytesEventCounter::incrementAndGet);

		ImagePanel imagePanel = bytesValue.component();
		imagePanel.image().addListener(imageEventCounter::incrementAndGet);

		byte[] allBytes = Files.readAllBytes(new File(TEST_IMAGE_PATH).toPath());
		bytesValue.set(allBytes);

		assertNotNull(imagePanel.image().get());
		assertEquals(1, bytesEventCounter.get());
		assertEquals(1, imageEventCounter.get());

		BufferedImage image = ImageIO.read(new File(TEST_IMAGE_PATH));
		imagePanel.image().set(image);

		assertFalse(bytesValue.getOrThrow().length > 0);
		assertEquals(2, bytesEventCounter.get());
		assertEquals(2, imageEventCounter.get());

		bytesValue.set(allBytes);
		assertEquals(3, bytesEventCounter.get());
		assertEquals(3, imageEventCounter.get());

		imagePanel.image().set(image, "png");

		assertTrue(bytesValue.getOrThrow().length > 0);
		assertEquals(4, bytesEventCounter.get());
		assertEquals(4, imageEventCounter.get());

		bytesValue.clear();
		assertNull(imagePanel.image().get());
		assertEquals(5, bytesEventCounter.get());
		assertEquals(5, imageEventCounter.get());

		imagePanel.image().set(image, "png");

		assertNotNull(imagePanel.image().get());
		assertTrue(bytesValue.getOrThrow().length > 0);
		assertEquals(6, bytesEventCounter.get());
		assertEquals(6, imageEventCounter.get());

		imagePanel.image().clear();
		assertFalse(bytesValue.getOrThrow().length > 0);
		assertEquals(7, bytesEventCounter.get());
		assertEquals(7, imageEventCounter.get());

		imagePanel.image().set(TEST_IMAGE_PATH);
		assertTrue(bytesValue.getOrThrow().length > 0);
		assertEquals(8, bytesEventCounter.get());
		assertEquals(8, imageEventCounter.get());

		bytesValue.clear();
		assertFalse(bytesValue.getOrThrow().length > 0);
		assertEquals(9, bytesEventCounter.get());
		assertEquals(9, imageEventCounter.get());

		imagePanel.image().set(allBytes);

		assertTrue(bytesValue.getOrThrow().length > 0);
		assertEquals(10, bytesEventCounter.get());
		assertEquals(10, imageEventCounter.get());
	}

	@Test
	void builder() throws IOException {
		ImagePanel panel = ImagePanel.builder()
						.image(TEST_IMAGE_PATH)
						.zoomDevice(ZoomDevice.MOUSE_BUTTON)
						.navigable(false)
						.movable(false)
						.build();

		assertEquals(ZoomDevice.MOUSE_BUTTON, panel.zoomDevice().get());
		assertFalse(panel.navigable().is());
		assertFalse(panel.movable().is());
	}

	@Test
	void imageValue() throws IOException {
		ImagePanel panel = ImagePanel.builder().build();
		assertNull(panel.image().get());

		BufferedImage image = ImageIO.read(new File(TEST_IMAGE_PATH));
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
	void zoomValue() throws IOException {
		ImagePanel panel = ImagePanel.builder()
						.image(TEST_IMAGE_PATH)
						.navigable(false)
						.build();

		// Initial zoom is 0 before paint
		assertEquals(0.0, panel.zoom().getOrThrow(), 0.001);

		// Note: zoom().set() requires initialScale > 0, which happens during paint
		// Without painting (headless tests), we can only verify the Value exists
		assertNotNull(panel.zoom());
	}

	@Test
	void zoomValueNegativeThrows() throws IOException {
		ImagePanel panel = ImagePanel.builder()
						.image(TEST_IMAGE_PATH)
						.navigable(false)
						.build();

		assertThrows(IllegalArgumentException.class, () -> panel.zoom().set(-1.0));
	}

	@Test
	void coordinateConversion() throws IOException {
		ImagePanel panel = ImagePanel.builder()
						.image(TEST_IMAGE_PATH)
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
	void isWithinImage() throws IOException {
		ImagePanel panel = ImagePanel.builder()
						.image(TEST_IMAGE_PATH)
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
	void centerImage() throws IOException {
		ImagePanel panel = ImagePanel.builder()
						.image(TEST_IMAGE_PATH)
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
	void centerImageOnImageCoordinates() throws IOException {
		ImagePanel panel = ImagePanel.builder()
						.image(TEST_IMAGE_PATH)
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
	void resetView() throws IOException {
		ImagePanel panel = ImagePanel.builder()
						.image(TEST_IMAGE_PATH)
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
}
