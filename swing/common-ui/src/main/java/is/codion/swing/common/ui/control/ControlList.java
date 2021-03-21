/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.control;

import is.codion.common.state.StateObserver;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import java.util.List;

/**
 * A list of controls and separator, note that these can be nested control lists.
 */
public interface ControlList extends Control {

  /**
   * @return a list containing all ControlLists this ControlList contains
   */
  List<ControlList> getControlLists();

  /**
   * @return an unmodifiable view of the actions in this set
   */
  List<Action> getActions();

  /**
   * Adds the given action to this ControlList,
   * adding a null action has the same effect as addSeparator()
   * @param action the action to add
   * @return this ControlList instance
   */
  ControlList add(Action action);

  /**
   * Adds the given action to this ControlList at the specified index,
   * adding a null action has the same effect as addSeparator()
   * @param index the index
   * @param action the action to add at the specified index
   * @return this ControlList instance
   */
  ControlList addAt(int index, Action action);

  /**
   * @param action the action to remove
   * @return this ControlList instance
   */
  ControlList remove(Action action);

  /**
   * Removes all actions from this control list
   * @return this ControlList instance
   */
  ControlList removeAll();

  /**
   * @return the number of controls in this list
   */
  int size();

  /**
   * @return true if this control list contains no controls
   */
  boolean isEmpty();

  /**
   * @param index the index
   * @return the action at the given index
   */
  Action get(int index);

  /**
   * @param controls the control list to add
   * @return this ControlList instance
   */
  ControlList add(ControlList controls);

  /**
   * @param index the index
   * @param controls the control list to add at the specified index
   * @return this ControlList instance
   */
  ControlList addAt(int index, ControlList controls);

  /**
   * Adds a separator to the end of this control list
   * @return this ControlList instance
   */
  ControlList addSeparator();

  /**
   * Adds a separator at the given index
   * @param index the index
   * @return this ControlList instance
   */
  ControlList addSeparatorAt(int index);

  /**
   * Adds all actions found in {@code controls} to this control list
   * @param controls the source list
   * @return this ControlList instance
   */
  ControlList addAll(ControlList controls);

  /**
   * Creates a vertically laid out panel of buttons from this control list
   * @return the button panel
   */
  JPanel createVerticalButtonPanel();

  /**
   * Creates a horizontally laid out panel of buttons from this control list
   * @return the button panel
   */
  JPanel createHorizontalButtonPanel();

  /**
   * Creates a popup menu from this control list
   * @return a popup menu based on this control list
   */
  JPopupMenu createPopupMenu();

  /**
   * Creates a menu from this control list
   * @return a menu based on this control list
   */
  JMenu createMenu();

  /**
   * Creates a JToolBar populated with these controls.
   * @param orientation the toolbar orientation
   * @return a toolbar based on these controls
   */
  JToolBar createToolBar(final int orientation);

  /**
   * Adds these controls to the given tool bar.
   * @param toolBar the toolbar to add the controls to
   */
  void populateToolBar(final JToolBar toolBar);

  /**
   * @return a menu bar based on the given controls
   */
  JMenuBar createMenuBar();

  /**
   * @param menuBar the menubar to add the controls to
   * @return the menu bar with the added controls
   */
  JMenuBar populateMenuBar(final JMenuBar menuBar);

  /**
   * Constructs a new ControlList.
   * @return a new ControlList instance.
   */
  static ControlList controlList() {
    return builder().build();
  }

  /**
   * @return a new ControlList.Builder instance
   */
  static Builder builder() {
    return new ControlListBuilder();
  }

  /**
   * A builder for ControlList
   * @see ControlList#builder()
   */
  interface Builder extends Control.Builder {

    /**
     * @param name the control list name
     * @return this Builder instance
     */
    Builder name(String name);

    /**
     * @param description a description for the control list
     * @return this Builder instance
     */
    Builder description(String description);

    /**
     * @param mnenomic the mnemonic to assign to this control list
     * @return this Builder instance
     */
    Builder mnemonic(char mnenomic);

    /**
     * @param keyStroke the keystroke to associate with the control
     * @return this Builder instance
     */
    Builder keyStroke(KeyStroke keyStroke);

    /**
     * @param enabledState the state observer dictating the enable state of this control
     * @return this Builder instance
     */
    Builder enabledState(StateObserver enabledState);

    /**
     * @param icon the icon
     * @return this Builder instance
     */
    Builder icon(Icon icon);

    /**
     * @param control the control to add to this list
     * @return this Builder instance
     */
    Builder control(Control control);

    /**
     * @param controlBuilder the control builder to add to this list
     * @return this Builder instance
     */
    Builder control(Control.Builder controlBuilder);

    /**
     * @param controls the controls to add
     * @return this Builder instance
     */
    Builder controls(Control... controls);

    /**
     * @param controlBuilders the control builder to add
     * @return this Builder instance
     */
    Builder controls(Control.Builder... controlBuilders);

    /**
     * @param action the Action to add to this list
     * @return this Builder instance
     */
    Builder action(Action action);

    /**
     * @param actions the Actions to add to this list
     * @return this Builder instance
     */
    Builder actions(Action... actions);

    /**
     * Adds a separator to the ControlList
     * @return this Builder instance
     */
    Builder separator();

    /**
     * Builds the ControlList
     * @return a new ControlList
     */
    ControlList build();
  }
}
