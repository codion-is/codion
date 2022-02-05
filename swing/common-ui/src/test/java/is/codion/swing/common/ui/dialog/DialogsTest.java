/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.dialog;

import is.codion.common.event.Event;
import is.codion.common.user.User;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.common.ui.icons.Logos;

import org.junit.jupiter.api.Test;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.util.Collections;

public final class DialogsTest {

  @Test
  void componentDialog() {
    Dialogs.componentDialog(new JLabel())
            .owner(new JLabel())
            .title("title")
            .icon(Logos.logoTransparent())
            .modal(false)
            .resizable(false)
            .enterAction(Control.control(() -> {}))
            .onOpened(e -> {})
            .onClosed(e -> {})
            .closeEvent(Event.event())
            .confirmCloseListener(state -> {})
            .disposeOnEscape(false)
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
            .failTitle("Fail")
            .successMessage("Success")
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
    Dialogs.okCancelDialog(new JLabel())
            .owner(new JLabel())
            .title("title")
            .modal(false)
            .icon(Logos.logoTransparent())
            .onOk(() -> {})
            .onCancel(() -> {})
            .build();
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
            .singleSelection(false)
            .defaultSelection("hello");
  }

  @Test
  void addLookupDialog() {
    Dialogs.addLookupDialog(new JTextField(), () -> Collections.singletonList("hello"));
  }

  @Test
  void createBrowseAction() {
    Dialogs.createBrowseAction(new JTextField());
  }
}
