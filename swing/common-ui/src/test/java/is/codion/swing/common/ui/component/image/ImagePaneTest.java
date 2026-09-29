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
 * Copyright (c) 2025 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.image;

import is.codion.swing.common.ui.component.image.ImagePane.ZoomDevice;
import is.codion.swing.common.ui.component.value.ComponentValue;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public final class ImagePaneTest {

	private static final String TEST_IMAGE_PATH = "../../documentation/src/docs/asciidoc/images/chinook-client.png";

	@Test
	void imageBytes() throws IOException {
		AtomicInteger bytesEventCounter = new AtomicInteger();
		AtomicInteger imageEventCounter = new AtomicInteger();

		ComponentValue<ImagePane, byte[]> bytesValue = ImagePane.builder()
						.nullable(false)
						.buildValue();
		bytesValue.addListener(bytesEventCounter::incrementAndGet);
		assertFalse(bytesValue.optional().isPresent());

		ImagePane imagePane = bytesValue.component();
		imagePane.image().addListener(imageEventCounter::incrementAndGet);

		byte[] allBytes = Files.readAllBytes(new File(TEST_IMAGE_PATH).toPath());
		bytesValue.set(allBytes);
		assertTrue(bytesValue.optional().isPresent());

		assertNotNull(imagePane.image().get());
		assertEquals(1, bytesEventCounter.get());
		assertEquals(1, imageEventCounter.get());

		BufferedImage image = ImageIO.read(new File(TEST_IMAGE_PATH));
		imagePane.image().set(image);

		assertFalse(bytesValue.getOrThrow().length > 0);
		assertEquals(2, bytesEventCounter.get());
		assertEquals(2, imageEventCounter.get());

		bytesValue.set(allBytes);
		assertEquals(3, bytesEventCounter.get());
		assertEquals(3, imageEventCounter.get());

		imagePane.image().set(image, "png");

		assertTrue(bytesValue.getOrThrow().length > 0);
		assertEquals(4, bytesEventCounter.get());
		assertEquals(4, imageEventCounter.get());

		bytesValue.clear();
		assertFalse(bytesValue.optional().isPresent());
		assertNull(imagePane.image().get());
		assertEquals(5, bytesEventCounter.get());
		assertEquals(5, imageEventCounter.get());

		imagePane.image().set(image, "png");

		assertNotNull(imagePane.image().get());
		assertTrue(bytesValue.getOrThrow().length > 0);
		assertEquals(6, bytesEventCounter.get());
		assertEquals(6, imageEventCounter.get());

		imagePane.image().clear();
		assertFalse(bytesValue.getOrThrow().length > 0);
		assertEquals(7, bytesEventCounter.get());
		assertEquals(7, imageEventCounter.get());

		imagePane.image().set(TEST_IMAGE_PATH);
		assertTrue(bytesValue.getOrThrow().length > 0);
		assertEquals(8, bytesEventCounter.get());
		assertEquals(8, imageEventCounter.get());

		bytesValue.clear();
		assertFalse(bytesValue.getOrThrow().length > 0);
		assertEquals(9, bytesEventCounter.get());
		assertEquals(9, imageEventCounter.get());

		imagePane.image().set(allBytes);

		assertTrue(bytesValue.getOrThrow().length > 0);
		assertEquals(10, bytesEventCounter.get());
		assertEquals(10, imageEventCounter.get());

		bytesValue = ImagePane.builder().buildValue();
		assertNull(bytesValue.get());
		imagePane = bytesValue.component();
		imagePane.image().set(image, "png");
		assertNotNull(bytesValue.get());
		assertTrue(bytesValue.optional().isPresent());
		imagePane.image().set((byte[]) null);
		assertNull(bytesValue.get());
		imagePane.image().set(image, "png");
		imagePane.image().set(new byte[0]);
		assertNull(bytesValue.get());
		assertFalse(bytesValue.optional().isPresent());
	}

	@Test
	void builder() throws IOException {
		ImagePane panel = ImagePane.builder()
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
		ImagePane panel = ImagePane.builder().build();
		assertNull(panel.image().get());

		BufferedImage image = ImageIO.read(new File(TEST_IMAGE_PATH));
		panel.image().set(image);

		assertEquals(image, panel.image().get());
	}

	@Test
	void zoomDeviceValue() {
		ImagePane panel = ImagePane.builder()
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
		ImagePane panel = ImagePane.builder()
						.movable(true)
						.build();

		assertTrue(panel.movable().is());

		panel.movable().set(false);
		assertFalse(panel.movable().is());
	}

	@Test
	void navigableState() {
		ImagePane panel = ImagePane.builder()
						.navigable(true)
						.build();

		assertTrue(panel.navigable().is());

		panel.navigable().set(false);
		assertFalse(panel.navigable().is());
	}

	@Test
	void navigationImage() throws Exception {
		ImagePane pane = ImagePane.builder()
						.navigable(true)
						.build();
		pane.image().set(image(Color.RED, 400, 400));
		pane.setSize(800, 250);
		SwingUtilities.invokeAndWait(() -> {});
		assertEquals(Color.RED, navigationImagePixel(pane));
		pane.navigable().set(false);
		assertEquals(pane.getBackground(), navigationImagePixel(pane));
		// the image changes while not navigable
		pane.image().set(image(Color.GREEN, 400, 400));
		assertEquals(pane.getBackground(), navigationImagePixel(pane));
		pane.navigable().set(true);
		assertEquals(Color.GREEN, navigationImagePixel(pane));
	}

	@Test
	void navigationImageInitialImage() throws Exception {
		ImagePane pane = ImagePane.builder()
						.image(image(Color.RED, 400, 400))
						.navigable(true)
						.build();
		pane.setSize(800, 250);
		SwingUtilities.invokeAndWait(() -> {});
		assertEquals(Color.RED, navigationImagePixel(pane));
	}

	@Test
	void navigationImageTooSmall() throws Exception {
		ImagePane pane = ImagePane.builder()
						.navigable(true)
						.build();
		pane.image().set(image(Color.RED, 4000, 10));
		pane.setSize(800, 250);
		SwingUtilities.invokeAndWait(() -> {});
		assertEquals(pane.getBackground(), navigationImagePixel(pane));
		pane.image().set(image(Color.RED, 400, 400));
		pane.setSize(3, 100);
		SwingUtilities.invokeAndWait(() -> {});
		assertDoesNotThrow(() -> paint(pane));
	}

	@Test
	void centeredOnResize() throws Exception {
		ImagePane pane = ImagePane.builder().build();
		pane.image().set(image(Color.RED, 400, 400));
		pane.setSize(796, 256);
		SwingUtilities.invokeAndWait(() -> {});
		paint(pane);
		assertCentered(pane);
		pane.setSize(1200, 256);
		SwingUtilities.invokeAndWait(() -> {});
		assertCentered(pane);
		pane.setSize(600, 256);
		SwingUtilities.invokeAndWait(() -> {});
		assertCentered(pane);
		pane.setSize(600, 400);
		SwingUtilities.invokeAndWait(() -> {});
		assertCentered(pane);
	}

	@Test
	void zoomAreaOutline() throws Exception {
		ImagePane pane = ImagePane.builder()
						.navigable(true)
						.build();
		pane.image().set(image(Color.RED, 400, 400));
		pane.setSize(800, 250);
		SwingUtilities.invokeAndWait(() -> {});
		// the whole image is visible
		assertEquals(0, navigationImageWhitePixels(pane));
		pane.zoom().set(2.0);
		assertTrue(navigationImageWhitePixels(pane) > 0);
	}

	@Test
	void imageOutOfView() throws Exception {
		AtomicInteger overlayCounter = new AtomicInteger();
		ImagePane pane = ImagePane.builder()
						.navigable(true)
						.overlay((graphics, imagePane) -> overlayCounter.incrementAndGet())
						.build();
		pane.image().set(image(Color.RED, 400, 400));
		pane.setSize(800, 250);
		SwingUtilities.invokeAndWait(() -> {});
		paint(pane);
		// past 100%, rendering only the part of the image in view
		pane.zoom().set(3.0);
		pane.origin().set(new Point(-5000, -5000));
		overlayCounter.set(0);
		assertEquals(Color.RED, navigationImagePixel(pane));
		assertEquals(1, overlayCounter.get());
	}

	@Test
	void zoomIncrement() {
		ImagePane panel = ImagePane.builder().build();

		// Default value
		assertEquals(0.2, panel.zoomIncrement().getOrThrow(), 0.001);

		panel.zoomIncrement().set(0.5);
		assertEquals(0.5, panel.zoomIncrement().getOrThrow(), 0.001);
	}

	@Test
	void zoomIncrementNegativeThrows() {
		ImagePane panel = ImagePane.builder().build();

		assertThrows(IllegalArgumentException.class, () -> panel.zoomIncrement().set(-0.1));
	}

	@Test
	void zoomValue() throws Exception {
		ImagePane panel = ImagePane.builder()
						.image(TEST_IMAGE_PATH)
						.navigable(false)
						.build();

		// the pane has no size
		assertEquals(0.0, panel.zoom().getOrThrow(), 0.001);
		assertThrows(IllegalStateException.class, () -> panel.zoom().set(2.0));
		assertThrows(IllegalStateException.class, () -> panel.coordinates().toImage(new Point(0, 0)));

		panel.setSize(800, 250);
		SwingUtilities.invokeAndWait(() -> {});
		// fitted when first needed, not painted
		assertEquals(1.0, panel.zoom().getOrThrow(), 0.001);
		panel.zoom().set(2.0);
		assertEquals(2.0, panel.zoom().getOrThrow(), 0.001);

		panel.image().clear();
		assertEquals(0.0, panel.zoom().getOrThrow(), 0.001);
		assertThrows(IllegalStateException.class, () -> panel.zoom().set(2.0));
	}

	@Test
	void zoomBeforePaint() throws Exception {
		ImagePane painted = ImagePane.builder().build();
		painted.image().set(image(Color.RED, 400, 300));
		painted.setSize(800, 250);
		ImagePane notPainted = ImagePane.builder().build();
		notPainted.image().set(image(Color.RED, 400, 300));
		notPainted.setSize(800, 250);
		SwingUtilities.invokeAndWait(() -> {});

		paint(painted);
		painted.zoom().set(2.0);
		notPainted.zoom().set(2.0);

		assertEquals(painted.scale(), notPainted.scale());
		assertEquals(painted.origin().get(), notPainted.origin().get());
	}

	@Test
	void zoomValueNegativeThrows() throws IOException {
		ImagePane panel = ImagePane.builder()
						.image(TEST_IMAGE_PATH)
						.navigable(false)
						.build();

		assertThrows(IllegalArgumentException.class, () -> panel.zoom().set(-1.0));
	}

	@Test
	void readImageFromFile() throws IOException {
		ImagePane.readImage(TEST_IMAGE_PATH);
		assertThrows(IOException.class, () -> ImagePane.readImage("nonexistent.png"));
	}

	private static BufferedImage image(Color color, int width, int height) {
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		Graphics2D graphics = image.createGraphics();
		graphics.setColor(color);
		graphics.fillRect(0, 0, width, height);
		graphics.dispose();

		return image;
	}

	/**
	 * @return the color of a pixel within the navigation image, in the upper left corner,
	 * clear of the image itself, which is centered
	 */
	private static Color navigationImagePixel(ImagePane pane) {
		return new Color(paint(pane).getRGB(10, 10));
	}

	/**
	 * @return the number of white pixels in the navigation image area, the zoom area outline being white
	 */
	private static int navigationImageWhitePixels(ImagePane pane) {
		BufferedImage canvas = paint(pane);
		int whitePixels = 0;
		for (int x = 0; x < pane.getWidth() * 0.15; x++) {
			for (int y = 0; y < pane.getHeight(); y++) {
				if ((canvas.getRGB(x, y) & 0xFFFFFF) == 0xFFFFFF) {
					whitePixels++;
				}
			}
		}

		return whitePixels;
	}

	private static void assertCentered(ImagePane pane) {
		BufferedImage image = pane.image().getOrThrow();
		int screenImageWidth = (int) (pane.scale() * image.getWidth());
		int screenImageHeight = (int) (pane.scale() * image.getHeight());
		assertEquals(new Point((pane.getWidth() - screenImageWidth) / 2, (pane.getHeight() - screenImageHeight) / 2),
						pane.origin().getOrThrow(), "at " + pane.getWidth() + "x" + pane.getHeight());
	}

	private static BufferedImage paint(ImagePane pane) {
		BufferedImage canvas = new BufferedImage(pane.getWidth(), pane.getHeight(), BufferedImage.TYPE_INT_RGB);
		Graphics2D graphics = canvas.createGraphics();
		pane.paint(graphics);
		graphics.dispose();

		return canvas;
	}
}
