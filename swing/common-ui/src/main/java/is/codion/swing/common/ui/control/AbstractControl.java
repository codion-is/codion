/*
 * Copyright (c) 2008 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.control;

import is.codion.common.state.State;
import is.codion.common.state.StateObserver;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.KeyStroke;
import java.awt.Color;
import java.awt.Font;

/**
 * An abstrct Control implementation, implementing everything except actionPerformed().
 */
abstract class AbstractControl extends AbstractAction implements Control {

  private final StateObserver enabledObserver;

  /**
   * Constructs a new Control.
   * @param name the control name
   * @param enabledObserver the state observer controlling the enabled state of this control
   */
  AbstractControl(String name, StateObserver enabledObserver) {
    super(name);
    this.enabledObserver = enabledObserver == null ? State.state(true) : enabledObserver;
    this.enabledObserver.addDataListener(super::setEnabled);
    super.setEnabled(this.enabledObserver.get());
  }

  @Override
  public final String toString() {
    return getName();
  }

  @Override
  public final void setEnabled(boolean newValue) {
    throw new UnsupportedOperationException();
  }

  @Override
  public final String getDescription() {
    return (String) getValue(Action.SHORT_DESCRIPTION);
  }

  @Override
  public final void setDescription(String description) {
    putValue(Action.SHORT_DESCRIPTION, description);
  }

  @Override
  public final String getName() {
    Object value = getValue(NAME);

    return value == null ? "" : String.valueOf(value);
  }

  @Override
  public final void setName(String name) {
    putValue(NAME, name);
  }

  @Override
  public final StateObserver enabled() {
    return enabledObserver;
  }

  @Override
  public final void setMnemonic(int key) {
    putValue(MNEMONIC_KEY, key);
  }

  @Override
  public final int getMnemonic() {
    Integer mnemonic = (Integer) getValue(MNEMONIC_KEY);
    return mnemonic == null ? 0 : mnemonic;
  }

  @Override
  public final void setKeyStroke(KeyStroke keyStroke) {
    putValue(ACCELERATOR_KEY, keyStroke);
  }

  @Override
  public final KeyStroke getKeyStroke() {
    return (KeyStroke) getValue(ACCELERATOR_KEY);
  }

  @Override
  public final void setSmallIcon(Icon smallIcon) {
    putValue(SMALL_ICON, smallIcon);
  }

  @Override
  public final Icon getSmallIcon() {
    return (Icon) getValue(SMALL_ICON);
  }

  @Override
  public final void setLargeIcon(Icon largeIcon) {
    putValue(LARGE_ICON_KEY, largeIcon);
  }

  @Override
  public final Icon getLargeIcon() {
    return (Icon) getValue(LARGE_ICON_KEY);
  }

  @Override
  public final void setBackground(Color background) {
    putValue(BACKGROUND, background);
  }

  @Override
  public final Color getBackground() {
    return (Color) getValue(BACKGROUND);
  }

  @Override
  public final void setForeground(Color foreground) {
    putValue(FOREGROUND, foreground);
  }

  @Override
  public final Color getForeground() {
    return (Color) getValue(FOREGROUND);
  }

  @Override
  public final void setFont(Font font) {
    putValue(FONT, font);
  }

  @Override
  public final Font getFont() {
    return (Font) getValue(FONT);
  }
}
