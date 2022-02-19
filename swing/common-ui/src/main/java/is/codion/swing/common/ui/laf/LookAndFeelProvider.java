/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.laf;

import is.codion.common.Configuration;
import is.codion.common.item.Item;
import is.codion.common.model.UserPreferences;
import is.codion.common.value.PropertyValue;
import is.codion.common.value.Value;
import is.codion.swing.common.model.combobox.ItemComboBoxModel;
import is.codion.swing.common.ui.combobox.Completion;
import is.codion.swing.common.ui.component.Components;
import is.codion.swing.common.ui.control.Control;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.Window;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

import static java.util.Objects.requireNonNull;

/**
 * Provides a LookAndFeel implementation.
 */
public interface LookAndFeelProvider {

  /**
   * Specifies whether to change the Look and Feel dynamically when choosing<br>
   * Value type: Boolean<br>
   * Default value: false
   */
  PropertyValue<Boolean> CHANGE_DURING_SELECTION = Configuration.booleanValue("codion.swing.lookAndFeel.changeDuringSelection", false);

  /**
   * The name of the underlying LookAndFeel class
   * @return the look and feel classname
   */
  String getClassName();

  /**
   * @return a unique name representing this look and feel, the classname by default
   */
  default String getName() {
    return getClassName();
  }

  /**
   * Configures and enables this LookAndFeel.
   */
  default void enable() {
    try {
      UIManager.setLookAndFeel(getClassName());
    }
    catch (final Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Instantiates a new LookAndFeelProvider, using {@link UIManager#setLookAndFeel(String)} to enable.
   * @param classname the look and feel classname
   * @return a look and feel provider
   */
  static LookAndFeelProvider create(final String classname) {
    return create(classname, () -> {
      try {
        UIManager.setLookAndFeel(classname);
      }
      catch (final Exception e) {
        throw new RuntimeException(e);
      }
    });
  }

  /**
   * Instantiates a new LookAndFeelProvider.
   * @param classname the look and feel classname
   * @param enabler configures and enables this look and feel
   * @return a look and feel provider
   */
  static LookAndFeelProvider create(final String classname, final Runnable enabler) {
    return create(classname, classname, enabler);
  }

  /**
   * Instantiates a new LookAndFeelProvider, using {@link UIManager#setLookAndFeel(String)} to enable.
   * @param classname the look and feel classname
   * @param name a unique name
   * @return a look and feel provider
   */
  static LookAndFeelProvider create(final String classname, final String name) {
    return create(classname, name, () -> {
      try {
        UIManager.setLookAndFeel(classname);
      }
      catch (final Exception e) {
        throw new RuntimeException(e);
      }
    });
  }

  /**
   * Instantiates a new LookAndFeelProvider.
   * @param classname the look and feel classname
   * @param name a unique name
   * @param enabler configures and enables this look and feel
   * @return a look and feel provider
   */
  static LookAndFeelProvider create(final String classname, final String name, final Runnable enabler) {
    return new DefaultLookAndFeelProvider(classname, name, enabler);
  }

  /**
   * Adds the given look and feel provider.
   * Note that this overrides any existing look and feel provider with the same name.
   * @param lookAndFeelProvider the look and feel provider to add
   */
  static void addLookAndFeelProvider(final LookAndFeelProvider lookAndFeelProvider) {
    DefaultLookAndFeelProvider.LOOK_AND_FEEL_PROVIDERS.put(requireNonNull(lookAndFeelProvider).getName(), lookAndFeelProvider);
  }

  /**
   * Returns a look and feel provider with the given name, if available
   * @param name the look and feel name
   * @return a look and feel provider, an empty Optional if not found
   */
  static Optional<LookAndFeelProvider> getLookAndFeelProvider(final String name) {
    return name == null ? Optional.empty() : Optional.ofNullable(DefaultLookAndFeelProvider.LOOK_AND_FEEL_PROVIDERS.get(name));
  }

  /**
   * Allows the user the select between all available Look and Feels.
   * @param dialogOwner the dialog owner
   * @param dialogTitle the dialog title
   * @return the selected look and feel provider, an empty Optional if cancelled
   */
  static Optional<LookAndFeelProvider> selectLookAndFeel(final JComponent dialogOwner) {
    return selectLookAndFeel(dialogOwner, CHANGE_DURING_SELECTION.get());
  }


  /**
   * Allows the user the select between all available Look and Feels.
   * @param dialogOwner the dialog owner
   * @param dialogTitle the dialog title
   * @param changeDuringSelection true if the Look and Feel should change dynamically when choosing
   * @return the selected look and feel provider, an empty Optional if cancelled
   */
  static Optional<LookAndFeelProvider> selectLookAndFeel(final JComponent dialogOwner, final boolean changeDuringSelection) {
    final List<Item<LookAndFeelProvider>> items = new ArrayList<>();
    final Value<Item<LookAndFeelProvider>> currentLookAndFeel = Value.value();
    final String currentLookAndFeelClassName = UIManager.getLookAndFeel().getClass().getName();
    DefaultLookAndFeelProvider.LOOK_AND_FEEL_PROVIDERS.values().stream()
            .sorted(Comparator.comparing(LookAndFeelProvider::getName))
            .map(provider -> Item.item(provider, provider.getName()))
            .forEach(item -> {
              items.add(item);
              if (currentLookAndFeelClassName.equals(item.getValue().getClassName())) {
                currentLookAndFeel.set(item);
              }
            });
    final ItemComboBoxModel<LookAndFeelProvider> comboBoxModel = ItemComboBoxModel.createModel(items);
    currentLookAndFeel.toOptional().ifPresent(comboBoxModel::setSelectedItem);
    if (changeDuringSelection) {
      comboBoxModel.addSelectionListener(lookAndFeelProvider -> enableLookAndFeel(lookAndFeelProvider.getValue()));
    }

    final JComboBox<Item<LookAndFeelProvider>> comboBox = Components.comboBox(comboBoxModel)
            .completionMode(Completion.Mode.NONE)
            .mouseWheelScrolling(true)
            .build();

    final ResourceBundle resourceBundle = ResourceBundle.getBundle(LookAndFeelProvider.class.getName());
    final String dialogTitle = resourceBundle.getString("select_look_and_feel");

    final int option = JOptionPane.showOptionDialog(dialogOwner, comboBox, dialogTitle,
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, null, null);
    final LookAndFeelProvider selectedLookAndFeel = comboBoxModel.getSelectedValue().getValue();
    if (option == JOptionPane.OK_OPTION) {
      if (currentLookAndFeel.get().getValue() != selectedLookAndFeel) {
        enableLookAndFeel(selectedLookAndFeel);
      }

      return Optional.of(selectedLookAndFeel);
    }
    if (changeDuringSelection && currentLookAndFeel.get().getValue() != selectedLookAndFeel) {
      enableLookAndFeel(currentLookAndFeel.get().getValue());
    }

    return Optional.empty();
  }

  /**
   * Creates a {@link Control} for selecting the Look & Feel.
   * @param dialogOwner the dialog owner
   * @return a look and feel selection control
   */
  static Control selectLookAndFeelControl(final JComponent dialogOwner) {
    return selectLookAndFeelControl(dialogOwner, null);
  }

  /**
   * Creates a {@link Control} for selecting the Look & Feel.
   * @param dialogOwner the dialog owner
   * @param userPreferencePropertyName the name of the property to use when saving the selected look and feel as a user preference
   * @return a look and feel selection control
   */
  static Control selectLookAndFeelControl(final JComponent dialogOwner, final String userPreferencePropertyName) {
    final ResourceBundle resourceBundle = ResourceBundle.getBundle(LookAndFeelProvider.class.getName());
    final String caption = resourceBundle.getString("select_look_and_feel");

    return Control.builder(() -> selectLookAndFeel(dialogOwner)
                    .ifPresent(provider -> {
                      if (userPreferencePropertyName != null) {
                        UserPreferences.putUserPreference(userPreferencePropertyName, provider.getName());
                      }
                    }))
            .caption(caption)
            .build();
  }

  /**
   * Returns the look and feel specified by the given user preference or the system look and feel if no preference value is found.
   * @param userPreferencePropertyName the name of the user preference look and feel property
   * @return the look and feel specified by user preference or the default system look and feel
   */
  static String getDefaultLookAndFeelName(final String userPreferencePropertyName) {
    return UserPreferences.getUserPreference(userPreferencePropertyName, getSystemLookAndFeelClassName());
  }

  /**
   * Enables the given look and feel and updates all window component trees.
   * @param lookAndFeelProvider the look and feel provider to enable
   */
  static void enableLookAndFeel(final LookAndFeelProvider lookAndFeelProvider) {
    requireNonNull(lookAndFeelProvider).enable();
    for (final Window window : Window.getWindows()) {
      SwingUtilities.updateComponentTreeUI(window);
    }
  }

  /**
   * Note that GTKLookAndFeel is overridden with MetalLookAndFeel, since JTabbedPane
   * does not respect the 'TabbedPane.contentBorderInsets' setting, making hierachical
   * tabbed panes look bad
   * @return the default look and feel for the platform we're running on
   */
  static String getSystemLookAndFeelClassName() {
    String systemLookAndFeel = UIManager.getSystemLookAndFeelClassName();
    if (systemLookAndFeel.endsWith("GTKLookAndFeel")) {
      systemLookAndFeel = "javax.swing.plaf.metal.MetalLookAndFeel";
    }

    return systemLookAndFeel;
  }
}