/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui.builder;

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

  DefaultTextAreaBuilder(final Property<String> attribute, final Value<String> value) {
    super(attribute, value);
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
  public JTextArea build() {
    if (!property.getAttribute().isString()) {
      throw new IllegalArgumentException("Cannot create a text area for a non-string attribute");
    }

    final JTextArea textArea = setDescriptionAndEnabledState(rows > 0 && columns > 0 ? new JTextArea(rows, columns) :
            new JTextArea(), property.getDescription(), enabledState);
    setPreferredSize(textArea);
    onBuild(textArea);
    textArea.setLineWrap(true);//todo
    textArea.setWrapStyleWord(true);//todo
    if (property.getMaximumLength() > 0) {
      ((AbstractDocument) textArea.getDocument()).setDocumentFilter(
              parsingDocumentFilter(stringLengthValidator(property.getMaximumLength())));
    }
    ComponentValues.textComponent(textArea, null, updateOn).link(value);

    return textArea;
  }
}
