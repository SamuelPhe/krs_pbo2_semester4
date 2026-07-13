/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JDialog.java to edit this template
 */
package PBO_Project_2;

import javax.swing.table.DefaultTableModel;
import javax.swing.JOptionPane;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.view.JasperViewer;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableCellRenderer;
import java.awt.Component;

public class Detail_KRS extends javax.swing.JDialog {

    private String idPengajuanAktif;
    private java.awt.Component parentForm;
    private boolean forceReadOnly = false; // FLAG BARU: Untuk mematikan checkbox & tombol
    private boolean dataBerubah = false;

    // --- CONSTRUCTOR BARU (Disarankan untuk dipanggil dari form lain) ---
    public Detail_KRS(java.awt.Frame parent, boolean modal, String id, boolean readOnly) {
        super(parent, modal);
        this.parentForm = parent;
        this.idPengajuanAktif = id;
        this.forceReadOnly = readOnly; // Set status read-only
        initComponents();
        this.setLocationRelativeTo(null);
        
        aturVisibilitasTombol();
        tampilkanDetailKRS();
    }

    // --- CONSTRUCTOR LAMA (Sebagai fallback agar form lama tidak error) ---
    public Detail_KRS(java.awt.Frame parent, boolean modal, String id) {
        super(parent, modal);
        this.parentForm = parent;
        this.idPengajuanAktif = id;
        
        // Deteksi otomatis jika dipanggil pakai constructor lama
        if (parent instanceof DataMahasiswaBimbingan || parent instanceof ArsipKRS_Admin) {
            this.forceReadOnly = true; 
        } else {
            this.forceReadOnly = false;
        }
        
        initComponents();
        this.setLocationRelativeTo(null);
        
        aturVisibilitasTombol();
        tampilkanDetailKRS();
    }

    public Detail_KRS(java.awt.Frame parent, boolean modal) {
        super(parent, modal);
        initComponents();
        this.setLocationRelativeTo(null);
    }
    
    // Method khusus untuk mengatur tombol
    private void aturVisibilitasTombol() {
        if (this.forceReadOnly) {
            btnAcc.setVisible(false);
            btnTolak.setVisible(false);
            btnSelesai.setVisible(false);
            btnHapus.setVisible(false); // Sembunyikan kalau mode read-only
        } else {
            String role = Session.getRole();
            boolean isDosen = role != null && (role.equalsIgnoreCase("dosen") || role.equalsIgnoreCase("kaprodi"));
            boolean isMahasiswa = role != null && role.equalsIgnoreCase("mahasiswa"); // Cek Mahasiswa
            
            btnAcc.setVisible(isDosen);
            btnTolak.setVisible(isDosen);
            btnSelesai.setVisible(isDosen);
            
            // Tampilkan tombol hapus HANYA untuk mahasiswa
            btnHapus.setVisible(isMahasiswa); 
        }
    }

    private void tampilkanDetailKRS() {
        // Gunakan flag forceReadOnly untuk menentukan header dan kolom
        boolean isModeBacaSaja = this.forceReadOnly;
        
        String[] columns;
        if (isModeBacaSaja) {
            columns = new String[]{"ID Detail", "Mata Kuliah", "SKS", "Kelas", "Hari", "Jam", "Ruang", "Status", "Catatan"};
        } else {
            columns = new String[]{"Pilih", "ID Detail", "Mata Kuliah", "SKS", "Kelas", "Hari", "Jam", "Ruang", "Status", "Catatan"};
        }

        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public Class<?> getColumnClass(int column) {
                // Kolom checkbox (indeks 0) hanya ada jika BUKAN mode baca saja
                return (!isModeBacaSaja && column == 0) ? Boolean.class : String.class;
            }
        };
        
        jTable1.setModel(model);
        aturLebarKolom(jTable1);

        try {
            Connection conn = new database().getConnection();
            String sql = "SELECT kd.id_detail, m.nama_matkul, m.sks, k.nama_kelas, j.hari, j.jam_mulai, j.jam_selesai, r.nama_ruang, kd.status_acc, kd.catatan_revisi " +
                         "FROM krs_detail kd " +
                         "JOIN jadwal j ON kd.id_jadwal = j.id_jadwal " +
                         "JOIN matkul m ON j.id_matkul = m.id_matkul " +
                         "LEFT JOIN kelas k ON j.kelas = k.id_kelas " +
                         "LEFT JOIN ruang r ON j.ruang = r.id_ruang " +
                         "WHERE kd.id_pengajuan = ?";

            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, idPengajuanAktif.trim());
            ResultSet res = ps.executeQuery();

            while (res.next()) {
                String waktu = res.getString("jam_mulai") + " - " + res.getString("jam_selesai");
                Object[] row;
                
                if (isModeBacaSaja) {
                    // Tanpa kolom Checkbox (index 0 adalah ID Detail)
                    row = new Object[]{res.getString("id_detail"), res.getString("nama_matkul"), res.getInt("sks"), res.getString("nama_kelas"), res.getString("hari"), waktu, res.getString("nama_ruang"), res.getString("status_acc"), (res.getString("catatan_revisi") == null ? "-" : res.getString("catatan_revisi"))};
                } else {
                    // Dengan kolom Checkbox (index 0 adalah false)
                    row = new Object[]{false, res.getString("id_detail"), res.getString("nama_matkul"), res.getInt("sks"), res.getString("nama_kelas"), res.getString("hari"), waktu, res.getString("nama_ruang"), res.getString("status_acc"), (res.getString("catatan_revisi") == null ? "-" : res.getString("catatan_revisi"))};
                }
                model.addRow(row);
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "GAGAL LOAD DATA: " + e.getMessage());
        }
    }
   
    public void setDetailKRS(String id) {
        this.idPengajuanAktif = id;
        tampilkanDetailKRS(); 
    }
    
    private void aturLebarKolom(javax.swing.JTable tabel) {
        TableColumnModel columnModel = tabel.getColumnModel();
        for (int col = 0; col < tabel.getColumnCount(); col++) {
            int width = 60; // Lebar minimal
            for (int row = 0; row < tabel.getRowCount(); row++) {
                TableCellRenderer renderer = tabel.getCellRenderer(row, col);
                Component comp = tabel.prepareRenderer(renderer, row, col);
                width = Math.max(comp.getPreferredSize().width + 10, width);
            }
            columnModel.getColumn(col).setPreferredWidth(Math.min(width, 250));
        }
    }
    
    // Panggil method ini setiap kali ada operasi yang mengubah database
    // (misal di tombol ACC, tombol Revisi, atau tombol Hapus Matkul)
    private void setAdaPerubahan() {
        this.dataBerubah = true;
    }

    // Buat public agar bisa diakses oleh form induk (Pengajuan_KRS)
    public boolean isDataBerubah() {
        return dataBerubah;
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
        tabelDetail = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();
        Tutup = new javax.swing.JButton();
        btnAcc = new javax.swing.JButton();
        btnTolak = new javax.swing.JButton();
        btnSelesai = new javax.swing.JButton();
        btnCetakKRS = new javax.swing.JButton();
        btnHapus = new javax.swing.JButton();
        jLabel2 = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        getContentPane().setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jLabel1.setFont(new java.awt.Font("Segoe UI", 1, 24)); // NOI18N
        jLabel1.setText("Daftar Mata Kuliah yang Diambil:");
        getContentPane().add(jLabel1, new org.netbeans.lib.awtextra.AbsoluteConstraints(100, 70, -1, -1));

        jTable1.setModel(new javax.swing.table.DefaultTableModel(
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
        tabelDetail.setViewportView(jTable1);

        getContentPane().add(tabelDetail, new org.netbeans.lib.awtextra.AbsoluteConstraints(100, 110, 830, 300));

        Tutup.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        Tutup.setText("Back");
        Tutup.addActionListener(this::TutupActionPerformed);
        getContentPane().add(Tutup, new org.netbeans.lib.awtextra.AbsoluteConstraints(100, 470, 110, 40));

        btnAcc.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        btnAcc.setText("Setujui");
        btnAcc.addActionListener(this::btnAccActionPerformed);
        getContentPane().add(btnAcc, new org.netbeans.lib.awtextra.AbsoluteConstraints(100, 420, 130, -1));

        btnTolak.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        btnTolak.setText("Revisi");
        btnTolak.addActionListener(this::btnTolakActionPerformed);
        getContentPane().add(btnTolak, new org.netbeans.lib.awtextra.AbsoluteConstraints(240, 420, 130, -1));

        btnSelesai.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        btnSelesai.setText("Selesai");
        btnSelesai.addActionListener(this::btnSelesaiActionPerformed);
        getContentPane().add(btnSelesai, new org.netbeans.lib.awtextra.AbsoluteConstraints(790, 470, 140, 40));

        btnCetakKRS.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        btnCetakKRS.setText("Cetak KRS");
        btnCetakKRS.addActionListener(this::btnCetakKRSActionPerformed);
        getContentPane().add(btnCetakKRS, new org.netbeans.lib.awtextra.AbsoluteConstraints(800, 70, 130, -1));

        btnHapus.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        btnHapus.setText("Hapus Matkul");
        btnHapus.addActionListener(this::btnHapusActionPerformed);
        getContentPane().add(btnHapus, new org.netbeans.lib.awtextra.AbsoluteConstraints(770, 420, 160, -1));

        jLabel2.setIcon(new javax.swing.ImageIcon(getClass().getResource("/desain/Rectangle (right).png"))); // NOI18N
        getContentPane().add(jLabel2, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 0, -1, 600));

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void TutupActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_TutupActionPerformed
                                         
        this.dispose(); // Menutup jendela dialog pop-up saat tombol klik close
    
    }//GEN-LAST:event_TutupActionPerformed

    private void btnAccActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnAccActionPerformed
   try {
            Connection conn = new database().getConnection();
            boolean adaYgDicentang = false;

            for (int i = 0; i < jTable1.getRowCount(); i++) {
                Boolean isChecked = (Boolean) jTable1.getValueAt(i, 0);
                if (isChecked != null && isChecked) {
                    adaYgDicentang = true;
                    String idDetail = jTable1.getValueAt(i, 1).toString(); 
                    
                    String sql = "UPDATE krs_detail SET status_acc = 'Disetujui', catatan_revisi = '' WHERE id_detail = ?";
                    PreparedStatement ps = conn.prepareStatement(sql);
                    ps.setString(1, idDetail);
                    ps.executeUpdate();
                }
            }
            
            if (!adaYgDicentang) {
                JOptionPane.showMessageDialog(this, "Centang dulu mata kuliah di tabel!");
                return;
            }
            
            tampilkanDetailKRS(); 
            JOptionPane.showMessageDialog(this, "Matkul yang dicentang berhasil Disetujui.");
        } catch (Exception e) { 
            JOptionPane.showMessageDialog(this, "Gagal: " + e.getMessage()); 
        }
    }//GEN-LAST:event_btnAccActionPerformed

    private void btnTolakActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnTolakActionPerformed
   try {
            boolean adaYgDicentang = false;
            for (int i = 0; i < jTable1.getRowCount(); i++) {
                Boolean isChecked = (Boolean) jTable1.getValueAt(i, 0);
                if (isChecked != null && isChecked) { adaYgDicentang = true; break; }
            }

            if (!adaYgDicentang) {
                JOptionPane.showMessageDialog(this, "Centang dulu mata kuliah di tabel yang ingin direvisi!");
                return;
            }

            String catatan = JOptionPane.showInputDialog(this, "Masukkan alasan penolakan untuk matkul yang dicentang:");
            if (catatan == null || catatan.trim().isEmpty()) return;

            Connection conn = new database().getConnection();
            for (int i = 0; i < jTable1.getRowCount(); i++) {
                Boolean isChecked = (Boolean) jTable1.getValueAt(i, 0);
                if (isChecked != null && isChecked) {
                    String idDetail = jTable1.getValueAt(i, 1).toString();
                    String sql = "UPDATE krs_detail SET status_acc = 'Ditolak', catatan_revisi = ? WHERE id_detail = ?";
                    PreparedStatement ps = conn.prepareStatement(sql);
                    ps.setString(1, catatan);
                    ps.setString(2, idDetail);
                    ps.executeUpdate();
                }
            }
            
            tampilkanDetailKRS();
            JOptionPane.showMessageDialog(this, "Matkul yang dicentang berhasil ditandai Revisi.");
        } catch (Exception e) { 
            JOptionPane.showMessageDialog(this, "Gagal: " + e.getMessage()); 
            setAdaPerubahan(); // Panggil ini!
        }
    }//GEN-LAST:event_btnTolakActionPerformed

    private void btnSelesaiActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSelesaiActionPerformed
      String role = Session.getRole();
    if (role == null) return;
    
    // 1. CEK STATUS SEMUA MATKUL
    boolean adaBelumDiproses = false;
    boolean adaDitolak = false;
    
    for (int i = 0; i < jTable1.getRowCount(); i++) {
        Object statusObj = jTable1.getValueAt(i, 8); // Pastikan index 8 adalah kolom status
        String status = (statusObj != null) ? statusObj.toString() : "";
        
        // Cek jika masih ada yang belum di-ACC atau Ditolak
        if (status.equalsIgnoreCase("Ditinjau") || status.equalsIgnoreCase("Draft")) {
            adaBelumDiproses = true;
            break; 
        }
        
        if (status.equalsIgnoreCase("Ditolak")) {
            adaDitolak = true;
        }
    }

    // 2. VALIDASI: Jika masih ada matkul yang menggantung (Ditinjau), hentikan!
    if (adaBelumDiproses) {
        JOptionPane.showMessageDialog(this, "Tidak bisa selesai! Masih ada mata kuliah yang berstatus 'Ditinjau'. Harap berikan keputusan (Setujui/Revisi) untuk semua matkul.");
        return;
    }

    // 3. PROSES UPDATE STATUS
    try {
        Connection conn = new database().getConnection();
        String nextStatus;

        if (adaDitolak) {
            nextStatus = "Ditolak";
        } else {
            // Logika berdasarkan form asal agar tidak salah status
            if (parentForm instanceof Acc_KRS_DosenPA) {
                nextStatus = "AccDosenPA";
            } else if (parentForm instanceof Finalisasi_KRS) {
                nextStatus = "Disetujui"; 
            } else {
                nextStatus = "Disetujui";
            }
        }

        String sql = "UPDATE pengajuan_krs SET status_acc = ? WHERE id_pengajuan = ?";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setString(1, nextStatus);
        ps.setString(2, idPengajuanAktif);
        
        int row = ps.executeUpdate();
        
        if(row > 0) {
            JOptionPane.showMessageDialog(this, "Status pengajuan berhasil diperbarui ke: " + nextStatus);
            
            // Refresh form induk
            if (parentForm instanceof Acc_KRS_DosenPA) {
                ((Acc_KRS_DosenPA) parentForm).tampilkanData();
            } else if (parentForm instanceof Finalisasi_KRS) {
                ((Finalisasi_KRS) parentForm).tampilkanKRSMenungguFinalisasi();
            }
            
            this.dispose(); 
        }
    } catch (Exception e) { 
        e.printStackTrace(); 
        JOptionPane.showMessageDialog(this, "Error Database: " + e.getMessage()); 
    }
    }//GEN-LAST:event_btnSelesaiActionPerformed

    private void btnCetakKRSActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCetakKRSActionPerformed
  try {
            java.sql.Connection conn = new database().getConnection();
            String path = "src/main/java/PBO_Project_2/KRS_Report.jasper";
            
            java.util.HashMap<String, Object> parameter = new java.util.HashMap<>();
            parameter.put("mode", "single");
            parameter.put("target_id", idPengajuanAktif.trim()); 
            
            // --- TAMBAHAN KODE ---
            // Mengirimkan parameter filter status ke JasperReport agar yang tercetak hanya yang "Disetujui"
            parameter.put("status_acc_matkul", "Disetujui"); 
            // ---------------------
            
            JasperPrint print = JasperFillManager.fillReport(path, parameter, conn);
            
            this.dispose(); // Menutup form detail setelah mencetak
            
            net.sf.jasperreports.swing.JRViewer jrViewer = new net.sf.jasperreports.swing.JRViewer(print);
            jrViewer.setZoomRatio(0.50f);
            
            javax.swing.JFrame frameViewer = new javax.swing.JFrame("Cetak Detail KRS");
            frameViewer.getContentPane().add(jrViewer); 
            frameViewer.setDefaultCloseOperation(javax.swing.JFrame.DISPOSE_ON_CLOSE); 
            
            frameViewer.setExtendedState(javax.swing.JFrame.MAXIMIZED_BOTH); 
            
            frameViewer.setVisible(true); 
            frameViewer.setAlwaysOnTop(true); 
            frameViewer.setAlwaysOnTop(false); 
            frameViewer.toFront(); 
            
        } catch (Exception e) {
            e.printStackTrace();
            javax.swing.JOptionPane.showMessageDialog(null, "Error Report: " + e.getMessage());
        }
    }//GEN-LAST:event_btnCetakKRSActionPerformed

    private void btnHapusActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnHapusActionPerformed
       try {
            // 1. Cek dulu ada yang dicentang atau tidak
            boolean adaYgDicentang = false;
            for (int i = 0; i < jTable1.getRowCount(); i++) {
                Boolean isChecked = (Boolean) jTable1.getValueAt(i, 0); // Kolom 0 adalah Checkbox
                if (isChecked != null && isChecked) {
                    adaYgDicentang = true;
                    break;
                }
            }

            if (!adaYgDicentang) {
                JOptionPane.showMessageDialog(this, "Centang dulu mata kuliah yang ingin dihapus!");
                return;
            }

            // 2. Minta konfirmasi
            int konfirmasi = JOptionPane.showConfirmDialog(this, 
                    "Yakin ingin menghapus mata kuliah yang dicentang dari KRS?", 
                    "Konfirmasi Hapus", JOptionPane.YES_NO_OPTION);
            
            if (konfirmasi != JOptionPane.YES_OPTION) {
                return;
            }

            // 3. Eksekusi Hapus dari Database
            Connection conn = new database().getConnection();
            for (int i = 0; i < jTable1.getRowCount(); i++) {
                Boolean isChecked = (Boolean) jTable1.getValueAt(i, 0);
                if (isChecked != null && isChecked) {
                    // Ambil id_detail dari kolom ke-1 (ID Detail)
                    String idDetail = jTable1.getValueAt(i, 1).toString();
                    
                    String sql = "DELETE FROM krs_detail WHERE id_detail = ?";
                    PreparedStatement ps = conn.prepareStatement(sql);
                    ps.setString(1, idDetail);
                    ps.executeUpdate();
                }
            }
            
            // 4. Refresh tabel biar matkul yang dihapus hilang dari layar
            tampilkanDetailKRS(); 
            JOptionPane.showMessageDialog(this, "Mata kuliah yang dipilih berhasil dihapus dari KRS.");
            
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Gagal Menghapus: " + e.getMessage());
        }
    }//GEN-LAST:event_btnHapusActionPerformed

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
            // PERBAIKAN: Menggunakan pencatatan anonymous logger standar agar bebas error
            java.util.logging.Logger.getLogger("global").log(java.util.logging.Level.SEVERE, null, ex);
        }

        /* Create and display the dialog */
        java.awt.EventQueue.invokeLater(() -> {
            Detail_KRS dialog = new Detail_KRS(new javax.swing.JFrame(), true);
            dialog.addWindowListener(new java.awt.event.WindowAdapter() {
                @Override
                public void windowClosing(java.awt.event.WindowEvent e) {
                    System.exit(0);
                }
            });
            dialog.setVisible(true);
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton Tutup;
    private javax.swing.JButton btnAcc;
    private javax.swing.JButton btnCetakKRS;
    private javax.swing.JButton btnHapus;
    private javax.swing.JButton btnSelesai;
    private javax.swing.JButton btnTolak;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JTable jTable1;
    private javax.swing.JScrollPane tabelDetail;
    // End of variables declaration//GEN-END:variables
}
