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
 * Copyright (c) 2024 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.swing.framework.ui;

import is.codion.common.model.preferences.UserPreferences;
import is.codion.common.utilities.Text;
import is.codion.common.utilities.user.User;
import is.codion.framework.domain.DomainType;

import org.json.JSONObject;
import org.jspecify.annotations.Nullable;

import java.awt.Dimension;
import java.util.prefs.Preferences;

import static is.codion.framework.model.EntityApplicationModel.PREFERENCES_KEY;
import static java.lang.Integer.parseInt;

final class ApplicationPreferences {

	private static final String DEFAULT_USERNAME_KEY = "defaultUsername";
	private static final String LOOK_AND_FEEL_KEY = "lookAndFeel";
	private static final String SCALING_KEY = "scaling";
	private static final String FRAME_SIZE_KEY = "frameSize";
	private static final String FRAME_MAXIMIZED_KEY = "maximized";
	private static final String APPLICATION_PANEL = "applicationPanel";
	private static final String EMPTY_JSON_OBJECT = "{}";

	private final @Nullable String defaultUsername;
	private final @Nullable String lookAndFeel;
	private final int scaling;
	private final @Nullable Dimension frameSize;
	private final boolean frameMaximized;

	ApplicationPreferences(@Nullable String defaultUsername, @Nullable String lookAndFeel, int scaling,
												 @Nullable Dimension frameSize, boolean frameMaximized) {
		this.defaultUsername = defaultUsername;
		this.lookAndFeel = lookAndFeel;
		this.scaling = scaling;
		this.frameSize = frameSize;
		this.frameMaximized = frameMaximized;
	}

	@Nullable String lookAndFeel() {
		return lookAndFeel;
	}

	int scaling() {
		return scaling;
	}

	@Nullable Dimension frameSize() {
		return frameSize;
	}

	boolean frameMaximized() {
		return frameMaximized;
	}

	void save(Preferences preferences) {
		preferences.put(APPLICATION_PANEL, preferences().toString());
	}

	@Nullable User defaultLoginUser() {
		return defaultUsername == null || defaultUsername.isEmpty() ? null : User.user(defaultUsername);
	}

	/**
	 * @return a JSONObject containing the application preferences
	 */
	JSONObject preferences() {
		JSONObject preferences = new JSONObject();
		preferences.put(DEFAULT_USERNAME_KEY, defaultUsername);
		preferences.put(LOOK_AND_FEEL_KEY, lookAndFeel);
		preferences.put(SCALING_KEY, scaling);
		preferences.put(FRAME_SIZE_KEY, frameSize == null ? null : (frameSize.width + "x" + frameSize.height));
		preferences.put(FRAME_MAXIMIZED_KEY, frameMaximized);

		return preferences;
	}

	static ApplicationPreferences load(Class<?> applicationModelClass, DomainType domain) {
		String preferences = UserPreferences.file(PREFERENCES_KEY.optional().orElse(domain.name())).get(APPLICATION_PANEL, EMPTY_JSON_OBJECT);
		if (preferences.equals(EMPTY_JSON_OBJECT)) {
			preferences = UserPreferences.file(applicationModelClass.getName()).get(APPLICATION_PANEL, EMPTY_JSON_OBJECT);
		}

		return fromString(preferences);
	}

	static ApplicationPreferences fromString(String preferences) {
		JSONObject jsonObject = new JSONObject(preferences);

		return new ApplicationPreferences(
						jsonObject.has(DEFAULT_USERNAME_KEY) ? jsonObject.getString(DEFAULT_USERNAME_KEY) : null,
						jsonObject.has(LOOK_AND_FEEL_KEY) ? jsonObject.getString(LOOK_AND_FEEL_KEY) : null,
						jsonObject.has(SCALING_KEY) ? jsonObject.getInt(SCALING_KEY) : 100,
						jsonObject.has(FRAME_SIZE_KEY) ? parseFrameSize(jsonObject.getString(FRAME_SIZE_KEY)) : null,
						jsonObject.has(FRAME_MAXIMIZED_KEY) && jsonObject.getBoolean(FRAME_MAXIMIZED_KEY));
	}

	private static @Nullable Dimension parseFrameSize(String userPreference) {
		if (Text.nullOrEmpty(userPreference)) {
			return null;
		}

		String[] split = userPreference.split("x");

		return new Dimension(parseInt(split[0]), parseInt(split[1]));
	}
}
