package de.tr7zw.singlefilezcollector;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import net.sf.sevenzipjbinding.ArchiveFormat;
import net.sf.sevenzipjbinding.IInArchive;
import net.sf.sevenzipjbinding.IInStream;
import net.sf.sevenzipjbinding.IOutCreateArchive7z;
import net.sf.sevenzipjbinding.IOutCreateCallback;
import net.sf.sevenzipjbinding.IOutItem7z;
import net.sf.sevenzipjbinding.IOutUpdateArchive;
import net.sf.sevenzipjbinding.ISequentialInStream;
import net.sf.sevenzipjbinding.SevenZip;
import net.sf.sevenzipjbinding.SevenZipException;
import net.sf.sevenzipjbinding.impl.OutItemFactory;
import net.sf.sevenzipjbinding.impl.RandomAccessFileInStream;
import net.sf.sevenzipjbinding.impl.RandomAccessFileOutStream;
import net.sf.sevenzipjbinding.simple.ISimpleInArchiveItem;
import net.sf.sevenzipjbinding.util.ByteArrayStream;

public class SevenZipUtil {

	public static void archiveFile(File path, String archivePath, File archive) throws IOException {
		boolean success = false;
		RandomAccessFile inRaf = null;
		RandomAccessFile outRaf = null;
		IInArchive inArchive;
		IOutUpdateArchive<IOutItem7z> outArchive = null;
		List<Closeable> closeables = new ArrayList<Closeable>();
		try {
			// Open input file
			inRaf = new RandomAccessFile(archive, "r");
			closeables.add(inRaf);
			IInStream inStream = new RandomAccessFileInStream(inRaf);

			// Open in-archive
			inArchive = SevenZip.openInArchive(ArchiveFormat.SEVEN_ZIP, inStream);
			closeables.add(inArchive);
			
			outRaf = new RandomAccessFile(archive, "rw");
			closeables.add(outRaf);

			
			itemToAdd = inArchive.getNumberOfItems();
			itemToAddPath = archivePath;
			itemToAddContent = Files.readAllBytes(path.toPath());
			
			// Open out-archive object
			outArchive = inArchive.getConnectedOutArchive7z();

			// Modify archive
			outArchive.updateItems(new RandomAccessFileOutStream(outRaf), inArchive.getNumberOfItems() + 1,
					new MyCreateCallback());

			success = true;
		} finally {
			for (int i = closeables.size() - 1; i >= 0; i--) {
				try {
					closeables.get(i).close();
				} catch (Throwable e) {
					System.err.println("Error closing resource: " + e);
					success = false;
				}
			}
		}
		if (success) {
			System.out.println("Update successful");
		}
	}

	static int itemToAdd; // New index of the item to add
	static String itemToAddPath;
	static byte[] itemToAddContent;

	static int itemToRemove; // Old index of the item to be removed

	private static final class MyCreateCallback implements IOutCreateCallback<IOutItem7z> {

		public void setOperationResult(boolean operationResultOk) throws SevenZipException {
			// Track each operation result here
		}

		public void setTotal(long total) throws SevenZipException {
			// Track operation progress here
		}

		public void setCompleted(long complete) throws SevenZipException {
			// Track operation progress here
		}

		public IOutItem7z getItemInformation(int index, OutItemFactory<IOutItem7z> outItemFactory)
				throws SevenZipException {
			if (index == itemToAdd) {
				// Adding new item
				IOutItem7z outItem = outItemFactory.createOutItem();
				outItem.setPropertyPath(itemToAddPath);
				outItem.setDataSize((long) itemToAddContent.length);

				return outItem;
			}

			return outItemFactory.createOutItem(index);
		}

		public ISequentialInStream getStream(int i) throws SevenZipException {
			if (i != itemToAdd) {
				return null;
			}
			return new ByteArrayStream(itemToAddContent, false);
		}
	}
	

	public static void createArchive(File path) {
		RandomAccessFile raf = null;
		IOutCreateArchive7z outArchive = null;
		try {
			raf = new RandomAccessFile(path, "rw");

			outArchive = SevenZip.openOutArchive7z();
			outArchive.setLevel(5);
			outArchive.createArchive(new RandomAccessFileOutStream(raf), 0, new IOutCreateCallback<IOutItem7z>() {

				@Override
				public void setTotal(long total) throws SevenZipException {
				}

				@Override
				public void setCompleted(long complete) throws SevenZipException {
				}

				@Override
				public void setOperationResult(boolean operationResultOk) throws SevenZipException {
				}

				@Override
				public IOutItem7z getItemInformation(int index, OutItemFactory<IOutItem7z> outItemFactory)
						throws SevenZipException {
					return null;
				}

				@Override
				public ISequentialInStream getStream(int index) throws SevenZipException {
					return null;
				}
			});

			System.out.println("Compression operation succeeded");
		} catch (SevenZipException e) {
			System.err.println("7-Zip-JBinding-Error:");
			// Extended stack trace prints more information
			e.printStackTraceExtended();
		} catch (Exception e) {
			System.err.println("Error occurs: " + e);
		} finally {
			if (outArchive != null) {
				try {
					outArchive.close();
				} catch (IOException e) {
					System.err.println("Error closing archive: " + e);
				}
			}
			if (raf != null) {
				try {
					raf.close();
				} catch (IOException e) {
					System.err.println("Error closing file: " + e);
				}
			}
		}
	}
	
	public static byte[] getFileFromArchive(File file, String path) {
		IInArchive archive = null;
		RandomAccessFile randomAccessFile = null;
		try {
			randomAccessFile = new RandomAccessFile(file, "r");

			archive = SevenZip.openInArchive(null, // null - autodetect
					new RandomAccessFileInStream(randomAccessFile));

			for (ISimpleInArchiveItem item : archive.getSimpleInterface().getArchiveItems()) {
				if (path.equals(item.getPath())) {
					ByteArrayStream stream = new ByteArrayStream(Integer.MAX_VALUE);
					archive.extractSlow(item.getItemIndex(), stream);
					return stream.getBytes();
				}
			}
		} catch (Exception ex) {

		} finally {
			try {
				if (archive != null)
					archive.close();
				if (randomAccessFile != null)
					randomAccessFile.close();
			} catch (Exception ex) {
			}
		}
		return null;
	}
	
}
