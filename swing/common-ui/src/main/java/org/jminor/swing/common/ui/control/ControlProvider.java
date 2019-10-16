/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
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

import static java.util.Objects.requireNonNull;

/**
 * Provides UI controls based on the Control class and its descendants.
 */
public final class ControlProvider {

  private ControlProvider() {}

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
    final JPanel panel = new JPanel(new GridLayout(0, 1, 5, 5));
    iterate(new ButtonControlIterator(panel, true), controlSet);

    return panel;
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
    final JPanel panel = new JPanel(new GridLayout(1, 0, 5, 5));
    iterate(new ButtonControlIterator(panel, false), controlSet);

    return panel;
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
  public static JCheckBoxMenuItem createCheckBoxMenuItem(final Controls.ToggleControl toggleControl) {
    final JCheckBoxMenuItem box = new JCheckBoxMenuItem(toggleControl);
    box.setModel(toggleControl.getButtonModel());

    return box;
  }

  /**
   * @param toggleControl the toggle control
   * @return a radio button menu item based on the control
   */
  public static JRadioButtonMenuItem createRadioButtonMenuItem(final Controls.ToggleControl toggleControl) {
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

    private final JPanel panel;
    private final boolean vertical;

    private ButtonControlIterator(final JPanel panel, final boolean vertical) {
      this.panel = panel;
      this.vertical = vertical;
    }

    @Override
    public void handleSeparator() {
      panel.add(new JLabel());
    }

    @Override
    public void handleControl(final Control control) {
      if (control instanceof Controls.ToggleControl) {
        panel.add(createCheckBox((Controls.ToggleControl) control));
      }
      else {
        panel.add(new JButton(control));
      }
    }

    @Override
    public void handleControlSet(final ControlSet controlSet) {
      if (vertical) {
        createVerticalButtonPanel(panel, controlSet);
      }
      else {
        createHorizontalButtonPanel(panel, controlSet);
      }
    }

    @Override
    public void handleAction(final Action action) {
      panel.add(new JButton(action));
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
        menu.setEnabled(enabledState.get());
        enabledState.addListener(() -> menu.setEnabled(enabledState.get()));
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
      if (control instanceof Controls.ToggleControl) {
        menu.add(createCheckBoxMenuItem((Controls.ToggleControl) control));
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

    private final JToolBar toolbar;
    private final boolean includeCaption;

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
      if (control instanceof Controls.ToggleControl) {
        toolbar.add(createToggleButton((Controls.ToggleControl) control, includeCaption));
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

  /**
   * Creates a JCheckBox based on the given toggle control
   * @param toggleControl the toggle control
   * @return a check box
   */
  public static JCheckBox createCheckBox(final Controls.ToggleControl toggleControl) {
    final JCheckBox checkBox = new JCheckBox(toggleControl);
    checkBox.setModel(toggleControl.getButtonModel());

    return checkBox;
  }

  /**
   * Creates a JToggleButton based on the given toggle control
   * @param toggleControl the toggle control
   * @return a toggle button
   */
  public static JToggleButton createToggleButton(final Controls.ToggleControl toggleControl) {
    return createToggleButton(toggleControl, true);
  }

  /**
   * Creates a JToggleButton based on the given toggle control
   * @param toggleControl the toggle control
   * @param includeCaption if true a caption is included
   * @return a toggle button
   */
  public static JToggleButton createToggleButton(final Controls.ToggleControl toggleControl, final boolean includeCaption) {
    final JToggleButton toggleButton = new JToggleButton(toggleControl);
    toggleButton.setModel(toggleControl.getButtonModel());
    toggleButton.setText(includeCaption ? toggleControl.getName() : null);

    return toggleButton;
  }

  /**
   * Iterates the given control set using the given control iterator
   * @param controlIterator the control iterator
   * @param controlSet the control set
   */
  private static void iterate(final ControlIterator controlIterator, final ControlSet controlSet) {
    requireNonNull(controlIterator, "controlIterator");
    requireNonNull(controlSet, "controlSet");
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
