/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package PBO_Project_2;

import java.sql.*;
import javax.swing.JOptionPane;

public class FormEditProfile extends javax.swing.JFrame {

    database db = new database();
    private String origNama, origPassword, origTambahan1, origTambahan2;

    public FormEditProfile() {
    initComponents();
    this.setLocationRelativeTo(null);
    
    // Kunci ID agar tidak bisa diubah user
    txtUsername.setEditable(false); 
    // Tambahkan warna abu-abu agar user tahu field ini read-only
    txtUsername.setBackground(new java.awt.Color(240, 240, 240)); 
    
    aturTampilanBerdasarkanRole();
    loadDataDiri();
}

  private void aturTampilanBerdasarkanRole() {
        String role = Session.getRole();
        
        if (role.equalsIgnoreCase("Mahasiswa")) {
            jLabel7.setText("Angkatan");
            jLabel8.setText("Semester");
            // txtTambahan1 & 2 tetap tampil
            
            // --- TAMBAHKAN INI ---
        txtTambahan1.setEditable(false); // Kunci Angkatan
        txtTambahan2.setEditable(false); // Kunci Semester
        
        
        txtTambahan2.setBackground(new java.awt.Color(240, 240, 240));
        
        } else if (role.equalsIgnoreCase("Dosen")) {
            jLabel7.setText("Posisi / Jabatan"); // Ubah teks label
            jLabel8.setVisible(false);           // Sembunyikan label semester
            txtTambahan2.setVisible(false);      // Sembunyikan field semester
            txtTambahan1.setEditable(false);     // Posisi tidak bisa diedit
        } else {
            // Untuk Admin
            jLabel7.setVisible(false);
            jLabel8.setVisible(false);
            txtTambahan1.setVisible(false);
            txtTambahan2.setVisible(false);
        }
    }

    private void loadDataDiri() {
        String role = Session.getRole();
        String id = Session.getId();
        try {
            Connection conn = db.getConnection();
            String sql = role.equalsIgnoreCase("Admin") ? "SELECT nama_admin AS nama, id_admin AS username, password FROM admin WHERE id_admin = ?" :
                         role.equalsIgnoreCase("Dosen") ? "SELECT nama_dosen AS nama, id_dosen AS username, password, posisi FROM dosen WHERE id_dosen = ?" :
                         "SELECT nama_mahasiswa AS nama, id_mahasiswa AS username, password, angkatan, semester FROM mahasiswa WHERE id_mahasiswa = ?";

            PreparedStatement pst = conn.prepareStatement(sql);
            pst.setString(1, id);
            ResultSet rs = pst.executeQuery();

            if (rs.next()) {
                origNama = rs.getString("nama");
                origPassword = rs.getString("password");
                txtNama.setText(origNama);
                txtUsername.setText(rs.getString("username"));
                txtPass.setText(origPassword);
                txtConfirmPass.setText(origPassword);
                
                if (role.equalsIgnoreCase("Mahasiswa")) {
                    origTambahan1 = rs.getString("angkatan");
                    origTambahan2 = rs.getString("semester");
                    txtTambahan1.setText(origTambahan1);
                    txtTambahan2.setText(origTambahan2);
                } else if (role.equalsIgnoreCase("Dosen")) {
                    origTambahan1 = rs.getString("posisi");
                    txtTambahan1.setText(origTambahan1);
                }
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Gagal memuat profil: " + e.getMessage());
        }
    }

    private void simpanData(java.awt.event.ActionEvent evt) {
        String role = Session.getRole();
        String id = Session.getId();
        String namaBaru = txtNama.getText().trim();
        String passwordBaru = new String(txtPass.getPassword());
        String konfirmasiPass = new String(txtConfirmPass.getPassword());

        if (namaBaru.isEmpty() || passwordBaru.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Data tidak boleh kosong!");
            return;
        }
        if (!passwordBaru.equals(konfirmasiPass)) {
            JOptionPane.showMessageDialog(this, "Password tidak cocok!");
            return;
        }

        try {
            Connection conn = db.getConnection();
            String sql = role.equalsIgnoreCase("Admin") ? "UPDATE admin SET nama_admin=?, password=? WHERE id_admin=?" :
                         role.equalsIgnoreCase("Dosen") ? "UPDATE dosen SET nama_dosen=?, password=? WHERE id_dosen=?" :
                         "UPDATE mahasiswa SET nama_mahasiswa=?, password=?, angkatan=?, semester=? WHERE id_mahasiswa=?";
            
            PreparedStatement pst = conn.prepareStatement(sql);
            pst.setString(1, namaBaru);
            pst.setString(2, passwordBaru);
            
            if (role.equalsIgnoreCase("Mahasiswa")) {
                pst.setString(3, txtTambahan1.getText());
                pst.setString(4, txtTambahan2.getText());
                pst.setString(5, id);
            } else {
                pst.setString(3, id);
            }

            pst.executeUpdate();
            JOptionPane.showMessageDialog(this, "Profil diperbarui! Silakan login kembali.");
            new login_form().setVisible(true);
            this.dispose();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Gagal menyimpan: " + e.getMessage());
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

        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        btnSImpan = new javax.swing.JButton();
        btnBack = new javax.swing.JButton();
        txtNama = new javax.swing.JTextField();
        txtUsername = new javax.swing.JTextField();
        txtTambahan1 = new javax.swing.JTextField();
        txtTambahan2 = new javax.swing.JTextField();
        txtPass = new javax.swing.JPasswordField();
        txtConfirmPass = new javax.swing.JPasswordField();
        jLabel1 = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        getContentPane().setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jLabel2.setFont(new java.awt.Font("Segoe UI", 1, 36)); // NOI18N
        jLabel2.setText("Edit Profile Saya");
        getContentPane().add(jLabel2, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 10, -1, -1));

        jLabel3.setText("Nama Lengkap");
        getContentPane().add(jLabel3, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 100, -1, -1));

        jLabel4.setText("ID Anda");
        getContentPane().add(jLabel4, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 250, -1, 20));

        jLabel5.setText("Password Baru");
        getContentPane().add(jLabel5, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 150, -1, -1));

        jLabel6.setText("Confirm Password");
        getContentPane().add(jLabel6, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 200, -1, -1));

        jLabel7.setText("Angkatan");
        getContentPane().add(jLabel7, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 300, -1, -1));

        jLabel8.setText("Semester");
        getContentPane().add(jLabel8, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 350, -1, -1));

        btnSImpan.setText("Simpan");
        btnSImpan.addActionListener(this::btnSImpanActionPerformed);
        getContentPane().add(btnSImpan, new org.netbeans.lib.awtextra.AbsoluteConstraints(490, 450, -1, -1));

        btnBack.setText("Back");
        btnBack.addActionListener(this::btnBackActionPerformed);
        getContentPane().add(btnBack, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 450, -1, -1));
        getContentPane().add(txtNama, new org.netbeans.lib.awtextra.AbsoluteConstraints(170, 100, 390, -1));
        getContentPane().add(txtUsername, new org.netbeans.lib.awtextra.AbsoluteConstraints(170, 250, 390, -1));
        getContentPane().add(txtTambahan1, new org.netbeans.lib.awtextra.AbsoluteConstraints(170, 300, 390, -1));
        getContentPane().add(txtTambahan2, new org.netbeans.lib.awtextra.AbsoluteConstraints(170, 350, 390, -1));
        getContentPane().add(txtPass, new org.netbeans.lib.awtextra.AbsoluteConstraints(170, 150, 390, -1));
        getContentPane().add(txtConfirmPass, new org.netbeans.lib.awtextra.AbsoluteConstraints(170, 200, 390, -1));

        jLabel1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/desain/Square.png"))); // NOI18N
        getContentPane().add(jLabel1, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 0, -1, -1));

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnSImpanActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSImpanActionPerformed
        simpanData(evt); // Memanggil logika simpan yang sudah kita buat
    }//GEN-LAST:event_btnSImpanActionPerformed

    private void btnBackActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnBackActionPerformed
       new dashboard().setVisible(true); // Kembali ke dashboard
        this.dispose();                   // Menutup form edit
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
           java.util.logging.Logger.getLogger(FormEditProfile.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(() -> new FormEditProfile().setVisible(true));
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnBack;
    private javax.swing.JButton btnSImpan;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JPasswordField txtConfirmPass;
    private javax.swing.JTextField txtNama;
    private javax.swing.JPasswordField txtPass;
    private javax.swing.JTextField txtTambahan1;
    private javax.swing.JTextField txtTambahan2;
    private javax.swing.JTextField txtUsername;
    // End of variables declaration//GEN-END:variables
}
