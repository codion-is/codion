/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui.images;

import org.jminor.common.model.Util;

import javax.swing.ImageIcon;
import java.awt.Toolkit;
import java.net.URL;

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
  public static final String IMG_PROPERTIES_16 = "Properties16.gif";
  public static final String IMG_UP_16 = "Up16.gif";
  public static final String IMG_DOWN_16 = "Down16.gif";
  public static final String IMG_STOP_16 = "Stop16.gif";
  public static final String IMG_HISTORY_16 = "History16.gif";
  public static final String IMG_FIND_16 = "Find16.gif";
  public static final String ICON_SELECTION_FILTERS_DETAIL = "icon_selection_filters_detail.gif";
  public static final String ICON_CASCADE_REFRESH = "icon_cascade_refresh.gif";
  public static final String ICON_SMART_REFRESH = "icon_smart_refresh.gif";
  public static final String ICON_PRINT_QUERIES = "icon_print_queries.gif";
  public static final String ICON_CHECK_REFERENCES_ON_DELETE = "icon_check_references_on_delete.gif";

  private Images() {}

  public static ImageIcon loadImage(final String imageFileName) {
    return getImageIcon(Images.class, imageFileName);
  }

  public static ImageIcon getImageIcon(final Class resourceOwnerClass, final String resourceName) {
    final URL url = resourceOwnerClass.getResource(resourceName);
    Util.rejectNullValue(url, "Resource: " + resourceName + " for " + resourceOwnerClass + " not found");

    return new ImageIcon(Toolkit.getDefaultToolkit().getImage(url));
  }
}
