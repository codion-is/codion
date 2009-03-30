package org.jminor.common.ui;

import org.jminor.common.model.State;
import org.jminor.common.model.UserCancelException;
import org.jminor.common.model.formats.AbstractDateMaskFormat;
import org.jminor.framework.i18n.FrameworkMessages;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JFormattedTextField;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.text.ParseException;
import java.util.Date;

public class DateInputPanel extends JPanel {

  public final JFormattedTextField inputField;
  public final AbstractDateMaskFormat maskFormat;

  public DateInputPanel(final JFormattedTextField inputField, final AbstractDateMaskFormat maskFormat,
                        final boolean includeButton, final State enabledState) {
    super(new BorderLayout());
    this.inputField = inputField;
    this.maskFormat = maskFormat;
    add(inputField, BorderLayout.CENTER);
    if (includeButton) {
      final AbstractAction buttonAction = new AbstractAction("...") {
        public void actionPerformed(ActionEvent e) {
          try {
            Date currentValue = null;
            try {
              currentValue = maskFormat.parse(inputField.getText());
            }
            catch (ParseException e1) {/**/}
            final Date newValue = UiUtil.getDateFromUser(
                    currentValue, FrameworkMessages.get(FrameworkMessages.SELECT_DATE), inputField);
            inputField.setText(maskFormat.format(newValue));
          }
          catch (UserCancelException e1) {/**/}
        }
      };
      final JButton btnChooser = new JButton(buttonAction);
      btnChooser.setPreferredSize(UiUtil.DIMENSION18x18);
      if (enabledState != null)
        UiUtil.linkToEnabledState(enabledState, btnChooser);
      add(btnChooser, BorderLayout.EAST);
    }
  }
}
