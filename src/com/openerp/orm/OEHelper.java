/*
 * OpenERP, Open Source Management Solution
 * Copyright (C) 2012-today OpenERP SA (<http:www.openerp.com>)
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http:www.gnu.org/licenses/>
 * 
 */
package com.openerp.orm;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import openerp.OEVersionException;
import openerp.OpenERP;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.preference.PreferenceManager;
import android.widget.Toast;

import com.openerp.auth.OpenERPAccountManager;
import com.openerp.base.ir.Ir_model;
import com.openerp.support.JSONDataHelper;
import com.openerp.support.OEArgsHelper;
import com.openerp.support.OEUser;
import com.openerp.support.OpenERPServerConnection;
import com.openerp.support.listview.OEListViewRow;
import com.openerp.util.OEDate;

/**
 * The Class OEHelper.
 */
public class OEHelper extends OpenERP {

	/** The user context. */
	OEUser userContext = null;

	/** The m context. */
	Context mContext = null;

	HashMap<String, List<OEDataRow>> deleted_rows = new HashMap<String, List<OEDataRow>>();

	/**
	 * Instantiates a new oE helper.
	 * 
	 * @param context
	 *            the context
	 * @param base_url
	 *            the base_url
	 * @throws ClientProtocolException
	 *             the client protocol exception
	 * @throws JSONException
	 *             the jSON exception
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 * @throws OEVersionException
	 */
	public OEHelper(Context context, String base_url)
			throws ClientProtocolException, JSONException, IOException,
			OEVersionException {
		super(base_url);
		this.mContext = context;

	}

	/**
	 * Instantiates a new oE helper.
	 * 
	 * @param context
	 *            the context
	 * @param data
	 *            the data
	 * @throws ClientProtocolException
	 *             the client protocol exception
	 * @throws JSONException
	 *             the jSON exception
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 * @throws OEVersionException
	 */
	public OEHelper(Context context, OEUser data)
			throws ClientProtocolException, JSONException, IOException,
			OEVersionException {
		super(data.getHost(), OpenERPServerConnection
				.isNetworkAvailable(context));
		this.mContext = context;
		this.userContext = this.login(data.getUsername(), data.getPassword(),
				data.getDatabase(), data.getHost());

	}

	/**
	 * Gets the user context.
	 * 
	 * @return the user context
	 */
	public OEUser getUserContext() {
		return this.userContext;
	}

	/**
	 * Login.
	 * 
	 * @param username
	 *            the username
	 * @param password
	 *            the password
	 * @param database
	 *            the database
	 * @param serverURL
	 *            the server url
	 * @return the user object
	 */
	public OEUser login(String username, String password, String database,
			String serverURL) {
		OEUser userObj = null;
		JSONObject domain = new JSONObject();
		try {
			JSONObject response = this.authenticate(username, password,
					database);
			int userId = 0;
			if (response.getString("uid") != "false") {
				userId = response.getInt("uid");

				JSONObject fields = new JSONObject();
				fields.accumulate("fields", "partner_id");
				fields.accumulate("fields", "tz");
				fields.accumulate("fields", "image");
				fields.accumulate("fields", "company_id");
				domain.accumulate("domain", new JSONArray("[[\"id\",\"=\","
						+ String.valueOf(userId) + "]]"));
				JSONObject res = this
						.search_read("res.users", fields, domain, 0, 0, null,
								null).getJSONArray("records").getJSONObject(0);

				userObj = new OEUser();
				userObj.setAvatar(res.getString("image"));

				userObj.setDatabase(database);
				userObj.setHost(serverURL);
				userObj.setIsactive(true);
				userObj.setAndroidName(this.generateAndroidName(username,
						database));
				userObj.setPartner_id(res.getJSONArray("partner_id").getString(
						0));
				userObj.setTimezone(res.getString("tz"));
				userObj.setUser_id(String.valueOf(userId));
				userObj.setUsername(username);
				userObj.setPassword(password);
				String company_id = new JSONArray(res.getString("company_id"))
						.getString(0);
				userObj.setCompany_id(company_id);

			}
		} catch (Exception e) {
			// e.printStackTrace();
			Toast.makeText(mContext, "Unable to reach OpenERP 7.0 Server ! ",
					Toast.LENGTH_LONG).show();
		}
		return userObj;
	}

	/**
	 * Generate android name.
	 * 
	 * @param username
	 *            the username
	 * @param database
	 *            the database
	 * @return the string
	 */
	public String generateAndroidName(String username, String database) {
		StringBuffer str = new StringBuffer();
		str.append(username);
		str.append("[");
		str.append(database);
		str.append("]");
		return str.toString();
	}

	/**
	 * Gets the all ids.
	 * 
	 * @param db
	 *            the db
	 * @return the all ids
	 */
	public int[] getAllIds(BaseDBHelper db) {
		int[] ids = db.localIds(db);
		return ids;
	}

	/**
	 * Find all name.
	 * 
	 * @param dbHelper
	 *            the db helper
	 * @return the list
	 * @throws NullPointerException
	 *             the null pointer exception
	 */
	public List<String> findAllName(BaseDBHelper dbHelper)
			throws NullPointerException {
		List<String> names = new ArrayList<String>();
		List<OEDataRow> result = dbHelper.search(dbHelper);

		if (result.size() > 0) {
			for (OEDataRow row : result) {
				names.add(row.get("name").toString());
			}
		}
		return names;
	}

	/**
	 * Call server method.
	 * 
	 * @param db
	 *            the db
	 * @param method
	 *            the method
	 * @param arguments
	 *            the arguments
	 * @param values
	 *            the values
	 * @param ids
	 *            the ids
	 * @return true, if successful
	 */
	public boolean callServerMethod(BaseDBHelper db, String method,
			JSONArray arguments, ContentValues values, int[] ids) {
		boolean flag = false;
		try {
			call_kw(db.getModelName(), method, arguments);

			for (int id : ids) {
				flag = db.write(db, values, id);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return flag;
	}

	/**
	 * Common method to Sync data with server.
	 * 
	 * It will take database helper and fetch data from server as given columns.
	 * If any column with many2many relation found it will automatic sync
	 * related table data.
	 * 
	 * @param db
	 *            the BaseDBHelper instance
	 * @return true, if successful
	 */
	public boolean syncWithServer(BaseDBHelper db) {
		deleted_rows = new HashMap<String, List<OEDataRow>>();
		boolean success = false;
		try {
			JSONObject result = getDataFromServer(db, null);
			if (result != null) {
				success = handleServerData(db, result);
			}
		} catch (Exception e) {
		}
		return success;
	}

	/**
	 * Common method to Sync data with server.
	 * 
	 * It will take database helper and fetch data from server as given columns.
	 * If any column with many2many relation found it will automatic sync
	 * related table data.
	 * 
	 * @param db
	 * @param domain
	 * @return
	 */
	public boolean syncWithServer(BaseDBHelper db, JSONObject domain) {
		deleted_rows = new HashMap<String, List<OEDataRow>>();
		boolean success = false;
		try {
			JSONObject result = getDataFromServer(db, domain);
			if (result != null) {
				success = handleServerData(db, result);
			}
		} catch (Exception e) {
		}
		return success;
	}

	public boolean syncWithServer(BaseDBHelper db, JSONObject domain,
			boolean delete, boolean syncLimitedData) {
		deleted_rows = new HashMap<String, List<OEDataRow>>();
		boolean success = false;
		try {
			JSONObject result = getDataFromServer(db, domain, syncLimitedData);
			if (result != null) {
				success = handleServerData(db, result, delete);
			}
		} catch (Exception e) {
		}
		return success;
	}

	/**
	 * sync with server
	 * 
	 * 
	 * @param db
	 * @param domain
	 * @param delete
	 * @return
	 */
	public boolean syncWithServer(BaseDBHelper db, JSONObject domain,
			boolean delete) {
		deleted_rows = new HashMap<String, List<OEDataRow>>();
		boolean success = false;
		try {
			JSONObject result = getDataFromServer(db, domain);
			if (result != null) {
				success = handleServerData(db, result, delete);
			}
		} catch (Exception e) {
		}
		return success;
	}

	/**
	 * Handle server data.
	 * 
	 * @param db
	 *            the db
	 * @param res
	 *            the res
	 * @return true, if successful
	 */
	private boolean handleServerData(BaseDBHelper db, JSONObject res) {
		return handleServerData(db, res, true);
	}

	/**
	 * Handle server data.
	 * 
	 * @param db
	 *            the db
	 * @param res
	 *            the res
	 * @return true, if successful
	 */
	private boolean handleServerData(BaseDBHelper db, JSONObject res,
			boolean delete) {
		boolean success = false;
		HashMap<String, Object> m2oCols = db.getMany2OneColumns();
		HashMap<String, Object> m2mCols = db.getMany2ManyColumns();
		HashMap<String, List<Integer>> m2oColsIds = new HashMap<String, List<Integer>>();
		HashMap<String, List<Integer>> m2mColsIds = new HashMap<String, List<Integer>>();
		List<String> serverIds = new ArrayList<String>();
		try {
			int total = res.getJSONArray("records").length();
			for (int i = 0; i < total; i++) {
				ContentValues values = new ContentValues();
				JSONObject row = res.getJSONArray("records").getJSONObject(i);
				int row_id = row.getInt("id");
				serverIds.add(String.valueOf(row_id));
				for (String key : db.columnListToStringArray(db.getColumns())) {
					if (row.has(key)) {
						// Handling many2one columns.
						if (m2oCols.containsKey(key)) {
							if (!row.getString(key).equals("false")) {
								int m2oid = Integer.parseInt(db
										.many2oneRecord(row.getJSONArray(key)));
								if (m2oColsIds.containsKey(key)) {
									m2oColsIds.get(key).add(m2oid);
									m2oColsIds.put(key, m2oColsIds.get(key));
								} else {
									List<Integer> list = new ArrayList<Integer>();
									list.add(m2oid);
									m2oColsIds.put(key, list);
								}
								// m2oIds.add(db.many2oneRecord(row
								// .getJSONArray(key)));
							}
						}
						// Handling many2manycolumns
						if (m2mCols.containsKey(key)) {
							int[] ref_ids = JSONDataHelper
									.jsonArrayTointArray(new JSONArray(row
											.getString(key)));
							m2mColsIds.put(key, intArrayToList(ref_ids));
						}

						values.put(key, row.getString(key));
					}

				}
				if (!db.hasRecord(db, row_id)) {
					@SuppressWarnings("unused")
					int newId = db.create(db, values);
				} else {
					// Updating data of row
					db.write(db, values, row_id, true);
				}

			}

			// Handling many2One reference table. :)
			for (String key : m2oCols.keySet()) {
				if (m2oColsIds.containsKey(key)) {
					Many2One many2one = (Many2One) m2oCols.get(key);
					if (many2one.isM2OObject()) {
						BaseDBHelper m2oObj = (BaseDBHelper) many2one
								.getM2OObject();
						List<Integer> m2oIds = (ArrayList<Integer>) m2oColsIds
								.get(key);
						List<Integer> idsToSync = new ArrayList<Integer>();
						for (int id : m2oIds) {
							if (!m2oObj.hasRecord(m2oObj, id)) {
								idsToSync.add(id);
							}
						}
						if (idsToSync.size() > 0) {
							syncReferenceTables(m2oObj, idsToSync);
						}

					}
				}
			}

			// Handling many2many reference table. :)
			for (String key : m2mCols.keySet()) {
				if (m2mColsIds.containsKey(key)) {
					Many2Many many2many = (Many2Many) m2mCols.get(key);
					if (many2many.isM2MObject()) {
						syncReferenceTables(
								(BaseDBHelper) many2many.getM2mObject(),
								m2mColsIds.get(key));
					}
				}
			}

			/**
			 * Comparing server Ids with local id. If local id is not present in
			 * server Ids than deleting local record.
			 */
			List<OEDataRow> del_rows = new ArrayList<OEDataRow>();
			if (delete) {
				if (total > 1) {
					for (int id : db.localIds(db)) {
						if (serverIds.size() > 0) {
							if (!serverIds.contains(String.valueOf(id))) {
								// Delete record with id.
								List<OEDataRow> del_row = db.search(db,
										new String[] { "id = ?" },
										new String[] { String.valueOf(id) });

								if (db.delete(db, id, true)) {
									del_rows.add(del_row.get(0));
								}

							}
						}
					}
				}
			}

			deleted_rows.put(db.getModelName(), del_rows);

			success = true;
		} catch (Exception e) {
			e.printStackTrace();
		}

		return success;
	}

	public HashMap<String, List<OEDataRow>> getDeletedRows() {
		return deleted_rows;
	}

	private JSONObject getDataFromServer(BaseDBHelper db, JSONObject domain,
			boolean loadLimitedData) {
		JSONObject fields = getFieldsFromCols(getSyncCols(db.getServerColumns()));
		String model = db.getModelName();
		try {
			if (loadLimitedData) {
				JSONArray domainArgs = new JSONArray();
				if (domain == null) {
					domain = new JSONObject();
				} else {
					domainArgs = domain.getJSONArray("domain");
				}
				SharedPreferences pref = PreferenceManager
						.getDefaultSharedPreferences(mContext);
				int data_limit = Integer.parseInt(pref.getString(
						"sync_data_limit", "60"));
				domainArgs.put(new JSONArray("[\"create_date\", \">=\", \""
						+ OEDate.getDateBefore(data_limit) + "\"]"));
				domain.put("domain", domainArgs);
			}
			return search_read(model, fields, domain, 0, 50, null, null);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Gets the data from server.
	 * 
	 * @param db
	 *            the db
	 * @param domain
	 *            the domain
	 * @return the data from server
	 */
	private JSONObject getDataFromServer(BaseDBHelper db, JSONObject domain) {
		return getDataFromServer(db, domain, true);
	}

	/**
	 * Gets the fields from cols.
	 * 
	 * @param cols
	 *            the cols
	 * @return the fields from cols
	 */
	private JSONObject getFieldsFromCols(String[] cols) {
		JSONObject fields = new JSONObject();
		for (String col : cols) {
			try {
				fields.accumulate("fields", col);
			} catch (Exception e) {
			}
		}
		return fields;
	}

	/**
	 * Gets the sync cols.
	 * 
	 * @param columns
	 *            the columns
	 * @return the sync cols
	 */
	private String[] getSyncCols(List<OEColumn> columns) {
		String[] fields = new String[columns.size()];
		int i = 0;
		for (OEColumn col : columns) {
			fields[i] = col.getName();
			i++;
		}
		return fields;
	}

	/**
	 * Sync reference tables.
	 * 
	 * @param db
	 *            the db
	 * @param list
	 *            the list
	 * @return true, if successful
	 */
	public boolean syncReferenceTables(BaseDBHelper db, List<Integer> list) {
		return syncReferenceTables(db, list, true);
	}

	/**
	 * Sync reference tables.
	 * 
	 * @param db
	 *            the db
	 * @param list
	 *            the list
	 * @param can_delete
	 *            can delete
	 * @return true, if successful
	 */
	public boolean syncReferenceTables(BaseDBHelper db, List<Integer> list,
			boolean can_delete) {
		boolean success = false;
		try {
			OEArgsHelper ids = new OEArgsHelper();
			for (int id : list) {
				ids.addArg(id);
			}

			OEArgsHelper args = new OEArgsHelper();
			args.addArgCondition("id", "in", ids.getArgs());

			JSONObject domain = new JSONObject();
			domain.accumulate("domain", new JSONArray().put(args.getArgs()));

			JSONObject result = getDataFromServer(db, domain, false);
			if (result != null) {
				success = handleServerData(db, result, can_delete);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return success;
	}

	/**
	 * Int array to list.
	 * 
	 * @param ids
	 *            the ids
	 * @return the list
	 */
	private List<Integer> intArrayToList(int[] ids) {
		List<Integer> list = new ArrayList<Integer>();
		for (int id : ids) {
			list.add(id);
		}
		return list;
	}

	/**
	 * Search_data from server.
	 * 
	 * @param db
	 *            the db
	 * @param domain
	 *            the domain
	 * @param offset
	 *            the offset
	 * @param limit
	 *            the limit
	 * @return the list
	 */
	public List<OEListViewRow> search_data(BaseDBHelper db, JSONObject domain,
			int offset, int limit) {
		List<OEListViewRow> record_lists = new ArrayList<OEListViewRow>();
		try {
			JSONObject fields = fieldsToOEFields(db.getServerColumns());
			JSONObject result = search_read(db.getModelName(), fields, domain,
					offset, limit, null, null);
			if (result.getJSONArray("records").length() > 0) {
				for (int i = 0; i < result.getJSONArray("records").length(); i++) {
					JSONObject row = result.getJSONArray("records")
							.getJSONObject(i);
					OEDataRow oe_datarow = jsonDataToMap(row);
					int row_id = row.getInt("id");

					OEListViewRow oe_row = new OEListViewRow(row_id,
							oe_datarow);
					record_lists.add(oe_row);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return record_lists;
	}

	/**
	 * Json data to hasmap object.
	 * 
	 * @param data
	 *            the data
	 * @return the hash map
	 */
	public OEDataRow jsonDataToMap(JSONObject data) {
		OEDataRow map_data = new OEDataRow();
		@SuppressWarnings("unchecked")
		Iterator<String> keys = data.keys();
		try {
			while (keys.hasNext()) {
				String key = keys.next();
				map_data.put(key, data.get(key));
			}
		} catch (Exception e) {
		}

		return map_data;
	}

	/**
	 * Fields to JSONObject fields Accumulates fields.
	 * 
	 * @param fields
	 *            the fields
	 * @return the jSON object
	 */
	public JSONObject fieldsToOEFields(List<OEColumn> fields) {
		JSONObject oeFields = new JSONObject();
		try {
			for (OEColumn field : fields) {
				oeFields.accumulate("fields", field.getName());
			}
		} catch (Exception e) {
		}
		return oeFields;
	}

	public boolean isInstalled(String modelname) {
		String oea_name = OpenERPAccountManager.currentUser(mContext)
				.getAndroidName();
		Ir_model modelObj = new Ir_model(mContext);
		boolean flag = false;
		try {
			JSONObject fields = new JSONObject();
			fields.accumulate("fields", "model");
			JSONObject domain = new JSONObject();
			JSONArray domainArgs = new JSONArray();
			domainArgs.put(new JSONArray("[\"model\",\"=\",\"" + modelname
					+ "\"]"));
			domain.put("domain", domainArgs);
			JSONObject res = search_read("ir.model", fields, domain, 0, 0,
					null, null);
			if (res.getInt("length") > 0) {
				flag = true;
			} else {
				flag = false;
			}
		} catch (Exception e) {
			List<OEDataRow> records = modelObj.search(modelObj,
					new String[] { "is_installed" }, new String[] {
							"model = ?", " AND ", "oea_name = ?" },
					new String[] { modelname, oea_name });
			if (records.size() > 0) {
				flag = Boolean.parseBoolean(records.get(0).get("is_installed")
						.toString());
			} else {
				flag = false;
			}
		}
		/* updating user install module info */
		int records = modelObj
				.count(modelObj, new String[] { "model = ?", " AND ",
						"oea_name = ?" }, new String[] { modelname, oea_name });
		if (records > 0) {
			// updating
			SQLiteDatabase db = modelObj.getWritableDatabase();
			db.execSQL(
					"update ir_model set is_installed = ? where model = ? and oea_name = ?",
					new String[] { String.valueOf(flag), modelname, oea_name });
			db.close();
		} else {
			ContentValues data_values = new ContentValues();
			data_values.put("id", 0);
			data_values.put("model", modelname);
			data_values.put("is_installed", String.valueOf(flag));
			modelObj.create(modelObj, data_values);
		}
		return flag;
	}
}
