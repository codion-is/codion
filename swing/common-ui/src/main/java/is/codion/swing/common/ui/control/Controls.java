/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.control;

import is.codion.common.state.StateObserver;
import is.codion.common.value.Value;
import is.codion.swing.common.model.checkbox.NullableToggleButtonModel;
import is.codion.swing.common.ui.checkbox.NullableCheckBox;
import is.codion.swing.common.ui.layout.Layouts;
import is.codion.swing.common.ui.value.BooleanValues;

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
 * A factory class for UI controls based on the Control class and its descendants.
 */
public final class Controls {

  private Controls() {}

  /**
   * Creates a vertically laid out panel of buttons from a control list
   * @param controls the control list
   * @return the button panel
   */
  public static JPanel verticalButtonPanel(final ControlList controls) {
    final JPanel panel = new JPanel(Layouts.gridLayout(0, 1));
    controls.getActions().forEach(new ButtonControlHandler(panel, true));

    return panel;
  }

  /**
   * Creates a horizontally laid out panel of buttons from a control list
   * @param controls the control list
   * @return the button panel
   */
  public static JPanel horizontalButtonPanel(final ControlList controls) {
    final JPanel panel = new JPanel(Layouts.gridLayout(1, 0));
    controls.getActions().forEach(new ButtonControlHandler(panel, false));

    return panel;
  }

  /**
   * Creates a popup menu from the given controls
   * @param controls the control list
   * @return a popup menu based on the given controls
   */
  public static JPopupMenu popupMenu(final ControlList controls) {
    return menu(controls).getPopupMenu();
  }

  /**
   * Creates a menu from the given controls
   * @param controls the control list
   * @return a menu based on the given controls
   */
  public static JMenu menu(final ControlList controls) {
    final MenuControlHandler controlHandler = new MenuControlHandler(controls);
    controls.getActions().forEach(controlHandler);

    return controlHandler.menu;
  }

  /**
   * @param toggleControl the toggle control
   * @return a check box menu item based on the control
   */
  public static JCheckBoxMenuItem checkBoxMenuItem(final ToggleControl toggleControl) {
    final JCheckBoxMenuItem item = new JCheckBoxMenuItem(toggleControl);
    item.setModel(buttonModel(toggleControl));

    return item;
  }

  /**
   * @param toggleControl the toggle control
   * @return a radio button menu item based on the control
   */
  public static JRadioButtonMenuItem radioButtonMenuItem(final ToggleControl toggleControl) {
    final JRadioButtonMenuItem item = new JRadioButtonMenuItem(toggleControl);
    item.setModel(buttonModel(toggleControl));

    return item;
  }

  /**
   * Creates a JToolBar populated with the given controls.
   * @param controls the controls
   * @param orientation the toolbar orientation
   * @return a toolbar based on the given controls
   */
  public static JToolBar toolBar(final ControlList controls, final int orientation) {
    final JToolBar toolBar = new JToolBar(orientation);
    populateToolBar(toolBar, controls);

    return toolBar;
  }

  /**
   * Adds the given controls to the given tool bar.
   * @param toolBar the toolbar to add the controls to
   * @param controls the controls
   */
  public static void populateToolBar(final JToolBar toolBar, final ControlList controls) {
    controls.getActions().forEach(new ToolBarControlHandler(toolBar));
  }

  /**
   * @param controls the controls
   * @return a menu bar based on the given controls
   */
  public static JMenuBar menuBar(final List<ControlList> controls) {
    final JMenuBar menuBar = new JMenuBar();
    controls.forEach(controlList -> populateMenuBar(menuBar, controlList));

    return menuBar;
  }

  /**
   * @param controls the controls
   * @return a menu bar based on the given controls
   */
  public static JMenuBar menuBar(final ControlList controls) {
    final JMenuBar menuBar = new JMenuBar();
    controls.getControlLists().forEach(subControlList -> populateMenuBar(menuBar, subControlList));

    return menuBar;
  }

  /**
   * @param menuBar the menubar to add the controls to
   * @param controls the controls
   * @return the menu bar with the added controls
   */
  public static JMenuBar populateMenuBar(final JMenuBar menuBar, final ControlList controls) {
    menuBar.add(menu(controls));

    return menuBar;
  }

  /**
   * Creates a ButtonModel based on the given {@link ToggleControl}.
   * If the underlying value is nullable then a NullableButtonModel is returned.
   * @param toggleControl the toggle control on which to base the button model
   * @return a button model
   */
  public static ButtonModel buttonModel(final ToggleControl toggleControl) {
    requireNonNull(toggleControl, "toggleControl");
    final Value<Boolean> value = toggleControl.getValue();
    final ButtonModel buttonModel;
    if (value.isNullable()) {
      buttonModel = new NullableToggleButtonModel(value.get());
    }
    else {
      buttonModel = new JToggleButton.ToggleButtonModel();
    }
    BooleanValues.booleanButtonModelValue(buttonModel).link(value);
    buttonModel.setEnabled(toggleControl.getEnabledObserver().get());
    toggleControl.getEnabledObserver().addDataListener(buttonModel::setEnabled);
    toggleControl.addPropertyChangeListener(changeEvent -> {
      if (Action.MNEMONIC_KEY.equals(changeEvent.getPropertyName())) {
        buttonModel.setMnemonic((Integer) changeEvent.getNewValue());
      }
    });

    return buttonModel;
  }

  /**
   * Creates a JCheckBox based on the given toggle control
   * @param toggleControl the toggle control
   * @return a check box
   */
  public static JCheckBox checkBox(final ToggleControl toggleControl) {
    final ButtonModel buttonModel = buttonModel(toggleControl);
    if (buttonModel instanceof NullableToggleButtonModel) {
      return new NullableCheckBox((NullableToggleButtonModel) buttonModel, toggleControl.getName());
    }

    final JCheckBox checkBox = new JCheckBox(toggleControl);
    checkBox.setModel(buttonModel);
    checkBox.setMnemonic(toggleControl.getMnemonic());

    return checkBox;
  }

  /**
   * Creates a JToggleButton based on the given toggle control
   * @param toggleControl the toggle control
   * @return a toggle button
   */
  public static JToggleButton toggleButton(final ToggleControl toggleControl) {
    requireNonNull(toggleControl, "toggleControl");
    final JToggleButton toggleButton = new JToggleButton(toggleControl);
    toggleButton.setModel(buttonModel(toggleControl));
    toggleButton.setText(toggleControl.getName());
    toggleButton.setMnemonic(toggleControl.getMnemonic());

    return toggleButton;
  }

  private abstract static class ControlHandler implements Consumer<Action> {

    @Override
    public final void accept(final Action action) {
      if (action == null) {
        onSeparator();
      }
      else if (action instanceof ControlList) {
        onControlList((ControlList) action);
      }
      else if (action instanceof Control) {
        onControl((Control) action);
      }
      else {
        onAction(action);
      }
    }

    abstract void onSeparator();

    abstract void onControl(Control control);

    abstract void onControlList(ControlList controls);

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
        panel.add(checkBox((ToggleControl) control));
      }
      else {
        panel.add(new JButton(control));
      }
    }

    @Override
    public void onControlList(final ControlList controls) {
      panel.add(vertical ? verticalButtonPanel(controls) : horizontalButtonPanel(controls));
    }

    @Override
    public void onAction(final Action action) {
      panel.add(new JButton(action));
    }
  }

  private static final class MenuControlHandler extends ControlHandler {

    private final JMenu menu;

    private MenuControlHandler(final ControlList controls) {
      menu = new JMenu(controls.getName());
      final String description = controls.getDescription();
      if (description != null) {
        menu.setToolTipText(description);
      }
      final StateObserver enabledObserver = controls.getEnabledObserver();
      if (enabledObserver != null) {
        menu.setEnabled(enabledObserver.get());
        enabledObserver.addListener(() -> menu.setEnabled(enabledObserver.get()));
      }
      final Icon icon = controls.getIcon();
      if (icon != null) {
        menu.setIcon(icon);
      }
      final int mnemonic = controls.getMnemonic();
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
        menu.add(checkBoxMenuItem((ToggleControl) control));
      }
      else {
        menu.add(control);
      }
    }

    @Override
    public void onControlList(final ControlList controls) {
      final MenuControlHandler controlHandler = new MenuControlHandler(controls);
      controls.getActions().forEach(controlHandler);
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
        toolbar.add(toggleButton((ToggleControl) control));
      }
      else {
        toolbar.add(control);
      }
    }

    @Override
    public void onControlList(final ControlList controls) {
      controls.getActions().forEach(new ToolBarControlHandler(toolbar));
    }

    @Override
    public void onAction(final Action action) {
      toolbar.add(action);
    }
  }
}