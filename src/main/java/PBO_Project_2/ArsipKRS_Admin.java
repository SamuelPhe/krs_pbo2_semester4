/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package PBO_Project_2;

import javax.swing.table.DefaultTableModel;
import java.sql.*;
import javax.swing.JOptionPane;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableCellRenderer;
import java.awt.Component;

public class ArsipKRS_Admin extends javax.swing.JFrame {
    private String idMahasiswaFilter = null; // Menyimpan ID jika dipanggil dari Dosen PA
   
    // Constructor Asli (Untuk Admin)
    public ArsipKRS_Admin() {
        initComponents();
        this.setLocationRelativeTo(null);
        loadComboPeriode();
        
        // Panggil fungsi untuk set dropdown ke terbaru, lalu tampilkan datanya
        setFilterKeTerbaru();
        tampilkanData();
    }

    // --- TAMBAHKAN CONSTRUCTOR BARU INI (Untuk Dosen PA) ---
    public ArsipKRS_Admin(String idMhs, String namaMhs) {
        this.idMahasiswaFilter = idMhs; 
        initComponents();
        this.setLocationRelativeTo(null);
        loadComboPeriode();
        
        // Ubah judul Label agar dosen tahu ini data spesifik
        jLabel1.setText("Riwayat KRS Mahasiswa: " + namaMhs);
        
        // Sembunyikan tombol "Cetak Arsip Periode" (karena ini khusus batch Admin)
        btnCetakArsip.setVisible(false);

        // Panggil fungsi untuk set dropdown ke terbaru, lalu tampilkan datanya
        setFilterKeTerbaru();
        tampilkanData();
    }

    private void loadComboPeriode() {
        try {
            Connection conn = new database().getConnection();
            String sql = "SELECT DISTINCT tahun_ajaran FROM krs ORDER BY tahun_ajaran DESC";
            ResultSet rs = conn.createStatement().executeQuery(sql);
            cmbTahun.removeAllItems();
            while(rs.next()) cmbTahun.addItem(rs.getString("tahun_ajaran"));
        } catch (Exception e) { e.printStackTrace(); }
    }

   private void tampilkanData() {
        DefaultTableModel model = new DefaultTableModel(new String[]{"ID Pengajuan", "ID KRS", "Mahasiswa", "Semester", "Tahun", "Status"}, 0);
        tabelArsip.setModel(model);
        aturLebarKolom(tabelArsip);
        try {
            Connection conn = new database().getConnection();
            String sql = "SELECT pk.id_pengajuan, k.id_krs, m.nama_mahasiswa, k.semester, k.tahun_ajaran, pk.status_acc " +
                         "FROM krs k " +
                         "JOIN pengajuan_krs pk ON k.id_krs = pk.id_krs " +
                         "JOIN mahasiswa m ON pk.id_mahasiswa = m.id_mahasiswa " +
                         "WHERE k.tahun_ajaran = ? AND k.semester = ?";
            
            // JIKA DIPANGGIL OLEH DOSEN PA, TAMBAHKAN FILTER ID MAHASISWA
            if (idMahasiswaFilter != null) {
                sql += " AND m.id_mahasiswa = ?";
            }
            
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, cmbTahun.getSelectedItem().toString());
            ps.setString(2, cmbSemester.getSelectedItem().toString());
            
            // Masukkan parameter ke-3 jika filter aktif
            if (idMahasiswaFilter != null) {
                ps.setString(3, idMahasiswaFilter);
            }
            
            ResultSet rs = ps.executeQuery();
            // ... (lanjutan while(rs.next()) sama persis seperti yang kamu punya sebelumnya)
            
            boolean adaData = false;
            while(rs.next()) {
                adaData = true;
                model.addRow(new Object[]{
                    rs.getString("id_pengajuan"), 
                    rs.getString("id_krs"),       
                    rs.getString("nama_mahasiswa"), 
                    rs.getString("semester"), 
                    rs.getString("tahun_ajaran"), 
                    rs.getString("status_acc")
                });
            }
            if (!adaData) {
                JOptionPane.showMessageDialog(this, "Tidak ada data KRS ditemukan.");
            }
        } catch (Exception e) { 
            e.printStackTrace();
        }
    }
   
   private void aturLebarKolom(javax.swing.JTable tabel) {
    TableColumnModel columnModel = tabel.getColumnModel();
    for (int col = 0; col < tabel.getColumnCount(); col++) {
        int width = 70; // Lebar minimal
        for (int row = 0; row < tabel.getRowCount(); row++) {
            TableCellRenderer renderer = tabel.getCellRenderer(row, col);
            Component comp = tabel.prepareRenderer(renderer, row, col);
            width = Math.max(comp.getPreferredSize().width + 15, width);
        }
        // Batasi maksimal 250px agar tabel tetap rapi
        columnModel.getColumn(col).setPreferredWidth(Math.min(width, 250));
    }
}
   
  private void setFilterKeTerbaru() {
        try {
            Connection conn = new database().getConnection();
            String sql;
            PreparedStatement ps;

            // Cek apakah ini mode Dosen PA (ada filter ID) atau mode Admin (tanpa filter)
            if (idMahasiswaFilter != null) {
                // Cari pengajuan terbaru khusus untuk mahasiswa ini
                sql = "SELECT k.tahun_ajaran, k.semester " +
                      "FROM pengajuan_krs pk " +
                      "JOIN krs k ON pk.id_krs = k.id_krs " +
                      "WHERE pk.id_mahasiswa = ? " +
                      "ORDER BY pk.id_pengajuan DESC LIMIT 1";
                ps = conn.prepareStatement(sql);
                ps.setString(1, idMahasiswaFilter);
            } else {
                // Cari pengajuan terbaru dari semua mahasiswa (Admin)
                sql = "SELECT k.tahun_ajaran, k.semester " +
                      "FROM pengajuan_krs pk " +
                      "JOIN krs k ON pk.id_krs = k.id_krs " +
                      "ORDER BY pk.id_pengajuan DESC LIMIT 1";
                ps = conn.prepareStatement(sql);
            }

            ResultSet rs = ps.executeQuery();

            // Jika ada riwayat pengajuan yang ditemukan, atur dropdown-nya
            if (rs.next()) {
                String tahunTerbaru = rs.getString("tahun_ajaran");
                String semesterTerbaru = rs.getString("semester");

                // Ubah pilihan di dropdown secara otomatis sesuai data pengajuan asli
                cmbTahun.setSelectedItem(tahunTerbaru);
                cmbSemester.setSelectedItem(semesterTerbaru);
            }
        } catch (Exception e) {
            e.printStackTrace();
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
        cmbTahun = new javax.swing.JComboBox<>();
        cmbSemester = new javax.swing.JComboBox<>();
        btnTampil = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        tabelArsip = new javax.swing.JTable();
        btnBack = new javax.swing.JButton();
        btnDetail = new javax.swing.JButton();
        btnCetakArsip = new javax.swing.JButton();
        jLabel2 = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        getContentPane().setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jLabel1.setFont(new java.awt.Font("Segoe UI", 1, 24)); // NOI18N
        jLabel1.setText("Filter Periode KRS:");
        getContentPane().add(jLabel1, new org.netbeans.lib.awtextra.AbsoluteConstraints(110, 70, -1, -1));

        cmbTahun.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "2026", "2025", "2024", "2023" }));
        cmbTahun.addActionListener(this::cmbTahunActionPerformed);
        getContentPane().add(cmbTahun, new org.netbeans.lib.awtextra.AbsoluteConstraints(110, 110, 174, -1));

        cmbSemester.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14" }));
        getContentPane().add(cmbSemester, new org.netbeans.lib.awtextra.AbsoluteConstraints(300, 110, 104, -1));

        btnTampil.setText("Tampilkan Data");
        btnTampil.addActionListener(this::btnTampilActionPerformed);
        getContentPane().add(btnTampil, new org.netbeans.lib.awtextra.AbsoluteConstraints(420, 110, -1, -1));

        tabelArsip.setModel(new javax.swing.table.DefaultTableModel(
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
        jScrollPane1.setViewportView(tabelArsip);

        getContentPane().add(jScrollPane1, new org.netbeans.lib.awtextra.AbsoluteConstraints(110, 147, 800, 310));

        btnBack.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        btnBack.setText("Back");
        btnBack.addActionListener(this::btnBackActionPerformed);
        getContentPane().add(btnBack, new org.netbeans.lib.awtextra.AbsoluteConstraints(110, 470, 120, 40));

        btnDetail.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        btnDetail.setText("Lihat Detail");
        btnDetail.addActionListener(this::btnDetailActionPerformed);
        getContentPane().add(btnDetail, new org.netbeans.lib.awtextra.AbsoluteConstraints(770, 470, 140, 40));

        btnCetakArsip.setText("Cetak Arsip Periode");
        btnCetakArsip.addActionListener(this::btnCetakArsipActionPerformed);
        getContentPane().add(btnCetakArsip, new org.netbeans.lib.awtextra.AbsoluteConstraints(770, 110, 140, -1));

        jLabel2.setIcon(new javax.swing.ImageIcon(getClass().getResource("/desain/Rectangle (right).png"))); // NOI18N
        getContentPane().add(jLabel2, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 0, -1, -1));

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnTampilActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnTampilActionPerformed
       tampilkanData();
    }//GEN-LAST:event_btnTampilActionPerformed

    private void btnDetailActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnDetailActionPerformed
   int baris = tabelArsip.getSelectedRow();
    if (baris == -1) {
        JOptionPane.showMessageDialog(this, "Pilih data mahasiswa dulu!");
        return;
    }

    String idPengajuan = tabelArsip.getValueAt(baris, 0).toString();

    // Arahkan ke Detail_KRS
    Detail_KRS dialog = new Detail_KRS((java.awt.Frame) this, true, idPengajuan);
    dialog.setVisible(true);
    }//GEN-LAST:event_btnDetailActionPerformed

    private void btnBackActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnBackActionPerformed
        this.dispose();
    }//GEN-LAST:event_btnBackActionPerformed

    private void btnCetakArsipActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCetakArsipActionPerformed
        try {
            Connection conn = new database().getConnection();
            // Path ke report khusus arsip (Kamu mungkin perlu buat report baru untuk daftar)
            String path = "src/main/java/PBO_Project_2/KRS_Report.jasper";
            
            java.util.HashMap<String, Object> parameter = new java.util.HashMap<>();
            parameter.put("mode", "batch"); // Mode batch untuk cetak banyak
            parameter.put("tahun", cmbTahun.getSelectedItem().toString());
            parameter.put("semester", cmbSemester.getSelectedItem().toString());
            
            // --- TAMBAHAN KODE ---
            // Mengirimkan parameter filter status ke JasperReport
            parameter.put("status_acc_matkul", "Disetujui"); 
            // ---------------------
            
            JasperPrint print = JasperFillManager.fillReport(path, parameter, conn);
            
            net.sf.jasperreports.swing.JRViewer jrViewer = new net.sf.jasperreports.swing.JRViewer(print);
            jrViewer.setZoomRatio(0.50f); 
            
            javax.swing.JFrame frameViewer = new javax.swing.JFrame("Cetak Arsip KRS");
            frameViewer.getContentPane().add(jrViewer);
            frameViewer.setDefaultCloseOperation(javax.swing.JFrame.DISPOSE_ON_CLOSE); // Agar frame utama tidak ikut tertutup
            frameViewer.setExtendedState(javax.swing.JFrame.MAXIMIZED_BOTH);
            frameViewer.setVisible(true);
            
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error Report: " + e.getMessage());
            e.printStackTrace();
        }
    }//GEN-LAST:event_btnCetakArsipActionPerformed

    private void cmbTahunActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cmbTahunActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_cmbTahunActionPerformed

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
        ex.printStackTrace(); // Ganti logger dengan ini agar tidak error
    }

    java.awt.EventQueue.invokeLater(() -> new ArsipKRS_Admin().setVisible(true));
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnBack;
    private javax.swing.JButton btnCetakArsip;
    private javax.swing.JButton btnDetail;
    private javax.swing.JButton btnTampil;
    private javax.swing.JComboBox<String> cmbSemester;
    private javax.swing.JComboBox<String> cmbTahun;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTable tabelArsip;
    // End of variables declaration//GEN-END:variables
}
