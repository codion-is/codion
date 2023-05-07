/*
 * Copyright (c) 2021 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.laf;

import is.codion.common.model.UserPreferences;
import is.codion.swing.common.ui.Utilities;

import javax.swing.LookAndFeel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.Window;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

/**
 * Provides a LookAndFeel implementation.
 */
public interface LookAndFeelProvider {

  /**
   * The classname of the underlying LookAndFeel
   * @return the look and feel classname
   */
  String className();

  /**
   * @return a user-friendly name representing this look and feel, the classname by default
   */
  default String name() {
    return className();
  }

  /**
   * Configures and enables this LookAndFeel.
   * Calls {@link UIManager#setLookAndFeel(String)} by default, override to add any custom configuration.
   */
  default void enable() {
    try {
      UIManager.setLookAndFeel(className());
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * @return the LookAndFeel instance represented by this provider
   * @throws Exception in case an instance could not be created
   */
  default LookAndFeel lookAndFeel() throws Exception {
    return (LookAndFeel) Class.forName(className()).getDeclaredConstructor().newInstance();
  }

  /**
   * Instantiates a new LookAndFeelProvider, using {@link UIManager#setLookAndFeel(String)} to enable.
   * @param className the look and feel classname
   * @return a look and feel provider
   */
  static LookAndFeelProvider lookAndFeelProvider(String className) {
    return lookAndFeelProvider(className, () -> {
      try {
        UIManager.setLookAndFeel(className);
      }
      catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
  }

  /**
   * Instantiates a new LookAndFeelProvider.
   * @param className the look and feel classname
   * @param enabler configures and enables this look and feel
   * @return a look and feel provider
   */
  static LookAndFeelProvider lookAndFeelProvider(String className, Runnable enabler) {
    return lookAndFeelProvider(className, className, enabler);
  }

  /**
   * Instantiates a new LookAndFeelProvider, using {@link UIManager#setLookAndFeel(String)} to enable.
   * @param className the look and feel classname
   * @param name a user-friendly name
   * @return a look and feel provider
   */
  static LookAndFeelProvider lookAndFeelProvider(String className, String name) {
    return lookAndFeelProvider(className, name, () -> {
      try {
        UIManager.setLookAndFeel(className);
      }
      catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
  }

  /**
   * Instantiates a new LookAndFeelProvider.
   * @param classname the look and feel classname
   * @param name a user-friendly name
   * @param enabler configures and enables this look and feel
   * @return a look and feel provider
   */
  static LookAndFeelProvider lookAndFeelProvider(String classname, String name, Runnable enabler) {
    return new DefaultLookAndFeelProvider(classname, name, enabler);
  }

  /**
   * Adds the given look and feel provider.
   * Note that this replaces any existing look and feel provider based on the same classname.
   * @param lookAndFeelProvider the look and feel provider to add
   */
  static void addLookAndFeelProvider(LookAndFeelProvider lookAndFeelProvider) {
    DefaultLookAndFeelProvider.LOOK_AND_FEEL_PROVIDERS.put(requireNonNull(lookAndFeelProvider).className(), lookAndFeelProvider);
  }

  /**
   * @return the available {@link LookAndFeelProvider}s
   * @see #addLookAndFeelProvider(LookAndFeelProvider)
   */
  static Map<String, LookAndFeelProvider> lookAndFeelProviders() {
    return Collections.unmodifiableMap(DefaultLookAndFeelProvider.LOOK_AND_FEEL_PROVIDERS);
  }

  /**
   * Returns a look and feel provider with the given classname, if available
   * @param className the look and feel classname
   * @return a look and feel provider, an empty Optional if not found
   */
  static Optional<LookAndFeelProvider> findLookAndFeelProvider(String className) {
    return className == null ? Optional.empty() : Optional.ofNullable(DefaultLookAndFeelProvider.LOOK_AND_FEEL_PROVIDERS.get(className));
  }

  /**
   * Returns the look and feel specified by the given user preference or the system look and feel if no preference value is found.
   * @param userPreferencePropertyName the name of the user preference look and feel property
   * @return the look and feel specified by user preference or the default system look and feel
   */
  static String defaultLookAndFeelName(String userPreferencePropertyName) {
    return UserPreferences.getUserPreference(userPreferencePropertyName, Utilities.systemLookAndFeelClassName());
  }

  /**
   * Enables the given look and feel and updates all window component trees.
   * @param lookAndFeelProvider the look and feel provider to enable
   */
  static void enableLookAndFeel(LookAndFeelProvider lookAndFeelProvider) {
    requireNonNull(lookAndFeelProvider).enable();
    for (Window window : Window.getWindows()) {
      SwingUtilities.updateComponentTreeUI(window);
    }
  }
}