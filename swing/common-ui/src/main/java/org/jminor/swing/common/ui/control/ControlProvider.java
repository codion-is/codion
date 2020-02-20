/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui.control;

import org.jminor.common.state.StateObserver;
import org.jminor.common.value.Value;
import org.jminor.swing.common.model.checkbox.NullableToggleButtonModel;
import org.jminor.swing.common.ui.checkbox.NullableCheckBox;
import org.jminor.swing.common.ui.layout.Layouts;
import org.jminor.swing.common.ui.value.BooleanValues;

import javax.swing.Action;
import javax.swing.ButtonModel;
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
import java.util.List;
import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;

/**
 * Provides UI controls based on the Control class and its descendants.
 */
public final class ControlProvider {

  private ControlProvider() {}

  /**
   * Creates a vertically laid out panel of buttons from a control set
   * @param controlSet the control set
   * @return the button panel
   */
  public static JPanel createVerticalButtonPanel(final ControlSet controlSet) {
    final JPanel panel = new JPanel(Layouts.createGridLayout(0, 1));
    controlSet.getActions().forEach(new ButtonControlHandler(panel, true));

    return panel;
  }

  /**
   * Creates a horizontally laid out panel of buttons from a control set
   * @param controlSet the control set
   * @return the button panel
   */
  public static JPanel createHorizontalButtonPanel(final ControlSet controlSet) {
    final JPanel panel = new JPanel(Layouts.createGridLayout(1, 0));
    controlSet.getActions().forEach(new ButtonControlHandler(panel, false));

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
    final MenuControlHandler controlHandler = new MenuControlHandler(controlSet);
    controlSet.getActions().forEach(controlHandler);

    return controlHandler.menu;
  }

  /**
   * @param toggleControl the toggle control
   * @return a check box menu item based on the control
   */
  public static JCheckBoxMenuItem createCheckBoxMenuItem(final ToggleControl toggleControl) {
    final JCheckBoxMenuItem item = new JCheckBoxMenuItem(toggleControl);
    item.setModel(createButtonModel(toggleControl));

    return item;
  }

  /**
   * @param toggleControl the toggle control
   * @return a radio button menu item based on the control
   */
  public static JRadioButtonMenuItem createRadioButtonMenuItem(final ToggleControl toggleControl) {
    final JRadioButtonMenuItem item = new JRadioButtonMenuItem(toggleControl);
    item.setModel(createButtonModel(toggleControl));

    return item;
  }

  /**
   * Creates a JToolBar populated with the given controls.
   * @param controlSet the controls
   * @param orientation the toolbar orientation
   * @return a toolbar based on the given controls
   */
  public static JToolBar createToolBar(final ControlSet controlSet, final int orientation) {
    final JToolBar toolBar = new JToolBar(orientation);
    populateToolBar(toolBar, controlSet);

    return toolBar;
  }

  /**
   * Adds the given controls to the given tool bar.
   * @param toolBar the toolbar to add the controls to
   * @param controlSet the controls
   */
  public static void populateToolBar(final JToolBar toolBar, final ControlSet controlSet) {
    controlSet.getActions().forEach(new ToolBarControlHandler(toolBar));
  }

  /**
   * @param controlSets the controls
   * @return a menu bar based on the given controls
   */
  public static JMenuBar createMenuBar(final List<ControlSet> controlSets) {
    final JMenuBar menuBar = new JMenuBar();
    controlSets.forEach(controlSet -> populateMenuBar(menuBar, controlSet));

    return menuBar;
  }

  /**
   * @param controlSet the controls
   * @return a menu bar based on the given controls
   */
  public static JMenuBar createMenuBar(final ControlSet controlSet) {
    final JMenuBar menuBar = new JMenuBar();
    controlSet.getControlSets().forEach(subControlSet -> populateMenuBar(menuBar, subControlSet));

    return menuBar;
  }

  /**
   * @param menuBar the menubar to add the controls to
   * @param controlSet the controls
   * @return the menu bar with the added controls
   */
  public static JMenuBar populateMenuBar(final JMenuBar menuBar, final ControlSet controlSet) {
    menuBar.add(createMenu(controlSet));

    return menuBar;
  }

  /**
   * Creates a ButtonModel based on the given {@link ToggleControl}.
   * If the underlying value is nullable then a NullableButtonModel is returned.
   * @param toggleControl the toggle control on which to base the button model
   * @return a button model
   */
  public static ButtonModel createButtonModel(final ToggleControl toggleControl) {
    requireNonNull(toggleControl, "toggleControl");
    final Value<Boolean> value = toggleControl.getValue();
    final ButtonModel buttonModel;
    if (value.isNullable()) {
      buttonModel = new NullableToggleButtonModel(value.get());
    }
    else {
      buttonModel = new JToggleButton.ToggleButtonModel();
    }
    BooleanValues.booleanValueLink(buttonModel, value);
    buttonModel.setEnabled(toggleControl.getEnabledObserver().get());
    toggleControl.getEnabledObserver().addDataListener(buttonModel::setEnabled);
    toggleControl.addPropertyChangeListener(changeEvent -> {
      if (Action.MNEMONIC_KEY.equals(changeEvent.getPropertyName())) {
        buttonModel.setMnemonic((Integer) changeEvent.getNewValue());
      }
    });

    return buttonModel;
  }

  private static abstract class ControlHandler implements Consumer<Action> {

    @Override
    public final void accept(final Action action) {
      if (action == null) {
        onSeparator();
      }
      else if (action instanceof ControlSet) {
        onControlSet((ControlSet) action);
      }
      else if (action instanceof Control) {
        onControl((Control) action);
      }
      else {
        onAction(action);
      }
    }

    /**
     * Creates a separator
     */
    abstract void onSeparator();

    /**
     * Creates a component based on the given control
     * @param control the control
     */
    abstract void onControl(Control control);

    /**
     * Creates a component based on the given control set
     * @param controlSet the control set
     */
    abstract void onControlSet(ControlSet controlSet);

    /**
     * Creates a component base on the given action
     * @param action the action
     */
    abstract void onAction(Action action);
  }

  private static final class ButtonControlHandler extends ControlHandler {

    private final JPanel panel;
    private final boolean vertical;

    private ButtonControlHandler(final JPanel panel, final boolean vertical) {
      this.panel = panel;
      this.vertical = vertical;
    }

    @Override
    public void onSeparator() {
      panel.add(new JLabel());
    }

    @Override
    public void onControl(final Control control) {
      if (control instanceof ToggleControl) {
        panel.add(createCheckBox((ToggleControl) control));
      }
      else {
        panel.add(new JButton(control));
      }
    }

    @Override
    public void onControlSet(final ControlSet controlSet) {
      panel.add(vertical ? createVerticalButtonPanel(controlSet) : createHorizontalButtonPanel(controlSet));
    }

    @Override
    public void onAction(final Action action) {
      panel.add(new JButton(action));
    }
  }

  private static final class MenuControlHandler extends ControlHandler {

    private final JMenu menu;

    private MenuControlHandler(final ControlSet controlSet) {
      menu = new JMenu(controlSet.getName());
      final String description = controlSet.getDescription();
      if (description != null) {
        menu.setToolTipText(description);
      }
      final StateObserver enabledObserver = controlSet.getEnabledObserver();
      if (enabledObserver != null) {
        menu.setEnabled(enabledObserver.get());
        enabledObserver.addListener(() -> menu.setEnabled(enabledObserver.get()));
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

    @Override
    public void onSeparator() {
      menu.addSeparator();
    }

    @Override
    public void onControl(final Control control) {
      if (control instanceof ToggleControl) {
        menu.add(createCheckBoxMenuItem((ToggleControl) control));
      }
      else {
        menu.add(control);
      }
    }

    @Override
    public void onControlSet(final ControlSet controlSet) {
      final MenuControlHandler controlHandler = new MenuControlHandler(controlSet);
      controlSet.getActions().forEach(controlHandler);
      menu.add(controlHandler.menu);
    }

    @Override
    public void onAction(final Action action) {
      menu.add(action);
    }
  }

  private static final class ToolBarControlHandler extends ControlHandler {

    private final JToolBar toolbar;

    private ToolBarControlHandler(final JToolBar owner) {
      this.toolbar = owner;
    }

    @Override
    public void onSeparator() {
      toolbar.addSeparator();
    }

    @Override
    public void onControl(final Control control) {
      if (control instanceof ToggleControl) {
        toolbar.add(createToggleButton((ToggleControl) control));
      }
      else {
        toolbar.add(control);
      }
    }

    @Override
    public void onControlSet(final ControlSet controlSet) {
      controlSet.getActions().forEach(new ToolBarControlHandler(toolbar));
    }

    @Override
    public void onAction(final Action action) {
      toolbar.add(action);
    }
  }

  /**
   * Creates a JCheckBox based on the given toggle control
   * @param toggleControl the toggle control
   * @return a check box
   */
  public static JCheckBox createCheckBox(final ToggleControl toggleControl) {
    final ButtonModel buttonModel = createButtonModel(toggleControl);
    if (buttonModel instanceof NullableToggleButtonModel) {
      return new NullableCheckBox((NullableToggleButtonModel) buttonModel, toggleControl.getName());
    }

    final JCheckBox checkBox = new JCheckBox(toggleControl);
    checkBox.setModel(buttonModel);

    return checkBox;
  }

  /**
   * Creates a JToggleButton based on the given toggle control
   * @param toggleControl the toggle control
   * @return a toggle button
   */
  public static JToggleButton createToggleButton(final ToggleControl toggleControl) {
    requireNonNull(toggleControl, "toggleControl");
    final JToggleButton toggleButton = new JToggleButton(toggleControl);
    toggleButton.setModel(createButtonModel(toggleControl));
    toggleButton.setText(toggleControl.getName());

    return toggleButton;
  }
}
