package oakbot.util;

/**
 * CLASS WITH PUBLIC STATIC METHODS THAT HAVE NOTHING TO DO WITH EACH OTHER.
 * @author Michael Angstadt
 */
public final class JunkDrawer {
	/**
	 * Specifies if the local operating system is Windows.
	 */
	public static boolean WINDOWS_OS;
	static {
		String os = System.getProperty("os.name").toLowerCase();
		WINDOWS_OS = (os.indexOf("windows") == 0);
	}

	private JunkDrawer() {
		//hide
	}
}
