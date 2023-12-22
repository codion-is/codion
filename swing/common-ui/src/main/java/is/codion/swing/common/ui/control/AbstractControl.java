/*
 * Copyright (c) 2008 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.control;

import is.codion.common.event.Event;
import is.codion.common.state.State;
import is.codion.common.state.StateObserver;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.KeyStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.util.Arrays;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;

/**
 * An abstrct Control implementation, implementing everything except actionPerformed().
 */
abstract class AbstractControl extends AbstractAction implements Control {

  private static final String ENABLED = "enabled";

  protected static final List<String> STANDARD_KEYS = unmodifiableList(asList(
          Action.NAME, Action.SHORT_DESCRIPTION, Action.MNEMONIC_KEY,
          Action.ACCELERATOR_KEY, Action.SMALL_ICON, Action.LARGE_ICON_KEY
  ));

  protected final StateObserver enabledObserver;

  /**
   * Constructs a new Control.
   * @param name the control name
   * @param enabled the state observer controlling the enabled state of this control
   */
  AbstractControl(String name, StateObserver enabled) {
    super(name);
    this.enabledObserver = enabled == null ? State.state(true) : enabled;
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
  public final void putValue(String key, Object newValue) {
    if (ENABLED.equals(key)) {
      throw new IllegalArgumentException("Can not set the enabled state of a Control");
    }
    super.putValue(key, newValue);
  }

  @Override
  public final Object getValue(String key) {
    if (ENABLED.equals(key)) {
      return enabledObserver.get();
    }

    return super.getValue(key);
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

  @Override
  public final <B extends Builder<Control, B>> Builder<Control, B> copyBuilder(Command command) {
    return createBuilder(command, null);
  }

  @Override
  public final <B extends Builder<Control, B>> Builder<Control, B> copyBuilder(ActionCommand actionCommand) {
    return createBuilder(null, actionCommand);
  }

  @Override
  public final <B extends Builder<Control, B>> Builder<Control, B> copyBuilder(Event<ActionEvent> event) {
    requireNonNull(event);

    return copyBuilder(event::accept);
  }

  private <B extends Builder<Control, B>> Builder<Control, B> createBuilder(Command command, ActionCommand actionCommand) {
    B builder = (B) (command == null ? new ControlBuilder<Control, B>(actionCommand) : new ControlBuilder<Control, B>(command));
    builder.enabled(enabledObserver)
            .description(getDescription())
            .name(getName())
            .mnemonic((char) getMnemonic())
            .keyStroke(getKeyStroke())
            .smallIcon(getSmallIcon())
            .largeIcon(getLargeIcon());
    Arrays.stream(getKeys())
            .filter(key -> !STANDARD_KEYS.contains(key))
            .map(String.class::cast)
            .forEach(key -> builder.value(key, getValue(key)));

    return builder;
  }
}
