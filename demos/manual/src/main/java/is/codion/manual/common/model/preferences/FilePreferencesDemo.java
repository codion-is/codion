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
 * Copyright (c) 2026, Björn Darri Sigurðsson.
 */
package is.codion.manual.common.model.preferences;

import is.codion.common.model.preferences.FilePreferences;

import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

public final class FilePreferencesDemo {

	void usage() throws BackingStoreException {
		//tag::usage[]
		Preferences preferences = FilePreferences.filePreferences("my.config.file");
		// A key, a value and a node name may be of any length
		preferences.put("a.key.far.longer.than.the.eighty.characters.the.standard.implementation.allows.for.a.key",
						"a value of any length");
		// Writes my.config.file.json, see File Storage below
		preferences.flush();
		//end::usage[]
	}

	void hierarchy() throws BackingStoreException {
		//tag::hierarchy[]
		Preferences root = FilePreferences.filePreferences("my.config.file");

		// Create nested preference nodes
		Preferences appNode = root.node("myapp");
		Preferences uiNode = appNode.node("ui");
		Preferences dbNode = appNode.node("database");

		// Store preferences at different levels
		uiNode.put("theme", "dark");
		uiNode.put("font.size", "14");
		dbNode.put("connection.url", "jdbc:postgresql://localhost/mydb");
		dbNode.put("connection.pool.size", "10");

		// Navigate to nodes using paths
		Preferences ui = root.node("myapp/ui");
		String theme = ui.get("theme", "light"); // "dark"

		// List child nodes
		String[] appChildren = appNode.childrenNames(); // ["database", "ui"]

		// Remove entire node and its children
		dbNode.removeNode();
		root.flush();
		//end::hierarchy[]
	}

	void sharing(Preferences first, Preferences second) throws BackingStoreException {
		//tag::sharing[]
		// One instance of the application
		first.put("shared.value", "from the first");
		first.flush();

		// Another, in a JVM of its own, holding a change it has not saved
		second.put("other.value", "from the second");
		// Reloads the file, sees what the first saved and saves its own change on top of it
		second.flush();
		String value = second.get("shared.value", null); // "from the first"
		//end::sharing[]
	}
}
