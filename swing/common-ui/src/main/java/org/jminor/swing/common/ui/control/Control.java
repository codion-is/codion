/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui.control;

import org.jminor.common.state.StateObserver;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.KeyStroke;

/**
 * A beefed up Action.
 */
public interface Control extends Action {

  /**
   * @param description the description string
   * @return this control instance
   */
  Control setDescription(String description);

  /**
   * @return the description
   */
  String getDescription();

  /**
   * @return the name
   */
  String getName();

  /**
   * @param name the name of this Control instance
   * @return this Control instance
   */
  Control setName(String test);

  /**
   * @return the state which controls whether this Control instance is enabled
   */
  StateObserver getEnabledObserver();

  /**
   * @param key the mnemonic to associate with this Control instance
   * @return this Control instance
   */
  Control setMnemonic(int mnemonic);

  /**
   * @return the mnemonic, 0 if none is specified
   */
  int getMnemonic();

  /**
   * @param keyStroke the KeyStroke to associate with this Control
   * @return this Control instance
   */
  Control setKeyStroke(KeyStroke keyStroke);

  /**
   * @return the KeyStroke associated with this Control, if any
   */
  KeyStroke getKeyStroke();

  /**
   * @param icon the icon to associate with this Control
   * @return this Control instance
   */
  Control setIcon(Icon icon);

  /**
   * @return the icon
   */
  Icon getIcon();

  /**
   * A simple command interface, allowing for Controls based on method references
   */
  interface Command {

    /**
     * Performs the work
     * @throws Exception in case of an exception
     */
    void perform() throws Exception;
  }
}
