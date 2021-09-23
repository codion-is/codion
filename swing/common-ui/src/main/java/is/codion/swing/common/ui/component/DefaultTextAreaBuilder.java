/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import is.codion.swing.common.ui.textfield.CaseDocumentFilter;
import is.codion.swing.common.ui.textfield.CaseDocumentFilter.DocumentCase;
import is.codion.swing.common.ui.textfield.StringLengthValidator;
import is.codion.swing.common.ui.value.ComponentValue;
import is.codion.swing.common.ui.value.ComponentValues;

import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.text.AbstractDocument;
import java.util.function.Consumer;

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
    final AbstractDocument document = (AbstractDocument) textArea.getDocument();
    final CaseDocumentFilter caseDocumentFilter = CaseDocumentFilter.caseDocumentFilter();
    caseDocumentFilter.addValidator(new StringLengthValidator(maximumLength));
    document.setDocumentFilter(caseDocumentFilter);
    textArea.setAutoscrolls(autoscrolls);
    textArea.setLineWrap(lineWrap);
    textArea.setWrapStyleWord(wrapStyleWord);
    textArea.setEditable(editable);
    if (margin != null) {
      textArea.setMargin(margin);
    }
    if (upperCase) {
      caseDocumentFilter.setDocumentCase(DocumentCase.UPPERCASE);
    }
    if (lowerCase) {
      caseDocumentFilter.setDocumentCase(DocumentCase.LOWERCASE);
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
