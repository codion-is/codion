/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package dev.codion.swing.common.ui.control;

import dev.codion.common.model.CancelException;
import dev.codion.common.state.StateObserver;

import java.awt.event.ActionEvent;

import static java.util.Objects.requireNonNull;

final class CommandControl extends AbstractControl {

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
