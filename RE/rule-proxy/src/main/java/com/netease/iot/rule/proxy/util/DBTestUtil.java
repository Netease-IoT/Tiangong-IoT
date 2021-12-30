package com.netease.iot.rule.proxy.util;

import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;


public class DBTestUtil {
    private static final String DRIVER_NAME = "com.mysql.jdbc.Driver";

    private final String url;
    private final String user;
    private final String password;

    public DBTestUtil(String url, String user, String password) {
        this.url = url;
        this.user = user;
        this.password = password;
    }

    /**
     * create on not exists
     *
     * @return true if created, false else.
     */
    public boolean tryToCreateTable(String tableName,
                                    Map<String, Class> fields,
                                    Set<String> pks,
                                    boolean deleteOnExists) {
        Connection conn = null;
        try {
            conn = establishConnection();

            if (isTableExists(conn, tableName)) {
                if (deleteOnExists) {
                    dropTable(conn, tableName);
                } else {
                    return false;
                }
            }
            String createDDL = buildCreateDDL(tableName, fields, pks);
            PreparedStatement stat = conn.prepareStatement(createDDL);
            return stat.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }

        return false;
    }

    public boolean tryToDropTable(String tableName) {
        Connection conn = null;
        try {
            conn = establishConnection();

            if (isTableExists(conn, tableName)) {
                return dropTable(conn, tableName);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
        return false;
    }

    public List<List<Object>> tryToQuery(String table) {
        Connection conn = null;
        try {
            conn = establishConnection();
            String sql = String.format("select * from %s", table);
            PreparedStatement stat = conn.prepareStatement(sql);
            ResultSet rs = stat.executeQuery();
            List<List<Object>> rows = new ArrayList<>();
            while (rs.next()) {
                int columnCount = rs.getMetaData().getColumnCount();
                List<Object> row = new ArrayList<>();
                for (int i = 1; i <= columnCount; ++i) {
                    row.add(rs.getObject(i));
                }
                rows.add(row);
            }

            stat.close();
            rs.close();
            return rows;
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
        return Collections.EMPTY_LIST;
    }

    private String buildCreateDDL(String tableName, Map<String, Class> fields, Set<String> pks) {

        StringBuilder builder = new StringBuilder();
        builder.append("create table ").append(tableName).append("( ");
        String filedsStr = fields.entrySet().stream().map(field -> {
            String name = field.getKey();
            Class type = field.getValue();

            String typeName;
            if (type.equals(Integer.class)) {
                typeName = "int";
            } else if (type.equals(Long.class)) {
                typeName = "bigint";
            } else if (type.equals(String.class)) {
                typeName = "varchar(1024)";
            } else if (type.equals(Double.class) || type.equals(Float.class)) {
                typeName = "double";
            } else {
                throw new RuntimeException("Unsupported type:" + type);
            }
            return String.format("%s %s", name, typeName);
        }).collect(Collectors.joining(","));
        builder.append(filedsStr);

        if (pks.size() > 0) {
            builder.append(", primary_key (");
            String pksStr = pks.stream().map(pk -> {
                if (fields.containsKey(pk)) {
                    return pk;
                } else {
                    throw new RuntimeException("PK " + pk + " not found in fields.");
                }
            }).collect(Collectors.joining(", "));
            builder.append(pksStr);
            builder.append(")");
        }
        builder.append(" );");
        return builder.toString();
    }

    private boolean isTableExists(Connection conn, String tableName) throws SQLException {
        String sql = String.format("SELECT table_name FROM information_schema.TABLES WHERE table_name ='%s'", tableName);
        PreparedStatement stat = conn.prepareStatement(sql);
        ResultSet rs = stat.executeQuery();
        if (rs.next()) {
            rs.close();
            return true;
        }
        return false;
    }

    private boolean dropTable(Connection conn, String tableName) throws SQLException {
        String sql = String.format("drop table %s", tableName);
        PreparedStatement stat = conn.prepareStatement(sql);
        return stat.execute();
    }

    private Connection establishConnection() throws SQLException, ClassNotFoundException {
        Class.forName(DRIVER_NAME);
        if (user == null) {
            return DriverManager.getConnection(url);
        } else {
            return DriverManager.getConnection(url, user, password);
        }
    }
}
