/*
 * Copyright (C) 2026 Abel ANEIROS <abelaneiros@protonmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package aneiros.vmusic;

import android.content.Context;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

public class PlayHistory {
	public static final int MAX_ENTRIES = 10;
	private static final String HISTORY_FILE = "history";
	private static final long HISTORY_FILE_MAGIC = 0x48697374A1B2C3D4L;
	private static final int HISTORY_VERSION = 2;
	private static final int MAX_FILE_ENTRIES = 1000;

	public interface Listener {
		void onHistoryChanged();
	}

	public static class Execution {
		public long id;
		public long updatedAt;
		public long[] songIds = new long[0];
		public int[] songFlags = new int[0];
		public int queuePos;
		public int positionMs;
		public long currentSongId = -1;
		public long currentAlbumId = -1;
		public String currentTitle = "";
		public String currentArtist = "";
		public String label = "";
		public boolean pinned;

		public static Execution fromSongs(ArrayList<Song> songs, int queuePos)
		{
			int size = songs.size();
			if (size == 0)
				return null;

			Execution e = new Execution();
			e.songIds = new long[size];
			e.songFlags = new int[size];
			for (int i = 0; i != size; ++i) {
				Song song = songs.get(i);
				e.songIds[i] = song.id;
				e.songFlags[i] = song.flags;
			}
			e.queuePos = Math.max(0, Math.min(queuePos, size - 1));
			e.updatedAt = System.currentTimeMillis();

			Song current = songs.get(e.queuePos);
			e.currentSongId = current.id;
			e.currentAlbumId = current.albumId;
			e.currentTitle = nonNull(current.title);
			e.currentArtist = nonNull(current.artist);
			e.label = buildLabel(songs);
			return e;
		}

		public int getSongCount()
		{
			return songIds.length;
		}

		Execution copy()
		{
			Execution e = new Execution();
			e.id = id;
			e.label = label;
			e.pinned = pinned;
			e.copyPlaybackFrom(this);
			return e;
		}

		void copyPlaybackFrom(Execution other)
		{
			updatedAt = other.updatedAt;
			songIds = other.songIds.clone();
			songFlags = other.songFlags.clone();
			queuePos = other.queuePos;
			positionMs = other.positionMs;
			currentSongId = other.currentSongId;
			currentAlbumId = other.currentAlbumId;
			currentTitle = other.currentTitle;
			currentArtist = other.currentArtist;
		}

		void write(DataOutputStream out) throws IOException
		{
			out.writeLong(id);
			out.writeLong(updatedAt);
			out.writeInt(songIds.length);
			for (int i = 0; i != songIds.length; ++i) {
				out.writeLong(songIds[i]);
				out.writeInt(songFlags[i]);
			}
			out.writeInt(queuePos);
			out.writeInt(positionMs);
			out.writeLong(currentSongId);
			out.writeLong(currentAlbumId);
			out.writeUTF(currentTitle);
			out.writeUTF(currentArtist);
			out.writeUTF(label);
			out.writeBoolean(pinned);
		}

		static Execution read(DataInputStream in, int version) throws IOException
		{
			Execution e = new Execution();
			e.id = in.readLong();
			e.updatedAt = in.readLong();
			int size = in.readInt();
			if (size < 0)
				throw new IOException("Invalid history entry size: " + size);
			e.songIds = new long[size];
			e.songFlags = new int[size];
			for (int i = 0; i != size; ++i) {
				e.songIds[i] = in.readLong();
				e.songFlags[i] = in.readInt();
			}
			e.queuePos = in.readInt();
			e.positionMs = in.readInt();
			e.currentSongId = in.readLong();
			e.currentAlbumId = in.readLong();
			e.currentTitle = in.readUTF();
			e.currentArtist = in.readUTF();
			e.label = in.readUTF();
			if (version >= 2)
				e.pinned = in.readBoolean();
			return e;
		}

		private static String buildLabel(ArrayList<Song> songs)
		{
			Song first = songs.get(0);
			if (songs.size() == 1)
				return nonNull(first.title);

			boolean sameAlbum = true;
			boolean sameArtist = true;
			for (Song song : songs) {
				if (song.albumId != first.albumId)
					sameAlbum = false;
				if (song.artistId != first.artistId)
					sameArtist = false;
			}

			if (sameAlbum && first.album != null)
				return first.album;
			if (sameArtist && first.artist != null)
				return first.artist;
			return nonNull(first.title) + " +" + (songs.size() - 1);
		}

		private static String nonNull(String text)
		{
			return text == null ? "" : text;
		}
	}

	private static PlayHistory sInstance;

	private final File mFile;
	private final ArrayList<Execution> mEntries = new ArrayList<Execution>(MAX_ENTRIES + 1);
	private final ArrayList<Listener> mListeners = new ArrayList<Listener>(2);
	private long mCurrentId = -1;

	PlayHistory(File file)
	{
		mFile = file;
	}

	public static synchronized PlayHistory get(Context context)
	{
		if (sInstance == null) {
			PlayHistory history = new PlayHistory(new File(context.getApplicationContext().getFilesDir(), HISTORY_FILE));
			history.load();
			sInstance = history;
		}
		return sInstance;
	}

	public void addListener(Listener listener)
	{
		synchronized (mListeners) {
			if (!mListeners.contains(listener))
				mListeners.add(listener);
		}
	}

	public void removeListener(Listener listener)
	{
		synchronized (mListeners) {
			mListeners.remove(listener);
		}
	}

	public synchronized ArrayList<Execution> getAll()
	{
		ArrayList<Execution> result = new ArrayList<Execution>(mEntries.size());
		for (Execution e : mEntries)
			result.add(e.copy());
		return result;
	}

	public synchronized long getCurrentId()
	{
		return mCurrentId;
	}

	public synchronized Execution find(long id)
	{
		int index = indexOf(id);
		return index == -1 ? null : mEntries.get(index).copy();
	}

	public synchronized boolean isEmpty()
	{
		return mEntries.isEmpty();
	}

	public void startNew(Execution execution)
	{
		synchronized (this) {
			long id = System.currentTimeMillis();
			for (Execution e : mEntries) {
				if (e.id >= id)
					id = e.id + 1;
			}
			Execution entry = execution.copy();
			entry.id = id;
			mEntries.add(0, entry);
			trim();
			mCurrentId = id;
			save();
		}
		notifyListeners();
	}

	public boolean updateCurrent(Execution execution)
	{
		synchronized (this) {
			int index = indexOf(mCurrentId);
			if (index == -1)
				return false;
			mEntries.get(index).copyPlaybackFrom(execution);
			save();
		}
		notifyListeners();
		return true;
	}

	public Execution select(long id)
	{
		Execution result;
		synchronized (this) {
			int index = indexOf(id);
			if (index == -1)
				return null;
			Execution entry = mEntries.remove(index);
			mEntries.add(0, entry);
			mCurrentId = id;
			save();
			result = entry.copy();
		}
		notifyListeners();
		return result;
	}

	public synchronized boolean isPinned(long id)
	{
		int index = indexOf(id);
		return index != -1 && mEntries.get(index).pinned;
	}

	public boolean setPinned(long id, boolean pinned)
	{
		synchronized (this) {
			int index = indexOf(id);
			if (index == -1)
				return false;
			mEntries.get(index).pinned = pinned;
			save();
		}
		notifyListeners();
		return true;
	}

	public boolean finishCurrent(Execution restart)
	{
		boolean removed;
		synchronized (this) {
			int index = indexOf(mCurrentId);
			if (index == -1)
				return false;
			Execution entry = mEntries.get(index);
			removed = !entry.pinned;
			if (removed) {
				mEntries.remove(index);
				mCurrentId = -1;
			} else if (restart != null) {
				entry.copyPlaybackFrom(restart);
			}
			save();
		}
		notifyListeners();
		return removed;
	}

	public boolean remove(long id)
	{
		synchronized (this) {
			int index = indexOf(id);
			if (index == -1)
				return false;
			mEntries.remove(index);
			if (mCurrentId == id)
				mCurrentId = -1;
			save();
		}
		notifyListeners();
		return true;
	}

	private void trim()
	{
		int unpinned = 0;
		for (Execution e : mEntries) {
			if (!e.pinned)
				++unpinned;
		}
		for (int i = mEntries.size() - 1; i >= 0 && unpinned > MAX_ENTRIES; --i) {
			if (!mEntries.get(i).pinned) {
				mEntries.remove(i);
				--unpinned;
			}
		}
	}

	private int indexOf(long id)
	{
		if (id == -1)
			return -1;
		for (int i = 0; i != mEntries.size(); ++i) {
			if (mEntries.get(i).id == id)
				return i;
		}
		return -1;
	}

	private void notifyListeners()
	{
		ArrayList<Listener> listeners;
		synchronized (mListeners) {
			listeners = new ArrayList<Listener>(mListeners);
		}
		for (Listener listener : listeners)
			listener.onHistoryChanged();
	}

	synchronized void load()
	{
		mEntries.clear();
		mCurrentId = -1;
		if (!mFile.exists())
			return;

		DataInputStream in = null;
		try {
			in = new DataInputStream(new BufferedInputStream(new FileInputStream(mFile)));
			readFrom(in);
		} catch (IOException e) {
			Log.w("vMusic", "Failed to load play history", e);
			mEntries.clear();
			mCurrentId = -1;
		} finally {
			closeQuietly(in);
		}
	}

	private synchronized void save()
	{
		File tmp = new File(mFile.getPath() + ".tmp");
		DataOutputStream out = null;
		try {
			out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(tmp)));
			writeTo(out);
			out.close();
			out = null;
			if (!tmp.renameTo(mFile))
				Log.w("vMusic", "Failed to replace play history file");
		} catch (IOException e) {
			Log.w("vMusic", "Failed to save play history", e);
		} finally {
			closeQuietly(out);
		}
	}

	synchronized void writeTo(DataOutputStream out) throws IOException
	{
		out.writeLong(HISTORY_FILE_MAGIC);
		out.writeInt(HISTORY_VERSION);
		out.writeLong(mCurrentId);
		out.writeInt(mEntries.size());
		for (Execution e : mEntries)
			e.write(out);
		out.flush();
	}

	synchronized void readFrom(DataInputStream in) throws IOException
	{
		mEntries.clear();
		mCurrentId = -1;
		if (in.readLong() != HISTORY_FILE_MAGIC)
			return;
		int version = in.readInt();
		if (version < 1 || version > HISTORY_VERSION)
			return;

		long currentId = in.readLong();
		int count = in.readInt();
		if (count < 0 || count > MAX_FILE_ENTRIES)
			throw new IOException("Invalid history entry count: " + count);
		for (int i = 0; i != count; ++i)
			mEntries.add(Execution.read(in, version));
		mCurrentId = indexOf(currentId) == -1 ? -1 : currentId;
	}

	private static void closeQuietly(Closeable closeable)
	{
		if (closeable == null)
			return;
		try {
			closeable.close();
		} catch (IOException e) {
		}
	}
}
