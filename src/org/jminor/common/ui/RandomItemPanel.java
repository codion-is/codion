/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui;

import org.jminor.common.model.RandomItemModel;
import org.jminor.common.model.Util;
import org.jminor.common.ui.control.AbstractValueLink;
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
public final class RandomItemPanel<T> extends JPanel {

  private final RandomItemModel<T> model;

  /**
   * Instantiates a new RandomItemPanel.
   * @param randomItemModel the RandomItemModel to base this panel on
   */
  public RandomItemPanel(final RandomItemModel<T> randomItemModel) {
    Util.rejectNullValue(randomItemModel, "model");
    this.model = randomItemModel;
    initializeUI();
  }

  public RandomItemModel<T> getModel() {
    return model;
  }

  /**
   * Initializes the UI
   */
  private void initializeUI() {
    final int count = model.getItemCount();
    setLayout(new FlexibleGridLayout(count * 2, 1, 5, 5, true, false));
    for (final RandomItemModel.RandomItem<T> item : model.getItems()) {
      add(new JLabel(item.getItem().toString()));
      add(initializeWeightPanel(item));
    }
  }

  /**
   * Returns a JPanel with controls configuring the weight of the given item
   * @param item the item for which to create a configuration panel
   * @return a conrol panel for the item weight
   */
  private JPanel initializeWeightPanel(final RandomItemModel.RandomItem<T> item) {
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
  private SpinnerModel createWeightSpinnerModel(final T item) {
    final SpinnerNumberModel spinnerModel = new SpinnerNumberModel(model.getWeight(item), 0, Integer.MAX_VALUE, 1);
    final AbstractValueLink<RandomItemPanel, Integer> valueLink =
            new AbstractValueLink<RandomItemPanel, Integer>(this, getModel().getWeightsObserver(), LinkType.READ_WRITE) {
      @Override
      public Integer getModelValue() {
        return getModel().getWeight(item);
      }
      @Override
      protected Integer getUIValue() {
        return (Integer) spinnerModel.getValue();
      }
      @Override
      public void setModelValue(final Integer value) {
        getModel().setWeight(item, value);
      }
      @Override
      protected void setUIValue(final Integer value) {
        spinnerModel.setValue(value);
      }
    };
    valueLink.updateUI();
    spinnerModel.addChangeListener(new ChangeListener() {
      public void stateChanged(final ChangeEvent e) {
        valueLink.updateModel();
      }
    });

    return spinnerModel;
  }
}