package oakbot.util;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Downloads files from the Internet.
 * @author Michael Angstadt
 */
public class Downloader {
	private final String url;
	private final Path saveTo;
	private Progress progress;

	/**
	 * @param url the URL to the file
	 * @param saveTo where to save the file
	 */
	public Downloader(String url, Path saveTo) {
		this.url = url;
		this.saveTo = saveTo;
	}

	/**
	 * Monitors the progress of the download.
	 * @param progress the callback interface
	 * @return this
	 */
	public Downloader onProgress(Progress progress) {
		this.progress = progress;
		return this;
	}

	/**
	 * Downloads the file.
	 * @throws IOException if there's a problem downloading the file
	 */
	public void start() throws IOException {
		HttpURLConnection connection = getConnection(url);
		try (InputStream in = new BufferedInputStream(connection.getInputStream())) {
			if (progress == null) {
				Files.copy(in, saveTo);
				return;
			}

			long size = connection.getContentLengthLong();
			long downloaded = 0;
			byte buffer[] = new byte[1024 * 16];
			try (OutputStream out = Files.newOutputStream(saveTo)) {
				int read;
				while ((read = in.read(buffer)) != -1) {
					out.write(buffer, 0, read);
					downloaded += read;
					progress.progress(downloaded, size);
				}
			}
		}
	}

	/**
	 * Opens an HTTP connection to the file (for unit testing).
	 * @param url the URL
	 * @return the connection
	 * @throws IOException if there's a problem establishing the connection
	 */
	HttpURLConnection getConnection(String url) throws IOException {
		return (HttpURLConnection) new URL(url).openConnection();
	}

	/**
	 * @author Michael Angstadt
	 * @see Downloader#onProgress(Progress)
	 */
	public interface Progress {
		/**
		 * @param downloaded the number of bytes downloaded
		 * @param size the total size of the file or -1 if the size is not known
		 */
		void progress(long downloaded, long size);
	}
}
