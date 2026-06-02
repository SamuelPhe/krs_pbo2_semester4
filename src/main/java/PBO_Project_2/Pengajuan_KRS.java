/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package PBO_Project_2;

import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;
import java.util.logging.Logger;
import java.util.logging.Level;

public class Pengajuan_KRS extends javax.swing.JFrame {

    // Hapus variabel namaSesi, roleSesi
    private int idMahasiswaAktif = -1; 
    private static final Logger LOGGER = Logger.getLogger(Pengajuan_KRS.class.getName());

    // --- KONSTRUKTOR UTAMA ---
    public Pengajuan_KRS() {
        initComponents();
        this.setLocationRelativeTo(null);
        
        // Panggil fungsi-fungsi langsung
        cariIdMahasiswa();    
        tampilkanSesiAktif(); 
        tampilkanRiwayat();   
    }
    
    // Contoh method di kelas Pengajuan_KRS.java

    // ================= FUNGSI 1: CARI ID MAHASISWA =================
    private void cariIdMahasiswa() {
        try {
            java.sql.Connection conn = new database().getConnection();
            java.sql.PreparedStatement ps = conn.prepareStatement("SELECT id_mahasiswa FROM mahasiswa WHERE nama_mahasiswa = ?");
            ps.setString(1, Session.getNama()); // Ambil nama dari Session
            java.sql.ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                idMahasiswaAktif = rs.getInt("id_mahasiswa");
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Gagal memuat profil mahasiswa: " + e.getMessage());
        }
    }

    // ================= FUNGSI 2: LOAD TABEL SESI AKTIF =================
    private void tampilkanSesiAktif() {
        DefaultTableModel model = new DefaultTableModel();
        model.addColumn("ID Sesi");
        model.addColumn("Program Studi");
        model.addColumn("Semester"); 
        model.addColumn("Tahun Ajaran");
        model.addColumn("Batas Tutup");

        try {
            java.sql.Connection conn = new database().getConnection();
            int idProdiMahasiswa = 0;
            int semesterMahasiswa = 0;
            
            String sqlCariMhs = "SELECT id_prodi, semester FROM mahasiswa WHERE nama_mahasiswa = ?";
            java.sql.PreparedStatement psCari = conn.prepareStatement(sqlCariMhs);
            psCari.setString(1, Session.getNama()); // Ambil nama dari Session
            java.sql.ResultSet rsCari = psCari.executeQuery();
            
            if (rsCari.next()) {
                idProdiMahasiswa = rsCari.getInt("id_prodi");
                semesterMahasiswa = rsCari.getInt("semester");
            }
            
            String sql = "SELECT k.*, p.nama_prodi FROM krs k " +
                         "LEFT JOIN prodi p ON k.id_prodi = p.id_prodi " +
                         "WHERE k.status_krs = 'terbuka' " +
                         "AND CURRENT_DATE >= k.tanggal_mulai AND CURRENT_DATE <= k.tanggal_selesai " +
                         "AND k.id_prodi = ? " +
                         "AND k.semester = ?"; 
                         
            java.sql.PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, idProdiMahasiswa); 
            ps.setInt(2, semesterMahasiswa); 
            java.sql.ResultSet res = ps.executeQuery();
            
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
            
        } catch (Exception e) {
            System.out.println("Error load sesi aktif: " + e.getMessage());
        }
    }
    
    // ================= FUNGSI 3: LOAD TABEL RIWAYAT SAYA =================
    private void tampilkanRiwayat() {
    DefaultTableModel model = new DefaultTableModel();
    model.addColumn("ID Pengajuan");
    model.addColumn("Sesi (Sem/Thn)");
    model.addColumn("Tgl Pengajuan");
    model.addColumn("Dosen PA");
    model.addColumn("Status");

    try {
        java.sql.Connection conn = new database().getConnection();
        String sql = "SELECT pk.id_pengajuan, k.semester, k.tahun_ajaran, pk.tanggal_pengajuan, d.nama_dosen, pk.status_acc " +
                     "FROM pengajuan_krs pk " +
                     "JOIN krs k ON pk.id_krs = k.id_krs " +
                     "JOIN mahasiswa m ON pk.id_mahasiswa = m.id_mahasiswa " +
                     "LEFT JOIN dosen d ON m.id_dosen_pa = d.id_dosen " +
                     "WHERE pk.id_mahasiswa = ?";
        
        java.sql.PreparedStatement ps = conn.prepareStatement(sql);
        ps.setInt(1, idMahasiswaAktif);
        java.sql.ResultSet res = ps.executeQuery();
        
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
        checkRevisi(); // Panggil ini setelah tabel dimuat
    } catch (Exception e) {
        System.out.println("Error load riwayat: " + e.getMessage());
    }
}
    
   private void checkRevisi() {
    for (int i = 0; i < tabelRiwayat.getRowCount(); i++) {
        String status = tabelRiwayat.getValueAt(i, 4).toString();
        
        if (status.equalsIgnoreCase("Ditolak")) {
            String idPengajuan = tabelRiwayat.getValueAt(i, 0).toString();
            // Panggil method dari database.java
            String catatan = new database().getCatatanRevisi(idPengajuan); 
            
            JOptionPane.showMessageDialog(this, 
                "Perhatian! KRS Anda ditolak.\nAlasan: " + (catatan == null || catatan.isEmpty() ? "Tidak ada catatan" : catatan), 
                "Status KRS", JOptionPane.WARNING_MESSAGE);
            
            // Kita break agar popup tidak muncul berkali-kali jika ada banyak data
            break; 
        }
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

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jLabel1.setText("Daftar Sesi KRS yang Sedang Buka");

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

        btnBuatPengajuan.setText("Buat Pengajuan dari Sesi Terpilih");
        btnBuatPengajuan.setToolTipText("");
        btnBuatPengajuan.addActionListener(this::btnBuatPengajuanActionPerformed);

        jLabel2.setText("Riwayat Pengajuan KRS Saya");

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

        btnIsiMatkul.setText("Pilih Jadwal Matkul");
        btnIsiMatkul.addActionListener(this::btnIsiMatkulActionPerformed);

        btnHapusPengajuan.setText("Hapus Pengajuan");
        btnHapusPengajuan.addActionListener(this::btnHapusPengajuanActionPerformed);

        btnBack.setText("Back");
        btnBack.addActionListener(this::btnBackActionPerformed);

        btnLihatDetail.setText("Lihat Detail");
        btnLihatDetail.addActionListener(this::btnLihatDetailActionPerformed);

        btnLock.setText("Lock KRS");
        btnLock.addActionListener(this::btnLockActionPerformed);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(14, 14, 14)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(btnBack)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                            .addComponent(btnLihatDetail)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                            .addComponent(btnIsiMatkul)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(btnHapusPengajuan))
                        .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 497, Short.MAX_VALUE)
                        .addComponent(jScrollPane1)
                        .addComponent(jLabel1)
                        .addGroup(layout.createSequentialGroup()
                            .addComponent(jLabel2)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(btnLock))
                        .addComponent(btnBuatPengajuan)))
                .addContainerGap(20, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(18, 18, 18)
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 103, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(btnBuatPengajuan)
                .addGap(37, 37, 37)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(btnLock))
                .addGap(18, 18, 18)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 118, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(btnHapusPengajuan)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(btnLihatDetail)
                        .addComponent(btnIsiMatkul)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 24, Short.MAX_VALUE)
                .addComponent(btnBack)
                .addGap(24, 24, 24))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnHapusPengajuanActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnHapusPengajuanActionPerformed
        int baris = tabelRiwayat.getSelectedRow();
    if (baris == -1) {
        JOptionPane.showMessageDialog(this, "Pilih riwayat pengajuan di tabel!");
        return;
    }
    
    String status = tabelRiwayat.getValueAt(baris, 4).toString();
    // IZINKAN jika Draft ATAU Ditolak
    if (!status.equalsIgnoreCase("Draft") && !status.equalsIgnoreCase("Ditolak")) {
        JOptionPane.showMessageDialog(this, "Hanya status Draft atau Ditolak yang bisa dihapus!");
        return;
    }

    int konfirmasi = JOptionPane.showConfirmDialog(this, "Yakin ingin menghapus?", "Konfirmasi", JOptionPane.YES_NO_OPTION);
    if (konfirmasi == JOptionPane.YES_OPTION) {
        try {
            String idPengajuan = tabelRiwayat.getValueAt(baris, 0).toString();
            java.sql.Connection conn = new database().getConnection();
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
        if (baris == -1) {
            JOptionPane.showMessageDialog(this, "Silakan klik/pilih sesi KRS di tabel atas terlebih dahulu!");
            return;
        }
        
        if (idMahasiswaAktif == -1) {
            JOptionPane.showMessageDialog(this, "Error: Data Mahasiswa tidak ditemukan di sistem!");
            return;
        }

        String idKrsTerpilih = tabelSesiAktif.getValueAt(baris, 0).toString();

        try {
            java.sql.Connection conn = new database().getConnection();
            
            java.sql.PreparedStatement psCek = conn.prepareStatement("SELECT id_pengajuan FROM pengajuan_krs WHERE id_krs = ? AND id_mahasiswa = ?");
            psCek.setString(1, idKrsTerpilih);
            psCek.setInt(2, idMahasiswaAktif);
            java.sql.ResultSet rsCek = psCek.executeQuery();
            
            if (rsCek.next()) {
                JOptionPane.showMessageDialog(this, "Anda sudah membuat pengajuan untuk sesi ini! Silakan cek tabel riwayat di bawah.");
                return;
            }
            
            String sqlInsert = "INSERT INTO pengajuan_krs (id_krs, id_mahasiswa, tanggal_pengajuan, status_acc) VALUES (?, ?, CURRENT_DATE, 'Draft')";
            java.sql.PreparedStatement psInsert = conn.prepareStatement(sqlInsert);
            psInsert.setString(1, idKrsTerpilih);
            psInsert.setInt(2, idMahasiswaAktif);
            psInsert.executeUpdate();
            
            JOptionPane.showMessageDialog(this, "Berhasil! Data formulir pengajuan KRS Anda telah dibuat. Silakan pilih jadwal mata kuliah pada tabel riwayat di bawah.");
            tampilkanRiwayat(); 
            
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Gagal membuat pengajuan: " + e.getMessage());
        }
    }//GEN-LAST:event_btnBuatPengajuanActionPerformed

    private void btnIsiMatkulActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnIsiMatkulActionPerformed
   int baris = tabelRiwayat.getSelectedRow();
    if (baris == -1) {
        JOptionPane.showMessageDialog(this, "Pilih riwayat pengajuan!");
        return;
    }
    
    String status = tabelRiwayat.getValueAt(baris, 4).toString();
    if (!status.equalsIgnoreCase("Draft") && !status.equalsIgnoreCase("Ditolak")) {
        JOptionPane.showMessageDialog(this, "KRS sudah dikunci/disetujui!");
        return;
    }
    
    String idPengajuan = tabelRiwayat.getValueAt(baris, 0).toString();
    
    // Jika Ditolak, ubah otomatis jadi Draft agar bisa diedit
    if (status.equalsIgnoreCase("Ditolak")) {
        try {
            java.sql.Connection conn = new database().getConnection();
            conn.createStatement().executeUpdate("UPDATE pengajuan_krs SET status_acc = 'Draft' WHERE id_pengajuan = " + idPengajuan);
        } catch (Exception e) {
            System.out.println("Gagal reset status: " + e.getMessage());
        }
    }

    // Buka form Pilih Jadwal
    new Pilih_Jadwal(idPengajuan, 0).setVisible(true);
    }//GEN-LAST:event_btnIsiMatkulActionPerformed

    private void btnBackActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnBackActionPerformed
        new dashboard().setVisible(true); // Panggil langsung
        this.dispose();
    }//GEN-LAST:event_btnBackActionPerformed

    private void btnLihatDetailActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnLihatDetailActionPerformed
       int baris = tabelRiwayat.getSelectedRow();
    if (baris == -1) {
        JOptionPane.showMessageDialog(this, "Silakan pilih salah satu riwayat pengajuan di tabel bawah terlebih dahulu!");
        return;
    }
    
    // Ambil ID Pengajuan dari kolom indeks 0 (Kolom pertama di tabel riwayat kamu)
    String idPengajuanTerpilih = tabelRiwayat.getValueAt(baris, 0).toString();
    
    // Lempar ID-nya ke constructor Detail_KRS
    Detail_KRS popUp = new Detail_KRS(this, true, idPengajuanTerpilih);
    popUp.setVisible(true);
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
            
            JOptionPane.showMessageDialog(this, "KRS Berhasil Dikunci! Formulir pengajuan siap direview oleh Dosen PA dan Kaprodi.");
            
            tampilkanRiwayat(); // Refresh tabel biar status berubah dari 'Draft' menjadi 'Ditinjau'
            
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
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JTable tabelRiwayat;
    private javax.swing.JTable tabelSesiAktif;
    // End of variables declaration//GEN-END:variables
}
