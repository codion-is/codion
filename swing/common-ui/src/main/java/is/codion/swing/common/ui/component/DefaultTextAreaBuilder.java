/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import is.codion.common.value.Value;
import is.codion.swing.common.ui.textfield.TextFields;
import is.codion.swing.common.ui.value.ComponentValues;

import javax.swing.JTextArea;
import javax.swing.text.AbstractDocument;

import static is.codion.swing.common.ui.textfield.ParsingDocumentFilter.parsingDocumentFilter;
import static is.codion.swing.common.ui.textfield.StringLengthValidator.stringLengthValidator;

final class DefaultTextAreaBuilder extends AbstractTextComponentBuilder<String, JTextArea, TextAreaBuilder>
        implements TextAreaBuilder {

  private int rows;
  private boolean lineWrap = true;
  private boolean wrapStyleWord = true;

  DefaultTextAreaBuilder(final Value<String> value) {
    super(value);
  }

  @Override
  public TextAreaBuilder rows(final int rows) {
    this.rows = rows;
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
  protected JTextArea buildComponent() {
    final JTextArea textArea = new JTextArea(rows, columns);
    textArea.setLineWrap(lineWrap);
    textArea.setWrapStyleWord(wrapStyleWord);
    textArea.setEditable(editable);
    if (upperCase) {
      TextFields.upperCase(textArea);
    }
    if (lowerCase) {
      TextFields.lowerCase(textArea);
    }
    if (maximumLength > 0) {
      ((AbstractDocument) textArea.getDocument()).setDocumentFilter(
              parsingDocumentFilter(stringLengthValidator(maximumLength)));
    }
    ComponentValues.textComponent(textArea, null, updateOn).link(value);

    return textArea;
  }
}
