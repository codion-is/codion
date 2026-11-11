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

import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.util.Arrays;

import static java.awt.image.BufferedImage.TYPE_INT_RGB;
import static java.nio.ByteOrder.BIG_ENDIAN;
import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

public final class ExifOrientationTest {

	private static final byte[] SOI = {(byte) 0xFF, (byte) 0xD8};
	private static final byte[] SOS = {(byte) 0xFF, (byte) 0xDA, 0, 2};

	@Test
	void orientation() {
		for (int orientation = 1; orientation <= 8; orientation++) {
			assertEquals(orientation, ExifOrientation.orientation(jpeg(app1(orientation, false))), "big endian");
			assertEquals(orientation, ExifOrientation.orientation(jpeg(app1(orientation, true))), "little endian");
		}
		// none, no JPEG, a value out of range, a header cut short
		assertEquals(ExifOrientation.NORMAL, ExifOrientation.orientation(jpeg()));
		assertEquals(ExifOrientation.NORMAL, ExifOrientation.orientation(new byte[] {(byte) 0x89, 'P', 'N', 'G'}));
		assertEquals(ExifOrientation.NORMAL, ExifOrientation.orientation(new byte[0]));
		assertEquals(ExifOrientation.NORMAL, ExifOrientation.orientation(jpeg(app1(9, false))));
		byte[] whole = jpeg(app1(6, false));
		assertEquals(ExifOrientation.NORMAL, ExifOrientation.orientation(Arrays.copyOf(whole, whole.length - SOS.length - 6)));
	}

	@Test
	void orient() {
		// each pixel holding its own stored position, so each orientation's mapping can be checked pixel by pixel
		int width = 3;
		int height = 2;
		BufferedImage stored = new BufferedImage(width, height, TYPE_INT_RGB);
		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				stored.setRGB(x, y, rgb(x, y));
			}
		}
		assertSame(stored, ExifOrientation.orient(stored, ExifOrientation.NORMAL));
		assertSame(stored, ExifOrientation.orient(stored, 9));
		for (int orientation = 2; orientation <= 8; orientation++) {
			BufferedImage displayed = ExifOrientation.orient(stored, orientation);
			boolean quarterTurn = orientation >= 5;
			assertEquals(quarterTurn ? height : width, displayed.getWidth());
			assertEquals(quarterTurn ? width : height, displayed.getHeight());
			for (int x = 0; x < displayed.getWidth(); x++) {
				for (int y = 0; y < displayed.getHeight(); y++) {
					int[] source = source(orientation, x, y, width, height);
					assertEquals(rgb(source[0], source[1]), displayed.getRGB(x, y) & 0xFFFFFF,
									"orientation " + orientation + ", displayed " + x + "," + y);
				}
			}
		}
	}

	// The stored pixel displayed at x, y, as EXIF defines each orientation
	private static int[] source(int orientation, int x, int y, int width, int height) {
		switch (orientation) {
			case 2:
				return new int[] {width - 1 - x, y};
			case 3:
				return new int[] {width - 1 - x, height - 1 - y};
			case 4:
				return new int[] {x, height - 1 - y};
			case 5:
				return new int[] {y, x};
			case 6:
				return new int[] {y, height - 1 - x};
			case 7:
				return new int[] {width - 1 - y, height - 1 - x};
			default:
				return new int[] {width - 1 - y, x};
		}
	}

	private static int rgb(int x, int y) {
		return (x * 40 + 20) << 16 | (y * 40 + 20) << 8;
	}

	/**
	 * @param jpeg JPEG bytes, starting with SOI and APP0, as ImageIO writes them
	 * @param orientation the EXIF orientation
	 * @return the bytes with an APP1 segment holding the given orientation after their APP0
	 */
	static byte[] withOrientation(byte[] jpeg, int orientation) {
		int app0Length = (jpeg[4] & 0xFF) << 8 | (jpeg[5] & 0xFF);
		int insertAt = 4 + app0Length;
		byte[] app1 = app1(orientation, false);
		byte[] result = new byte[jpeg.length + app1.length];
		System.arraycopy(jpeg, 0, result, 0, insertAt);
		System.arraycopy(app1, 0, result, insertAt, app1.length);
		System.arraycopy(jpeg, insertAt, result, insertAt + app1.length, jpeg.length - insertAt);

		return result;
	}

	// SOI, the given segments, then SOS, where the image data would start
	private static byte[] jpeg(byte[]... segments) {
		ByteBuffer buffer = ByteBuffer.allocate(SOI.length + Arrays.stream(segments).mapToInt(segment -> segment.length).sum() + SOS.length);
		buffer.put(SOI);
		for (byte[] segment : segments) {
			buffer.put(segment);
		}
		buffer.put(SOS);

		return buffer.array();
	}

	// An APP1 segment holding "Exif\0\0" and a TIFF structure, its first directory holding the orientation alone
	private static byte[] app1(int orientation, boolean littleEndian) {
		ByteBuffer tiff = ByteBuffer.allocate(26).order(littleEndian ? LITTLE_ENDIAN : BIG_ENDIAN);
		byte order = (byte) (littleEndian ? 'I' : 'M');
		tiff.put(order).put(order).putShort((short) 42).putInt(8);
		// one entry, the orientation, a SHORT, then no next directory
		tiff.putShort((short) 1);
		tiff.putShort((short) 0x0112).putShort((short) 3).putInt(1).putShort((short) orientation).putShort((short) 0);
		tiff.putInt(0);
		ByteBuffer app1 = ByteBuffer.allocate(4 + 6 + tiff.capacity()).order(BIG_ENDIAN);
		app1.putShort((short) 0xFFE1).putShort((short) (2 + 6 + tiff.capacity()));
		app1.put(new byte[] {'E', 'x', 'i', 'f', 0, 0});
		app1.put(tiff.array());

		return app1.array();
	}
}
