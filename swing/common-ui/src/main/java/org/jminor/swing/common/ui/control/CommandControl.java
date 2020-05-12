/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui.control;

import org.jminor.common.model.CancelException;
import org.jminor.common.state.StateObserver;

import java.awt.event.ActionEvent;

import static java.util.Objects.requireNonNull;

final class CommandControl extends DefaultControl {

  private final Command command;

  CommandControl(final Command command, final String name, final StateObserver enabledObserver) {
    super(name, enabledObserver);
    this.command = requireNonNull(command);
  }

  @Override
  public void actionPerformed(final ActionEvent e) {
    try {
      command.perform();
    }
    catch (final CancelException ce) {/*Operation cancelled*/}
    catch (final RuntimeException re) {
      throw re;
    }
    catch (final Exception ex) {
      throw new RuntimeException(ex);
    }
  }
}
