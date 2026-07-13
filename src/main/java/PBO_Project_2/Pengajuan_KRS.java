/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package PBO_Project_2;

import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.sql.*;
import javax.swing.table.TableColumn;
import java.awt.Component;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;

public class Pengajuan_KRS extends javax.swing.JFrame {

    private int idMahasiswaAktif = -1; 
    private static final Logger LOGGER = Logger.getLogger(Pengajuan_KRS.class.getName());

    public Pengajuan_KRS() {
        initComponents();
        this.setLocationRelativeTo(null);
        cariIdMahasiswa();    
        tampilkanSesiAktif(); 
        tampilkanRiwayat();   
        cekKunciAkses();
    }

    // --- FUNGSI-FUNGSI UTAMA ---
    private void cariIdMahasiswa() {
        try {
            Connection conn = new database().getConnection();
            PreparedStatement ps = conn.prepareStatement("SELECT id_mahasiswa FROM mahasiswa WHERE nama_mahasiswa = ?");
            ps.setString(1, Session.getNama());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                idMahasiswaAktif = rs.getInt("id_mahasiswa");
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Gagal memuat profil: " + e.getMessage());
        }
    }

    private void tampilkanSesiAktif() {
        DefaultTableModel model = new DefaultTableModel();
        model.addColumn("ID Sesi");
        model.addColumn("Program Studi");
        model.addColumn("Semester"); 
        model.addColumn("Tahun Ajaran");
        model.addColumn("Batas Tutup");

        try {
            Connection conn = new database().getConnection();
            int idProdiMahasiswa = 0;
            int semesterMahasiswa = 0;
            
            String sqlCariMhs = "SELECT id_prodi, semester FROM mahasiswa WHERE nama_mahasiswa = ?";
            PreparedStatement psCari = conn.prepareStatement(sqlCariMhs);
            psCari.setString(1, Session.getNama());
            ResultSet rsCari = psCari.executeQuery();
            
            if (rsCari.next()) {
                idProdiMahasiswa = rsCari.getInt("id_prodi");
                semesterMahasiswa = rsCari.getInt("semester");
            }
            
            String sql = "SELECT k.*, p.nama_prodi FROM krs k " +
                         "LEFT JOIN prodi p ON k.id_prodi = p.id_prodi " +
                         "WHERE k.status_krs = 'terbuka' " +
                         "AND CURRENT_DATE BETWEEN k.tanggal_mulai AND k.tanggal_selesai " +
                         "AND k.id_prodi = ? AND k.semester = ?"; 
                         
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, idProdiMahasiswa); 
            ps.setInt(2, semesterMahasiswa); 
            ResultSet res = ps.executeQuery();
            
            while (res.next()) {
                String prodi = res.getString("nama_prodi");
                model.addRow(new Object[]{
                    res.getString("id_krs"),
                    (prodi == null) ? "Semua Prodi" : prodi,
                    "Semester " + res.getInt("semester"), 
                    res.getString("tahun_ajaran"),
                    res.getString("tanggal_selesai")
                });
            }
            tabelSesiAktif.setModel(model); 
            aturLebarKolom(tabelSesiAktif);
        } catch (Exception e) {
            System.out.println("Error load sesi aktif: " + e.getMessage());
        }
    }

    private void tampilkanRiwayat() {
        DefaultTableModel model = new DefaultTableModel();
        model.addColumn("ID Pengajuan");
        model.addColumn("Sesi (Sem/Thn)");
        model.addColumn("Tgl Pengajuan");
        model.addColumn("Dosen PA");
        model.addColumn("Status");

        try {
            Connection conn = new database().getConnection();
            String sql = "SELECT pk.id_pengajuan, k.semester, k.tahun_ajaran, pk.tanggal_pengajuan, d.nama_dosen, pk.status_acc " +
                         "FROM pengajuan_krs pk " +
                         "JOIN krs k ON pk.id_krs = k.id_krs " +
                         "JOIN mahasiswa m ON pk.id_mahasiswa = m.id_mahasiswa " +
                         "LEFT JOIN dosen d ON m.id_dosen_pa = d.id_dosen " +
                         "WHERE pk.id_mahasiswa = ?";
            
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, idMahasiswaAktif);
            ResultSet res = ps.executeQuery();
            
            while (res.next()) {
                String dsn = res.getString("nama_dosen");
                model.addRow(new Object[]{
                    res.getString("id_pengajuan"),
                    "Sem " + res.getString("semester") + " (" + res.getString("tahun_ajaran") + ")",
                    res.getString("tanggal_pengajuan"),
                    (dsn == null) ? "Belum Diatur" : dsn,
                    res.getString("status_acc")
                });
            }
            tabelRiwayat.setModel(model);
            aturLebarKolom(tabelRiwayat);
            checkRevisi();
        } catch (Exception e) {
            System.out.println("Error load riwayat: " + e.getMessage());
        }
    }
    
    private void checkRevisi() {
        for (int i = 0; i < tabelRiwayat.getRowCount(); i++) {
            if (tabelRiwayat.getValueAt(i, 4).toString().equalsIgnoreCase("Ditolak")) {
                JOptionPane.showMessageDialog(this, "Ada mata kuliah yang ditolak. Silakan cek detail.");
                break; 
            }
        }
    }

    private void cekKunciAkses() {
        try {
            Connection conn = new database().getConnection(); 
            String sql = "SELECT id_krs FROM krs WHERE status_krs = 'terbuka' AND CURRENT_DATE BETWEEN tanggal_mulai AND tanggal_selesai";
            Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery(sql);
            boolean buka = rs.next();
            btnBuatPengajuan.setEnabled(buka);
            btnIsiMatkul.setEnabled(buka);
            btnHapusPengajuan.setEnabled(buka);
            btnLock.setEnabled(buka);
            jLabel1.setText(buka ? "Daftar Sesi KRS yang Sedang Buka" : "Sesi KRS (TUTUP/DILUAR JADWAL)");
        } catch (Exception e) {
            System.out.println("Gagal cek kunci: " + e.getMessage());
        }
    }

    private void aturLebarKolom(javax.swing.JTable tabel) {
        TableColumnModel columnModel = tabel.getColumnModel();
        for (int col = 0; col < tabel.getColumnCount(); col++) {
            columnModel.getColumn(col).setPreferredWidth(100);
        }
    }
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel1 = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        tabelSesiAktif = new javax.swing.JTable();
        btnBuatPengajuan = new javax.swing.JButton();
        jLabel2 = new javax.swing.JLabel();
        jScrollPane2 = new javax.swing.JScrollPane();
        tabelRiwayat = new javax.swing.JTable();
        btnIsiMatkul = new javax.swing.JButton();
        btnHapusPengajuan = new javax.swing.JButton();
        btnBack = new javax.swing.JButton();
        btnLihatDetail = new javax.swing.JButton();
        btnLock = new javax.swing.JButton();
        jLabel3 = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        getContentPane().setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jLabel1.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel1.setText("Daftar Sesi KRS yang Sedang Buka");
        getContentPane().add(jLabel1, new org.netbeans.lib.awtextra.AbsoluteConstraints(110, 70, -1, -1));

        tabelSesiAktif.setModel(new javax.swing.table.DefaultTableModel(
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
        jScrollPane1.setViewportView(tabelSesiAktif);

        getContentPane().add(jScrollPane1, new org.netbeans.lib.awtextra.AbsoluteConstraints(110, 110, 810, 103));

        btnBuatPengajuan.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        btnBuatPengajuan.setText("Buat Pengajuan dari Sesi Terpilih");
        btnBuatPengajuan.setToolTipText("");
        btnBuatPengajuan.addActionListener(this::btnBuatPengajuanActionPerformed);
        getContentPane().add(btnBuatPengajuan, new org.netbeans.lib.awtextra.AbsoluteConstraints(110, 230, -1, -1));

        jLabel2.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel2.setText("Riwayat Pengajuan KRS Saya");
        getContentPane().add(jLabel2, new org.netbeans.lib.awtextra.AbsoluteConstraints(110, 280, -1, -1));

        tabelRiwayat.setModel(new javax.swing.table.DefaultTableModel(
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
        jScrollPane2.setViewportView(tabelRiwayat);

        getContentPane().add(jScrollPane2, new org.netbeans.lib.awtextra.AbsoluteConstraints(110, 310, 810, 130));

        btnIsiMatkul.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        btnIsiMatkul.setText("Pilih Jadwal Matkul");
        btnIsiMatkul.addActionListener(this::btnIsiMatkulActionPerformed);
        getContentPane().add(btnIsiMatkul, new org.netbeans.lib.awtextra.AbsoluteConstraints(590, 450, -1, -1));

        btnHapusPengajuan.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        btnHapusPengajuan.setText("Hapus Pengajuan");
        btnHapusPengajuan.addActionListener(this::btnHapusPengajuanActionPerformed);
        getContentPane().add(btnHapusPengajuan, new org.netbeans.lib.awtextra.AbsoluteConstraints(775, 450, -1, -1));

        btnBack.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        btnBack.setText("Back");
        btnBack.addActionListener(this::btnBackActionPerformed);
        getContentPane().add(btnBack, new org.netbeans.lib.awtextra.AbsoluteConstraints(110, 500, -1, -1));

        btnLihatDetail.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        btnLihatDetail.setText("Lihat Detail");
        btnLihatDetail.addActionListener(this::btnLihatDetailActionPerformed);
        getContentPane().add(btnLihatDetail, new org.netbeans.lib.awtextra.AbsoluteConstraints(110, 450, -1, -1));

        btnLock.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        btnLock.setText("Lock KRS");
        btnLock.addActionListener(this::btnLockActionPerformed);
        getContentPane().add(btnLock, new org.netbeans.lib.awtextra.AbsoluteConstraints(830, 270, -1, -1));

        jLabel3.setIcon(new javax.swing.ImageIcon(getClass().getResource("/desain/Rectangle (right).png"))); // NOI18N
        getContentPane().add(jLabel3, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 0, -1, -1));

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnHapusPengajuanActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnHapusPengajuanActionPerformed
      int baris = tabelRiwayat.getSelectedRow();
        if (baris == -1) return;
        
        String status = tabelRiwayat.getValueAt(baris, 4).toString();
        if (!status.equalsIgnoreCase("Draft") && !status.equalsIgnoreCase("Ditolak")) {
            JOptionPane.showMessageDialog(this, "Hanya status Draft/Ditolak yang bisa dihapus!");
            return;
        }

        // PERUBAHAN DI SINI: Tambahkan judul popup dan parameter YES_NO_OPTION
        int konfirmasi = JOptionPane.showConfirmDialog(this, 
                "Hapus pengajuan ini?", 
                "Konfirmasi Hapus", 
                JOptionPane.YES_NO_OPTION);
                
        if (konfirmasi == JOptionPane.YES_OPTION) {
            try {
                String idPengajuan = tabelRiwayat.getValueAt(baris, 0).toString();
                Connection conn = new database().getConnection();
                conn.createStatement().executeUpdate("DELETE FROM krs_detail WHERE id_pengajuan = " + idPengajuan);
                conn.createStatement().executeUpdate("DELETE FROM pengajuan_krs WHERE id_pengajuan = " + idPengajuan);
                tampilkanRiwayat();
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Gagal: " + e.getMessage());
            }
        }
    }//GEN-LAST:event_btnHapusPengajuanActionPerformed

    private void btnBuatPengajuanActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnBuatPengajuanActionPerformed
       int baris = tabelSesiAktif.getSelectedRow();
        if (baris == -1) return;
        
        String idKrsTerpilih = tabelSesiAktif.getValueAt(baris, 0).toString();
        try {
            Connection conn = new database().getConnection();
            String sqlInsert = "INSERT INTO pengajuan_krs (id_krs, id_mahasiswa, tanggal_pengajuan, status_acc) VALUES (?, ?, CURRENT_DATE, 'Draft')";
            PreparedStatement psInsert = conn.prepareStatement(sqlInsert);
            psInsert.setString(1, idKrsTerpilih);
            psInsert.setInt(2, idMahasiswaAktif);
            psInsert.executeUpdate();
            tampilkanRiwayat();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Gagal: " + e.getMessage());
        }
    }//GEN-LAST:event_btnBuatPengajuanActionPerformed

    private void btnIsiMatkulActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnIsiMatkulActionPerformed
  int baris = tabelRiwayat.getSelectedRow();
    if (baris == -1) {
        JOptionPane.showMessageDialog(this, "Pilih riwayat pengajuan!");
        return;
    }
    
    String status = tabelRiwayat.getValueAt(baris, 4).toString();
    String idPengajuan = tabelRiwayat.getValueAt(baris, 0).toString();
    
    // Logika Reset ke Draft jika status Ditolak
    if (status.equalsIgnoreCase("Ditolak")) {
        try {
            Connection conn = new database().getConnection();
            // Update status pengajuan utama ke 'Draft'
            String sql = "UPDATE pengajuan_krs SET status_acc = 'Draft' WHERE id_pengajuan = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, idPengajuan);
            ps.executeUpdate();
            
            // Opsional: Reset juga status matkul yang tadinya ditolak ke 'Draft'
            String sqlDetail = "UPDATE krs_detail SET status_acc = 'Draft' WHERE id_pengajuan = ? AND status_acc = 'Ditolak'";
            PreparedStatement psDetail = conn.prepareStatement(sqlDetail);
            psDetail.setString(1, idPengajuan);
            psDetail.executeUpdate();
            
            tampilkanRiwayat(); // Refresh tabel setelah update
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Gagal reset status: " + e.getMessage());
        }
    }

    // Buka form Pilih Jadwal
    Pilih_Jadwal formPilih = new Pilih_Jadwal(idPengajuan, 0);
    formPilih.setVisible(true);
    
    // Tambahkan WindowListener untuk refresh tabel saat Pilih_Jadwal ditutup
    formPilih.addWindowListener(new java.awt.event.WindowAdapter() {
        @Override
        public void windowClosed(java.awt.event.WindowEvent e) {
            tampilkanRiwayat();
        }
    });
    }//GEN-LAST:event_btnIsiMatkulActionPerformed

    private void btnBackActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnBackActionPerformed
        new dashboard().setVisible(true); // Panggil langsung
        this.dispose();
    }//GEN-LAST:event_btnBackActionPerformed

    private void btnLihatDetailActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnLihatDetailActionPerformed
    int baris = tabelRiwayat.getSelectedRow();
        if (baris == -1) {
            JOptionPane.showMessageDialog(this, "Pilih data di tabel riwayat!");
            return;
        }

        String idPengajuan = tabelRiwayat.getValueAt(baris, 0).toString();
        String status = tabelRiwayat.getValueAt(baris, 4).toString();
        // ReadOnly jika status bukan Draft atau Ditolak
        boolean isReadOnly = !status.equalsIgnoreCase("Draft") && !status.equalsIgnoreCase("Ditolak");

        Detail_KRS popUp = new Detail_KRS((java.awt.Frame) this.getParent(), true, idPengajuan, isReadOnly);
        popUp.setVisible(true);

        if (popUp.isDataBerubah()) {
            tampilkanRiwayat();
        }
    }//GEN-LAST:event_btnLihatDetailActionPerformed

    private void btnLockActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnLockActionPerformed
        int baris = tabelRiwayat.getSelectedRow();
    if (baris == -1) {
        JOptionPane.showMessageDialog(this, "Silakan pilih pengajuan KRS di tabel bawah yang ingin dikunci!");
        return;
    }
    
    String status = tabelRiwayat.getValueAt(baris, 4).toString();
    
    // Jika statusnya sudah beralih dari Draft, cegah pengerjaan ganda
    if (!status.equalsIgnoreCase("Draft")) {
        JOptionPane.showMessageDialog(this, "KRS ini sudah dikunci sebelumnya atau telah selesai ditinjau!");
        return;
    }
    
    int konfirmasi = JOptionPane.showConfirmDialog(this, 
            "Apakah Anda yakin ingin mengunci KRS ini?\nSetelah dikunci, daftar mata kuliah tidak dapat diubah kembali.", 
            "Konfirmasi Lock KRS", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            
    if (konfirmasi == JOptionPane.YES_OPTION) {
        try {
            String idPengajuan = tabelRiwayat.getValueAt(baris, 0).toString();
            java.sql.Connection conn = new database().getConnection();
            
            // Query untuk mengubah status_acc menjadi 'Ditinjau' (Artinya terkunci & siap direview dosen)
            String sqlUpdate = "UPDATE pengajuan_krs SET status_acc = 'Ditinjau' WHERE id_pengajuan = ?";
            java.sql.PreparedStatement ps = conn.prepareStatement(sqlUpdate);
            ps.setString(1, idPengajuan);
            ps.executeUpdate();
            
            // Tambahan: Kunci semua matkul di dalam KRS agar tidak bisa diubah mahasiswa lagi
                String sqlLockDetail = "UPDATE krs_detail SET status_acc = 'Ditinjau' WHERE id_pengajuan = ? AND status_acc = 'Draft'";
                java.sql.PreparedStatement psLock = conn.prepareStatement(sqlLockDetail);
                psLock.setString(1, idPengajuan);
                psLock.executeUpdate();

                JOptionPane.showMessageDialog(this, "KRS Berhasil Dikunci! Status matkul diubah menjadi Ditinjau.");
                tampilkanRiwayat();
            
  
            
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Gagal mengunci KRS: " + e.getMessage());
        }
    }
    }//GEN-LAST:event_btnLockActionPerformed

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
            LOGGER.log(Level.SEVERE, null, ex);
        }
        java.awt.EventQueue.invokeLater(() -> new Pengajuan_KRS().setVisible(true));
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnBack;
    private javax.swing.JButton btnBuatPengajuan;
    private javax.swing.JButton btnHapusPengajuan;
    private javax.swing.JButton btnIsiMatkul;
    private javax.swing.JButton btnLihatDetail;
    private javax.swing.JButton btnLock;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JTable tabelRiwayat;
    private javax.swing.JTable tabelSesiAktif;
    // End of variables declaration//GEN-END:variables
}
