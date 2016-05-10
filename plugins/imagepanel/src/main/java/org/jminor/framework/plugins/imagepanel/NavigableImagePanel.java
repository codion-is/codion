package org.jminor.framework.plugins.imagepanel;

import org.jminor.common.Util;
import org.jminor.common.i18n.Messages;
import org.jminor.framework.domain.Entity;
import org.jminor.swing.common.ui.UiUtil;
import org.jminor.swing.common.ui.control.Control;
import org.jminor.swing.framework.ui.EntityTablePanel;

import javax.imageio.ImageIO;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;

/**
 * <code>NavigableImagePanel</code> is a lightweight container displaying
 * an image that can be zoomed in and out and panned with ease and simplicity,
 * using an adaptive rendering for high quality display and satisfactory performance.
 * <h3>Image</h3>
 * <p>An image is loaded either via a constructor:</p>
 * <pre>
 * NavigableImagePanel panel = new NavigableImagePanel(image);
 * </pre>
 * or using a setter:
 * <pre>
 * NavigableImagePanel panel = new NavigableImagePanel();
 * panel.setImage(image);
 * </pre>
 * When an image is set, it is initially painted centered in the component,
 * at the largest possible size, fully visible, with its aspect ratio is preserved.
 * This is defined as 100% of the image size and its corresponding zoom level is 1.0.
 * <h3>Zooming</h3>
 * Zooming can be controlled interactively, using either the mouse scroll wheel (default)
 * or the mouse two buttons, or programmatically, allowing the programmer to
 * implement other custom zooming methods. If the mouse does not have a scroll wheel,
 * set the zooming device to mouse buttons:
 * <pre>
 * panel.setZoomDevice(ZoomDevice.MOUSE_BUTTON);
 * </pre>
 * The left mouse button works as a toggle switch between zooming in and zooming out
 * modes, and the right button zooms an image by one increment (default is 20%).
 * You can change the zoom increment value by:
 * <pre>
 * panel.setZoomIncrement(newZoomIncrement);
 * </pre>
 * If you intend to provide programmatic zoom control, set the zoom device to none
 * to disable both the mouse wheel and buttons for zooming purposes:
 * <pre>
 * panel.setZoomDevice(ZoomDevice.NONE);
 * </pre>
 * and use <code>setZoom()</code> to change the zoom level.
 * Zooming is always around the point the mouse pointer is currently at, so that
 * this point (called a zooming center) remains stationary ensuring that the area
 * of an image we are zooming into does not disappear off the screen. The zooming center
 * stays at the same location on the screen and all other points move radially away from
 * it (when zooming in), or towards it (when zooming out). For programmatically
 * controlled zooming the zooming center is either specified when <code>setZoom()</code>
 * is called:
 * <pre>
 * panel.setZoom(newZoomLevel, newZoomingCenter);
 * </pre>
 * or assumed to be the point of an image which is
 * the closest to the center of the panel, if no zooming center is specified:
 * <pre>
 * panel.setZoom(newZoomLevel);
 * </pre>
 * There are no lower or upper zoom level limits.
 * <h3>Navigation</h3>
 * <code>NavigableImagePanel</code> does not use scroll bars for navigation,
 * but relies on a navigation image located in the upper left corner of the panel.
 * The navigation image is a small replica of the image displayed in the panel.
 * When you click on any point of the navigation image that part of the image
 * is displayed in the panel, centered. The navigation image can also be
 * zoomed in the same way as the main image.
 * <p>In order to adjust the position of an image in the panel, it can be dragged
 * with the mouse, using the left button.
 * For programmatic image navigation, disable the navigation image:
 * <pre>
 * panel.setNavigationImageEnabled(false)
 * </pre>
 * and use <code>getImageOrigin()</code> and
 * <code>setImageOrigin()</code> to move the image around the panel.
 * <h3>Rendering</h3>
 * <code>NavigableImagePanel</code> uses the Nearest Neighbor interpolation
 * for image rendering (default in Java).
 * When the scaled image becomes larger than the original image,
 * the Bilinear interpolation is applied, but only to the part
 * of the image which is displayed in the panel. This interpolation change threshold
 * can be controlled by adjusting the value of
 * <code>HIGH_QUALITY_RENDERING_SCALE_THRESHOLD</code>.
 *
 * Author: Slav Boleslawski
 * http://today.java.net/pub/a/today/2007/03/27/navigable-image-panel.html
 */
public final class NavigableImagePanel extends JPanel {

  /**
   * <p>Identifies a change to the zoom level.</p>
   */
  public static final String ZOOM_LEVEL_CHANGED_PROPERTY = "zoomLevel";

  /**
   * <p>Identifies a change to the zoom increment.</p>
   */
  public static final String ZOOM_INCREMENT_CHANGED_PROPERTY = "zoomIncrement";

  /**
   * <p>Identifies that the image in the panel has changed.</p>
   */
  public static final String IMAGE_CHANGED_PROPERTY = "image";

  private static final Collection<String> IMAGE_FILE_TYPES = Arrays.asList("gif", "tif", "jpg", "jpeg", "png", "bmp");

  private static final double DEFAULT_IMAGE_PANEL_SCREEN_SIZE_RATIO = 0.5;
  private static final double SCREEN_NAV_IMAGE_FACTOR = 0.15; // 15% of panel's width
  private static final double NAV_IMAGE_FACTOR = 0.3; // 30% of panel's width
  private static final double ONE_POINT_O = 1.0;
  private static final double HIGH_QUALITY_RENDERING_SCALE_THRESHOLD = ONE_POINT_O;
  private static final double DEFAULT_ZOOM_INCREMENT = 0.2;
  private static final Object INTERPOLATION_TYPE = RenderingHints.VALUE_INTERPOLATION_BILINEAR;

  private double zoomIncrement = DEFAULT_ZOOM_INCREMENT;
  private double zoomFactor = ONE_POINT_O + zoomIncrement;
  private double navZoomFactor = ONE_POINT_O + zoomIncrement;
  private BufferedImage image;
  private BufferedImage navigationImage;
  private int navImageWidth;
  private int navImageHeight;
  private double initialScale = 0.0;
  private double scale = 0.0;
  private double navScale = 0.0;
  private int originX = 0;
  private int originY = 0;
  private Point mousePosition;
  private Dimension previousPanelSize;
  private boolean navigationImageEnabled = true;
  private boolean highQualityRenderingEnabled = true;

  private WheelZoomDevice wheelZoomDevice = null;
  private ButtonZoomDevice buttonZoomDevice = null;

  /**
   * <p>Defines zoom devices.</p>
   */
  public static final class ZoomDevice {
    /**
     * <p>Identifies that the panel does not implement zooming,
     * but the component using the panel does (programmatic zooming method).</p>
     */
    public static final ZoomDevice NONE = new ZoomDevice("none");

    /**
     * <p>Identifies the left and right mouse buttons as the zooming device.</p>
     */
    public static final ZoomDevice MOUSE_BUTTON = new ZoomDevice("mouseButton");

    /**
     * <p>Identifies the mouse scroll wheel as the zooming device.</p>
     */
    public static final ZoomDevice MOUSE_WHEEL = new ZoomDevice("mouseWheel");

    private final String zoomDevice;
    private ZoomDevice(final String zoomDevice) {
      this.zoomDevice = zoomDevice;
    }
    @Override
    public String toString() {
      return zoomDevice;
    }
  }

  //This class is required for high precision image coordinates translation.
  private static final class Coords {
    private double x;
    private double y;
    private Coords(final double x, final double y) {
      this.x = x;
      this.y = y;
    }
    public int getIntX() {
      return (int)Math.round(x);
    }
    public int getIntY() {
      return (int)Math.round(y);
    }
    @Override
    public String toString() {
      return "[Coords: x=" + x + ",y=" + y + "]";
    }
  }

  private final class WheelZoomDevice implements MouseWheelListener {
    @Override
    public void mouseWheelMoved(final MouseWheelEvent e) {
      final Point p = e.getPoint();
      final boolean zoomIn = e.getWheelRotation() < 0;
      if (isInNavigationImage(p)) {
        if (zoomIn) {
          navZoomFactor = ONE_POINT_O + zoomIncrement;
        } else {
          navZoomFactor = ONE_POINT_O - zoomIncrement;
        }
        zoomNavigationImage();
      } else if (isInImage(p)) {
        if (zoomIn) {
          zoomFactor = ONE_POINT_O + zoomIncrement;
        } else {
          zoomFactor = ONE_POINT_O - zoomIncrement;
        }
        zoomImage();
      }
    }
  }

  private final class ButtonZoomDevice extends MouseAdapter {
    @Override
    public void mouseClicked(final MouseEvent e) {
      final Point p = e.getPoint();
      if (SwingUtilities.isRightMouseButton(e)) {
        if (isInNavigationImage(p)) {
          navZoomFactor = ONE_POINT_O - zoomIncrement;
          zoomNavigationImage();
        } else if (isInImage(p)) {
          zoomFactor = ONE_POINT_O - zoomIncrement;
          zoomImage();
        }
      } else {
        if (isInNavigationImage(p)) {
          navZoomFactor = ONE_POINT_O + zoomIncrement;
          zoomNavigationImage();
        } else if (isInImage(p)) {
          zoomFactor = ONE_POINT_O + zoomIncrement;
          zoomImage();
        }
      }
    }
  }

  /**
   * <p>Creates a new navigable image panel with no default image and
   * the mouse scroll wheel as the zooming device.</p>
   */
  public NavigableImagePanel() {
    setOpaque(false);
    previousPanelSize = getSize();
    addComponentListener(new ComponentAdapter() {
      @Override
      public void componentResized(final ComponentEvent e) {
        if (scale > 0.0) {
          if (isFullImageInPanel()) {
            centerImage();
          } else if (isImageEdgeInPanel()) {
            scaleOrigin();
          }
          if (isNavigationImageEnabled()) {
            createNavigationImage();
          }
          repaint();
        }
        previousPanelSize = getSize();
      }
    });

    addMouseListener(new MouseAdapter() {
      @Override
      public void mousePressed(final MouseEvent e) {
        if (SwingUtilities.isLeftMouseButton(e) &&isInNavigationImage(e.getPoint())) {
          displayImageAt(e.getPoint());
        }
      }
    });

    addMouseMotionListener(new MouseMotionListener() {
      @Override
      public void mouseDragged(final MouseEvent e) {
        if (SwingUtilities.isLeftMouseButton(e)
                && !isInNavigationImage(e.getPoint())) {
          moveImage(e.getPoint());
        }
      }
      @Override
      public void mouseMoved(final MouseEvent e) {
        //we need the mouse position so that after zooming
        //that position of the image is maintained
        mousePosition = e.getPoint();
      }
    });

    setZoomDevice(ZoomDevice.MOUSE_WHEEL);
  }

  /**
   * <p>Creates a new navigable image panel with the specified image
   * and the mouse scroll wheel as the zooming device.</p>
   * @param image the default image
   */
  public NavigableImagePanel(final BufferedImage image) {
    this();
    setImage(image);
  }

  /**
   * @param imagePath the path to the image to show
   * @param dialogParent the component to use as dialog parent
   * @throws IOException in case of an IO exception
   */
  public static void showImage(final String imagePath, final JComponent dialogParent) throws IOException {
    showImage(imagePath, dialogParent, IMAGE_FILE_TYPES);
  }

  /**
   * @param imagePath the path to the image to show, if the file has a file type suffix it
   * is checked against the <code>acceptedFileTypes</code> collection.
   * @param dialogParent the component to use as dialog parent
   * @param acceptedFileTypes a collection of lower case file type suffixes, "gif", "jpeg"...
   * @throws IOException in case of an IO exception, f.ex. if the image file is not found
   * @throws IllegalArgumentException in case the file type is not accepted
   */
  public static void showImage(final String imagePath, final JComponent dialogParent,
                               final Collection<String> acceptedFileTypes) throws IOException {
    Util.rejectNullValue(imagePath, "imagePath");
    if (imagePath.length() == 0) {
      return;
    }

    final int lastDotIndex = imagePath.lastIndexOf('.');
    if (lastDotIndex != -1) {//if the type is specified check it
      final String type = imagePath.substring(lastDotIndex + 1, imagePath.length()).toLowerCase();
      if (!acceptedFileTypes.contains(type)) {
        throw new IllegalArgumentException(Messages.get(Messages.UNKNOWN_FILE_TYPE) + ": " + type);
      }
    }
    final NavigableImagePanel imagePanel;
    try {
      UiUtil.setWaitCursor(true, dialogParent);
      imagePanel = new NavigableImagePanel();
      final BufferedImage image;
      if (imagePath.toLowerCase().startsWith("http")) {
        final URL url = new URL(imagePath);
        image = ImageIO.read(url);
      }
      else {
        final File imageFile = new File(imagePath);
        if (!imageFile.exists()) {
          throw new FileNotFoundException(Messages.get(Messages.FILE_NOT_FOUND) + ": " + imagePath);
        }
        image = ImageIO.read(imageFile);
      }
      imagePanel.setImage(image);
    }
    finally {
      UiUtil.setWaitCursor(false, dialogParent);
    }
    imagePanel.setPreferredSize(UiUtil.getScreenSizeRatio(DEFAULT_IMAGE_PANEL_SCREEN_SIZE_RATIO));
    UiUtil.displayInDialog(dialogParent, imagePanel, imagePath, false);
  }

  /**
   * Creates a Control for viewing an image based on the entity selected in this EntityTablePanel.
   * The action shows an image found at the path specified by the value of the given propertyID.
   * If no entity is selected or the image path value is null no action is performed.
   * @param tablePanel the EntityTablePanel on which to base the control
   * @param imagePathPropertyID the ID of the property specifying the image path
   * @return a Control for viewing an image based on the selected entity in a EntityTablePanel
   * @see NavigableImagePanel#showImage(String, JComponent)
   */
  public static Control getViewImageControl(final EntityTablePanel tablePanel, final String imagePathPropertyID) {
    Util.rejectNullValue(imagePathPropertyID, "imagePathPropertyID");
    return new Control() {
      @Override
      public void actionPerformed(final ActionEvent e) {
        try {
          if (!tablePanel.getTableModel().getSelectionModel().isSelectionEmpty()) {
            final Entity selected = tablePanel.getTableModel().getSelectionModel().getSelectedItem();
            if (!selected.isValueNull(imagePathPropertyID)) {
              showImage(selected.getString(imagePathPropertyID), tablePanel);
            }
          }
        }
        catch (final IOException ex) {
          throw new RuntimeException(ex);
        }
      }
    };
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

  /**
   * <p>Sets a new zoom device.</p>
   *
   * @param newZoomDevice specifies the type of a new zoom device.
   */
  public void setZoomDevice(final ZoomDevice newZoomDevice) {
    if (newZoomDevice == ZoomDevice.NONE) {
      removeWheelZoomDevice();
      removeButtonZoomDevice();
    } else if (newZoomDevice == ZoomDevice.MOUSE_BUTTON) {
      removeWheelZoomDevice();
      addButtonZoomDevice();
    } else if (newZoomDevice == ZoomDevice.MOUSE_WHEEL) {
      removeButtonZoomDevice();
      addWheelZoomDevice();
    }
  }

  /**
   * <p>Gets the current zoom device.</p>
   * @return the ZoomDevice
   */
  public ZoomDevice getZoomDevice() {
    if (buttonZoomDevice != null) {
      return ZoomDevice.MOUSE_BUTTON;
    } else if (wheelZoomDevice != null) {
      return ZoomDevice.MOUSE_WHEEL;
    } else {
      return ZoomDevice.NONE;
    }
  }

  //Called from paintComponent() when a new image is set.
  private void initializeParams() {
    final double xScale = (double)getWidth() / image.getWidth();
    final double yScale = (double)getHeight() / image.getHeight();
    initialScale = Math.min(xScale, yScale);
    scale = initialScale;

    //An image is initially centered
    centerImage();
    if (navigationImageEnabled) {
      createNavigationImage();
    }
  }

  //Centers the current image in the panel.
  private void centerImage() {
    originX = (getWidth() - getScreenImageWidth()) / 2;
    originY = (getHeight() - getScreenImageHeight()) / 2;
  }

  //Creates and renders the navigation image in the upper let corner of the panel.
  private void createNavigationImage() {
    //We keep the original navigation image larger than initially
    //displayed to allow for zooming into it without pixellation effect.
    navImageWidth = (int)(getWidth() * NAV_IMAGE_FACTOR);
    navImageHeight = navImageWidth * image.getHeight() / image.getWidth();
    final int scrNavImageWidth = (int)(getWidth() * SCREEN_NAV_IMAGE_FACTOR);
    navScale = (double)scrNavImageWidth / navImageWidth;
    navigationImage = new BufferedImage(navImageWidth, navImageHeight,
            image.getType());
    final Graphics g = navigationImage.getGraphics();
    g.drawImage(image, 0, 0, navImageWidth, navImageHeight, null);
  }

  /**
   * <p>Sets an image for display in the panel.</p>
   *
   * @param image an image to be set in the panel
   */
  public void setImage(final BufferedImage image) {
    final BufferedImage oldImage = this.image;
    this.image = image;
    //Reset scale so that initializeParameters() is called in paintComponent()
    //for the new image.
    scale = 0.0;
    firePropertyChange(IMAGE_CHANGED_PROPERTY, oldImage, image);
    repaint();
  }

  /**
   * <p>Tests whether an image uses the standard RGB color space.</p>
   * @param bImage the image to test
   * @return true if the image uses the standard RGB color space
   */
  public static boolean isStandardRGBImage(final BufferedImage bImage) {
    return bImage.getColorModel().getColorSpace().isCS_sRGB();
  }

  //Converts this panel's coordinates into the original image coordinates
  private Coords panelToImageCoords(final Point p) {
    return new Coords((p.x - originX) / scale, (p.y - originY) / scale);
  }

  //Converts the original image coordinates into this panel's coordinates
  private Coords imageToPanelCoords(final Coords p) {
    return new Coords((p.x * scale) + originX, (p.y * scale) + originY);
  }

  //Converts the navigation image coordinates into the zoomed image coordinates
  private Point navToZoomedImageCoords(final Point p) {
    final int x = p.x * getScreenImageWidth() / getScreenNavImageWidth();
    final int y = p.y * getScreenImageHeight() / getScreenNavImageHeight();
    return new Point(x, y);
  }

  //The user clicked within the navigation image and this part of the image
  //is displayed in the panel.
  //The clicked point of the image is centered in the panel.
  private void displayImageAt(final Point p) {
    final Point scrImagePoint = navToZoomedImageCoords(p);
    originX = -(scrImagePoint.x - getWidth() / 2);
    originY = -(scrImagePoint.y - getHeight() / 2);
    repaint();
  }

  //Tests whether a given point in the panel falls within the image boundaries.
  private boolean isInImage(final Point p) {
    final Coords coords = panelToImageCoords(p);
    final int x = coords.getIntX();
    final int y = coords.getIntY();
    return x >= 0 && x < image.getWidth() && y >= 0 && y < image.getHeight();
  }

  //Tests whether a given point in the panel falls within the navigation image
  //boundaries.
  private boolean isInNavigationImage(final Point p) {
    return navigationImageEnabled && p.x < getScreenNavImageWidth() && p.y < getScreenNavImageHeight();
  }

  //Used when the image is resized.
  private boolean isImageEdgeInPanel() {
    final boolean originXOK = originX > 0 && originX < previousPanelSize.width;
    final boolean originYOK = originY > 0 && originY < previousPanelSize.height;

    return previousPanelSize != null && (originXOK || originYOK);
  }

  //Tests whether the image is displayed in its entirety in the panel.
  private boolean isFullImageInPanel() {
    return originX >= 0 && (originX + getScreenImageWidth()) < getWidth()
            && originY >= 0 && (originY + getScreenImageHeight()) < getHeight();
  }

  /**
   * <p>Indicates whether the high quality rendering feature is enabled.</p>
   *
   * @return true if high quality rendering is enabled, false otherwise.
   */
  public boolean isHighQualityRenderingEnabled() {
    return highQualityRenderingEnabled;
  }

  /**
   * <p>Enables/disables high quality rendering.</p>
   *
   * @param enabled enables/disables high quality rendering
   */
  public void setHighQualityRenderingEnabled(final boolean enabled) {
    highQualityRenderingEnabled = enabled;
  }

  //High quality rendering kicks in when when a scaled image is larger
  //than the original image. In other words,
  //when image decimation stops and interpolation starts.
  private boolean isHighQualityRendering() {
    return highQualityRenderingEnabled && scale > HIGH_QUALITY_RENDERING_SCALE_THRESHOLD;
  }

  /**
   * <p>Indicates whether navigation image is enabled.<p>
   *
   * @return true when navigation image is enabled, false otherwise.
   */
  public boolean isNavigationImageEnabled() {
    return navigationImageEnabled;
  }

  /**
   * <p>Enables/disables navigation with the navigation image.</p>
   * <p>Navigation image should be disabled when custom, programmatic navigation
   * is implemented.</p>
   *
   * @param enabled true when navigation image is enabled, false otherwise.
   */
  public void setNavigationImageEnabled(final boolean enabled) {
    navigationImageEnabled = enabled;
    repaint();
  }

  //Used when the panel is resized
  private void scaleOrigin() {
    originX = originX * getWidth() / previousPanelSize.width;
    originY = originY * getHeight() / previousPanelSize.height;
    repaint();
  }

  //Converts the specified zoom level	to scale.
  private double zoomToScale(final double zoom) {
    return initialScale * zoom;
  }

  /**
   * <p>Gets the current zoom level.</p>
   *
   * @return the current zoom level
   */
  public double getZoom() {
    return scale / initialScale;
  }

  /**
   * <p>Sets the zoom level used to display the image.</p>
   * <p>This method is used in programmatic zooming. The zooming center is
   * the point of the image closest to the center of the panel.
   * After a new zoom level is set the image is repainted.</p>
   *
   * @param newZoom the zoom level used to display this panel's image.
   */
  public void setZoom(final double newZoom) {
    final Point zoomingCenter = new Point(getWidth() / 2, getHeight() / 2);
    setZoom(newZoom, zoomingCenter);
  }

  /**
   * <p>Sets the zoom level used to display the image, and the zooming center,
   * around which zooming is done.</p>
   * <p>This method is used in programmatic zooming.
   * After a new zoom level is set the image is repainted.</p>
   *
   * @param newZoom the zoom level used to display this panel's image.
   * @param zoomingCenter the zooming center
   */
  public void setZoom(final double newZoom, final Point zoomingCenter) {
    final Coords imageP = panelToImageCoords(zoomingCenter);
    if (imageP.x < 0.0) {
      imageP.x = 0.0;
    }
    if (imageP.y < 0.0) {
      imageP.y = 0.0;
    }
    if (imageP.x >= image.getWidth()) {
      imageP.x = image.getWidth() - ONE_POINT_O;
    }
    if (imageP.y >= image.getHeight()) {
      imageP.y = image.getHeight() - ONE_POINT_O;
    }

    final Coords correctedP = imageToPanelCoords(imageP);
    final double oldZoom = getZoom();
    scale = zoomToScale(newZoom);
    final Coords panelP = imageToPanelCoords(imageP);

    originX += (correctedP.getIntX() - (int)panelP.x);
    originY += (correctedP.getIntY() - (int)panelP.y);

    firePropertyChange(ZOOM_LEVEL_CHANGED_PROPERTY, Double.valueOf(oldZoom),
            Double.valueOf(getZoom()));

    repaint();
  }

  /**
   * <p>Gets the current zoom increment.</p>
   *
   * @return the current zoom increment
   */
  public double getZoomIncrement() {
    return zoomIncrement;
  }

  /**
   * <p>Sets a new zoom increment value.</p>
   *
   * @param newZoomIncrement new zoom increment value
   */
  public void setZoomIncrement(final double newZoomIncrement) {
    final double oldZoomIncrement = zoomIncrement;
    zoomIncrement = newZoomIncrement;
    firePropertyChange(ZOOM_INCREMENT_CHANGED_PROPERTY,
            Double.valueOf(oldZoomIncrement), Double.valueOf(zoomIncrement));
  }

  //Zooms an image in the panel by repainting it at the new zoom level.
  //The current mouse position is the zooming center.
  private void zoomImage() {
    final Coords imageP = panelToImageCoords(mousePosition);
    final double oldZoom = getZoom();
    scale *= zoomFactor;
    final Coords panelP = imageToPanelCoords(imageP);

    originX += (mousePosition.x - (int)panelP.x);
    originY += (mousePosition.y - (int)panelP.y);

    firePropertyChange(ZOOM_LEVEL_CHANGED_PROPERTY, Double.valueOf(oldZoom) ,
            Double.valueOf(getZoom()));

    repaint();
  }

  //Zooms the navigation image
  private void zoomNavigationImage() {
    navScale *= navZoomFactor;
    repaint();
  }

  /**
   * <p>Gets the image origin.</p>
   * <p>Image origin is defined as the upper, left corner of the image in
   * the panel's coordinate system.</p>
   * @return the point of the upper, left corner of the image in the panel's coordinates
   * system.
   */
  public Point getImageOrigin() {
    return new Point(originX, originY);
  }

  /**
   * <p>Sets the image origin.</p>
   * <p>Image origin is defined as the upper, left corner of the image in
   * the panel's coordinate system. After a new origin is set, the image is repainted.
   * This method is used for programmatic image navigation.</p>
   * @param x the x coordinate of the new image origin
   * @param y the y coordinate of the new image origin
   */
  public void setImageOrigin(final int x, final int y) {
    setImageOrigin(new Point(x, y));
  }

  /**
   * <p>Sets the image origin.</p>
   * <p>Image origin is defined as the upper, left corner of the image in
   * the panel's coordinate system. After a new origin is set, the image is repainted.
   * This method is used for programmatic image navigation.</p>
   * @param newOrigin the value of a new image origin
   */
  public void setImageOrigin(final Point newOrigin) {
    originX = newOrigin.x;
    originY = newOrigin.y;
    repaint();
  }

  //Moves te image (by dragging with the mouse) to a new mouse position p.
  private void moveImage(final Point p) {
    final int xDelta = p.x - mousePosition.x;
    final int yDelta = p.y - mousePosition.y;
    originX += xDelta;
    originY += yDelta;
    mousePosition = p;
    repaint();
  }

  //Gets the bounds of the image area currently displayed in the panel (in image
  //coordinates).
  private Rectangle getImageClipBounds() {
    final Coords startCoords = panelToImageCoords(new Point(0, 0));
    final Coords endCoords = panelToImageCoords(new Point(getWidth() - 1, getHeight() - 1));
    final int panelX1 = startCoords.getIntX();
    final int panelY1 = startCoords.getIntY();
    final int panelX2 = endCoords.getIntX();
    final int panelY2 = endCoords.getIntY();
    //No intersection?
    if (panelX1 >= image.getWidth() || panelX2 < 0 || panelY1 >= image.getHeight() || panelY2 < 0) {
      return null;
    }

    final int x1 = (panelX1 < 0) ? 0 : panelX1;
    final int y1 = (panelY1 < 0) ? 0 : panelY1;
    final int x2 = (panelX2 >= image.getWidth()) ? image.getWidth() - 1 : panelX2;
    final int y2 = (panelY2 >= image.getHeight()) ? image.getHeight() - 1 : panelY2;
    return new Rectangle(x1, y1, x2 - x1 + 1, y2 - y1 + 1);
  }

  /**
   * Paints the panel and its image at the current zoom level, location, and
   * interpolation method dependent on the image scale.
   *
   * @param g the <code>Graphics</code> context for painting
   */
  @Override
  protected void paintComponent(final Graphics g) {
    super.paintComponent(g); // Paints the background

    if (image == null) {
      return;
    }

    if (scale == 0.0) {
      initializeParams();
    }

    if (isHighQualityRendering()) {
      final Rectangle rect = getImageClipBounds();
      if (rect == null || rect.width == 0 || rect.height == 0) { // no part of image is displayed in the panel
        return;
      }

      final BufferedImage subimage = image.getSubimage(rect.x, rect.y, rect.width,
              rect.height);
      final Graphics2D g2 = (Graphics2D)g;
      g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, INTERPOLATION_TYPE);
      g2.drawImage(subimage, Math.max(0, originX), Math.max(0, originY),
              Math.min((int)(subimage.getWidth() * scale), getWidth()),
              Math.min((int)(subimage.getHeight() * scale), getHeight()), null);
    } else {
      g.drawImage(image, originX, originY, getScreenImageWidth(), getScreenImageHeight(), null);
    }

    //Draw navigation image
    if (navigationImageEnabled) {
      g.drawImage(navigationImage, 0, 0, getScreenNavImageWidth(), getScreenNavImageHeight(), null);
      drawZoomAreaOutline(g);
    }
  }

  //Paints a white outline over the navigation image indicating
  //the area of the image currently displayed in the panel.
  private void drawZoomAreaOutline(final Graphics g) {
    if (isFullImageInPanel()) {
      return;
    }

    final int x = -originX * getScreenNavImageWidth() / getScreenImageWidth();
    final int y = -originY * getScreenNavImageHeight() / getScreenImageHeight();
    final int width = getWidth() * getScreenNavImageWidth() / getScreenImageWidth();
    final int height = getHeight() * getScreenNavImageHeight() / getScreenImageHeight();
    g.setColor(Color.white);
    g.drawRect(x, y, width, height);
  }

  private int getScreenImageWidth() {
    return (int)(scale * image.getWidth());
  }

  private int getScreenImageHeight() {
    return (int)(scale * image.getHeight());
  }

  private int getScreenNavImageWidth() {
    return (int)(navScale * navImageWidth);
  }

  private int getScreenNavImageHeight() {
    return (int)(navScale * navImageHeight);
  }
}
