package com.dnielfe.manager;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipOutputStream;

import com.stericson.RootTools.RootTools;

import android.app.Application;
import android.util.Log;

public class FileUtils {

	private static final int BUFFER = 2048;
	private long mDirSize = 0;

	public long getDirSize(String path) {
		get_dir_size(new File(path));

		return mDirSize;
	}

	// Inspired by org.apache.commons.io.FileUtils.isSymlink()
	private static boolean isSymlink(File file) throws IOException {
		File fileInCanonicalDir = null;
		if (file.getParent() == null) {
			fileInCanonicalDir = file;
		} else {
			File canonicalDir = file.getParentFile().getCanonicalFile();
			fileInCanonicalDir = new File(canonicalDir, file.getName());
		}
		return !fileInCanonicalDir.getCanonicalFile().equals(
				fileInCanonicalDir.getAbsoluteFile());
	}

	/*
	 * 
	 * @param path
	 */
	private void get_dir_size(File path) {
		File[] list = path.listFiles();
		int len;

		if (list != null) {
			len = list.length;

			for (int i = 0; i < len; i++) {
				try {
					if (list[i].isFile() && list[i].canRead()) {
						mDirSize += list[i].length();

					} else if (list[i].isDirectory() && list[i].canRead()
							&& !isSymlink(list[i])) {
						get_dir_size(list[i]);
					}
				} catch (IOException e) {
				}
			}
		}
	}

	/*
	 * 
	 * @param file
	 * 
	 * @param zout
	 * 
	 * @throws IOException
	 */
	private static void zip_folder(File file, ZipOutputStream zout)
			throws ZipException, IOException {
		byte[] data = new byte[BUFFER];
		int read;

		if (file.isFile()) {
			ZipEntry entry = new ZipEntry(file.getName());
			zout.putNextEntry(entry);
			BufferedInputStream instream = new BufferedInputStream(
					new FileInputStream(file));

			while ((read = instream.read(data, 0, BUFFER)) != -1)
				zout.write(data, 0, read);

			zout.closeEntry();
			instream.close();

		} else if (file.isDirectory()) {
			String[] list = file.list();
			int len = list.length;

			for (int i = 0; i < len; i++)
				zip_folder(new File(file.getPath() + "/" + list[i]), zout);
		}
	}

	/**
	 * 
	 * @param old
	 *            the file to be copied
	 * @param newDir
	 *            the directory to move the file to
	 * @return
	 */
	public static int copyToDirectory(String old, String newDir) {
		File old_file = new File(old);
		File temp_dir = new File(newDir);
		byte[] data = new byte[BUFFER];
		int read = 0;

		if (old_file.isFile() && temp_dir.isDirectory() && temp_dir.canWrite()) {
			String file_name = old
					.substring(old.lastIndexOf("/"), old.length());
			File cp_file = new File(newDir + file_name);

			try {

				BufferedOutputStream o_stream = new BufferedOutputStream(
						new FileOutputStream(cp_file));
				BufferedInputStream i_stream = new BufferedInputStream(
						new FileInputStream(old_file));

				while ((read = i_stream.read(data, 0, BUFFER)) != -1)
					o_stream.write(data, 0, read);

				o_stream.flush();
				i_stream.close();
				o_stream.close();

			} catch (FileNotFoundException e) {
				Log.e("FileNotFoundException", e.getMessage());
				return -1;

			} catch (IOException e) {
				Log.e("IOException", e.getMessage());
				return -1;

			}

		} else if (old_file.isDirectory() && temp_dir.isDirectory()
				&& temp_dir.canWrite()) {
			String files[] = old_file.list();
			String dir = newDir
					+ old.substring(old.lastIndexOf("/"), old.length());
			int len = files.length;

			if (!new File(dir).mkdir())
				return -1;

			for (int i = 0; i < len; i++)
				copyToDirectory(old + "/" + files[i], dir);

		} else if (old_file.isFile() && !temp_dir.canWrite()) {
			if (RootTools.isAccessGiven()) {
				RootTools.copyFile(old, newDir, true, true);
			}

		} else if (!temp_dir.canWrite())
			return -1;

		return 0;
	}

	/**
	 * 
	 * @param path
	 */

	public static void createZipFile(String path) {

		File dir = new File(path);

		File parent = dir.getParentFile();
		String filepath = parent.getAbsolutePath();
		String[] list = dir.list();
		String name = path.substring(path.lastIndexOf("/"), path.length());
		String _path;

		if (!dir.canRead() || !dir.canWrite())
			return;

		int len = list.length;

		if (path.charAt(path.length() - 1) != '/')
			_path = path + "/";
		else
			_path = path;

		try {
			ZipOutputStream zip_out = new ZipOutputStream(
					new BufferedOutputStream(new FileOutputStream(filepath
							+ name + ".zip"), BUFFER));

			for (int i = 0; i < len; i++)
				zip_folder(new File(_path + list[i]), zip_out);

			zip_out.close();

		} catch (FileNotFoundException e) {
			Log.e("File not found", e.getMessage());

		} catch (IOException e) {
			Log.e("IOException", e.getMessage());
		}
	}

	/**
	 * 
	 * @param filePath
	 * @param newName
	 * @return
	 */
	public static int renameTarget(String filePath, String newName) {
		File src = new File(filePath);
		File dest;

		if (newName.length() < 1)
			return -1;

		String temp = filePath.substring(0, filePath.lastIndexOf("/"));

		dest = new File(temp + "/" + newName);
		if (src.renameTo(dest))
			return 0;
		else
			return -1;
	}

	/**
	 * 
	 * @param path
	 * @param name
	 * @return
	 */
	public static int createDir(String path, String name) {
		int len = path.length();

		if (len < 1 || len < 1)
			return -1;

		if (path.charAt(len - 1) != '/')
			path += "/";

		if (new File(path + name).mkdir())
			return 0;

		return -1;
	}

	/**
	 * The full path name of the file to delete.
	 * 
	 * @param path
	 *            name
	 * @return
	 */
	public static int deleteTarget(String path) {
		File target = new File(path);

		if (target.exists() && target.isFile() && target.canWrite()) {
			target.delete();
			return 0;
		}

		else if (target.exists() && target.isDirectory() && target.canRead()) {
			String[] file_list = target.list();

			if (file_list != null && file_list.length == 0) {
				target.delete();
				return 0;

			} else if (file_list != null && file_list.length > 0) {

				for (int i = 0; i < file_list.length; i++) {
					File temp_f = new File(target.getAbsolutePath() + "/"
							+ file_list[i]);

					if (temp_f.isDirectory())
						deleteTarget(temp_f.getAbsolutePath());
					else if (temp_f.isFile())
						temp_f.delete();
				}
			}
			if (target.exists())
				if (target.delete())
					return 0;
		}

		else if (!target.delete() && target.exists()) {
			if (RootTools.isAccessGiven()) {
				RootTools.deleteFileOrDirectory(path, true);
			}
		}
		return -1;
	}

	/**
	 * 
	 * @param dir
	 * @param pathName
	 * @return
	 */
	public static ArrayList<String> searchInDirectory(String dir,
			String fileName) {
		ArrayList<String> names = new ArrayList<String>();
		search_file(dir, fileName, names);

		return names;
	}

	/*
	 * @param dir directory to search in
	 * 
	 * @param fileName filename that is being searched for
	 * 
	 * @param n ArrayList to populate results
	 */
	private static void search_file(String dir, String fileName,
			ArrayList<String> n) {
		File root_dir = new File(dir);
		String[] list = root_dir.list();

		if (list != null && root_dir.canRead()) {
			int len = list.length;

			for (int i = 0; i < len; i++) {
				File check = new File(dir + "/" + list[i]);
				String name = check.getName();

				if (check.isFile()
						&& name.toLowerCase().contains(fileName.toLowerCase())) {
					n.add(check.getPath());
				} else if (check.isDirectory()) {
					if (name.toLowerCase().contains(fileName.toLowerCase())) {
						n.add(check.getPath());

					} else if (check.canRead() && !dir.equals("/"))
						search_file(check.getAbsolutePath(), fileName, n);
				}
			}
		}
	}

	public static class ProgressbarClass extends Application {

		public static int totalMemory(File dir) {
			long longTotal = dir.getTotalSpace() / 1048576;
			int Total = (int) longTotal;
			return Total;
		}

		public static int freeMemory(File dir) {
			long longFree = (dir.getTotalSpace() - dir.getFreeSpace()) / 1048576;
			int Free = (int) longFree;
			return Free;
		}
	}
}