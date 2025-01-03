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
 * Copyright (c) 2013 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.swing.framework.ui.test;

import is.codion.common.user.User;
import is.codion.framework.domain.DomainType;
import is.codion.swing.framework.model.SwingEntityApplicationModel;
import is.codion.swing.framework.ui.EntityApplicationPanel;
import is.codion.swing.framework.ui.EntityPanel;

import static is.codion.framework.db.EntityConnectionProvider.CLIENT_DOMAIN_TYPE;
import static java.util.Objects.requireNonNull;
import static javax.swing.UIManager.getCrossPlatformLookAndFeelClassName;

/**
 * A class for testing {@link EntityApplicationPanel} classes
 */
public class EntityApplicationPanelTestUnit<M extends SwingEntityApplicationModel> {

	private static final String TEST_USER = "codion.test.user";

	private final Class<M> modelClass;
	private final Class<? extends EntityApplicationPanel<M>> panelClass;
	private final User user;
	private final DomainType domainType;

	/**
	 * Instantiates a new entity application panel test unit,
	 * using the User specified by the 'codion.test.user' system property.
	 * @param modelClass the application model class
	 * @param panelClass the application panel class
	 */
	protected EntityApplicationPanelTestUnit(Class<M> modelClass, Class<? extends EntityApplicationPanel<M>> panelClass) {
		this(modelClass, panelClass, testUser());
	}

	/**
	 * Instantiates a new entity application panel test unit
	 * @param modelClass the application model class
	 * @param panelClass the application panel class
	 * @param user the application user
	 */
	protected EntityApplicationPanelTestUnit(Class<M> modelClass, Class<? extends EntityApplicationPanel<M>> panelClass,
																					 User user) {
		this(modelClass, panelClass, user, CLIENT_DOMAIN_TYPE.getOrThrow());
	}

	/**
	 * Instantiates a new entity application panel test unit
	 * @param modelClass the application model class
	 * @param panelClass the application panel class
	 * @param user the application user
	 * @param domainType the application domain type
	 */
	protected EntityApplicationPanelTestUnit(Class<M> modelClass, Class<? extends EntityApplicationPanel<M>> panelClass,
																					 User user, DomainType domainType) {
		this.modelClass = requireNonNull(modelClass);
		this.panelClass = requireNonNull(panelClass);
		this.user = requireNonNull(user);
		this.domainType = requireNonNull(domainType);
	}

	/**
	 * Instantiates the panel and initializes it along with all its entity panels
	 */
	protected final void testInitialize() {
		EntityApplicationPanel.builder(modelClass, panelClass)
						.lookAndFeelClassName(getCrossPlatformLookAndFeelClassName())
						.domainType(domainType)
						.automaticLoginUser(user)
						.saveDefaultUsername(false)
						.setUncaughtExceptionHandler(false)
						.displayStartupDialog(false)
						.displayFrame(false)
						.onApplicationStarted(this::testApplicationPanel)
						.start(false);
	}

	private void testApplicationPanel(EntityApplicationPanel<M> applicationPanel) {
		applicationPanel.entityPanels().forEach(this::initialize);
		applicationPanel.applicationModel().connectionProvider().close();
	}

	private void initialize(EntityPanel entityPanel) {
		entityPanel.initialize();
		entityPanel.detailPanels().get().forEach(this::initialize);
	}

	private static User testUser() {
		String testUser = System.getProperty(TEST_USER);
		if (testUser == null) {
			throw new IllegalStateException("Required property '" + TEST_USER + "' not set");
		}

		return User.parse(testUser);
	}
}
