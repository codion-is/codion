/*
 * Copyright (c) 2016 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.model;

import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import static java.util.Objects.requireNonNull;

/**
 * A utility class for working with user preferences
 */
public final class UserPreferences {

  private static final String KEY = "key";

  private static Preferences preferences;

  private UserPreferences() {}

  /**
   * @param key the key identifying the preference
   * @param defaultValue the default value if no preference is available
   * @return the user preference associated with the given key
   */
  public static String getUserPreference(String key, String defaultValue) {
    return userPreferences().get(requireNonNull(key, KEY), defaultValue);
  }

  /**
   * @param key the key to use to identify the preference
   * @param value the preference value to associate with the given key
   */
  public static void setUserPreference(String key, String value) {
    userPreferences().put(requireNonNull(key, KEY), value);
  }

  /**
   * Removes the preference associated with the given key
   * @param key the key to use to identify the preference to remove
   */
  public static void removeUserPreference(String key) {
    userPreferences().remove(requireNonNull(key, KEY));
  }

  /**
   * Flushes the preferences to disk
   * @throws BackingStoreException in case of a backing store failure
   */
  public static void flushUserPreferences() throws BackingStoreException {
    userPreferences().flush();
  }

  private static synchronized Preferences userPreferences() {
    if (preferences == null) {
      preferences = Preferences.userRoot();
    }

    return preferences;
  }
}
