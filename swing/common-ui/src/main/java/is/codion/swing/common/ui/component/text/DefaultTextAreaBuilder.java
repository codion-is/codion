/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2021 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.text;

import is.codion.common.value.Value;
import is.codion.swing.common.ui.component.value.ComponentValue;

import javax.swing.JTextArea;
import javax.swing.text.Document;

import static java.util.Objects.requireNonNull;

final class DefaultTextAreaBuilder extends AbstractTextComponentBuilder<String, JTextArea, TextAreaBuilder> implements TextAreaBuilder {

  private int columns;
  private int rows;
  private int tabSize;
  private boolean lineWrap = false;
  private boolean wrapStyleWord = false;
  private boolean autoscrolls = false;
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
}
