/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.control;

import is.codion.common.model.CancelException;
import is.codion.common.state.StateObserver;

import java.awt.event.ActionEvent;

import static java.util.Objects.requireNonNull;

final class DefaultControl extends AbstractControl {

  private final Command command;

  DefaultControl(final Command command, final String name, final StateObserver enabledObserver) {
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
