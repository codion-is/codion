/*
 * Copyright (c) 2021 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.text;

import is.codion.common.value.Value;

import javax.swing.JTextArea;
import javax.swing.text.Document;

import static java.util.Objects.requireNonNull;

/**
 * Builds a JTextArea.
 */
public interface TextAreaBuilder extends TextComponentBuilder<String, JTextArea, TextAreaBuilder> {

  /**
   * @param rows the number of rows in the text area
   * @return this builder instance
   * @see JTextArea#setRows(int)
   */
  TextAreaBuilder rows(int rows);

  /**
   * @param rows the rows
   * @param columns the columns
   * @return this builder instance
   * @see JTextArea#setRows(int)
   * @see JTextArea#setColumns(int)
   */
  TextAreaBuilder rowsColumns(int rows, int columns);

  /**
   * @param lineWrap true if line wrap should be used
   * @return this builder instance
   * @see JTextArea#setLineWrap(boolean)
   */
  TextAreaBuilder lineWrap(boolean lineWrap);

  /**
   * @param wrapStyleWord true if wrap style word should be used
   * @return this builder instance
   * @see JTextArea#setWrapStyleWord(boolean)
   */
  TextAreaBuilder wrapStyleWord(boolean wrapStyleWord);

  /**
   * @param autoscrolls true if autoscrolling should be enabled
   * @return this builder instance
   * @see JTextArea#setAutoscrolls(boolean)
   */
  TextAreaBuilder autoscrolls(boolean autoscrolls);

  /**
   * @param tabSize the tab size
   * @return this builder instance
   * @see JTextArea#setTabSize(int)
   */
  TextAreaBuilder tabSize(int tabSize);

  /**
   * @param document the document
   * @return this builder instance
   * @see JTextArea#setDocument(Document)
   */
  TextAreaBuilder document(Document document);

  /**
   * @return a builder for a component
   */
  static TextAreaBuilder builder() {
    return new DefaultTextAreaBuilder(null);
  }

  /**
   * @param linkedValue the value to link to the component
   * @return a builder for a component
   */
  static TextAreaBuilder builder(Value<String> linkedValue) {
    return new DefaultTextAreaBuilder(requireNonNull(linkedValue));
  }
}
