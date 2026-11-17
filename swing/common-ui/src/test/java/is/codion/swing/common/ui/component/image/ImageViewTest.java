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
package is.codion.swing.common.ui.component.image;

import is.codion.swing.common.ui.component.value.ComponentValue;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.CellRendererPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.*;

public final class ImageViewTest {

	private static final Color BACKGROUND = Color.WHITE;

	@Test
	void renderer() throws Exception {
		List<Color> colors = asList(Color.RED, Color.GREEN, Color.BLUE);
		List<byte[]> cells = new ArrayList<>();
		for (Color color : colors) {
			cells.add(png(color, 10, 10));
		}
		ComponentValue<ImageView, byte[]> bytesValue = ImageView.builder().buildValue();
		ImageView view = bytesValue.component();
		CellRendererPane rendererPane = new CellRendererPane();
		BufferedImage canvas = new BufferedImage(20 * colors.size(), 20, BufferedImage.TYPE_INT_RGB);
		JPanel table = new JPanel();
		for (int pass = 0; pass < 2; pass++) {
			// painted as a table paints its cells, all in one event, each right after its bytes are set
			SwingUtilities.invokeAndWait(() -> {
				Graphics2D graphics = canvas.createGraphics();
				for (int cell = 0; cell < cells.size(); cell++) {
					bytesValue.set(cells.get(cell));
					view.setBackground(BACKGROUND);
					rendererPane.paintComponent(graphics, view, table, cell * 20, 0, 20, 20);
				}
				graphics.dispose();
			});
			for (int cell = 0; cell < colors.size(); cell++) {
				assertEquals(colors.get(cell), color(canvas, cell * 20 + 10, 10), "pass " + pass + ", cell " + cell);
				// not enlarged, the background around the 10x10 image
				assertEquals(BACKGROUND, color(canvas, cell * 20 + 2, 10), "pass " + pass + ", cell " + cell);
			}
		}
	}

	@Test
	void scaledKept() throws IOException {
		ImageView view = ImageView.builder().build();
		byte[] bytes = png(Color.RED, 40, 20);
		BufferedImage scaled = view.scaled(bytes, 20, 20, 1, 1);
		assertEquals("20x10", size(scaled));
		// the same array at the same size decodes nothing
		assertSame(scaled, view.scaled(bytes, 20, 20, 1, 1));
		// the same bytes in another array are decoded
		assertNotSame(scaled, view.scaled(bytes.clone(), 20, 20, 1, 1));
		// another size scales again
		assertEquals("10x5", size(view.scaled(bytes, 10, 10, 1, 1)));
		// in device pixels
		assertEquals("40x20", size(view.scaled(bytes, 20, 20, 2, 2)));
	}

	@Test
	void cacheSize() throws IOException {
		ImageView view = ImageView.builder().build();
		byte[] first = png(Color.RED, 4, 4);
		BufferedImage firstScaled = view.scaled(first, 4, 4, 1, 1);
		byte[] bytes = png(Color.GREEN, 4, 4);
		// held, as a table's rows hold their bytes, the cache holding them only weakly
		List<byte[]> rows = new ArrayList<>();
		for (int i = 0; i < 199; i++) {
			rows.add(bytes.clone());
			view.scaled(rows.get(i), 4, 4, 1, 1);
		}
		assertEquals(200, view.cached());
		// painted again, now the most recent
		assertSame(firstScaled, view.scaled(first, 4, 4, 1, 1));
		for (int i = 0; i < 200; i++) {
			rows.add(bytes.clone());
			view.scaled(rows.get(rows.size() - 1), 4, 4, 1, 1);
		}
		assertEquals(200, view.cached());
		assertNotSame(firstScaled, view.scaled(first, 4, 4, 1, 1));
	}

	@Test
	void cacheReleased() throws Exception {
		ImageView view = ImageView.builder().build();
		byte[] bytes = png(Color.RED, 4, 4);
		view.scaled(bytes, 4, 4, 1, 1);
		assertEquals(1, view.cached());
		// the row holding the bytes refreshed away
		WeakReference<byte[]> reference = new WeakReference<>(bytes);
		bytes = null;
		for (int i = 0; i < 100 && (reference.get() != null || view.cached() > 0); i++) {
			System.gc();
			Thread.sleep(10);
		}
		// held by nothing, the cache included, its entry gone
		assertNull(reference.get());
		assertEquals(0, view.cached());
	}

	@Test
	void enlarge() throws IOException {
		byte[] bytes = png(Color.RED, 10, 5);
		assertEquals("10x5", size(ImageView.builder().build().scaled(bytes, 40, 40, 1, 1)));
		assertEquals("40x20", size(ImageView.builder().enlarge(true).build().scaled(bytes, 40, 40, 1, 1)));
		// shrunk either way
		assertEquals("4x2", size(ImageView.builder().build().scaled(bytes, 4, 4, 1, 1)));
	}

	@Test
	void smooth() throws IOException {
		// one pixel wide stripes, black and white, scaled down to flat grey, where a single bilinear step, by a factor
		// other than a power of two, aliases
		BufferedImage scaled = ImageView.builder().build().scaled(png(stripes(1000, 8)), 30, 30, 1, 1);
		assertNotNull(scaled);
		for (int x = 0; x < scaled.getWidth(); x++) {
			int grey = scaled.getRGB(x, 0) & 0xFF;
			assertTrue(Math.abs(grey - 128) < 8, "x " + x + ": " + grey);
		}
	}

	@Test
	void orient() throws IOException {
		// 30x10 stored, turned a quarter clockwise, its left edge red
		BufferedImage image = new BufferedImage(30, 10, BufferedImage.TYPE_INT_RGB);
		Graphics2D graphics = image.createGraphics();
		graphics.setColor(Color.BLUE);
		graphics.fillRect(0, 0, 30, 10);
		graphics.setColor(Color.RED);
		graphics.fillRect(0, 0, 10, 10);
		graphics.dispose();
		byte[] bytes = ExifOrientationTest.withOrientation(jpeg(image), 6);
		assertEquals("30x10", size(ImageView.builder().orient(false).build().scaled(bytes, 60, 60, 1, 1)));
		BufferedImage turned = ImageView.builder().orient(true).build().scaled(bytes, 60, 60, 1, 1);
		assertEquals("10x30", size(turned));
		// the stored left edge displayed at the top
		Color top = new Color(turned.getRGB(5, 2));
		assertTrue(top.getRed() > 200 && top.getBlue() < 60, top.toString());
		// fitted as displayed, then scaled
		assertEquals("5x15", size(ImageView.builder().orient(true).build().scaled(bytes, 15, 15, 1, 1)));
	}

	@Test
	void insets() throws IOException {
		ImageView view = ImageView.builder()
						.enlarge(true)
						.background(BACKGROUND)
						.border(BorderFactory.createEmptyBorder(5, 5, 5, 5))
						.value(png(Color.RED, 10, 10))
						.build();
		view.setSize(30, 30);
		BufferedImage painted = paint(view);
		assertEquals(BACKGROUND, color(painted, 2, 15));
		assertEquals(Color.RED, color(painted, 6, 15));
		assertEquals(Color.RED, color(painted, 23, 15));
		assertEquals(BACKGROUND, color(painted, 27, 15));
	}

	@Test
	void hiDpi() throws IOException {
		// 20x20 stripes, one pixel wide, in a 10x10 view at 200%, displayed pixel for pixel, where scaling to 10x10 and
		// drawing that at 200% would display them grey
		ImageView view = ImageView.builder()
						.background(Color.RED)
						.border(BorderFactory.createEmptyBorder(2, 2, 2, 2))
						.value(png(stripes(20, 20)))
						.build();
		view.setSize(14, 14);
		BufferedImage canvas = new BufferedImage(28, 28, BufferedImage.TYPE_INT_RGB);
		Graphics2D graphics = canvas.createGraphics();
		graphics.scale(2, 2);
		view.paint(graphics);
		graphics.dispose();
		assertEquals(Color.RED, color(canvas, 3, 14));
		for (int x = 4; x < 24; x++) {
			assertEquals(x % 2 == 0 ? Color.BLACK : Color.WHITE, color(canvas, x, 14), "x " + x);
		}
		assertEquals(Color.RED, color(canvas, 24, 14));
	}

	@Test
	void opaque() {
		ImageView view = ImageView.builder()
						.background(Color.YELLOW)
						.build();
		view.setSize(10, 10);
		assertTrue(view.isOpaque());
		assertEquals(Color.YELLOW, color(paint(view), 5, 5));
		view.setOpaque(false);
		// the canvas left black
		assertEquals(Color.BLACK, color(paint(view), 5, 5));
	}

	@Test
	void nothingToDisplay() {
		ComponentValue<ImageView, byte[]> bytesValue = ImageView.builder()
						.background(BACKGROUND)
						.buildValue();
		ImageView view = bytesValue.component();
		view.setSize(10, 10);
		for (byte[] bytes : asList(null, new byte[0], new byte[] {1, 2, 3},
						// a JPEG's start, which no JPEG reader can decode
						new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF})) {
			bytesValue.set(bytes);
			assertEquals(BACKGROUND, color(paint(view), 5, 5));
		}
	}

	@Test
	void componentValue() throws IOException {
		ComponentValue<ImageView, byte[]> bytesValue = ImageView.builder().buildValue();
		AtomicInteger changes = new AtomicInteger();
		bytesValue.addListener(changes::incrementAndGet);
		byte[] bytes = png(Color.RED, 2, 2);
		bytesValue.set(bytes);
		// not copied
		assertSame(bytes, bytesValue.get());
		assertEquals(1, changes.get());
		// the same bytes in another array, held, but no change
		byte[] copy = bytes.clone();
		bytesValue.set(copy);
		assertSame(copy, bytesValue.get());
		assertEquals(1, changes.get());
		bytesValue.clear();
		assertNull(bytesValue.get());
		assertEquals(2, changes.get());
	}

	private static Color color(BufferedImage image, int x, int y) {
		return new Color(image.getRGB(x, y));
	}

	private static String size(BufferedImage image) {
		return image == null ? "none" : image.getWidth() + "x" + image.getHeight();
	}

	private static BufferedImage paint(ImageView view) {
		BufferedImage canvas = new BufferedImage(view.getWidth(), view.getHeight(), BufferedImage.TYPE_INT_RGB);
		Graphics2D graphics = canvas.createGraphics();
		view.paint(graphics);
		graphics.dispose();

		return canvas;
	}

	private static byte[] png(Color color, int width, int height) throws IOException {
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		Graphics2D graphics = image.createGraphics();
		graphics.setColor(color);
		graphics.fillRect(0, 0, width, height);
		graphics.dispose();

		return png(image);
	}

	// vertical, one pixel wide, black then white
	private static BufferedImage stripes(int width, int height) {
		BufferedImage stripes = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				stripes.setRGB(x, y, x % 2 == 0 ? 0x000000 : 0xFFFFFF);
			}
		}

		return stripes;
	}

	private static byte[] png(BufferedImage image) throws IOException {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		ImageIO.write(image, "png", output);

		return output.toByteArray();
	}

	private static byte[] jpeg(BufferedImage image) throws IOException {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		ImageIO.write(image, "jpg", output);

		return output.toByteArray();
	}
}
