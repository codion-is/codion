/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui.control;

import org.jminor.common.model.EventObserver;
import org.jminor.common.model.StateObserver;
import org.jminor.common.model.Util;

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
 * Provides UI controls based on the Control class and it's descendants.
 */
public final class ControlProvider {

  private ControlProvider() {}

  public static void bindItemSelector(final JComboBox combo, final Object owner, final String property,
                                      final Class propertyClass, final EventObserver changedEvent) {
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
    for (final ControlSet set : controlSets) {
      addControlSetToMenuBar(menubar, set);
    }

    return menubar;
  }

  public static JMenuBar createMenuBar(final ControlSet controlSet) {
    final JMenuBar menubar = new JMenuBar();
    for (final ControlSet set : controlSet.getControlSets()) {
      addControlSetToMenuBar(menubar, set);
    }

    return menubar;
  }

  public static JMenuBar addControlSetToMenuBar(final JMenuBar menuBar, final ControlSet controlSet) {
    menuBar.add(createMenu(controlSet));

    return menuBar;
  }

  private static class ButtonControlIterator implements ControlIterator {

    private final JPanel btnPanel;
    private final boolean vertical;

    ButtonControlIterator(final JPanel btnPanel, final boolean vertical) {
      this.btnPanel = btnPanel;
      this.vertical = vertical;
    }

    public void handleSeparator() {}

    public void handleControl(final Control control) {
      btnPanel.add(createButton(control));
    }

    public void handleToggleControl(final ToggleBeanValueLink control) {
      btnPanel.add(createCheckBox(control));
    }

    public void handleControlSet(final ControlSet controlSet) {
      if (vertical) {
        createVerticalButtonPanel(btnPanel, controlSet);
      }
      else {
        createHorizontalButtonPanel(btnPanel, controlSet);
      }
    }

    public void handleAction(final Action action) {
      btnPanel.add(new JButton(action));
    }
  }

  private static class MenuControlIterator implements ControlIterator {

    private final JMenu menu;

    MenuControlIterator(final ControlSet controlSet) {
      menu = new JMenu(controlSet.getName());
      final String description = controlSet.getDescription();
      if (description != null) {
        menu.setToolTipText(description);
      }
      final StateObserver enabledState = controlSet.getEnabledObserver();
      if (enabledState != null) {
        menu.setEnabled(enabledState.isActive());
        enabledState.addListener(new ActionListener() {
          public void actionPerformed(final ActionEvent e) {
            menu.setEnabled(enabledState.isActive());
          }
        });
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

    public void handleSeparator() {
      menu.addSeparator();
    }

    public void handleControl(final Control control) {
      menu.add(control);
    }

    public void handleToggleControl(final ToggleBeanValueLink control) {
      menu.add(createCheckBoxMenuItem(control));
    }

    public void handleControlSet(final ControlSet controlSet) {
      final MenuControlIterator mv = new MenuControlIterator(controlSet);
      iterate(mv, controlSet);
      menu.add(mv.menu);
    }

    public void handleAction(final Action action) {
      menu.add(action);
    }
  }

  private static class ToolBarControlIterator implements ControlIterator {

    private boolean includeCaption = true;
    private final JToolBar toolbar;

    ToolBarControlIterator(final JToolBar owner) {
      this(owner, true);
    }

    ToolBarControlIterator(final JToolBar owner, final boolean includeCaption) {
      this.toolbar = owner;
      this.includeCaption = includeCaption;
    }

    public void handleSeparator() {
      toolbar.addSeparator();
    }

    public void handleControl(final Control control) {
      toolbar.add(control);
    }

    public void handleToggleControl(final ToggleBeanValueLink control) {
      toolbar.add(createToggleButton(control, includeCaption));
    }

    public void handleControlSet(final ControlSet controlSet) {
      iterate(new ToolBarControlIterator(toolbar), controlSet);
    }

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
    Util.rejectNullValue(controlIterator, "controlIterator");
    for (final Action action : controlSet.getActions()) {
      if (action == null) {
        controlIterator.handleSeparator();
      }
      else if (action instanceof ToggleBeanValueLink) {
        controlIterator.handleToggleControl((ToggleBeanValueLink) action);
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
