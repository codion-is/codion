/*
 * Copyright (c) 2021 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.control;

import is.codion.common.model.CancelException;
import is.codion.common.state.StateObserver;

import java.awt.event.ActionEvent;

import static java.util.Objects.requireNonNull;

final class DefaultActionControl extends AbstractControl {

  private final ActionCommand command;

  DefaultActionControl(ActionCommand command, String name, StateObserver enabledObserver) {
    super(name, enabledObserver);
    this.command = requireNonNull(command);
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    try {
      command.perform(e);
    }
    catch (CancelException ce) {/*Operation cancelled*/}
    catch (RuntimeException re) {
      throw re;
    }
    catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }
}
