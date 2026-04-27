package PBO_Project_2;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.PreparedStatement;
import javax.swing.AbstractCellEditor;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

public class data_krs extends javax.swing.JFrame {

    private String namaSesi, roleSesi;
    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(data_krs.class.getName());

    public data_krs(String nama, String role) {
        this.namaSesi = nama;
        this.roleSesi = role;
        initComponents();
        tampilkan_data();
        this.setLocationRelativeTo(null); 
        
        // Memanggil fungsi pengecekan hak akses setelah tampilan dimuat
        cekHakAkses(); 
    }

    public data_krs() {
        initComponents();
        tampilkan_data();
        this.setLocationRelativeTo(null);
    }

    // ================= FUNGSI PEMBATASAN HAK AKSES =================
    private void cekHakAkses() {
        if (roleSesi == null || !roleSesi.equalsIgnoreCase("admin")) {
            btnAdd.setVisible(false);
            btnEdit.setVisible(false);
            btnDelete.setVisible(false);
            jLabel1.setText("Data KRS (Mode Lihat)");
        } else {
            jLabel1.setText("Manajemen Data KRS");
        }
    }

    private void tampilkan_data() {
        DefaultTableModel model = new DefaultTableModel();

        model.addColumn("ID KRS"); // Kolom 0
        model.addColumn("Nama Mahasiswa"); // Kolom 1
        model.addColumn("Nama Dosen ACC"); // Kolom 2
        model.addColumn("Semester"); // Kolom 3
        model.addColumn("Tahun Ajaran"); // Kolom 4
        model.addColumn("Status KRS"); // Kolom 5
        model.addColumn("Tanggal Pengajuan"); // Kolom 6
        model.addColumn("Detail"); // Kolom 7

        try {
            Connection conn = new database().getConnection();
            
            String sql = "SELECT krs.id_krs, mahasiswa.nama_mahasiswa, dosen.nama_dosen, krs.semester, krs.tahun_ajaran, krs.status_krs, krs.tanggal_pengajuan " +
                         "FROM krs " +
                         "LEFT JOIN mahasiswa ON krs.id_mahasiswa = mahasiswa.id_mahasiswa " +
                         "LEFT JOIN dosen ON krs.id_dosen_acc = dosen.id_dosen";
            
            Statement st = conn.createStatement();
            ResultSet res = st.executeQuery(sql);
            
            while (res.next()) {
                String namaMhs = res.getString("nama_mahasiswa");
                String namaDosen = res.getString("nama_dosen");
                
                model.addRow(new Object[]{
                    res.getString("id_krs"),
                    (namaMhs == null) ? "Tidak Ditemukan" : namaMhs,
                    (namaDosen == null) ? "Belum Di-ACC" : namaDosen,
                    res.getString("semester"),
                    res.getString("tahun_ajaran"),
                    res.getString("status_krs"),
                    res.getString("tanggal_pengajuan"),
                    "See Detail" 
                });
            }
            tabelKRS.setModel(model);
            
            tabelKRS.getColumnModel().getColumn(7).setCellRenderer(new ButtonRenderer());
            tabelKRS.getColumnModel().getColumn(7).setCellEditor(new ButtonEditor(tabelKRS));
            
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Gagal memuat data KRS: " + e.getMessage());
        }
    }

    // ================= CLASS RENDERER UNTUK MENGGAMBAR TOMBOL =================
    class ButtonRenderer extends JButton implements TableCellRenderer {
        public ButtonRenderer() {
            setOpaque(true);
        }
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            setText((value == null) ? "See Detail" : value.toString());
            return this;
        }
    }

    // ================= CLASS EDITOR: POP-UP DETAIL KRS =================
    class ButtonEditor extends AbstractCellEditor implements TableCellEditor, ActionListener {
        private JButton button;
        private String label;
        private boolean isPushed;
        private JTable table;
        private int currentRow;

        public ButtonEditor(JTable table) {
            button = new JButton();
            button.setOpaque(true);
            button.addActionListener(this);
            this.table = table;
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            label = (value == null) ? "See Detail" : value.toString();
            button.setText(label);
            isPushed = true;
            currentRow = row;
            return button;
        }

        @Override
        public Object getCellEditorValue() {
            return label;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (isPushed) {
                String idKrs = table.getValueAt(currentRow, 0).toString();
                String namaMhs = table.getValueAt(currentRow, 1).toString();
                
                // 1. Buat Model Tabel Khusus untuk Pop-Up Detail dengan kolom Nilai
                DefaultTableModel detailModel = new DefaultTableModel();
                detailModel.addColumn("ID Detail");
                detailModel.addColumn("Mata Kuliah");
                detailModel.addColumn("SKS");
                detailModel.addColumn("Nilai"); // Kolom Baru untuk Nilai

                // 2. Ambil data dari database dengan struktur relasi krs_detail -> jadwal -> matkul
                try {
                    Connection conn = new database().getConnection();
                    
                    // QUERY BARU: Multi-Join agar mendapat nama Matkul dari id_jadwal
                    String sqlDetail = "SELECT kd.id_detail, m.nama_matkul, m.sks, kd.nilai " +
                                       "FROM krs_detail kd " +
                                       "JOIN jadwal j ON kd.id_jadwal = j.id_jadwal " +
                                       "JOIN matkul m ON j.id_matkul = m.id_matkul " +
                                       "WHERE kd.id_krs = ?";
                    
                    PreparedStatement pst = conn.prepareStatement(sqlDetail);
                    
                    // Pastikan menggunakan setInt agar aman untuk database MySQL
                    pst.setInt(1, Integer.parseInt(idKrs)); 
                    
                    ResultSet rs = pst.executeQuery();
                    
                    int totalSKS = 0;
                    while (rs.next()) {
                        String nilai = rs.getString("nilai");
                        
                        detailModel.addRow(new Object[]{
                            rs.getString("id_detail"),
                            rs.getString("nama_matkul"),
                            rs.getString("sks"),
                            (nilai == null || nilai.isEmpty()) ? "-" : nilai // Menampilkan "-" jika nilai masih kosong
                        });
                        totalSKS += rs.getInt("sks");
                    }
                    
                    // Jika data detail masih kosong
                    if (detailModel.getRowCount() == 0) {
                        detailModel.addRow(new Object[]{"-", "Belum ada mata kuliah", "-", "-"});
                    } else {
                        // Tambahkan baris kosong sebagai pemisah, lalu tampilkan Total SKS
                        detailModel.addRow(new Object[]{"", "TOTAL SKS", totalSKS, ""});
                    }

                    // 3. Masukkan tabel ke dalam ScrollPane agar rapi dan bisa di-scroll
                    JTable tabelDetail = new JTable(detailModel);
                    JScrollPane scrollPane = new JScrollPane(tabelDetail);
                    scrollPane.setPreferredSize(new Dimension(550, 250)); // Sedikit diperlebar untuk kolom nilai

                    // 4. Tampilkan dalam Pop-Up JOptionPane
                    JOptionPane.showMessageDialog(button, scrollPane, "Detail KRS - " + namaMhs + " (ID: " + idKrs + ")", JOptionPane.PLAIN_MESSAGE);

                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(button, "Gagal memuat detail KRS:\n" + ex.getMessage(), "Error Database", JOptionPane.ERROR_MESSAGE);
                }
            }
            isPushed = false;
            fireEditingStopped();
        }
    }
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jScrollPane1 = new javax.swing.JScrollPane();
        tabelKRS = new javax.swing.JTable();
        back = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();
        btnAdd = new javax.swing.JButton();
        btnEdit = new javax.swing.JButton();
        btnDelete = new javax.swing.JButton();
        jLabel2 = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        getContentPane().setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jScrollPane1.setPreferredSize(new java.awt.Dimension(1000, 1000));

        tabelKRS.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null}
            },
            new String [] {
                "id_krs ", "id_mahasiswa", "id_dosen_acc", "semester", "tahun_ajaran", "status_krs", "tanggal_pengajuan", "Detail"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Object.class, java.lang.Object.class, java.lang.Object.class
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }
        });
        jScrollPane1.setViewportView(tabelKRS);

        getContentPane().add(jScrollPane1, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 100, 970, 370));

        back.setFont(new java.awt.Font("Segoe UI", 1, 24)); // NOI18N
        back.setText("Back");
        back.addActionListener(this::backActionPerformed);
        getContentPane().add(back, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 530, 140, -1));

        jLabel1.setFont(new java.awt.Font("Segoe UI", 0, 36)); // NOI18N
        getContentPane().add(jLabel1, new org.netbeans.lib.awtextra.AbsoluteConstraints(6, 812, 269, 39));

        btnAdd.setFont(new java.awt.Font("Segoe UI", 1, 24)); // NOI18N
        btnAdd.setText("Add");
        btnAdd.addActionListener(this::btnAddActionPerformed);
        getContentPane().add(btnAdd, new org.netbeans.lib.awtextra.AbsoluteConstraints(270, 530, 240, -1));

        btnEdit.setFont(new java.awt.Font("Segoe UI", 1, 24)); // NOI18N
        btnEdit.setText("Edit");
        btnEdit.addActionListener(this::btnEditActionPerformed);
        getContentPane().add(btnEdit, new org.netbeans.lib.awtextra.AbsoluteConstraints(520, 530, 230, -1));

        btnDelete.setFont(new java.awt.Font("Segoe UI", 1, 24)); // NOI18N
        btnDelete.setText("Delete");
        btnDelete.addActionListener(this::btnDeleteActionPerformed);
        getContentPane().add(btnDelete, new org.netbeans.lib.awtextra.AbsoluteConstraints(760, 530, 230, -1));

        jLabel2.setIcon(new javax.swing.ImageIcon(getClass().getResource("/desain/Manajemen data.png"))); // NOI18N
        getContentPane().add(jLabel2, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, -40, -1, 900));

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void backActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_backActionPerformed
    // Panggil dashboard kembali dengan membawa namaSesi dan roleSesi
    dashboard balik = new dashboard(namaSesi, roleSesi);
    
    balik.setVisible(true);
    balik.setLocationRelativeTo(null);
    this.dispose(); // Tutup masteruser        // TODO add your handling code here:
    }//GEN-LAST:event_backActionPerformed

    private void btnAddActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnAddActionPerformed
        JComboBox<String> cbNamaMhs = new JComboBox<>();
        JComboBox<String> cbNamaDosen = new JComboBox<>();
        cbNamaDosen.addItem("-- Belum Di-ACC --");

        try {
            Connection conn = new database().getConnection();
            Statement st = conn.createStatement();
            
            ResultSet rsMhs = st.executeQuery("SELECT nama_mahasiswa FROM mahasiswa");
            while(rsMhs.next()) { cbNamaMhs.addItem(rsMhs.getString("nama_mahasiswa")); }
            
            ResultSet rsDsn = st.executeQuery("SELECT nama_dosen FROM dosen");
            while(rsDsn.next()) { cbNamaDosen.addItem(rsDsn.getString("nama_dosen")); }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Gagal memuat daftar nama dari database: " + e.getMessage());
        }

        JComboBox<String> cbStatus = new JComboBox<>(new String[]{"terbuka", "tertutup"});
        javax.swing.JSpinner dateSpinner = new javax.swing.JSpinner(new javax.swing.SpinnerDateModel());
        javax.swing.JSpinner.DateEditor dateEditor = new javax.swing.JSpinner.DateEditor(dateSpinner, "yyyy-MM-dd");
        dateSpinner.setEditor(dateEditor);

        JTextField txtSemester = new JTextField();
        JTextField txtTahun = new JTextField();

        Object[] formFields = {
            "Pilih Mahasiswa:", cbNamaMhs,
            "Pilih Dosen ACC:", cbNamaDosen,
            "Semester:", txtSemester,
            "Tahun Ajaran (Contoh: 2025/2026):", txtTahun,
            "Status KRS:", cbStatus,
            "Tanggal Pengajuan:", dateSpinner
        };

        // PERUBAHAN DI SINI: MENGGUNAKAN PLAIN_MESSAGE
        int option = JOptionPane.showConfirmDialog(this, formFields, "Tambah Data KRS", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        
        if (option == JOptionPane.OK_OPTION) {
            try {
                String selectedMhs = cbNamaMhs.getSelectedItem() != null ? cbNamaMhs.getSelectedItem().toString() : "";
                String selectedDosen = cbNamaDosen.getSelectedItem() != null ? cbNamaDosen.getSelectedItem().toString() : "";
                
                Connection conn = new database().getConnection();
                
                int idMhs = -1;
                PreparedStatement pstMhs = conn.prepareStatement("SELECT id_mahasiswa FROM mahasiswa WHERE nama_mahasiswa = ?");
                pstMhs.setString(1, selectedMhs);
                ResultSet rsMhs = pstMhs.executeQuery();
                if (rsMhs.next()) idMhs = rsMhs.getInt("id_mahasiswa");

                Integer idDosen = null;
                if (!selectedDosen.equals("-- Belum Di-ACC --") && !selectedDosen.isEmpty()) {
                    PreparedStatement pstDsn = conn.prepareStatement("SELECT id_dosen FROM dosen WHERE nama_dosen = ?");
                    pstDsn.setString(1, selectedDosen);
                    ResultSet rsDsn = pstDsn.executeQuery();
                    if (rsDsn.next()) idDosen = rsDsn.getInt("id_dosen");
                }

                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd");
                String tanggalFix = sdf.format(dateSpinner.getValue());

                String sql = "INSERT INTO krs (id_mahasiswa, id_dosen_acc, semester, tahun_ajaran, status_krs, tanggal_pengajuan) VALUES (?, ?, ?, ?, ?, ?)";
                PreparedStatement pst = conn.prepareStatement(sql);
                
                pst.setInt(1, idMhs);
                if (idDosen == null) pst.setNull(2, java.sql.Types.INTEGER);
                else pst.setInt(2, idDosen);
                
                pst.setString(3, txtSemester.getText());
                pst.setString(4, txtTahun.getText());
                pst.setString(5, cbStatus.getSelectedItem().toString()); 
                pst.setString(6, tanggalFix); 
                
                pst.executeUpdate();
                JOptionPane.showMessageDialog(this, "Data KRS berhasil ditambahkan!");
                tampilkan_data(); 
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Terjadi kesalahan sistem: " + e.getMessage());
            }
        }
    
    }//GEN-LAST:event_btnAddActionPerformed

    private void btnEditActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnEditActionPerformed

int baris = tabelKRS.getSelectedRow();
        if (baris == -1) {
            JOptionPane.showMessageDialog(this, "Silakan pilih baris tabel yang ingin diedit terlebih dahulu!");
            return;
        }

        String idKrs = tabelKRS.getValueAt(baris, 0).toString();
        String oldNamaMhs = tabelKRS.getValueAt(baris, 1).toString();
        String oldNamaDosen = tabelKRS.getValueAt(baris, 2).toString();

        JComboBox<String> cbNamaMhs = new JComboBox<>();
        JComboBox<String> cbNamaDosen = new JComboBox<>();
        cbNamaDosen.addItem("-- Belum Di-ACC --");

        try {
            Connection conn = new database().getConnection();
            Statement st = conn.createStatement();
            
            ResultSet rsMhs = st.executeQuery("SELECT nama_mahasiswa FROM mahasiswa");
            while(rsMhs.next()) { cbNamaMhs.addItem(rsMhs.getString("nama_mahasiswa")); }
            
            ResultSet rsDsn = st.executeQuery("SELECT nama_dosen FROM dosen");
            while(rsDsn.next()) { cbNamaDosen.addItem(rsDsn.getString("nama_dosen")); }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Gagal memuat daftar nama dari database: " + e.getMessage());
        }

        if (!oldNamaMhs.equals("Tidak Ditemukan")) cbNamaMhs.setSelectedItem(oldNamaMhs);
        if (!oldNamaDosen.equals("Belum Di-ACC")) cbNamaDosen.setSelectedItem(oldNamaDosen);

        JComboBox<String> cbStatus = new JComboBox<>(new String[]{"terbuka", "tertutup"});
        String oldStatus = tabelKRS.getValueAt(baris, 5) != null ? tabelKRS.getValueAt(baris, 5).toString() : "terbuka";
        cbStatus.setSelectedItem(oldStatus);

        javax.swing.JSpinner dateSpinner = new javax.swing.JSpinner(new javax.swing.SpinnerDateModel());
        javax.swing.JSpinner.DateEditor dateEditor = new javax.swing.JSpinner.DateEditor(dateSpinner, "yyyy-MM-dd");
        dateSpinner.setEditor(dateEditor);
        
        String oldTanggal = tabelKRS.getValueAt(baris, 6) != null ? tabelKRS.getValueAt(baris, 6).toString() : "";
        try {
            if (!oldTanggal.isEmpty() && !oldTanggal.equals("-")) {
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd");
                dateSpinner.setValue(sdf.parse(oldTanggal));
            }
        } catch (Exception e) { }

        JTextField txtSemester = new JTextField(tabelKRS.getValueAt(baris, 3) != null ? tabelKRS.getValueAt(baris, 3).toString() : "");
        JTextField txtTahun = new JTextField(tabelKRS.getValueAt(baris, 4) != null ? tabelKRS.getValueAt(baris, 4).toString() : "");

        Object[] formFields = {
            "Pilih Mahasiswa:", cbNamaMhs,
            "Pilih Dosen ACC:", cbNamaDosen,
            "Semester:", txtSemester,
            "Tahun Ajaran:", txtTahun,
            "Status KRS:", cbStatus,
            "Tanggal Pengajuan:", dateSpinner
        };

        // PERUBAHAN DI SINI: MENGGUNAKAN PLAIN_MESSAGE
        int option = JOptionPane.showConfirmDialog(this, formFields, "Edit Data KRS (ID: " + idKrs + ")", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        
        if (option == JOptionPane.OK_OPTION) {
            try {
                String selectedMhs = cbNamaMhs.getSelectedItem() != null ? cbNamaMhs.getSelectedItem().toString() : "";
                String selectedDosen = cbNamaDosen.getSelectedItem() != null ? cbNamaDosen.getSelectedItem().toString() : "";
                
                Connection conn = new database().getConnection();
                
                int idMhs = -1;
                PreparedStatement pstMhs = conn.prepareStatement("SELECT id_mahasiswa FROM mahasiswa WHERE nama_mahasiswa = ?");
                pstMhs.setString(1, selectedMhs);
                ResultSet rsMhs = pstMhs.executeQuery();
                if (rsMhs.next()) idMhs = rsMhs.getInt("id_mahasiswa");

                Integer idDosen = null;
                if (!selectedDosen.equals("-- Belum Di-ACC --") && !selectedDosen.isEmpty()) {
                    PreparedStatement pstDsn = conn.prepareStatement("SELECT id_dosen FROM dosen WHERE nama_dosen = ?");
                    pstDsn.setString(1, selectedDosen);
                    ResultSet rsDsn = pstDsn.executeQuery();
                    if (rsDsn.next()) idDosen = rsDsn.getInt("id_dosen");
                }

                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd");
                String tanggalFix = sdf.format(dateSpinner.getValue());

                String sql = "UPDATE krs SET id_mahasiswa=?, id_dosen_acc=?, semester=?, tahun_ajaran=?, status_krs=?, tanggal_pengajuan=? WHERE id_krs=?";
                PreparedStatement pst = conn.prepareStatement(sql);
                
                pst.setInt(1, idMhs);
                if (idDosen == null) pst.setNull(2, java.sql.Types.INTEGER);
                else pst.setInt(2, idDosen);
                
                pst.setString(3, txtSemester.getText());
                pst.setString(4, txtTahun.getText());
                pst.setString(5, cbStatus.getSelectedItem().toString());
                pst.setString(6, tanggalFix);
                pst.setString(7, idKrs);
                
                pst.executeUpdate();
                JOptionPane.showMessageDialog(this, "Data KRS berhasil diperbarui!");
                tampilkan_data();
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Terjadi kesalahan saat update: " + e.getMessage());
            }
        }
    
    }//GEN-LAST:event_btnEditActionPerformed

    private void btnDeleteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnDeleteActionPerformed

        int baris = tabelKRS.getSelectedRow();
        if (baris == -1) {
            JOptionPane.showMessageDialog(this, "Silakan pilih baris tabel yang ingin dihapus!");
            return;
        }

        String idKrs = tabelKRS.getValueAt(baris, 0).toString();
        int konfirmasi = JOptionPane.showConfirmDialog(this, "Yakin ingin menghapus KRS dengan ID " + idKrs + "?", "Konfirmasi Hapus", JOptionPane.YES_NO_OPTION);
        
        if (konfirmasi == JOptionPane.YES_OPTION) {
            try {
                Connection conn = new database().getConnection();
                PreparedStatement pst = conn.prepareStatement("DELETE FROM krs WHERE id_krs = ?");
                pst.setString(1, idKrs);
                pst.executeUpdate();
                
                JOptionPane.showMessageDialog(this, "Data KRS berhasil dihapus!");
                tampilkan_data();
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Gagal menghapus data: " + e.getMessage());
            }
        }
    
    }//GEN-LAST:event_btnDeleteActionPerformed

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
    private javax.swing.JButton btnAdd;
    private javax.swing.JButton btnDelete;
    private javax.swing.JButton btnEdit;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTable tabelKRS;
    // End of variables declaration//GEN-END:variables
}
