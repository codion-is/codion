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
package is.codion.manual.javadoc;

import is.codion.common.reactive.value.Value;
import is.codion.common.utilities.property.PropertyStore;

import java.io.IOException;
import java.nio.file.Path;

/**
 * The {@link PropertyStore} javadoc snippets, each the region of the same name.
 */
final class PropertyStoreSnippets {

  void usage() throws IOException {
    Path configurationFile = Path.of(System.getProperty("user.home") + "/app.properties"); // @start region=usage

    PropertyStore store = PropertyStore.propertyStore(configurationFile);

    Value<Boolean> featureEnabled = store.booleanValue("feature.enabled", false);
    Value<String> defaultUsername = store.stringValue("default.username", System.getProperty("user.name"));

    featureEnabled.set(true);
    defaultUsername.set("scott");

    store.writeToFile(configurationFile);

    //reverts to the default value
    featureEnabled.set(null);
    defaultUsername.set(null);

    String isFeatureEnabled = System.getProperty("feature.enabled"); // "false" // @end
  }
}
