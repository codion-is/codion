/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui.builder;

import is.codion.common.state.StateObserver;
import is.codion.common.value.Value;
import is.codion.framework.domain.property.Property;
import is.codion.swing.common.ui.value.ComponentValues;
import is.codion.swing.common.ui.value.UpdateOn;

import javax.swing.JTextArea;
import javax.swing.text.AbstractDocument;
import java.awt.Dimension;
import java.util.function.Consumer;

import static is.codion.swing.common.ui.textfield.ParsingDocumentFilter.parsingDocumentFilter;
import static is.codion.swing.common.ui.textfield.StringLengthValidator.stringLengthValidator;
import static java.util.Objects.requireNonNull;

final class DefaultTextAreaBuilder extends AbstractComponentBuilder<String, JTextArea> implements TextAreaBuilder {

  private UpdateOn updateOn = UpdateOn.KEYSTROKE;
  private int rows;
  private int columns;

  DefaultTextAreaBuilder(final Property<String> attribute, final Value<String> value) {
    super(attribute, value);
  }

  @Override
  public TextAreaBuilder preferredHeight(final int preferredHeight) {
    return (TextAreaBuilder) super.preferredHeight(preferredHeight);
  }

  @Override
  public TextAreaBuilder preferredWidth(final int preferredWidth) {
    return (TextAreaBuilder) super.preferredWidth(preferredWidth);
  }

  @Override
  public TextAreaBuilder preferredSize(final Dimension preferredSize) {
    return (TextAreaBuilder) super.preferredSize(preferredSize);
  }

  @Override
  public TextAreaBuilder transferFocusOnEnter(final boolean transferFocusOnEnter) {
    return (TextAreaBuilder) super.transferFocusOnEnter(transferFocusOnEnter);
  }

  @Override
  public TextAreaBuilder enabledState(final StateObserver enabledState) {
    return (TextAreaBuilder) super.enabledState(enabledState);
  }

  @Override
  public TextAreaBuilder onBuild(final Consumer<JTextArea> onBuild) {
    return (TextAreaBuilder) super.onBuild(onBuild);
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
