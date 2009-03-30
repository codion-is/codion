package org.jminor.common.ui;

import org.jminor.common.i18n.Messages;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.Action;
import javax.swing.text.JTextComponent;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;

public class TextInputPanel extends JPanel {

  private final JTextComponent textComponent;
  private final String dialogTitle;
  private final Dimension txtAreaSize;

  public TextInputPanel(final JTextComponent textComponent, final String dialogTitle) {
    this(textComponent, dialogTitle, null);
  }

  public TextInputPanel(final JTextComponent textComponent, final String dialogTitle,
                        final Dimension txtAreaSize) {
    this(textComponent, dialogTitle, txtAreaSize, true);
  }

  public TextInputPanel(final JTextComponent textComponent, final String dialogTitle,
                        final Dimension txtAreaSize, final boolean buttonFocusable) {
    this.dialogTitle = dialogTitle;
    this.textComponent = textComponent;
    this.txtAreaSize = txtAreaSize;
    initializeUI(textComponent, createButton(textComponent, buttonFocusable, UiUtil.DIMENSION18x18));
  }

  public void setText(final String text) {
    textComponent.setText(text);
  }

  public String getText() {
    return textComponent.getText();
  }

  public JTextComponent getTextComponent() {
    return textComponent;
  }

  protected void initializeUI(final JTextComponent textComponent, final JButton button) {
    setLayout(new BorderLayout());
    add(textComponent, BorderLayout.CENTER);
    add(button, BorderLayout.EAST);
  }

  private JButton createButton(final JTextComponent textComponent, final boolean buttonFocusable,
                               final Dimension buttonSize) {
    final AbstractAction action = new AbstractAction("...") {
      public void actionPerformed(final ActionEvent e) {
        final JTextArea txtArea = new JTextArea(textComponent.getText());
        txtArea.setPreferredSize(txtAreaSize == null ? UiUtil.getSize(0.3) : txtAreaSize);
        txtArea.setLineWrap(true);
        txtArea.setWrapStyleWord(true);
        final JScrollPane scroller = new JScrollPane(txtArea);
        final AbstractAction okAction = new AbstractAction(Messages.get(Messages.OK)) {
          public void actionPerformed(final ActionEvent e) {
            textComponent.setText(txtArea.getText());
          }
        };
        okAction.putValue(Action.MNEMONIC_KEY, Messages.get(Messages.OK_MNEMONIC).charAt(0));
        UiUtil.showInDialog(UiUtil.getParentWindow(textComponent), scroller, true, dialogTitle, true, true, okAction);
      }
    };

    final JButton ret = new JButton(action);
    ret.setFocusable(buttonFocusable);
    if (buttonSize != null)
      ret.setPreferredSize(buttonSize);

    return ret;
  }
}
