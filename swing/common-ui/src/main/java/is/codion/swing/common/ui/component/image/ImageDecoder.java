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

import org.jspecify.annotations.Nullable;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.event.IIOReadProgressListener;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.MemoryCacheImageInputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.function.BooleanSupplier;

/**
 * Decodes image bytes, as {@link ImageIO#read(java.io.InputStream)} does, with the same readers, so the same formats,
 * but abortable: a decode superseded while running stops at the reader's next progress report.
 */
final class ImageDecoder {

	private ImageDecoder() {}

	/**
	 * @param bytes the image bytes
	 * @param orient true if the image should be turned as the bytes' EXIF orientation says
	 * @param superseded true once the decode is no longer wanted, checked on the decoding thread
	 * @return the image, null where no reader can read the bytes, or the decode was superseded
	 * @throws IOException in case of an exception while decoding
	 */
	static @Nullable BufferedImage decode(byte[] bytes, boolean orient, BooleanSupplier superseded) throws IOException {
		BufferedImage image = read(bytes, superseded);
		// an aborted read returns the image partly decoded, so the flag decides, not the result
		if (image == null || superseded.getAsBoolean()) {
			return null;
		}

		return orient ? ExifOrientation.orient(image, ExifOrientation.orientation(bytes)) : image;
	}

	// ImageIO.read(InputStream) spelled out, the reader then available to abort, the bytes cached in memory, where
	// ImageIO caches a stream in a temporary file by default
	private static @Nullable BufferedImage read(byte[] bytes, BooleanSupplier superseded) throws IOException {
		try (ImageInputStream input = new MemoryCacheImageInputStream(new ByteArrayInputStream(bytes))) {
			Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
			if (!readers.hasNext()) {
				return null;
			}
			ImageReader reader = readers.next();
			try {
				reader.setInput(input, true, true);
				reader.addIIOReadProgressListener(new AbortSuperseded(superseded));

				return reader.read(0, reader.getDefaultReadParam());
			}
			finally {
				reader.dispose();
			}
		}
	}

	/**
	 * Aborts the read once superseded. A reader can only be aborted from the thread reading, the JPEG reader refusing
	 * any other, which is the thread it reports its progress on, some twenty times per image.
	 */
	private static final class AbortSuperseded implements IIOReadProgressListener {

		private final BooleanSupplier superseded;

		private AbortSuperseded(BooleanSupplier superseded) {
			this.superseded = superseded;
		}

		@Override
		public void imageProgress(ImageReader source, float percentageDone) {
			if (superseded.getAsBoolean()) {
				source.abort();
			}
		}

		@Override
		public void sequenceStarted(ImageReader source, int minIndex) {}

		@Override
		public void sequenceComplete(ImageReader source) {}

		@Override
		public void imageStarted(ImageReader source, int imageIndex) {}

		@Override
		public void imageComplete(ImageReader source) {}

		@Override
		public void thumbnailStarted(ImageReader source, int imageIndex, int thumbnailIndex) {}

		@Override
		public void thumbnailProgress(ImageReader source, float percentageDone) {}

		@Override
		public void thumbnailComplete(ImageReader source) {}

		@Override
		public void readAborted(ImageReader source) {}
	}
}
