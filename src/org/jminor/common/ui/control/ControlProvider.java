/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui.control;

import org.jminor.common.model.Event;
import org.jminor.common.model.State;

import javax.swing.*;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

/**
 * Provides UI controls based on the Control class and it's descendants.
 */
public class ControlProvider {

  public static void bindItemSelector(final JComboBox combo, final Object owner, final String property,
                                      final Class propertyClass, final Event changedEvent) {
    new SelectedItemBeanValueLink(combo, owner, property, propertyClass, changedEvent, LinkType.READ_WRITE);
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

  public static JCheckBoxMenuItem createCheckBoxMenuItem(final ToggleBeanValueLink propertyLink) {
    final JCheckBoxMenuItem box = new JCheckBoxMenuItem(propertyLink);
    box.setModel(propertyLink.getButtonModel());

    return box;
  }

  public static JRadioButtonMenuItem createRadioButtonMenuItem(final ToggleBeanValueLink propertyLink) {
    final JRadioButtonMenuItem box = new JRadioButtonMenuItem(propertyLink);
    box.setModel(propertyLink.getButtonModel());

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
    public void handleToggleControl(final ToggleBeanValueLink control) {
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
        enabledState.eventStateChanged().addListener(new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            menu.setEnabled(enabledState.isActive());
          }
        });
      }
      final Icon icon = controlSet.getIcon();
      if (icon != null)
        menu.setIcon(icon);
      final int mnemonic = controlSet.getMnemonic();
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
    public void handleToggleControl(final ToggleBeanValueLink control) {
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
    public void handleToggleControl(final ToggleBeanValueLink control) {
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

  public static JCheckBox createCheckBox(final ToggleBeanValueLink propertyLink) {
    final JCheckBox checkBox = new JCheckBox(propertyLink);
    checkBox.setModel(propertyLink.getButtonModel());

    return checkBox;
  }

  public static JToggleButton createToggleButton(final ToggleBeanValueLink propertyLink) {
    return createToggleButton(propertyLink, true);
  }

  public static JToggleButton createToggleButton(final ToggleBeanValueLink propertyLink, final boolean includeCaption) {
    final JToggleButton toggleButton = new JToggleButton(propertyLink);
    toggleButton.setModel(propertyLink.getButtonModel());
    toggleButton.setText(includeCaption ? propertyLink.getName() : null);

    return toggleButton;
  }

  public static void iterate(final ControlIterator controlIterator, final ControlSet controlSet) {
    if (controlIterator == null)
      throw new IllegalArgumentException("Iterator can't be null");

    for (final Action action : controlSet.getActions()) {
      if (action == null)
        controlIterator.handleSeparator();
      else if (action instanceof ToggleBeanValueLink)
        controlIterator.handleToggleControl((ToggleBeanValueLink) action);
      else if (action instanceof ControlSet)
        controlIterator.handleControlSet((ControlSet) action);
      else if (action instanceof Control)
        controlIterator.handleControl((Control) action);
      else
        controlIterator.handleAction(action);
    }
  }
}
