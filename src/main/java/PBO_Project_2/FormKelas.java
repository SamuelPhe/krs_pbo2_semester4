/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package PBO_Project_2;

import javax.swing.table.DefaultTableModel;
import javax.swing.JOptionPane;
import java.sql.ResultSet;
import javax.swing.table.TableColumn;
import java.awt.Component;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;

public class FormKelas extends javax.swing.JFrame {

    database db = new database();

    public FormKelas() {
        initComponents();
        loadData();
        setLocationRelativeTo(null);
        
    }

    public void loadData() {
        DefaultTableModel model = new DefaultTableModel();
        model.addColumn("ID Kelas");
        model.addColumn("Nama Kelas");
        tblKelas.setModel(model);

        try {
            ResultSet rs = db.getAllKelas();
            while (rs != null && rs.next()) {
                model.addRow(new Object[]{
                    rs.getInt("id_kelas"),
                    rs.getString("nama_kelas")
                });
            }
            aturLebarKolom(tblKelas);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Gagal memuat data kelas: " + e.getMessage());
        }
    }
    
    private void aturLebarKolom(javax.swing.JTable tabel) {
        TableColumnModel columnModel = tabel.getColumnModel();
        for (int col = 0; col < tabel.getColumnCount(); col++) {
            int width = 50; // Lebar minimal
            for (int row = 0; row < tabel.getRowCount(); row++) {
                TableCellRenderer renderer = tabel.getCellRenderer(row, col);
                Component comp = tabel.prepareRenderer(renderer, row, col);
                width = Math.max(comp.getPreferredSize().width + 10, width);
            }
            // Batasi maksimal agar tidak terlalu lebar
            columnModel.getColumn(col).setPreferredWidth(Math.min(width, 300));
        }
    }
    
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        tblKelas = new javax.swing.JTable();
        btnAdd = new javax.swing.JButton();
        btnEdit = new javax.swing.JButton();
        btnDelete = new javax.swing.JButton();
        btnBack = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();

        jLabel2.setFont(new java.awt.Font("Segoe UI", 1, 36)); // NOI18N
        jLabel2.setText("Tambah Jadwal Baru:");

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        getContentPane().setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jLabel3.setFont(new java.awt.Font("Segoe UI", 1, 36)); // NOI18N
        getContentPane().add(jLabel3, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 9, -1, -1));

        jLabel4.setFont(new java.awt.Font("Segoe UI", 1, 36)); // NOI18N
        getContentPane().add(jLabel4, new org.netbeans.lib.awtextra.AbsoluteConstraints(310, 268, -1, -1));

        tblKelas.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Title 1", "Title 2", "Title 3", "Title 4"
            }
        ));
        jScrollPane1.setViewportView(tblKelas);

        getContentPane().add(jScrollPane1, new org.netbeans.lib.awtextra.AbsoluteConstraints(50, 90, 610, 480));

        btnAdd.setFont(new java.awt.Font("Segoe UI", 1, 24)); // NOI18N
        btnAdd.setText("Add");
        btnAdd.addActionListener(this::btnAddActionPerformed);
        getContentPane().add(btnAdd, new org.netbeans.lib.awtextra.AbsoluteConstraints(710, 170, 240, -1));

        btnEdit.setFont(new java.awt.Font("Segoe UI", 1, 24)); // NOI18N
        btnEdit.setText("Edit");
        btnEdit.addActionListener(this::btnEditActionPerformed);
        getContentPane().add(btnEdit, new org.netbeans.lib.awtextra.AbsoluteConstraints(710, 290, 240, -1));

        btnDelete.setFont(new java.awt.Font("Segoe UI", 1, 24)); // NOI18N
        btnDelete.setText("Delete");
        btnDelete.addActionListener(this::btnDeleteActionPerformed);
        getContentPane().add(btnDelete, new org.netbeans.lib.awtextra.AbsoluteConstraints(710, 420, 240, -1));

        btnBack.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        btnBack.setText("Back");
        btnBack.addActionListener(this::btnBackActionPerformed);
        getContentPane().add(btnBack, new org.netbeans.lib.awtextra.AbsoluteConstraints(50, 620, 130, 40));

        jLabel1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/desain/Kelas.png"))); // NOI18N
        getContentPane().add(jLabel1, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 0, -1, -1));

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnAddActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnAddActionPerformed
       // Membuka pop-up input kelas (akan kita buat setelah ini)
        FormInputKelas frmInput = new FormInputKelas(this, true); 
        frmInput.setVisible(true);
    }//GEN-LAST:event_btnAddActionPerformed

    private void btnEditActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnEditActionPerformed
        int baris = tblKelas.getSelectedRow();
        if (baris != -1) {
            int id = Integer.parseInt(tblKelas.getValueAt(baris, 0).toString());
            String nama = tblKelas.getValueAt(baris, 1).toString();
            
            FormInputKelas frmInput = new FormInputKelas(this, true, id, nama);
            frmInput.setVisible(true);
        } else {
            JOptionPane.showMessageDialog(this, "Silakan pilih kelas yang ingin diedit!");
        }
    }//GEN-LAST:event_btnEditActionPerformed

    private void btnDeleteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnDeleteActionPerformed
       int baris = tblKelas.getSelectedRow();
        if (baris != -1) {
            int id = Integer.parseInt(tblKelas.getValueAt(baris, 0).toString());
            int confirm = JOptionPane.showConfirmDialog(this, "Yakin ingin menghapus kelas ini?", "Konfirmasi", JOptionPane.YES_NO_OPTION);
            
            if (confirm == JOptionPane.YES_OPTION) {
                if (db.deleteKelas(id)) {
                    JOptionPane.showMessageDialog(this, "Kelas berhasil dihapus!");
                    loadData();
                } else {
                    JOptionPane.showMessageDialog(this, "Gagal menghapus kelas!");
                }
            }
        } else {
            JOptionPane.showMessageDialog(this, "Silakan pilih kelas yang ingin dihapus!");
        }
    }//GEN-LAST:event_btnDeleteActionPerformed

    private void btnBackActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnBackActionPerformed
        this.dispose(); // Menutup jendela FormKelas
    }//GEN-LAST:event_btnBackActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ReflectiveOperationException | javax.swing.UnsupportedLookAndFeelException ex) {
           java.util.logging.Logger.getLogger(FormKelas.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(() -> new FormRuang().setVisible(true));
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnAdd;
    private javax.swing.JButton btnBack;
    private javax.swing.JButton btnDelete;
    private javax.swing.JButton btnEdit;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTable tblKelas;
    // End of variables declaration//GEN-END:variables
}
