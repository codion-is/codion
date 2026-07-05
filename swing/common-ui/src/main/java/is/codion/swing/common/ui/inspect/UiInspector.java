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
package is.codion.swing.common.ui.inspect;

import java.awt.Component;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;

import static java.util.stream.Collectors.toList;
import static java.util.stream.StreamSupport.stream;

/**
 * Provides a description of the application state behind a UI component, as a structure of plain values
 * (maps, lists, scalars), for tooling and automation. Implementations are located using a {@link ServiceLoader},
 * so consumers depend only on this interface and not on any particular application framework.
 * <p>
 * Must be called on the event dispatch thread, since it reads UI and model state.
 */
public interface UiInspector {

	/**
	 * @param focusOwner the component to inspect, typically the current focus owner
	 * @return a description of the state behind the given component, or an empty {@link Optional} if this
	 * inspector does not apply to it
	 */
	Optional<Map<String, Object>> state(Component focusOwner);

	/**
	 * @return the available {@link UiInspector} instances, located using a {@link ServiceLoader}
	 */
	static List<UiInspector> instances() {
		return stream(ServiceLoader.load(UiInspector.class).spliterator(), false).collect(toList());
	}
}
