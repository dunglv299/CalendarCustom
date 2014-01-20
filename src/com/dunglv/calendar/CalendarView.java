package com.dunglv.calendar;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

import android.app.Activity;
import android.content.ContentValues;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.CalendarContract;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

public class CalendarView extends Activity {

	public GregorianCalendar month, itemmonth;// calendar instances.

	public CalendarAdapter adapter;// adapter instance
	public Handler handler;// for grabbing some event values for showing the dot
							// marker.
	public ArrayList<String> items; // container to store calendar items which
									// needs showing the event marker
	ArrayList<String> event;
	LinearLayout rLayout;
	ArrayList<String> date;
	ArrayList<String> desc;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.calendar);
		Locale.setDefault(Locale.US);

		rLayout = (LinearLayout) findViewById(R.id.text);
		month = (GregorianCalendar) GregorianCalendar.getInstance();
		itemmonth = (GregorianCalendar) month.clone();

		items = new ArrayList<String>();

		adapter = new CalendarAdapter(this, month);

		GridView gridview = (GridView) findViewById(R.id.gridview);
		gridview.setAdapter(adapter);

		handler = new Handler();
		handler.post(calendarUpdater);

		TextView title = (TextView) findViewById(R.id.title);
		title.setText(android.text.format.DateFormat.format("MMMM yyyy", month));

		RelativeLayout previous = (RelativeLayout) findViewById(R.id.previous);

		previous.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				setPreviousMonth();
				refreshCalendar();
			}
		});

		RelativeLayout next = (RelativeLayout) findViewById(R.id.next);
		next.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				setNextMonth();
				refreshCalendar();

			}
		});

		gridview.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View v,
					int position, long id) {
				// removing the previous view if added
				if (((LinearLayout) rLayout).getChildCount() > 0) {
					((LinearLayout) rLayout).removeAllViews();
				}
				desc = new ArrayList<String>();
				date = new ArrayList<String>();
				((CalendarAdapter) parent.getAdapter()).setSelected(v);
				String selectedGridDate = CalendarAdapter.dayString
						.get(position);
				String[] separatedTime = selectedGridDate.split("-");
				String gridvalueString = separatedTime[2].replaceFirst("^0*",
						"");// taking last part of date. ie; 2 from 2012-12-02.
				int gridvalue = Integer.parseInt(gridvalueString);
				// navigate to next or previous month on clicking offdays.
				if ((gridvalue > 10) && (position < 8)) {
					setPreviousMonth();
					refreshCalendar();
				} else if ((gridvalue < 7) && (position > 28)) {
					setNextMonth();
					refreshCalendar();
				}
				((CalendarAdapter) parent.getAdapter()).setSelected(v);

				for (int i = 0; i < Utility.startDates.size(); i++) {
					if (Utility.startDates.get(i).equals(selectedGridDate)) {
						desc.add(Utility.nameOfEvent.get(i));
					}
				}

				if (desc.size() > 0) {
					for (int i = 0; i < desc.size(); i++) {
						TextView rowTextView = new TextView(CalendarView.this);

						// set some properties of rowTextView or something
						rowTextView.setText("Event:" + desc.get(i));
						rowTextView.setTextColor(Color.BLACK);

						// add the textview to the linearlayout
						rLayout.addView(rowTextView);

					}

				}

				desc = null;

			}

		});
	}

	protected void setNextMonth() {
		if (month.get(GregorianCalendar.MONTH) == month
				.getActualMaximum(GregorianCalendar.MONTH)) {
			month.set((month.get(GregorianCalendar.YEAR) + 1),
					month.getActualMinimum(GregorianCalendar.MONTH), 1);
		} else {
			month.set(GregorianCalendar.MONTH,
					month.get(GregorianCalendar.MONTH) + 1);
		}

	}

	protected void setPreviousMonth() {
		if (month.get(GregorianCalendar.MONTH) == month
				.getActualMinimum(GregorianCalendar.MONTH)) {
			month.set((month.get(GregorianCalendar.YEAR) - 1),
					month.getActualMaximum(GregorianCalendar.MONTH), 1);
		} else {
			month.set(GregorianCalendar.MONTH,
					month.get(GregorianCalendar.MONTH) - 1);
		}

	}

	protected void showToast(String string) {
		Toast.makeText(this, string, Toast.LENGTH_SHORT).show();

	}

	public void refreshCalendar() {
		TextView title = (TextView) findViewById(R.id.title);

		adapter.refreshDays();
		adapter.notifyDataSetChanged();
		handler.post(calendarUpdater); // generate some calendar items

		title.setText(android.text.format.DateFormat.format("MMMM yyyy", month));
	}

	public Runnable calendarUpdater = new Runnable() {

		@Override
		public void run() {
			items.clear();

			// Print dates of the current week
			DateFormat df = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
			String itemvalue;
			event = Utility.readCalendarEvent(CalendarView.this);
			Log.d("=====Event====", event.toString());
			Log.d("=====Date ARRAY====", Utility.startDates.toString());

			for (int i = 0; i < Utility.startDates.size(); i++) {
				itemvalue = df.format(itemmonth.getTime());
				itemmonth.add(GregorianCalendar.DATE, 1);
				items.add(Utility.startDates.get(i).toString());
			}
			adapter.setItems(items);
			adapter.notifyDataSetChanged();
		}
	};
	private Uri eventsUri;
	Cursor cursor;
	int calendarId;

	public void testAddEvent(View v) {

		if (android.os.Build.VERSION.SDK_INT <= 7) {
			eventsUri = Uri.parse("content://calendar/events");
			cursor = this.getContentResolver().query(
					Uri.parse("content://calendar/calendars"),
					new String[] { "_id", "displayName" }, null, null, null);

		}

		else if (android.os.Build.VERSION.SDK_INT <= 14) {
			eventsUri = Uri.parse("content://com.android.calendar/events");
			cursor = this.getContentResolver().query(
					Uri.parse("content://com.android.calendar/calendars"),
					new String[] { "_id", "displayName" }, null, null, null);

		}

		else {
			eventsUri = Uri.parse("content://com.android.calendar/events");
			cursor = this.getContentResolver().query(
					Uri.parse("content://com.android.calendar/calendars"),
					new String[] { "_id", "calendar_displayName" }, null, null,
					null);

		}

		if (cursor.moveToFirst()) {
			do {
				int calId = cursor.getInt(0);
				String calName = cursor.getString(1);
				if (calName.contains("@gmail.com")) {
					calendarId = calId;
					break;
				}
				// do what ever you want here
			} while (cursor.moveToNext());
		}
		long startCalTime;
		long endCalTime;
		Date eventDate = null;
		TimeZone timeZone = TimeZone.getDefault();
		Calendar cal = Calendar.getInstance();

		try {
			eventDate = new SimpleDateFormat("MM/dd/yyyy").parse("01/19/2014");
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		cal.setTime(eventDate);

		cal.set(Calendar.HOUR_OF_DAY, 13);
		cal.set(Calendar.MINUTE, 13);
		startCalTime = cal.getTimeInMillis();

		cal.set(Calendar.HOUR_OF_DAY, 14);
		cal.set(Calendar.MINUTE, 14);
		endCalTime = cal.getTimeInMillis();

		ContentValues event = new ContentValues();

		event.put(CalendarContract.Events.CALENDAR_ID, calendarId);
		event.put(CalendarContract.Events.TITLE, "new event");
		event.put(CalendarContract.Events.DESCRIPTION, "Adding Event");
		event.put(CalendarContract.Events.EVENT_LOCATION, "dunglv.com");
		event.put(CalendarContract.Events.DTSTART, startCalTime);
		event.put(CalendarContract.Events.DTEND, endCalTime);
		event.put(CalendarContract.Events.STATUS, 1);
		event.put(CalendarContract.Events.HAS_ALARM, 1);
		event.put(CalendarContract.Events.EVENT_TIMEZONE, timeZone.getID());
		// To Insert
		this.getContentResolver().insert(eventsUri, event);

	}
}
