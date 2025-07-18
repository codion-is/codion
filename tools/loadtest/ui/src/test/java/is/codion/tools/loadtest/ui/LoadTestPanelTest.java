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
 * Copyright (c) 2010 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.tools.loadtest.ui;

import is.codion.common.user.User;
import is.codion.tools.loadtest.LoadTest;
import is.codion.tools.loadtest.model.LoadTestModel;

import org.junit.jupiter.api.Test;

import static is.codion.tools.loadtest.model.LoadTestModel.loadTestModel;
import static is.codion.tools.loadtest.ui.LoadTestPanel.loadTestPanel;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class LoadTestPanelTest {

	@Test
	void test() {
		LoadTest<Object> loadTest = LoadTest.builder()
						.createApplication(user -> new Object())
						.closeApplication(object -> {})
						.user(User.user("test"))
						.minimumThinkTime(25)
						.maximumThinkTime(50)
						.loginDelayFactor(2)
						.applicationBatchSize(2)
						.build();
		LoadTestModel<Object> model = loadTestModel(loadTest);
		LoadTestPanel<Object> panel = loadTestPanel(model);
		assertEquals(model, panel.model());
		loadTest.shutdown();
	}

	@Test
	void constructorNullModel() {
		assertThrows(NullPointerException.class, () -> loadTestPanel(null));
	}
}
