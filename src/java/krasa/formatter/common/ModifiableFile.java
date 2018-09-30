package krasa.formatter.common;

import java.io.File;

import krasa.formatter.exception.FileDoesNotExistsException;
import krasa.formatter.utils.FileUtils;

/**
 * @author Vojtech Krasa
 */
public class ModifiableFile extends File {

	public ModifiableFile(String pathToConfigFileJava) {
		super(createFile(pathToConfigFileJava));
	}

	private static String createFile(String pathToConfigFileJava) {
		if (FileUtils.isHttpPath(pathToConfigFileJava)) {
			return FileUtils.loadRemoteConfig(pathToConfigFileJava);
		} else {
			return pathToConfigFileJava;
		}
	}

	public boolean wasChanged(Monitor lastState) {
		checkIfExists();
		return this.lastModified() > lastState.getLastStateTime();
	}

	public void checkIfExists() throws FileDoesNotExistsException {
		if (!this.exists()) {
			throw new FileDoesNotExistsException(this);
		}
	}

	public Monitor getModifiedMonitor() {
		return new Monitor(this);
	}

	/**
	 * @author Vojtech Krasa
	 */
	public static class Monitor {
		private long lastStateTime;

		public Monitor(File file) {
			lastStateTime = file.lastModified();
		}

		public long getLastStateTime() {
			return lastStateTime;
		}

		public boolean wasModified(File l) {
			return l.lastModified() > lastStateTime;
		}
	}
}
