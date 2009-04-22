/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui.control;

import org.jminor.common.model.Event;
import org.jminor.common.model.State;

import javax.swing.Icon;
import javax.swing.KeyStroke;

/**
 * A static utility class for providing Control objects
 */
public class ControlFactory {

  public static ToggleBeanPropertyLink toggleControl(final Object owner, final String propertyName, final String name,
                                                     final Event changeEvent) {
    return toggleControl(owner, propertyName, name, changeEvent, null);
  }

  public static ToggleBeanPropertyLink toggleControl(final Object owner, final String propertyName, final String name,
                                                     final Event changeEvent, final String description) {
    final ToggleBeanPropertyLink link = new ToggleBeanPropertyLink(owner, propertyName, changeEvent, name,
                    LinkType.READ_WRITE);

    return (ToggleBeanPropertyLink) setControlDesc(link, description);
  }

  public static MethodControl methodControl(final Object owner, final String method, final Icon icon) {
    return methodControl(owner, method, null, null, null, -1, null, icon);
  }

  public static MethodControl methodControl(final Object owner, final String method, final String name) {
    return methodControl(owner, method, name, null);
  }

  public static MethodControl methodControl(final Object owner, final String method, final String name,
                                            final State state) {
    return new MethodControl(name, owner, method, state);
  }

  public static MethodControl methodControl(final Object owner, final String method, final String name,
                                            final State state, final String desc) {
    return (MethodControl) setControlDesc(methodControl(owner, method, name, state), desc);
  }

  public static MethodControl methodControl(final Object owner, final String method, final String name,
                                            final State state, final String desc, final int mnemonic) {
    return (MethodControl) setControlMnemonic(methodControl(owner, method, name, state, desc), mnemonic);
  }

  public static MethodControl methodControl(final Object owner, final String method, final String name,
                                            final State state, final String desc, final int mnemonic, final KeyStroke ks) {
    return (MethodControl) setControlKeyStroke(methodControl(owner, method, name, state, desc, mnemonic), ks);
  }

  public static MethodControl methodControl(final Object owner, final String method, final String name,
                                            final State state, final String desc, final int mnemonic,
                                            final KeyStroke ks, final Icon icon) {
    return (MethodControl) setControlIcon(methodControl(owner, method, name, state, desc, mnemonic, ks), icon);
  }

  public static Control setControlDesc(final Control control, final String desc) {
    control.setDescription(desc);

    return control;
  }

  public static Control setControlMnemonic(final Control control, final int mnemonic) {
    control.setMnemonic(mnemonic);

    return control;
  }

  public static Control setControlKeyStroke(final Control control, final KeyStroke keyStroke) {
    control.setKeyStroke(keyStroke);

    return control;
  }

  public static Control setControlIcon(final Control control, final Icon icon) {
    control.setIcon(icon);

    return control;
  }
}
