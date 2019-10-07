/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model;

import java.util.Objects;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 * A utility class for working with user preferences
 */
public final class PreferencesUtil {

  private static final String KEY = "key";

  private static Preferences userPreferences;

  private PreferencesUtil() {}

  /**
   * @param key the key identifying the preference
   * @param defaultValue the default value if no preference is available
   * @return the user preference associated with the given key
   */
  public static String getUserPreference(final String key, final String defaultValue) {
    return getUserPreferences().get(Objects.requireNonNull(key, KEY), defaultValue);
  }

  /**
   * @param key the key to use to identify the preference
   * @param value the preference value to associate with the given key
   */
  public static void putUserPreference(final String key, final String value) {
    getUserPreferences().put(Objects.requireNonNull(key, KEY), value);
  }

  /**
   * Removes the preference associated with the given key
   * @param key the key to use to identify the preference to remove
   */
  public static void removeUserPreference(final String key) {
    getUserPreferences().remove(Objects.requireNonNull(key, KEY));
  }

  /**
   * Flushes the preferences to disk
   * @throws BackingStoreException in case of a backing store failure
   */
  public static void flushUserPreferences() throws BackingStoreException {
    getUserPreferences().flush();
  }

  private static synchronized Preferences getUserPreferences() {
    if (userPreferences == null) {
      userPreferences = Preferences.userRoot();
    }

    return userPreferences;
  }
}
