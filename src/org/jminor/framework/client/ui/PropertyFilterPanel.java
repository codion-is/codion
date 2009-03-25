/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.ui;

import org.jminor.common.i18n.Messages;
import org.jminor.common.model.SearchType;
import org.jminor.common.model.State;
import org.jminor.common.model.UserCancelException;
import org.jminor.common.model.Util;
import org.jminor.common.model.formats.LongDateFormat;
import org.jminor.common.model.formats.ShortDashDateFormat;
import org.jminor.common.model.formats.ShortDotDateFormat;
import org.jminor.common.ui.ControlProvider;
import org.jminor.common.ui.UiUtil;
import org.jminor.common.ui.control.DoubleBeanPropertyLink;
import org.jminor.common.ui.control.IntBeanPropertyLink;
import org.jminor.common.ui.control.LinkType;
import org.jminor.common.ui.control.TextBeanPropertyLink;
import org.jminor.common.ui.textfield.DoubleField;
import org.jminor.common.ui.textfield.IntField;
import org.jminor.framework.client.model.PropertyFilterModel;
import org.jminor.framework.model.Type;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class PropertyFilterPanel extends AbstractSearchPanel {

  private final State stIsDialogActive = new State("ColumnSearchPanel.stIsDialogActive");
  private final State stIsDialogShowing = new State("ColumnSearchPanel.stIsDialogShowing");

  private JDialog searchDlg;
  private Point lastPosition;

  public PropertyFilterPanel(final PropertyFilterModel model) {
    this(model, false, false);
  }

  public PropertyFilterPanel(final PropertyFilterModel model, final boolean includeActivateBtn,
                             final boolean includeToggleAdvBtn) {
    super(model, includeActivateBtn, includeToggleAdvBtn);
  }

  /**
   * @return the last screen position
   */
  public Point getLastPosition() {
    return lastPosition;
  }

  /**
   * @return true if the dialog is active
   */
  public boolean isDialogActive() {
    return stIsDialogActive.isActive();
  }

  /**
   * @return true if the dialog is being shown
   */
  public boolean isDialogShowing() {
    return stIsDialogShowing.isActive();
  }

  public void activateDialog(Container dialogParent, Point position) {
    if (!isDialogActive()) {
      initSearchDlg(dialogParent);
      if (position == null)
        position = lastPosition;
      if (position == null)
        position = new Point(0,0);

      position.y = position.y - searchDlg.getHeight();
      searchDlg.setLocation(position);
      stIsDialogActive.setActive(true);
    }

    showDialog();
  }

  public void inactivateDialog() {
    if (isDialogActive()) {
      if (isDialogShowing())
        hideDialog();
      lastPosition = searchDlg.getLocation();
      lastPosition.y = lastPosition.y + searchDlg.getHeight();
      searchDlg.dispose();
      searchDlg = null;

      stIsDialogActive.setActive(false);
    }
  }

  public void showDialog() {
    if (isDialogActive() && !isDialogShowing()) {
      searchDlg.setVisible(true);
      upperField.requestFocusInWindow();
      stIsDialogShowing.setActive(true);
    }
  }

  public void hideDialog() {
    if (isDialogShowing()) {
      searchDlg.setVisible(false);
      stIsDialogShowing.setActive(false);
    }
  }

  /**
   * @return the dialog used to show this filter panel
   */
  public JDialog getDialog() {
    return searchDlg;
  }

  /** {@inheritDoc} */
  protected boolean isLowerFieldRequired(Type type) {
    return type != Type.BOOLEAN;
  }

  /** {@inheritDoc} */
  protected boolean searchTypeAllowed(final SearchType searchType) {
    return true;
  }

  /** {@inheritDoc} */
  protected JComponent getInputField(final boolean isUpperBound) {
    JComponent field;
    switch (model.getPropertyType()) {
      case LONG_DATE:
      case SHORT_DATE:
        field = createDateChooserField(isUpperBound, model.getPropertyType() == Type.LONG_DATE);
        break;
      case DOUBLE:
        createTextProperty(field = new DoubleField(4), isUpperBound);
        break;
      case INT:
        createTextProperty(field = new IntField(4), isUpperBound);
        break;
      case BOOLEAN:
        createToggleProperty((JCheckBox) (field = new JCheckBox()), isUpperBound);
        break;
      default:
        createTextProperty(field = new JTextField(4), isUpperBound);
    }
    if (field instanceof JTextField) {//enter button toggles the filter on/off
      ((JTextField) field).addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          getModel().setSearchEnabled(!getModel().isSearchEnabled());
        }
      });
    }
    field.setToolTipText(isUpperBound ? "a" : "b");

    return field;
  }

  private void initSearchDlg(Container parent) {
    if (searchDlg != null)
      return;

    final JDialog dlgParent = UiUtil.getParentDialog(parent);
    if (dlgParent != null)
      searchDlg = new JDialog(dlgParent, model.getCaption(), false);
    else
      searchDlg = new JDialog(UiUtil.getParentFrame(parent), model.getCaption(), false);

    final JPanel searchPanel = new JPanel(new BorderLayout());
    searchPanel.add(this, BorderLayout.NORTH);
    searchDlg.getContentPane().add(searchPanel);
    searchDlg.pack();

    stAdvancedSearch.evtStateChanged.addListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        searchDlg.pack();
      }
    });

    searchDlg.addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent e) {
        inactivateDialog();
      }
    });
  }

  private JTextField createDateChooserField(final boolean isUpperBound, final boolean useLongDate) {
    final String mask = useLongDate ? "##-##-#### ##:##" : "##-##-####";
    final JFormattedTextField ret = UiUtil.createFormattedField(mask);
    model.evtSearchModelCleared.addListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        ret.setValue(null);
      }
    });

    ret.getDocument().addDocumentListener(new DocumentListener() {
      public void changedUpdate(DocumentEvent e) {
        refreshDateField(ret, useLongDate, isUpperBound);
      }

      public void insertUpdate(DocumentEvent e) {
        refreshDateField(ret, useLongDate, isUpperBound);
      }

      public void removeUpdate(DocumentEvent e) {
        refreshDateField(ret, useLongDate, isUpperBound);
      }
    });

    ret.addKeyListener(new KeyAdapter() {
      public void keyTyped(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ENTER) {
          try {
            final Date currVal = isUpperBound ? (Date) model.getUpperBound() : (Date) model.getLowerBound();
            final Date val = UiUtil.getDateFromUser(currVal, Messages.get(Messages.CHOOSE_DATE), PropertyFilterPanel.this);
            if (isUpperBound)
              model.setUpperBound(useLongDate ? new Timestamp(val.getTime()) : val);
            else
              model.setLowerBound(useLongDate ? new Timestamp(val.getTime()) : val);
          }
          catch (UserCancelException uce) {/**/}
        }
      }
    });

    return ret;
  }

  private void refreshDateField(final JFormattedTextField dateField,
                                final boolean useLongDate, final boolean isUpperBound) {
    try {
      final String txt = dateField.getText();
      if (Util.isDateOk(txt, false, useLongDate)) {
        final SimpleDateFormat format = useLongDate ? LongDateFormat.get() : ShortDashDateFormat.get();
        final Date val = getDate(format, dateField);
        if (isUpperBound)
          model.setUpperBound(useLongDate ? new Timestamp(val.getTime()) : val);
        else
          model.setLowerBound(useLongDate ? new Timestamp(val.getTime()) : val);
      }
      else {
        if (isUpperBound)
          model.setUpperBound((Comparable) null);
        else
          model.setLowerBound((Comparable) null);
      }
    }
    catch (ParseException ex) {
      throw new RuntimeException(ex);
    }
  }

  private Date getDate(final SimpleDateFormat format, final JFormattedTextField dateField) throws ParseException {
    final Date date = format.parse(dateField.getText());
    final Calendar cal = Calendar.getInstance();
    cal.setTime(date);
    cal.set(Calendar.SECOND, 0);
    cal.set(Calendar.MILLISECOND, 0);

    return new Date(cal.getTimeInMillis());
  }

  private void createToggleProperty(final JCheckBox checkBox, final boolean isUpperBound) {
    ControlProvider.bindToggleButtonAndProperty(checkBox, model,
            isUpperBound ? PropertyFilterModel.UPPER_BOUND_PROPERTY : PropertyFilterModel.LOWER_BOUND_PROPERTY,
            null, isUpperBound ? model.evtUpperBoundChanged : model.evtLowerBoundChanged);
  }

  private TextBeanPropertyLink createTextProperty(final JComponent component, boolean isUpper) {
    switch(model.getPropertyType()) {
      case INT :
        return new IntBeanPropertyLink((IntField) component, model,
                isUpper ? PropertyFilterModel.UPPER_BOUND_PROPERTY : PropertyFilterModel.LOWER_BOUND_PROPERTY,
                isUpper ? model.evtUpperBoundChanged : model.evtLowerBoundChanged, null) {
          public void setModelPropertyValue(final Object obj) {
            if (obj instanceof String && obj.equals(""))
              super.setModelPropertyValue(null);
            else
              super.setModelPropertyValue(obj);
          }
        };
      case DOUBLE :
        return new DoubleBeanPropertyLink((DoubleField) component, model,
                isUpper ? PropertyFilterModel.UPPER_BOUND_PROPERTY : PropertyFilterModel.LOWER_BOUND_PROPERTY,
                isUpper ? model.evtUpperBoundChanged : model.evtLowerBoundChanged, null){
          public void setModelPropertyValue(final Object obj) {
            if (obj instanceof String && obj.equals(""))
              super.setModelPropertyValue(null);
            else
              super.setModelPropertyValue(obj);
          }
        };
      case SHORT_DATE :
      case LONG_DATE :
        return new TextBeanPropertyLink((JTextField) component, model,
                isUpper ? PropertyFilterModel.UPPER_BOUND_PROPERTY : PropertyFilterModel.LOWER_BOUND_PROPERTY,
                Timestamp.class, isUpper ? model.evtUpperBoundChanged : model.evtLowerBoundChanged, null,
                LinkType.READ_WRITE, model.getPropertyType() == Type.SHORT_DATE
                ? new ShortDotDateFormat() : new LongDateFormat());
      default :
        return new TextBeanPropertyLink((JTextField) component, model,
                isUpper ? PropertyFilterModel.UPPER_BOUND_PROPERTY : PropertyFilterModel.LOWER_BOUND_PROPERTY,
                String.class, isUpper ? model.evtUpperBoundChanged : model.evtLowerBoundChanged, null);
    }
  }
}