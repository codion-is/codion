/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui.control;

import org.jminor.common.State;
import org.jminor.common.StateObserver;

import javax.swing.Action;
import javax.swing.ImageIcon;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.jminor.common.Util.nullOrEmpty;

/**
 * A set of Actions/Controls, includes separators.
 */
public final class ControlSet extends Control {

  private final List<Action> actions = new ArrayList<>();

  /**
   * Constructs a new ControlSet.
   */
  public ControlSet() {
    this("");
  }

  /**
   * Constructs a new ControlSet
   * @param name the control set name
   */
  public ControlSet(final String name) {
    this(name, (char) -1);
  }

  /**
   * Constructs a new ControlSet
   * @param name the control set name
   * @param mnemonic the mnemonic to assign to this control set
   */
  public ControlSet(final String name, final char mnemonic) {
    this(name, mnemonic, (ImageIcon) null);
  }

  /**
   * Constructs a new ControlSet
   * @param name the control set name
   * @param mnemonic the mnemonic to assign to this control set
   * @param icon the icon
   */
  public ControlSet(final String name, final char mnemonic, final ImageIcon icon) {
    this(name, mnemonic, icon, null);
  }

  /**
   * Constructs a new ControlSet
   * @param name the control set name
   * @param mnemonic the mnemonic to assign to this control set
   * @param icon the icon
   * @param enabledState the state observer dictating the enable state of this control
   */
  public ControlSet(final String name, final char mnemonic, final ImageIcon icon,
                    final StateObserver enabledState) {
    super(name, enabledState, icon);
    setMnemonic(mnemonic);
  }

  /**
   * Constructs a new ControlSet
   * @param controls the controls to add to this set
   */
  public ControlSet(final Control... controls) {
    this(null, controls);
  }

  /**
   * Constructs a new ControlSet
   * @param name the control set name
   * @param controls the controls to add to this set
   */
  public ControlSet(final String name, final Control... controls) {
    this(name, (char) -1, controls);
  }

  /**
   * Constructs a new ControlSet
   * @param name the control set name
   * @param mnemonic the mnemonic to assign to this control set
   * @param controls the controls to add to this set
   */
  public ControlSet(final String name, final char mnemonic, final Control... controls) {
    this(name, mnemonic, null, controls);
  }

  /**
   * Constructs a new ControlSet
   * @param name the control set name
   * @param mnemonic the mnemonic to assign to this control set
   * @param enabledState the state observer dictating the enable state of this control
   * @param controls the controls to add to this set
   */
  public ControlSet(final String name, final char mnemonic, final State enabledState,
                    final Control... controls) {
    this(name, mnemonic, enabledState, null, controls);
  }

  /**
   * Constructs a new ControlSet
   * @param name the control set name
   * @param mnemonic the mnemonic to assign to this control set
   * @param enabledState the state observer dictating the enable state of this control
   * @param icon the icon
   * @param controls the controls to add to this set
   */
  public ControlSet(final String name, final char mnemonic, final State enabledState,
                    final ImageIcon icon, final Control... controls) {
    super(name, enabledState, icon);
    setMnemonic(mnemonic);
    for (final Control control : controls) {
      add(control);
    }
  }

  /**
   * @return a list containing all ControlSets this ControlSet contains
   */
  public List<ControlSet> getControlSets() {
    return actions.stream().filter(control -> control instanceof ControlSet)
            .map(control -> (ControlSet) control).collect(Collectors.toList());
  }

  /**
   * @return the actions in this set
   */
  public List<Action> getActions() {
    return actions;
  }

  /**
   * Adds the given action to this ControlSet,
   * adding a null action has the same effect as addSeparator()
   * @param action the action to add
   */
  public void add(final Action action) {
    if (action != null) {
      actions.add(action);
    }
  }

  /**
   * Adds the given action to this ControlSet at the specified index,
   * adding a null action has the same effect as addSeparator()
   * @param action the action to add at the specified index
   * @param index the index
   */
  public void addAt(final Action action, final int index) {
    if (action != null) {
      actions.add(index, action);
    }
  }

  /**
   * @param action the action to remove
   * @return true if the action was removed
   */
  public boolean remove(final Action action) {
    return action != null && actions.remove(action);
  }

  /**
   * Removes all actions from this control set
   */
  public void removeAll() {
    actions.clear();
  }

  /**
   * @return the size of this control set
   */
  public int size() {
    return actions.size();
  }

  /**
   * @param index the index
   * @return the action at the given index
   */
  public Action get(final int index) {
    return actions.get(index);
  }

  /**
   * @param controlSet the control set to add
   */
  public void add(final ControlSet controlSet) {
    if (controlSet != null) {
      actions.add(controlSet);
    }
  }

  /**
   * @param controlSet the control set to add at the specified index
   * @param index the index
   */
  public void addAt(final ControlSet controlSet, final int index) {
    if (controlSet != null) {
      actions.add(index, controlSet);
    }
  }

  /**
   * Adds a separator to this control set
   */
  public void addSeparator() {
    actions.add(null);
  }

  /**
   * Adds a separator at the given index
   * @param index the index
   */
  public void addSeparatorAt(final int index) {
    actions.add(index, null);
  }

  /**
   * @return true if this control set has a name
   */
  public boolean hasName() {
    return !nullOrEmpty(getName());
  }

  /**
   * @return true if this control set has an icon
   */
  public boolean hasIcon() {
    return getIcon() != null;
  }

  /**
   * Adds all action found in {@code controlSet} to this control set
   * @param controlSet the source set
   */
  public void addAll(final ControlSet controlSet) {
    actions.addAll(controlSet.actions);
  }
}
