/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.tools.ui;

import org.jminor.common.event.Event;
import org.jminor.common.event.EventDataListener;
import org.jminor.common.event.Events;
import org.jminor.common.value.AbstractValue;
import org.jminor.swing.common.tools.ItemRandomizer;
import org.jminor.swing.common.tools.ItemRandomizerModel;
import org.jminor.swing.common.ui.layout.Layouts;

import javax.swing.ButtonModel;
import javax.swing.DefaultListModel;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static java.util.Objects.requireNonNull;

/**
 * A default UI for the ItemRandomizer class.
 * @param <T> the type of items being randomized
 */
public final class ItemRandomizerPanel<T> extends JPanel {

  private static final int SPINNER_COLUMNS = 3;

  private final ItemRandomizer<T> model;
  private final JPanel configPanel = new JPanel(Layouts.createGridLayout(0, 1));
  private final JList<ItemRandomizer.RandomItem<T>> itemList = new JList<>(new DefaultListModel<>());
  private final Event<List<ItemRandomizer.RandomItem<T>>> selectedItemChangedEvent = Events.event();

  /**
   * Instantiates a new RandomItemPanel.
   * @param itemRandomizer the ItemRandomizer to base this panel on
   * @throws NullPointerException in case itemRandomizer is null
   */
  public ItemRandomizerPanel(final ItemRandomizer<T> itemRandomizer) {
    requireNonNull(itemRandomizer, "itemRandomizer");
    this.model = itemRandomizer;
    initializeUI();
  }

  /**
   * @return the randomizer this panel is based on
   */
  public ItemRandomizer<T> getModel() {
    return model;
  }

  /**
   * @param listener a listener notified each time the selected item changes
   */
  public void addSelectedItemListener(final EventDataListener<List<ItemRandomizer.RandomItem<T>>> listener) {
    selectedItemChangedEvent.addDataListener(listener);
  }

  /**
   * @param listener the listener to remove
   */
  public void removeSelectedItemListener(final EventDataListener listener) {
    selectedItemChangedEvent.removeDataListener(listener);
  }

  /**
   * @return the currently selected item
   */
  public List<ItemRandomizer.RandomItem<T>> getSelectedItems() {
    return itemList.getSelectedValuesList();
  }

  /**
   * Initializes the UI
   */
  private void initializeUI() {
    final List<ItemRandomizer.RandomItem<T>> items = new ArrayList<>(model.getItems());
    items.sort(Comparator.comparing(item -> item.getItem().toString()));
    items.forEach(((DefaultListModel<ItemRandomizer.RandomItem<T>>) itemList.getModel())::addElement);
    itemList.addListSelectionListener(e -> selectedItemChangedEvent.onEvent(itemList.getSelectedValuesList()));
    addSelectedItemListener(selectedItems -> {
      configPanel.removeAll();
      for (final ItemRandomizer.RandomItem<T> item : selectedItems) {
        configPanel.add(initializeWeightPanel(item));
      }
      revalidate();
    });
    setLayout(Layouts.createBorderLayout());
    add(new JScrollPane(itemList), BorderLayout.CENTER);
    add(configPanel, BorderLayout.SOUTH);
  }

  /**
   * Returns a JPanel with controls configuring the weight of the given item
   * @param item the item for which to create a configuration panel
   * @return a control panel for the item weight
   */
  private JPanel initializeWeightPanel(final ItemRandomizerModel.RandomItem<T> item) {
    final JPanel panel = new JPanel(Layouts.createBorderLayout());
    final JSpinner spinner = new JSpinner(createWeightSpinnerModel(item.getItem()));
    ((JSpinner.DefaultEditor) spinner.getEditor()).getTextField().setColumns(SPINNER_COLUMNS);
    spinner.setToolTipText(item.getItem().toString());
    final JCheckBox enabledCheckBox = createEnabledCheckBox(item.getItem());
    final JLabel weightLabel = new JLabel("Weight");
    weightLabel.setHorizontalAlignment(SwingConstants.RIGHT);

    panel.add(new JLabel(item.getItem().toString()), BorderLayout.NORTH);
    panel.add(enabledCheckBox, BorderLayout.WEST);
    panel.add(weightLabel, BorderLayout.CENTER);
    panel.add(spinner, BorderLayout.EAST);

    return panel;
  }

  /**
   * Returns a JCheckBox for controlling the enabled state of the given item.
   * @param item the item
   * @return an enabling JCheckBox
   */
  private JCheckBox createEnabledCheckBox(final T item) {
    final JCheckBox enabledBox = new JCheckBox("Enabled");
    new EnabledModelValue(item).link(new EnabledUIValue(enabledBox.getModel()));

    return enabledBox;
  }

  /**
   * Returns a SpinnerModel for controlling the weight of the given item.
   * @param item the item
   * @return a weight controlling SpinnerModel
   */
  private SpinnerModel createWeightSpinnerModel(final T item) {
    final SpinnerNumberModel spinnerModel = new SpinnerNumberModel(model.getWeight(item), 0, Integer.MAX_VALUE, 1);
    new WeightModelValue(item).link(new WeightUIValue(spinnerModel));

    return spinnerModel;
  }

  private final class EnabledModelValue extends AbstractValue<Boolean> {
    private final T item;

    private EnabledModelValue(final T item) {
      this.item = item;
    }

    @Override
    public void set(final Boolean value) {
      model.setItemEnabled(item, value);
    }

    @Override
    public Boolean get() {
      return model.isItemEnabled(item);
    }

    @Override
    public boolean isNullable() {
      return false;
    }
  }

  private static final class EnabledUIValue extends AbstractValue<Boolean> {
    private final ButtonModel buttonModel;

    private EnabledUIValue(final ButtonModel buttonModel) {
      this.buttonModel = buttonModel;
      buttonModel.addItemListener(e -> notifyValueChange());
    }

    @Override
    public void set(final Boolean value) {
      buttonModel.setSelected(value);
    }

    @Override
    public Boolean get() {
      return buttonModel.isSelected();
    }

    @Override
    public boolean isNullable() {
      return false;
    }
  }

  private final class WeightModelValue extends AbstractValue<Integer> {
    private final T item;

    private WeightModelValue(final T item) {
      this.item = item;
    }

    @Override
    public void set(final Integer value) {
      model.setWeight(item, value);
    }

    @Override
    public Integer get() {
      return model.getWeight(item);
    }

    @Override
    public boolean isNullable() {
      return false;
    }
  }

  private static final class WeightUIValue extends AbstractValue<Integer> {
    private final SpinnerNumberModel spinnerModel;

    private WeightUIValue(final SpinnerNumberModel spinnerModel) {
      this.spinnerModel = spinnerModel;
      spinnerModel.addChangeListener(e -> notifyValueChange());
    }

    @Override
    public void set(final Integer value) {
      spinnerModel.setValue(value);
    }

    @Override
    public Integer get() {
      return (Integer) spinnerModel.getValue();
    }

    @Override
    public boolean isNullable() {
      return false;
    }
  }
}