/*
 * Copyright (c) 2020 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.icon;

import is.codion.common.Configuration;
import is.codion.common.properties.PropertyValue;

import org.kordamp.ikonli.Ikon;

import javax.swing.ImageIcon;
import javax.swing.UIManager;
import java.awt.Color;

/**
 * Provides icons for ui components.
 * The icon color follows the 'Button.foreground' color of the current Look and feel.
 * Add custom icons via {@link #add(Ikon...)} (Ikon)} and retrieve them via {@link #icon(Ikon)}.
 * @see #icons()
 */
public interface Icons {

  int DEFAULT_ICON_SIZE = 16;

  /**
   * The icon size, note that this will affect the size of buttons<br>
   * Value type: Integer<br>
   * Default value: 16
   */
  PropertyValue<Integer> ICON_SIZE = Configuration.integerValue("codion.swing.iconSize", DEFAULT_ICON_SIZE);

  /**
   * The icon color<br>
   * Value type: Color<br>
   * Default value: UIManager.getColor("Button.foreground")
   */
  PropertyValue<Color> ICON_COLOR = Configuration.value("codion.swing.iconColor", Color::decode, UIManager.getColor("Button.foreground"));

  /**
   * Adds the given ikons to this FrameworkIcons instance. Retrieve an icon via {@link #icon(Ikon)}.
   * @param ikons the ikons to add
   * @throws IllegalArgumentException in case an icon has already been associated with any of the given ikons
   */
  void add(Ikon... ikons);

  /**
   * Retrieves the ImageIcon associated with the given ikon from this FrameworkIcons instance.
   * @param ikon the ikon
   * @return the ImageIcon associated with the given ikon
   * @throws IllegalArgumentException in case no icon has been associated with the given ikon
   * @see #add(Ikon...)
   */
  ImageIcon icon(Ikon ikon);

  /**
   * @return a new {@link Icons} instance
   */
  static Icons icons() {
    return new DefaultIcons();
  }
}