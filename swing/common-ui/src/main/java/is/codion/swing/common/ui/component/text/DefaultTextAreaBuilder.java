/*
 * Copyright (c) 2021 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.text;

import is.codion.common.value.Value;
import is.codion.swing.common.ui.component.ComponentValue;

import javax.swing.JTextArea;
import javax.swing.text.Document;

import static java.util.Objects.requireNonNull;

final class DefaultTextAreaBuilder extends AbstractTextComponentBuilder<String, JTextArea, TextAreaBuilder> implements TextAreaBuilder {

  private int columns;
  private int rows;
  private int tabSize;
  private boolean lineWrap = true;
  private boolean wrapStyleWord = true;
  private boolean autoscrolls = true;
  private Document document;

  DefaultTextAreaBuilder(Value<String> linkedValue) {
    super(linkedValue);
  }

  @Override
  public TextAreaBuilder rows(int rows) {
    this.rows = rows;
    return this;
  }

  @Override
  public TextAreaBuilder rowsColumns(int rows, int columns) {
    this.rows = rows;
    this.columns = columns;
    return this;
  }

  @Override
  public TextAreaBuilder tabSize(int tabSize) {
    this.tabSize = tabSize;
    return this;
  }

  @Override
  public TextAreaBuilder lineWrap(boolean lineWrap) {
    this.lineWrap = lineWrap;
    return this;
  }

  @Override
  public TextAreaBuilder wrapStyleWord(boolean wrapStyleWord) {
    this.wrapStyleWord = wrapStyleWord;
    return this;
  }

  @Override
  public TextAreaBuilder autoscrolls(boolean autoscrolls) {
    this.autoscrolls = autoscrolls;
    return this;
  }

  @Override
  public TextAreaBuilder document(Document document) {
    this.document = requireNonNull(document);
    return this;
  }

  @Override
  protected JTextArea createTextComponent() {
    JTextArea textArea = new JTextArea(rows, columns);
    if (document != null) {
      textArea.setDocument(document);
    }
    else {
      document = textArea.getDocument();
    }
    textArea.setAutoscrolls(autoscrolls);
    textArea.setLineWrap(lineWrap);
    textArea.setWrapStyleWord(wrapStyleWord);
    if (tabSize > 0) {
      textArea.setTabSize(tabSize);
    }

    return textArea;
  }

  @Override
  protected ComponentValue<String, JTextArea> createComponentValue(JTextArea component) {
    return new DefaultTextComponentValue<>(component, null, updateOn);
  }

  @Override
  protected void setInitialValue(JTextArea component, String initialValue) {
    component.setText(initialValue);
    component.setCaretPosition(0);
  }
}
