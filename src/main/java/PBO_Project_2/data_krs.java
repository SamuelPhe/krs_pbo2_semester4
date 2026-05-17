package PBO_Project_2;

import java.awt.Component;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.PreparedStatement;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

public class data_krs extends javax.swing.JFrame {

    private String namaSesi, roleSesi;

    public data_krs(String nama, String role) {
        this.namaSesi = nama;
        this.roleSesi = role;
        initComponents();
        tampilkan_data();
        this.setLocationRelativeTo(null); 
        cekHakAkses(); 
    }

    public data_krs() {
        initComponents();
        tampilkan_data();
        this.setLocationRelativeTo(null);
    }

    private void cekHakAkses() {
        if (roleSesi == null || !roleSesi.equalsIgnoreCase("admin")) {
            btnAdd.setVisible(false);
            btnEdit.setVisible(false);
            btnDelete.setVisible(false);
            jLabel1.setText("Sesi Pengisian KRS (Mode Lihat)");
        } else {
            jLabel1.setText("Manajemen Aktifasi Sesi KRS");
        }
    }

    // ================= REVISI UTAMA: TAMPILAN TABEL SESI KRS =================
    private void tampilkan_data() {
        DefaultTableModel model = new DefaultTableModel();

        model.addColumn("ID Sesi");
        model.addColumn("Program Studi");
        model.addColumn("Semester");
        model.addColumn("Tahun Ajaran");
        model.addColumn("Status KRS");
        model.addColumn("Tanggal Mulai");
        model.addColumn("Tanggal Selesai");

        try {
            Connection conn = new database().getConnection();
            
            // Mengambil data sesi krs dan men-join nama prodi aslinya
            String sql = "SELECT krs.*, prodi.nama_prodi FROM krs " +
                         "LEFT JOIN prodi ON krs.id_prodi = prodi.id_prodi";
            
            Statement st = conn.createStatement();
            ResultSet res = st.executeQuery(sql);
            
            while (res.next()) {
                String prodi = res.getString("nama_prodi");
                model.addRow(new Object[]{
                    res.getString("id_krs"),
                    (prodi == null) ? "Semua Prodi" : prodi,
                    res.getString("semester"),
                    res.getString("tahun_ajaran"),
                    res.getString("status_krs"),
                    res.getString("tanggal_mulai"),
                    res.getString("tanggal_selesai")
                });
            }
            tabelKRS.setModel(model);
            
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Gagal memuat data Sesi KRS: " + e.getMessage());
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
    dashboard balik = new dashboard(namaSesi, roleSesi);
        balik.setVisible(true);
        balik.setLocationRelativeTo(null);
        this.dispose();
    }//GEN-LAST:event_backActionPerformed

    private void btnAddActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnAddActionPerformed
        JComboBox<String> cbProdi = new JComboBox<>();
        cbProdi.addItem("-- Pilih Program Studi --");

        // Ambil data prodi yang ada di database untuk dimasukkan ke dropdown prodi
        try {
            Connection conn = new database().getConnection();
            ResultSet rsProdi = conn.createStatement().executeQuery("SELECT nama_prodi FROM prodi");
            while(rsProdi.next()) { 
                cbProdi.addItem(rsProdi.getString("nama_prodi")); 
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Gagal memuat prodi: " + e.getMessage());
        }

        JTextField txtSemester = new JTextField();
        JTextField txtTahun = new JTextField();
        JComboBox<String> cbStatus = new JComboBox<>(new String[]{"terbuka", "tertutup"});
        
        // Input Tanggal Mulai & Selesai Aktivasi KRS
        javax.swing.JSpinner spinMulai = new javax.swing.JSpinner(new javax.swing.SpinnerDateModel());
        javax.swing.JSpinner.DateEditor edMulai = new javax.swing.JSpinner.DateEditor(spinMulai, "yyyy-MM-dd");
        spinMulai.setEditor(edMulai);

        javax.swing.JSpinner spinSelesai = new javax.swing.JSpinner(new javax.swing.SpinnerDateModel());
        javax.swing.JSpinner.DateEditor edSelesai = new javax.swing.JSpinner.DateEditor(spinSelesai, "yyyy-MM-dd");
        spinSelesai.setEditor(edSelesai);

        Object[] formFields = {
            "Pilih Program Studi:", cbProdi,
            "Semester:", txtSemester,
            "Tahun Ajaran (Contoh: 2025/2026):", txtTahun,
            "Status Sesi KRS:", cbStatus,
            "Tanggal Mulai Dibuka:", spinMulai,
            "Tanggal Selesai Penutupan:", spinSelesai
        };

        int option = JOptionPane.showConfirmDialog(this, formFields, "Tambah Sesi Pembukaan KRS Global", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        
        if (option == JOptionPane.OK_OPTION) {
            if(cbProdi.getSelectedIndex() == 0 || txtSemester.getText().isEmpty() || txtTahun.getText().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Semua data wajib diisi/dipilih!");
                return;
            }
            
            try {
                Connection conn = new database().getConnection();
                
                // Cari ID Prodi berdasarkan teks nama prodi terpilih
                int idProdi = -1;
                PreparedStatement pstProdi = conn.prepareStatement("SELECT id_prodi FROM prodi WHERE nama_prodi = ?");
                pstProdi.setString(1, cbProdi.getSelectedItem().toString());
                ResultSet rsP = pstProdi.executeQuery();
                if (rsP.next()) idProdi = rsP.getInt("id_prodi");

                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd");
                String tglMulai = sdf.format(spinMulai.getValue());
                String tglSelesai = sdf.format(spinSelesai.getValue());
                
                // --- KODE BARU: CEK BENTROK SESI KRS ---
                String sqlCek = "SELECT id_krs FROM krs WHERE id_prodi = ? AND semester = ? AND tahun_ajaran = ? " +
                                "AND (tanggal_mulai <= ? AND tanggal_selesai >= ?)";
                
                PreparedStatement pstCek = conn.prepareStatement(sqlCek);
                pstCek.setInt(1, idProdi);
                pstCek.setString(2, txtSemester.getText());
                pstCek.setString(3, txtTahun.getText());
                pstCek.setString(4, tglSelesai); // Perhatikan: ini parameter untuk <= tglSelesai inputan
                pstCek.setString(5, tglMulai);   // Perhatikan: ini parameter untuk >= tglMulai inputan
                
                ResultSet rsCek = pstCek.executeQuery();
                if (rsCek.next()) {
                    JOptionPane.showMessageDialog(this, "GAGAL: Sesi KRS untuk Prodi, Semester, dan Tahun Ajaran tersebut sudah ada pada rentang tanggal yang bersinggungan!", "Peringatan Bentrok", JOptionPane.WARNING_MESSAGE);
                    return; // Batalkan proses simpan
                }
                // --- BATAS KODE BARU ---

             
                String sql = "INSERT INTO krs (id_prodi, semester, tahun_ajaran, status_krs, tanggal_mulai, tanggal_selesai) VALUES (?, ?, ?, ?, ?, ?)";
                PreparedStatement pst = conn.prepareStatement(sql);
                pst.setInt(1, idProdi);
                pst.setString(2, txtSemester.getText());
                pst.setString(3, txtTahun.getText());
                pst.setString(4, cbStatus.getSelectedItem().toString());
                pst.setString(5, tglMulai);
                pst.setString(6, tglSelesai);
                
                pst.executeUpdate();
                JOptionPane.showMessageDialog(this, "Sesi Aktivasi KRS Global Berhasil Dibuka!");
                tampilkan_data(); 
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Kesalahan sistem input krs: " + e.getMessage());
            }
        }
    
    }//GEN-LAST:event_btnAddActionPerformed

    private void btnEditActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnEditActionPerformed
                                     
        int baris = tabelKRS.getSelectedRow();
        if (baris == -1) {
            JOptionPane.showMessageDialog(this, "Silakan pilih baris sesi KRS yang ingin diedit!");
            return;
        }

        String idKrs = tabelKRS.getValueAt(baris, 0).toString();
        String oldProdi = tabelKRS.getValueAt(baris, 1).toString();

        JComboBox<String> cbProdi = new JComboBox<>();
        cbProdi.addItem("-- Pilih Program Studi --");
        try {
            Connection conn = new database().getConnection();
            ResultSet rsProdi = conn.createStatement().executeQuery("SELECT nama_prodi FROM prodi");
            while(rsProdi.next()) { cbProdi.addItem(rsProdi.getString("nama_prodi")); }
        } catch (Exception e) {}
        cbProdi.setSelectedItem(oldProdi);

        JTextField txtSemester = new JTextField(tabelKRS.getValueAt(baris, 2).toString());
        JTextField txtTahun = new JTextField(tabelKRS.getValueAt(baris, 3).toString());
        
        JComboBox<String> cbStatus = new JComboBox<>(new String[]{"terbuka", "tertutup"});
        cbStatus.setSelectedItem(tabelKRS.getValueAt(baris, 4).toString());

        javax.swing.JSpinner spinMulai = new javax.swing.JSpinner(new javax.swing.SpinnerDateModel());
        javax.swing.JSpinner.DateEditor edMulai = new javax.swing.JSpinner.DateEditor(spinMulai, "yyyy-MM-dd");
        spinMulai.setEditor(edMulai);

        javax.swing.JSpinner spinSelesai = new javax.swing.JSpinner(new javax.swing.SpinnerDateModel());
        javax.swing.JSpinner.DateEditor edSelesai = new javax.swing.JSpinner.DateEditor(spinSelesai, "yyyy-MM-dd");
        spinSelesai.setEditor(edSelesai);
        
        try {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd");
            spinMulai.setValue(sdf.parse(tabelKRS.getValueAt(baris, 5).toString()));
            spinSelesai.setValue(sdf.parse(tabelKRS.getValueAt(baris, 6).toString()));
        } catch (Exception e) {}

        Object[] formFields = {
            "Pilih Program Studi:", cbProdi,
            "Semester:", txtSemester,
            "Tahun Ajaran:", txtTahun,
            "Status Sesi KRS:", cbStatus,
            "Tanggal Mulai Dibuka:", spinMulai,
            "Tanggal Selesai Penutupan:", spinSelesai
        };

        int option = JOptionPane.showConfirmDialog(this, formFields, "Edit Sesi KRS (ID Sesi: " + idKrs + ")", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        
        if (option == JOptionPane.OK_OPTION) {
            try {
                Connection conn = new database().getConnection();
                
                int idProdi = -1;
                PreparedStatement pstProdi = conn.prepareStatement("SELECT id_prodi FROM prodi WHERE nama_prodi = ?");
                pstProdi.setString(1, cbProdi.getSelectedItem().toString());
                ResultSet rsP = pstProdi.executeQuery();
                if (rsP.next()) idProdi = rsP.getInt("id_prodi");

                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd");
                String tglMulai = sdf.format(spinMulai.getValue());
                String tglSelesai = sdf.format(spinSelesai.getValue());
                
                // --- KODE BARU: CEK BENTROK SESI KRS (UNTUK EDIT) ---
                String sqlCek = "SELECT id_krs FROM krs WHERE id_prodi = ? AND semester = ? AND tahun_ajaran = ? " +
                                "AND (tanggal_mulai <= ? AND tanggal_selesai >= ?) AND id_krs != ?";
                
                PreparedStatement pstCek = conn.prepareStatement(sqlCek);
                pstCek.setInt(1, idProdi);
                pstCek.setString(2, txtSemester.getText());
                pstCek.setString(3, txtTahun.getText());
                pstCek.setString(4, tglSelesai); 
                pstCek.setString(5, tglMulai);   
                pstCek.setString(6, idKrs); // Abaikan ID KRS yang sedang diedit
                
                ResultSet rsCek = pstCek.executeQuery();
                if (rsCek.next()) {
                    JOptionPane.showMessageDialog(this, "GAGAL: Perubahan bentrok dengan Sesi KRS lain yang sudah ada pada rentang tanggal tersebut!", "Peringatan Bentrok", JOptionPane.WARNING_MESSAGE);
                    return; // Batalkan proses update
                }
                // --- BATAS KODE BARU ---

               
                String sql = "UPDATE krs SET id_prodi=?, semester=?, tahun_ajaran=?, status_krs=?, tanggal_mulai=?, tanggal_selesai=? WHERE id_krs=?";
                PreparedStatement pst = conn.prepareStatement(sql);
                pst.setInt(1, idProdi);
                pst.setString(2, txtSemester.getText());
                pst.setString(3, txtTahun.getText());
                pst.setString(4, cbStatus.getSelectedItem().toString());
                pst.setString(5, tglMulai);
                pst.setString(6, tglSelesai);
                pst.setString(7, idKrs);
                
                pst.executeUpdate();
                JOptionPane.showMessageDialog(this, "Data Sesi KRS Berhasil Diperbarui!");
                tampilkan_data();
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Gagal melakukan pembaruan data: " + e.getMessage());
            }
        }
    
    }//GEN-LAST:event_btnEditActionPerformed

    private void btnDeleteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnDeleteActionPerformed
int baris = tabelKRS.getSelectedRow();
        if (baris == -1) {
            JOptionPane.showMessageDialog(this, "Silakan pilih baris sesi KRS yang ingin dihapus!");
            return;
        }

        String idKrs = tabelKRS.getValueAt(baris, 0).toString();
        int konfirmasi = JOptionPane.showConfirmDialog(this, "Apakah Anda yakin ingin menghapus Sesi KRS dengan ID " + idKrs + "?", "Konfirmasi Hapus", JOptionPane.YES_NO_OPTION);
        
        if (konfirmasi == JOptionPane.YES_OPTION) {
            try {
                Connection conn = new database().getConnection();
                PreparedStatement pst = conn.prepareStatement("DELETE FROM krs WHERE id_krs = ?");
                pst.setString(1, idKrs);
                pst.executeUpdate();
                
                JOptionPane.showMessageDialog(this, "Data Sesi KRS berhasil dihapus dari sistem!");
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
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception ex) {
            java.util.logging.Logger.getLogger(data_krs.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }

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
