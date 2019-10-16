/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.tools.ui;

import org.jminor.common.Event;
import org.jminor.common.EventListener;
import org.jminor.common.EventObserver;
import org.jminor.common.Events;
import org.jminor.common.Value;
import org.jminor.common.ValueObserver;
import org.jminor.common.Values;
import org.jminor.swing.common.tools.ItemRandomizer;
import org.jminor.swing.common.tools.ItemRandomizerModel;
import org.jminor.swing.common.ui.UiUtil;

import javax.swing.ButtonModel;
import javax.swing.DefaultListModel;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.ListSelectionModel;
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
  private final JPanel configPanel = new JPanel(UiUtil.createGridLayout(0, 1));
  private final JList<ItemRandomizer.RandomItem<T>> itemList = new JList<>(new DefaultListModel<>());
  private final Event selectedItemChangedEvent = Events.event();

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
  public void addSelectedItemListener(final EventListener listener) {
    selectedItemChangedEvent.addListener(listener);
  }

  /**
   * @param listener the listener to remove
   */
  public void removeSelectedItemListener(final EventListener listener) {
    selectedItemChangedEvent.removeListener(listener);
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
    for (final ItemRandomizer.RandomItem<T> item : items) {
      ((DefaultListModel<ItemRandomizer.RandomItem<T>>) itemList.getModel()).addElement(item);
    }
    itemList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
    itemList.addListSelectionListener(e -> {
      handleSelectionChanged();
      selectedItemChangedEvent.fire();
    });
    setLayout(UiUtil.createBorderLayout());
    add(new JScrollPane(itemList), BorderLayout.CENTER);
    add(configPanel, BorderLayout.SOUTH);
  }

  private void handleSelectionChanged() {
    configPanel.removeAll();
    for (final ItemRandomizer.RandomItem<T> item : getSelectedItems()) {
      configPanel.add(initializeWeightPanel(item));
    }
    revalidate();
  }

  /**
   * Returns a JPanel with controls configuring the weight of the given item
   * @param item the item for which to create a configuration panel
   * @return a control panel for the item weight
   */
  private JPanel initializeWeightPanel(final ItemRandomizerModel.RandomItem<T> item) {
    final JPanel panel = new JPanel(UiUtil.createBorderLayout());
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
    Values.link(new EnabledModelValue(item), new EnabledUIValue(enabledBox.getModel()));

    return enabledBox;
  }

  /**
   * Returns a SpinnerModel for controlling the weight of the given item.
   * @param item the item
   * @return a weight controlling SpinnerModel
   */
  private SpinnerModel createWeightSpinnerModel(final T item) {
    final SpinnerNumberModel spinnerModel = new SpinnerNumberModel(model.getWeight(item), 0, Integer.MAX_VALUE, 1);
    Values.link(new WeightModelValue(item), new WeightUIValue(spinnerModel));

    return spinnerModel;
  }

  private final class EnabledModelValue implements Value<Boolean> {
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

    @Override
    public EventObserver<Boolean> getChangeObserver() {
      return model.getEnabledObserver();
    }

    @Override
    public ValueObserver<Boolean> getValueObserver() {
      return Values.valueObserver(this);
    }
  }

  private static final class EnabledUIValue implements Value<Boolean> {
    private final Event<Boolean> changeEvent = Events.event();
    private final ButtonModel buttonModel;

    private EnabledUIValue(final ButtonModel buttonModel) {
      this.buttonModel = buttonModel;
      buttonModel.addItemListener(e -> changeEvent.fire());
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

    @Override
    public EventObserver<Boolean> getChangeObserver() {
      return changeEvent.getObserver();
    }

    @Override
    public ValueObserver<Boolean> getValueObserver() {
      return Values.valueObserver(this);
    }
  }

  private final class WeightModelValue implements Value<Integer> {
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

    @Override
    public EventObserver<Integer> getChangeObserver() {
      return model.getWeightsObserver();
    }

    @Override
    public ValueObserver<Integer> getValueObserver() {
      return Values.valueObserver(this);
    }
  }

  private static final class WeightUIValue implements Value<Integer> {
    private final Event<Integer> changeEvent = Events.event();
    private final SpinnerNumberModel spinnerModel;

    private WeightUIValue(final SpinnerNumberModel spinnerModel) {
      this.spinnerModel = spinnerModel;
      spinnerModel.addChangeListener(e -> changeEvent.fire((Integer) spinnerModel.getValue()));
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

    @Override
    public EventObserver<Integer> getChangeObserver() {
      return changeEvent.getObserver();
    }

    @Override
    public ValueObserver<Integer> getValueObserver() {
      return Values.valueObserver(this);
    }
  }
}