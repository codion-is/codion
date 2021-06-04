/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import is.codion.swing.common.ui.textfield.TextFields;
import is.codion.swing.common.ui.value.ComponentValue;
import is.codion.swing.common.ui.value.ComponentValues;

import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.text.AbstractDocument;
import java.util.function.Consumer;

import static is.codion.swing.common.ui.textfield.ParsingDocumentFilter.parsingDocumentFilter;
import static is.codion.swing.common.ui.textfield.StringLengthValidator.stringLengthValidator;

final class DefaultTextAreaBuilder extends AbstractTextComponentBuilder<String, JTextArea, TextAreaBuilder>
        implements TextAreaBuilder {

  private int rows;
  private boolean lineWrap = true;
  private boolean wrapStyleWord = true;
  private boolean autoscrolls = true;

  private JScrollPane scrollPane;

  @Override
  public TextAreaBuilder rows(final int rows) {
    this.rows = rows;
    return this;
  }

  @Override
  public TextAreaBuilder rowsColumns(final int rows, final int columns) {
    this.rows = rows;
    return columns(columns);
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
  public JScrollPane buildScrollPane() {
    return buildScrollPane(null);
  }

  @Override
  public JScrollPane buildScrollPane(final Consumer<JScrollPane> onBuild) {
    if (scrollPane == null) {
      scrollPane = new JScrollPane(build());
      if (onBuild != null) {
        onBuild.accept(scrollPane);
      }
    }

    return scrollPane;
  }

  @Override
  protected JTextArea buildComponent() {
    final JTextArea textArea = new JTextArea(rows, columns);
    textArea.setAutoscrolls(autoscrolls);
    textArea.setLineWrap(lineWrap);
    textArea.setWrapStyleWord(wrapStyleWord);
    textArea.setEditable(editable);
    if (margin != null) {
      textArea.setMargin(margin);
    }
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
