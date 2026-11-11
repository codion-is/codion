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

import javax.imageio.IIOException;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static java.awt.image.BufferedImage.TYPE_INT_RGB;
import static org.junit.jupiter.api.Assertions.*;

public final class ImageDecoderTest {

	@Test
	void decode() throws IOException {
		byte[] png = write(new BufferedImage(3, 1, TYPE_INT_RGB), "png");
		BufferedImage decoded = ImageDecoder.decode(png, false, () -> false);
		assertNotNull(decoded);
		assertEquals(3, decoded.getWidth());
		assertEquals(1, decoded.getHeight());
		// no reader for these
		assertNull(ImageDecoder.decode(new byte[] {1, 2, 3}, false, () -> false));
		// a reader, failing
		assertThrows(IIOException.class, () -> ImageDecoder.decode(new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}, false, () -> false));
	}

	@Test
	void orient() throws IOException {
		byte[] jpeg = ExifOrientationTest.withOrientation(write(new BufferedImage(4, 2, TYPE_INT_RGB), "jpg"), 6);
		BufferedImage asStored = ImageDecoder.decode(jpeg, false, () -> false);
		assertNotNull(asStored);
		assertEquals(4, asStored.getWidth());
		BufferedImage oriented = ImageDecoder.decode(jpeg, true, () -> false);
		assertNotNull(oriented);
		assertEquals(2, oriented.getWidth());
		assertEquals(4, oriented.getHeight());
	}

	@Test
	void superseded() throws IOException {
		// superseded, aborted at the first progress report, the partly decoded image discarded
		byte[] jpeg = write(new BufferedImage(1000, 1000, TYPE_INT_RGB), "jpg");
		assertNull(ImageDecoder.decode(jpeg, true, () -> true));
	}

	private static byte[] write(BufferedImage image, String format) throws IOException {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		ImageIO.write(image, format, output);

		return output.toByteArray();
	}
}
