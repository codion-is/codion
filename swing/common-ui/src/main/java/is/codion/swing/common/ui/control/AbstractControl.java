/*
 * Copyright (c) 2008 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
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
  AbstractControl(String name, StateObserver enabledObserver) {
    super(name);
    this.enabledObserver = enabledObserver == null ? State.state(true) : enabledObserver;
    this.enabledObserver.addDataListener(super::setEnabled);
    super.setEnabled(this.enabledObserver.get());
  }

  @Override
  public final String toString() {
    return getCaption();
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
  public final Control setDescription(String description) {
    putValue(Action.SHORT_DESCRIPTION, description);
    return this;
  }

  @Override
  public final String getCaption() {
    Object value = getValue(NAME);

    return value == null ? "" : String.valueOf(value);
  }

  @Override
  public final Control setCaption(String caption) {
    putValue(NAME, caption);
    return this;
  }

  @Override
  public final StateObserver enabledObserver() {
    return enabledObserver;
  }

  @Override
  public final Control setMnemonic(int key) {
    putValue(MNEMONIC_KEY, key);
    return this;
  }

  @Override
  public final int getMnemonic() {
    Integer mnemonic = (Integer) getValue(MNEMONIC_KEY);
    return mnemonic == null ? 0 : mnemonic;
  }

  @Override
  public final Control setKeyStroke(KeyStroke keyStroke) {
    putValue(ACCELERATOR_KEY, keyStroke);
    return this;
  }

  @Override
  public final KeyStroke getKeyStroke() {
    return (KeyStroke) getValue(ACCELERATOR_KEY);
  }

  @Override
  public final Control setSmallIcon(Icon smallIcon) {
    putValue(SMALL_ICON, smallIcon);
    return this;
  }

  @Override
  public final Icon getSmallIcon() {
    return (Icon) getValue(SMALL_ICON);
  }

  @Override
  public final Control setLargeIcon(Icon largeIcon) {
    putValue(LARGE_ICON_KEY, largeIcon);
    return this;
  }

  @Override
  public final Icon getLargeIcon() {
    return (Icon) getValue(LARGE_ICON_KEY);
  }

  @Override
  public final Control setBackground(Color background) {
    putValue(BACKGROUND, background);
    return this;
  }

  @Override
  public final Color getBackground() {
    return (Color) getValue(BACKGROUND);
  }

  @Override
  public final Control setForeground(Color foreground) {
    putValue(FOREGROUND, foreground);
    return this;
  }

  @Override
  public final Color getForeground() {
    return (Color) getValue(FOREGROUND);
  }

  @Override
  public final Control setFont(Font font) {
    putValue(FONT, font);
    return this;
  }

  @Override
  public final Font getFont() {
    return (Font) getValue(FONT);
  }

  @Override
  public final JButton createButton() {
    JButton button = new JButton(this);
    addPropertyChangeListener(new ButtonPropertyChangeListener(button));

    return button;
  }

  @Override
  public final JMenuItem createMenuItem() {
    JMenuItem menuItem = new JMenuItem(this);
    addPropertyChangeListener(new ButtonPropertyChangeListener(menuItem));

    return menuItem;
  }

  static final class ButtonPropertyChangeListener implements PropertyChangeListener {

    private final AbstractButton button;

    ButtonPropertyChangeListener(AbstractButton button) {
      this.button = requireNonNull(button);
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
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
