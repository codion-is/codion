/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.ui;

import org.jminor.common.model.SearchType;
import org.jminor.common.model.State;
import org.jminor.common.ui.UiUtil;
import org.jminor.common.ui.control.ControlProvider;
import org.jminor.common.ui.control.DoubleBeanPropertyLink;
import org.jminor.common.ui.control.IntBeanPropertyLink;
import org.jminor.common.ui.control.LinkType;
import org.jminor.common.ui.control.TextBeanPropertyLink;
import org.jminor.common.ui.textfield.DoubleField;
import org.jminor.common.ui.textfield.IntField;
import org.jminor.framework.Configuration;
import org.jminor.framework.DateUtil;
import org.jminor.framework.client.model.PropertyFilterModel;
import org.jminor.framework.domain.Type;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;

public class PropertyFilterPanel extends AbstractSearchPanel {

  private final State stIsDialogActive = new State();
  private final State stIsDialogShowing = new State();

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
      upperBoundField.requestFocusInWindow();
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
  @Override
  protected boolean isLowerBoundFieldRequired(Type type) {
    return type != Type.BOOLEAN;
  }

  /** {@inheritDoc} */
  @Override
  protected boolean searchTypeAllowed(final SearchType searchType) {
    return true;
  }

  /** {@inheritDoc} */
  @Override
  protected JComponent getInputField(final boolean isUpperBound) {
    final SimpleDateFormat format = initFormat();
    final JComponent field = initField(format);
    if (model.getPropertyType() == Type.BOOLEAN)
      createToggleProperty((JCheckBox) field, isUpperBound);
    else
      createTextProperty(field, isUpperBound, format);

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

  private JComponent initField(final SimpleDateFormat format) {
    switch (model.getPropertyType()) {
      case DATE:
      case TIMESTAMP:
        return UiUtil.createFormattedField(DateUtil.getDateMask(format));
      case DOUBLE:
        return new DoubleField(4);
      case INT:
        return new IntField(4);
      case BOOLEAN:
        return new JCheckBox();
      default:
        return new JTextField(4);
    }
  }

  private SimpleDateFormat initFormat() {
    if (model.getPropertyType() == Type.TIMESTAMP)
      return new SimpleDateFormat((String) Configuration.getValue(Configuration.DEFAULT_TIMESTAMP_FORMAT));
    if (model.getPropertyType() == Type.DATE)
      return new SimpleDateFormat((String) Configuration.getValue(Configuration.DEFAULT_DATE_FORMAT));

    return null;
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
      @Override
      public void windowClosing(WindowEvent e) {
        inactivateDialog();
      }
    });
  }

  private void createToggleProperty(final JCheckBox checkBox, final boolean isUpperBound) {
    ControlProvider.bindToggleButtonAndProperty(checkBox, model,
            isUpperBound ? PropertyFilterModel.UPPER_BOUND_PROPERTY : PropertyFilterModel.LOWER_BOUND_PROPERTY,
            null, isUpperBound ? model.evtUpperBoundChanged : model.evtLowerBoundChanged);
  }

  private TextBeanPropertyLink createTextProperty(final JComponent component, boolean isUpper, final SimpleDateFormat format) {
    switch(model.getPropertyType()) {
      case INT:
        return new IntBeanPropertyLink((IntField) component, model,
                isUpper ? PropertyFilterModel.UPPER_BOUND_PROPERTY : PropertyFilterModel.LOWER_BOUND_PROPERTY,
                isUpper ? model.evtUpperBoundChanged : model.evtLowerBoundChanged, null) {
          @Override
          public void setModelPropertyValue(final Object obj) {
            super.setModelPropertyValue(obj instanceof String && obj.equals("") ? null : obj);
          }
        };
      case DOUBLE:
        return new DoubleBeanPropertyLink((DoubleField) component, model,
                isUpper ? PropertyFilterModel.UPPER_BOUND_PROPERTY : PropertyFilterModel.LOWER_BOUND_PROPERTY,
                isUpper ? model.evtUpperBoundChanged : model.evtLowerBoundChanged, null) {
          @Override
          public void setModelPropertyValue(final Object obj) {
            super.setModelPropertyValue(obj instanceof String && obj.equals("") ? null : obj);
          }
        };
      case DATE:
        return new TextBeanPropertyLink((JTextField) component, model,
                isUpper ? PropertyFilterModel.UPPER_BOUND_PROPERTY : PropertyFilterModel.LOWER_BOUND_PROPERTY,
                Date.class, isUpper ? model.evtUpperBoundChanged : model.evtLowerBoundChanged, null,
                LinkType.READ_WRITE, format);
      case TIMESTAMP:
        return new TextBeanPropertyLink((JTextField) component, model,
                isUpper ? PropertyFilterModel.UPPER_BOUND_PROPERTY : PropertyFilterModel.LOWER_BOUND_PROPERTY,
                Timestamp.class, isUpper ? model.evtUpperBoundChanged : model.evtLowerBoundChanged, null,
                LinkType.READ_WRITE, format) {
          @Override
          protected Object getParsedValue() {
            final Date date = (Date) super.getParsedValue();
            if (date != null)
              return new Timestamp(date.getTime());

            return null;
          }
        };
      default:
        return new TextBeanPropertyLink((JTextField) component, model,
                isUpper ? PropertyFilterModel.UPPER_BOUND_PROPERTY : PropertyFilterModel.LOWER_BOUND_PROPERTY,
                String.class, isUpper ? model.evtUpperBoundChanged : model.evtLowerBoundChanged, null);
    }
  }
}