/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package PBO_Project_2;

import javax.swing.table.DefaultTableModel;
import javax.swing.JOptionPane;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.logging.Logger;
import java.util.logging.Level;

public class Pilih_Jadwal extends javax.swing.JFrame {

    private String idPengajuanAktif;
    private int semesterMahasiswa = 0;
    private int idProdiMahasiswa = 0;
    private static final Logger LOGGER = Logger.getLogger(Pilih_Jadwal.class.getName());
    private int totalSKSExisting; // Simpan di variabel class

// 1. Buat method ini
private void initData() {
    ambilSemesterMahasiswa();
    tampilkanJadwalKuliah();
}

// 2. Konstruktor 1
public Pilih_Jadwal(String idPengajuan, int totalSKS) {
    this.idPengajuanAktif = idPengajuan;
    this.totalSKSExisting = totalSKS;
    initComponents();
    this.setLocationRelativeTo(null);
    initData(); // Panggil di sini
}

// 3. Konstruktor 2 (jika masih dipakai)
public Pilih_Jadwal(String idPengajuan) {
    this.idPengajuanAktif = idPengajuan;
    initComponents();
    this.setLocationRelativeTo(null);
    initData(); // Panggil di sini
}
    
    // ================= FUNGSI 1: AMBIL DATA SEMESTER =================
    private void ambilSemesterMahasiswa() {
        try {
            java.sql.Connection conn = new database().getConnection();
            String sql = "SELECT id_prodi, semester FROM mahasiswa WHERE nama_mahasiswa = ?";
            java.sql.PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, Session.getNama()); // Ambil langsung dari Session
            java.sql.ResultSet rs = ps.executeQuery();
            
            if (rs.next()) {
                this.idProdiMahasiswa = rs.getInt("id_prodi");
                this.semesterMahasiswa = rs.getInt("semester");
            }
        } catch (Exception e) {
            System.out.println("Error ambil data filter: " + e.getMessage());
        }
    }
    
    private int hitungTotalSKS() {
    int total = 0;
    try {
        java.sql.Connection conn = new database().getConnection();
        // Query untuk menjumlahkan SKS dari matkul yang sudah diambil di pengajuan ini
        String sql = "SELECT SUM(m.sks) as total FROM krs_detail kd " +
                     "JOIN jadwal j ON kd.id_jadwal = j.id_jadwal " +
                     "JOIN matkul m ON j.id_matkul = m.id_matkul " +
                     "WHERE kd.id_pengajuan = ?";
        java.sql.PreparedStatement ps = conn.prepareStatement(sql);
        ps.setString(1, this.idPengajuanAktif);
        java.sql.ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            total = rs.getInt("total");
        }
    } catch (Exception e) {
        System.out.println("Error hitung SKS: " + e.getMessage());
    }
    return total;
}
    
    // Fungsi Cek: Apakah Matkul sudah diambil (duplikat)?
private boolean isMatkulAlreadyTaken(String idPengajuan, String idJadwalBaru) {
    try {
        java.sql.Connection conn = new database().getConnection();
        // Ambil id_matkul dari jadwal yang baru dipilih
        String sqlGetMatkul = "SELECT id_matkul FROM jadwal WHERE id_jadwal = ?";
        java.sql.PreparedStatement psMatkul = conn.prepareStatement(sqlGetMatkul);
        psMatkul.setString(1, idJadwalBaru);
        java.sql.ResultSet rsMatkul = psMatkul.executeQuery();
        
        if (rsMatkul.next()) {
            int idMatkulBaru = rsMatkul.getInt("id_matkul");
            // Cek apakah id_matkul ini sudah ada di pengajuan ini
            String sqlCek = "SELECT kd.id_detail FROM krs_detail kd JOIN jadwal j ON kd.id_jadwal = j.id_jadwal " +
                            "WHERE kd.id_pengajuan = ? AND j.id_matkul = ?";
            java.sql.PreparedStatement psCek = conn.prepareStatement(sqlCek);
            psCek.setString(1, idPengajuan);
            psCek.setInt(2, idMatkulBaru);
            return psCek.executeQuery().next();
        }
    } catch (Exception e) {
        System.out.println("Error cek duplikat: " + e.getMessage());
    }
    return false;
}

// Fungsi Cek: Apakah Jadwal bentrok?
private boolean isJadwalBentrok(String idPengajuan, String idJadwalBaru) {
    try {
        java.sql.Connection conn = new database().getConnection();
        String sqlDetail = "SELECT hari, jam_mulai, jam_selesai FROM jadwal WHERE id_jadwal = ?";
        java.sql.PreparedStatement psDetail = conn.prepareStatement(sqlDetail);
        psDetail.setString(1, idJadwalBaru);
        java.sql.ResultSet rsDetail = psDetail.executeQuery();
        
        if (rsDetail.next()) {
            String hariBaru = rsDetail.getString("hari");
            java.sql.Time mulaiBaru = rsDetail.getTime("jam_mulai");
            java.sql.Time selesaiBaru = rsDetail.getTime("jam_selesai");
            
            // Logika bentrok: Hari sama DAN waktu beririsan
            String sqlCek = "SELECT j.id_jadwal FROM krs_detail kd " +
                            "JOIN jadwal j ON kd.id_jadwal = j.id_jadwal " +
                            "WHERE kd.id_pengajuan = ? AND j.hari = ? " +
                            "AND (j.jam_mulai < ? AND j.jam_selesai > ?)";
            
            java.sql.PreparedStatement psCek = conn.prepareStatement(sqlCek);
            psCek.setString(1, idPengajuan);
            psCek.setString(2, hariBaru);
            psCek.setTime(3, selesaiBaru);
            psCek.setTime(4, mulaiBaru);
            
            java.sql.ResultSet rsCek = psCek.executeQuery();
            return rsCek.next(); 
        }
    } catch (Exception e) {
        System.out.println("Error cek bentrok: " + e.getMessage());
    }
    return false;
}
    
    // ================= FUNGSI 2: TAMPILKAN JADWAL =================
    private void tampilkanJadwalKuliah() {
        DefaultTableModel model = new DefaultTableModel();
        model.addColumn("ID Jadwal");
        model.addColumn("Mata Kuliah");
        model.addColumn("SKS");
        model.addColumn("Kelas");
        model.addColumn("Hari");
        model.addColumn("Jam");
        model.addColumn("Ruang");

        try {
            java.sql.Connection conn = new database().getConnection();
            String sql = "SELECT j.id_jadwal, m.nama_matkul, m.sks, k.nama_kelas, j.hari, j.jam_mulai, j.jam_selesai, r.nama_ruang " +
                         "FROM jadwal j " +
                         "JOIN matkul m ON j.id_matkul = m.id_matkul " +
                         "JOIN kelas k ON j.kelas = k.id_kelas " +
                         "JOIN ruang r ON j.ruang = r.id_ruang " +
                         "WHERE m.id_prodi = ? AND m.semester = ?";
                         
            java.sql.PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, this.idProdiMahasiswa); 
            ps.setInt(2, this.semesterMahasiswa); 
            
            java.sql.ResultSet res = ps.executeQuery();
            
            while (res.next()) {
                String waktuKuliah = res.getString("jam_mulai") + " - " + res.getString("jam_selesai");
                model.addRow(new Object[]{
                    res.getString("id_jadwal"),
                    res.getString("nama_matkul"),
                    res.getInt("sks"),
                    res.getString("nama_kelas"),
                    res.getString("hari"),
                    waktuKuliah,
                    res.getString("nama_ruang")
                });
            }
            tabelJadwal.setModel(model); 
            aturLebarKolomOtomatis(tabelJadwal);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Gagal memuat jadwal: " + e.getMessage());
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
        tabelJadwal = new javax.swing.JTable();
        btnTambah = new javax.swing.JButton();
        btnBack = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jLabel1.setText("Pilih Mata Kuliah untuk Pengajuan KRS:");

        tabelJadwal.setModel(new javax.swing.table.DefaultTableModel(
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
        jScrollPane1.setViewportView(tabelJadwal);

        btnTambah.setText("Add");
        btnTambah.addActionListener(this::btnTambahActionPerformed);

        btnBack.setText("Back");
        btnBack.addActionListener(this::btnBackActionPerformed);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(17, 17, 17)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(btnBack, javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jLabel1)
                                .addGap(307, 307, 307)))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 91, Short.MAX_VALUE)
                        .addComponent(btnTambah))
                    .addComponent(jScrollPane1))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 287, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnTambah)
                    .addComponent(btnBack))
                .addContainerGap(15, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnTambahActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnTambahActionPerformed
     int barisTerpilih = tabelJadwal.getSelectedRow();
    if (barisTerpilih == -1) {
        JOptionPane.showMessageDialog(this, "Pilih mata kuliah terlebih dahulu!");
        return;
    }

    String idJadwal = tabelJadwal.getValueAt(barisTerpilih, 0).toString();

    // 1. VALIDASI DUPLIKAT MATKUL
    if (isMatkulAlreadyTaken(idPengajuanAktif, idJadwal)) {
        JOptionPane.showMessageDialog(this, "Gagal! Mata kuliah ini sudah Anda ambil.");
        return;
    }

    // 2. VALIDASI JADWAL BENTROK
    if (isJadwalBentrok(idPengajuanAktif, idJadwal)) {
        JOptionPane.showMessageDialog(this, "Gagal! Jadwal ini bentrok dengan mata kuliah lain.");
        return;
    }

    // 3. VALIDASI SKS
    int maxSKS = 24;
    int totalSKS = hitungTotalSKS();
    int sksBaru = Integer.parseInt(tabelJadwal.getValueAt(barisTerpilih, 2).toString());
    
    if ((totalSKS + sksBaru) > maxSKS) {
        JOptionPane.showMessageDialog(this, "Gagal! Total SKS (" + (totalSKS + sksBaru) + ") melebihi batas maksimal (" + maxSKS + " SKS).");
        return; 
    }

    // 4. INSERT KE DATABASE
    try {
        java.sql.Connection conn = new database().getConnection();
        String sqlInsert = "INSERT INTO krs_detail (id_pengajuan, id_jadwal) VALUES (?, ?)";
        java.sql.PreparedStatement psInsert = conn.prepareStatement(sqlInsert);
        psInsert.setString(1, idPengajuanAktif); 
        psInsert.setString(2, idJadwal);
        psInsert.executeUpdate();
        
        JOptionPane.showMessageDialog(this, "Berhasil ditambahkan!");
        this.dispose(); 
    } catch (Exception e) {
        JOptionPane.showMessageDialog(this, "Gagal: " + e.getMessage());
    }
    }//GEN-LAST:event_btnTambahActionPerformed

    private void btnBackActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnBackActionPerformed
     new Pengajuan_KRS().setVisible(true); // Panggil langsung
        this.dispose();
    }//GEN-LAST:event_btnBackActionPerformed

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
        java.awt.EventQueue.invokeLater(() -> new Pilih_Jadwal("0").setVisible(true));
    }
    
private void aturLebarKolomOtomatis(javax.swing.JTable tabel) {
   final javax.swing.table.TableColumnModel columnModel = tabel.getColumnModel();
        for (int kolom = 0; kolom < tabel.getColumnCount(); kolom++) {
            int lebarMaksimal = 50;
            javax.swing.table.TableCellRenderer headerRenderer = tabel.getTableHeader().getDefaultRenderer();
            Object headerValue = tabel.getColumnName(kolom);
            java.awt.Component headerComp = headerRenderer.getTableCellRendererComponent(tabel, headerValue, false, false, -1, kolom);
            lebarMaksimal = Math.max(headerComp.getPreferredSize().width + 15, lebarMaksimal);
            for (int baris = 0; baris < tabel.getRowCount(); baris++) {
                javax.swing.table.TableCellRenderer renderer = tabel.getCellRenderer(baris, kolom);
                java.awt.Component comp = tabel.prepareRenderer(renderer, baris, kolom);
                lebarMaksimal = Math.max(comp.getPreferredSize().width + 15, lebarMaksimal);
            }
            columnModel.getColumn(kolom).setPreferredWidth(lebarMaksimal);
        }
}
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnBack;
    private javax.swing.JButton btnTambah;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTable tabelJadwal;
    // End of variables declaration//GEN-END:variables
}
