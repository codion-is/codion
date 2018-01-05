/*
 * Chinook.Copyright (c) 2004 - 2018, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui.images;

import javax.swing.ImageIcon;
import java.awt.Toolkit;
import java.net.URL;
import java.util.Objects;

/**
 * A class serving as resource owner for images.
 */
public final class Images {

  public static final String IMG_ADD_16 = "Add16.gif";
  public static final String IMG_DELETE_16 = "Delete16.gif";
  public static final String IMG_SAVE_16 = "Save16.gif";
  public static final String IMG_FILTER_16 = "Filter16.gif";
  public static final String IMG_PREFERENCES_16 = "Preferences16.gif";
  public static final String IMG_REFRESH_16 = "Refresh16.gif";
  public static final String IMG_NEW_16 = "New16.gif";
  public static final String IMG_UP_16 = "Up16.gif";
  public static final String IMG_DOWN_16 = "Down16.gif";
  public static final String IMG_STOP_16 = "Stop16.gif";
  public static final String IMG_HISTORY_16 = "History16.gif";
  public static final String ICON_LOGGING = "icon_print_queries.gif";

  private Images() {}

  /**
   * Loads the given image assuming it is on the classpath on the same level as this class
   * @param imageFileName the image name
   * @return an ImageIcon based on the given image
   */
  public static ImageIcon loadImage(final String imageFileName) {
    return getImageIcon(Images.class, imageFileName);
  }

  /**
   * Loads an image from the given resource assuming it is on the classpath on the same level as the given class
   * @param resourceOwnerClass the class owning the given resource
   * @param resourceName the image name
   * @return an ImageIcon based on the given image
   */
  public static ImageIcon getImageIcon(final Class resourceOwnerClass, final String resourceName) {
    final URL url = resourceOwnerClass.getResource(resourceName);
    Objects.requireNonNull(url, "Resource: " + resourceName + " for " + resourceOwnerClass);

    return new ImageIcon(Toolkit.getDefaultToolkit().getImage(url));
  }
}
