/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2021 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.dialog;

import is.codion.common.event.Event;
import is.codion.common.state.State;
import is.codion.common.state.StateObserver;
import is.codion.common.user.User;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.common.ui.icon.Logos;
import is.codion.swing.common.ui.key.KeyEvents;

import org.junit.jupiter.api.Test;

import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.event.KeyEvent;
import java.util.Collections;

import static is.codion.swing.common.ui.control.Control.control;
import static org.junit.jupiter.api.Assertions.assertThrows;

public final class DialogsTest {

  @Test
  void componentDialog() {
    Dialogs.componentDialog(new JLabel())
            .owner(new JLabel())
            .title("title")
            .icon(Logos.logoTransparent())
            .modal(false)
            .resizable(false)
            .enterAction(control(() -> {}))
            .onOpened(e -> {})
            .onClosed(e -> {})
            .closeEvent(Event.event())
            .confirmCloseListener(state -> {})
            .disposeOnEscape(false)
            .keyEvent(KeyEvents.builder(KeyEvent.VK_ESCAPE)
                    .action(Control.control(() -> {})))
            .build();
  }

  @Test
  void progressWorkerDialog() {
    Dialogs.progressWorkerDialog(() -> {})
            .owner(new JLabel())
            .title("title")
            .icon(Logos.logoTransparent())
            .northPanel(new JPanel())
            .westPanel(new JPanel())
            .onException("Fail")
            .onResult("Success")
            .stringPainted(true)
            .indeterminate(false)
            .build();
  }

  @Test
  void exceptionDialog() {
    Dialogs.exceptionDialog()
            .owner(new JLabel())
            .title("title")
            .icon(Logos.logoTransparent())
            .message("message");
  }

  @Test
  void fileSelectionDialog() {
    Dialogs.fileSelectionDialog()
            .owner(new JLabel())
            .title("title")
            .startDirectory(System.getProperty("user.home"))
            .confirmOverwrite(true);
  }

  @Test
  void loginDialog() {
    Dialogs.loginDialog()
            .owner(new JLabel())
            .title("title")
            .icon(Logos.logoTransparent())
            .validator(user -> {})
            .southComponent(new JLabel())
            .defaultUser(User.user("scott"));
  }

  @Test
  void okCancelDialog() {
    JLabel label = new JLabel();
    Runnable runnable = () -> {};
    Dialogs.okCancelDialog(label)
            .owner(label)
            .title("title")
            .modal(false)
            .icon(Logos.logoTransparent())
            .onOk(runnable)
            .onCancel(runnable)
            .build();

    Control.Command command = () -> {};
    StateObserver state = State.state().observer();

    assertThrows(IllegalStateException.class, () -> Dialogs.okCancelDialog(label)
            .onOk(runnable)
            .okAction(control(command)));
    assertThrows(IllegalStateException.class, () -> Dialogs.okCancelDialog(label)
            .onCancel(runnable)
            .cancelAction(control(command)));

    assertThrows(IllegalStateException.class, () -> Dialogs.okCancelDialog(label)
            .okAction(control(command))
            .onOk(runnable));
    assertThrows(IllegalStateException.class, () -> Dialogs.okCancelDialog(label)
            .cancelAction(control(command))
            .onCancel(runnable));

    assertThrows(IllegalStateException.class, () -> Dialogs.okCancelDialog(label)
            .okAction(control(command))
            .okEnabled(state));
    assertThrows(IllegalStateException.class, () -> Dialogs.okCancelDialog(label)
            .cancelAction(control(command))
            .cancelEnabled(state));
  }

  @Test
  void progressDialog() {
    Dialogs.progressDialog()
            .owner(new JLabel())
            .title("title")
            .indeterminate(true)
            .stringPainted(true)
            .northPanel(new JPanel())
            .westPanel(new JPanel())
            .controls(Controls.builder()
                    .control(Control.builder(() -> {}))
                    .build())
            .build();
  }

  @Test
  void selectionDialog() {
    Dialogs.selectionDialog(Collections.singletonList("hello"))
            .owner(new JLabel())
            .title("title")
            .allowEmptySelection(true)
            .defaultSelection("hello");
  }

  @Test
  void selectionDialogNoItems() {
    assertThrows(IllegalArgumentException.class, () -> Dialogs.selectionDialog(Collections.emptyList()));
  }

  @Test
  void selectionDialogNonExistingDefaultSelection() {
    assertThrows(IllegalArgumentException.class, () -> Dialogs.selectionDialog(Collections.singletonList("helloist"))
            .defaultSelection("hello"));
  }
}
