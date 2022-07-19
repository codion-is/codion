/*
 * Copyright (c) 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.text;

import javax.swing.AbstractAction;
import javax.swing.JPasswordField;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import javax.swing.text.Utilities;
import java.awt.event.ActionEvent;

final class DeletePreviousWordAction extends AbstractAction {

  @Override
  public void actionPerformed(ActionEvent actionEvent) {
    JTextComponent textComponent = (JTextComponent) actionEvent.getSource();
    Document document = textComponent.getDocument();
    int caretPosition = textComponent.getCaretPosition();
    try {
      int removeFromPosition = textComponent instanceof JPasswordField ?
              0 ://special handling for passwords, just remove everything before cursor
              Utilities.getWordStart(textComponent, caretPosition);
      document.remove(removeFromPosition, caretPosition - removeFromPosition);
    }
    catch (BadLocationException e) {
      throw new RuntimeException(e);
    }
  }
}
