/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui.icons;

import org.jminor.common.Configuration;
import org.jminor.common.value.PropertyValue;

import javax.swing.ImageIcon;
import java.util.Objects;
import java.util.ServiceLoader;

/**
 * Provides icons for common ui components.
 */
public interface Icons {

  PropertyValue<String> ICONS_CLASSNAME = Configuration.stringValue("org.jminor.swing.iconsClassName", DefaultIcons.class.getName());

  /**
   * @return icon for the 'filter' action.
   */
  ImageIcon filter();

  /**
   * @return icon for the 'configure' action.
   */
  ImageIcon configure();

  /**
   * @return icon for the jminor logo
   */
  ImageIcon logo();

  /**
   * @return icon for the jminor logo in red
   */
  ImageIcon logoRed();

  /**
   * @return a {@link Icons} implementation of the type specified by
   * {@link Icons#ICONS_CLASSNAME}.
   * @throws IllegalArgumentException in case no such implementation is found
   */
  static Icons icons() {
    final String iconsClassName = ICONS_CLASSNAME.get();
    final ServiceLoader<Icons> loader = ServiceLoader.load(Icons.class);
    for (final Icons icons : loader) {
      if (Objects.equals(icons.getClass().getName(), iconsClassName)) {
        return icons;
      }
    }

    throw new IllegalArgumentException("No Icons implementation available of type: " + iconsClassName);
  }
}
