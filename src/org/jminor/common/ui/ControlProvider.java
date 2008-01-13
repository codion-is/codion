/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui;

import org.jminor.common.model.Event;
import org.jminor.common.model.State;
import org.jminor.common.ui.control.Control;
import org.jminor.common.ui.control.ControlIterator;
import org.jminor.common.ui.control.ControlSet;
import org.jminor.common.ui.control.LinkType;
import org.jminor.common.ui.control.SelectedItemBeanPropertyLink;
import org.jminor.common.ui.control.ToggleBeanPropertyLink;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import java.awt.GridLayout;

/**
 * Provides UI controls
 */
public class ControlProvider {

  public static void bindItemSelector(final JComboBox combo, final Object owner, final String property,
                                      final Class propertyClass, final Event changedEvent, final State enableState) {
    new SelectedItemBeanPropertyLink(combo, owner, property, propertyClass, changedEvent, null,
            LinkType.READ_WRITE, enableState);
  }

  public static void bindToggleButtonAndProperty(final JToggleButton toggleButton, final Object owner, final String property,
                                                 final String label, final Event changedEvent, final State enableState) {
    final ToggleBeanPropertyLink propertyLink = new ToggleBeanPropertyLink(owner, property, changedEvent,
            label, LinkType.READ_WRITE, enableState);
    bindToggleButtonAndPropertyLink(toggleButton, propertyLink, label);
  }

  public static void bindToggleButtonAndPropertyLink(final JToggleButton toggleButton,
                                                     final ToggleBeanPropertyLink propertyLink, final String label) {
    toggleButton.setModel(propertyLink.getButtonModel());
    toggleButton.setAction(propertyLink);
    toggleButton.setText(label);
  }

  public static JButton createButton(final Control control) {
    return new JButton(control);
  }

  public static void createVerticalButtonPanel(final JComponent owner, final ControlSet controlSet) {
    owner.add(createVerticalButtonPanel(controlSet));
  }

  public static JPanel createVerticalButtonPanel(final ControlSet controlSet) {
    final JPanel btnPanel = new JPanel(new GridLayout(0,1,5,5));
    controlSet.iterate(new ButtonControlIterator(btnPanel, true));

    return btnPanel;
  }

  public static void createHorizontalButtonPanel(final JComponent owner, final ControlSet controlSet) {
    owner.add(createHorizontalButtonPanel(controlSet));
  }

  public static JPanel createHorizontalButtonPanel(final ControlSet controlSet) {
    final JPanel btnPanel = new JPanel(new GridLayout(1,0,5,5));
    controlSet.iterate(new ButtonControlIterator(btnPanel, false));

    return btnPanel;
  }

  public static JPopupMenu createPopupMenu(final ControlSet controlSet) {
    return createMenu(controlSet).getPopupMenu();
  }

  public static JMenu createMenu(final ControlSet controlSet) {
    final MenuControlIterator mv = new MenuControlIterator(controlSet);
    controlSet.iterate(mv);

    return mv.getMenu();
  }

  public static JCheckBoxMenuItem createCheckBoxMenuItem(final ToggleBeanPropertyLink propertyLink) {
    try {
      final JCheckBoxMenuItem box = new JCheckBoxMenuItem(propertyLink);
      box.setModel(propertyLink.getButtonModel());

      return box;
    }
    catch (Exception ex) {
      ex.printStackTrace();
      throw new RuntimeException(ex.toString());
    }
  }

  public static JRadioButtonMenuItem createRadioButtonMenuItem(final ToggleBeanPropertyLink propertyLink) {
    try {
      final JRadioButtonMenuItem box = new JRadioButtonMenuItem(propertyLink);
      box.setModel(propertyLink.getButtonModel());

      return box;
    }
    catch (Exception ex) {
      ex.printStackTrace();
      throw new RuntimeException(ex.toString());
    }
  }

  public static JToolBar createToolbar(final ControlSet controlSet, final int orientation) {
    final JToolBar toolBar = new JToolBar(orientation);
    createToolbar(toolBar, controlSet);

    return toolBar;
  }

  public static void createToolbar(final JToolBar owner, final ControlSet controlSet) {
    controlSet.iterate(new ToolBarControlIterator(owner));
  }

  public static JMenuBar createMenuBar(final ControlSet[] controlSets) {
    final JMenuBar menubar = new JMenuBar();
    for (final ControlSet set : controlSets)
      addControlSetToMenuBar(menubar, set);

    return menubar;
  }

  public static JMenuBar createMenuBar(final ControlSet controlSet) {
    final JMenuBar menubar = new JMenuBar();
    for (final ControlSet set : controlSet.getControlSets())
      addControlSetToMenuBar(menubar, set);

    return menubar;
  }

  public static JMenuBar addControlSetToMenuBar(final JMenuBar menuBar, final ControlSet controlSet) {
    menuBar.add(createMenu(controlSet));

    return menuBar;
  }

  private static class ButtonControlIterator implements ControlIterator {

    private final JPanel btnPanel;
    private final boolean vertical;

    public ButtonControlIterator(final JPanel btnPanel, final boolean vertical) {
      this.btnPanel = btnPanel;
      this.vertical = vertical;
    }

    /** {@inheritDoc} */
    public void doSeparator() {}

    /** {@inheritDoc} */
    public void doControl(final Control control) {
      btnPanel.add(createButton(control));
    }

    /** {@inheritDoc} */
    public void doToggleControl(final ToggleBeanPropertyLink control) {
      btnPanel.add(createCheckBox(control));
    }

    /** {@inheritDoc} */
    public void doControlSet(final ControlSet controlSet) {
      if (vertical)
        createVerticalButtonPanel(btnPanel, controlSet);
      else
        createHorizontalButtonPanel(btnPanel, controlSet);
    }

    /** {@inheritDoc} */
    public void doAction(final Action action) {
      btnPanel.add(new JButton(action));
    }
  }

  private static class MenuControlIterator implements ControlIterator {

    private final JMenu menu;

    public MenuControlIterator(final ControlSet controlSet) {
      menu = new JMenu(controlSet.getName());
      final String description = controlSet.getDescription();
      if (description != null)
        menu.setToolTipText(description);
      final State enabledState = controlSet.getEnabledState();
      if (enabledState != null)
        UiUtil.linkToEnabledState(enabledState, menu);
      final Icon icon = controlSet.getIcon();
      if (icon != null)
        menu.setIcon(icon);
      final int mnemonic = controlSet.getMnemonic();
      if (mnemonic != -1)
        menu.setMnemonic(mnemonic);
    }

    /**
     * @return Value for property 'menu'.
     */
    public JMenu getMenu() {
      return menu;
    }

    /** {@inheritDoc} */
    public void doSeparator() {
      menu.addSeparator();
    }

    /** {@inheritDoc} */
    public void doControl(final Control control) {
      menu.add(control);
    }

    /** {@inheritDoc} */
    public void doToggleControl(final ToggleBeanPropertyLink control) {
      menu.add(createCheckBoxMenuItem(control));
    }

    /** {@inheritDoc} */
    public void doControlSet(final ControlSet controlSet) {
      final MenuControlIterator mv = new MenuControlIterator(controlSet);
      controlSet.iterate(mv);
      menu.add(mv.getMenu());
    }

    /** {@inheritDoc} */
    public void doAction(final Action action) {
      menu.add(action);
    }
  }

  private static class ToolBarControlIterator implements ControlIterator {

    private boolean includeCaption = true;
    private final JToolBar toolbar;

    public ToolBarControlIterator(final JToolBar owner) {
      this(owner, true);
    }

    public ToolBarControlIterator(final JToolBar owner, boolean includeCaption) {
      this.toolbar = owner;
      this.includeCaption = includeCaption;
    }

    /** {@inheritDoc} */
    public void doSeparator() {
      toolbar.addSeparator();
    }

    /** {@inheritDoc} */
    public void doControl(final Control control) {
      toolbar.add(control);
    }

    /** {@inheritDoc} */
    public void doToggleControl(final ToggleBeanPropertyLink control) {
      toolbar.add(createToggleButton(control, includeCaption));
    }

    /** {@inheritDoc} */
    public void doControlSet(final ControlSet controlSet) {
      controlSet.iterate(new ToolBarControlIterator(toolbar));
    }

    /** {@inheritDoc} */
    public void doAction(final Action action) {
      toolbar.add(action);
    }
  }

  public static JCheckBox createCheckBox(final ToggleBeanPropertyLink propertyLink) {
    try {
      final JCheckBox ret = new JCheckBox(propertyLink);
      ret.setModel(propertyLink.getButtonModel());

      return ret;
    }
    catch (Exception ex) {
      ex.printStackTrace();
      throw new RuntimeException(ex.toString());
    }
  }

  public static JToggleButton createToggleButton(final ToggleBeanPropertyLink propertyLink) {
    return createToggleButton(propertyLink, true);
  }

  public static JToggleButton createToggleButton(final ToggleBeanPropertyLink propertyLink, final boolean includeCaption) {
    try {
      final JToggleButton ret = new JToggleButton();
      bindToggleButtonAndPropertyLink(ret, propertyLink, includeCaption ? propertyLink.getName() : null);

      return ret;
    }
    catch (Exception ex) {
      ex.printStackTrace();
      throw new RuntimeException(ex.toString());
    }
  }
}
