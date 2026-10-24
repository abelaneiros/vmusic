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

import aneiros.vmusic.ui.FancyMenu;
import aneiros.vmusic.ui.FancyMenuItem;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.Collections;
import java.util.regex.Pattern;

public class HistoryAdapter
	extends SortableAdapter
	implements LibraryAdapter
	         , PlayHistory.Listener
	         , FancyMenu.Callback
{
	private static final Pattern SPACE_SPLIT = Pattern.compile("\\s+");
	private static final int SORT_RECENT = 0;
	private static final int[] SORT_RES_IDS = new int[] { R.string.history_recent };
	private static final int MENU_PLAY = 0;
	private static final int MENU_PIN = 1;
	private static final int MENU_UNPIN = 2;
	private static final int MENU_DELETE = 3;

	private final LibraryActivity mActivity;
	private final LayoutInflater mInflater;
	private ArrayList<PlayHistory.Execution> mEntries;
	private long mCurrentId = -1;
	private String[] mFilter;

	public HistoryAdapter(LibraryActivity activity)
	{
		mActivity = activity;
		mInflater = (LayoutInflater)activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		mSortEntries = SORT_RES_IDS;
		PlayHistory.get(activity).addListener(this);
	}

	public void release()
	{
		PlayHistory.get(mActivity).removeListener(this);
	}

	@Override
	public void onHistoryChanged()
	{
		mActivity.mPagerAdapter.postRequestRequery(this);
	}

	@Override
	public Object query()
	{
		ArrayList<PlayHistory.Execution> entries = PlayHistory.get(mActivity).getAll();
		String[] filter = mFilter;
		if (filter != null) {
			ArrayList<PlayHistory.Execution> filtered = new ArrayList<PlayHistory.Execution>(entries.size());
			for (PlayHistory.Execution e : entries) {
				if (matches(e, filter))
					filtered.add(e);
			}
			entries = filtered;
		}
		if (isSortDescending())
			Collections.reverse(entries);
		return entries;
	}

	private static boolean matches(PlayHistory.Execution e, String[] filter)
	{
		String text = (e.label + " " + e.currentTitle + " " + e.currentArtist).toLowerCase();
		for (String term : filter) {
			if (!text.contains(term))
				return false;
		}
		return true;
	}

	@Override
	@SuppressWarnings("unchecked")
	public void commitQuery(Object data)
	{
		mEntries = (ArrayList<PlayHistory.Execution>)data;
		mCurrentId = PlayHistory.get(mActivity).getCurrentId();
		notifyDataSetChanged();
	}

	@Override
	public void clear()
	{
		mEntries = null;
		notifyDataSetInvalidated();
	}

	@Override
	public int getCount()
	{
		return mEntries == null ? 0 : mEntries.size();
	}

	@Override
	public Object getItem(int pos)
	{
		return mEntries.get(pos);
	}

	@Override
	public long getItemId(int pos)
	{
		return mEntries.get(pos).id;
	}

	@Override
	public View getView(int pos, View convertView, ViewGroup parent)
	{
		DraggableRow row;
		ViewHolder holder;

		if (convertView == null) {
			row = (DraggableRow)mInflater.inflate(R.layout.draggable_row, parent, false);
			row.setupLayout(DraggableRow.LAYOUT_LISTVIEW);
			row.showDragger(false);

			holder = new ViewHolder();
			row.setTag(holder);
		} else {
			row = (DraggableRow)convertView;
			holder = (ViewHolder)row.getTag();
		}

		PlayHistory.Execution e = mEntries.get(pos);
		holder.id = e.id;
		holder.title = e.label;

		StringBuilder subtitle = new StringBuilder();
		if (e.currentArtist.length() != 0)
			subtitle.append(e.currentArtist).append(" – ");
		subtitle.append(e.currentTitle);
		subtitle.append(" · ").append(e.queuePos + 1).append('/').append(e.getSongCount());
		subtitle.append(" · ").append(DateUtils.getRelativeTimeSpanString(e.updatedAt, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS));

		row.setText(e.label, subtitle.toString());
		row.highlightRow(e.id == mCurrentId);
		row.showPin(e.pinned);
		row.setPinnedBackground(e.pinned);
		row.showDuration(true);
		row.setDuration(e.positionMs);
		row.getCoverView().setCover(MediaUtils.TYPE_ALBUM, e.currentAlbumId, e.label);
		return row;
	}

	@Override
	public int getMediaType()
	{
		return MediaUtils.TYPE_HISTORY;
	}

	@Override
	public void setLimiter(Limiter limiter)
	{
	}

	@Override
	public Limiter getLimiter()
	{
		return null;
	}

	@Override
	public Limiter buildLimiter(long id)
	{
		return null;
	}

	@Override
	public void setFilter(String filter)
	{
		if (filter == null)
			mFilter = null;
		else
			mFilter = SPACE_SPLIT.split(filter.toLowerCase());
	}

	@Override
	public QueryTask buildSongQuery(String[] projection)
	{
		return null;
	}

	@Override
	public Intent createData(View view)
	{
		ViewHolder holder = (ViewHolder)view.getTag();
		Intent intent = new Intent();
		intent.putExtra(LibraryAdapter.DATA_TYPE, MediaUtils.TYPE_HISTORY);
		intent.putExtra(LibraryAdapter.DATA_ID, holder.id);
		intent.putExtra(LibraryAdapter.DATA_TITLE, holder.title);
		intent.putExtra(LibraryAdapter.DATA_EXPANDABLE, false);
		return intent;
	}

	public boolean onCreateFancyMenu(Intent intent, View view, float x, float y)
	{
		long id = intent.getLongExtra(LibraryAdapter.DATA_ID, LibraryAdapter.INVALID_ID);
		boolean pinned = PlayHistory.get(mActivity).isPinned(id);

		FancyMenu fm = new FancyMenu(mActivity, this);
		fm.setHeaderTitle(intent.getStringExtra(LibraryAdapter.DATA_TITLE));
		fm.add(MENU_PLAY, 0, R.drawable.menu_play, R.string.play).setIntent(intent);
		if (pinned)
			fm.add(MENU_UNPIN, 0, R.drawable.ic_history_pin, R.string.history_unpin).setIntent(intent);
		else
			fm.add(MENU_PIN, 0, R.drawable.ic_history_pin, R.string.history_pin).setIntent(intent);
		fm.addSpacer(90);
		fm.add(MENU_DELETE, 91, R.drawable.menu_delete, R.string.delete).setIntent(intent);
		fm.show(view, x, y);
		return true;
	}

	@Override
	public boolean onFancyItemSelected(FancyMenuItem item)
	{
		Intent intent = item.getIntent();
		long id = intent.getLongExtra(LibraryAdapter.DATA_ID, LibraryAdapter.INVALID_ID);
		switch (item.getItemId()) {
		case MENU_PLAY:
			PlaybackService.get(mActivity).restoreHistoryExecution(id);
			break;
		case MENU_PIN:
			PlayHistory.get(mActivity).setPinned(id, true);
			break;
		case MENU_UNPIN:
			PlayHistory.get(mActivity).setPinned(id, false);
			break;
		case MENU_DELETE:
			confirmDelete(id, intent.getStringExtra(LibraryAdapter.DATA_TITLE));
			break;
		}
		return true;
	}

	private void confirmDelete(final long executionId, String title)
	{
		new AlertDialog.Builder(mActivity)
			.setTitle(R.string.delete)
			.setMessage(mActivity.getString(R.string.history_delete_message, title))
			.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					PlayHistory.get(mActivity).remove(executionId);
				}
			})
			.setNegativeButton(android.R.string.cancel, null)
			.show();
	}

	@Override
	public int getDefaultSortMode()
	{
		return SORT_RECENT;
	}

	@Override
	public String getSortSettingsKey()
	{
		return "sort_history";
	}
}
