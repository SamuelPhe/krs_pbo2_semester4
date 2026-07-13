/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package PBO_Project_2;

import javax.swing.table.DefaultTableModel;
import javax.swing.JOptionPane;
import java.sql.*;
import java.util.logging.Logger;
import java.util.logging.Level;
import javax.swing.table.TableColumn;
import java.awt.Component;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;

public class Pilih_Jadwal extends javax.swing.JFrame {

    private String idPengajuanAktif;
    private int semesterMahasiswa = 0;
    private int idProdiMahasiswa = 0;
    private static final Logger LOGGER = Logger.getLogger(Pilih_Jadwal.class.getName());
    private int totalSKSExisting;

    public Pilih_Jadwal(String idPengajuan, int totalSKS) {
        this.idPengajuanAktif = idPengajuan;
        this.totalSKSExisting = totalSKS;
        initComponents();
        this.setLocationRelativeTo(null);
        initData();
    }

    private void initData() {
        ambilSemesterMahasiswa();
        tampilkanJadwalKuliah();
    }

    // --- FUNGSI TUNGGAL UNTUK ATUR LEBAR KOLOM ---
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
        // Khusus kolom "Pilih" (index 0) agar tetap kecil
        tabel.getColumnModel().getColumn(0).setPreferredWidth(50);
        tabel.getColumnModel().getColumn(0).setMaxWidth(50);
    }

    private void tampilkanJadwalKuliah() {
        DefaultTableModel model = new DefaultTableModel() {
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return columnIndex == 0 ? Boolean.class : String.class;
            }
        };
        
        model.addColumn("Pilih"); 
        model.addColumn("ID Jadwal"); 
        model.addColumn("Mata Kuliah");
        model.addColumn("SKS");
        model.addColumn("Kelas");
        model.addColumn("Hari");
        model.addColumn("Jam");
        model.addColumn("Ruang");

        try {
            Connection conn = new database().getConnection();
            String sql = "SELECT j.id_jadwal, m.nama_matkul, m.sks, k.nama_kelas, j.hari, j.jam_mulai, j.jam_selesai, r.nama_ruang " +
                         "FROM jadwal j " +
                         "JOIN matkul m ON j.id_matkul = m.id_matkul " +
                         "JOIN kelas k ON j.kelas = k.id_kelas " +
                         "JOIN ruang r ON j.ruang = r.id_ruang " +
                         "WHERE m.id_prodi = ? AND m.semester = ?";
            
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, this.idProdiMahasiswa); 
            ps.setInt(2, this.semesterMahasiswa); 
            
            ResultSet res = ps.executeQuery();
            while (res.next()) {
                model.addRow(new Object[]{
                    false, res.getString("id_jadwal"), res.getString("nama_matkul"), 
                    res.getInt("sks"), res.getString("nama_kelas"), res.getString("hari"), 
                    res.getString("jam_mulai") + " - " + res.getString("jam_selesai"), res.getString("nama_ruang")
                });
            }
            tabelJadwal.setModel(model);
            
            // Panggil method pengatur lebar kolom (SATU KALI SAJA)
            aturLebarKolom(tabelJadwal);
            
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Gagal memuat jadwal: " + e.getMessage());
        }
    }
    
    private int hitungTotalSKS() {
        int total = 0;
        try {
            java.sql.Connection conn = new database().getConnection();
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
    
    private boolean isMatkulAlreadyTaken(String idPengajuan, String idJadwalBaru) {
        try {
            java.sql.Connection conn = new database().getConnection();
            String sqlGetMatkul = "SELECT id_matkul FROM jadwal WHERE id_jadwal = ?";
            java.sql.PreparedStatement psMatkul = conn.prepareStatement(sqlGetMatkul);
            psMatkul.setString(1, idJadwalBaru);
            java.sql.ResultSet rsMatkul = psMatkul.executeQuery();
            
            if (rsMatkul.next()) {
                int idMatkulBaru = rsMatkul.getInt("id_matkul");
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

    private void ambilSemesterMahasiswa() {
        try {
            java.sql.Connection conn = new database().getConnection();
            String sql = "SELECT id_prodi, semester FROM mahasiswa WHERE nama_mahasiswa = ?";
            java.sql.PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, Session.getNama()); 
            java.sql.ResultSet rs = ps.executeQuery();
            
            if (rs.next()) {
                this.idProdiMahasiswa = rs.getInt("id_prodi");
                this.semesterMahasiswa = rs.getInt("semester");
            }
        } catch (Exception e) {
            System.out.println("Error ambil data filter: " + e.getMessage());
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
        jLabel2 = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        getContentPane().setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jLabel1.setFont(new java.awt.Font("Segoe UI", 1, 24)); // NOI18N
        jLabel1.setText("Pilih Mata Kuliah untuk Pengajuan KRS:");
        getContentPane().add(jLabel1, new org.netbeans.lib.awtextra.AbsoluteConstraints(100, 80, -1, -1));

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

        getContentPane().add(jScrollPane1, new org.netbeans.lib.awtextra.AbsoluteConstraints(100, 130, 830, 287));

        btnTambah.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        btnTambah.setText("Add");
        btnTambah.addActionListener(this::btnTambahActionPerformed);
        getContentPane().add(btnTambah, new org.netbeans.lib.awtextra.AbsoluteConstraints(800, 450, 130, 50));

        btnBack.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        btnBack.setText("Back");
        btnBack.addActionListener(this::btnBackActionPerformed);
        getContentPane().add(btnBack, new org.netbeans.lib.awtextra.AbsoluteConstraints(100, 450, 130, 50));

        jLabel2.setIcon(new javax.swing.ImageIcon(getClass().getResource("/desain/Rectangle (right).png"))); // NOI18N
        getContentPane().add(jLabel2, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 0, -1, -1));

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnTambahActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnTambahActionPerformed
     int totalSKS = hitungTotalSKS(); 
        int maxSKS = 24;
        boolean adaYangDipilih = false;

        try {
            java.sql.Connection conn = new database().getConnection();
            String sqlInsert = "INSERT INTO krs_detail (id_pengajuan, id_jadwal) VALUES (?, ?)";
            java.sql.PreparedStatement psInsert = conn.prepareStatement(sqlInsert);

            for (int i = 0; i < tabelJadwal.getRowCount(); i++) {
                Boolean isChecked = (Boolean) tabelJadwal.getValueAt(i, 0); 
                
                if (isChecked) {
                    adaYangDipilih = true;
                    String idJadwal = tabelJadwal.getValueAt(i, 1).toString();
                    int sksMatkul = Integer.parseInt(tabelJadwal.getValueAt(i, 3).toString());

                    if (isMatkulAlreadyTaken(idPengajuanAktif, idJadwal)) {
                        JOptionPane.showMessageDialog(this, "Matkul " + tabelJadwal.getValueAt(i, 2) + " sudah diambil. Dilewati.");
                        continue;
                    }

                    if (isJadwalBentrok(idPengajuanAktif, idJadwal)) {
                        JOptionPane.showMessageDialog(this, "Matkul " + tabelJadwal.getValueAt(i, 2) + " bentrok jadwal. Dilewati.");
                        continue;
                    }

                    if ((totalSKS + sksMatkul) > maxSKS) {
                        JOptionPane.showMessageDialog(this, "Batas SKS terlampaui saat menambah " + tabelJadwal.getValueAt(i, 2));
                        break; 
                    }

                    psInsert.setString(1, idPengajuanAktif);
                    psInsert.setString(2, idJadwal);
                    psInsert.executeUpdate();
                    
                    totalSKS += sksMatkul; 
                }
            }
            
            if (adaYangDipilih) {
                JOptionPane.showMessageDialog(this, "Proses penambahan selesai.");
                this.dispose();
            } else {
                JOptionPane.showMessageDialog(this, "Tidak ada jadwal yang dipilih!");
            }
            
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
        }
    }//GEN-LAST:event_btnTambahActionPerformed

    private void btnBackActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnBackActionPerformed
     
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
        java.awt.EventQueue.invokeLater(() -> new Pilih_Jadwal("0", 0).setVisible(true));
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
    private javax.swing.JLabel jLabel2;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTable tabelJadwal;
    // End of variables declaration//GEN-END:variables
}
