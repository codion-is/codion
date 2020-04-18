/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui.images;

import javax.swing.ImageIcon;
import java.awt.Toolkit;
import java.net.URL;

import static java.util.Objects.requireNonNull;

/**
 * A class serving as resource owner for images.
 */
public final class Images {

  private Images() {}

  /**
   * Loads an image from the given resource assuming it is on the classpath on the same level as the given class
   * @param resourceOwnerClass the class owning the given resource
   * @param resourceName the image name
   * @return an ImageIcon based on the given image
   */
  public static ImageIcon loadIcon(final Class resourceOwnerClass, final String resourceName) {
    final URL url = resourceOwnerClass.getResource(resourceName);
    requireNonNull(url, "Resource: " + resourceName + " for " + resourceOwnerClass);

    return new ImageIcon(Toolkit.getDefaultToolkit().getImage(url));
  }
}
