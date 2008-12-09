/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui.control;

import org.jminor.common.model.State;
import org.jminor.common.model.UserCancelException;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.KeyStroke;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * A beefed up Action
 */
public class Control extends AbstractAction {

  private final State enabledState;

  /** Constructs a new Control. */
  public Control() {
    this(null);
  }

  public Control(final String name) {
    this(name, null);
  }

  public Control(String name, final State enabledState) {
    this(name, enabledState,  null);
  }

  public Control(String name, final State enabledState, final Icon icon) {
    super(name);
    this.enabledState = enabledState == null ? new State(true) : enabledState;
    this.enabledState.evtStateChanged.addListener(new ActionListener() {
      public void actionPerformed(final ActionEvent e) {
        firePropertyChange("enabled", !Control.this.enabledState.isActive(), Control.this.enabledState.isActive());
      }
    });
    setIcon(icon);
  }

  /** {@inheritDoc} */
  public boolean isEnabled() {
    return enabledState.isActive();
  }

  /** {@inheritDoc} */
  public void setEnabled(final boolean enabled) {
    enabledState.setActive(enabled);
  }

  /** {@inheritDoc} */
  public void actionPerformed(final ActionEvent e) {}

  /**
   * @return Value for property 'description'.
   */
  public String getDescription() {
    return (String) this.getValue(Action.SHORT_DESCRIPTION);
  }

  /**
   * @param desc Value to set for property 'description'.
   */
  public void setDescription(final String desc) {
    this.putValue(Action.SHORT_DESCRIPTION, desc);
  }

  /**
   * @return Value for property 'name'.
   */
  public String getName() {
    return (String) this.getValue(Action.NAME);
  }

  /**
   * @param name Value to set for property 'name'.
   */
  public void setName(final String name) {
    this.putValue(NAME, name);
  }

  /**
   * @return Value for property 'enabledState'.
   */
  public State getEnabledState() {
    return enabledState;
  }

  /**
   * @param key Value to set for property 'mnemonic'.
   */
  public void setMnemonic(final int key) {
    this.putValue(MNEMONIC_KEY, key);
  }

  /**
   * @return Value for property 'mnemonic'.
   */
  public int getMnemonic() {
    return (Integer) this.getValue(MNEMONIC_KEY);
  }

  /**
   * @param ks Value to set for property 'keyStroke'.
   */
  public void setKeyStroke(final KeyStroke ks) {
    this.putValue(ACCELERATOR_KEY, ks);
  }

  /**
   * @param icon Value to set for property 'icon'.
   */
  public void setIcon(final Icon icon) {
    this.putValue(SMALL_ICON, icon);
  }

  /**
   * @return Value for property 'icon'.
   */
  public Icon getIcon() {
    return (Icon) getValue(SMALL_ICON);
  }

  protected void handleException(final Throwable exception) {
    if (exception instanceof UserCancelException)
      return;

    exception.printStackTrace();
    throw new RuntimeException(exception);
  }
}
