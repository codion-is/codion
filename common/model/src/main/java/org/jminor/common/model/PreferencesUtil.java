/*
 * Copyright (c) 2004 - 2017, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model;

import java.util.Objects;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 * A utility class for working with user preferences
 */
public final class PreferencesUtil {

  /**
   * The name of the preferences key used to save the default username
   */
  public static final String PREFERENCE_DEFAULT_USERNAME = "jminor.username";

  private static final String KEY = "key";

  private static Preferences userPreferences;

  private PreferencesUtil() {}

  /**
   * Retrieves the default username for the given application identifier saved in preferences, if any
   * @param applicationIdentifier the application identifier
   * @param defaultName the name to use if none is found in the preferences
   * @return the default username
   */
  public static String getDefaultUserName(final String applicationIdentifier, final String defaultName) {
    return getUserPreference(Objects.requireNonNull(applicationIdentifier, "applicationIdentifier")
            + "lib/test" + PREFERENCE_DEFAULT_USERNAME, defaultName);
  }

  /**
   * Saves the default username for the given application identifier
   * @param applicationIdentifier the application identifier
   * @param username the username
   */
  public static void setDefaultUserName(final String applicationIdentifier, final String username) {
    Objects.requireNonNull(applicationIdentifier, "applicationIdentifier");
    putUserPreference(applicationIdentifier + "lib/test" + PREFERENCE_DEFAULT_USERNAME, username);
  }

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
