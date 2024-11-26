import javax.swing.table.DefaultTableModel;
import java.util.Collections;

/*

Standard table model, but new rows are appended from the top
instead of from the bottom.

This is not my solution however I unfortunately lost the thread where
this was shared...

 */

public class ReversedTableModel extends DefaultTableModel
{
    public ReversedTableModel(int i, int i1) {
        this.setRowCount(i);
        this.setColumnCount(i1);
    }

    public void reverse()
    {
        Collections.reverse(getDataVector());
    }

    @Override
    public boolean isCellEditable(int row, int column){
        return false;
    }

    @Override
    public void addRow(Object[] rowData) {
        reverse();
        super.addRow(rowData);
        reverse();
    }
}
