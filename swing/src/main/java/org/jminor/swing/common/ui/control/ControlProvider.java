/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui.control;

import org.jminor.common.StateObserver;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import java.awt.GridLayout;
import java.util.List;
import java.util.Objects;

/**
 * Provides UI controls based on the Control class and its descendants.
 */
public final class ControlProvider {

  private ControlProvider() {}

  /**
   * @param control the control
   * @return a button based on the given control
   */
  public static JButton createButton(final Control control) {
    return new JButton(control);
  }

  /**
   * Creates a vertically laid out panel of buttons from a control set and adds it to the panel
   * @param panel the panel
   * @param controlSet the control set
   */
  public static void createVerticalButtonPanel(final JPanel panel, final ControlSet controlSet) {
    panel.add(createVerticalButtonPanel(controlSet));
  }

  /**
   * Creates a vertically laid out panel of buttons from a control set
   * @param controlSet the control set
   * @return the button panel
   */
  public static JPanel createVerticalButtonPanel(final ControlSet controlSet) {
    final JPanel btnPanel = new JPanel(new GridLayout(0, 1, 5, 5));
    iterate(new ButtonControlIterator(btnPanel, true), controlSet);

    return btnPanel;
  }

  /**
   * Creates a horizontally laid out panel of buttons from a control set and adds it to the panel
   * @param panel the panel
   * @param controlSet the control set
   */
  public static void createHorizontalButtonPanel(final JPanel panel, final ControlSet controlSet) {
    panel.add(createHorizontalButtonPanel(controlSet));
  }

  /**
   * Creates a horizontally laid out panel of buttons from a control set
   * @param controlSet the control set
   * @return the button panel
   */
  public static JPanel createHorizontalButtonPanel(final ControlSet controlSet) {
    final JPanel btnPanel = new JPanel(new GridLayout(1, 0, 5, 5));
    iterate(new ButtonControlIterator(btnPanel, false), controlSet);

    return btnPanel;
  }

  /**
   * Creates a popup menu from the given controls
   * @param controlSet the control set
   * @return a popup menu based on the given controls
   */
  public static JPopupMenu createPopupMenu(final ControlSet controlSet) {
    return createMenu(controlSet).getPopupMenu();
  }

  /**
   * Creates a menu from the given controls
   * @param controlSet the control set
   * @return a menu based on the given controls
   */
  public static JMenu createMenu(final ControlSet controlSet) {
    final MenuControlIterator iterator = new MenuControlIterator(controlSet);
    iterate(iterator, controlSet);

    return iterator.getMenu();
  }

  /**
   * @param toggleControl the toggle control
   * @return a check box menu item based on the control
   */
  public static JCheckBoxMenuItem createCheckBoxMenuItem(final ToggleControl toggleControl) {
    final JCheckBoxMenuItem box = new JCheckBoxMenuItem(toggleControl);
    box.setModel(toggleControl.getButtonModel());

    return box;
  }

  /**
   * @param toggleControl the toggle control
   * @return a radio button menu item based on the control
   */
  public static JRadioButtonMenuItem createRadioButtonMenuItem(final ToggleControl toggleControl) {
    final JRadioButtonMenuItem box = new JRadioButtonMenuItem(toggleControl);
    box.setModel(toggleControl.getButtonModel());

    return box;
  }

  /**
   * @param controlSet the controls
   * @param orientation the toolbar orientation
   * @return a toolbar based on the given controls
   */
  public static JToolBar createToolbar(final ControlSet controlSet, final int orientation) {
    final JToolBar toolBar = new JToolBar(orientation);
    createToolbar(toolBar, controlSet);

    return toolBar;
  }

  /**
   * Adds the given controls to the given tool bar
   * @param toolBar the toolbar to add the controls to
   * @param controlSet the controls
   */
  public static void createToolbar(final JToolBar toolBar, final ControlSet controlSet) {
    iterate(new ToolBarControlIterator(toolBar), controlSet);
  }

  /**
   * @param controlSets the controls
   * @return a menu bar based on the given controls
   */
  public static JMenuBar createMenuBar(final List<ControlSet> controlSets) {
    final JMenuBar menubar = new JMenuBar();
    for (final ControlSet set : controlSets) {
      addControlSetToMenuBar(menubar, set);
    }

    return menubar;
  }

  /**
   * @param controlSet the controls
   * @return a menu bar based on the given controls
   */
  public static JMenuBar createMenuBar(final ControlSet controlSet) {
    final JMenuBar menubar = new JMenuBar();
    for (final ControlSet set : controlSet.getControlSets()) {
      addControlSetToMenuBar(menubar, set);
    }

    return menubar;
  }

  /**
   * @param menuBar the menubar to add the controls to
   * @param controlSet the controls
   * @return the menu bar with the added controls
   */
  public static JMenuBar addControlSetToMenuBar(final JMenuBar menuBar, final ControlSet controlSet) {
    menuBar.add(createMenu(controlSet));

    return menuBar;
  }

  private static final class ButtonControlIterator implements ControlIterator {

    private final JPanel btnPanel;
    private final boolean vertical;

    private ButtonControlIterator(final JPanel btnPanel, final boolean vertical) {
      this.btnPanel = btnPanel;
      this.vertical = vertical;
    }

    @Override
    public void handleSeparator() {
      btnPanel.add(new JLabel());
    }

    @Override
    public void handleControl(final Control control) {
      if (control instanceof ToggleControl) {
        btnPanel.add(createCheckBox((ToggleControl) control));
      }
      else {
        btnPanel.add(createButton(control));
      }
    }

    @Override
    public void handleControlSet(final ControlSet controlSet) {
      if (vertical) {
        createVerticalButtonPanel(btnPanel, controlSet);
      }
      else {
        createHorizontalButtonPanel(btnPanel, controlSet);
      }
    }

    @Override
    public void handleAction(final Action action) {
      btnPanel.add(new JButton(action));
    }
  }

  private static final class MenuControlIterator implements ControlIterator {

    private final JMenu menu;

    private MenuControlIterator(final ControlSet controlSet) {
      menu = new JMenu(controlSet.getName());
      final String description = controlSet.getDescription();
      if (description != null) {
        menu.setToolTipText(description);
      }
      final StateObserver enabledState = controlSet.getEnabledObserver();
      if (enabledState != null) {
        menu.setEnabled(enabledState.isActive());
        enabledState.addListener(() -> menu.setEnabled(enabledState.isActive()));
      }
      final Icon icon = controlSet.getIcon();
      if (icon != null) {
        menu.setIcon(icon);
      }
      final int mnemonic = controlSet.getMnemonic();
      if (mnemonic != -1) {
        menu.setMnemonic(mnemonic);
      }
    }

    /**
     * @return the JMenu
     */
    public JMenu getMenu() {
      return menu;
    }

    @Override
    public void handleSeparator() {
      menu.addSeparator();
    }

    @Override
    public void handleControl(final Control control) {
      if (control instanceof ToggleControl) {
        menu.add(createCheckBoxMenuItem((ToggleControl) control));
      }
      else {
        menu.add(control);
      }
    }

    @Override
    public void handleControlSet(final ControlSet controlSet) {
      final MenuControlIterator mv = new MenuControlIterator(controlSet);
      iterate(mv, controlSet);
      menu.add(mv.menu);
    }

    @Override
    public void handleAction(final Action action) {
      menu.add(action);
    }
  }

  private static final class ToolBarControlIterator implements ControlIterator {

    private boolean includeCaption = true;
    private final JToolBar toolbar;

    private ToolBarControlIterator(final JToolBar owner) {
      this(owner, true);
    }

    private ToolBarControlIterator(final JToolBar owner, final boolean includeCaption) {
      this.toolbar = owner;
      this.includeCaption = includeCaption;
    }

    @Override
    public void handleSeparator() {
      toolbar.addSeparator();
    }

    @Override
    public void handleControl(final Control control) {
      if (control instanceof ToggleControl) {
        toolbar.add(createToggleButton((ToggleControl) control, includeCaption));
      }
      else {
        toolbar.add(control);
      }
    }

    @Override
    public void handleControlSet(final ControlSet controlSet) {
      iterate(new ToolBarControlIterator(toolbar), controlSet);
    }

    @Override
    public void handleAction(final Action action) {
      toolbar.add(action);
    }
  }

  public static JCheckBox createCheckBox(final ToggleControl toggleControl) {
    final JCheckBox checkBox = new JCheckBox(toggleControl);
    checkBox.setModel(toggleControl.getButtonModel());

    return checkBox;
  }

  public static JToggleButton createToggleButton(final ToggleControl toggleControl) {
    return createToggleButton(toggleControl, true);
  }

  public static JToggleButton createToggleButton(final ToggleControl toggleControl, final boolean includeCaption) {
    final JToggleButton toggleButton = new JToggleButton(toggleControl);
    toggleButton.setModel(toggleControl.getButtonModel());
    toggleButton.setText(includeCaption ? toggleControl.getName() : null);

    return toggleButton;
  }

  public static void iterate(final ControlIterator controlIterator, final ControlSet controlSet) {
    Objects.requireNonNull(controlIterator, "controlIterator");
    for (final Action action : controlSet.getActions()) {
      if (action == null) {
        controlIterator.handleSeparator();
      }
      else if (action instanceof ControlSet) {
        controlIterator.handleControlSet((ControlSet) action);
      }
      else if (action instanceof Control) {
        controlIterator.handleControl((Control) action);
      }
      else {
        controlIterator.handleAction(action);
      }
    }
  }
}
