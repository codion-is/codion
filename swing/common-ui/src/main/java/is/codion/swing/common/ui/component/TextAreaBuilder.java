/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import java.util.function.Consumer;

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
   * Builds the text area and returns a scroll pane containing it, note that subsequent calls return the same scroll pane.
   * @return a scroll pane containing the text area
   */
  JScrollPane buildScrollPane();

  /**
   * Builds the text area and returns a scroll pane containing it
   * @param onBuild called after the first call when the component is built, not called on subsequent calls.
   * @return a scroll pane containing the text area
   */
  JScrollPane buildScrollPane(Consumer<JScrollPane> onBuild);
}
