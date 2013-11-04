/*
 * Property : Confiz Solutions
 * Created by : Arslan Anwar
 * Updated by : Arslan Anwar
 */

package com.confiz.uploadqueue.db;

import java.util.ArrayList;

import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import com.antlersoft.android.dbimpl.ImplementationBase;
import com.confiz.uploadqueue.model.UQUploadingStatus;
import com.confiz.uploadqueue.model.UQRequest;
import com.confiz.uploadqueue.utils.UQDebugHelper;

public class UQDBAdapter {


	private String TAG = "UQDBAdapter";

	/** The context. */
	public Context context = null;

	/** The DB helper. */
	private UQDatabaseHelper databaseHelper = null;

	/** The db. */
	private SQLiteDatabase database = null;

	/** The single ton object. */
	private static UQDBAdapter singleTonObject = null;

	private int databaseOpenedCount = 0;


	/**
	 * This method Initializes the context of the for further use and creates a
	 * new object of UQDatabaseHelper class.
	 * 
	 * @param ctx
	 *            Context of calling Activity
	 */
	private UQDBAdapter(Context ctx) {

		if (ctx != null) {
			if (this.context == null) {
				this.context = ctx;
			}
			if (this.databaseHelper == null) {
				this.databaseHelper = new UQDatabaseHelper(this.context);
			}
		}
	}


	/**
	 * Gets the single instance of UQDBAdapter.
	 * 
	 * @param context
	 *            the context
	 * @return single instance of UQDBAdapter
	 */
	public static UQDBAdapter getInstance(Context context) {

		if (UQDBAdapter.singleTonObject == null) {
			UQDBAdapter.singleTonObject = new UQDBAdapter(context);
		}
		return UQDBAdapter.singleTonObject;
	}


	/**
	 * This method opens a writableDatabase
	 * 
	 * @return <b>UQDBAdapter</b> object
	 * @throws SQLException
	 */

	private boolean openToWrite() {

		boolean flag = false;
		try {
			if (database == null || database.isOpen() == false) {
				database = databaseHelper.getWritableDatabase();
				flag = true;
			} else if (database != null && database.isOpen()) {
				flag = true;
			}
		} catch (Exception exception) {
			UQDebugHelper.printException(TAG, exception);
			flag = false;
		}
		databaseOpenedCount++;
		return flag;

	}


	private boolean openToRead() {

		boolean flag = false;
		try {
			if (database == null || database.isOpen() == false) {
				database = databaseHelper.getReadableDatabase();
				flag = true;
			} else if (database != null && database.isOpen()) {
				flag = true;
			}
		} catch (Exception exception) {
			UQDebugHelper.printException(TAG, exception);
			flag = false;
		}
		databaseOpenedCount++;
		return flag;

	}


	/**
	 * Closes the database
	 */
	private void close() {

		try {
			if (databaseOpenedCount > 0) {
				--databaseOpenedCount;
			}
			// AppDebuger.printData( "Close" , "Now count is = " + openedCount )
			// ;
			if (databaseOpenedCount <= 0 && database != null && database.isOpen()) {
				database.close();
			}
		} catch (Exception exception) {
			UQDebugHelper.printException(TAG, exception);
		}
	}


	/**
	 * Deletes the data from all Database tables.
	 */
	public void truncateTables() {

		truncateTable(UQRequest.GEN_TABLE_NAME);
	}


	/**
	 * Deletes the data from a specific table.
	 */
	public void truncateTable(String tableName) {

		executeWriteQuery("DELETE from " + tableName);
	}


	public static void destroy() {

		singleTonObject = null;
	}


	public synchronized boolean executeWriteQuery(String sqlCommand) {

		boolean flag = false;
		try {
			if (openToWrite()) {
				if (database.isDbLockedByCurrentThread() == false) {
					if (sqlCommand != null && sqlCommand.length() > 0) {
						database.execSQL(sqlCommand);
						flag = true;
					}
				}
			}
		} catch (Exception exception) {
			UQDebugHelper.printException(TAG, exception);
			flag = false;
		} finally {
			close();
		}
		return flag;
	}


	public Cursor executeReadQuery(String sqlQuery) {

		Cursor cursor = null;
		try {
			if (openToRead()) {
				if (sqlQuery != null && sqlQuery.length() > 0) {
					cursor = database.rawQuery(sqlQuery, null);
				}
			}
		} catch (Exception exception) {
			UQDebugHelper.printException(TAG, exception);
			cursor = null;
		}
		return cursor;
	}


	public void endReadOperation() {

		close();
	}


	public boolean insertRecord(ImplementationBase request) {

		boolean flag = false;
		try {
			if (openToWrite()) {
				long id = database.insert(request.Gen_tableName(), null, request.Gen_getValues());
				if (id != -1) {
					flag = true;
				}
			}
		} catch (Exception exception) {
			UQDebugHelper.printException(TAG, exception);
			flag = false;
		} finally {
			close();
		}
		return flag;
	}


	public ArrayList<? extends ImplementationBase> getUploadRequests(String className, String query) {

		ArrayList<ImplementationBase> requestedList = null;
		Cursor cursor = executeReadQuery(query);
		int[] columnIndex = null;
		try {
			if (cursor != null && cursor.moveToFirst()) {
				requestedList = new ArrayList<ImplementationBase>();
				do {
					Object tempObject = Class.forName(className).newInstance();
					if (tempObject instanceof ImplementationBase) {
						ImplementationBase convertedObject = (ImplementationBase) tempObject;
						if (columnIndex == null) {
							columnIndex = convertedObject.Gen_columnIndices(cursor);
						}
						convertedObject.Gen_populate(cursor, columnIndex);
						requestedList.add(convertedObject);
					}
				} while (cursor.moveToNext());
			}
		} catch (Exception exception) {
			UQDebugHelper.printAndTrackException(exception);
		} finally {
			if (cursor != null && !cursor.isClosed()) {
				cursor.close();
				cursor = null;
			}
			close();
		}
		return requestedList;
	}


	public boolean deleteItem(String query) {

		boolean flag = false;
		flag = executeWriteQuery(query);
		return flag;
	}


	@SuppressWarnings("unchecked")
	public ArrayList<UQRequest> getUploadRequests(String userId) {

		String query = "SELECT * FROM " + UQRequest.GEN_TABLE_NAME + " WHERE " + UQRequest.GEN_FIELD_USERID + " = '" + userId + "' ORDER BY position ASC";
		return (ArrayList<UQRequest>) getUploadRequests(UQRequest.class.getName(), query);
	}


	public boolean updateUploadStatus(String userId) {
		String query = "UPDATE " + UQRequest.GEN_TABLE_NAME + " set " + UQRequest.GEN_FIELD_STATUS + " =" + UQUploadingStatus.WAITING
		        .ordinal() + " WHERE (" + UQRequest.GEN_FIELD_STATUS + " =" + UQUploadingStatus.DOWNLOADING.ordinal() + " OR " + UQRequest.GEN_FIELD_STATUS + " =" + UQUploadingStatus.DOWNLOAD_REQUEST
		        .ordinal() + ") AND " + UQRequest.GEN_FIELD_USERID + " ='" + userId + "'";
//		String query = "UPDATE " + UQRequest.GEN_TABLE_NAME + " set " + UQRequest.GEN_FIELD_STATUS + " =" + UQUploadingStatus.WAITING
//		        .ordinal() + " WHERE " + UQRequest.GEN_FIELD_USERID + " ='" + userId + "'";
		boolean flag = executeWriteQuery(query);
		return flag;
	}


	public boolean deleteDQRequest(UQRequest dRequest, String userId) {

		boolean flag = false;
		String query = "DELETE FROM  " + UQRequest.GEN_TABLE_NAME + " WHERE " + UQRequest.GEN_FIELD_KEY + " = '" + dRequest
		        .getKey() + "' AND " + UQRequest.GEN_FIELD_USERID + " = '" + userId + "'";
		flag = executeWriteQuery(query);
		return flag;
	}


	public boolean deleteUploadQueue(String userId) {

		boolean flag = false;
		String query = "DELETE FROM " + UQRequest.GEN_TABLE_NAME + " WHERE " + UQRequest.GEN_FIELD_USERID + "  = '" + userId + "'";
		flag = executeWriteQuery(query);

		return flag;
	}


	public UQUploadingStatus getUploadStatus(String key, String userId) {

		UQUploadingStatus status = UQUploadingStatus.WAITING;
		String query = "SELECT * FROM " + UQRequest.GEN_TABLE_NAME + " WHERE " + UQRequest.GEN_FIELD_KEY + " = '" + key + "' AND " + UQRequest.GEN_FIELD_USERID + "  = '" + userId + "'";
		Cursor cursor = executeReadQuery(query);
		try {
			if (cursor != null && cursor.moveToFirst()) {
				status = UQUploadingStatus.get(cursor.getInt(2));
			}
		} catch (Exception exception) {
			UQDebugHelper.printAndTrackException(exception);
		} finally {
			if (cursor != null && !cursor.isClosed()) {

				cursor.close();
				cursor = null;
			}
			close();
		}
		return status;
	}


	public boolean isInUploadQueue(String key, String userId) {

		boolean flag = false;
		Cursor cursor = null;
		try {
			String qCount = "Select * From " + UQRequest.GEN_TABLE_NAME + " WHERE " + UQRequest.GEN_FIELD_KEY + " = '" + key + "'";
			cursor = executeReadQuery(qCount.toString());
			if (cursor != null && cursor.getCount() > 0 && cursor.moveToFirst()) {
				flag = true;
			}
		} catch (Exception exception) {
			UQDebugHelper.printAndTrackException(exception);
		} finally {
			if (cursor != null && !cursor.isClosed()) {

				cursor.close();
				cursor = null;
			}
			close();
		}
		return flag;
	}


	public boolean updateUploadPositions(ArrayList<UQRequest> list, String userId) {

		boolean flag = false;
		try {
			if (list != null) {
				UQRequest data = null;
				for (int newPos = 0; newPos < list.size(); newPos++) {
					data = list.get(newPos);
					String uQuery = "UPDATE " + UQRequest.GEN_TABLE_NAME + " set " + UQRequest.GEN_FIELD_POSITION + "=" + newPos + " WHERE " + UQRequest.GEN_FIELD_KEY + "  ='" + data
					        .getKey() + "' AND " + UQRequest.GEN_FIELD_USERID + "  = '" + userId + "'";
					flag = executeWriteQuery(uQuery);
					if (flag == false) {
						break;
					}
				}
			}
		} catch (Exception exception) {
			UQDebugHelper.printAndTrackException(exception);
		} finally {
			close();
		}
		return flag;
	}


	public boolean updateUploadStatus(UQRequest dRequest, String userId) {

		boolean flag = false;

		try {
			if (dRequest != null) {
				String uQuery = "UPDATE " + UQRequest.GEN_TABLE_NAME + " set " + UQRequest.GEN_FIELD_STATUS + " = " + dRequest
				        .getStatus().ordinal() + " WHERE " + UQRequest.GEN_FIELD_KEY + "  ='" + dRequest.getKey() + "' AND " + UQRequest.GEN_FIELD_USERID + "  = '" + userId + "'";
				flag = executeWriteQuery(uQuery);
			}
		} catch (Exception exception) {
			UQDebugHelper.printAndTrackException(exception);
		} finally {
			close();
		}
		return flag;
	}


	public boolean updateUploadedSize(UQRequest dRequest, String userId) {

		boolean flag = false;

		try {
			if (dRequest != null) {
				String uQuery = "UPDATE " + UQRequest.GEN_TABLE_NAME + " set " + UQRequest.GEN_FIELD_DOWNLOADEDSIZE + " = " + dRequest
				        .getUploadedSize() + " WHERE " + UQRequest.GEN_FIELD_KEY + "  ='" + dRequest.getKey() + "' AND " + UQRequest.GEN_FIELD_USERID + "  = '" + userId + "'";
				flag = executeWriteQuery(uQuery);
			}
		} catch (Exception exception) {
			UQDebugHelper.printAndTrackException(exception);
		} finally {
			close();
		}
		return flag;
	}


	public boolean updateUploadTotalSize(UQRequest dRequest, String userId) {

		boolean flag = false;

		try {
			if (dRequest != null) {
				String uQuery = "UPDATE " + UQRequest.GEN_TABLE_NAME + " set " + UQRequest.GEN_FIELD_TOTALSIZE + " = " + dRequest
				        .getTotalSize() + " WHERE " + UQRequest.GEN_FIELD_KEY + "  ='" + dRequest.getKey() + "' AND " + UQRequest.GEN_FIELD_USERID + "  = '" + userId + "'";
				flag = executeWriteQuery(uQuery);
			}
		} catch (Exception exception) {
			UQDebugHelper.printAndTrackException(exception);
		} finally {
			close();
		}
		return flag;
	}


	public boolean updateDQRequestData(UQRequest dRequest, String userId) {

		boolean flag = false;

		try {
			if (dRequest != null) {
				String uQuery = "UPDATE " + UQRequest.GEN_TABLE_NAME + " set " + UQRequest.GEN_FIELD_TIMEESTIMATIONS + " = " + dRequest
				        .getTotalSize() + ", " + UQRequest.GEN_FIELD_DOWNLOADEDSIZE + " = " + dRequest.getUploadedSize() + ", " + UQRequest.GEN_FIELD_STATUS + " = " + dRequest
				        .getStatus().ordinal() + " WHERE " + UQRequest.GEN_FIELD_KEY + "  ='" + dRequest.getKey() + "' AND " + UQRequest.GEN_FIELD_USERID + "  = '" + userId + "'";
				flag = executeWriteQuery(uQuery);
			}
		} catch (Exception exception) {
			UQDebugHelper.printAndTrackException(exception);
		} finally {
			close();
		}
		return flag;
	}


	public boolean updateErrorDiscription(UQRequest dRequest, String userId) {

		boolean flag = false;

		try {
			if (dRequest != null) {
				String uQuery = "UPDATE " + UQRequest.GEN_TABLE_NAME + " set " + UQRequest.GEN_FIELD_ERRORDISCRIPTION + " = '" + dRequest
				        .getErrorDiscription() + "' WHERE key ='" + dRequest.getKey() + "' AND " + UQRequest.GEN_FIELD_USERID + "  = '" + userId + "'";
				flag = executeWriteQuery(uQuery);
			}
		} catch (Exception exception) {
			UQDebugHelper.printAndTrackException(exception);
		} finally {
			close();
		}
		return flag;
	}


	public boolean isItemAvilableForUpload(String userId) {

		boolean flag = false;
		Cursor cursor = null;
		try {
			String uQuery = "SELECT count(*) FROM " + UQRequest.GEN_TABLE_NAME + " WHERE " + UQRequest.GEN_FIELD_STATUS + " !=" + UQUploadingStatus.PAUSED
			        .ordinal() + " AND " + UQRequest.GEN_FIELD_USERID + "  = '" + userId + "'";
			cursor = executeReadQuery(uQuery);

			if (cursor != null && cursor.moveToFirst() && cursor.getCount() > 0) {
				flag = true;
			}
		} catch (Exception exception) {
			UQDebugHelper.printAndTrackException(exception);
		} finally {
			if (cursor != null && !cursor.isClosed()) {

				cursor.close();
				cursor = null;
			}
			close();
		}
		return flag;
	}

}