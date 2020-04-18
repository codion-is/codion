/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.framework.ui.icons;

import org.jminor.common.Configuration;
import org.jminor.common.value.PropertyValue;
import org.jminor.swing.common.ui.icons.Icons;

import javax.swing.ImageIcon;
import java.util.Objects;
import java.util.ServiceLoader;

/**
 * Provides icons for framework ui components.
 */
public interface FrameworkIcons extends Icons {

  PropertyValue<String> FRAMEWORK_ICONS_CLASSNAME = Configuration.stringValue("org.jminor.swing.frameworkIconsClassName", DefaultFrameworkIcons.class.getName());

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
   * @return a {@link FrameworkIcons} implementation of the type specified by
   * {@link FrameworkIcons#FRAMEWORK_ICONS_CLASSNAME}.
   * @throws IllegalArgumentException in case no such implementation is found
   */
  static FrameworkIcons frameworkIcons() {
    final String iconsClassName = FRAMEWORK_ICONS_CLASSNAME.get();
    try {
      final ServiceLoader<FrameworkIcons> loader = ServiceLoader.load(FrameworkIcons.class);
      for (final FrameworkIcons icons : loader) {
        if (Objects.equals(icons.getClass().getName(), iconsClassName)) {
          return icons;
        }
      }

      throw new IllegalArgumentException("No FrameworkIcons implementation available of type: " + iconsClassName);
    }
    catch (final Exception e) {
      throw new RuntimeException(e);
    }
  }
}
