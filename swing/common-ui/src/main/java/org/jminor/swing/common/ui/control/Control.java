/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui.control;

import org.jminor.common.state.StateObserver;
import org.jminor.common.state.States;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.KeyStroke;
import java.awt.event.ActionEvent;

/**
 * A beefed up Action.
 */
public class Control extends AbstractAction {

  private final StateObserver enabledObserver;

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
   * @param enabledObserver the state observer controlling the enabled state of this control
   */
  public Control(final String name, final StateObserver enabledObserver) {
    this(name, enabledObserver, null);
  }

  /**
   * Constructs a new Control.
   * @param name the control name
   * @param enabledObserver the state observer controlling the enabled state of this control
   * @param icon the icon
   */
  public Control(final String name, final StateObserver enabledObserver, final Icon icon) {
    super(name, icon);
    this.enabledObserver = enabledObserver == null ? States.state(true) : enabledObserver;
    this.enabledObserver.addDataListener(super::setEnabled);
    super.setEnabled(this.enabledObserver.get());
  }

  /** {@inheritDoc} */
  @Override
  public final String toString() {
    return getName();
  }

  /**
   * Unsupported, the enabled state of Controls is based on their {@code enabledObserver}
   * @throws UnsupportedOperationException always
   * @see #Control(String, StateObserver)
   */
  @Override
  public final void setEnabled(final boolean newValue) {
    throw new UnsupportedOperationException();
  }

  /** {@inheritDoc} */
  @Override
  public void actionPerformed(final ActionEvent e) {/*Not required*/}

  /**
   * @return the description
   */
  public final String getDescription() {
    return (String) super.getValue(javax.swing.Action.SHORT_DESCRIPTION);
  }

  /**
   * @param description the description string
   * @return this control instance
   */
  public final Control setDescription(final String description) {
    super.putValue(Action.SHORT_DESCRIPTION, description);
    return this;
  }

  /**
   * @return the name
   */
  public final String getName() {
    return (String) super.getValue(NAME);
  }

  /**
   * @param name the name of this Control instance
   * @return this Control instance
   */
  public final Control setName(final String name) {
    super.putValue(NAME, name);
    return this;
  }

  /**
   * @return the state which controls whether this Control instance is enabled
   */
  public final StateObserver getEnabledObserver() {
    return enabledObserver;
  }

  /**
   * @param key the mnemonic to associate with this Control instance
   * @return this Control instance
   */
  public final Control setMnemonic(final int key) {
    super.putValue(MNEMONIC_KEY, key);
    return this;
  }

  /**
   * @return the mnemonic, 0 if none is specified
   */
  public final int getMnemonic() {
    final Integer mnemonic = (Integer) super.getValue(MNEMONIC_KEY);
    return mnemonic == null ? 0 : mnemonic;
  }

  /**
   * @param ks the KeyStroke to associate with this Control
   * @return this Control instance
   */
  public final Control setKeyStroke(final KeyStroke ks) {
    super.putValue(ACCELERATOR_KEY, ks);
    return this;
  }

  /**
   * @param icon the icon to associate with this Control
   * @return this Control instance
   */
  public final Control setIcon(final Icon icon) {
    super.putValue(SMALL_ICON, icon);
    return this;
  }

  /**
   * @return the icon
   */
  public final Icon getIcon() {
    return (Icon) getValue(SMALL_ICON);
  }

  /**
   * A simple command interface, allowing for Controls based on method references
   */
  public interface Command {
    /**
     * Performs the work
     * @throws Exception in case of an exception
     */
    void perform() throws Exception;
  }
}
