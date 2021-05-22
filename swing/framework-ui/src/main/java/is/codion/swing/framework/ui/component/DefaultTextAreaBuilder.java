/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui.component;

import is.codion.common.value.Value;
import is.codion.framework.domain.property.Property;
import is.codion.swing.common.ui.value.ComponentValues;
import is.codion.swing.common.ui.value.UpdateOn;

import javax.swing.JTextArea;
import javax.swing.text.AbstractDocument;

import static is.codion.swing.common.ui.textfield.ParsingDocumentFilter.parsingDocumentFilter;
import static is.codion.swing.common.ui.textfield.StringLengthValidator.stringLengthValidator;
import static java.util.Objects.requireNonNull;

final class DefaultTextAreaBuilder extends AbstractComponentBuilder<String, JTextArea, TextAreaBuilder>
        implements TextAreaBuilder {

  private UpdateOn updateOn = UpdateOn.KEYSTROKE;
  private int rows;
  private int columns;
  private boolean lineWrap = true;
  private boolean wrapStyleWord = true;

  DefaultTextAreaBuilder(final Property<String> attribute, final Value<String> value) {
    super(attribute, value);
    if (!property.getAttribute().isString()) {
      throw new IllegalArgumentException("Cannot create a text area for a non-string attribute");
    }
  }

  @Override
  public TextAreaBuilder updateOn(final UpdateOn updateOn) {
    this.updateOn = requireNonNull(updateOn);
    return this;
  }

  @Override
  public TextAreaBuilder rows(final int rows) {
    this.rows = rows;
    return this;
  }

  @Override
  public TextAreaBuilder columns(final int columns) {
    this.columns = columns;
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
    final JTextArea textArea = rows > 0 && columns > 0 ? new JTextArea(rows, columns) : new JTextArea();
    textArea.setLineWrap(lineWrap);
    textArea.setWrapStyleWord(wrapStyleWord);
    if (property.getMaximumLength() > 0) {
      ((AbstractDocument) textArea.getDocument()).setDocumentFilter(
              parsingDocumentFilter(stringLengthValidator(property.getMaximumLength())));
    }
    ComponentValues.textComponent(textArea, null, updateOn).link(value);

    return textArea;
  }
}
