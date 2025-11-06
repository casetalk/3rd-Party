// BEGIN EXTRA CODE
package mendix.actions;

import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mendix.core.Core;
import com.mendix.systemwideinterfaces.core.IContext;
import com.mendix.webui.CustomJavaAction;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Java Action to export Mendix database metadata in jcatalog format for CaseTalk.
 *
 * This action queries the Mendix database metadata and exports it as a JSON file
 * compatible with CaseTalk's jcatalog format.
 *
 * @author CaseTalk Integration
 */
public class ExportMendixMetadata extends CustomJavaAction<String>
{
    private String outputPath;
    private Boolean includeSystemTables;

    public ExportMendixMetadata(IContext context, String outputPath, Boolean includeSystemTables)
    {
        super(context);
        this.outputPath = outputPath;
        this.includeSystemTables = includeSystemTables != null ? includeSystemTables : false;
    }

    @Override
    public String executeAction() throws Exception
    {
        Connection conn = null;
        try
        {
            // Get database connection from Mendix runtime
            conn = Core.getDataSourceConnection();

            // Generate jcatalog JSON
            JSONObject jcatalog = generateJCatalog(conn);

            // Write to file
            String filePath = outputPath;
            if (filePath == null || filePath.isEmpty()) {
                filePath = "mendix_metadata.jcatalog";
            }

            try (FileWriter file = new FileWriter(filePath))
            {
                file.write(jcatalog.toString(4)); // Pretty print with 4-space indent
                file.flush();
            }

            return "Successfully exported metadata to: " + filePath;
        }
        catch (Exception e)
        {
            throw new Exception("Failed to export Mendix metadata: " + e.getMessage(), e);
        }
        finally
        {
            if (conn != null)
            {
                try
                {
                    conn.close();
                }
                catch (SQLException e)
                {
                    // Log but don't throw
                    Core.getLogger("ExportMendixMetadata").error("Failed to close connection", e);
                }
            }
        }
    }

    /**
     * Generate the jcatalog JSON structure from database metadata
     */
    private JSONObject generateJCatalog(Connection conn) throws SQLException
    {
        DatabaseMetaData metaData = conn.getMetaData();

        JSONObject root = new JSONObject();
        JSONObject connection = new JSONObject();
        JSONArray catalogs = new JSONArray();

        // Get catalog name
        String catalogName = conn.getCatalog();
        if (catalogName == null || catalogName.isEmpty()) {
            catalogName = "mendix";
        }

        JSONObject catalog = new JSONObject();
        catalog.put("name", catalogName);

        JSONArray schemas = new JSONArray();

        // Get all schemas
        ResultSet schemaRs = metaData.getSchemas();
        Map<String, List<String>> schemaTableMap = new HashMap<>();

        try
        {
            while (schemaRs.next())
            {
                String schemaName = schemaRs.getString("TABLE_SCHEM");

                // Filter out system schemas unless explicitly requested
                if (!includeSystemTables && isSystemSchema(schemaName)) {
                    continue;
                }

                schemaTableMap.put(schemaName, new ArrayList<String>());
            }
        }
        finally
        {
            schemaRs.close();
        }

        // If no schemas found, use a default schema
        if (schemaTableMap.isEmpty()) {
            schemaTableMap.put("public", new ArrayList<String>());
        }

        // Get all tables for each schema
        for (String schemaName : schemaTableMap.keySet())
        {
            JSONObject schema = new JSONObject();
            schema.put("name", schemaName);

            JSONArray tables = new JSONArray();

            // Get tables in this schema
            ResultSet tableRs = metaData.getTables(catalogName, schemaName, "%", new String[] {"TABLE"});

            try
            {
                while (tableRs.next())
                {
                    String tableName = tableRs.getString("TABLE_NAME");

                    // Filter out system tables unless explicitly requested
                    if (!includeSystemTables && isSystemTable(tableName)) {
                        continue;
                    }

                    JSONObject table = new JSONObject();
                    table.put("name", tableName);

                    // Get columns for this table
                    JSONArray columns = getTableColumns(metaData, catalogName, schemaName, tableName);
                    table.put("columns", columns);

                    // Get primary key for this table
                    JSONObject primaryKey = getTablePrimaryKey(metaData, catalogName, schemaName, tableName);
                    if (primaryKey != null && primaryKey.has("columns")) {
                        table.put("primarykey", primaryKey);
                    }

                    // Get foreign keys (optional - as annotations)
                    JSONArray foreignKeys = getTableForeignKeys(metaData, catalogName, schemaName, tableName);
                    if (foreignKeys.length() > 0) {
                        table.put("foreignkeys", foreignKeys);
                    }

                    // Get table comment/annotation if available
                    String remarks = tableRs.getString("REMARKS");
                    if (remarks != null && !remarks.isEmpty()) {
                        table.put("annotation", remarks);
                    }

                    tables.put(table);
                }
            }
            finally
            {
                tableRs.close();
            }

            // Only add schema if it has tables
            if (tables.length() > 0) {
                schema.put("tables", tables);
                schemas.put(schema);
            }
        }

        catalog.put("schemas", schemas);
        catalogs.put(catalog);

        connection.put("catalogs", catalogs);
        root.put("connection", connection);

        return root;
    }

    /**
     * Get all columns for a table
     */
    private JSONArray getTableColumns(DatabaseMetaData metaData, String catalog,
                                      String schema, String tableName) throws SQLException
    {
        JSONArray columns = new JSONArray();

        ResultSet columnRs = metaData.getColumns(catalog, schema, tableName, "%");

        try
        {
            while (columnRs.next())
            {
                JSONObject column = new JSONObject();
                String columnName = columnRs.getString("COLUMN_NAME");
                column.put("name", columnName);

                // Optional: Add data type information as annotation
                String dataType = columnRs.getString("TYPE_NAME");
                int columnSize = columnRs.getInt("COLUMN_SIZE");
                String nullable = columnRs.getString("IS_NULLABLE");
                String remarks = columnRs.getString("REMARKS");

                JSONObject metadata = new JSONObject();
                metadata.put("type", dataType);
                metadata.put("size", columnSize);
                metadata.put("nullable", "YES".equalsIgnoreCase(nullable));

                if (remarks != null && !remarks.isEmpty()) {
                    metadata.put("annotation", remarks);
                }

                // Add default value if exists
                String defaultValue = columnRs.getString("COLUMN_DEF");
                if (defaultValue != null && !defaultValue.isEmpty()) {
                    metadata.put("default", defaultValue);
                }

                column.put("metadata", metadata);

                columns.put(column);
            }
        }
        finally
        {
            columnRs.close();
        }

        return columns;
    }

    /**
     * Get primary key for a table
     */
    private JSONObject getTablePrimaryKey(DatabaseMetaData metaData, String catalog,
                                          String schema, String tableName) throws SQLException
    {
        JSONObject primaryKey = null;
        JSONArray pkColumns = new JSONArray();

        ResultSet pkRs = metaData.getPrimaryKeys(catalog, schema, tableName);

        try
        {
            while (pkRs.next())
            {
                JSONObject pkColumn = new JSONObject();
                pkColumn.put("column", pkRs.getString("COLUMN_NAME"));
                pkColumn.put("position", String.valueOf(pkRs.getInt("KEY_SEQ")));
                pkColumns.put(pkColumn);
            }
        }
        finally
        {
            pkRs.close();
        }

        if (pkColumns.length() > 0) {
            primaryKey = new JSONObject();
            primaryKey.put("columns", pkColumns);
        }

        return primaryKey;
    }

    /**
     * Get foreign keys for a table
     */
    private JSONArray getTableForeignKeys(DatabaseMetaData metaData, String catalog,
                                          String schema, String tableName) throws SQLException
    {
        JSONArray foreignKeys = new JSONArray();

        ResultSet fkRs = metaData.getImportedKeys(catalog, schema, tableName);

        try
        {
            Map<String, JSONObject> fkMap = new HashMap<>();

            while (fkRs.next())
            {
                String fkName = fkRs.getString("FK_NAME");

                if (fkName == null || fkName.isEmpty()) {
                    fkName = "FK_" + fkRs.getString("FKCOLUMN_NAME");
                }

                JSONObject fk = fkMap.get(fkName);
                if (fk == null) {
                    fk = new JSONObject();
                    fk.put("name", fkName);
                    fk.put("referencedTable", fkRs.getString("PKTABLE_NAME"));
                    fk.put("referencedSchema", fkRs.getString("PKTABLE_SCHEM"));
                    fk.put("columns", new JSONArray());
                    fkMap.put(fkName, fk);
                }

                JSONObject fkColumn = new JSONObject();
                fkColumn.put("column", fkRs.getString("FKCOLUMN_NAME"));
                fkColumn.put("referencedColumn", fkRs.getString("PKCOLUMN_NAME"));
                fkColumn.put("position", String.valueOf(fkRs.getInt("KEY_SEQ")));

                ((JSONArray) fk.get("columns")).put(fkColumn);
            }

            for (JSONObject fk : fkMap.values()) {
                foreignKeys.put(fk);
            }
        }
        finally
        {
            fkRs.close();
        }

        return foreignKeys;
    }

    /**
     * Check if schema is a system schema
     */
    private boolean isSystemSchema(String schemaName)
    {
        if (schemaName == null) return false;

        String lower = schemaName.toLowerCase();
        return lower.startsWith("information_schema") ||
               lower.startsWith("pg_") ||
               lower.startsWith("sys") ||
               lower.equals("performance_schema") ||
               lower.equals("mysql");
    }

    /**
     * Check if table is a system table
     */
    private boolean isSystemTable(String tableName)
    {
        if (tableName == null) return false;

        String lower = tableName.toLowerCase();
        return lower.startsWith("sys") ||
               lower.startsWith("msrep") ||
               lower.startsWith("dt") ||
               lower.startsWith("$");
    }

    /**
     * Returns a string representation of this action
     */
    @Override
    public String toString()
    {
        return "ExportMendixMetadata";
    }
}
// END EXTRA CODE
