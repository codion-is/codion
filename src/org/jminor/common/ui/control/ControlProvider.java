/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui.control;

import org.jminor.common.model.Event;
import org.jminor.common.model.State;

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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

/**
 * Provides UI controls
 */
public class ControlProvider {

  public static void bindItemSelector(final JComboBox combo, final Object owner, final String property,
                                      final Class propertyClass, final Event changedEvent) {
    new SelectedItemBeanPropertyLink(combo, owner, property, propertyClass, changedEvent, null, LinkType.READ_WRITE);
  }

  public static void bindToggleButtonAndProperty(final JToggleButton toggleButton, final Object owner, final String property,
                                                 final String label, final Event changedEvent) {
    final ToggleBeanPropertyLink propertyLink = new ToggleBeanPropertyLink(owner, property, changedEvent,
            label, LinkType.READ_WRITE);
    propertyLink.setButton(toggleButton);
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
    iterate(new ButtonControlIterator(btnPanel, true), controlSet);

    return btnPanel;
  }

  public static void createHorizontalButtonPanel(final JComponent owner, final ControlSet controlSet) {
    owner.add(createHorizontalButtonPanel(controlSet));
  }

  public static JPanel createHorizontalButtonPanel(final ControlSet controlSet) {
    final JPanel btnPanel = new JPanel(new GridLayout(1,0,5,5));
    iterate(new ButtonControlIterator(btnPanel, false), controlSet);

    return btnPanel;
  }

  public static JPopupMenu createPopupMenu(final ControlSet controlSet) {
    return createMenu(controlSet).getPopupMenu();
  }

  public static JMenu createMenu(final ControlSet controlSet) {
    final MenuControlIterator iterator = new MenuControlIterator(controlSet);
    iterate(iterator, controlSet);

    return iterator.getMenu();
  }

  public static JCheckBoxMenuItem createCheckBoxMenuItem(final ToggleBeanPropertyLink propertyLink) {
    final JCheckBoxMenuItem box = new JCheckBoxMenuItem(propertyLink);
    propertyLink.setButton(box);

    return box;
  }

  public static JRadioButtonMenuItem createRadioButtonMenuItem(final ToggleBeanPropertyLink propertyLink) {
    final JRadioButtonMenuItem box = new JRadioButtonMenuItem(propertyLink);
    propertyLink.setButton(box);

    return box;
  }

  public static JToolBar createToolbar(final ControlSet controlSet, final int orientation) {
    final JToolBar toolBar = new JToolBar(orientation);
    createToolbar(toolBar, controlSet);

    return toolBar;
  }

  public static void createToolbar(final JToolBar owner, final ControlSet controlSet) {
    iterate(new ToolBarControlIterator(owner), controlSet);
  }

  public static JMenuBar createMenuBar(final List<ControlSet> controlSets) {
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
    public void handleSeparator() {}

    /** {@inheritDoc} */
    public void handleControl(final Control control) {
      btnPanel.add(createButton(control));
    }

    /** {@inheritDoc} */
    public void handleToggleControl(final ToggleBeanPropertyLink control) {
      btnPanel.add(createCheckBox(control));
    }

    /** {@inheritDoc} */
    public void handleControlSet(final ControlSet controlSet) {
      if (vertical)
        createVerticalButtonPanel(btnPanel, controlSet);
      else
        createHorizontalButtonPanel(btnPanel, controlSet);
    }

    /** {@inheritDoc} */
    public void handleAction(final Action action) {
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
      if (enabledState != null) {
        menu.setEnabled(enabledState.isActive());
        enabledState.evtStateChanged.addListener(new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            menu.setEnabled(enabledState.isActive());
          }
        });
      }
      final Icon icon = controlSet.getIcon();
      if (icon != null)
        menu.setIcon(icon);
      final char mnemonic = controlSet.getMnemonic();
      if (mnemonic != -1)
        menu.setMnemonic(mnemonic);
    }

    /**
     * @return the JMenu
     */
    public JMenu getMenu() {
      return menu;
    }

    /** {@inheritDoc} */
    public void handleSeparator() {
      menu.addSeparator();
    }

    /** {@inheritDoc} */
    public void handleControl(final Control control) {
      menu.add(control);
    }

    /** {@inheritDoc} */
    public void handleToggleControl(final ToggleBeanPropertyLink control) {
      menu.add(createCheckBoxMenuItem(control));
    }

    /** {@inheritDoc} */
    public void handleControlSet(final ControlSet controlSet) {
      final MenuControlIterator mv = new MenuControlIterator(controlSet);
      iterate(mv, controlSet);
      menu.add(mv.getMenu());
    }

    /** {@inheritDoc} */
    public void handleAction(final Action action) {
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
    public void handleSeparator() {
      toolbar.addSeparator();
    }

    /** {@inheritDoc} */
    public void handleControl(final Control control) {
      toolbar.add(control);
    }

    /** {@inheritDoc} */
    public void handleToggleControl(final ToggleBeanPropertyLink control) {
      toolbar.add(createToggleButton(control, includeCaption));
    }

    /** {@inheritDoc} */
    public void handleControlSet(final ControlSet controlSet) {
      iterate(new ToolBarControlIterator(toolbar), controlSet);
    }

    /** {@inheritDoc} */
    public void handleAction(final Action action) {
      toolbar.add(action);
    }
  }

  public static JCheckBox createCheckBox(final ToggleBeanPropertyLink propertyLink) {
    final JCheckBox ret = new JCheckBox(propertyLink);
    propertyLink.setButton(ret);

    return ret;
  }

  public static JToggleButton createToggleButton(final ToggleBeanPropertyLink propertyLink) {
    return createToggleButton(propertyLink, true);
  }

  public static JToggleButton createToggleButton(final ToggleBeanPropertyLink propertyLink, final boolean includeCaption) {
    final JToggleButton ret = new JToggleButton();
    propertyLink.setButton(ret);
    ret.setText(includeCaption ? propertyLink.getName() : null);

    return ret;
  }

  public static void iterate(final ControlIterator controlIterator, final ControlSet controlSet) {
    if (controlIterator == null)
      throw new IllegalArgumentException("Iterator can't be null");

    for (final Action action : controlSet.getActions()) {
      if (action == null)
        controlIterator.handleSeparator();
      else if (action instanceof ToggleBeanPropertyLink)
        controlIterator.handleToggleControl((ToggleBeanPropertyLink) action);
      else if (action instanceof ControlSet)
        controlIterator.handleControlSet((ControlSet) action);
      else if (action instanceof Control)
        controlIterator.handleControl((Control) action);
      else
        controlIterator.handleAction(action);
    }
  }
}
