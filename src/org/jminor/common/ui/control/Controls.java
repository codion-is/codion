/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui.control;

import org.jminor.common.model.EventObserver;
import org.jminor.common.model.StateObserver;

import javax.swing.Icon;
import javax.swing.KeyStroke;

/**
 * A static utility class for constructing Control objects.
 */
public final class Controls {

  private Controls() {}

  public static ToggleBeanValueLink toggleControl(final Object owner, final String propertyName, final String caption,
                                                  final EventObserver changeEvent) {
    return toggleControl(owner, propertyName, caption, changeEvent, null);
  }

  public static ToggleBeanValueLink toggleControl(final Object owner, final String propertyName, final String caption,
                                                  final EventObserver changeEvent, final String description) {
    return (ToggleBeanValueLink) new ToggleBeanValueLink(owner, propertyName, changeEvent, caption,
            LinkType.READ_WRITE).setDescription(description);
  }

  public static MethodControl methodControl(final Object owner, final String method, final Icon icon) {
    return methodControl(owner, method, null, null, null, -1, null, icon);
  }

  public static MethodControl methodControl(final Object owner, final String method, final String name) {
    return methodControl(owner, method, name, null);
  }

  public static MethodControl methodControl(final Object owner, final String method, final String name,
                                            final StateObserver state) {
    return new MethodControl(name, owner, method, state);
  }

  public static MethodControl methodControl(final Object owner, final String method, final String name,
                                            final StateObserver state, final String description) {
    return (MethodControl) methodControl(owner, method, name, state).setDescription(description);
  }

  public static MethodControl methodControl(final Object owner, final String method, final String name,
                                            final StateObserver state, final String description, final int mnemonic) {
    return (MethodControl) methodControl(owner, method, name, state, description).setMnemonic(mnemonic);
  }

  public static MethodControl methodControl(final Object owner, final String method, final String name,
                                            final StateObserver state, final String description, final int mnemonic, final KeyStroke ks) {
    return (MethodControl) methodControl(owner, method, name, state, description, mnemonic).setKeyStroke(ks);
  }

  public static MethodControl methodControl(final Object owner, final String method, final String name,
                                            final StateObserver state, final String description, final int mnemonic,
                                            final KeyStroke ks, final Icon icon) {
    return (MethodControl) methodControl(owner, method, name, state, description, mnemonic, ks).setIcon(icon);
  }
}
