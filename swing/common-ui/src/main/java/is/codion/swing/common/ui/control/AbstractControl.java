/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.control;

import is.codion.common.state.StateObserver;
import is.codion.common.state.States;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.KeyStroke;

/**
 * An abstrct Control implementation, implementing everything except actionPerformed().
 */
abstract class AbstractControl extends AbstractAction implements Control {

  private final StateObserver enabledObserver;

  /**
   * Constructs a new Control.
   */
  AbstractControl() {
    this(null);
  }

  /**
   * Constructs a new Control.
   * @param name the control name
   */
  AbstractControl(final String name) {
    this(name, null);
  }

  /**
   * Constructs a new Control.
   * @param name the control name
   * @param enabledObserver the state observer controlling the enabled state of this control
   */
  AbstractControl(final String name, final StateObserver enabledObserver) {
    this(name, enabledObserver, null);
  }

  /**
   * Constructs a new Control.
   * @param name the control name
   * @param enabledObserver the state observer controlling the enabled state of this control
   * @param icon the icon
   */
  AbstractControl(final String name, final StateObserver enabledObserver, final Icon icon) {
    super(name, icon);
    this.enabledObserver = enabledObserver == null ? States.state(true) : enabledObserver;
    this.enabledObserver.addDataListener(super::setEnabled);
    super.setEnabled(this.enabledObserver.get());
  }

  @Override
  public final String toString() {
    return getName();
  }

  /**
   * Unsupported, the enabled state of Controls is based on their {@code enabledObserver}
   * @throws UnsupportedOperationException always
   * @see #AbstractControl(String, StateObserver)
   */
  @Override
  public final void setEnabled(final boolean newValue) {
    throw new UnsupportedOperationException();
  }

  @Override
  public final String getDescription() {
    return (String) super.getValue(javax.swing.Action.SHORT_DESCRIPTION);
  }

  @Override
  public final Control setDescription(final String description) {
    super.putValue(Action.SHORT_DESCRIPTION, description);
    return this;
  }

  @Override
  public final String getName() {
    return (String) super.getValue(NAME);
  }

  @Override
  public final Control setName(final String name) {
    super.putValue(NAME, name);
    return this;
  }

  @Override
  public final StateObserver getEnabledObserver() {
    return enabledObserver;
  }

  @Override
  public final Control setMnemonic(final int key) {
    super.putValue(MNEMONIC_KEY, key);
    return this;
  }

  @Override
  public final int getMnemonic() {
    final Integer mnemonic = (Integer) super.getValue(MNEMONIC_KEY);
    return mnemonic == null ? 0 : mnemonic;
  }

  @Override
  public final Control setKeyStroke(final KeyStroke keyStroke) {
    super.putValue(ACCELERATOR_KEY, keyStroke);
    return this;
  }

  @Override
  public KeyStroke getKeyStroke() {
    return (KeyStroke) getValue(ACCELERATOR_KEY);
  }

  @Override
  public final Control setIcon(final Icon icon) {
    super.putValue(SMALL_ICON, icon);
    return this;
  }

  @Override
  public final Icon getIcon() {
    return (Icon) getValue(SMALL_ICON);
  }
}
