/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui;

import org.jminor.common.model.RandomItemModel;
import org.jminor.common.ui.control.AbstractPropertyLink;
import org.jminor.common.ui.control.LinkType;
import org.jminor.common.ui.layout.FlexibleGridLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.BorderLayout;

/**
 * A default UI for the RandomItemModel.
 */
public class RandomItemPanel extends JPanel {

  private final RandomItemModel model;

  /**
   * Instantiates a new RandomItemPanel.
   * @param model the RandomItemModel to base this panel on
   */
  public RandomItemPanel(final RandomItemModel model) {
    if (model == null)
      throw new IllegalArgumentException("Model can not be null");

    this.model = model;
    initializeUI();
  }

  public RandomItemModel getModel() {
    return model;
  }

  /**
   * Initializes the UI
   */
  protected void initializeUI() {
    final int count = model.getItemCount();
    setLayout(new FlexibleGridLayout(count * 2, 1, 5, 5, true, false));
    for (final RandomItemModel.RandomItem item : getModel().getItems()) {
      add(new JLabel(item.getItem().toString()));
      add(initializeWeightPanel(item));
    }
  }

  /**
   * Returns a JPanel with controls configuring the weight of the given item
   * @param item the item for which to create a configuration panel
   * @return a conrol panel for the item weight
   */
  protected JPanel initializeWeightPanel(final RandomItemModel.RandomItem item) {
    final JPanel panel = new JPanel(new BorderLayout(0, 0));
    final JSpinner spinner = new JSpinner(createWeightSpinnerModel(item.getItem()));
    spinner.setToolTipText(item.getItem().toString());
    panel.add(spinner, BorderLayout.CENTER);

    return panel;
  }

  /**
   * Returns a SpinnerModel for controlling the weight of the given item.
   * @param item the item
   * @return a weight controlling SpinnerModel
   */
  private SpinnerModel createWeightSpinnerModel(final Object item) {
    final SpinnerModel spinnerModel = new SpinnerNumberModel(getModel().getWeight(item), 0, Integer.MAX_VALUE, 1);
    final AbstractPropertyLink propertyLink = new AbstractPropertyLink(this, getModel().eventWeightsChanged(), LinkType.READ_WRITE) {
      @Override
      public Object getModelPropertyValue() {
        return getModel().getWeight(item);
      }
      @Override
      protected Object getUIPropertyValue() {
        return spinnerModel.getValue();
      }
      @Override
      public void setModelPropertyValue(final Object value) {
        getModel().setWeight(item, (Integer) value);
      }
      @Override
      protected void setUIPropertyValue(final Object propertyValue) {
        spinnerModel.setValue(propertyValue);
      }
    };
    propertyLink.updateUI();
    spinnerModel.addChangeListener(new ChangeListener() {
      public void stateChanged(ChangeEvent e) {
        propertyLink.updateModel();
      }
    });

    return spinnerModel;
  }
}