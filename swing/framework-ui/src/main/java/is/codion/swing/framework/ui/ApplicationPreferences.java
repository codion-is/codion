/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2024 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.swing.framework.ui;

import is.codion.common.Text;
import is.codion.common.model.preferences.UserPreferences;
import is.codion.common.user.User;

import org.json.JSONObject;

import java.awt.Dimension;

import static java.lang.Boolean.parseBoolean;
import static java.lang.Integer.parseInt;

final class ApplicationPreferences {

	private static final String LEGACY_DEFAULT_USERNAME_PROPERTY = "is.codion.swing.framework.ui.defaultUsername";
	private static final String LEGACY_LOOK_AND_FEEL_PROPERTY = "is.codion.swing.framework.ui.LookAndFeel";
	private static final String LEGACY_FONT_SIZE_PROPERTY = "is.codion.swing.framework.ui.FontSize";
	private static final String LEGACY_FRAME_SIZE_PROPERTY = "is.codion.swing.framework.ui.frameSize";
	private static final String LEGACY_FRAME_MAXIMIZED_PROPERTY = "is.codion.swing.framework.ui.maximized";

	private static final String PREFERENCES_KEY = "#preferences";

	private static final String DEFAULT_USERNAME_KEY = "defaultUsername";
	private static final String LOOK_AND_FEEL_KEY = "lookAndFeel";
	private static final String SCALING_KEY = "scaling";
	private static final String FRAME_SIZE_KEY = "frameSize";
	private static final String FRAME_MAXIMIZED_KEY = "maximized";

	private final String defaultUsername;
	private final String lookAndFeel;
	private final int scaling;
	private final Dimension frameSize;
	private final boolean frameMaximized;

	ApplicationPreferences(String defaultUsername, String lookAndFeel, int scaling,
												 Dimension frameSize, boolean frameMaximized) {
		this.defaultUsername = defaultUsername;
		this.lookAndFeel = lookAndFeel;
		this.scaling = scaling;
		this.frameSize = frameSize;
		this.frameMaximized = frameMaximized;
	}

	String lookAndFeel() {
		return lookAndFeel;
	}

	int scaling() {
		return scaling;
	}

	Dimension frameSize() {
		return frameSize;
	}

	boolean frameMaximized() {
		return frameMaximized;
	}

	void save(Class<?> applicationClassName) {
		UserPreferences.set(applicationClassName.getName() + PREFERENCES_KEY, toJSONObject().toString());
	}

	User defaultLoginUser() {
		return defaultUsername == null || defaultUsername.isEmpty() ? null : User.user(defaultUsername);
	}

	private JSONObject toJSONObject() {
		JSONObject preferences = new JSONObject();
		preferences.put(DEFAULT_USERNAME_KEY, defaultUsername);
		preferences.put(LOOK_AND_FEEL_KEY, lookAndFeel);
		preferences.put(SCALING_KEY, scaling);
		preferences.put(FRAME_SIZE_KEY, frameSize.width + "x" + frameSize.height);
		preferences.put(FRAME_MAXIMIZED_KEY, frameMaximized);

		return preferences;
	}

	static ApplicationPreferences load(Class<?> applicationClass) {
		String preferences = UserPreferences.get(applicationClass.getName() + PREFERENCES_KEY, "");
		if (preferences.isEmpty()) {
			String applicationDefaultUsernameProperty = LEGACY_DEFAULT_USERNAME_PROPERTY + "#" + applicationClass.getSimpleName();
			String applicationLookAndFeelProperty = LEGACY_LOOK_AND_FEEL_PROPERTY + "#" + applicationClass.getSimpleName();
			String applicationFontSizeProperty = LEGACY_FONT_SIZE_PROPERTY + "#" + applicationClass.getSimpleName();
			String applicationFrameSizeProperty = LEGACY_FRAME_SIZE_PROPERTY + "#" + applicationClass.getSimpleName();
			String applicationFrameMaximizedProperty = LEGACY_FRAME_MAXIMIZED_PROPERTY + "#" + applicationClass.getSimpleName();

			return new ApplicationPreferences(
							UserPreferences.get(applicationDefaultUsernameProperty, System.getProperty("user.name")),
							UserPreferences.get(applicationLookAndFeelProperty),
							parseInt(UserPreferences.get(applicationFontSizeProperty, "100")),
							parseFrameSize(UserPreferences.get(applicationFrameSizeProperty, "")),
							parseBoolean(UserPreferences.get(applicationFrameMaximizedProperty, "false")));
		}

		return fromString(preferences);
	}

	private static ApplicationPreferences fromString(String preferences) {
		JSONObject jsonObject = new JSONObject(preferences);

		return new ApplicationPreferences(
						jsonObject.has(DEFAULT_USERNAME_KEY) ? jsonObject.getString(DEFAULT_USERNAME_KEY) : null,
						jsonObject.has(LOOK_AND_FEEL_KEY) ? jsonObject.getString(LOOK_AND_FEEL_KEY) : null,
						jsonObject.has(SCALING_KEY) ? jsonObject.getInt(SCALING_KEY) : 100,
						jsonObject.has(FRAME_SIZE_KEY) ? parseFrameSize(jsonObject.getString(FRAME_SIZE_KEY)) : null,
						jsonObject.has(FRAME_MAXIMIZED_KEY) && jsonObject.getBoolean(FRAME_MAXIMIZED_KEY));
	}

	private static Dimension parseFrameSize(String userPreference) {
		if (Text.nullOrEmpty(userPreference)) {
			return null;
		}

		String[] split = userPreference.split("x");

		return new Dimension(parseInt(split[0]), parseInt(split[1]));
	}
}
