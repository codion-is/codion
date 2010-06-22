/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui.control;

import org.jminor.common.model.State;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.KeyStroke;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * A beefed up Action.
 */
public class Control extends AbstractAction {

  private final State enabledState;

  /**
   * Constructs a new Control.
   */
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
    this.enabledState.eventStateChanged().addListener(new ActionListener() {
      public void actionPerformed(final ActionEvent e) {
        firePropertyChange("enabled", !Control.this.enabledState.isActive(), Control.this.enabledState.isActive());
      }
    });
    setIcon(icon);
  }

  /** {@inheritDoc} */
  @Override
  public boolean isEnabled() {
    return enabledState.isActive();
  }

  /** {@inheritDoc} */
  @Override
  public void setEnabled(final boolean newValue) {
    enabledState.setActive(newValue);
  }

  /** {@inheritDoc} */
  public void actionPerformed(final ActionEvent e) {}

  /**
   * @return the description
   */
  public String getDescription() {
    return (String) this.getValue(Action.SHORT_DESCRIPTION);
  }

  /**
   * @param description the description string
   * @return this control instance
   */
  public Control setDescription(final String description) {
    this.putValue(Action.SHORT_DESCRIPTION, description);
    return this;
  }

  /**
   * @return the name
   */
  public String getName() {
    return (String) this.getValue(Action.NAME);
  }

  /**
   * @param name the name of this Control instance
   * @return this Control instance
   */
  public Control setName(final String name) {
    this.putValue(NAME, name);
    return this;
  }

  /**
   * @return the state which controls whether this Control instance is enabled
   */
  public State getEnabledState() {
    return enabledState;
  }

  /**
   * @param key the mnemonic to associate with this Control instance
   * @return this Control instance
   */
  public Control setMnemonic(final int key) {
    this.putValue(MNEMONIC_KEY, key);
    return this;
  }

  /**
   * @return the mnemonic
   */
  public int getMnemonic() {
    return (Integer) this.getValue(MNEMONIC_KEY);
  }

  /**
   * @param ks the KeyStroke to associate with this Control
   * @return this Control instance
   */
  public Control setKeyStroke(final KeyStroke ks) {
    this.putValue(ACCELERATOR_KEY, ks);
    return this;
  }

  /**
   * @param icon the icon to associate with this Control
   * @return this Control instance
   */
  public Control setIcon(final Icon icon) {
    this.putValue(SMALL_ICON, icon);
    return this;
  }

  /**
   * @return the icon
   */
  public Icon getIcon() {
    return (Icon) getValue(SMALL_ICON);
  }
}
