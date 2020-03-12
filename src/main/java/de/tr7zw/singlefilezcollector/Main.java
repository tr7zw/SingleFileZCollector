package de.tr7zw.singlefilezcollector;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import com.google.gson.Gson;

import net.sf.sevenzipjbinding.SevenZip;
import net.sf.sevenzipjbinding.SevenZipNativeInitializationException;

public class Main {

	private static Gson gson = new Gson();

	public static void main(String[] args) throws InterruptedException, SevenZipNativeInitializationException {
		SevenZip.initSevenZipFromPlatformJAR();

		if(args.length != 2) {
			System.out.println("args: <watchFolder> <targetArchive>");
		}
		File inputDir = new File(args[0]);
		File archive = new File(args[1]);

		if (!archive.exists())
			SevenZipUtil.createArchive(archive);

		while (inputDir.exists()) {
			for (File f : inputDir.listFiles()) {
				if (f.getName().endsWith(".html")) {
					try {
						Index index = getIndex(f);
						if (index != null) {
							System.out.println(index);
							String targetPath = index.originalUrl;
							targetPath = targetPath.replace("http://", "");
							targetPath = targetPath.replace("https://", "");
							if (!targetPath.endsWith("/")) {
								targetPath = targetPath.substring(0, targetPath.lastIndexOf('/') + 1);
							}
							targetPath += f.getName();
							SevenZipUtil.archiveFile(f, targetPath, archive);
							f.delete();
						}
					} catch (IOException ex) {
						ex.printStackTrace();
					}
				}
			}
			Thread.sleep(5000);
		}
	}

	public static Index getIndex(File file) {
		byte[] data = SevenZipUtil.getFileFromArchive(file, "index.json");
		if (data != null) {
			String json = new String(data, StandardCharsets.UTF_8);
			return gson.fromJson(json, Index.class);
		}
		return null;
	}

	public static class Index {
		public String originalUrl;
		public String title;

		@Override
		public String toString() {
			return "Index [originalUrl=" + originalUrl + ", title=" + title + "]";
		}
	}

}
