/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.textfield;

import org.junit.jupiter.api.Test;

import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Document;
import java.awt.Color;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public final class SearchHighlighterTest {

  @Test
  void test() throws BadLocationException {
    Document document = new DefaultStyledDocument();
    document.insertString(0, "Hello there, here we are", null);

    JTextArea textArea = new JTextArea(document);

    SearchHighlighter highlighter = SearchHighlighter.searchHighlighter(textArea);
    highlighter.setHighlightColor(Color.BLUE);
    highlighter.setSelectedHighlightColor(Color.MAGENTA);

    JTextField searchField = highlighter.createSearchField();

    searchField.setText("th");
    assertEquals(6, highlighter.getSelectedHighlightPosition());

    searchField.setText(null);
    assertNull(highlighter.getSelectedHighlightPosition());

    searchField.setText("re");
    assertEquals(9, highlighter.getSelectedHighlightPosition());

    highlighter.nextSearchPosition();
    assertEquals(15, highlighter.getSelectedHighlightPosition());

    highlighter.nextSearchPosition();
    assertEquals(22, highlighter.getSelectedHighlightPosition());

    highlighter.nextSearchPosition();
    assertEquals(9, highlighter.getSelectedHighlightPosition());

    highlighter.previousSearchPosition();
    assertEquals(22, highlighter.getSelectedHighlightPosition());

    highlighter.previousSearchPosition();
    assertEquals(15, highlighter.getSelectedHighlightPosition());

    highlighter.previousSearchPosition();
    assertEquals(9, highlighter.getSelectedHighlightPosition());

    highlighter.previousSearchPosition();
    assertEquals(22, highlighter.getSelectedHighlightPosition());

    highlighter.getCaseSensitiveState().set(true);
    highlighter.getSearchStringValue().set("he");
    assertEquals(7, highlighter.getSelectedHighlightPosition());

    highlighter.getCaseSensitiveState().set(false);
    assertEquals(0, highlighter.getSelectedHighlightPosition());

    highlighter.getCaseSensitiveState().set(true);
    highlighter.getSearchStringValue().set("He");
    highlighter.nextSearchPosition();
    assertEquals(0, highlighter.getSelectedHighlightPosition());

    highlighter.previousSearchPosition();
    assertEquals(0, highlighter.getSelectedHighlightPosition());
  }
}
