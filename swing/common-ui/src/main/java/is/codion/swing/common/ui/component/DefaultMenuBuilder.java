/*
 * Copyright (c) 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import is.codion.swing.common.ui.component.button.CheckBoxMenuItemBuilder;
import is.codion.swing.common.ui.component.button.MenuItemBuilder;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.common.ui.control.ToggleControl;

import javax.swing.Action;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JPopupMenu;

import static java.util.Objects.requireNonNull;

final class DefaultMenuBuilder extends AbstractComponentBuilder<Void, JMenu, MenuBuilder> implements MenuBuilder {

  private final Controls controls;

  DefaultMenuBuilder(Controls controls) {
    this.controls = controls == null ? Controls.controls() : controls;
  }

  @Override
  public MenuBuilder action(Action action) {
    this.controls.add(requireNonNull(action));
    return this;
  }

  @Override
  public MenuBuilder controls(Controls controls) {
    this.controls.add(requireNonNull(controls));
    return this;
  }

  @Override
  public MenuBuilder separator() {
    this.controls.addSeparator();
    return this;
  }

  @Override
  public JPopupMenu createPopupMenu() {
    return createComponent().getPopupMenu();
  }

  @Override
  public JMenuBar createMenuBar() {
    JMenuBar menuBar = new JMenuBar();
    controls.actions().stream()
            .filter(Controls.class::isInstance)
            .map(Controls.class::cast)
            .filter(controls -> !controls.isEmpty())
            .forEach(subControls -> menuBar.add(new DefaultMenuBuilder(subControls).createComponent()));

    return menuBar;
  }

  @Override
  protected JMenu createComponent() {
    JMenu menu = new JMenu(controls);
    new MenuControlHandler(menu, controls);

    return menu;
  }

  @Override
  protected ComponentValue<Void, JMenu> createComponentValue(JMenu component) {
    throw new UnsupportedOperationException("A ComponentValue can not be based on a JMenu");
  }

  @Override
  protected void setInitialValue(JMenu component, Void initialValue) {}

  private static final class MenuControlHandler extends ControlHandler {

    private final JMenu menu;

    MenuControlHandler(JMenu menu, Controls controls) {
      this.menu = menu;
      controls.actions().forEach(this);
    }

    @Override
    void onSeparator() {
      menu.addSeparator();
    }

    @Override
    void onControl(Control control) {
      menu.add(MenuItemBuilder.builder(control).build());
    }

    @Override
    void onToggleControl(ToggleControl toggleControl) {
      menu.add(CheckBoxMenuItemBuilder.builder(toggleControl).build());
    }

    @Override
    void onControls(Controls controls) {
      if (!controls.isEmpty()) {
        JMenu subMenu = new JMenu(controls);
        new MenuControlHandler(subMenu, controls);
        this.menu.add(subMenu);
      }
    }

    @Override
    void onAction(Action action) {
      menu.add(action);
    }
  }
}