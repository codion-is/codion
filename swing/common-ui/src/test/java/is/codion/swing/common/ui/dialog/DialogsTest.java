/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.dialog;

import is.codion.common.event.Event;
import is.codion.common.user.User;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.common.ui.icons.Icons;

import org.junit.jupiter.api.Test;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.util.Collections;

public final class DialogsTest {

  @Test
  public void dialogBuilder() {
    Dialogs.dialogBuilder()
            .owner(new JLabel())
            .component(new JLabel())
            .title("title")
            .icon(Icons.icons().filter())
            .modal(false)
            .resizable(false)
            .enterAction(Control.control(() -> {}))
            .onClosedAction(Control.control(() -> {}))
            .closeEvent(Event.event())
            .confirmCloseListener(state -> {})
            .disposeOnEscape(false)
            .build();
  }

  @Test
  public void exceptionDialogBuilder() {
    Dialogs.exceptionDialogBuilder()
            .owner(new JLabel())
            .title("title")
            .icon(Icons.icons().filter())
            .message("message");
  }

  @Test
  public void fileSelectionDialogBuilder() {
    Dialogs.fileSelectionDialogBuilder()
            .owner(new JLabel())
            .title("title")
            .startDirectory(System.getProperty("user.home"))
            .confirmOverwrite(true);
  }

  @Test
  public void loginDialogBuilder() {
    Dialogs.loginDialogBuilder()
            .owner(new JLabel())
            .title("title")
            .icon(Icons.icons().filter())
            .validator(user -> {})
            .southComponent(new JLabel())
            .defaultUser(User.user("scott"));
  }

  @Test
  public void okCancelDialogBuilder() {
    Dialogs.okCancelDialogBuilder()
            .owner(new JLabel())
            .title("title")
            .icon(Icons.icons().filter())
            .component(new JLabel())
            .okAction(Control.control(() -> {}))
            .cancelAction(Control.control(() -> {}))
            .build();
  }

  @Test
  public void progressDialogBuilder() {
    Dialogs.progressDialogBuilder()
            .owner(new JLabel())
            .title("title")
            .indeterminate(true)
            .stringPainted(true)
            .northPanel(new JPanel())
            .westPanel(new JPanel())
            .buttonControls(Controls.builder()
                    .control(Control.builder(() -> {}))
                    .build())
            .build();
  }

  @Test
  public void selectionDialogBuilder() {
    Dialogs.selectionDialogBuilder(Collections.singletonList("hello"))
            .owner(new JLabel())
            .title("title")
            .singleSelection(false)
            .defaultSelection("hello");
  }

  @Test
  public void addLookupDialog() {
    Dialogs.addLookupDialog(new JTextField(), () -> Collections.singletonList("hello"));
  }

  @Test
  public void createBrowseAction() {
    Dialogs.createBrowseAction(new JTextField());
  }
}
