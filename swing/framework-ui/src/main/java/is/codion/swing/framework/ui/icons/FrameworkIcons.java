/*
 * Copyright (c) 2020 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui.icons;

import is.codion.common.Configuration;
import is.codion.common.properties.PropertyValue;
import is.codion.swing.common.ui.icon.Logos;

import javax.swing.ImageIcon;
import javax.swing.UIManager;
import java.awt.Color;
import java.util.Objects;
import java.util.ServiceLoader;

/**
 * Provides icons for framework ui components.
 */
public interface FrameworkIcons extends Logos {

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

  PropertyValue<String> FRAMEWORK_ICONS_CLASSNAME = Configuration.stringValue("codion.swing.frameworkIconsClassName", DefaultFrameworkIcons.class.getName());

  /**
   * @return icon for the 'filter' action.
   */
  ImageIcon filter();

  /**
   * @return icon for the 'add' action.
   */
  ImageIcon add();

  /**
   * @return icon for the 'delete' action.
   */
  ImageIcon delete();

  /**
   * @return icon for the 'update' action.
   */
  ImageIcon update();

  /**
   * @return icon for the 'copy' action.
   */
  ImageIcon copy();

  /**
   * @return icon for the 'refresh' action.
   */
  ImageIcon refresh();

  /**
   * @return icon for the 'refresh' action.
   */
  ImageIcon refreshRequired();

  /**
   * @return icon for the 'clear' action.
   */
  ImageIcon clear();

  /**
   * @return icon for the 'up' action.
   */
  ImageIcon up();

  /**
   * @return icon for the 'down' action.
   */
  ImageIcon down();

  /**
   * @return icon for the 'detail' action.
   */
  ImageIcon detail();

  /**
   * @return icon for the 'print' action.
   */
  ImageIcon print();

  /**
   * @return icon for the 'clear selection' action.
   */
  ImageIcon clearSelection();

  /**
   * @return icon for the 'edit' action.
   */
  ImageIcon edit();

  /**
   * @return icon for the 'summary' action.
   */
  ImageIcon summary();

  /**
   * @return icon for the 'edit panel' action.
   */
  ImageIcon editPanel();

  /**
   * @return icon for the 'view dependencies' action.
   */
  ImageIcon dependencies();

  /**
   * @return icon for a 'settings' action.
   */
  ImageIcon settings();

  /**
   * @return the logo icon.
   */
  ImageIcon logo();

  /**
   * @param size the logo size
   * @return a logo icon
   */
  ImageIcon logo(int size);

  /**
   * @return a {@link FrameworkIcons} implementation of the type specified by
   * {@link FrameworkIcons#FRAMEWORK_ICONS_CLASSNAME}.
   * @throws IllegalArgumentException in case no such implementation is found
   */
  static FrameworkIcons instance() {
    String iconsClassName = FRAMEWORK_ICONS_CLASSNAME.get();
    ServiceLoader<FrameworkIcons> loader = ServiceLoader.load(FrameworkIcons.class);
    for (FrameworkIcons icons : loader) {
      if (Objects.equals(icons.getClass().getName(), iconsClassName)) {
        return icons;
      }
    }

    throw new IllegalArgumentException("No FrameworkIcons implementation available of type: " + iconsClassName);
  }
}
