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

import is.codion.common.utilities.property.PropertyValue;
import is.codion.swing.common.ui.component.builder.AbstractComponentValueBuilder;
import is.codion.swing.common.ui.component.builder.ComponentValueBuilder;
import is.codion.swing.common.ui.component.value.AbstractComponentValue;
import is.codion.swing.common.ui.component.value.ComponentValue;

import org.jspecify.annotations.Nullable;

import javax.imageio.ImageIO;
import javax.swing.JComponent;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import static is.codion.common.utilities.Configuration.booleanValue;
import static java.awt.image.BufferedImage.TYPE_INT_ARGB;
import static java.awt.image.BufferedImage.TYPE_INT_RGB;

/**
 * <p>Displays image bytes fitted to its size, within its insets, the image's proportions kept and centred, an image
 * smaller than the view not enlarged unless {@link Builder#enlarge(boolean)}. Display only: no zooming, panning or mouse
 * handling, see {@link ImagePane} for those.
 * <p>Made for displaying images in table, list or tree cells, as a cell renderer component:
 * {@snippet class = "is.codion.demos.world.javadoc.ImageViewSnippets" region = "renderer":
 * FilterTable<Entity, Attribute<?>> table = FilterTable.builder() // @start region=renderer
 *         .model(tableModel)
 *         .cellRenderer(Country.FLAG, byte[].class, renderer -> renderer
 *                 .component(ImageView.builder().buildValue()))
 *         .build(); // @end}
 * <b>Decoding</b>
 * The bytes are decoded when painted, on the calling thread, and scaled smoothly to the size painted, in device pixels.
 * The scaled image is kept, the decoded one dropped, so a photo costs little memory once displayed. The scaled images
 * are kept by their byte array, the 200 painted most recently, so painting the same array again at the same size
 * decodes nothing, as when a table repaints its rows, each holding its bytes in the same array. So the bytes are not
 * copied: an array changed after being set keeps the image painted from it. The arrays are held weakly, a scaled image
 * dropped once nothing else holds its array, a table row refreshed away for example.
 * <p>This suits small images: icons, flags and thumbnails. A photo takes the better part of a second to decode, each
 * time its array is first painted, so a table of full size photos is better served by a column of thumbnails.
 * <p>Bytes no image reader can read, or failing to decode, display nothing.
 * <b>Painting</b>
 * Opaque by default, painting its background, so that a cell renderer's background, the selection's for example, is
 * painted, while a view with no background set takes its parent's.
 * <b>Orientation</b>
 * Where enabled, an image is displayed turned as its EXIF orientation says, as a camera records a photo taken with the
 * camera turned, see {@link Builder#orient(boolean)} and {@link #ORIENT}.
 * @see #builder()
 */
public final class ImageView extends JComponent {

	/**
	 * Specifies whether {@link ImageView}s display an image turned as its EXIF orientation says by default.
	 * <ul>
	 * <li>Value type: Boolean
	 * <li>Default value: false
	 * </ul>
	 * @see Builder#orient(boolean)
	 */
	public static final PropertyValue<Boolean> ORIENT = booleanValue(ImageView.class.getName() + ".orient", false);

	// the scaled images kept, the one painted least recently dropped first
	private static final int CACHE_SIZE = 200;

	private final boolean enlarge;
	private final boolean orient;
	// by the byte array's identity, held weakly, in access order: an entry goes once nothing else holds its array, a
	// table row refreshed away for example, or as the one painted least recently of more than CACHE_SIZE
	private final Map<Key, Scaled> cache = new LinkedHashMap<>(16, 0.75f, true);
	private final ReferenceQueue<byte[]> released = new ReferenceQueue<>();

	private byte @Nullable [] bytes;

	private ImageView(DefaultBuilder builder) {
		this.enlarge = builder.enlarge;
		this.orient = builder.orient;
		setOpaque(true);
		setFocusable(false);
	}

	/**
	 * @return a new {@link Builder} instance
	 */
	public static Builder builder() {
		return new DefaultBuilder();
	}

	@Override
	protected void paintComponent(Graphics g) {
		if (isOpaque()) {
			g.setColor(getBackground());
			g.fillRect(0, 0, getWidth(), getHeight());
		}
		byte[] imageBytes = bytes;
		Insets insets = getInsets();
		int width = getWidth() - insets.left - insets.right;
		int height = getHeight() - insets.top - insets.bottom;
		if (imageBytes == null || imageBytes.length == 0 || width <= 0 || height <= 0) {
			return;
		}
		Graphics2D graphics = (Graphics2D) g;
		AffineTransform transform = graphics.getTransform();
		double scaleX = transform.getScaleX() > 0 ? transform.getScaleX() : 1;
		double scaleY = transform.getScaleY() > 0 ? transform.getScaleY() : 1;
		BufferedImage image = scaled(imageBytes, width, height, scaleX, scaleY);
		if (image != null) {
			// centred, on a device pixel, drawn pixel for pixel
			double x = insets.left + (width - image.getWidth() / scaleX) / 2;
			double y = insets.top + (height - image.getHeight() / scaleY) / 2;
			AffineTransform imageTransform = AffineTransform.getTranslateInstance(
							(Math.round(x * scaleX + transform.getTranslateX()) - transform.getTranslateX()) / scaleX,
							(Math.round(y * scaleY + transform.getTranslateY()) - transform.getTranslateY()) / scaleY);
			imageTransform.scale(1 / scaleX, 1 / scaleY);
			graphics.drawImage(image, imageTransform, null);
		}
	}

	/**
	 * @param imageBytes the bytes
	 * @param width the width to fit, in user space
	 * @param height the height to fit, in user space
	 * @param scaleX the horizontal scale of user space to device pixels
	 * @param scaleY the vertical scale of user space to device pixels
	 * @return the image of the given bytes fitted to the given size in device pixels, the one kept where painted at that
	 * size before, null where none results
	 */
	@Nullable BufferedImage scaled(byte[] imageBytes, int width, int height, double scaleX, double scaleY) {
		purge();
		Scaled scaled = cache.get(new Key(imageBytes, null));
		if (scaled == null || !scaled.fits(width, height, scaleX, scaleY)) {
			scaled = new Scaled(width, height, scaleX, scaleY, scale(imageBytes, width, height, scaleX, scaleY));
			cache.put(new Key(imageBytes, released), scaled);
			if (cache.size() > CACHE_SIZE) {
				Iterator<Key> leastRecent = cache.keySet().iterator();
				leastRecent.next();
				leastRecent.remove();
			}
		}

		return scaled.image;
	}

	/**
	 * @return the number of scaled images kept, those of arrays no longer held elsewhere dropped
	 */
	int cached() {
		purge();

		return cache.size();
	}

	// Drops the entries of the arrays collected since
	private void purge() {
		Reference<? extends byte[]> key = released.poll();
		while (key != null) {
			cache.remove(key);
			key = released.poll();
		}
	}

	private void set(byte @Nullable [] imageBytes) {
		if (imageBytes != bytes) {
			bytes = imageBytes;
			repaint();
		}
	}

	// Decoded unturned, fitted as displayed, scaled, then turned, at the scaled size
	private @Nullable BufferedImage scale(byte[] imageBytes, int width, int height, double scaleX, double scaleY) {
		BufferedImage decoded;
		try {
			decoded = ImageDecoder.decode(imageBytes, false, () -> false);
		}
		catch (IOException | RuntimeException e) {
			// displaying nothing, as for bytes no reader can read
			return null;
		}
		if (decoded == null) {
			return null;
		}
		int orientation = orient ? ExifOrientation.orientation(imageBytes) : ExifOrientation.NORMAL;
		// orientations 5 to 8 turn the image a quarter, its width displayed as its height
		boolean quarterTurn = orientation >= 5 && orientation <= 8;
		int imageWidth = quarterTurn ? decoded.getHeight() : decoded.getWidth();
		int imageHeight = quarterTurn ? decoded.getWidth() : decoded.getHeight();
		double fit = ImageFit.scale(imageWidth, imageHeight, width, height, enlarge);
		int scaledWidth = Math.max(1, (int) Math.round(imageWidth * fit * scaleX));
		int scaledHeight = Math.max(1, (int) Math.round(imageHeight * fit * scaleY));

		return ExifOrientation.orient(quarterTurn ?
						scale(decoded, scaledHeight, scaledWidth) :
						scale(decoded, scaledWidth, scaledHeight), orientation);
	}

	/**
	 * Halves the image until within a factor of two of the given size, then scales it the rest of the way, with bilinear
	 * interpolation each step, a single step from a much larger image sampling too few of its pixels, aliasing. Enlarges
	 * in a single step.
	 */
	private static BufferedImage scale(BufferedImage image, int width, int height) {
		BufferedImage scaled = image;
		int scaledWidth = image.getWidth();
		int scaledHeight = image.getHeight();
		while (scaledWidth != width || scaledHeight != height) {
			scaledWidth = scaledWidth > width ? Math.max(width, scaledWidth / 2) : width;
			scaledHeight = scaledHeight > height ? Math.max(height, scaledHeight / 2) : height;
			scaled = draw(scaled, scaledWidth, scaledHeight);
		}

		return scaled;
	}

	private static BufferedImage draw(BufferedImage image, int width, int height) {
		BufferedImage drawn = new BufferedImage(width, height,
						image.getTransparency() == Transparency.OPAQUE ? TYPE_INT_RGB : TYPE_INT_ARGB);
		Graphics2D graphics = drawn.createGraphics();
		graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		graphics.drawImage(image, 0, 0, width, height, null);
		graphics.dispose();

		return drawn;
	}

	/**
	 * Builds an {@link ImageView}, its {@link ComponentValue} the image bytes, not copied.
	 */
	public interface Builder extends ComponentValueBuilder<ImageView, byte[], Builder> {

		/**
		 * Default false.
		 * @param enlarge true if an image smaller than the view should be enlarged to fit it, false to display it at its
		 * own size
		 * @return this builder
		 */
		Builder enlarge(boolean enlarge);

		/**
		 * Whether an image, a JPEG's, is displayed turned as its EXIF orientation says, as a camera records a photo taken
		 * with the camera turned, which {@link ImageIO} ignores. For display only: the bytes are left as they are.
		 * @param orient true if an image should be displayed as its EXIF orientation says
		 * @return this builder
		 * @see #ORIENT
		 */
		Builder orient(boolean orient);
	}

	/**
	 * A byte array held weakly, equal to another key of the same array, by identity, a cleared key only to itself, so
	 * that it can be removed once its array has been collected.
	 */
	private static final class Key extends WeakReference<byte[]> {

		private final int hash;

		private Key(byte[] bytes, @Nullable ReferenceQueue<byte[]> queue) {
			super(bytes, queue);
			this.hash = System.identityHashCode(bytes);
		}

		@Override
		public boolean equals(Object object) {
			if (this == object) {
				return true;
			}
			if (!(object instanceof Key)) {
				return false;
			}
			byte[] bytes = get();

			return bytes != null && bytes == ((Key) object).get();
		}

		@Override
		public int hashCode() {
			return hash;
		}
	}

	private static final class Scaled {

		private final int width;
		private final int height;
		private final double scaleX;
		private final double scaleY;
		private final @Nullable BufferedImage image;

		private Scaled(int width, int height, double scaleX, double scaleY, @Nullable BufferedImage image) {
			this.width = width;
			this.height = height;
			this.scaleX = scaleX;
			this.scaleY = scaleY;
			this.image = image;
		}

		private boolean fits(int width, int height, double scaleX, double scaleY) {
			return this.width == width && this.height == height
							&& Double.compare(this.scaleX, scaleX) == 0 && Double.compare(this.scaleY, scaleY) == 0;
		}
	}

	private static final class DefaultBuilder extends AbstractComponentValueBuilder<ImageView, byte[], Builder> implements Builder {

		private boolean enlarge = false;
		private boolean orient = ORIENT.getOrThrow();

		@Override
		public Builder enlarge(boolean enlarge) {
			this.enlarge = enlarge;
			return this;
		}

		@Override
		public Builder orient(boolean orient) {
			this.orient = orient;
			return this;
		}

		@Override
		protected ImageView createComponent() {
			return new ImageView(this);
		}

		@Override
		protected ComponentValue<ImageView, byte[]> createValue(ImageView component) {
			return new BytesValue(component);
		}
	}

	private static final class BytesValue extends AbstractComponentValue<ImageView, byte[]> {

		private BytesValue(ImageView component) {
			super(component);
		}

		@Override
		protected byte @Nullable [] getComponentValue() {
			return component().bytes;
		}

		@Override
		protected void setComponentValue(byte @Nullable [] value) {
			byte[] previous = component().bytes;
			component().set(value);
			if (!Arrays.equals(previous, value)) {
				notifyObserver();
			}
		}
	}
}
