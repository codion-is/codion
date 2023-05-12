/*
 * Copyright (c) 2020 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui.icons;

import is.codion.common.Configuration;
import is.codion.common.properties.PropertyValue;
import is.codion.swing.common.ui.icon.Logos;
import is.codion.swing.common.ui.icons.Icons;

import org.kordamp.ikonli.Ikon;

import javax.swing.ImageIcon;

/**
 * Provides icons for framework ui components.
 * The icon color follows the 'Button.foreground' color of the current Look and feel.
 * Add custom icons via {@link #add(Ikon...)} (Ikon)} and retrieve them via {@link #icon(Ikon)}.
 * @see #instance()
 */
public interface FrameworkIcons extends Icons, Logos {

  /**
   * Specifies the name of the {@link FrameworkIcons} implementation class to use.
   */
  PropertyValue<String> FRAMEWORK_ICONS_CLASSNAME = Configuration.stringValue("codion.swing.frameworkIconsClassName", DefaultFrameworkIcons.class.getName());

  /**
   * @return icon for the 'filter' action.
   */
  ImageIcon filter();

  /**
   * @return icon for the 'search' action.
   */
  ImageIcon search();

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
   * @return icon for the 'dependencies' action.
   */
  ImageIcon dependencies();

  /**
   * @return icon for a 'settings' action.
   */
  ImageIcon settings();

  /**
   * @return icon for a 'calendar' action
   */
  ImageIcon calendar();

  /**
   * @return icon for a 'editText' action
   */
  ImageIcon editText();

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
    return DefaultFrameworkIcons.instance();
  }
}
