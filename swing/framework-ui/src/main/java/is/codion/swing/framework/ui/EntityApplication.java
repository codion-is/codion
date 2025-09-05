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
package is.codion.swing.framework.ui;

import is.codion.common.model.CancelException;
import is.codion.common.observer.Observable;
import is.codion.common.property.PropertyValue;
import is.codion.common.user.User;
import is.codion.common.version.Version;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.DomainType;
import is.codion.swing.common.ui.laf.LookAndFeelProvider;
import is.codion.swing.framework.model.SwingEntityApplicationModel;

import org.jspecify.annotations.Nullable;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.LookAndFeel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static is.codion.common.Configuration.booleanValue;
import static is.codion.common.Configuration.stringValue;

/**
 * Builds a and starts an application.
 * @param <M> the application model type
 * @param <P> the application panel type
 * @see #builder(Class, Class)
 * @see #start()
 */
public interface EntityApplication<M extends SwingEntityApplicationModel, P extends EntityApplicationPanel<M>> {

	/**
	 * Specifies the user for logging into the application on the form {@code user:password}.
	 * If one is specified no login dialog is presented.
	 * <p>
	 * Initialized with the value of the CODION_CLIENT_USER environment variable.
	 * <p>
	 * <strong>Warning:</strong> System properties are visible in process listings.
	 * Use only for development/testing. In production, use secure credential management.
	 * <ul>
	 * <li>Value type: String
	 * <li>Default value: System.getenv("CODION_CLIENT_USER")
	 * </ul>
	 * @see User#parse(String)
	 */
	PropertyValue<String> USER = stringValue("codion.client.user", System.getenv("CODION_CLIENT_USER"));

	/**
	 * Specifies whether the client saves the last successful login username,
	 * which is then displayed as the default username the next time the application is started
	 * <ul>
	 * <li>Value type: Boolean
	 * <li>Default value: true
	 * </ul>
	 */
	PropertyValue<Boolean> SAVE_DEFAULT_USERNAME = booleanValue("codion.client.saveDefaultUsername", true);

	/**
	 * Specifies whether a startup dialog should be shown
	 * <ul>
	 * <li>Value type: Boolean
	 * <li>Default value: true
	 * </ul>
	 */
	PropertyValue<Boolean> STARTUP_DIALOG = booleanValue("codion.client.startupDialog", true);

	/**
	 * @param domain the domain type
	 * @return this Builder instance
	 */
	EntityApplication<M, P> domain(DomainType domain);

	/**
	 * <p>Sets the application name, used as frame title and client type identifier when using remote connnections.
	 * <p>If no application name is set, {@link DomainType#name()} is used or
	 * {@code applicationPanelClass.getSimpleName()} in case of no domain model.
	 * @param name the application name
	 * @return this Builder instance
	 */
	EntityApplication<M, P> name(String name);

	/**
	 * @param icon the application icon
	 * @return this Builder instance
	 */
	EntityApplication<M, P> icon(ImageIcon icon);

	/**
	 * @param version the application version
	 * @return this Builder instance
	 */
	EntityApplication<M, P> version(Version version);

	/**
	 * Sets the default look and feel class, used in case no look and feel settings are found in user preferences.
	 * @param defaultLookAndFeelClass the default look and feel class
	 * @return this Builder instance
	 * @see LookAndFeelProvider
	 */
	EntityApplication<M, P> defaultLookAndFeel(Class<? extends LookAndFeel> defaultLookAndFeelClass);

	/**
	 * Sets the default look and feel classname, used in case no look and feel settings are found in user preferences.
	 * @param defaultLookAndFeelClassName the default look and feel classname
	 * @return this Builder instance
	 * @see LookAndFeelProvider
	 */
	EntityApplication<M, P> defaultLookAndFeel(String defaultLookAndFeelClassName);

	/**
	 * Sets the look and feel class, overrides any look and feel settings found in user preferences.
	 * @param lookAndFeelClass the look and feel class
	 * @return this Builder instance
	 * @see LookAndFeelProvider
	 */
	EntityApplication<M, P> lookAndFeel(Class<? extends LookAndFeel> lookAndFeelClass);

	/**
	 * Sets the look and feel classname, overrides any look and feel settings found in user preferences.
	 * @param lookAndFeelClassName the look and feel classname
	 * @return this Builder instance
	 * @see LookAndFeelProvider
	 */
	EntityApplication<M, P> lookAndFeel(String lookAndFeelClassName);

	/**
	 * Overrides {@link #connectionProvider(Function)}
	 * @param connectionProvider the connection provider
	 * @return this Builder instance
	 */
	EntityApplication<M, P> connectionProvider(EntityConnectionProvider connectionProvider);

	/**
	 * @param connectionProvider initializes the connection provider, receives the user provided by {@link #user(Supplier)}
	 * @return this Builder instance
	 */
	EntityApplication<M, P> connectionProvider(Function<User, EntityConnectionProvider> connectionProvider);

	/**
	 * @param model the application model factory
	 * @return this Builder instance
	 */
	EntityApplication<M, P> model(Function<EntityConnectionProvider, M> model);

	/**
	 * @param panel the application panel factory
	 * @return this Builder instance
	 */
	EntityApplication<M, P> panel(Function<M, P> panel);

	/**
	 * <p>The {@link User} to use to connect to the database, this user is propagated to {@link #connectionProvider(Function)}.
	 * <p>If this user is null, {@link #user(Supplier)} is used to fetch a user.
	 * @param user the application user
	 * @return this Builder instance
	 * @see #USER
	 */
	EntityApplication<M, P> user(@Nullable User user);

	/**
	 * <p>Supplies the {@link User} to use to connect to the database, this user is then propagated to {@link #connectionProvider(Function)}.
	 * <p>This may be via a login dialog or simply by returning a hardcoded instance.
	 * <p>Startup is silently cancelled in case the {@link Supplier#get()} throws a {@link CancelException}.
	 * @param userSupplier supplies the application user, for example via a login dialog
	 * @return this Builder instance
	 */
	EntityApplication<M, P> user(Supplier<User> userSupplier);

	/**
	 * @param defaultUser the default user credentials to display in a login dialog
	 * @return this Builder instance
	 */
	EntityApplication<M, P> defaultUser(@Nullable User defaultUser);

	/**
	 * @param saveDefaultUsername true if the username should be saved in user preferences after a successful login
	 * @return this Builder instance
	 */
	EntityApplication<M, P> saveDefaultUsername(boolean saveDefaultUsername);

	/**
	 * Note that this does not apply when a custom {@link #user(Supplier)} has been specified.
	 * @param loginPanelSouthComponentSupplier supplies the component to add to the
	 * {@link BorderLayout#SOUTH} position of the default login panel
	 * @return this Builder instance
	 */
	EntityApplication<M, P> loginPanelSouthComponent(Supplier<JComponent> loginPanelSouthComponentSupplier);

	/**
	 * Runs as the application is starting, but after Look and Feel initialization.
	 * Throw {@link CancelException} in order to cancel the application startup.
	 * @param onStarting run before the application is started
	 * @return this Builder instance
	 */
	EntityApplication<M, P> onStarting(@Nullable Runnable onStarting);

	/**
	 * @param onStarted called after a successful application start
	 * @return this Builder instance
	 */
	EntityApplication<M, P> onStarted(@Nullable Consumer<P> onStarted);

	/**
	 * @param frame the supplies the frame to use
	 * @return this Builder instance
	 */
	EntityApplication<M, P> frame(Supplier<JFrame> frame);

	/**
	 * @param frameTitle the frame title
	 * @return this Builder instance
	 */
	EntityApplication<M, P> frameTitle(String frameTitle);

	/**
	 * For a dynamic frame title.
	 * @param frameTitle the observable controlling the frame title
	 * @return this Builder instance
	 */
	EntityApplication<M, P> frameTitle(Observable<String> frameTitle);

	/**
	 * @param mainMenu if true then a main menu is included
	 * @return this Builder instance
	 */
	EntityApplication<M, P> mainMenu(boolean mainMenu);

	/**
	 * @param maximizeFrame specifies whether the frame should be maximized or use its preferred size
	 * @return this Builder instance
	 */
	EntityApplication<M, P> maximizeFrame(boolean maximizeFrame);

	/**
	 * @param displayFrame specifies whether the frame should be displayed or left invisible
	 * @return this Builder instance
	 */
	EntityApplication<M, P> displayFrame(boolean displayFrame);

	/**
	 * Specifies whether to set the default uncaught exception handler when starting the application, true by default.
	 * @param uncaughtExceptionHandler if true the default uncaught exception handler is set on application start
	 * @return this Builder instance
	 * @see Thread#setDefaultUncaughtExceptionHandler(Thread.UncaughtExceptionHandler)
	 */
	EntityApplication<M, P> uncaughtExceptionHandler(boolean uncaughtExceptionHandler);

	/**
	 * @param startupDialog if true then a progress dialog is displayed while the application is being initialized
	 * @return this Builder instance
	 */
	EntityApplication<M, P> startupDialog(boolean startupDialog);

	/**
	 * @param frameSize the frame size when not maximized
	 * @return this Builder instance
	 */
	EntityApplication<M, P> frameSize(@Nullable Dimension frameSize);

	/**
	 * @param defaultFrameSize the default frame size when no previous size is available in user preferences
	 * @return this Builder instance
	 */
	EntityApplication<M, P> defaultFrameSize(@Nullable Dimension defaultFrameSize);

	/**
	 * Starts the application on the Event Dispatch Thread.
	 */
	void start();

	/**
	 * Starts the application.
	 * @param onEventDispatchThread if true then startup is performed on the Event Dispatch Thread
	 */
	void start(boolean onEventDispatchThread);

	/**
	 * @param <M> the application model type
	 * @param <P> the application panel type
	 * @param applicationModelClass the application model class
	 * @param applicationPanelClass the application panel class
	 * @return a {@link EntityApplication}
	 */
	static <M extends SwingEntityApplicationModel, P extends EntityApplicationPanel<M>> EntityApplication<M, P> builder(
					Class<M> applicationModelClass, Class<P> applicationPanelClass) {
		return new DefaultEntityApplication<>(applicationModelClass, applicationPanelClass);
	}
}
