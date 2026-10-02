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

import is.codion.common.reactive.observer.Observer;

/**
 * The {@link Observer} javadoc snippets, each the region of the same name.
 */
final class ObserverSnippets {

	void removeWeakListener(Observer<String> observer) {
		// Clean up dead weak references // @start region=removeWeakListener
		observer.removeWeakListener(() -> {}); // @end
	}

	void removeWeakConsumer(Observer<String> observer) {
		// Clean up dead weak references // @start region=removeWeakConsumer
		observer.removeWeakConsumer(data -> {}); // @end
	}
}
