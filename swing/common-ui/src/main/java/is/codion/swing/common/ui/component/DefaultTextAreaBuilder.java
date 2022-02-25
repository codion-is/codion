/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import is.codion.common.value.Value;

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

  DefaultTextAreaBuilder(final Value<String> linkedValue) {
    super(linkedValue);
  }

  @Override
  public TextAreaBuilder rows(final int rows) {
    this.rows = rows;
    return this;
  }

  @Override
  public TextAreaBuilder rowsColumns(final int rows, final int columns) {
    this.rows = rows;
    this.columns = columns;
    return this;
  }

  @Override
  public TextAreaBuilder tabSize(final int tabSize) {
    this.tabSize = tabSize;
    return this;
  }

  @Override
  public TextAreaBuilder lineWrap(final boolean lineWrap) {
    this.lineWrap = lineWrap;
    return this;
  }

  @Override
  public TextAreaBuilder wrapStyleWord(final boolean wrapStyleWord) {
    this.wrapStyleWord = wrapStyleWord;
    return this;
  }

  @Override
  public TextAreaBuilder autoscrolls(final boolean autoscrolls) {
    this.autoscrolls = autoscrolls;
    return this;
  }

  @Override
  public TextAreaBuilder document(final Document document) {
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
  protected ComponentValue<String, JTextArea> buildComponentValue(final JTextArea component) {
    return ComponentValues.textComponent(component, null, updateOn);
  }

  @Override
  protected void setInitialValue(final JTextArea component, final String initialValue) {
    component.setText(initialValue);
    component.setCaretPosition(0);
  }
}
