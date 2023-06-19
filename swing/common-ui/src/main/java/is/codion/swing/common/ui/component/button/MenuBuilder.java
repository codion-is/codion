/*
 * Copyright (c) 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.button;

import is.codion.swing.common.ui.component.ComponentBuilder;
import is.codion.swing.common.ui.control.Controls;

import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JPopupMenu;
import javax.swing.event.MenuListener;
import javax.swing.event.PopupMenuListener;

import static java.util.Objects.requireNonNull;

/**
 * A builder for menus.
 */
public interface MenuBuilder extends ComponentBuilder<Void, JMenu, MenuBuilder>, MenuItemBuilder<JMenu, MenuBuilder> {

  /**
   * Adds all actions from the given {@link Controls} instance
   * @param controls the Controls instance
   * @return this builder instance
   */
  MenuBuilder controls(Controls controls);

  /**
   * Adds a separator
   * @return this builder instance
   */
  MenuBuilder separator();

  /**
   * @param menuListener the menu listener
   * @return this builder instance
   */
  MenuBuilder menuListener(MenuListener menuListener);

  /**
   * Has no effect if a popup menu is not created.
   * @param popupMenuListener the popup menu listener
   * @return this builder instance
   * @see #createPopupMenu()
   */
  MenuBuilder popupMenuListener(PopupMenuListener popupMenuListener);

  /**
   * @param menuItemBuilder the menu item builder to use when creating menu items
   * @return this builder instance
   */
  MenuBuilder menuItemBuilder(MenuItemBuilder<?, ?> menuItemBuilder);

  /**
   * @param toggleMenuItemBuilder the toggle menu item builder to use when creating toggle menu items
   * @return this builder instance
   */
  MenuBuilder toggleMenuItemBuilder(ToggleMenuItemBuilder<?, ?> toggleMenuItemBuilder);

  /**
   * @return a new JPopupMenu based on this menu builder
   */
  JPopupMenu createPopupMenu();

  /**
   * @return a new JMenuBar based on this menu builder
   */
  JMenuBar createMenuBar();

  /**
   * @return a new MenuBuilder
   */
  static MenuBuilder builder() {
    return new DefaultMenuBuilder(null);
  }

  /**
   * @param controls the controls to base the menu on
   * @return a new MenuBuilder based on the given controls
   */
  static MenuBuilder builder(Controls controls) {
    return new DefaultMenuBuilder(requireNonNull(controls));
  }

  /**
   * @param controlsBuilder the controls builder to base the menu on
   * @return a new MenuBuilder based on the given controls
   */
  static MenuBuilder builder(Controls.Builder controlsBuilder) {
    return new DefaultMenuBuilder(requireNonNull(controlsBuilder).build());
  }
}
