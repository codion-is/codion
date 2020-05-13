/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package dev.codion.swing.common.ui.control;

import javax.swing.Action;
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
   */
  void add(Action action);

  /**
   * Adds the given action to this ControlList at the specified index,
   * adding a null action has the same effect as addSeparator()
   * @param index the index
   * @param action the action to add at the specified index
   */
  void addAt(int index, Action action);

  /**
   * @param action the action to remove
   * @return true if the action was removed
   */
  boolean remove(Action action);

  /**
   * Removes all actions from this control list
   */
  void removeAll();

  /**
   * @return the number of controls in this list
   */
  int size();

  /**
   * @param index the index
   * @return the action at the given index
   */
  Action get(int index);

  /**
   * @param controls the control list to add
   */
  void add(ControlList controls);

  /**
   * @param index the index
   * @param controls the control list to add at the specified index
   */
  void addAt(int index, ControlList controls);

  /**
   * Adds a separator to the end of this control list
   */
  void addSeparator();

  /**
   * Adds a separator at the given index
   * @param index the index
   */
  void addSeparatorAt(int index);

  /**
   * Adds all actions found in {@code controls} to this control list
   * @param controls the source list
   */
  void addAll(ControlList controls);
}
