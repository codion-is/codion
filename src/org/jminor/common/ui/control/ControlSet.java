/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui.control;

import org.jminor.common.model.State;

import javax.swing.Action;
import javax.swing.ImageIcon;
import java.util.ArrayList;
import java.util.List;

/**
 * A set of Actions/Controls, includes separators.
 */
public class ControlSet extends Control {

  private final List<Action> actions = new ArrayList<Action>();

  /** Constructs a new ControlSet. */
  public ControlSet() {
    this("");
  }

  public ControlSet(final String name) {
    this(name, (char) -1);
  }

  public ControlSet(final String name, final char mnemonic) {
    this(name, mnemonic, (ImageIcon) null);
  }

  public ControlSet(final String name, final char mnemonic, final ImageIcon icon) {
    this(name, mnemonic, icon, null);
  }

  public ControlSet(final String name, final char mnemonic, final ImageIcon icon,
                    final State enabledState) {
    super(name, enabledState, icon);
    setMnemonic(mnemonic);
  }

  public ControlSet(final Control... controls) {
    this(null, controls);
  }

  public ControlSet(final String name, final Control... controls) {
    this(name, (char) -1, controls);
  }

  public ControlSet(final String name, final char mnemonic, final Control... controls) {
    this(name, mnemonic, null, controls);
  }

  public ControlSet(final String name, final char mnemonic, final State enabledState,
                    final Control... controls) {
    this(name, mnemonic, enabledState, null, controls);
  }

  public ControlSet(final String name, final char mnemonic, final State enabledState,
                    final ImageIcon icon, final Control... controls) {
    super(name, enabledState, icon);
    setMnemonic(mnemonic);
    for (final Control control : controls)
      add(control);
  }

  public List<ControlSet> getControlSets() {
    final List<ControlSet> controlSets = new ArrayList<ControlSet>();
    for (final Action control : actions)
      if (control instanceof ControlSet)
        controlSets.add((ControlSet) control);

    return controlSets;
  }

  public List<Action> getActions() {
    return actions;
  }

  /**
   * Adds the given action to this ControlSet,
   * adding a null action has the same effect as addSeparator()
   * @param action the action to add
   */
  public void add(final Action action) {
    if (action != null)
      actions.add(action);
  }

  /**
   * Adds the given action to this ControlSet at the specified index,
   * adding a null action has the same effect as addSeparator()
   * @param action the action to add at the specified index
   * @param index the index
   */
  public void addAt(final Action action, final int index) {
    if (action != null)
      actions.add(index, action);
  }

  public boolean remove(final Action action) {
    return action != null && actions.remove(action);
  }

  public boolean remove(final ControlSet controlSet) {
    return controlSet != null && actions.remove(controlSet);
  }

  public void removeAll() {
    actions.clear();
  }

  public int size() {
    return actions.size();
  }

  public Object get(final int index) {
    return actions.get(index);
  }

  public void add(final ControlSet controlSet) {
    if (controlSet != null)
      actions.add(controlSet);
  }

  public void addAt(final ControlSet controlSet, final int index) {
    if (controlSet != null)
      actions.add(index, controlSet);
  }

  public void addSeparator() {
    actions.add(null);
  }

  public void addSeparatorAt(final int index) {
    actions.add(index, null);
  }

  public boolean hasName() {
    final String name = getName();
    return name != null && name.length() > 0;
  }

  public boolean hasIcon() {
    return getIcon() != null;
  }

  public void addAll(final ControlSet controlSet) {
    actions.addAll(controlSet.actions);
  }
}
