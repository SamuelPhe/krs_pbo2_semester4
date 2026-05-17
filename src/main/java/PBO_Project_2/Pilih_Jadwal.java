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

public class Pilih_Jadwal extends javax.swing.JFrame {

    // Variabel global untuk menyimpan data sesi dan transaksi KRS
    private String namaSesi, roleSesi;
    private String idPengajuanAktif;
    private int semesterMahasiswa = 0;
    private int idProdiMahasiswa = 0;
    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(Pilih_Jadwal.class.getName());

    // Konstruktor utama yang dipanggil dari halaman Pengajuan_KRS
    public Pilih_Jadwal(String nama, String role, String idPengajuan) {
        this.namaSesi = nama;
        this.roleSesi = role;
        this.idPengajuanAktif = idPengajuan;
        initComponents();
        this.setLocationRelativeTo(null);
        
        // Ambil data semester mahasiswa terlebih dahulu sebelum memfilter jadwal
        ambilSemesterMahasiswa();
        tampilkanJadwalKuliah();
    }

    // Konstruktor default bawaan NetBeans
    public Pilih_Jadwal() {
        initComponents();
        this.setLocationRelativeTo(null);
    }
    
   
    // ================= FUNGSI 1: AMBIL DATA SEMESTER MAHASISWA =================
    private void ambilSemesterMahasiswa() {
        try {
        java.sql.Connection conn = new database().getConnection();
        // Kita ambil id_prodi DAN semester sekaligus berdasarkan nama mahasiswa yang login
        String sql = "SELECT id_prodi, semester FROM mahasiswa WHERE nama_mahasiswa = ?";
        java.sql.PreparedStatement ps = conn.prepareStatement(sql);
        ps.setString(1, namaSesi); // namaSesi didapat dari data login pembuka form
        java.sql.ResultSet rs = ps.executeQuery();
        
        if (rs.next()) {
            this.idProdiMahasiswa = rs.getInt("id_prodi"); // Menyimpan ID Prodi (Misal: 1)
            this.semesterMahasiswa = rs.getInt("semester"); // Menyimpan Semester (Misal: 4)
        }
    } catch (Exception e) {
        System.out.println("Error ambil data filter mahasiswa: " + e.getMessage());
    }
    }

    // ================= FUNGSI 2: TAMPILKAN JADWAL TERFILTER KETAT =================
    private void tampilkanJadwalKuliah() {
      javax.swing.table.DefaultTableModel model = new javax.swing.table.DefaultTableModel();
    
    // Kolom tabel di form Pilih_Jadwal kamu
    model.addColumn("ID Jadwal");
    model.addColumn("Mata Kuliah");
    model.addColumn("SKS");
    model.addColumn("Kelas");
    model.addColumn("Hari");
    model.addColumn("Jam");
    model.addColumn("Ruang");

    try {
        java.sql.Connection conn = new database().getConnection();
        
        // REVOLUSI QUERY: Kita JOIN 4 tabel sekaligus!
        // j = jadwal, m = matkul, k = kelas, r = ruang (silakan sesuaikan nama tabel/kolom aslimu jika ada typo)
        String sql = "SELECT j.id_jadwal, m.nama_matkul, m.sks, k.nama_kelas, j.hari, j.jam_mulai, j.jam_selesai, r.nama_ruang " +
                     "FROM jadwal j " +
                     "JOIN matkul m ON j.id_matkul = m.id_matkul " +
                     "JOIN kelas k ON j.kelas = k.id_kelas " +   // <-- Ambil NAMA KELAS asli lewat ID Kelas
                     "JOIN ruang r ON j.ruang = r.id_ruang " + // <-- Ambil NAMA RUANG asli lewat ID Ruang
                     "WHERE m.id_prodi = ? AND m.semester = ?";
                     
        java.sql.PreparedStatement ps = conn.prepareStatement(sql);
        ps.setInt(1, this.idProdiMahasiswa); 
        ps.setInt(2, this.semesterMahasiswa); 
        
        java.sql.ResultSet res = ps.executeQuery();
        
        while (res.next()) {
            // Gabungkan jam mulai dan jam selesai agar rapi di satu kolom
            String waktuKuliah = res.getString("jam_mulai") + " - " + res.getString("jam_selesai");
            
            model.addRow(new Object[]{
                res.getString("id_jadwal"),
                res.getString("nama_matkul"),
                res.getInt("sks"),
                res.getString("nama_kelas"),   // Menampilkan nama asli kelas (Misal: "A", "B", "Reguler")
                res.getString("hari"),
                waktuKuliah,
                res.getString("nama_ruang")  // Menampilkan nama asli ruang (Misal: "Lab 1", "Teori 2")
            });
        }
        
        tabelJadwal.setModel(model); 
        // Pasang model data ke tabel
        tabelJadwal.setModel(model); 
        
        // --- PANGGIL SI SATPAM AUTORESIZE DI SINI ---
        aturLebarKolomOtomatis(tabelJadwal);
    } catch (Exception e) {
        System.out.println("Error load jadwal kuliah lengkap: " + e.getMessage());
        javax.swing.JOptionPane.showMessageDialog(this, "Gagal memuat jadwal: " + e.getMessage());
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
        JOptionPane.showMessageDialog(this, "Silakan pilih mata kuliah dari tabel terlebih dahulu!");
        return;
    }

    String idJadwal = tabelJadwal.getValueAt(barisTerpilih, 0).toString();

    try {
        java.sql.Connection conn = new database().getConnection();
        
        // 1. Logika INSERT (Pastikan baris ini sudah aktif dan tidak di-comment)
        String sqlInsert = "INSERT INTO krs_detail (id_pengajuan, id_jadwal) VALUES (?, ?)";
        java.sql.PreparedStatement psInsert = conn.prepareStatement(sqlInsert);
        psInsert.setString(1, idPengajuanAktif); 
        psInsert.setString(2, idJadwal);
        psInsert.executeUpdate();
        
        // 2. Munculkan pesan sukses
        JOptionPane.showMessageDialog(this, "Mata kuliah berhasil ditambahkan ke daftar KRS Anda!");
        
        // 3. TAMBAHKAN BARIS INI (Sangat Penting!):
        this.dispose(); // <--- Menutup form Pilih_Jadwal agar tabel di Pengajuan_KRS otomatis ter-refresh dan muncul list-nya
        
    } catch (Exception e) {
        JOptionPane.showMessageDialog(this, "Gagal menambahkan mata kuliah: " + e.getMessage());
    }
    }//GEN-LAST:event_btnTambahActionPerformed

    private void btnBackActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnBackActionPerformed
       // Kembalikan data namaSesi dan roleSesi agar dashboard asal tidak hilang datanya
        Pengajuan_KRS pk = new Pengajuan_KRS(namaSesi, roleSesi);
        pk.setVisible(true);
        this.dispose();
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
            logger.log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(() -> new Pilih_Jadwal().setVisible(true));
    }
private void aturLebarKolomOtomatis(javax.swing.JTable tabel) {
    // Mengambil model kolom dari tabel
    final javax.swing.table.TableColumnModel columnModel = tabel.getColumnModel();
    
    for (int kolom = 0; kolom < tabel.getColumnCount(); kolom++) {
        int lebarMaksimal = 50; // Lebar minimal kolom (dalam pixel)
        
        // 1. Periksa panjang teks di Header Kolom (Judulnya)
        javax.swing.table.TableCellRenderer headerRenderer = tabel.getTableHeader().getDefaultRenderer();
        Object headerValue = tabel.getColumnName(kolom);
        java.awt.Component headerComp = headerRenderer.getTableCellRendererComponent(tabel, headerValue, false, false, -1, kolom);
        lebarMaksimal = Math.max(headerComp.getPreferredSize().width + 15, lebarMaksimal);
        
        // 2. Periksa panjang teks di setiap baris data pada kolom tersebut
        for (int baris = 0; baris < tabel.getRowCount(); baris++) {
            javax.swing.table.TableCellRenderer renderer = tabel.getCellRenderer(baris, kolom);
            java.awt.Component comp = tabel.prepareRenderer(renderer, baris, kolom);
            // Tambahkan padding 15 pixel agar teks tidak terlalu mepet dengan garis pembatas
            lebarMaksimal = Math.max(comp.getPreferredSize().width + 15, lebarMaksimal);
        }
        
        // 3. Set lebar baru yang paling maksimal ke kolom tersebut
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
