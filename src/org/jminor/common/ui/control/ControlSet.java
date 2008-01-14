/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui.control;

import org.jminor.common.model.State;

import javax.swing.Action;
import javax.swing.ImageIcon;
import java.util.ArrayList;
import java.util.List;

/**
 * A set of Actions/Controls, includes seperators
 */
public class ControlSet extends Control {

  private final ArrayList<Action> actions = new ArrayList<Action>();
  private final char mnemonic;

  private String description;

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
    this.mnemonic = mnemonic;
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
    this.mnemonic = mnemonic;
    for (final Control control : controls)
      add(control);
  }

  public List<ControlSet> getControlSets() {
    final List<ControlSet> ret = new ArrayList<ControlSet>();
    for (final Action control : actions)
      if (control instanceof ControlSet)
        ret.add((ControlSet) control);

    return ret;
  }

  /** {@inheritDoc} */
  public void setDescription(final String description) {
    this.description = description;
  }

  /** {@inheritDoc} */
  public String getDescription() {
    return description;
  }

  /**
   * @return Value for property 'mnemonic'.
   */
  public int getMnemonic() {
    return mnemonic;
  }

  public void add(final Action action) {
    actions.add(action);
  }

  public void addAt(final Action action, final int idx) {
    actions.add(idx, action);
  }

  public boolean remove(final Action action) {
    return actions.remove(action);
  }

  public boolean remove(final ControlSet set) {
    return actions.remove(set);
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
    actions.add(controlSet);
  }

  public void addAt(final ControlSet controlSet, final int idx) {
    actions.add(idx, controlSet);
  }

  public void addSeparator() {
    actions.add(null);
  }

  public void addSeparatorAt(final int idx) {
    actions.add(idx, null);
  }

  public void iterate(final IControlIterator controlIterator) {
    if (controlIterator == null)
      throw new IllegalArgumentException("Iterator can't be null");

    for (final Action action : actions) {
      if (action == null)
        controlIterator.doSeparator();
      else if (action instanceof ToggleBeanPropertyLink)
        controlIterator.doToggleControl((ToggleBeanPropertyLink) action);
      else if (action instanceof ControlSet)
        controlIterator.doControlSet((ControlSet) action);
      else if (action instanceof Control)
        controlIterator.doControl((Control) action);
      else
        controlIterator.doAction(action);
    }
  }
}
