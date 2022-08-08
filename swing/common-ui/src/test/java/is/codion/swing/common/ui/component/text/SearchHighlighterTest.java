/*
 * Copyright (c) 2021 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.text;

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
    assertEquals(6, highlighter.selectedHighlightPosition());

    searchField.setText(null);
    assertNull(highlighter.selectedHighlightPosition());

    searchField.setText("re");
    assertEquals(9, highlighter.selectedHighlightPosition());

    highlighter.nextSearchPosition();
    assertEquals(15, highlighter.selectedHighlightPosition());

    highlighter.nextSearchPosition();
    assertEquals(22, highlighter.selectedHighlightPosition());

    highlighter.nextSearchPosition();
    assertEquals(9, highlighter.selectedHighlightPosition());

    highlighter.previousSearchPosition();
    assertEquals(22, highlighter.selectedHighlightPosition());

    highlighter.previousSearchPosition();
    assertEquals(15, highlighter.selectedHighlightPosition());

    highlighter.previousSearchPosition();
    assertEquals(9, highlighter.selectedHighlightPosition());

    highlighter.previousSearchPosition();
    assertEquals(22, highlighter.selectedHighlightPosition());

    highlighter.caseSensitiveState().set(true);
    highlighter.searchStringValue().set("he");
    assertEquals(7, highlighter.selectedHighlightPosition());

    highlighter.caseSensitiveState().set(false);
    assertEquals(0, highlighter.selectedHighlightPosition());

    highlighter.caseSensitiveState().set(true);
    highlighter.searchStringValue().set("He");
    highlighter.nextSearchPosition();
    assertEquals(0, highlighter.selectedHighlightPosition());

    highlighter.previousSearchPosition();
    assertEquals(0, highlighter.selectedHighlightPosition());
  }
}
