/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.laf;

import is.codion.common.Configuration;
import is.codion.common.item.Item;
import is.codion.common.properties.PropertyValue;
import is.codion.swing.common.model.component.combobox.ItemComboBoxModel;
import is.codion.swing.common.ui.component.Components;
import is.codion.swing.common.ui.component.combobox.Completion;
import is.codion.swing.common.ui.layout.Layouts;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;
import javax.swing.LookAndFeel;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.plaf.basic.BasicComboBoxEditor;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static javax.swing.BorderFactory.createLineBorder;

/**
 * Provides a combo box for selecting a LookAndFeel.
 * @see LookAndFeelProvider#addLookAndFeelProvider(LookAndFeelProvider)
 */
public final class LookAndFeelSelectionPanel extends JPanel {

  /**
   * Specifies whether to change the Look and Feel dynamically when choosing<br>
   * Value type: Boolean<br>
   * Default value: false
   */
  public static final PropertyValue<Boolean> CHANGE_DURING_SELECTION =
          Configuration.booleanValue("is.codion.swing.common.ui.dialog.LookAndFeelSelectionPanel.changeDuringSelection", false);

  private static final int PADDING = 10;
  private static final int BORDER_THICKNESS = 5;
  private static final int COLOR_LABEL_WIDTH = 100;

  private final ItemComboBoxModel<LookAndFeelProvider> comboBoxModel;
  private final LookAndFeelProvider originalLookAndFeel;
  private final Map<String, UIDefaults> lookAndFeelDefaults = new HashMap<>();
  private final UIDefaults nullDefaults = new UIDefaults(0, 0.1f);

  /**
   * Instantiates a new LookAndFeelSelectionPanel
   */
  public LookAndFeelSelectionPanel() {
    this(CHANGE_DURING_SELECTION.get());
  }

  /**
   * Instantiates a new LookAndFeelSelectionPanel
   * @param changeDuringSelection if true the look and feel is changed when selected
   */
  public LookAndFeelSelectionPanel(boolean changeDuringSelection) {
    this.comboBoxModel = ItemComboBoxModel.createModel(initializeAvailableLookAndFeels());
    getCurrentLookAndFeel().ifPresent(comboBoxModel::setSelectedItem);
    this.originalLookAndFeel = comboBoxModel.getSelectedValue().getValue();
    if (changeDuringSelection) {
      comboBoxModel.addSelectionListener(lookAndFeelProvider ->
              LookAndFeelProvider.enableLookAndFeel(lookAndFeelProvider.getValue()));
    }

    setLayout(Layouts.borderLayout());
    add(Components.comboBox(comboBoxModel)
            .completionMode(Completion.Mode.NONE)
            .mouseWheelScrolling(true)
            .renderer(new LookAndFeelRenderer())
            .editor(new LookAndFeelEditor())
            .border(createLineBorder(UIManager.getColor("TextField.foreground")))
            .editable(true)
            .build(), BorderLayout.NORTH);
    setBorder(BorderFactory.createEmptyBorder(PADDING, PADDING, 0, PADDING));
  }

  /**
   * @return the currently selected look and feel
   */
  public LookAndFeelProvider getSelectedLookAndFeel() {
    return comboBoxModel.getSelectedValue().getValue();
  }

  /**
   * Enables the currently selected look and feel, if it is already selected, this method does nothing
   */
  public void enableSelected() {
    String currentLookAndFeelClassName = UIManager.getLookAndFeel().getClass().getName();
    if (!getSelectedLookAndFeel().getClassName().equals(currentLookAndFeelClassName)) {
      LookAndFeelProvider.enableLookAndFeel(getSelectedLookAndFeel());
    }
  }

  /**
   * Reverts the look and feel to the look and feel active when this look and feel panel was created,
   * if it is already enabled, this method does nothing
   */
  public void revert() {
    String currentLookAndFeelClassName = UIManager.getLookAndFeel().getClass().getName();
    if (!currentLookAndFeelClassName.equals(originalLookAndFeel.getClassName())) {
      LookAndFeelProvider.enableLookAndFeel(originalLookAndFeel);
    }
  }

  private Optional<Item<LookAndFeelProvider>> getCurrentLookAndFeel() {
    String currentLookAndFeelClassName = UIManager.getLookAndFeel().getClass().getName();

    return comboBoxModel.getItems().stream()
            .filter(item -> item.getValue().getClassName().equals(currentLookAndFeelClassName))
            .findFirst();
  }

  private static List<Item<LookAndFeelProvider>> initializeAvailableLookAndFeels() {
    return LookAndFeelProvider.getLookAndFeelProviders().values().stream()
            .sorted(Comparator.comparing(LookAndFeelProvider::getName))
            .map(provider -> Item.item(provider, provider.getName()))
            .collect(Collectors.toList());
  }

  private final class LookAndFeelPanel extends JPanel {

    private final JLabel textLabel = new JLabel();
    private final JLabel colorLabel = Components.label()
            .preferredWidth(COLOR_LABEL_WIDTH)
            .build();

    private LookAndFeelPanel() {
      super(new BorderLayout());
      add(textLabel, BorderLayout.CENTER);
      add(colorLabel, BorderLayout.EAST);
    }

    private void setLookAndFeel(LookAndFeelProvider lookAndFeel, boolean selected) {
      textLabel.setOpaque(true);
      colorLabel.setOpaque(true);
      textLabel.setText(lookAndFeel.getClassName());
      UIDefaults defaults = getDefaults(lookAndFeel.getClassName());
      if (defaults == nullDefaults) {
        textLabel.setBackground(selected ? Color.LIGHT_GRAY : Color.WHITE);
        textLabel.setForeground(Color.BLACK);
        colorLabel.setBackground(Color.WHITE);
        colorLabel.setBorder(null);
      }
      else {
        textLabel.setFont(defaults.getFont("TextField.font"));
        textLabel.setBackground(defaults.getColor(selected ? "Table.selectionBackground" : "TextField.background"));
        textLabel.setForeground(defaults.getColor("TextField.foreground"));
        colorLabel.setBackground(defaults.getColor("Button.background"));
        colorLabel.setBorder(createLineBorder(defaults.getColor("ProgressBar.foreground"), BORDER_THICKNESS));
      }
    }

    private UIDefaults getDefaults(String className) {
      return lookAndFeelDefaults.computeIfAbsent(className, this::initializeLookAndFeelDefaults);
    }

    private UIDefaults initializeLookAndFeelDefaults(String className) {
      try {
        Class<LookAndFeel> clazz = (Class<LookAndFeel>) Class.forName(className);

        return clazz.getDeclaredConstructor().newInstance().getDefaults();
      }
      catch (Exception e) {
        System.err.println(e.getMessage());
        return nullDefaults;
      }
    }
  }

  private final class LookAndFeelEditor extends BasicComboBoxEditor {

    private final LookAndFeelPanel panel = new LookAndFeelPanel();

    private Item<LookAndFeelProvider> item;

    @Override
    public Component getEditorComponent() {
      return panel;
    }

    @Override
    public Object getItem() {
      return item;
    }

    @Override
    public void setItem(Object item) {
      this.item = (Item<LookAndFeelProvider>) item;
      if (this.item != null) {
        panel.setLookAndFeel(this.item.getValue(), false);
      }
    }
  }

  private final class LookAndFeelRenderer implements ListCellRenderer<Item<LookAndFeelProvider>> {

    private final LookAndFeelPanel panel = new LookAndFeelPanel();

    @Override
    public Component getListCellRendererComponent(JList<? extends Item<LookAndFeelProvider>> list, Item<LookAndFeelProvider> value,
                                                  int index, boolean isSelected, boolean cellHasFocus) {
      panel.setLookAndFeel(value.getValue(), isSelected);

      return panel;
    }
  }
}
