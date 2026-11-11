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

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;

import static java.awt.image.BufferedImage.TYPE_CUSTOM;
import static java.awt.image.BufferedImage.TYPE_INT_RGB;
import static java.nio.ByteOrder.BIG_ENDIAN;
import static java.nio.ByteOrder.LITTLE_ENDIAN;

/**
 * A JPEG's EXIF orientation, and an image turned as it says. A camera, a phone's for example, stores a photo as the
 * sensor saw it and records in the Orientation tag (0x0112) how to turn it for display, which ImageIO ignores.
 */
final class ExifOrientation {

	static final int NORMAL = 1;

	private static final int SOI = 0xFFD8;
	private static final int APP1 = 0xFFE1;
	private static final int SOS = 0xFFDA;
	private static final int ORIENTATION_TAG = 0x0112;
	private static final int IFD_ENTRY_SIZE = 12;

	private ExifOrientation() {}

	/**
	 * @param bytes the image bytes
	 * @return the EXIF orientation, 1 to 8, {@link #NORMAL} where the bytes are no JPEG, it has none, or its header
	 * can't be read
	 */
	static int orientation(byte[] bytes) {
		try {
			ByteBuffer buffer = ByteBuffer.wrap(bytes).order(BIG_ENDIAN);
			if (unsigned(buffer.getShort(0)) != SOI) {
				return NORMAL;
			}
			int offset = 2;
			// the segments before the image data, each a marker followed by its length, which includes the length bytes
			while (offset + 4 <= bytes.length) {
				int marker = unsigned(buffer.getShort(offset));
				int length = unsigned(buffer.getShort(offset + 2));
				if (marker == APP1 && exif(bytes, offset + 4)) {
					// the TIFF structure follows "Exif\0\0"
					return orientation(ByteBuffer.wrap(bytes, offset + 10, length - 8).slice());
				}
				if (marker == SOS || (marker & 0xFF00) != 0xFF00) {
					return NORMAL;
				}
				offset += 2 + length;
			}

			return NORMAL;
		}
		catch (IndexOutOfBoundsException e) {
			return NORMAL;
		}
	}

	/**
	 * @param image the image as stored
	 * @param orientation the EXIF orientation
	 * @return the image as it should be displayed, the given one where the orientation is {@link #NORMAL} or unknown
	 */
	static BufferedImage orient(BufferedImage image, int orientation) {
		if (orientation <= NORMAL || orientation > 8) {
			return image;
		}
		int width = image.getWidth();
		int height = image.getHeight();
		// the transforms apply last to first, each mapping a stored pixel (x, y) to where it is displayed
		AffineTransform transform = new AffineTransform();
		switch (orientation) {
			case 2: // mirrored: (w - x, y)
				transform.scale(-1, 1);
				transform.translate(-width, 0);
				break;
			case 3: // upside down: (w - x, h - y)
				transform.translate(width, height);
				transform.rotate(Math.PI);
				break;
			case 4: // mirrored upside down: (x, h - y)
				transform.scale(1, -1);
				transform.translate(0, -height);
				break;
			case 5: // transposed: (y, x)
				transform.rotate(Math.PI / 2);
				transform.scale(1, -1);
				break;
			case 6: // turned a quarter clockwise: (h - y, x)
				transform.translate(height, 0);
				transform.rotate(Math.PI / 2);
				break;
			case 7: // transversed: (h - y, w - x)
				transform.translate(height, width);
				transform.rotate(Math.PI / 2);
				transform.scale(-1, 1);
				break;
			default: // 8, turned a quarter counterclockwise: (y, w - x)
				transform.translate(0, width);
				transform.rotate(3 * Math.PI / 2);
				break;
		}
		boolean quarterTurn = orientation >= 5;
		BufferedImage oriented = new BufferedImage(quarterTurn ? height : width, quarterTurn ? width : height,
						image.getType() == TYPE_CUSTOM ? TYPE_INT_RGB : image.getType());
		Graphics2D graphics = oriented.createGraphics();
		graphics.drawImage(image, transform, null);
		graphics.dispose();

		return oriented;
	}

	private static boolean exif(byte[] bytes, int offset) {
		return offset + 6 <= bytes.length
						&& bytes[offset] == 'E' && bytes[offset + 1] == 'x' && bytes[offset + 2] == 'i' && bytes[offset + 3] == 'f'
						&& bytes[offset + 4] == 0 && bytes[offset + 5] == 0;
	}

	// The orientation in the first image file directory of the given TIFF structure, which starts with its byte order,
	// "II", little endian, or "MM", big endian
	private static int orientation(ByteBuffer tiff) {
		tiff.order(tiff.get(0) == 'I' ? LITTLE_ENDIAN : BIG_ENDIAN);
		int directory = tiff.getInt(4);
		int entries = unsigned(tiff.getShort(directory));
		for (int i = 0; i < entries; i++) {
			int entry = directory + 2 + i * IFD_ENTRY_SIZE;
			if (unsigned(tiff.getShort(entry)) == ORIENTATION_TAG) {
				// a SHORT, its value in the first two bytes of the value field
				int orientation = unsigned(tiff.getShort(entry + 8));

				return orientation >= NORMAL && orientation <= 8 ? orientation : NORMAL;
			}
		}

		return NORMAL;
	}

	private static int unsigned(short value) {
		return value & 0xFFFF;
	}
}
