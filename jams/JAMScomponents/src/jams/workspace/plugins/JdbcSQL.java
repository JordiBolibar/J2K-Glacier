/*
 * JdbcSQL.java
 * Created on 31. Januar 2008, 16:18
 *
 * This file is part of JAMS
 * Copyright (C) FSU Jena
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, see <http://www.gnu.org/licenses/>.
 *
 */
package jams.workspace.plugins;

import jams.data.Attribute;
import jams.data.DefaultDataFactory;
import jams.workspace.DataReader;
import jams.workspace.DataSet;
import jams.workspace.DataValue;
import java.sql.SQLException;
import jams.workspace.DefaultDataSet;
import jams.workspace.Workspace;
import jams.workspace.datatypes.CalendarValue;
import jams.workspace.datatypes.DoubleValue;
import jams.workspace.datatypes.LongValue;
import jams.workspace.datatypes.ObjectValue;
import jams.workspace.datatypes.StringValue;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.TimeZone;

/**
 *
 * @author Sven Kralisch
 */
public class JdbcSQL implements DataReader {

    static final int DOUBLE = 0;
    static final int LONG = 1;
    static final int STRING = 2;
    static final int TIMESTAMP = 3;
    static final int OBJECT = 4;
    
    static class QueryResult {
        ResultSet rs;
        int numberOfColumns = -1;
        int[] type;
    }
    private String user, password, host, db, query, metadataQuery, driver;
    transient private JdbcSQLConnector pgsql=null;
    transient private QueryResult metadataResult=null, dataResult=null;
    private final boolean alwaysReconnect = false;
    private DefaultDataSet[] currentData = null, currentMetadata = null;
    private boolean isClosed=true;

    private int offset = 0;

    public void setUser(String user) {
        this.user = user;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public void setDb(String db) {
        this.db = db;
    }

    public void setDriver(String driver) {
        this.driver = driver;
    }

    @Override
    public DefaultDataSet[] getData() {
        return currentData;
    }

    public ReaderType getReaderType(){
        if (this.metadataQuery!=null && this.query!=null)
            return ReaderType.ContentAndMetadataReader;
        else if (this.metadataQuery!=null)
            return ReaderType.MetadataReader;
        else if (this.query != null)
            return ReaderType.ContentReader;
        else
            return ReaderType.Empty;
    }

    @Override
    public int numberOfColumns() {
        return dataResult.numberOfColumns;
    }

    /**
     * @return the metadataQuery
     */
    public String getMetadataQuery() {
        return metadataQuery;
    }

    /**
     * @param metadataQuery the metadataQuery to set
     */
    public void setMetadataQuery(String metadataQuery) {
        this.metadataQuery = metadataQuery;
    }

    @Override
    public int fetchValues() {
        return fetchValues(Integer.MAX_VALUE);
    }

    @Override
    public int fetchValues(int count) {
        if (getReaderType() != ReaderType.ContentReader && getReaderType() != ReaderType.ContentAndMetadataReader)
            return 0;

        if (dataResult==null)
            query();
        currentData = queryResultToDataSet(dataResult, count);
        offset += currentData.length;

        return 0;
    }

    public DataSet getMetadata(int i) {
        if (getReaderType() != ReaderType.MetadataReader && getReaderType() != ReaderType.ContentAndMetadataReader)
            return null;

        if (isClosed) {
            establishConnection();
        }

        if (currentMetadata==null){
            metadataResult = executeQuery(getMetadataQuery());
            currentMetadata = queryResultToDataSet(metadataResult, 10000);
        }
        if (currentMetadata==null)
            return null;

        return currentMetadata[i];
    }

    DefaultDataSet[] queryResultToDataSet(QueryResult r, long count) {
        ArrayList<DefaultDataSet> data = new ArrayList<DefaultDataSet>();
        DefaultDataSet dataSet;
        DataValue value;

        if (r == null || r.rs == null)
            return null;

        try {
            int i = 0;
            while ((i < count) && r.rs.next()) {
                i++;
                dataSet = new DefaultDataSet(r.numberOfColumns);

                for (int j = 0; j < r.numberOfColumns; j++) {

                    switch (r.type[j]) {
                        case DOUBLE:
                            double v = r.rs.getDouble(j + 1);
                            if (!r.rs.wasNull()) {
                                value = new DoubleValue(v);
                            } else {
                                value = new StringValue("");
                            }
                            dataSet.setData(j, value);
                            break;
                        case LONG:
                            value = new LongValue(r.rs.getLong(j + 1));
                            dataSet.setData(j, value);
                            break;
                        case STRING:
                            value = new StringValue(r.rs.getString(j + 1));
                            dataSet.setData(j, value);
                            break;
                        case TIMESTAMP:
                            Attribute.Calendar cal = DefaultDataFactory.getDataFactory().createCalendar();
                            //does not work .. hours are not represented well
                            GregorianCalendar greg = new GregorianCalendar();
                            greg.setTimeZone(TimeZone.getTimeZone("GMT"));
                            cal.setTimeInMillis(r.rs.getDate(j + 1, greg).getTime());

                            String date = r.rs.getString(j + 1);
                            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z");
                            try {

                                long millis = format.parse(date + " +0000").getTime();
                                cal.setTimeInMillis(millis);
                            } catch (Exception e) {
                                throw new SQLException(e.toString());
                            }

                            value = new CalendarValue(cal);
                            dataSet.setData(j, value);
                            break;
                        default:
                            value = new ObjectValue(r.rs.getObject(j + 1));
                            dataSet.setData(j, value);
                    }
                }
                data.add(dataSet);
            }

        } catch (SQLException sqlex) {
            System.out.println("jdbcSQL: " + sqlex);
            sqlex.printStackTrace();
        }

        return data.toArray(new DefaultDataSet[data.size()]);
    }
    
    QueryResult executeQuery(String query) {
        if (isClosed || query == null)
            return null;

        QueryResult result = new QueryResult();
        try {
            ResultSet rs = null;
            if (rs != null) {
                rs.close();
            }

            rs = pgsql.execQuery(query);
            //rs.setFetchSize(0);
            ResultSetMetaData rsmd = rs.getMetaData();
            int numberOfColumns = rsmd.getColumnCount();
            int type[] = new int[numberOfColumns];
            for (int i = 0; i < numberOfColumns; i++) {
                if (rsmd.getColumnTypeName(i + 1).startsWith("int") || rsmd.getColumnTypeName(i + 1).startsWith("INT")
                        || rsmd.getColumnTypeName(i + 1).startsWith("integer") || rsmd.getColumnTypeName(i + 1).startsWith("INTEGER")) {
                    type[i] = LONG;
                } else if (rsmd.getColumnTypeName(i + 1).startsWith("float") || rsmd.getColumnTypeName(i + 1).startsWith("FLOAT")) {
                    type[i] = DOUBLE;
                } else if (rsmd.getColumnTypeName(i + 1).startsWith("double") || rsmd.getColumnTypeName(i + 1).startsWith("DOUBLE")) {
                    type[i] = DOUBLE;
                } else if (rsmd.getColumnTypeName(i + 1).startsWith("numeric") || rsmd.getColumnTypeName(i + 1).startsWith("NUMERIC")) {
                    type[i] = DOUBLE;
                } else if (rsmd.getColumnTypeName(i + 1).startsWith("varchar") || rsmd.getColumnTypeName(i + 1).startsWith("VARCHAR")) {
                    type[i] = STRING;
                } else if (rsmd.getColumnTypeName(i + 1).startsWith("datetime") || rsmd.getColumnTypeName(i + 1).startsWith("DATETIME")) {
                    type[i] = TIMESTAMP;
                } else {
                    type[i] = OBJECT;
                }
            }
            result.numberOfColumns = numberOfColumns;
            result.type = type;
            result.rs = rs;
        } catch (SQLException sqlex) {
            System.err.println("jdbcSQL: " + sqlex);
            sqlex.printStackTrace();
            return null;
        }
        return result;
    }

    private boolean skip(long count) {
        try {
            dataResult.rs.relative((int)count-1);
            dataResult.rs.next();
            /*for (int i = 0; i < count; i++) {
                dataResult.rs.next();
            }*/
            System.out.println("after skip position is: " + dataResult.rs.getString(0));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    void establishConnection() {
        try {
            if (pgsql == null) {
                pgsql = new JdbcSQLConnector(host, db, user, password, driver);
                pgsql.connect();
                isClosed = false;
            } else if (this.alwaysReconnect) {
                pgsql.close();
                pgsql = null;
                isClosed = true;
                establishConnection();
            }
        } catch (SQLException sqlex) {
            System.err.println("jdbcSQL: " + sqlex);
            sqlex.printStackTrace();
            isClosed = true;
        }
    }

    public void query() {
        establishConnection();
        this.dataResult = executeQuery(query);
        return;
    }

    @Override
    public int init() {
        offset = 0;

        if (db == null) {
            return -1;
        }

        if (user == null) {
            return -1;
        }

        if (password == null) {
            return -1;
        }

        if (host == null) {
            return -1;
        }

        if (query == null && metadataQuery == null) {
            return -1;
        }

        if (driver == null) {
            driver = "jdbc:postgresql";
        }       
        return 1;
    }

    private int closeResult(QueryResult r) {
        try {
            if (r != null && r.rs != null){
                r.rs.close();
                r.rs = null;
            }
        } catch (SQLException sqlex) {
            System.out.println("jdbcSQL: " + sqlex);
            sqlex.printStackTrace();
            return -1;
        }
        return 0;
    }

    @Override
    public int cleanup() {
        try {
            if (closeResult(metadataResult) != 0) {
                return -1;
            }
            if (closeResult(dataResult) != 0) {
                return -1;
            }

            if (pgsql != null) {
                pgsql.close();
                pgsql = null;
                isClosed = true;
            }
        } catch (SQLException sqlex) {
            System.out.println("jdbcSQL: " + sqlex);
            return -1;
        }

        return 0;
    }
    public void setWorkspace(Workspace ws){

    }
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        if (isClosed) {
            this.cleanup();
            return;
        }
        query();
        this.skip(this.offset);
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
    }
}
