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

import javax.imageio.IIOException;
import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.Arrays.asList;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.*;

public final class ImagePaneTest {

	private static final String TEST_IMAGE_PATH = "../../documentation/src/docs/asciidoc/images/chinook-client.png";
	// a JPEG's start, which no JPEG reader can decode
	private static final byte[] CORRUPT = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};

	@Test
	void imageBytes() throws IOException {
		AtomicInteger bytesEventCounter = new AtomicInteger();
		AtomicInteger imageEventCounter = new AtomicInteger();

		// the component value sets the bytes on the event dispatch thread, decoded there with async disabled
		ComponentValue<ImagePane, byte[]> bytesValue = ImagePane.builder()
						.nullable(false)
						.async(false)
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
	void orient() throws IOException {
		// a JPEG stored as the sensor saw it, 4 by 2, its EXIF orientation turning it a quarter clockwise for display
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		ImageIO.write(new BufferedImage(4, 2, BufferedImage.TYPE_INT_RGB), "jpg", output);
		byte[] turned = ExifOrientationTest.withOrientation(output.toByteArray(), 6);

		ComponentValue<ImagePane, byte[]> oriented = ImagePane.builder()
						.orient(true)
						.async(false)
						.buildValue();
		oriented.set(turned);
		BufferedImage displayed = oriented.component().image().getOrThrow();
		assertEquals(2, displayed.getWidth());
		assertEquals(4, displayed.getHeight());
		// for display only, the bytes left as they are
		assertArrayEquals(turned, oriented.get());

		// as stored by default, see ORIENT
		ImagePane asStored = ImagePane.builder().build();
		asStored.image().set(turned);
		assertEquals(4, asStored.image().getOrThrow().getWidth());
		assertEquals(2, asStored.image().getOrThrow().getHeight());
	}

	@Test
	void asyncLatestWins() throws Exception {
		byte[] first = png(3, 1);
		byte[] second = png(1, 3);
		byte[] last = png(2, 2);
		ComponentValue<ImagePane, byte[]> bytesValue = ImagePane.builder()
						.image(image(Color.RED, 5, 5))
						.buildValue();
		ImagePane pane = bytesValue.component();
		List<String> images = new ArrayList<>();
		List<Boolean> decoding = new ArrayList<>();
		CountDownLatch decoded = decoded(pane);
		AtomicReference<byte[]> bytesAtOnce = new AtomicReference<>();
		AtomicReference<BufferedImage> imageAtOnce = new AtomicReference<>();
		SwingUtilities.invokeAndWait(() -> {
			pane.image().addConsumer(image -> images.add(size(image)));
			pane.decoding().addConsumer(decoding::add);
			bytesValue.set(first);
			bytesAtOnce.set(bytesValue.get());
			imageAtOnce.set(pane.image().get());
			// a burst, as when arrowing through rows, each superseding the one before
			bytesValue.set(second);
			bytesValue.set(last);
		});
		assertTrue(decoded.await(10, SECONDS));
		AtomicReference<byte[]> bytesDecoded = new AtomicReference<>();
		SwingUtilities.invokeAndWait(() -> bytesDecoded.set(bytesValue.get()));
		// the bytes the value at once, the image cleared until decoded
		assertArrayEquals(first, bytesAtOnce.get());
		assertNull(imageAtOnce.get());
		// only the last applied, decoding active throughout the burst
		assertEquals(asList("none", "2x2"), images);
		assertEquals(asList(true, false), decoding);
		assertArrayEquals(last, bytesDecoded.get());
	}

	@Test
	void asyncComponentValue() throws Exception {
		byte[] bytes = png(2, 2);
		ComponentValue<ImagePane, byte[]> bytesValue = ImagePane.builder().buildValue();
		ImagePane pane = bytesValue.component();
		CountDownLatch decoded = decoded(pane);
		// notified on the event dispatch thread, in the same event as the bytes are set, before any decode can end
		AtomicReference<BufferedImage> imageAtOnce = new AtomicReference<>(image(Color.RED, 5, 5));
		bytesValue.addListener(() -> imageAtOnce.set(pane.image().get()));
		// set from this thread, the component value sets the bytes on the event dispatch thread, decoded in the background
		bytesValue.set(bytes);
		assertNull(imageAtOnce.get());
		assertTrue(decoded.await(10, SECONDS));
		AtomicReference<BufferedImage> image = new AtomicReference<>();
		SwingUtilities.invokeAndWait(() -> image.set(pane.image().get()));
		assertEquals("2x2", size(image.get()));
		// image().set(byte[]) decodes on the calling thread, off the event dispatch thread
		pane.image().set(png(3, 1));
		assertEquals("3x1", size(pane.image().get()));
	}

	@Test
	void onImageSync() throws IOException {
		List<String> called = new ArrayList<>();
		ImagePane pane = ImagePane.builder()
						.onException(exception -> {})
						.build();
		// off the event dispatch thread, called before returning
		pane.image().set(png(2, 2), image -> called.add(size(image)));
		assertEquals(asList("2x2"), called);
		// the same bytes, the image already set
		pane.image().set(png(2, 2), image -> called.add(size(image)));
		assertEquals(asList("2x2", "2x2"), called);
		pane.image().set(TEST_IMAGE_PATH, image -> called.add("path"));
		assertEquals(asList("2x2", "2x2", "path"), called);
		// no image, failed, or none, not called
		pane.image().set(new byte[] {1, 2, 3}, image -> called.add("unreadable"));
		pane.image().set(CORRUPT, image -> called.add("corrupt"));
		pane.image().set(new byte[0], image -> called.add("empty"));
		pane.image().set((byte[]) null, image -> called.add("null"));
		assertEquals(asList("2x2", "2x2", "path"), called);
	}

	@Test
	void onImageAsync() throws Exception {
		byte[] superseded = png(3, 1);
		byte[] last = png(2, 2);
		byte[] equal = png(2, 2);
		ImagePane pane = ImagePane.builder().build();
		List<String> called = new ArrayList<>();
		List<String> calledAtOnce = new ArrayList<>();
		AtomicReference<BufferedImage> displayed = new AtomicReference<>();
		AtomicReference<Boolean> decoding = new AtomicReference<>();
		CountDownLatch decoded = decoded(pane);
		SwingUtilities.invokeAndWait(() -> {
			pane.image().set(superseded, image -> called.add("superseded"));
			pane.image().set(last, image -> {
				called.add(size(image));
				displayed.set(pane.image().get());
				decoding.set(pane.decoding().is());
			});
			// the same bytes again, while decoding them
			pane.image().set(equal, image -> called.add("again"));
			calledAtOnce.addAll(called);
		});
		assertTrue(calledAtOnce.isEmpty());
		assertTrue(decoded.await(10, SECONDS));
		SwingUtilities.invokeAndWait(() -> {});
		assertEquals(asList("2x2", "again"), called);
		// called once the image is set and decoding has ended
		assertEquals("2x2", size(displayed.get()));
		assertFalse(decoding.get());
		// the same bytes again, the image already set, called at once
		SwingUtilities.invokeAndWait(() -> pane.image().set(last, image -> called.add("set")));
		assertEquals(asList("2x2", "again", "set"), called);
	}

	@Test
	void asyncImageSetWins() throws Exception {
		byte[] superseded = png(3, 1);
		byte[] last = png(2, 2);
		ComponentValue<ImagePane, byte[]> bytesValue = ImagePane.builder().buildValue();
		ImagePane pane = bytesValue.component();
		List<String> images = new ArrayList<>();
		List<Boolean> decoding = new ArrayList<>();
		AtomicReference<byte[]> bytesAfterImage = new AtomicReference<>(new byte[0]);
		List<String> called = new ArrayList<>();
		SwingUtilities.invokeAndWait(() -> {
			pane.image().addConsumer(image -> images.add(size(image)));
			pane.decoding().addConsumer(decoding::add);
			pane.image().set(superseded, image -> called.add(size(image)));
			// an image set directly supersedes the decode under way, at once
			pane.image().set(image(Color.RED, 5, 5));
			bytesAfterImage.set(bytesValue.get());
		});
		assertEquals(asList(true, false), decoding);
		assertNull(bytesAfterImage.get());
		// started once the superseded decode ends, after which its result, discarded, would have arrived
		CountDownLatch decoded = decoded(pane);
		SwingUtilities.invokeAndWait(() -> bytesValue.set(last));
		assertTrue(decoded.await(10, SECONDS));
		SwingUtilities.invokeAndWait(() -> {});
		assertEquals(asList("5x5", "none", "2x2"), images);
		// the superseded one's callback never called
		assertTrue(called.isEmpty());
	}

	@Test
	void asyncFailure() throws Exception {
		List<Exception> handled = new ArrayList<>();
		List<Exception> observed = new ArrayList<>();
		ComponentValue<ImagePane, byte[]> bytesValue = ImagePane.builder()
						.image(image(Color.RED, 5, 5))
						.onException(handled::add)
						.buildValue();
		ImagePane pane = bytesValue.component();
		pane.exception().addConsumer(observed::add);
		CountDownLatch decoded = decoded(pane);
		SwingUtilities.invokeAndWait(() -> bytesValue.set(CORRUPT));
		assertTrue(decoded.await(10, SECONDS));
		AtomicReference<BufferedImage> image = new AtomicReference<>();
		AtomicReference<byte[]> bytes = new AtomicReference<>();
		SwingUtilities.invokeAndWait(() -> {
			image.set(pane.image().get());
			bytes.set(bytesValue.get());
		});
		assertEquals(1, handled.size());
		assertInstanceOf(IIOException.class, handled.get(0));
		assertEquals(handled, observed);
		assertNull(image.get());
		assertArrayEquals(CORRUPT, bytes.get());
	}

	@Test
	void syncFailure() throws IOException {
		ImagePane pane = ImagePane.builder()
						.image(image(Color.RED, 5, 5))
						.build();
		// rethrown by default, as it is decoded on this thread, off the event dispatch thread
		assertThrows(UncheckedIOException.class, () -> pane.image().set(CORRUPT));
		// the image cleared, rather than left displayed beside bytes it isn't
		assertNull(pane.image().get());
		assertFalse(pane.decoding().is());

		List<Exception> handled = new ArrayList<>();
		ImagePane handling = ImagePane.builder()
						.onException(handled::add)
						.build();
		handling.image().set(CORRUPT);
		assertInstanceOf(IIOException.class, handled.get(0));
		// no reader, no image, no exception
		handling.image().set(new byte[] {1, 2, 3});
		assertNull(handling.image().get());
		assertEquals(1, handled.size());
	}

	@Test
	void asyncDisabled() throws Exception {
		byte[] bytes = png(2, 2);
		ImagePane pane = ImagePane.builder()
						.async(false)
						.build();
		AtomicReference<BufferedImage> decodedAtOnce = new AtomicReference<>();
		AtomicReference<BufferedImage> clearedAtOnce = new AtomicReference<>(image(Color.RED, 5, 5));
		// decoding ends twice, on the event dispatch thread, then in the background
		CountDownLatch decoded = new CountDownLatch(2);
		pane.decoding().addConsumer(active -> {
			if (!active) {
				decoded.countDown();
			}
		});
		SwingUtilities.invokeAndWait(() -> {
			// decoded on the event dispatch thread
			pane.image().set(bytes);
			decodedAtOnce.set(pane.image().get());
			pane.image().clear();
			pane.async().set(true);
			pane.image().set(bytes);
			clearedAtOnce.set(pane.image().get());
		});
		assertNotNull(decodedAtOnce.get());
		assertNull(clearedAtOnce.get());
		assertTrue(decoded.await(10, SECONDS));
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
	void zoomNotifications() throws Exception {
		ImagePane pane = ImagePane.builder()
						.zoomDevice(ZoomDevice.MOUSE_WHEEL)
						.build();
		pane.image().set(image(Color.RED, 400, 400));
		pane.setSize(800, 250);
		SwingUtilities.invokeAndWait(() -> {});
		AtomicInteger zoomEvents = new AtomicInteger();
		pane.zoom().addListener(zoomEvents::incrementAndGet);

		pane.zoom().set(2.0);
		assertEquals(1, zoomEvents.get());
		Point origin = pane.origin().getOrThrow();
		pane.zoom().set(2.0);
		assertEquals(1, zoomEvents.get());
		assertEquals(origin, pane.origin().get());

		wheel(pane, -1, 400, 125);
		assertEquals(2, zoomEvents.get());
		wheel(pane, 0, 400, 125);
		assertEquals(2, zoomEvents.get());

		pane.reset();
		assertEquals(3, zoomEvents.get());
		assertEquals(1.0, pane.zoom().getOrThrow());
		pane.reset();
		assertEquals(3, zoomEvents.get());

		pane.zoom().set(2.0);
		pane.image().set(image(Color.GREEN, 400, 400));
		assertEquals(5, zoomEvents.get());
		assertEquals(1.0, pane.zoom().getOrThrow());
		pane.image().clear();
		assertEquals(6, zoomEvents.get());
		assertEquals(0.0, pane.zoom().getOrThrow());
	}

	@Test
	void zoomInAndOut() throws Exception {
		ImagePane pane = ImagePane.builder()
						.zoomDevice(ZoomDevice.MOUSE_WHEEL)
						.build();
		pane.image().set(image(Color.RED, 411, 397));
		pane.setSize(803, 251);
		SwingUtilities.invokeAndWait(() -> {});
		// larger than 1, zooming out used to multiply by 1 - 1.5
		pane.zoomIncrement().set(1.5);
		assertEquals(1.0, pane.zoom().getOrThrow());
		Point origin = pane.origin().getOrThrow();
		for (int i = 0; i < 3; i++) {
			wheel(pane, -1, 451, 123);
		}
		for (int i = 0; i < 3; i++) {
			wheel(pane, 1, 451, 123);
		}
		assertEquals(1.0, pane.zoom().getOrThrow(), 1e-9);
		assertEquals(origin, pane.origin().get());
		wheel(pane, 1, 451, 123);
		assertEquals(0.4, pane.zoom().getOrThrow(), 1e-9);
	}

	@Test
	void navigationImageZoom() throws Exception {
		// every column a different color
		BufferedImage image = new BufferedImage(1000, 1000, BufferedImage.TYPE_INT_RGB);
		for (int x = 0; x < image.getWidth(); x++) {
			for (int y = 0; y < image.getHeight(); y++) {
				image.setRGB(x, y, new Color(x % 256, x / 256 * 50, 128).getRGB());
			}
		}
		ImagePane pane = ImagePane.builder()
						.navigable(true)
						.zoomDevice(ZoomDevice.MOUSE_WHEEL)
						.build();
		pane.image().set(image);
		pane.setSize(800, 250);
		SwingUtilities.invokeAndWait(() -> {});
		paint(pane);
		// zooming the navigation image past the size it was created at
		for (int i = 0; i < 4; i++) {
			wheel(pane, -1, 10, 10);
		}
		// not stretched, no two adjacent columns alike
		BufferedImage canvas = paint(pane);
		for (int x = 1; x < 240; x++) {
			assertNotEquals(canvas.getRGB(x - 1, 5), canvas.getRGB(x, 5), "column " + x);
		}
		// no larger than the pane, the image being 250 pixels high, starting at x 275
		for (int i = 0; i < 20; i++) {
			wheel(pane, -1, 10, 10);
		}
		assertEquals(pane.getBackground(), new Color(paint(pane).getRGB(260, 5)));
	}

	@Test
	void navigationImageMinimumSize() throws Exception {
		ImagePane pane = ImagePane.builder()
						.navigable(true)
						.zoomDevice(ZoomDevice.MOUSE_WHEEL)
						.build();
		pane.image().set(image(Color.RED, 400, 400));
		pane.setSize(800, 250);
		SwingUtilities.invokeAndWait(() -> {});
		paint(pane);
		for (int i = 0; i < 20; i++) {
			wheel(pane, 1, 5, 5);
		}
		// still there to zoom back in
		assertEquals(Color.RED, navigationImagePixel(pane));
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
		// the builder's bytes set via the component value, on the event dispatch thread
		ImagePane panel = ImagePane.builder()
						.image(TEST_IMAGE_PATH)
						.navigable(false)
						.async(false)
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
		panel.zoom().set(null);
		assertEquals(1.0, panel.zoom().getOrThrow(), 0.001);

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
		assertThrows(IllegalArgumentException.class, () -> panel.zoom().set(0.0));
	}

	// counts down once decoding ends, the image then set
	private static CountDownLatch decoded(ImagePane pane) {
		CountDownLatch decoded = new CountDownLatch(1);
		pane.decoding().addConsumer(active -> {
			if (!active) {
				decoded.countDown();
			}
		});

		return decoded;
	}

	private static String size(BufferedImage image) {
		return image == null ? "none" : image.getWidth() + "x" + image.getHeight();
	}

	private static byte[] png(int width, int height) throws IOException {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		ImageIO.write(image(Color.BLUE, width, height), "png", output);

		return output.toByteArray();
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

	private static void wheel(ImagePane pane, int rotation, int x, int y) {
		pane.dispatchEvent(new MouseWheelEvent(pane, MouseWheelEvent.MOUSE_WHEEL, System.currentTimeMillis(), 0,
						x, y, 0, false, MouseWheelEvent.WHEEL_UNIT_SCROLL, 1, rotation));
	}

	private static BufferedImage paint(ImagePane pane) {
		BufferedImage canvas = new BufferedImage(pane.getWidth(), pane.getHeight(), BufferedImage.TYPE_INT_RGB);
		Graphics2D graphics = canvas.createGraphics();
		pane.paint(graphics);
		graphics.dispose();

		return canvas;
	}
}
