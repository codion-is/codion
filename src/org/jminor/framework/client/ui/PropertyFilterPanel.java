/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.ui;

import org.jminor.common.model.SearchType;
import org.jminor.common.model.State;
import org.jminor.common.ui.UiUtil;
import org.jminor.common.ui.control.DoubleBeanPropertyLink;
import org.jminor.common.ui.control.FormattedTextBeanPropertyLink;
import org.jminor.common.ui.control.IntBeanPropertyLink;
import org.jminor.common.ui.control.LinkType;
import org.jminor.common.ui.control.TextBeanPropertyLink;
import org.jminor.common.ui.control.ToggleBeanPropertyLink;
import org.jminor.common.ui.textfield.DoubleField;
import org.jminor.common.ui.textfield.IntField;
import org.jminor.framework.client.model.PropertyFilterModel;
import org.jminor.framework.client.model.util.DateUtil;
import org.jminor.framework.domain.Type;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
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

  private JDialog dialog;
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

      position.y = position.y - dialog.getHeight();
      dialog.setLocation(position);
      stIsDialogActive.setActive(true);
    }

    showDialog();
  }

  public void inactivateDialog() {
    if (isDialogActive()) {
      if (isDialogShowing())
        hideDialog();
      lastPosition = dialog.getLocation();
      lastPosition.y = lastPosition.y + dialog.getHeight();
      dialog.dispose();
      dialog = null;

      stIsDialogActive.setActive(false);
    }
  }

  public void showDialog() {
    if (isDialogActive() && !isDialogShowing()) {
      dialog.setVisible(true);
      upperBoundField.requestFocusInWindow();
      stIsDialogShowing.setActive(true);
    }
  }

  public void hideDialog() {
    if (isDialogShowing()) {
      dialog.setVisible(false);
      stIsDialogShowing.setActive(false);
    }
  }

  /**
   * @return the dialog used to show this filter panel
   */
  public JDialog getDialog() {
    return dialog;
  }

  public State stateIsDialogActive() {
    return stIsDialogActive;
  }

  public State stateIsDialogShowing() {
    return stIsDialogShowing;
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
    final SimpleDateFormat format = getInputFormat();
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

  private void initSearchDlg(Container parent) {
    if (dialog != null)
      return;

    final JDialog dlgParent = UiUtil.getParentDialog(parent);
    if (dlgParent != null)
      dialog = new JDialog(dlgParent, model.getCaption(), false);
    else
      dialog = new JDialog(UiUtil.getParentFrame(parent), model.getCaption(), false);

    final JPanel searchPanel = new JPanel(new BorderLayout());
    searchPanel.add(this, BorderLayout.NORTH);
    dialog.getContentPane().add(searchPanel);
    dialog.pack();

    stateAdvancedSearch().eventStateChanged().addListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        dialog.pack();
      }
    });

    dialog.addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(WindowEvent e) {
        inactivateDialog();
      }
    });
  }

  private void createToggleProperty(final JCheckBox checkBox, final boolean isUpperBound) {
    new ToggleBeanPropertyLink(checkBox.getModel(), model,
            isUpperBound ? PropertyFilterModel.UPPER_BOUND_PROPERTY : PropertyFilterModel.LOWER_BOUND_PROPERTY,
            isUpperBound ? model.eventUpperBoundChanged() : model.eventLowerBoundChanged(), null);
  }

  private TextBeanPropertyLink createTextProperty(final JComponent component, boolean isUpper, final SimpleDateFormat format) {
    switch(model.getPropertyType()) {
      case INT:
        return new IntBeanPropertyLink((IntField) component, model,
                isUpper ? PropertyFilterModel.UPPER_BOUND_PROPERTY : PropertyFilterModel.LOWER_BOUND_PROPERTY,
                isUpper ? model.eventUpperBoundChanged() : model.eventLowerBoundChanged(), null);
      case DOUBLE:
        return new DoubleBeanPropertyLink((DoubleField) component, model,
                isUpper ? PropertyFilterModel.UPPER_BOUND_PROPERTY : PropertyFilterModel.LOWER_BOUND_PROPERTY,
                isUpper ? model.eventUpperBoundChanged() : model.eventLowerBoundChanged(), null);
      case DATE:
      case TIMESTAMP:
        return new FormattedTextBeanPropertyLink((JFormattedTextField) component, model,
                isUpper ? PropertyFilterModel.UPPER_BOUND_PROPERTY : PropertyFilterModel.LOWER_BOUND_PROPERTY,
                model.getPropertyType() == Type.TIMESTAMP ? Timestamp.class : Date.class,
                isUpper ? model.eventUpperBoundChanged() : model.eventLowerBoundChanged(), LinkType.READ_WRITE, format) {
          @Override
          protected Object getUIPropertyValue() {
            final Date date = (Date) super.getUIPropertyValue();
            if (date != null)
              return model.getPropertyType() == Type.TIMESTAMP ? new Timestamp(date.getTime()) : date;

            return null;
          }
        };
      default:
        return new TextBeanPropertyLink((JTextField) component, model,
                isUpper ? PropertyFilterModel.UPPER_BOUND_PROPERTY : PropertyFilterModel.LOWER_BOUND_PROPERTY,
                String.class, isUpper ? model.eventUpperBoundChanged() : model.eventLowerBoundChanged());
    }
  }
}