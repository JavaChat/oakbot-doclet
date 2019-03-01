package oakbot.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class Files2Test {
	@Rule
	public TemporaryFolder temp = new TemporaryFolder();

	@Test
	public void unzip() throws Exception {
		Path destinationDir = temp.getRoot().toPath();
		Path zipFile = Paths.get("src", "test", "resources", "oakbot", "util", "archive.zip");

		Files2.unzip(destinationDir, zipFile);

		assertTrue(Files.exists(destinationDir.resolve("file1.txt")));
		assertTrue(Files.isDirectory(destinationDir.resolve("dir1")));
		assertTrue(Files.exists(destinationDir.resolve("dir1").resolve("file2.txt")));
	}

	@Test
	public void deleteDirectory() throws Exception {
		Path dir = temp.newFolder().toPath();
		Files.write(dir.resolve("file1"), new byte[1]);
		Files.createDirectory(dir.resolve("dir1"));
		Files.write(dir.resolve("dir1").resolve("file2"), new byte[1]);

		Files2.deleteDirectory(dir);

		assertFalse(Files.exists(dir));
	}
}
