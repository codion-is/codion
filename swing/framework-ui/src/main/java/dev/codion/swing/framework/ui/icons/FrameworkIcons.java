/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package dev.codion.swing.framework.ui.icons;

import dev.codion.common.Configuration;
import dev.codion.common.value.PropertyValue;
import dev.codion.swing.common.ui.icons.Icons;

import javax.swing.ImageIcon;
import java.util.Objects;
import java.util.ServiceLoader;

/**
 * Provides icons for framework ui components.
 */
public interface FrameworkIcons extends Icons {

  PropertyValue<String> FRAMEWORK_ICONS_CLASSNAME = Configuration.stringValue("dev.codion.swing.frameworkIconsClassName", DefaultFrameworkIcons.class.getName());

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
   * @return a {@link FrameworkIcons} implementation of the type specified by
   * {@link FrameworkIcons#FRAMEWORK_ICONS_CLASSNAME}.
   * @throws IllegalArgumentException in case no such implementation is found
   */
  static FrameworkIcons frameworkIcons() {
    final String iconsClassName = FRAMEWORK_ICONS_CLASSNAME.get();
    final ServiceLoader<FrameworkIcons> loader = ServiceLoader.load(FrameworkIcons.class);
    for (final FrameworkIcons icons : loader) {
      if (Objects.equals(icons.getClass().getName(), iconsClassName)) {
        return icons;
      }
    }

    throw new IllegalArgumentException("No FrameworkIcons implementation available of type: " + iconsClassName);
  }
}
