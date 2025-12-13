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
 * Copyright (c) Slav Boleslawski.
 */
package is.codion.swing.common.ui.component.image;

import is.codion.common.reactive.state.State;
import is.codion.common.reactive.value.AbstractValue;
import is.codion.common.reactive.value.Value;
import is.codion.common.reactive.value.Value.Validator;
import is.codion.common.utilities.property.PropertyValue;
import is.codion.swing.common.ui.component.builder.AbstractComponentValueBuilder;
import is.codion.swing.common.ui.component.builder.ComponentValueBuilder;
import is.codion.swing.common.ui.component.value.AbstractComponentValue;
import is.codion.swing.common.ui.component.value.ComponentValue;

import org.jspecify.annotations.Nullable;

import javax.imageio.ImageIO;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.function.BiConsumer;

import static is.codion.common.utilities.Configuration.booleanValue;
import static is.codion.common.utilities.Configuration.enumValue;
import static java.nio.file.Files.readAllBytes;
import static java.util.Arrays.asList;
import static java.util.Arrays.copyOf;
import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;
import static java.util.ResourceBundle.getBundle;
import static javax.swing.SwingUtilities.isLeftMouseButton;

/**
 * {@code ImagePane} is a lightweight container displaying an image that can be zoomed in and out
 * and panned with ease and simplicity, using adaptive rendering for high quality display and satisfactory performance.
 * <p>
 * All configuration is done via an Observable/Value-based API, allowing reactive UI updates.
 * <b>Image</b>
 * <p>An image is loaded via the builder or controlled via the {@link #image()} Value:</p>
 * <pre>
 * ImagePane pane = ImagePane.builder()
 *     .image(bufferedImage)
 *     .build();
 *
 * // Or change the image reactively
 * pane.image().set(newImage);
 * </pre>
 * When an image is set, it is initially painted centered in the component at the largest possible size,
 * fully visible, with its aspect ratio preserved. This is defined as 100% of the image size and
 * its corresponding zoom level is 1.0.
 * <b>Zooming</b>
 * Zooming can be controlled interactively using either the mouse scroll wheel (default) or mouse buttons,
 * or programmatically via the {@link #zoom()} Value. To change the zoom device:
 * <pre>
 * ImagePane pane = ImagePane.builder()
 *     .zoomDevice(ZoomDevice.MOUSE_BUTTON)
 *     .build();
 *
 * // Or change reactively
 * pane.zoomDevice().set(ZoomDevice.NONE);
 * </pre>
 * When using {@code ZoomDevice.MOUSE_BUTTON}, the left mouse button toggles between zooming in and out modes,
 * and the right button zooms by one increment (default 20%). The zoom increment can be controlled:
 * <pre>
 * pane.zoomIncrement().set(0.3); // 30% increment
 * </pre>
 * For programmatic zoom control, set the zoom device to {@code ZoomDevice.NONE} and use the {@link #zoom()} Value:
 * <pre>
 * pane.zoom().set(2.0); // Zoom to 200%
 * </pre>
 * Mouse wheel zooming is always around the point the mouse pointer is currently at, ensuring that
 * the area being zoomed into remains visible. Programmatic zooming via {@code zoom().set()} zooms
 * around the center of the pane.
 * <p>
 * There are no lower or upper zoom level limits.
 * <b>Auto-Resize</b>
 * When auto-resize is enabled, the image automatically re-fits to the pane whenever the pane is resized:
 * <pre>
 * ImagePane pane = ImagePane.builder()
 *     .autoResize(true)
 *     .build();
 *
 * // Or toggle reactively
 * pane.autoResize().set(true);
 * </pre>
 * When auto-resize is enabled, the image will reset to fit the pane dimensions on every resize event,
 * regardless of the current zoom level. This is useful for responsive layouts where you want the image
 * to always fill the available space.
 * <b>Navigation</b>
 * {@code ImagePane} does not use scroll bars for navigation, but can optionally display a navigation image
 * in the upper left corner. The navigation image is a small replica of the main image. Clicking on any point
 * of the navigation image displays that part of the image in the pane, centered. The navigation image can
 * be enabled/disabled via the {@link #navigable()} State:
 * <pre>
 * ImagePane pane = ImagePane.builder()
 *     .navigable(true)
 *     .build();
 *
 * // Or toggle reactively
 * pane.navigable().set(false);
 * </pre>
 * The image can be dragged with the left mouse button when {@link #movable()} is enabled (default):
 * <pre>
 * pane.movable().set(false); // Disable dragging
 * </pre>
 * For programmatic navigation, use {@link #center()}.
 * <b>Coordinate Conversion</b>
 * The pane provides coordinate translation between pane and image coordinate spaces via {@link #coordinates()}.
 * Use this for overlay drawing or when working with mouse events on the image.
 * <b>Rendering</b>
 * {@code ImagePane} uses Nearest Neighbor interpolation for image rendering (default in Java).
 * When the scaled image becomes larger than the original image, Bilinear interpolation is applied,
 * but only to the part of the image displayed in the pane.
 * <b>Custom Overlays</b>
 * The pane supports custom overlay painting for drawing annotations, grids, highlights, or any custom
 * graphics on top of the image. See {@link Builder#overlay(BiConsumer)} for details.
 * <p>
 * The {@link #origin()} Value provides access to the current image origin (top-left corner position in pane coordinates),
 * which can be used to programmatically position the image to make specific regions visible:
 * <pre>
 * // Move image to show a specific region
 * pane.origin().set(new Point(-200, -100));
 *
 * // React to origin changes
 * pane.origin().addConsumer(origin -&gt;
 *     System.out.println("Image origin: " + origin));
 * </pre>
 * <b>Example Usage</b>
 * <pre>
 * BufferedImage image = ImageIO.read(new File("photo.jpg"));
 *
 * ImagePane imagePane = ImagePane.builder()
 *     .image(image)
 *     .zoomDevice(ZoomDevice.MOUSE_WHEEL)
 *     .autoResize(true)
 *     .navigable(true)
 *     .movable(true)
 *     .build();
 *
 * // Coordinate translation
 * Point2D.Double imagePoint = pane.coordinates().toImage(mouseEvent.getPoint());
 *
 * // Center image on a specific point
 * pane.center().onImage(new Point2D.Double(500, 300));
 *
 * // Programmatic zoom
 * pane.zoom().set(1.5);
 *
 * // React to zoom changes
 * pane.zoom().addConsumer(zoom -&gt;
 *     System.out.println("Zoom level: " + zoom));
 * </pre>
 * <p>
 * Originally based on <a href="http://today.java.net/pub/a/today/2007/03/27/navigable-image-pane.html">http://today.java.net/pub/a/today/2007/03/27/navigable-image-pane.html</a>
 * Included with express permission from the author, 2019.
 * @author Slav Boleslawski
 * @author Björn Darri Sigurðsson
 */
public final class ImagePane extends JPanel {

	/**
	 * Specifies the default {@link ZoomDevice} for {@link ImagePane}s.
	 * <ul>
	 * <li>Value type: {@link ZoomDevice}
	 * <li>Default value: {@link ZoomDevice#NONE}
	 * </ul>
	 */
	public static final PropertyValue<ZoomDevice> ZOOM_DEVICE = enumValue(ImagePane.class.getName() + ".zoomDevice", ZoomDevice.class, ZoomDevice.NONE);

	/**
	 * Specifies the default auto-resize behaviour for {@link ImagePane}s.
	 * <ul>
	 * <li>Value type: Boolean
	 * <li>Default value: false
	 * </ul>
	 */
	public static final PropertyValue<Boolean> AUTO_RESIZE = booleanValue(ImagePane.class.getName() + ".autoResize", false);

	private static final ResourceBundle MESSAGES = getBundle(ImagePane.class.getName());
	private static final byte[] EMPTY_BYTES = new byte[0];

	private static final double SCREEN_NAV_IMAGE_FACTOR = 0.15; // 15% of pane's width
	private static final double NAV_IMAGE_FACTOR = 0.3; // 30% of pane's width
	private static final double HIGH_QUALITY_RENDERING_SCALE_THRESHOLD = 1;
	private static final double DEFAULT_ZOOM_INCREMENT = 0.2;
	private static final Object INTERPOLATION_TYPE = RenderingHints.VALUE_INTERPOLATION_BILINEAR;
	private static final Validator<BufferedImage> IMAGE_VALIDATOR = new ImageValidator();
	private static final Validator<? super Double> POSITIVE_NUMBER = value -> {
		if (value != null && value < 0) {
			throw new IllegalArgumentException("Value must be a positive number");
		}
	};

	private final List<BiConsumer<Graphics2D, ImagePane>> overlays;
	private final DefaultImageValue image;
	private final Value<ZoomDevice> zoomDevice;
	private final State autoResize;
	private final State movable;
	private final State navigable;
	private final Value<Double> zoomIncrement = Value.builder()
					.nonNull(DEFAULT_ZOOM_INCREMENT)
					.validator(POSITIVE_NUMBER)
					.build();
	private final ImageOriginValue origin;
	private final ZoomValue zoom;
	private final CoordinateTranslator coordinates = new CoordinateTranslator();
	private final CenterImage centerImage = new CenterImage();

	private double zoomFactor = 1 + zoomIncrement.getOrThrow();
	private double navZoomFactor = 1 + zoomIncrement.getOrThrow();
	private @Nullable BufferedImage navigationImage;
	private int navigationImageWidth;
	private int navigationImageHeight;
	private double initialScale = 0;
	private double scale = 0;
	private double navigationScale = 0;
	private Dimension previousPaneSize = getSize();

	private ImagePane(DefaultBuilder builder) {
		addComponentListener(new ImageComponentAdapter());
		addMouseListener(new ImageMouseAdapter());
		addMouseMotionListener(new ImageMouseMotionListener());
		overlays = builder.overlays.isEmpty() ? emptyList() : new ArrayList<>(builder.overlays);
		image = new DefaultImageValue(builder.image);
		zoomDevice = new ZoomDeviceValue(builder.zoomDevice);
		autoResize = State.state(builder.autoResize);
		movable = State.state(builder.movable);
		navigable = State.state(builder.navigable);
		zoom = new ZoomValue();
		origin = new ImageOriginValue();
		if (navigable.is() && !image.isNull()) {
			createNavigationImage();
		}
	}

	/**
	 * Note that setting the image via this value without specifying the format does not populate the associated
	 * byte[] {@link ComponentValue}, for that to happen you must use {@link ImageValue#set(BufferedImage, String)}
	 * @return the {@link ImageValue} controlling the image
	 */
	public ImageValue image() {
		return image;
	}

	/**
	 * @return the {@link Value} controlling the active {@link ZoomDevice}
	 */
	public Value<ZoomDevice> zoomDevice() {
		return zoomDevice;
	}

	/**
	 * Returns the {@link State} controlling whether the image automatically re-fits to the pane on resize.
	 * When enabled, the image will reset to fit the pane dimensions whenever the pane is resized,
	 * regardless of the current zoom level.
	 * @return the {@link State} controlling the auto-resize behavior
	 */
	public State autoResize() {
		return autoResize;
	}

	/**
	 * @return the {@link State} controlling whether the image is movable within the pane
	 */
	public State movable() {
		return movable;
	}

	/**
	 * @return the {@link State} controlling whether the image is navigable via a navigation image
	 */
	public State navigable() {
		return navigable;
	}

	/**
	 * @return the {@link Value} controlling the zoom increment
	 */
	public Value<Double> zoomIncrement() {
		return zoomIncrement;
	}

	/**
	 * @return the {@link Value} controlling the current zoom level
	 */
	public Value<Double> zoom() {
		return zoom;
	}

	/**
	 * Returns the {@link Value} controlling the image origin (the position of the image's top-left corner
	 * in pane coordinates). This can be used to programmatically position the image within the pane.
	 * <p>
	 * The origin is typically negative when the image is zoomed in and larger than the pane,
	 * representing how much of the image is scrolled off the top-left edge of the pane.
	 * <b>Example Usage</b>
	 * <pre>
	 * // Move image to show a region at image coordinates (500, 300)
	 * Point2D.Double imagePoint = new Point2D.Double(500, 300);
	 * Point2D.Double panePoint = pane.toPaneCoordinates(imagePoint);
	 *
	 * // Center that point in the pane
	 * int centerX = pane.getWidth() / 2;
	 * int centerY = pane.getHeight() / 2;
	 * pane.origin().set(new Point(
	 *     centerX - (int) panePoint.x,
	 *     centerY - (int) panePoint.y));
	 *
	 * // React to origin changes (e.g., when user drags the image)
	 * pane.origin().addConsumer(origin -&gt;
	 *     updateVisibleRegionIndicator(origin));
	 * </pre>
	 * @return the {@link Value} controlling the image origin in pane coordinates
	 */
	public Value<Point> origin() {
		return origin;
	}

	/**
	 * @return the current scale
	 */
	public double scale() {
		return scale;
	}

	/**
	 * Resets the view so the image is centered and fits the pane
	 */
	public void reset() {
		scale = 0.0;
		repaint();
	}

	/**
	 * @return the coordinate translator
	 */
	public CoordinateTranslator coordinates() {
		return coordinates;
	}

	/**
	 * @return image centerer
	 */
	public CenterImage center() {
		return centerImage;
	}

	/**
	 * Reads an image from the given path
	 * @param imagePath the path, either file or http
	 * @return the loaded image
	 * @throws IOException in case of an exception
	 */
	public static BufferedImage readImage(String imagePath) throws IOException {
		if (imagePath.toLowerCase().startsWith("http")) {
			return ImageIO.read(URI.create(imagePath).toURL());
		}
		else {
			File imageFile = new File(imagePath);
			if (!imageFile.exists()) {
				throw new FileNotFoundException(MESSAGES.getString("file_not_found") + ": " + imagePath);
			}

			return ImageIO.read(imageFile);
		}
	}

	/**
	 * @return a new {@link Builder} instance
	 */
	public static Builder builder() {
		return new DefaultBuilder();
	}

	/**
	 * Controls the image displayed in an {@link ImagePane}
	 */
	public interface ImageValue extends Value<BufferedImage> {

		/**
		 * @param imageBytes the image bytes
		 */
		void set(byte[] imageBytes);

		/**
		 * @param imagePath the path from which to load the image
		 * @throws IOException in case image loading failed
		 */
		void set(String imagePath) throws IOException;

		/**
		 * @param image the image
		 * @param format the format
		 * @throws IOException in case of an exception
		 * @throws IllegalArgumentException in case no writer was found for the given format
		 */
		void set(BufferedImage image, String format) throws IOException;
	}

	/**
	 * Builds an {@link ImagePane}
	 */
	public interface Builder extends ComponentValueBuilder<ImagePane, byte[], Builder> {

		/**
		 * Clears any image set via {@link #image(BufferedImage)}.
		 * @param imagePath the path to the image to initially display
		 * @return this builder
		 */
		Builder image(String imagePath) throws IOException;

		/**
		 * Clears any value set via {@link #value(Object)}.
		 * @param image the image to initially display
		 * @return this builder
		 */
		Builder image(BufferedImage image);


		/**
		 * Clears any image set via {@link #image(BufferedImage)}.
		 * @param image the image to initially display
		 * @param format the format name
		 * @return this builder
		 * @throws IllegalArgumentException in case no image writer was found for the given format
		 */
		Builder image(BufferedImage image, String format) throws IOException;

		/**
		 * Sets the overlay painter that will be called after the image is painted but before the navigation image.
		 * This allows custom graphics to be drawn on top of the image, such as annotations, grids, highlights,
		 * or selection markers.
		 * <p>
		 * The overlay painter receives the Graphics2D context for drawing and the ImagePane for accessing
		 * coordinate conversion methods and pane state.
		 * <b>Example - Drawing Grid Lines</b>
		 * <pre>
		 * ImagePane imagePane = ImagePane.builder()
		 *     .image(image)
		 *     .overlay((g2d, pane) -&gt; {
		 *         g2d.setColor(new Color(255, 255, 255, 100));
		 *         // Draw grid lines every 100 image pixels
		 *         for (int x = 0; x &lt; image.getWidth(); x += 100) {
		 *             Point2D.Double top = pane.toPaneCoordinates(new Point2D.Double(x, 0));
		 *             Point2D.Double bottom = pane.toPaneCoordinates(
		 *                 new Point2D.Double(x, image.getHeight()));
		 *             g2d.drawLine((int) top.x, (int) top.y, (int) bottom.x, (int) bottom.y);
		 *         }
		 *     })
		 *     .build();
		 * </pre>
		 * <b>Example - Highlighting Regions</b>
		 * <pre>
		 * List&lt;Rectangle&gt; taggedRegions = getTaggedRegions();
		 *
		 * ImagePane imagePane = ImagePane.builder()
		 *     .image(image)
		 *     .overlay((g2d, pane) -&gt; {
		 *         g2d.setColor(new Color(255, 0, 0, 128));
		 *         for (Rectangle region : taggedRegions) {
		 *             // Convert image coordinates to pane coordinates
		 *             Point2D.Double topLeft = pane.toPaneCoordinates(
		 *                 new Point2D.Double(region.x, region.y));
		 *             Point2D.Double bottomRight = pane.toPaneCoordinates(
		 *                 new Point2D.Double(region.x + region.width, region.y + region.height));
		 *
		 *             int width = (int) (bottomRight.x - topLeft.x);
		 *             int height = (int) (bottomRight.y - topLeft.y);
		 *             g2d.fillRect((int) topLeft.x, (int) topLeft.y, width, height);
		 *         }
		 *     })
		 *     .build();
		 * </pre>
		 * The overlay is redrawn automatically whenever the pane repaints (e.g., when zooming, panning, or resizing).
		 * @param overlay the overlay painter, receives Graphics2D for drawing and ImagePane for coordinate conversion
		 * @return this builder
		 */
		Builder overlay(BiConsumer<Graphics2D, ImagePane> overlay);

		/**
		 * @param zoomDevice the initial zoom device
		 * @return this builder
		 * @see #ZOOM_DEVICE
		 */
		Builder zoomDevice(ZoomDevice zoomDevice);

		/**
		 * Sets whether the image should automatically re-fit to the pane when the pane is resized.
		 * When enabled, the image resets to fit the pane dimensions on every resize event,
		 * regardless of the current zoom level.
		 * @param autoResize true to enable auto-resize
		 * @return this builder
		 * @see #AUTO_RESIZE
		 */
		Builder autoResize(boolean autoResize);

		/**
		 * Default false
		 * @param navigable true if the image should be navigable with a navigation image
		 * @return this builder
		 */
		Builder navigable(boolean navigable);

		/**
		 * Default false
		 * @param movable true if the image should be movable within the pane
		 * @return this builder
		 */
		Builder movable(boolean movable);
	}

	/**
	 * Provides coordinate translations.
	 */
	public final class CoordinateTranslator {

		private CoordinateTranslator() {}

		/**
		 * Converts this pane's point into the original image coordinates
		 * @param paneCoordinate the pane coordinates
		 * @return the image coordinates
		 */
		public Point2D.Double toImage(Point paneCoordinate) {
			requireNonNull(paneCoordinate);
			if (Double.compare(scale, 0) == 0) {
				throw new IllegalStateException("Cannot translate coordinates before image is displayed");
			}
			return new Point2D.Double((paneCoordinate.x - origin.x) / scale, (paneCoordinate.y - origin.y) / scale);
		}

		/**
		 * Converts the original image point into this pane's coordinates
		 * @param imageCoordinate the image coordinates
		 * @return the pane coordinates
		 */
		public Point2D.Double toPane(Point2D.Double imageCoordinate) {
			requireNonNull(imageCoordinate);
			if (Double.compare(scale, 0) == 0) {
				throw new IllegalStateException("Cannot translate coordinates before image is displayed");
			}
			return new Point2D.Double((imageCoordinate.x * scale) + origin.x, (imageCoordinate.y * scale) + origin.y);
		}


		/**
		 * Tests whether a given pane coordinate in the pane falls within the image boundaries.
		 * @param paneCoordinate the point on the pane
		 * @return true if an image is available and the given point is within the image
		 */
		public boolean withinImage(Point paneCoordinate) {
			requireNonNull(paneCoordinate);
			if (!image.isNull()) {
				BufferedImage bufferedImage = image.getOrThrow();
				Point2D.Double imagePoint = coordinates.toImage(paneCoordinate);
				double width = bufferedImage.getWidth();
				double height = bufferedImage.getHeight();

				return imagePoint.getX() >= 0 && imagePoint.getX() <= width && imagePoint.getY() >= 0 && imagePoint.getY() <= height;
			}

			return false;
		}
	}

	/**
	 * Centers the image
	 */
	public final class CenterImage {

		private CenterImage() {}

		/**
		 * Centers the image on the given image coordinate
		 * @param coordinate the image coordinate on which to center the image
		 */
		public void onImage(Point2D.Double coordinate) {
			onPane(toPoint(coordinates.toPane(requireNonNull(coordinate))));
		}

		/**
		 * Centers the image on the given coordinate on the pane, if it is within the image boundaries.
		 * @param coordinate the pane coordinate on which to center the image
		 */
		public void onPane(Point coordinate) {
			requireNonNull(coordinate);
			if (coordinates.withinImage(coordinate)) {
				Point currentCenter = new Point(getWidth() / 2, getHeight() / 2);
				origin.x += (int) (currentCenter.getX() - coordinate.getX());
				origin.y += (int) (currentCenter.getY() - coordinate.getY());
				origin.changed();
			}
		}
	}

	/**
	 * Called from paintComponent() when a new image is set.
	 */
	private void initializeParams() {
		double xScale = (double) getWidth() / image.getOrThrow().getWidth();
		double yScale = (double) getHeight() / image.getOrThrow().getHeight();
		initialScale = Math.min(xScale, yScale);
		scale = initialScale;
		//An image is initially centered
		centerImage();
		if (navigable.is()) {
			createNavigationImage();
		}
	}

	private void centerImage() {
		origin.x = (getWidth() - getScreenImageWidth()) / 2;
		origin.y = (getHeight() - getScreenImageHeight()) / 2;
		origin.changed();
	}

	private void createNavigationImage() {
		BufferedImage bufferedImage = image.getOrThrow();
		//We keep the original navigation image larger than initially
		//displayed to allow for zooming into it without pixellation effect.
		navigationImageWidth = (int) (getWidth() * NAV_IMAGE_FACTOR);
		navigationImageHeight = navigationImageWidth * bufferedImage.getHeight() / bufferedImage.getWidth();
		int scrNavImageWidth = (int) (getWidth() * SCREEN_NAV_IMAGE_FACTOR);
		navigationScale = (double) scrNavImageWidth / navigationImageWidth;
		ColorModel colorModel = bufferedImage.getColorModel();
		WritableRaster raster = colorModel.createCompatibleWritableRaster(navigationImageWidth, navigationImageHeight);
		navigationImage = new BufferedImage(colorModel, raster, false, getProperties(bufferedImage));
		navigationImage.getGraphics().drawImage(bufferedImage, 0, 0, navigationImageWidth, navigationImageHeight, null);
	}

	/**
	 * Tests whether a given point in the pane falls within the navigation image boundaries.
	 * @param panePoint the point in the pane
	 * @return true if the given point is within the navigation image
	 */
	private boolean isInNavigationImage(Point panePoint) {
		return navigable.is() && panePoint.x < getScreenNavImageWidth() && panePoint.y < getScreenNavImageHeight();
	}

	/**
	 * Tests whether the image is displayed in its entirety in the pane.
	 * @return true if the image is fully within the pane bounds
	 */
	private boolean isFullImageInPane() {
		return origin.x >= 0 && (origin.x + getScreenImageWidth()) < getWidth() &&
						origin.y >= 0 && (origin.y + getScreenImageHeight()) < getHeight();
	}

	/**
	 * High quality rendering kicks in when a scaled image is larger
	 * than the original image. In other words,
	 * when image decimation stops and interpolation starts.
	 * @return true if high quality rendering is enabled
	 */
	private boolean isHighQualityRendering() {
		return scale > HIGH_QUALITY_RENDERING_SCALE_THRESHOLD;
	}

	private void zoomImage(Point mousePosition) {
		if (Double.compare(initialScale, 0) != 0) {
			Point2D.Double imagePoint = coordinates.toImage(mousePosition);
			scale *= zoomFactor;
			Point2D.Double panePoint = coordinates.toPane(imagePoint);

			origin.x += (mousePosition.x - (int) panePoint.x);
			origin.y += (mousePosition.y - (int) panePoint.y);
			origin.changed();

			zoom.set(scale / initialScale);
		}
	}

	private void zoomNavigationImage() {
		navigationScale *= navZoomFactor;
		repaint();
	}

	/**
	 * @return the bounds of the image area currently displayed in the pane (in image coordinates).
	 */
	private @Nullable Rectangle getImageClipBounds() {
		Point2D.Double startPoint = coordinates.toImage(new Point(0, 0));
		Point2D.Double endPoint = coordinates.toImage(new Point(getWidth() - 1, getHeight() - 1));
		int paneX1 = (int) Math.round(startPoint.getX());
		int paneY1 = (int) Math.round(startPoint.getY());
		int paneX2 = (int) Math.round(endPoint.getX());
		int paneY2 = (int) Math.round(endPoint.getY());
		BufferedImage bufferedImage = image.getOrThrow();
		//No intersection?
		if (paneX1 >= bufferedImage.getWidth() || paneX2 < 0 || paneY1 >= bufferedImage.getHeight() || paneY2 < 0) {
			return null;
		}

		int x1 = Math.max(paneX1, 0);
		int y1 = Math.max(paneY1, 0);
		int x2 = (paneX2 >= bufferedImage.getWidth()) ? bufferedImage.getWidth() - 1 : paneX2;
		int y2 = (paneY2 >= bufferedImage.getHeight()) ? bufferedImage.getHeight() - 1 : paneY2;

		return new Rectangle(x1, y1, x2 - x1 + 1, y2 - y1 + 1);
	}

	/**
	 * Paints the pane and its image at the current zoom level, location, and
	 * interpolation method dependent on the image scale.
	 * @param g the {@code Graphics} context for painting
	 */
	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		if (image.isNull()) {
			return;
		}

		if (Double.compare(scale, 0) == 0) {
			initializeParams();
		}

		BufferedImage bufferedImage = image.getOrThrow();
		if (isHighQualityRendering()) {
			Rectangle rect = getImageClipBounds();
			if (rect == null || rect.width == 0 || rect.height == 0) { // no part of image is displayed in the pane
				return;
			}
			BufferedImage subimage = bufferedImage.getSubimage(rect.x, rect.y, rect.width, rect.height);
			Graphics2D g2 = (Graphics2D) g;
			g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, INTERPOLATION_TYPE);
			g2.drawImage(subimage, Math.max(0, origin.x), Math.max(0, origin.y),
							Math.min((int) (subimage.getWidth() * scale), getWidth()),
							Math.min((int) (subimage.getHeight() * scale), getHeight()), null);
		}
		else {
			g.drawImage(bufferedImage, origin.x, origin.y, getScreenImageWidth(), getScreenImageHeight(), null);
		}

		// Paint overlays last, but not over the navigation image and zoom area outline
		if (!overlays.isEmpty()) {
			overlays.forEach(overlay -> overlay.accept((Graphics2D) g, this));
		}

		if (navigable.is()) {
			g.drawImage(navigationImage, 0, 0, getScreenNavImageWidth(), getScreenNavImageHeight(), null);
			drawZoomAreaOutline(g);
		}
	}

	/**
	 * Paints a white outline over the navigation image indicating
	 * the area of the image currently displayed in the pane.
	 * @param g the graphics
	 */
	private void drawZoomAreaOutline(Graphics g) {
		int screenImageWidth = getScreenImageWidth();
		int screenImageHeight = getScreenImageHeight();
		if (isFullImageInPane() || screenImageHeight == 0 || screenImageWidth == 0) {
			return;
		}

		int x = -origin.x * getScreenNavImageWidth() / screenImageWidth;
		int y = -origin.y * getScreenNavImageHeight() / screenImageHeight;
		int width = getWidth() * getScreenNavImageWidth() / screenImageWidth;
		int height = getHeight() * getScreenNavImageHeight() / screenImageHeight;
		g.setColor(Color.white);
		g.drawRect(x, y, width, height);
	}

	private int getScreenImageWidth() {
		return (int) (scale * image.getOrThrow().getWidth());
	}

	private int getScreenImageHeight() {
		return (int) (scale * image.getOrThrow().getHeight());
	}

	private int getScreenNavImageWidth() {
		return (int) (navigationScale * navigationImageWidth);
	}

	private int getScreenNavImageHeight() {
		return (int) (navigationScale * navigationImageHeight);
	}

	private static Hashtable<String, Object> getProperties(BufferedImage image) {
		Hashtable<String, Object> properties = new Hashtable<>();
		String[] propertyNames = image.getPropertyNames();
		if (propertyNames != null && propertyNames.length > 0) {
			asList(propertyNames).forEach(propertyName -> properties.put(propertyName, image.getProperty(propertyName)));
		}

		return properties;
	}

	private static Point toPoint(Point2D.Double doublePoint) {
		return new Point((int) Math.round(doublePoint.x), (int) Math.round(doublePoint.y));
	}

	/**
	 * <p>Defines zoom devices.</p>
	 */
	public enum ZoomDevice {

		/**
		 * <p>Identifies that the pane does not implement zooming,
		 * but the component using the pane does (programmatic zooming method).</p>
		 */
		NONE,

		/**
		 * <p>Identifies the left and right mouse buttons as the zooming device.</p>
		 */
		MOUSE_BUTTON,

		/**
		 * <p>Identifies the mouse scroll wheel as the zooming device.</p>
		 */
		MOUSE_WHEEL
	}

	private final class WheelZoomDevice implements MouseWheelListener {

		@Override
		public void mouseWheelMoved(MouseWheelEvent event) {
			Point point = event.getPoint();
			boolean zoomIn = event.getWheelRotation() < 0;
			if (isInNavigationImage(point)) {
				if (zoomIn) {
					navZoomFactor = 1 + zoomIncrement.getOrThrow();
				}
				else {
					navZoomFactor = 1 - zoomIncrement.getOrThrow();
				}
				zoomNavigationImage();
			}
			else if (coordinates.withinImage(point)) {
				if (zoomIn) {
					zoomFactor = 1 + zoomIncrement.getOrThrow();
				}
				else {
					zoomFactor = 1 - zoomIncrement.getOrThrow();
				}
				zoomImage(point);
			}
		}
	}

	private final class ButtonZoomDevice extends MouseAdapter {

		@Override
		public void mouseClicked(MouseEvent event) {
			Point point = event.getPoint();
			if (SwingUtilities.isRightMouseButton(event)) {
				if (isInNavigationImage(point)) {
					navZoomFactor = 1 - zoomIncrement.getOrThrow();
					zoomNavigationImage();
				}
				else if (coordinates.withinImage(point)) {
					zoomFactor = 1 - zoomIncrement.getOrThrow();
					zoomImage(point);
				}
			}
			else {
				if (isInNavigationImage(point)) {
					navZoomFactor = 1 + zoomIncrement.getOrThrow();
					zoomNavigationImage();
				}
				else if (coordinates.withinImage(point)) {
					zoomFactor = 1 + zoomIncrement.getOrThrow();
					zoomImage(point);
				}
			}
		}
	}

	private final class DefaultImageValue extends AbstractValue<BufferedImage> implements ImageValue {

		private final ImageBytes bytes;

		private @Nullable BufferedImage bufferedImage;
		private boolean settingBytesFromImage = false;

		private DefaultImageValue(@Nullable BufferedImage image) {
			super(Notify.SET);
			this.bytes = new ImageBytes();
			addListener(this::onImageChanged);
			addValidator(IMAGE_VALIDATOR);
			set(image);
		}

		@Override
		public void set(byte[] imageBytes) {
			bytes.set(imageBytes);
		}

		@Override
		public void set(String imagePath) throws IOException {
			bytes.set(readAllBytes(Path.of(requireNonNull(imagePath))));
		}

		@Override
		public void set(BufferedImage image, String format) throws IOException {
			requireNonNull(image);
			requireNonNull(format);
			settingBytesFromImage = true;
			set(image);
			try {
				bytes.set(readImage(image, format));
			}
			finally {
				settingBytesFromImage = false;
			}
		}

		@Override
		protected @Nullable BufferedImage getValue() {
			return bufferedImage;
		}

		@Override
		protected void setValue(@Nullable BufferedImage bufferedImage) {
			this.bufferedImage = bufferedImage;
			navigationImage = null;
			//Reset scale so that initializeParameters() is called in paintComponent() for the new image.
			scale = 0d;
			repaint();
		}

		private void onImageChanged() {
			if (!settingBytesFromImage && !bytes.settingImageFromBytes) {
				settingBytesFromImage = true;
				try {
					bytes.clear();
				}
				finally {
					settingBytesFromImage = false;
				}
			}
		}

		private final class ImageBytes extends AbstractValue<byte[]> {

			private byte[] bytes = EMPTY_BYTES;
			private boolean settingImageFromBytes = false;

			private ImageBytes() {
				super(EMPTY_BYTES, Notify.CHANGED);
				addConsumer(this::onBytesChanged);
			}

			@Override
			protected byte[] getValue() {
				return bytes;
			}

			@Override
			protected void setValue(byte[] value) {
				bytes = value;
			}

			private void onBytesChanged(byte[] bytes) {
				if (!settingBytesFromImage) {
					settingImageFromBytes = true;
					try {
						image.set(bytes.length == 0 ? null : ImageIO.read(new ByteArrayInputStream(bytes)));
					}
					catch (IOException e) {
						throw new UncheckedIOException(e);
					}
					finally {
						settingImageFromBytes = false;
					}
				}
			}
		}
	}

	private final class ZoomValue extends AbstractValue<Double> {

		private ZoomValue() {
			super(Notify.CHANGED);
			addValidator(POSITIVE_NUMBER);
		}

		@Override
		protected Double getValue() {
			if (Double.compare(initialScale, 0) == 0) {
				return 0d;
			}

			return scale / initialScale;
		}

		@Override
		protected void setValue(Double value) {
			setZoom(value, new Point(getWidth() / 2, getHeight() / 2));
		}

		/**
		 * <p>Sets the zoom level used to display the image, and the zooming center,
		 * around which zooming is done.</p>
		 * <p>This method is used in programmatic zooming.
		 * After a new zoom level is set the image is repainted.</p>
		 * @param newZoom the zoom level used to display this pane's image.
		 * @param zoomingCenter the zooming center
		 */
		private void setZoom(double newZoom, Point zoomingCenter) {
			Point2D.Double imageP = coordinates.toImage(zoomingCenter);
			if (imageP.x < 0.0) {
				imageP = new Point2D.Double(0.0, imageP.getY());
			}
			if (imageP.y < 0.0) {
				imageP = new Point2D.Double(imageP.getX(), 0.0);
			}
			BufferedImage bufferedImage = image.getOrThrow();
			if (imageP.x >= bufferedImage.getWidth()) {
				imageP = new Point2D.Double(bufferedImage.getWidth() - 1d, imageP.getY());
			}
			if (imageP.y >= bufferedImage.getHeight()) {
				imageP = new Point2D.Double(imageP.getX(), bufferedImage.getHeight() - 1d);
			}

			Point2D.Double correctedP = coordinates.toPane(imageP);
			scale = zoomToScale(newZoom);
			Point2D.Double paneP = coordinates.toPane(imageP);

			origin.x += (int) (Math.round(correctedP.getX()) - (int) paneP.x);
			origin.y += (int) (Math.round(correctedP.getY()) - (int) paneP.y);
			origin.changed();

			notifyObserver();
		}

		private double zoomToScale(double zoom) {
			return initialScale * zoom;
		}
	}

	private final class ImageOriginValue extends AbstractValue<Point> {

		private int x = 0;
		private int y = 0;

		private ImageOriginValue() {
			super(new Point(0, 0), Notify.CHANGED);
		}

		@Override
		protected Point getValue() {
			return new Point(x, y);
		}

		@Override
		protected void setValue(Point value) {
			x = value.x;
			y = value.y;
			repaint();
		}

		private void changed() {
			notifyObserver();
			repaint();
		}
	}

	private final class ZoomDeviceValue extends AbstractValue<ZoomDevice> {

		private ZoomDevice zoomDevice;

		private @Nullable WheelZoomDevice wheelZoomDevice;
		private @Nullable ButtonZoomDevice buttonZoomDevice;

		private ZoomDeviceValue(ZoomDevice zoomDevice) {
			super(zoomDevice, Notify.CHANGED);
			setValue(zoomDevice);
		}

		@Override
		protected ZoomDevice getValue() {
			return zoomDevice;
		}

		@Override
		protected void setValue(ZoomDevice zoomDevice) {
			this.zoomDevice = zoomDevice;
			if (zoomDevice == ZoomDevice.NONE) {
				removeWheelZoomDevice();
				removeButtonZoomDevice();
			}
			else if (zoomDevice == ZoomDevice.MOUSE_BUTTON) {
				removeWheelZoomDevice();
				addButtonZoomDevice();
			}
			else if (zoomDevice == ZoomDevice.MOUSE_WHEEL) {
				removeButtonZoomDevice();
				addWheelZoomDevice();
			}
		}

		private void addWheelZoomDevice() {
			if (wheelZoomDevice == null) {
				wheelZoomDevice = new WheelZoomDevice();
				addMouseWheelListener(wheelZoomDevice);
			}
		}

		private void addButtonZoomDevice() {
			if (buttonZoomDevice == null) {
				buttonZoomDevice = new ButtonZoomDevice();
				addMouseListener(buttonZoomDevice);
			}
		}

		private void removeWheelZoomDevice() {
			if (wheelZoomDevice != null) {
				removeMouseWheelListener(wheelZoomDevice);
				wheelZoomDevice = null;
			}
		}

		private void removeButtonZoomDevice() {
			if (buttonZoomDevice != null) {
				removeMouseListener(buttonZoomDevice);
				buttonZoomDevice = null;
			}
		}
	}

	private final class ImageComponentAdapter extends ComponentAdapter {

		@Override
		public void componentResized(ComponentEvent e) {
			if (scale > 0.0) {
				if (autoResize.is()) {
					reset(); // Fit to new pane size
				}
				else {
					if (isFullImageInPane()) {
						centerImage();
					}
					else if (isImageEdgeInPane()) {
						scaleOrigin();
					}
				}
				if (navigable.is()) {
					createNavigationImage();
				}
				repaint();
			}
			previousPaneSize = getSize();
		}

		private boolean isImageEdgeInPane() {
			boolean originXOK = origin.x > 0 && origin.x < previousPaneSize.width;
			boolean originYOK = origin.y > 0 && origin.y < previousPaneSize.height;

			return previousPaneSize != null && (originXOK || originYOK);
		}

		private void scaleOrigin() {
			origin.x = origin.x * getWidth() / previousPaneSize.width;
			origin.y = origin.y * getHeight() / previousPaneSize.height;
			origin.changed();
		}
	}

	private final class ImageMouseAdapter extends MouseAdapter {

		@Override
		public void mousePressed(MouseEvent e) {
			if (isLeftMouseButton(e) && isInNavigationImage(e.getPoint())) {
				displayImageAt(e.getPoint());
			}
		}

		private void displayImageAt(Point panePoint) {
			Point scrImagePoint = navigationToZoomedImagePoint(panePoint);
			origin.x = -(scrImagePoint.x - getWidth() / 2);
			origin.y = -(scrImagePoint.y - getHeight() / 2);
			origin.changed();
		}

		private Point navigationToZoomedImagePoint(Point panePoint) {
			int x = panePoint.x * getScreenImageWidth() / getScreenNavImageWidth();
			int y = panePoint.y * getScreenImageHeight() / getScreenNavImageHeight();

			return new Point(x, y);
		}
	}

	private final class ImageMouseMotionListener implements MouseMotionListener {

		private @Nullable Point mousePosition;

		@Override
		public void mouseDragged(MouseEvent e) {
			if (movable.is() && isLeftMouseButton(e) && !isInNavigationImage(e.getPoint())) {
				moveImage(e.getPoint());
			}
		}

		@Override
		public void mouseMoved(MouseEvent e) {
			//we need the mouse position so that after zooming
			//that position of the image is maintained
			mousePosition = e.getPoint();
		}

		/**
		 * Moves te image (by dragging with the mouse) to a new mouse position p.
		 * @param panePoint the point
		 */
		private void moveImage(Point panePoint) {
			if (mousePosition != null) {
				int xDelta = panePoint.x - mousePosition.x;
				int yDelta = panePoint.y - mousePosition.y;
				mousePosition = panePoint;
				origin.x += xDelta;
				origin.y += yDelta;
				origin.changed();
			}
		}
	}

	private static final class ImageValidator implements Validator<BufferedImage> {

		@Override
		public void validate(@Nullable BufferedImage image) {
			if (image != null && (image.getHeight() == 0 || image.getWidth() == 0)) {
				throw new IllegalArgumentException("Only images of non-zero size can be viewed");
			}
		}
	}

	private static final class DefaultBuilder extends AbstractComponentValueBuilder<ImagePane, byte[], Builder> implements Builder {

		private final List<BiConsumer<Graphics2D, ImagePane>> overlays = new ArrayList<>(1);

		private @Nullable BufferedImage image;
		private ZoomDevice zoomDevice = ZOOM_DEVICE.getOrThrow();
		private boolean autoResize = AUTO_RESIZE.getOrThrow();
		private boolean navigable = false;
		private boolean movable = false;

		@Override
		public Builder image(String imagePath) throws IOException {
			return value(readAllBytes(Path.of(requireNonNull(imagePath))));
		}

		@Override
		public Builder image(BufferedImage image) {
			this.image = requireNonNull(image);
			return value(null);
		}

		@Override
		public Builder image(BufferedImage image, String format) throws IOException {
			value(readImage(requireNonNull(image), requireNonNull(format)));
			this.image = null;
			return this;
		}

		@Override
		public Builder overlay(BiConsumer<Graphics2D, ImagePane> overlay) {
			this.overlays.add(requireNonNull(overlay));
			return this;
		}

		@Override
		public Builder zoomDevice(ZoomDevice zoomDevice) {
			this.zoomDevice = requireNonNull(zoomDevice);
			return this;
		}

		@Override
		public Builder autoResize(boolean autoResize) {
			this.autoResize = autoResize;
			return this;
		}

		@Override
		public Builder navigable(boolean navigable) {
			this.navigable = navigable;
			return this;
		}

		@Override
		public Builder movable(boolean movable) {
			this.movable = movable;
			return this;
		}

		@Override
		protected ImagePane createComponent() {
			return new ImagePane(this);
		}

		@Override
		protected ComponentValue<ImagePane, byte[]> createComponentValue(ImagePane component) {
			return new ByteArrayComponentValue(requireNonNull(component));
		}
	}

	private static byte[] readImage(BufferedImage image, String format) throws IOException {
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		if (!ImageIO.write(image, format, outputStream)) {
			throw new IllegalArgumentException("No image writer found for format: " + format);
		}

		return outputStream.toByteArray();
	}

	private static final class ByteArrayComponentValue extends AbstractComponentValue<ImagePane, byte[]> {

		private ByteArrayComponentValue(ImagePane component) {
			super(component, EMPTY_BYTES);
			component.image.bytes.addListener(this::notifyObserver);
		}

		@Override
		public Optional<byte[]> optional() {
			byte[] bytes = getOrThrow();

			return bytes.length > 0 ? Optional.of(bytes) : Optional.empty();
		}

		@Override
		protected byte[] getComponentValue() {
			byte[] bytes = component().image.bytes.getOrThrow();

			return copyOf(bytes, bytes.length);
		}

		@Override
		protected void setComponentValue(byte[] value) {
			component().image.bytes.set(copyOf(value, value.length));
		}
	}
}
