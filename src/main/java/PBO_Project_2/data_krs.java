package PBO_Project_2;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.PreparedStatement;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;

public class data_krs extends javax.swing.JFrame {

    private String namaSesi, roleSesi;
    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(data_krs.class.getName());

    public data_krs(String nama, String role) {
        this.namaSesi = nama;
        this.roleSesi = role;
        initComponents();
        tampilkan_data();
        this.setLocationRelativeTo(null); 
    }

    public data_krs() {
        initComponents();
        tampilkan_data();
        this.setLocationRelativeTo(null);
    }

private void tampilkan_data() {
        DefaultTableModel model = new DefaultTableModel();
        // 1. Sesuaikan 7 kolom sesuai dengan database
        model.addColumn("ID KRS");
        model.addColumn("ID Mahasiswa");
        model.addColumn("ID Dosen ACC");
        model.addColumn("Semester");
        model.addColumn("Tahun Ajaran");
        model.addColumn("Status KRS");
        model.addColumn("Tanggal Pengajuan");

        try {
            Connection conn = new database().getConnection();
            
            // 2. Query simple untuk mengambil 7 kolom dari tabel krs
            String sql = "SELECT id_krs, id_mahasiswa, id_dosen_acc, semester, tahun_ajaran, status_krs, tanggal_pengajuan FROM krs";
            
            Statement st = conn.createStatement();
            ResultSet res = st.executeQuery(sql);
            
            // 3. Masukkan 7 data tersebut ke dalam baris tabel
            while (res.next()) {
                model.addRow(new Object[]{
                    res.getString("id_krs"),
                    res.getString("id_mahasiswa"),
                    res.getString("id_dosen_acc"),
                    res.getString("semester"),
                    res.getString("tahun_ajaran"),
                    res.getString("status_krs"),
                    res.getString("tanggal_pengajuan")
                });
            }
            tabelKRS.setModel(model);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Gagal memuat data KRS: " + e.getMessage());
        }
    }
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jScrollPane1 = new javax.swing.JScrollPane();
        tabelKRS = new javax.swing.JTable();
        back = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jScrollPane1.setPreferredSize(new java.awt.Dimension(1000, 1000));

        tabelKRS.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null}
            },
            new String [] {
                "id_krs ", "id_mahasiswa", "id_dosen_acc", "semester", "tahun_ajaran", "status_krs", "tanggal_pengajuan"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Object.class, java.lang.Object.class
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }
        });
        jScrollPane1.setViewportView(tabelKRS);

        back.setText("Back");
        back.addActionListener(this::backActionPerformed);

        jLabel1.setFont(new java.awt.Font("Segoe UI", 0, 36)); // NOI18N
        jLabel1.setText("Data KRS");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(back)
                            .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 269, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(0, 357, Short.MAX_VALUE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(11, 11, 11)
                .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 39, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 319, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 9, Short.MAX_VALUE)
                .addComponent(back))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void backActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_backActionPerformed
    // Panggil dashboard kembali dengan membawa namaSesi dan roleSesi
    dashboard balik = new dashboard(namaSesi, roleSesi);
    
    balik.setVisible(true);
    balik.setLocationRelativeTo(null);
    this.dispose(); // Tutup masteruser        // TODO add your handling code here:
    }//GEN-LAST:event_backActionPerformed

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
            logger.log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(() -> new data_krs().setVisible(true));
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton back;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTable tabelKRS;
    // End of variables declaration//GEN-END:variables
}
