package oakbot.util;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import oakbot.util.Downloader.Progress;

/**
 * @author Michael Angstadt
 */
public class DownloaderTest {
	@Rule
	public TemporaryFolder temp = new TemporaryFolder();

	@Test
	public void no_progress() throws Exception {
		Path saveTo = temp.getRoot().toPath().resolve("file.txt");
		byte content[] = "content".getBytes();

		Downloader downloader = new Downloader("http://www.example.com/file.txt", saveTo) {
			@Override
			HttpURLConnection getConnection(String url) throws IOException {
				assertEquals("http://www.example.com/file.txt", url);
				return mockConnection(content);
			}
		};

		downloader.start();

		assertArrayEquals(content, Files.readAllBytes(saveTo));
	}

	@Test
	public void with_progress() throws Exception {
		Path saveTo = temp.getRoot().toPath().resolve("file.txt");
		byte content[] = new byte[30000];

		Downloader downloader = new Downloader("http://www.example.com/file.txt", saveTo) {
			@Override
			HttpURLConnection getConnection(String url) throws IOException {
				assertEquals("http://www.example.com/file.txt", url);
				return mockConnection(content);
			}
		};

		Progress progress = mock(Progress.class);
		downloader.onProgress(progress);

		downloader.start();

		verify(progress).progress(16384, 30000);
		verify(progress).progress(30000, 30000);
		verifyNoMoreInteractions(progress);

		assertArrayEquals(content, Files.readAllBytes(saveTo));
	}

	private static HttpURLConnection mockConnection(byte data[]) throws IOException {
		HttpURLConnection connection = mock(HttpURLConnection.class);
		when(connection.getInputStream()).thenReturn(new ByteArrayInputStream(data));
		when(connection.getContentLengthLong()).thenReturn((long) data.length);
		return connection;
	}
}
