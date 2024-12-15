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
 * Copyright (c) 2023 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.swing.framework.ui;

import is.codion.common.i18n.Messages;
import is.codion.common.model.CancelException;
import is.codion.common.observer.Observable;
import is.codion.common.resource.MessageBundle;
import is.codion.common.user.User;
import is.codion.common.value.Value;
import is.codion.common.version.Version;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.DomainType;
import is.codion.framework.model.EntityApplicationModel;
import is.codion.swing.common.model.worker.ProgressWorker;
import is.codion.swing.common.ui.Windows;
import is.codion.swing.common.ui.dialog.Dialogs;
import is.codion.swing.common.ui.dialog.ExceptionDialogBuilder;
import is.codion.swing.common.ui.dialog.LoginDialogBuilder.LoginValidator;
import is.codion.swing.common.ui.icon.Icons;
import is.codion.swing.framework.model.SwingEntityApplicationModel;
import is.codion.swing.framework.ui.icon.FrameworkIcons;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static is.codion.common.Text.nullOrEmpty;
import static is.codion.common.resource.MessageBundle.messageBundle;
import static is.codion.framework.db.EntityConnectionProvider.CLIENT_DOMAIN_TYPE;
import static is.codion.swing.common.ui.Utilities.*;
import static is.codion.swing.common.ui.Windows.screenSizeRatio;
import static is.codion.swing.common.ui.border.Borders.emptyBorder;
import static is.codion.swing.common.ui.dialog.Dialogs.displayExceptionDialog;
import static is.codion.swing.common.ui.laf.LookAndFeelProvider.findLookAndFeelProvider;
import static is.codion.swing.common.ui.laf.LookAndFeelProvider.lookAndFeelProvider;
import static java.awt.KeyboardFocusManager.getCurrentKeyboardFocusManager;
import static java.lang.System.currentTimeMillis;
import static java.lang.Thread.setDefaultUncaughtExceptionHandler;
import static java.util.Objects.requireNonNull;
import static java.util.ResourceBundle.getBundle;

final class DefaultEntityApplicationPanelBuilder<M extends SwingEntityApplicationModel, P extends EntityApplicationPanel<M>>
				implements EntityApplicationPanel.Builder<M, P> {

	private static final Logger LOG = LoggerFactory.getLogger(DefaultEntityApplicationPanelBuilder.class);

	private static final MessageBundle MESSAGES =
					messageBundle(DefaultEntityApplicationPanelBuilder.class, getBundle(DefaultEntityApplicationPanelBuilder.class.getName()));

	private static final String CODION_APPLICATION_VERSION = "codion.application.version";
	private static final int DEFAULT_LOGO_SIZE = 68;
	private static final String DASH = " - ";

	private final Class<M> applicationModelClass;
	private final Class<P> applicationPanelClass;

	private final ApplicationPreferences preferences;

	private DomainType domainType = CLIENT_DOMAIN_TYPE.get();
	private String applicationName = "";
	private ConnectionProviderFactory connectionProviderFactory = new DefaultConnectionProviderFactory();
	private Function<EntityConnectionProvider, M> applicationModelFactory = new DefaultApplicationModelFactory();
	private Function<M, P> applicationPanelFactory = new DefaultApplicationPanelFactory();
	private Observable<String> frameTitle;

	private LoginProvider loginProvider = new DefaultDialogLoginProvider();
	private Supplier<JFrame> frameSupplier = new DefaultFrameSupplier();
	private boolean displayStartupDialog = EntityApplicationPanel.SHOW_STARTUP_DIALOG.getOrThrow();
	private ImageIcon applicationIcon;
	private Version applicationVersion;
	private boolean saveDefaultUsername = EntityApplicationModel.SAVE_DEFAULT_USERNAME.getOrThrow();
	private Supplier<JComponent> loginPanelSouthComponentSupplier = new DefaultSouthComponentSupplier();
	private Runnable beforeApplicationStarted;
	private Consumer<P> onApplicationStarted;

	private String defaultLookAndFeelClassName = systemLookAndFeelClassName();
	private String lookAndFeelClassName;
	private boolean setUncaughtExceptionHandler = true;
	private boolean maximizeFrame = false;
	private boolean displayFrame = true;
	private boolean includeMainMenu = true;
	private Dimension frameSize;
	private Dimension defaultFrameSize;
	private boolean loginRequired = EntityApplicationModel.AUTHENTICATION_REQUIRED.getOrThrow();
	private User defaultLoginUser;
	private User automaticLoginUser;

	DefaultEntityApplicationPanelBuilder(Class<M> applicationModelClass, Class<P> applicationPanelClass) {
		this.applicationModelClass = requireNonNull(applicationModelClass);
		this.applicationPanelClass = requireNonNull(applicationPanelClass);
		this.preferences = ApplicationPreferences.load(applicationPanelClass);
		this.defaultLoginUser = preferences.defaultLoginUser();
		this.frameSize = preferences.frameSize();
		this.maximizeFrame = preferences.frameMaximized();
	}

	@Override
	public EntityApplicationPanel.Builder<M, P> domainType(DomainType domainType) {
		this.domainType = requireNonNull(domainType);
		return this;
	}

	@Override
	public EntityApplicationPanel.Builder<M, P> applicationIcon(ImageIcon applicationIcon) {
		this.applicationIcon = requireNonNull(applicationIcon);
		return this;
	}

	@Override
	public EntityApplicationPanel.Builder<M, P> defaultLookAndFeelClassName(String defaultLookAndFeelClassName) {
		this.defaultLookAndFeelClassName = requireNonNull(defaultLookAndFeelClassName);
		return this;
	}

	@Override
	public EntityApplicationPanel.Builder<M, P> lookAndFeelClassName(String lookAndFeelClassName) {
		this.lookAndFeelClassName = requireNonNull(lookAndFeelClassName);
		return this;
	}

	@Override
	public EntityApplicationPanel.Builder<M, P> applicationName(String applicationName) {
		this.applicationName = requireNonNull(applicationName);
		return this;
	}

	@Override
	public EntityApplicationPanel.Builder<M, P> applicationVersion(Version applicationVersion) {
		this.applicationVersion = requireNonNull(applicationVersion);
		return this;
	}

	@Override
	public EntityApplicationPanel.Builder<M, P> applicationModelFactory(Function<EntityConnectionProvider, M> applicationModelFactory) {
		this.applicationModelFactory = requireNonNull(applicationModelFactory);
		return this;
	}

	@Override
	public EntityApplicationPanel.Builder<M, P> applicationPanelFactory(Function<M, P> applicationPanelFactory) {
		this.applicationPanelFactory = requireNonNull(applicationPanelFactory);
		return this;
	}

	@Override
	public EntityApplicationPanel.Builder<M, P> defaultLoginUser(User defaultLoginUser) {
		this.defaultLoginUser = defaultLoginUser;
		return this;
	}

	@Override
	public EntityApplicationPanel.Builder<M, P> automaticLoginUser(User automaticLoginUser) {
		this.automaticLoginUser = automaticLoginUser;
		return this;
	}

	@Override
	public EntityApplicationPanel.Builder<M, P> loginProvider(LoginProvider loginProvider) {
		this.loginProvider = requireNonNull(loginProvider);
		return this;
	}

	@Override
	public EntityApplicationPanel.Builder<M, P> saveDefaultUsername(boolean saveDefaultUsername) {
		this.saveDefaultUsername = saveDefaultUsername;
		return this;
	}

	@Override
	public EntityApplicationPanel.Builder<M, P> frameSupplier(Supplier<JFrame> frameSupplier) {
		this.frameSupplier = requireNonNull(frameSupplier);
		return this;
	}

	@Override
	public EntityApplicationPanel.Builder<M, P> frameTitle(String frameTitle) {
		return frameTitle(Value.value(requireNonNull(frameTitle)));
	}

	@Override
	public EntityApplicationPanel.Builder<M, P> frameTitle(Observable<String> frameTitle) {
		this.frameTitle = requireNonNull(frameTitle);
		return this;
	}

	@Override
	public EntityApplicationPanel.Builder<M, P> includeMainMenu(boolean includeMainMenu) {
		this.includeMainMenu = includeMainMenu;
		return this;
	}

	@Override
	public EntityApplicationPanel.Builder<M, P> maximizeFrame(boolean maximizeFrame) {
		this.maximizeFrame = maximizeFrame;
		return this;
	}

	@Override
	public EntityApplicationPanel.Builder<M, P> displayFrame(boolean displayFrame) {
		this.displayFrame = displayFrame;
		return this;
	}

	@Override
	public EntityApplicationPanel.Builder<M, P> setUncaughtExceptionHandler(boolean setUncaughtExceptionHandler) {
		this.setUncaughtExceptionHandler = setUncaughtExceptionHandler;
		return this;
	}

	@Override
	public EntityApplicationPanel.Builder<M, P> displayStartupDialog(boolean displayStartupDialog) {
		this.displayStartupDialog = displayStartupDialog;
		return this;
	}

	@Override
	public EntityApplicationPanel.Builder<M, P> frameSize(Dimension frameSize) {
		this.frameSize = frameSize;
		return this;
	}

	@Override
	public EntityApplicationPanel.Builder<M, P> defaultFrameSize(Dimension defaultFrameSize) {
		this.defaultFrameSize = defaultFrameSize;
		return this;
	}

	@Override
	public EntityApplicationPanel.Builder<M, P> loginRequired(boolean loginRequired) {
		this.loginRequired = loginRequired;
		return this;
	}

	@Override
	public EntityApplicationPanel.Builder<M, P> loginPanelSouthComponent(Supplier<JComponent> loginPanelSouthComponentSupplier) {
		this.loginPanelSouthComponentSupplier = requireNonNull(loginPanelSouthComponentSupplier);
		return this;
	}

	@Override
	public EntityApplicationPanel.Builder<M, P> beforeApplicationStarted(Runnable beforeApplicationStarted) {
		this.beforeApplicationStarted = beforeApplicationStarted;
		return this;
	}

	@Override
	public EntityApplicationPanel.Builder<M, P> onApplicationStarted(Consumer<P> onApplicationStarted) {
		this.onApplicationStarted = onApplicationStarted;
		return this;
	}

	@Override
	public EntityApplicationPanel.Builder<M, P> connectionProviderFactory(ConnectionProviderFactory connectionProviderFactory) {
		this.connectionProviderFactory = requireNonNull(connectionProviderFactory);
		return this;
	}

	@Override
	public void start() {
		start(true);
	}

	@Override
	public void start(boolean onEventDispatchThread) {
		if (onEventDispatchThread) {
			SwingUtilities.invokeLater(new ApplicationStarter());
		}
		else {
			startApplication();
		}
	}

	private void startApplication() {
		LOG.debug("{} application starting", applicationName);
		if (setUncaughtExceptionHandler) {
			setDefaultUncaughtExceptionHandler(new DisplayUncaughtExceptionAndExit());
		}
		setVersionProperty();
		enableLookAndFeel();
		configureFontsAndIcons();
		if (beforeApplicationStarted != null) {
			beforeApplicationStarted.run();
		}
		EntityConnectionProvider connectionProvider = initializeConnectionProvider(initializeUser());
		long initializationStarted = currentTimeMillis();
		if (displayStartupDialog) {
			Dialogs.progressWorkerDialog(new InitializeApplicationModel(connectionProvider))
							.title(applicationName)
							.icon(applicationIcon)
							.border(emptyBorder())
							.westPanel(createStartupIconPanel())
							.onResult(new StartApplication(initializationStarted))
							.onException(new DisplayExceptionAndExit())
							.execute();
		}
		else {
			startApplication(initializeApplicationModel(connectionProvider), initializationStarted);
		}
	}

	private void enableLookAndFeel() {
		try {
			findLookAndFeelProvider(lookAndFeelClassName())
							.orElse(lookAndFeelProvider(new LookAndFeelInfo("LookAndFeel", lookAndFeelClassName())))
							.enable();
		}
		catch (Exception e) {
			LOG.error("Exception while enabling Look and Feel", e);
		}
	}

	private String lookAndFeelClassName() {
		if (lookAndFeelClassName != null) {
			return lookAndFeelClassName;
		}

		String userPreference = preferences.lookAndFeel();

		return userPreference == null ? defaultLookAndFeelClassName : userPreference;
	}

	private void configureFontsAndIcons() {
		int fontSizePercentage = preferences.fontSize();
		int logoSize = DEFAULT_LOGO_SIZE;
		if (fontSizePercentage != 100) {
			setFontSizePercentage(fontSizePercentage);
			Icons.ICON_SIZE.set(Math.round(Icons.ICON_SIZE.getOrThrow() * (fontSizePercentage / 100f)));
			logoSize = Math.round(logoSize * (fontSizePercentage / 100f));
		}
		if (applicationIcon == null) {
			applicationIcon = FrameworkIcons.instance().logo(logoSize);
		}
	}

	/**
	 * Sets the application version as a system property, so that it appears automatically in exception dialogs.
	 */
	private void setVersionProperty() {
		if (applicationVersion != null) {
			System.setProperty(CODION_APPLICATION_VERSION, applicationVersion.toString());
		}
	}

	private void startApplication(M applicationModel, long initializationStarted) {
		JFrame applicationFrame = createFrame();
		if (setUncaughtExceptionHandler) {
			setDefaultUncaughtExceptionHandler(new DisplayUncaughtExceptionHandler(applicationFrame));
		}
		if (displayFrame) {
			applicationFrame.setTitle(MESSAGES.getString("initializing") + " " + applicationName);
			applicationFrame.setVisible(true);
		}
		try {
			P applicationPanel = initializeApplicationPanel(applicationModel);
			applicationPanel.setSaveDefaultUsername(saveDefaultUsername);
			configureFrame(applicationFrame, applicationPanel);
			LOG.info("{}, application started successfully: {} ms", applicationFrame.getTitle(), currentTimeMillis() - initializationStarted);
			if (displayFrame) {
				applicationFrame.setVisible(true);
			}
			if (onApplicationStarted != null) {
				onApplicationStarted.accept(applicationPanel);
			}
		}
		catch (Exception e) {
			displayExceptionAndExit(e, applicationFrame);
		}
	}

	private JFrame createFrame() {
		JFrame frame = frameSupplier.get();
		frame.getContentPane().setLayout(new BorderLayout());
		frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		frame.setIconImage(applicationIcon.getImage());
		if (frameSize != null) {
			frame.setSize(frameSize);
		}
		else if (defaultFrameSize != null) {
			frame.setSize(defaultFrameSize);
		}
		else {
			frame.setSize(screenSizeRatio(0.5));
		}
		frame.setLocationRelativeTo(null);
		if (maximizeFrame) {
			frame.setExtendedState(Frame.MAXIMIZED_BOTH);
		}

		return frame;
	}

	private User initializeUser() {
		if (automaticLoginUser != null) {
			return automaticLoginUser;
		}
		if (!loginRequired) {
			return null;
		}

		return loginProvider.login();
	}

	private M initializeApplicationModel(EntityConnectionProvider connectionProvider) {
		return applicationModelFactory.apply(connectionProvider);
	}

	private P initializeApplicationPanel(M applicationModel) {
		P applicationPanel = applicationPanelFactory.apply(applicationModel);
		applicationPanel.initialize();

		return applicationPanel;
	}

	private void configureFrame(JFrame frame, P applicationPanel) {
		frame.addWindowListener(new ExitOnClose(applicationPanel));
		if (frameTitle != null) {
			frame.setTitle(frameTitle.get());
			frameTitle.addConsumer(new FrameTitleConsumer(frame));
		}
		else {
			frame.setTitle(createDefaultFrameTitle(applicationPanel.applicationModel()));
		}
		if (includeMainMenu) {
			applicationPanel.createMenuBar().ifPresent(frame::setJMenuBar);
		}
		frame.setAlwaysOnTop(applicationPanel.alwaysOnTop().get());
		frame.getContentPane().add(applicationPanel, BorderLayout.CENTER);
		if (frameSize == null && defaultFrameSize == null) {
			frame.pack();
			Windows.setSizeWithinScreenBounds(frame);
			frame.setLocationRelativeTo(null);
		}
	}

	private String createDefaultFrameTitle(M applicationModel) {
		StringBuilder builder = new StringBuilder(applicationName);
		if (applicationVersion != null) {
			if (builder.length() > 0) {
				builder.append(DASH);
			}
			builder.append(applicationVersion);
		}
		if (builder.length() > 0) {
			builder.append(DASH);
		}
		builder.append(userInfo(applicationModel.connectionProvider()));

		return builder.toString();
	}

	private JPanel createStartupIconPanel() {
		JPanel panel = new JPanel(new BorderLayout());
		if (applicationIcon != null) {
			panel.add(new JLabel(applicationIcon), BorderLayout.CENTER);
		}

		return panel;
	}

	private EntityConnectionProvider initializeConnectionProvider(User user) {
		if (loginProvider instanceof DefaultEntityApplicationPanelBuilder.DefaultDialogLoginProvider &&
						((DefaultDialogLoginProvider) loginProvider).loginValidator.connectionProvider != null) {
			return ((DefaultDialogLoginProvider) loginProvider).loginValidator.connectionProvider;
		}

		return initializeConnectionProvider(user, domainType, applicationPanelClass.getName(), applicationVersion);
	}

	private EntityConnectionProvider initializeConnectionProvider(User user, DomainType domainType,
																																String clientType, Version clientVersion) {
		return connectionProviderFactory.create(user, domainType, clientType, clientVersion);
	}

	private static String userInfo(EntityConnectionProvider connectionProvider) {
		String description = connectionProvider.description();

		return removeUsernamePrefix(connectionProvider.user().username().toUpperCase()) +
						(description != null ? "@" + description.toUpperCase() : "");
	}

	private static String removeUsernamePrefix(String username) {
		String usernamePrefix = EntityApplicationModel.USERNAME_PREFIX.get();
		if (!nullOrEmpty(usernamePrefix) && username.toUpperCase().startsWith(usernamePrefix.toUpperCase())) {
			return username.substring(usernamePrefix.length());
		}

		return username;
	}

	private static void displayExceptionAndExit(Throwable exception, JFrame applicationFrame) {
		Throwable unwrapped = ExceptionDialogBuilder.unwrap(exception);
		if (unwrapped instanceof CancelException) {
			System.exit(0);
		}
		else {
			displayException(exception, applicationFrame);
			System.exit(1);
		}
	}

	private static void displayException(Throwable exception, JFrame applicationFrame) {
		if (!(exception instanceof CancelException)) {
			Window focusOwnerParentWindow = parentWindow(getCurrentKeyboardFocusManager().getFocusOwner());
			displayExceptionDialog(exception, focusOwnerParentWindow == null ? applicationFrame : focusOwnerParentWindow);
		}
	}

	private final class DefaultDialogLoginProvider implements LoginProvider {

		private final DefaultLoginValidator loginValidator = new DefaultLoginValidator();

		@Override
		public User login() {
			return Dialogs.loginDialog()
							.defaultUser(defaultLoginUser)
							.validator(loginValidator)
							.title(loginDialogTitle())
							.icon(applicationIcon)
							.southComponent(loginPanelSouthComponentSupplier.get())
							.show();
		}

		private String loginDialogTitle() {
			StringBuilder builder = new StringBuilder(applicationName);
			if (builder.length() > 0 && applicationVersion != null) {
				builder.append(DASH).append(applicationVersion);
			}
			if (builder.length() > 0) {
				builder.append(DASH);
			}

			return builder.append(Messages.login()).toString();
		}
	}

	private final class DefaultLoginValidator implements LoginValidator {

		private EntityConnectionProvider connectionProvider;

		@Override
		public void validate(User user) {
			connectionProvider = initializeConnectionProvider(user, domainType,
							applicationPanelClass.getName(), applicationVersion);
			try {
				connectionProvider.connection();//throws exception if the server is not reachable
			}
			catch (Exception e) {
				connectionProvider.close();
				connectionProvider = null;
				throw e;
			}
		}
	}

	private final class DefaultApplicationModelFactory implements Function<EntityConnectionProvider, M> {

		@Override
		public M apply(EntityConnectionProvider connectionProvider) {
			try {
				return applicationModelClass.getConstructor(EntityConnectionProvider.class).newInstance(connectionProvider);
			}
			catch (RuntimeException e) {
				throw e;
			}
			catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}

	private final class DefaultApplicationPanelFactory implements Function<M, P> {

		@Override
		public P apply(M model) {
			try {
				return applicationPanelClass.getConstructor(model.getClass()).newInstance(model);
			}
			catch (RuntimeException e) {
				throw e;
			}
			catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}

	private final class ApplicationStarter implements Runnable {

		@Override
		public void run() {
			startApplication();
		}
	}

	private final class StartApplication implements Consumer<M> {

		private final long initializationStarted;

		private StartApplication(long initializationStarted) {
			this.initializationStarted = initializationStarted;
		}

		@Override
		public void accept(M applicationModel) {
			startApplication(applicationModel, initializationStarted);
		}
	}

	private final class InitializeApplicationModel implements ProgressWorker.ResultTask<M> {

		private final EntityConnectionProvider connectionProvider;

		private InitializeApplicationModel(EntityConnectionProvider connectionProvider) {
			this.connectionProvider = connectionProvider;
		}

		@Override
		public M execute() throws Exception {
			return initializeApplicationModel(connectionProvider);
		}
	}

	private static class DisplayExceptionAndExit implements Consumer<Exception> {

		@Override
		public void accept(Exception e) {
			displayExceptionAndExit(e, null);
		}
	}

	private static final class DefaultConnectionProviderFactory implements ConnectionProviderFactory {}

	private static final class DefaultSouthComponentSupplier implements Supplier<JComponent> {

		@Override
		public JComponent get() {
			return null;
		}
	}

	private static final class DefaultFrameSupplier implements Supplier<JFrame> {

		@Override
		public JFrame get() {
			return new JFrame();
		}
	}

	private static class DisplayUncaughtExceptionHandler implements Thread.UncaughtExceptionHandler {

		private final JFrame applicationFrame;

		private DisplayUncaughtExceptionHandler(JFrame applicationFrame) {
			this.applicationFrame = applicationFrame;
		}

		@Override
		public void uncaughtException(Thread thread, Throwable exception) {
			displayException(exception, applicationFrame);
		}
	}

	private static class DisplayUncaughtExceptionAndExit implements Thread.UncaughtExceptionHandler {

		@Override
		public void uncaughtException(Thread thread, Throwable e) {
			displayExceptionAndExit(e, null);
		}
	}

	private static final class ExitOnClose extends WindowAdapter {

		private final EntityApplicationPanel<?> applicationPanel;

		private ExitOnClose(EntityApplicationPanel<?> applicationPanel) {
			this.applicationPanel = applicationPanel;
		}

		@Override
		public void windowClosing(WindowEvent e) {
			try {
				applicationPanel.exit();
			}
			catch (CancelException ignored) {/*ignored*/}
		}
	}

	private static final class FrameTitleConsumer implements Consumer<String> {

		private final JFrame frame;

		public FrameTitleConsumer(JFrame frame) {
			this.frame = frame;
		}

		@Override
		public void accept(String title) {
			SwingUtilities.invokeLater(new SetFrameTitle(frame, title));
		}
	}

	private static class SetFrameTitle implements Runnable {

		private final JFrame frame;
		private final String title;

		private SetFrameTitle(JFrame frame, String title) {
			this.frame = frame;
			this.title = title;
		}

		@Override
		public void run() {
			frame.setTitle(title);
		}
	}
}
