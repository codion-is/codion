/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.icons;

import is.codion.common.Configuration;
import is.codion.common.value.PropertyValue;

import javax.swing.ImageIcon;
import java.awt.Color;
import java.util.Objects;
import java.util.ServiceLoader;

/**
 * Provides icons for common ui components.
 */
public interface Icons {

  int DEFAULT_ICON_SIZE = 16;

  /**
   * The icon size, note that this will affect the size of buttons<br>
   * Value type: Integer<br>
   * Default value: 16
   */
  PropertyValue<Integer> ICON_SIZE = Configuration.integerValue("is.codion.swing.iconSize", DEFAULT_ICON_SIZE);

  /**
   * The icon color<br>
   * Value type: Color<br>
   * Default value: Color.BLACK
   */
  PropertyValue<Color> ICON_COLOR = Configuration.value("is.codion.swing.iconColor", Color.BLACK, Color::decode);

  PropertyValue<String> ICONS_CLASSNAME = Configuration.stringValue("is.codion.swing.iconsClassName", DefaultIcons.class.getName());

  /**
   * @return icon for the 'filter' action.
   */
  ImageIcon filter();

  /**
   * @param size the icon size
   * @return icon for the 'filter' action.
   */
  ImageIcon filter(int size);

  /**
   * @return icon for the 'configure' action.
   */
  ImageIcon configure();

  /**
   * @param size the icon size
   * @return icon for the 'configure' action.
   */
  ImageIcon configure(int size);

  /**
   * @return icon for the codion logo
   */
  ImageIcon logoBlack();

  /**
   * @return icon for the codion logo
   */
  ImageIcon logoTransparent();

  /**
   * @return icon for the codion logo in red
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
