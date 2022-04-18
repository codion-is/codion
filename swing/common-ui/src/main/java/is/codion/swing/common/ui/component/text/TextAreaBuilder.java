/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.text;

import javax.swing.JTextArea;
import javax.swing.text.Document;

/**
 * Builds a JTextArea.
 */
public interface TextAreaBuilder extends TextComponentBuilder<String, JTextArea, TextAreaBuilder> {

  /**
   * @param rows the number of rows in the text area
   * @return this builder instance
   */
  TextAreaBuilder rows(int rows);

  /**
   * @param rows the rows
   * @param columns the columns
   * @return this builder instance
   */
  TextAreaBuilder rowsColumns(int rows, int columns);

  /**
   * @param lineWrap true if line wrap should be used
   * @return this builder instance
   */
  TextAreaBuilder lineWrap(boolean lineWrap);

  /**
   * @param wrapStyleWord true if wrap style word should be used
   * @return this builder instance
   */
  TextAreaBuilder wrapStyleWord(boolean wrapStyleWord);

  /**
   * @param autoscrolls true if autoscrolling should be enabled
   * @return this builder instance
   */
  TextAreaBuilder autoscrolls(boolean autoscrolls);

  /**
   * @param tabSize the tab size
   * @return this builder instance
   */
  TextAreaBuilder tabSize(int tabSize);

  /**
   * @param document the document
   * @return this builder instance
   */
  TextAreaBuilder document(Document document);
}
