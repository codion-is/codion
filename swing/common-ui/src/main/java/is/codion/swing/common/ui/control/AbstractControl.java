/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.control;

import is.codion.common.state.State;
import is.codion.common.state.StateObserver;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;
import java.awt.Color;
import java.awt.Font;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import static java.util.Objects.requireNonNull;

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
  AbstractControl(final String name, final StateObserver enabledObserver) {
    this(name, enabledObserver, null);
  }

  /**
   * Constructs a new Control.
   * @param name the control name
   * @param enabledObserver the state observer controlling the enabled state of this control
   * @param smallIcon the icon
   */
  AbstractControl(final String name, final StateObserver enabledObserver, final Icon smallIcon) {
    super(name, smallIcon);
    this.enabledObserver = enabledObserver == null ? State.state(true) : enabledObserver;
    this.enabledObserver.addDataListener(super::setEnabled);
    super.setEnabled(this.enabledObserver.get());
  }

  @Override
  public final String toString() {
    return getCaption();
  }

  @Override
  public final void setEnabled(final boolean newValue) {
    throw new UnsupportedOperationException();
  }

  @Override
  public final String getDescription() {
    return (String) super.getValue(Action.SHORT_DESCRIPTION);
  }

  @Override
  public final Control setDescription(final String description) {
    super.putValue(Action.SHORT_DESCRIPTION, description);
    return this;
  }

  @Override
  public final String getCaption() {
    return (String) super.getValue(NAME);
  }

  @Override
  public final Control setCaption(final String caption) {
    super.putValue(NAME, caption);
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
  public final KeyStroke getKeyStroke() {
    return (KeyStroke) getValue(ACCELERATOR_KEY);
  }

  @Override
  public final Control setSmallIcon(final Icon smallIcon) {
    super.putValue(SMALL_ICON, smallIcon);
    return this;
  }

  @Override
  public final Icon getSmallIcon() {
    return (Icon) getValue(SMALL_ICON);
  }

  @Override
  public final Control setBackground(final Color background) {
    putValue(BACKGROUND, background);
    return this;
  }

  @Override
  public final Color getBackground() {
    return (Color) getValue(BACKGROUND);
  }

  @Override
  public final Control setForeground(final Color foreground) {
    putValue(FOREGROUND, foreground);
    return this;
  }

  @Override
  public final Color getForeground() {
    return (Color) getValue(FOREGROUND);
  }

  @Override
  public final Control setFont(final Font font) {
    putValue(FONT, font);
    return this;
  }

  @Override
  public final Font getFont() {
    return (Font) getValue(FONT);
  }

  @Override
  public final JButton createButton() {
    final JButton button = new JButton(this);
    addPropertyChangeListener(new ButtonPropertyChangeListener(button));

    return button;
  }

  @Override
  public final JMenuItem createMenuItem() {
    final JMenuItem menuItem = new JMenuItem(this);
    addPropertyChangeListener(new ButtonPropertyChangeListener(menuItem));

    return menuItem;
  }

  static final class ButtonPropertyChangeListener implements PropertyChangeListener {

    private final AbstractButton button;

    ButtonPropertyChangeListener(final AbstractButton button) {
      this.button = requireNonNull(button);
    }

    @Override
    public void propertyChange(final PropertyChangeEvent evt) {
      switch (evt.getPropertyName()) {
        case BACKGROUND: {
          button.setBackground((Color) evt.getNewValue());
          break;
        }
        case FOREGROUND: {
          button.setForeground((Color) evt.getNewValue());
          break;
        }
        case FONT: {
          button.setFont((Font) evt.getNewValue());
          break;
        }
        default:
          break;
      }
    }
  }
}
