/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui.control;

import org.jminor.common.model.StateObserver;
import org.jminor.common.model.States;

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

  private final StateObserver enabledState;

  /**
   * Constructs a new Control.
   */
  public Control() {
    this(null);
  }

  /**
   * Constructs a new Control.
   * @param name the control name
   */
  public Control(final String name) {
    this(name, null);
  }

  /**
   * Constructs a new Control.
   * @param name the control name
   * @param enabledState the state observer dictating the enable state of this control
   */
  public Control(final String name, final StateObserver enabledState) {
    this(name, enabledState,  null);
  }

  /**
   * Constructs a new Control.
   * @param name the control name
   * @param enabledState the state observer dictating the enable state of this control
   * @param icon the icon
   */
  public Control(final String name, final StateObserver enabledState, final Icon icon) {
    super(name);
    this.enabledState = enabledState == null ? States.state(true) : enabledState;
    this.enabledState.addListener(new ActionListener() {
      public void actionPerformed(final ActionEvent e) {
        firePropertyChange("enabled", !Control.this.enabledState.isActive(), Control.this.enabledState.isActive());
      }
    });
    setIcon(icon);
  }

  /** {@inheritDoc} */
  @Override
  public final boolean isEnabled() {
    return enabledState.isActive();
  }

  /** {@inheritDoc} */
  @Override
  public final void setEnabled(final boolean newValue) {
    throw new UnsupportedOperationException();
  }

  /** {@inheritDoc} */
  public void actionPerformed(final ActionEvent e) {}

  /**
   * @return the description
   */
  public final String getDescription() {
    return (String) this.getValue(Action.SHORT_DESCRIPTION);
  }

  /**
   * @param description the description string
   * @return this control instance
   */
  public final Control setDescription(final String description) {
    this.putValue(Action.SHORT_DESCRIPTION, description);
    return this;
  }

  /**
   * @return the name
   */
  public final String getName() {
    return (String) this.getValue(Action.NAME);
  }

  /**
   * @param name the name of this Control instance
   * @return this Control instance
   */
  public final Control setName(final String name) {
    this.putValue(NAME, name);
    return this;
  }

  /**
   * @return the state which controls whether this Control instance is enabled
   */
  public final StateObserver getEnabledState() {
    return enabledState;
  }

  /**
   * @param key the mnemonic to associate with this Control instance
   * @return this Control instance
   */
  public final Control setMnemonic(final int key) {
    this.putValue(MNEMONIC_KEY, key);
    return this;
  }

  /**
   * @return the mnemonic
   */
  public final int getMnemonic() {
    return (Integer) this.getValue(MNEMONIC_KEY);
  }

  /**
   * @param ks the KeyStroke to associate with this Control
   * @return this Control instance
   */
  public final Control setKeyStroke(final KeyStroke ks) {
    this.putValue(ACCELERATOR_KEY, ks);
    return this;
  }

  /**
   * @param icon the icon to associate with this Control
   * @return this Control instance
   */
  public final Control setIcon(final Icon icon) {
    this.putValue(SMALL_ICON, icon);
    return this;
  }

  /**
   * @return the icon
   */
  public final Icon getIcon() {
    return (Icon) getValue(SMALL_ICON);
  }
}
