package is.codion.plugin.imagepanel;

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
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.Hashtable;
import java.util.ResourceBundle;

import static java.util.Arrays.asList;

/**
 * {@code NavigableImagePanel} is a lightweight container displaying
 * an image that can be zoomed in and out and panned with ease and simplicity,
 * using an adaptive rendering for high quality display and satisfactory performance.
 * <h2>Image</h2>
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
 * <h2>Zooming</h2>
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
 * and use {@code setZoom()} to change the zoom level.
 * Zooming is always around the point the mouse pointer is currently at, so that
 * this point (called a zooming center) remains stationary ensuring that the area
 * of an image we are zooming into does not disappear off the screen. The zooming center
 * stays at the same location on the screen and all other points move radially away from
 * it (when zooming in), or towards it (when zooming out). For programmatically
 * controlled zooming the zooming center is either specified when {@code setZoom()}
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
 * <h2>Navigation</h2>
 * {@code NavigableImagePanel} does not use scroll bars for navigation,
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
 * and use {@code getImageOrigin()} and
 * {@code setImageOrigin()} to move the image around the panel.
 * <h2>Rendering</h2>
 * {@code NavigableImagePanel} uses the Nearest Neighbor interpolation
 * for image rendering (default in Java).
 * When the scaled image becomes larger than the original image,
 * the Bilinear interpolation is applied, but only to the part
 * of the image which is displayed in the panel. This interpolation change threshold
 * can be controlled by adjusting the value of
 * {@code HIGH_QUALITY_RENDERING_SCALE_THRESHOLD}.
 * <p>
 * Author: Slav Boleslawski
 * http://today.java.net/pub/a/today/2007/03/27/navigable-image-panel.html
 */
public class NavigableImagePanel extends JPanel {

  private static final ResourceBundle MESSAGES = ResourceBundle.getBundle(NavigableImagePanel.class.getName());

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

  private static final double SCREEN_NAV_IMAGE_FACTOR = 0.15; // 15% of panel's width
  private static final double NAV_IMAGE_FACTOR = 0.3; // 30% of panel's width
  private static final double HIGH_QUALITY_RENDERING_SCALE_THRESHOLD = 1;
  private static final double DEFAULT_ZOOM_INCREMENT = 0.2;
  private static final Object INTERPOLATION_TYPE = RenderingHints.VALUE_INTERPOLATION_BILINEAR;

  private double zoomIncrement = DEFAULT_ZOOM_INCREMENT;
  private double zoomFactor = 1 + zoomIncrement;
  private double navZoomFactor = 1 + zoomIncrement;
  private BufferedImage image;
  private BufferedImage navigationImage;
  private int navImageWidth;
  private int navImageHeight;
  private double initialScale = 0;
  private double scale = 0;
  private double navScale = 0;
  private int originX = 0;
  private int originY = 0;
  private Point mousePosition;
  private Dimension previousPanelSize;
  private boolean navigationImageEnabled = true;
  private boolean highQualityRenderingEnabled = true;
  private boolean moveImageEnabled = true;

  private WheelZoomDevice wheelZoomDevice = null;
  private ButtonZoomDevice buttonZoomDevice = null;

  /**
   * <p>Creates a new navigable image panel with no default image and
   * the mouse scroll wheel as the zooming device.</p>
   */
  public NavigableImagePanel() {
    setOpaque(false);
    previousPanelSize = getSize();
    addComponentListener(new ComponentAdapter() {
      @Override
      public void componentResized(ComponentEvent e) {
        if (scale > 0.0) {
          if (isFullImageInPanel()) {
            centerImage();
          }
          else if (isImageEdgeInPanel()) {
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
      public void mousePressed(MouseEvent e) {
        if (SwingUtilities.isLeftMouseButton(e) && isInNavigationImage(e.getPoint())) {
          displayImageAt(e.getPoint());
        }
      }
    });

    addMouseMotionListener(new MouseMotionListener() {
      @Override
      public void mouseDragged(MouseEvent e) {
        if (moveImageEnabled && SwingUtilities.isLeftMouseButton(e)
                && !isInNavigationImage(e.getPoint())) {
          moveImage(e.getPoint());
        }
      }

      @Override
      public void mouseMoved(MouseEvent e) {
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
  public NavigableImagePanel(BufferedImage image) {
    setImage(image);
  }

  /**
   * Reads an image from the given path
   * @param imagePath the path, either file or http
   * @return the loaded image
   * @throws IOException in case of an exception
   */
  public static BufferedImage readImage(String imagePath) throws IOException {
    if (imagePath.toLowerCase().startsWith("http")) {
      return ImageIO.read(new URL(imagePath));
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
   * @return the image width
   */
  public final int getImageWidth() {
    return image.getWidth();
  }

  /**
   * @return the image height
   */
  public final int getImageHeight() {
    return image.getHeight();
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
   * @param newZoomDevice specifies the type of the new zoom device.
   */
  public final void setZoomDevice(ZoomDevice newZoomDevice) {
    if (newZoomDevice == ZoomDevice.NONE) {
      removeWheelZoomDevice();
      removeButtonZoomDevice();
    }
    else if (newZoomDevice == ZoomDevice.MOUSE_BUTTON) {
      removeWheelZoomDevice();
      addButtonZoomDevice();
    }
    else if (newZoomDevice == ZoomDevice.MOUSE_WHEEL) {
      removeButtonZoomDevice();
      addWheelZoomDevice();
    }
  }

  /**
   * <p>Gets the current zoom device.</p>
   * @return the ZoomDevice
   */
  public final ZoomDevice getZoomDevice() {
    if (buttonZoomDevice != null) {
      return ZoomDevice.MOUSE_BUTTON;
    }
    else if (wheelZoomDevice != null) {
      return ZoomDevice.MOUSE_WHEEL;
    }
    else {
      return ZoomDevice.NONE;
    }
  }

  /**
   * Called from paintComponent() when a new image is set.
   */
  private void initializeParams() {
    initializeScale();
    //An image is initially centered
    centerImage();
    if (navigationImageEnabled) {
      createNavigationImage();
    }
  }

  private void initializeScale() {
    double xScale = (double) getWidth() / image.getWidth();
    double yScale = (double) getHeight() / image.getHeight();
    initialScale = Math.min(xScale, yScale);
    scale = initialScale;
  }

  /**
   * Centers the current image in the panel.
   */
  private void centerImage() {
    originX = (getWidth() - getScreenImageWidth()) / 2;
    originY = (getHeight() - getScreenImageHeight()) / 2;
  }

  /**
   * Centers the image on the given image point
   * @param imagePoint the image point on which to center the image
   */
  public final void centerImage(Point2D.Double imagePoint) {
    centerImage(toPoint(imageToPanelPoint(imagePoint)));
  }

  /**
   * Centers the image on the given point on the panel, if it is within the image boundaries.
   * @param panelPoint the point on which to center the image
   */
  public final void centerImage(Point panelPoint) {
    if (isWithinImage(panelPoint)) {
      Point currentCenter = new Point(getWidth() / 2, getHeight() / 2);
      originX += (currentCenter.getX() - panelPoint.getX());
      originY += (currentCenter.getY() - panelPoint.getY());
      repaint();
    }
  }

  /**
   * Tests whether a given point in the panel falls within the image boundaries.
   * @param panelPoint the point on the panel
   * @return true if the given point is within the image
   */
  public final boolean isWithinImage(Point panelPoint) {
    Point2D.Double imagePoint = panelToImagePoint(panelPoint);
    double width = getImageWidth();
    double height = getImageHeight();

    return imagePoint.getX() >= 0 && imagePoint.getX() <= width && imagePoint.getY() >= 0 && imagePoint.getY() <= height;
  }

  /**
   * Creates and renders the navigation image in the upper let corner of the panel.
   */
  private void createNavigationImage() {
    //We keep the original navigation image larger than initially
    //displayed to allow for zooming into it without pixellation effect.
    navImageWidth = (int) (getWidth() * NAV_IMAGE_FACTOR);
    navImageHeight = navImageWidth * image.getHeight() / image.getWidth();
    int scrNavImageWidth = (int) (getWidth() * SCREEN_NAV_IMAGE_FACTOR);
    navScale = (double) scrNavImageWidth / navImageWidth;
    ColorModel colorModel = image.getColorModel();
    WritableRaster raster = colorModel.createCompatibleWritableRaster(navImageWidth, navImageHeight);
    navigationImage = new BufferedImage(colorModel, raster, false, getProperties(image));
    navigationImage.getGraphics().drawImage(image, 0, 0, navImageWidth, navImageHeight, null);
  }

  /**
   * <p>Sets an image for display in the panel.</p>
   * @param image an image to be set in the panel
   */
  public final void setImage(BufferedImage image) {
    BufferedImage oldImage = this.image;
    this.image = image;
    if (image != null) {
      initializeScale();
    }
    firePropertyChange(IMAGE_CHANGED_PROPERTY, oldImage, image);
    repaint();
  }

  /**
   * @return the image being displayed, null if none
   */
  public final BufferedImage getImage() {
    return image;
  }

  /**
   * @return the zoom scale
   */
  public final double getScale() {
    return scale;
  }

  /**
   * Resets the view so the image fits the panel
   */
  public final void resetView() {
    scale = 0.0;
    repaint();
  }

  /**
   * Converts this panel's point into the original image point
   * @param panelPoint the panel point
   * @return the image point
   */
  public final Point2D.Double panelToImagePoint(Point panelPoint) {
    return new Point2D.Double((panelPoint.x - originX) / scale, (panelPoint.y - originY) / scale);
  }

  /**
   * Converts the original image point into this panel's point
   * @param imagePoint the image point
   * @return the panel point
   */
  public final Point2D.Double imageToPanelPoint(Point2D.Double imagePoint) {
    return new Point2D.Double((imagePoint.x * scale) + originX, (imagePoint.y * scale) + originY);
  }

  /**
   * Converts the navigation image point into the zoomed image point
   */
  private Point navigationToZoomedImagePoint(Point panelPoint) {
    int x = panelPoint.x * getScreenImageWidth() / getScreenNavImageWidth();
    int y = panelPoint.y * getScreenImageHeight() / getScreenNavImageHeight();

    return new Point(x, y);
  }

  /**
   * The user clicked within the navigation image and this part of the image
   * is displayed in the panel.
   * The clicked point of the image is centered in the panel.
   * @param panelPoint the point
   */
  private void displayImageAt(Point panelPoint) {
    Point scrImagePoint = navigationToZoomedImagePoint(panelPoint);
    originX = -(scrImagePoint.x - getWidth() / 2);
    originY = -(scrImagePoint.y - getHeight() / 2);
    repaint();
  }

  /**
   * Tests whether a given point in the panel falls within the navigation image boundaries.
   * @param panelPoint the point in the panel
   * @return true if the given point is within the navigation image
   */
  private boolean isInNavigationImage(Point panelPoint) {
    return navigationImageEnabled && panelPoint.x < getScreenNavImageWidth() && panelPoint.y < getScreenNavImageHeight();
  }

  /**
   * Used when the image is resized.
   */
  private boolean isImageEdgeInPanel() {
    boolean originXOK = originX > 0 && originX < previousPanelSize.width;
    boolean originYOK = originY > 0 && originY < previousPanelSize.height;

    return previousPanelSize != null && (originXOK || originYOK);
  }

  /**
   * Tests whether the image is displayed in its entirety in the panel.
   * @return true if the image is fully within the panel bounds
   */
  private boolean isFullImageInPanel() {
    return originX >= 0 && (originX + getScreenImageWidth()) < getWidth()
            && originY >= 0 && (originY + getScreenImageHeight()) < getHeight();
  }

  /**
   * <p>Indicates whether the high quality rendering feature is enabled.</p>
   * @return true if high quality rendering is enabled, false otherwise.
   */
  public final boolean isHighQualityRenderingEnabled() {
    return highQualityRenderingEnabled;
  }

  /**
   * <p>Enables/disables high quality rendering.</p>
   * @param enabled enables/disables high quality rendering
   */
  public final void setHighQualityRenderingEnabled(boolean enabled) {
    highQualityRenderingEnabled = enabled;
  }

  /**
   * High quality rendering kicks in when a scaled image is larger
   * than the original image. In other words,
   * when image decimation stops and interpolation starts.
   * @return true if high quality rendering is enabled
   */
  private boolean isHighQualityRendering() {
    return highQualityRenderingEnabled && scale > HIGH_QUALITY_RENDERING_SCALE_THRESHOLD;
  }

  /**
   * <p>Indicates whether navigation image is enabled.<p>
   * @return true when navigation image is enabled, false otherwise.
   */
  public final boolean isNavigationImageEnabled() {
    return navigationImageEnabled;
  }

  /**
   * <p>Enables/disables navigation with the navigation image.</p>
   * <p>Navigation image should be disabled when custom, programmatic navigation
   * is implemented.</p>
   * @param enabled true when navigation image is enabled, false otherwise.
   */
  public final void setNavigationImageEnabled(boolean enabled) {
    navigationImageEnabled = enabled;
    repaint();
  }

  public final boolean isMoveImageEnabled() {
    return moveImageEnabled;
  }

  public final void setMoveImageEnabled(boolean moveImageEnabled) {
    this.moveImageEnabled = moveImageEnabled;
  }

  /**
   * Used when the panel is resized
   */
  private void scaleOrigin() {
    originX = originX * getWidth() / previousPanelSize.width;
    originY = originY * getHeight() / previousPanelSize.height;
    repaint();
  }

  /**
   * Converts the specified zoom level	to scale.
   * @param zoom the zoom level
   * @return the zoom scale
   */
  private double zoomToScale(double zoom) {
    return initialScale * zoom;
  }

  /**
   * <p>Gets the current zoom level.</p>
   * @return the current zoom level
   */
  public final double getZoom() {
    return scale / initialScale;
  }

  /**
   * <p>Sets the zoom level used to display the image.</p>
   * <p>This method is used in programmatic zooming. The zooming center is
   * the point of the image closest to the center of the panel.
   * After a new zoom level is set the image is repainted.</p>
   * @param newZoom the zoom level used to display this panel's image.
   */
  public final void setZoom(double newZoom) {
    setZoom(newZoom, new Point(getWidth() / 2, getHeight() / 2));
  }

  /**
   * <p>Sets the zoom level used to display the image, and the zooming center,
   * around which zooming is done.</p>
   * <p>This method is used in programmatic zooming.
   * After a new zoom level is set the image is repainted.</p>
   * @param newZoom the zoom level used to display this panel's image.
   * @param zoomingCenter the zooming center
   */
  public final void setZoom(double newZoom, Point zoomingCenter) {
    Point2D.Double imageP = panelToImagePoint(zoomingCenter);
    if (imageP.x < 0.0) {
      imageP = new Point2D.Double(0.0, imageP.getY());
    }
    if (imageP.y < 0.0) {
      imageP = new Point2D.Double(imageP.getX(), 0.0);
    }
    if (imageP.x >= image.getWidth()) {
      imageP = new Point2D.Double(image.getWidth() - 1d, imageP.getY());
    }
    if (imageP.y >= image.getHeight()) {
      imageP = new Point2D.Double(imageP.getX(), image.getHeight() - 1d);
    }

    Point2D.Double correctedP = imageToPanelPoint(imageP);
    double oldZoom = getZoom();
    scale = zoomToScale(newZoom);
    Point2D.Double panelP = imageToPanelPoint(imageP);

    originX += (Math.round(correctedP.getX()) - (int) panelP.x);
    originY += (Math.round(correctedP.getY()) - (int) panelP.y);

    firePropertyChange(ZOOM_LEVEL_CHANGED_PROPERTY, Double.valueOf(oldZoom), Double.valueOf(getZoom()));

    repaint();
  }

  /**
   * <p>Gets the current zoom increment.</p>
   * @return the current zoom increment
   */
  public final double getZoomIncrement() {
    return zoomIncrement;
  }

  /**
   * <p>Sets a new zoom increment value.</p>
   * @param newZoomIncrement new zoom increment value
   */
  public final void setZoomIncrement(double newZoomIncrement) {
    double oldZoomIncrement = zoomIncrement;
    zoomIncrement = newZoomIncrement;
    firePropertyChange(ZOOM_INCREMENT_CHANGED_PROPERTY,
            Double.valueOf(oldZoomIncrement), Double.valueOf(zoomIncrement));
  }

  /**
   * Zooms an image in the panel by repainting it at the new zoom level.
   * The current mouse position is the zooming center.
   */
  private void zoomImage() {
    Point2D.Double imagePoint = panelToImagePoint(mousePosition);
    double oldZoom = getZoom();
    scale *= zoomFactor;
    Point2D.Double panelPoint = imageToPanelPoint(imagePoint);

    originX += (mousePosition.x - (int) panelPoint.x);
    originY += (mousePosition.y - (int) panelPoint.y);

    firePropertyChange(ZOOM_LEVEL_CHANGED_PROPERTY, Double.valueOf(oldZoom), Double.valueOf(getZoom()));

    repaint();
  }

  /**
   * Zooms the navigation image
   */
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
  public final Point getImageOrigin() {
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
  public final void setImageOrigin(int x, int y) {
    setImageOrigin(new Point(x, y));
  }

  /**
   * <p>Sets the image origin.</p>
   * <p>Image origin is defined as the upper, left corner of the image in
   * the panel's coordinate system. After a new origin is set, the image is repainted.
   * This method is used for programmatic image navigation.</p>
   * @param newOrigin the value of a new image origin
   */
  public final void setImageOrigin(Point newOrigin) {
    originX = newOrigin.x;
    originY = newOrigin.y;
    repaint();
  }

  /**
   * Moves te image (by dragging with the mouse) to a new mouse position p.
   * @param panelPoint the point
   */
  private void moveImage(Point panelPoint) {
    int xDelta = panelPoint.x - mousePosition.x;
    int yDelta = panelPoint.y - mousePosition.y;
    originX += xDelta;
    originY += yDelta;
    mousePosition = panelPoint;
    repaint();
  }

  /**
   * @return the bounds of the image area currently displayed in the panel (in image coordinates).
   */
  private Rectangle getImageClipBounds() {
    Point2D.Double startPoint = panelToImagePoint(new Point(0, 0));
    Point2D.Double endPoint = panelToImagePoint(new Point(getWidth() - 1, getHeight() - 1));
    int panelX1 = (int) Math.round(startPoint.getX());
    int panelY1 = (int) Math.round(startPoint.getY());
    int panelX2 = (int) Math.round(endPoint.getX());
    int panelY2 = (int) Math.round(endPoint.getY());
    //No intersection?
    if (panelX1 >= image.getWidth() || panelX2 < 0 || panelY1 >= image.getHeight() || panelY2 < 0) {
      return null;
    }

    int x1 = Math.max(panelX1, 0);
    int y1 = Math.max(panelY1, 0);
    int x2 = (panelX2 >= image.getWidth()) ? image.getWidth() - 1 : panelX2;
    int y2 = (panelY2 >= image.getHeight()) ? image.getHeight() - 1 : panelY2;

    return new Rectangle(x1, y1, x2 - x1 + 1, y2 - y1 + 1);
  }

  /**
   * Paints the panel and its image at the current zoom level, location, and
   * interpolation method dependent on the image scale.
   * @param g the {@code Graphics} context for painting
   */
  @Override
  protected void paintComponent(Graphics g) {
    super.paintComponent(g); // Paints the background
    if (image == null) {
      return;
    }

    if (Double.compare(scale, 0) == 0) {
      initializeParams();
    }

    if (isHighQualityRendering()) {
      Rectangle rect = getImageClipBounds();
      if (rect == null || rect.width == 0 || rect.height == 0) { // no part of image is displayed in the panel
        return;
      }

      BufferedImage subimage = image.getSubimage(rect.x, rect.y, rect.width,
              rect.height);
      Graphics2D g2 = (Graphics2D) g;
      g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, INTERPOLATION_TYPE);
      g2.drawImage(subimage, Math.max(0, originX), Math.max(0, originY),
              Math.min((int) (subimage.getWidth() * scale), getWidth()),
              Math.min((int) (subimage.getHeight() * scale), getHeight()), null);
    }
    else {
      g.drawImage(image, originX, originY, getScreenImageWidth(), getScreenImageHeight(), null);
    }

    //Draw navigation image
    if (navigationImageEnabled) {
      g.drawImage(navigationImage, 0, 0, getScreenNavImageWidth(), getScreenNavImageHeight(), null);
      drawZoomAreaOutline(g);
    }
  }

  /**
   * Paints a white outline over the navigation image indicating
   * the area of the image currently displayed in the panel.
   * @param g the graphics
   */
  private void drawZoomAreaOutline(Graphics g) {
    if (isFullImageInPanel()) {
      return;
    }

    int x = -originX * getScreenNavImageWidth() / getScreenImageWidth();
    int y = -originY * getScreenNavImageHeight() / getScreenImageHeight();
    int width = getWidth() * getScreenNavImageWidth() / getScreenImageWidth();
    int height = getHeight() * getScreenNavImageHeight() / getScreenImageHeight();
    g.setColor(Color.white);
    g.drawRect(x, y, width, height);
  }

  private int getScreenImageWidth() {
    return (int) (scale * image.getWidth());
  }

  private int getScreenImageHeight() {
    return (int) (scale * image.getHeight());
  }

  private int getScreenNavImageWidth() {
    return (int) (navScale * navImageWidth);
  }

  private int getScreenNavImageHeight() {
    return (int) (navScale * navImageHeight);
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

    private final String zoomDeviceType;

    private ZoomDevice(String zoomDeviceType) {
      this.zoomDeviceType = zoomDeviceType;
    }

    @Override
    public String toString() {
      return zoomDeviceType;
    }
  }

  private final class WheelZoomDevice implements MouseWheelListener {

    @Override
    public void mouseWheelMoved(MouseWheelEvent event) {
      Point point = event.getPoint();
      boolean zoomIn = event.getWheelRotation() < 0;
      if (isInNavigationImage(point)) {
        if (zoomIn) {
          navZoomFactor = 1 + zoomIncrement;
        }
        else {
          navZoomFactor = 1 - zoomIncrement;
        }
        zoomNavigationImage();
      }
      else if (isWithinImage(point)) {
        if (zoomIn) {
          zoomFactor = 1 + zoomIncrement;
        }
        else {
          zoomFactor = 1 - zoomIncrement;
        }
        zoomImage();
      }
    }
  }

  private final class ButtonZoomDevice extends MouseAdapter {

    @Override
    public void mouseClicked(MouseEvent event) {
      Point point = event.getPoint();
      if (SwingUtilities.isRightMouseButton(event)) {
        if (isInNavigationImage(point)) {
          navZoomFactor = 1 - zoomIncrement;
          zoomNavigationImage();
        }
        else if (isWithinImage(point)) {
          zoomFactor = 1 - zoomIncrement;
          zoomImage();
        }
      }
      else {
        if (isInNavigationImage(point)) {
          navZoomFactor = 1 + zoomIncrement;
          zoomNavigationImage();
        }
        else if (isWithinImage(point)) {
          zoomFactor = 1 + zoomIncrement;
          zoomImage();
        }
      }
    }
  }
}
