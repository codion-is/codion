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
 * Copyright (c) 2025, Björn Darri Sigurðsson.
 */
package is.codion.demos.employees.demos;

import is.codion.plugin.swing.robot.Automation;
import is.codion.plugin.swing.robot.Controller;
import is.codion.plugin.swing.robot.Narrator;

import java.util.function.Consumer;

public final class DemoScript implements Consumer<Automation> {

	@Override
	public void accept(Automation automation) {
		Controller controller = automation.controller();
		Narrator narrator = automation.narrator().orElseThrow(IllegalStateException::new);
		controller.pause(5000);//For preparing screen capture
		int defaultAutoDelay = 110;

		narrator.narrate("Add a new department");
		controller.autoDelay(defaultAutoDelay);
		controller.type("50", "Input department no.");
		controller.key("ENTER", "Transfer focus");
		controller.type("Demo", "Input department name");
		controller.key("ENTER", "Transfer focus");
		controller.autoDelay(80);
		controller.type("Austin", "Input location");
		controller.autoDelay(defaultAutoDelay);
		controller.key("alt A", "Add new record");

		controller.key("ctrl alt DOWN", "Navigate down to employees");

		narrator.narrate("Add a new employee");
		controller.autoDelay(80);
		controller.type("John");
		controller.autoDelay(defaultAutoDelay);
		controller.key("ENTER", 3, "Transfer focus");
		controller.type("M", "Select Manager");
		controller.key("ENTER", 2, "Transfer focus");
		controller.type("3000", "Input salary");
		controller.key("ENTER", 2, "Transfer focus");
		controller.key("INSERT", "Open calendar");
		controller.pause(500);
		controller.autoDelay(200);
		controller.key("LEFT", 2, "Move to the previous day");
		controller.pause(300);
		controller.key("DOWN",  "Move to the next week");
		controller.pause(500);

		controller.key("ENTER", "Accept date");
		controller.autoDelay(50);
		controller.key("LEFT", 5, "Navigate to month");
		controller.pause(1000);
		controller.autoDelay(defaultAutoDelay);
		controller.key("DOWN", "Select last month");
		controller.pause(1000);
		controller.key("shift ENTER", "Transfer focus backward");
		controller.type("1000", "Add commission");
		controller.key("alt A", "Add new record");

		narrator.narrate("Update our new employee");
		controller.key("ctrl T", "Focus table");
		controller.key("DOWN", "Select our new employee");
		controller.key("ctrl I", "Select input field");
		controller.type("S", "Select salary field");
		controller.key("ENTER", "Confirm selection");
		controller.key("ctrl A", "Select all");
		controller.type("3500", "Increase salary");
		controller.pause(800);
		controller.key("alt U", "Update employee");
		controller.pause(200);
		controller.key("ENTER", "Confirm update");
		controller.pause(1000);

		narrator.narrate("Delete our test employee");
		controller.key("ctrl T", "Focus table");
		controller.key("DELETE", "Delete selected");
		controller.pause(500);
		controller.key("ENTER", "Confirm delete");

		controller.key("ctrl alt UP", "Navigate up to departments");

		narrator.narrate("Delete our test department");
		controller.key("ctrl T", "Focus table");
		controller.key("DOWN", "Select topmost department");
		controller.key("DELETE", "Delete selected");
		controller.pause(500);
		controller.key("ENTER", "Confirm delete");

		narrator.narrate("Demonstrate table usage, searching and column configuration");
		controller.key("ctrl alt DOWN", "Navigate down");
		controller.autoDelay(20);
		controller.key("alt shift LEFT", 4, "Rezise employees panel");

		controller.autoDelay(defaultAutoDelay);

		controller.key("ctrl S", "Select search field");
		controller.type("J", "Select Job");
		controller.key("ENTER", "Accept selection");
		controller.type("C", "Select Clerk");
		controller.key("ENTER", "Refresh table data");

		controller.key("ctrl T", "Focus table");
		controller.key("RIGHT", 2, "Next column");

		controller.autoDelay(20);
		controller.key("ctrl ADD", 3, "Enlarge column");
		controller.autoDelay(defaultAutoDelay);

		controller.key("LEFT", "Next column");
		controller.autoDelay(20);
		controller.key("ctrl ADD", 4, "Enlarge column");
		controller.autoDelay(defaultAutoDelay);

		controller.key("ctrl S", "Select search field");
		controller.type("D", "Select department");
		controller.key("ENTER", "Accept selection");
		controller.type("acc", "Enter search text");
		controller.pause(600);
		controller.key("ENTER", "Perform search");
		controller.pause(600);
		controller.key("ENTER", "Refresh table data");
		controller.pause(1000);

		controller.key("DELETE", "Clear search text");
		controller.key("ENTER", "Clear selection");
		controller.key("ENTER", "Refresh table data");
		controller.pause(1000);

		controller.key("ctrl T", "Focus table");
		controller.key("LEFT", 2, "Next column");
		controller.key("shift ctrl RIGHT", 3, "Move column");
		controller.pause(1000);

		controller.key("ctrl G", "Open popup menu");
		controller.key("UP", 3, "Navigate to Columns");
		controller.key("RIGHT", "Enter Columns submenu");
		controller.key("DOWN", "Navigate to Reset");
		controller.pause(400);
		controller.key("ENTER", "Accept selection");
		controller.pause(1000);
		controller.key("alt C", "Clear panel");

		controller.key("ctrl S", "Select search field");
		controller.type("J", "Select Job");
		controller.key("ENTER", "Accept selection");
		controller.key("ctrl alt S", "Toggle search to advanded");
		controller.key("ctrl DOWN", "Select NOT EQUAL operator");
		controller.pause(800);
		controller.key("ENTER", "Refresh table data");
		controller.pause(1000);
		controller.key("ctrl ENTER", "Toggle search enabled");
		controller.key("ENTER", "Refresh table data");
		controller.key("ctrl alt S", "Toggle search to hidden");
		controller.pause(1000);

		narrator.clearNarration();
		controller.key("ctrl alt UP", "Navigate up to departments");
		controller.key("ctrl T", "Focus table");
		controller.key("ctrl G", "Open popup menu");
		controller.key("UP", 3, "Navigate to Columns");
		controller.key("RIGHT", "Enter Columns submenu");
		controller.key("DOWN", 2, "Navigate to Auto-resize...");
		controller.key("ENTER", "Accept selection");
		controller.key("DOWN", 1, "Open combo box");
		controller.key("DOWN", 5, "Navigate to All columns");
		controller.key("ENTER", "Accept combo box selection");
		controller.key("ENTER", "Accept selection");
		controller.key("ctrl E", "Focus edit panel");
		controller.pause(5000);
	}
}
