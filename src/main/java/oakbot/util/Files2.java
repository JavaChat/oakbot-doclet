package oakbot.util;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Contains static methods that operate on files, in the same spirit as
 * {@link Files}.
 * @author Michael Angstadt
 */
public class Files2 {
	/**
	 * Extracts all the files in a ZIP file.
	 * @param destinationDir the destination directory
	 * @param zipFile the ZIP file
	 * @throws IOException if there's a problem extracting the ZIP file
	 */
	public static void unzip(Path destinationDir, Path zipFile) throws IOException {
		try (ZipInputStream zin = new ZipInputStream(Files.newInputStream(zipFile))) {
			ZipEntry entry;
			while ((entry = zin.getNextEntry()) != null) {
				String zipPath = entry.getName();
				Path destFile = destinationDir.resolve(zipPath);

				//entry is a directory
				if (zipPath.endsWith("/")) {
					if (!Files.exists(destFile)) {
						Files.createDirectories(destFile);
					}
					continue;
				}

				//make sure the parent directory exists
				Path parent = destFile.getParent();
				if (!Files.exists(parent)) {
					Files.createDirectories(parent);
				}

				//copy the file
				Files.copy(zin, destFile, StandardCopyOption.REPLACE_EXISTING);
			}
		}
	}

	/**
	 * Recursively deletes a directory.
	 * @param directory the directory to delete
	 * @throws IOException if there's a problem deleting any of the files
	 * @see http://stackoverflow.com/a/8685959/13379
	 */
	public static void deleteDirectory(Path directory) throws IOException {
		Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				Files.delete(file);
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult visitFileFailed(Path file, IOException exception) throws IOException {
				/*
				 * Try to delete the file anyway, even if its attributes could
				 * not be read, since delete-only access is theoretically
				 * possible.
				 */
				Files.delete(file);
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult postVisitDirectory(Path dir, IOException exception) throws IOException {
				if (exception == null) {
					Files.delete(dir);
					return FileVisitResult.CONTINUE;
				} else {
					// directory iteration failed; propagate exception
					throw exception;
				}
			}
		});
	}
}
