package base.operators.operator.nio.xml;

import base.operators.tools.I18N;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Vector;
import javax.swing.table.AbstractTableModel;

public class NamespaceMapTableModel extends AbstractTableModel {
    public static final int ID_COLUMN = 0;
    public static final int NAMESPACE_COLUMN = 1;
    private static final long serialVersionUID = 1L;
    private Vector<Vector<String>> tableData;

    public NamespaceMapTableModel(Map<String, String> idNamespaceMap) {
        if (idNamespaceMap != null) {
            initializeData(idNamespaceMap);
        } else {
            this.tableData = new Vector();
        }
    }

    public void initializeData(Map<String, String> namespaceUriToIdMap) {
        this.tableData = new Vector(namespaceUriToIdMap.size());
        String[] namespaceUris = new String[0];
        namespaceUris = (String[])namespaceUriToIdMap.keySet().toArray(namespaceUris);
        Arrays.sort(namespaceUris);
        for (String namespaceUri : namespaceUris) {
            String id = (String)namespaceUriToIdMap.get(namespaceUri);
            if (id == null) {
                id = "default";
                namespaceUriToIdMap.put(namespaceUri, id);
            }
            addRow(id, namespaceUri);
        }
        fireTableDataChanged();
    }

    public Map<String, String> getIdNamespaceMap() {
        Map<String, String> idNamespaceMap = new LinkedHashMap<String, String>();
        for (Vector<String> row : this.tableData) {
            if (row.get(0) != null && !((String)row.get(0)).isEmpty()) {
                idNamespaceMap.put(row.get(0), row.get(1));
            }
        }
        return idNamespaceMap;
    }

    private void addRow(String id, String namespace) {
        Vector<String> rowVector = new Vector<String>(2);
        rowVector.add(id);
        rowVector.add(namespace);
        this.tableData.add(rowVector);
        fireTableRowsInserted(getRowCount() - 1, getRowCount() - 1);
    }

    @Override
    public int getColumnCount() { return 2; }

    @Override
    public int getRowCount() { return this.tableData.size(); }

    @Override
    public String getColumnName(int column) {
        switch (column) {
            case 0:
                return I18N.getGUILabel("importwizard.xml.namespace_mapping.namespace_table.id_column", new Object[0]);
            case 1:
                return I18N.getGUILabel("importwizard.xml.namespace_mapping.namespace_table.namespace_column", new Object[0]);
        }
        return null;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        Object data = ((Vector)this.tableData.get(rowIndex)).get(columnIndex);
        if (data == null) {
            return "default";
        }
        return data;
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        if (columnIndex == 0) {
            return true;
        }
        return false;
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        String value = (String)aValue;
        ((Vector)this.tableData.get(rowIndex)).set(columnIndex, value);
        fireTableCellUpdated(rowIndex, columnIndex);
    }
}
