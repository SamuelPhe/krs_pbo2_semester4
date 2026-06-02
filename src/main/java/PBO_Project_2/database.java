package PBO_Project_2;

import java.sql.*;

public class database {
    private static final String HOST     = "localhost";
    private static final String PORT     = "3306";
    private static final String DB_NAME  = "krs_db";
    private static final String USER     = "root";
    private static final String PASSWORD = "";
    private static final String URL = "jdbc:mysql://" + HOST + ":" + PORT + "/" + DB_NAME + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    
    private Connection conn;

    public database() {
        connect();
    }

    public void connect() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            conn = DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (Exception e) {
            System.out.println("Gagal koneksi: " + e.getMessage());
        }
    }

    public Connection getConnection() {
        return conn;
    }

    // FUNGSI LOGIN (Hanya melakukan query, tidak membuka dashboard)
    public ResultSet loginMahasiswa(String nama, String password) {
        try {
            String sql = "SELECT * FROM mahasiswa WHERE nama_mahasiswa = ? AND password = ?";
            PreparedStatement pst = conn.prepareStatement(sql);
            pst.setString(1, nama);
            pst.setString(2, password);
            return pst.executeQuery();
        } catch (SQLException e) { return null; }
    }

    public ResultSet loginDosen(String username, String password) {
        try {
            String sql = "SELECT * FROM dosen WHERE nama_dosen = ? AND password = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, username);
            ps.setString(2, password);
            return ps.executeQuery();
        } catch (Exception e) { return null; }
    }

    public ResultSet loginAdmin(String nama, String password) {
        try {
            String sql = "SELECT * FROM admin WHERE nama_admin = ? AND password = ?";
            PreparedStatement pst = conn.prepareStatement(sql);
            pst.setString(1, nama);
            pst.setString(2, password);
            return pst.executeQuery();
        } catch (SQLException e) { return null; }
    }


    // ══════════════════════════════════════════════════════════════════════════
    //  CRUD - PRODI
    // ══════════════════════════════════════════════════════════════════════════

    public boolean tambahProdi(String namaProdi) {
        String sql = "INSERT INTO prodi (nama_prodi) VALUES (?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, namaProdi);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.out.println("Gagal menambah prodi: " + e.getMessage());
            return false;
        }
    }

    public ResultSet getAllProdi() {
        String sql = "SELECT * FROM prodi";
        try {
            Statement st = conn.createStatement();
            return st.executeQuery(sql);
        } catch (SQLException e) {
            System.out.println("Gagal mengambil data prodi: " + e.getMessage());
            return null;
        }
    }

    public boolean updateProdi(int idProdi, String namaProdi) {
        String sql = "UPDATE prodi SET nama_prodi = ? WHERE id_prodi = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, namaProdi);
            ps.setInt(2, idProdi);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Gagal update prodi: " + e.getMessage());
            return false;
        }
    }

    public boolean deleteProdi(int idProdi) {
        String sql = "DELETE FROM prodi WHERE id_prodi = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idProdi);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Gagal menghapus prodi: " + e.getMessage());
            return false;
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  CRUD - DOSEN
    // ══════════════════════════════════════════════════════════════════════════

    public boolean tambahDosen(String namaDosen, String posisi, String password) {
        String sql = "INSERT INTO dosen (nama_dosen, posisi, password) VALUES (?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, namaDosen);
            ps.setString(2, posisi);
            ps.setString(3, password);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.out.println("Gagal menambah dosen: " + e.getMessage());
            return false;
        }
    }

    public ResultSet getAllDosen() {
        String sql = "SELECT * FROM dosen";
        try {
            Statement st = conn.createStatement();
            return st.executeQuery(sql);
        } catch (SQLException e) {
            System.out.println("Gagal mengambil data dosen: " + e.getMessage());
            return null;
        }
    }

    public ResultSet getDosenById(int idDosen) {
        String sql = "SELECT * FROM dosen WHERE id_dosen = ?";
        try {
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, idDosen);
            return ps.executeQuery();
        } catch (SQLException e) {
            System.out.println("Gagal mencari dosen: " + e.getMessage());
            return null;
        }
    }

    public boolean updateDosen(int idDosen, String namaDosen, String posisi, String password) {
        String sql = "UPDATE dosen SET nama_dosen = ?, posisi = ?, password = ? WHERE id_dosen = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, namaDosen);
            ps.setString(2, posisi);
            ps.setString(3, password);
            ps.setInt(4, idDosen);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Gagal update dosen: " + e.getMessage());
            return false;
        }
    }

    public boolean deleteDosen(int idDosen) {
        String sql = "DELETE FROM dosen WHERE id_dosen = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idDosen);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Gagal menghapus dosen: " + e.getMessage());
            return false;
        }
    }


    // ══════════════════════════════════════════════════════════════════════════
    //  CRUD - MAHASISWA
    // ══════════════════════════════════════════════════════════════════════════
    
    // PERHATIKAN PARAMETERNYA SEKARANG ADA TAMBAHAN 'int semester'
    public boolean tambahMahasiswa(int idProdi, int idDosen, String nama, int angkatan, int semester, String password) {
        // Jangan lupa tambahkan kolom 'semester' di query INSERT
        String sql = "INSERT INTO mahasiswa (id_prodi, id_dosen_pa, nama_mahasiswa, angkatan, semester, password) VALUES (?, ?, ?, ?, ?, ?)";
        
        try {
            PreparedStatement pst = conn.prepareStatement(sql);
            pst.setInt(1, idProdi);
            pst.setInt(2, idDosen);
            pst.setString(3, nama);
            pst.setInt(4, angkatan);
            pst.setInt(5, semester); // <-- MASUKKAN DATA SEMESTER KE DATABASE
            pst.setString(6, password);
            
            pst.executeUpdate();
            return true;
        } catch (Exception e) {
            System.out.println("Gagal menambahkan mahasiswa: " + e.getMessage());
            return false;
        }
    }
    
    public ResultSet getAllMahasiswa() {
        String sql = "SELECT m.*, p.nama_prodi, d.nama_dosen AS nama_dosen_pa " +
                     "FROM mahasiswa m " +
                     "JOIN prodi p ON m.id_prodi = p.id_prodi " +
                     "JOIN dosen d ON m.id_dosen_pa = d.id_dosen";
        try {
            Statement st = conn.createStatement();
            return st.executeQuery(sql);
        } catch (SQLException e) {
            System.out.println("Gagal mengambil data mahasiswa: " + e.getMessage());
            return null;
        }
    }

    public ResultSet getMahasiswaById(int idMahasiswa) {
        String sql = "SELECT m.*, p.nama_prodi, d.nama_dosen AS nama_dosen_pa " +
                     "FROM mahasiswa m " +
                     "JOIN prodi p ON m.id_prodi = p.id_prodi " +
                     "JOIN dosen d ON m.id_dosen_pa = d.id_dosen " +
                     "WHERE m.id_mahasiswa = ?";
        try {
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, idMahasiswa);
            return ps.executeQuery();
        } catch (SQLException e) {
            System.out.println("Gagal mencari mahasiswa: " + e.getMessage());
            return null;
        }
    }

    public ResultSet getMahasiswaByDosenPA(int idDosenPA) {
        String sql = "SELECT m.*, p.nama_prodi FROM mahasiswa m " +
                     "JOIN prodi p ON m.id_prodi = p.id_prodi " +
                     "WHERE m.id_dosen_pa = ?";
        try {
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, idDosenPA);
            return ps.executeQuery();
        } catch (SQLException e) {
            System.out.println("Gagal mengambil mahasiswa by dosen PA: " + e.getMessage());
            return null;
        }
    }

    public boolean updateMahasiswa(int idMahasiswa, int idProdi, int idDosenPA, String namaMahasiswa, int angkatan, String password) {
        String sql = "UPDATE mahasiswa SET id_prodi = ?, id_dosen_pa = ?, nama_mahasiswa = ?, angkatan = ?, password = ? WHERE id_mahasiswa = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idProdi);
            ps.setInt(2, idDosenPA);
            ps.setString(3, namaMahasiswa);
            ps.setInt(4, angkatan);
            ps.setString(5, password);
            ps.setInt(6, idMahasiswa);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Gagal update mahasiswa: " + e.getMessage());
            return false;
        }
    }

    public boolean deleteMahasiswa(int idMahasiswa) {
        String sql = "DELETE FROM mahasiswa WHERE id_mahasiswa = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idMahasiswa);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Gagal menghapus mahasiswa: " + e.getMessage());
            return false;
        }
    }

    // ----- Sign Up Tools ------
    public ResultSet getDaftarProdi() {
        try {
            String sql = "SELECT * FROM prodi"; 
            Statement stmt = conn.createStatement();
            return stmt.executeQuery(sql);
        } catch (SQLException e) {
            System.out.println("Error ambil prodi: " + e.getMessage());
            return null;
        }
    }

    public ResultSet getDaftarDosen() {
        try {
            String sql = "SELECT * FROM dosen"; 
            Statement stmt = conn.createStatement();
            return stmt.executeQuery(sql);
        } catch (SQLException e) {
            System.out.println("Error ambil dosen: " + e.getMessage());
            return null;
        }
    }


    // ══════════════════════════════════════════════════════════════════════════
    //  CRUD - MATKUL
    // ══════════════════════════════════════════════════════════════════════════

    public boolean tambahMatkul(String namaMatkul, int sks) {
        String sql = "INSERT INTO matkul (nama_matkul, sks) VALUES (?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, namaMatkul);
            ps.setInt(2, sks);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.out.println("Gagal menambah matkul: " + e.getMessage());
            return false;
        }
    }

    public ResultSet getAllMatkul() {
        String sql = "SELECT * FROM matkul";
        try {
            Statement st = conn.createStatement();
            return st.executeQuery(sql);
        } catch (SQLException e) {
            System.out.println("Gagal mengambil data matkul: " + e.getMessage());
            return null;
        }
    }

    public boolean updateMatkul(int idMatkul, String namaMatkul, int sks) {
        String sql = "UPDATE matkul SET nama_matkul = ?, sks = ? WHERE id_matkul = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, namaMatkul);
            ps.setInt(2, sks);
            ps.setInt(3, idMatkul);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Gagal update matkul: " + e.getMessage());
            return false;
        }
    }

    public boolean deleteMatkul(int idMatkul) {
        String sql = "DELETE FROM matkul WHERE id_matkul = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idMatkul);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Gagal menghapus matkul: " + e.getMessage());
            return false;
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  CRUD - KELAS & RUANG
    // ══════════════════════════════════════════════════════════════════════════

    public boolean tambahKelas(String namaKelas) {
        String sql = "INSERT INTO kelas (nama_kelas) VALUES (?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, namaKelas);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            return false;
        }
    }

    public ResultSet getAllKelas() {
        try {
            return conn.createStatement().executeQuery("SELECT * FROM kelas");
        } catch (SQLException e) { return null; }
    }

    public boolean updateKelas(int idKelas, String namaKelas) {
        String sql = "UPDATE kelas SET nama_kelas = ? WHERE id_kelas = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, namaKelas); ps.setInt(2, idKelas); return ps.executeUpdate() > 0;
        } catch (SQLException e) { return false; }
    }

    public boolean deleteKelas(int idKelas) {
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM kelas WHERE id_kelas = ?")) {
            ps.setInt(1, idKelas); return ps.executeUpdate() > 0;
        } catch (SQLException e) { return false; }
    }

    public boolean tambahRuang(String namaRuang) {
        String sql = "INSERT INTO ruang (nama_ruang) VALUES (?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, namaRuang); return ps.executeUpdate() > 0;
        } catch (SQLException e) { return false; }
    }

    public ResultSet getAllRuang() {
        try { return conn.createStatement().executeQuery("SELECT * FROM ruang");
        } catch (SQLException e) { return null; }
    }

    public boolean updateRuang(int idRuang, String namaRuang) {
        String sql = "UPDATE ruang SET nama_ruang = ? WHERE id_ruang = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, namaRuang); ps.setInt(2, idRuang); return ps.executeUpdate() > 0;
        } catch (SQLException e) { return false; }
    }

    public boolean deleteRuang(int idRuang) {
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM ruang WHERE id_ruang = ?")) {
            ps.setInt(1, idRuang); return ps.executeUpdate() > 0;
        } catch (SQLException e) { return false; }
    }


    // ══════════════════════════════════════════════════════════════════════════
    //  CRUD - JADWAL & CEK BENTROK
    // ══════════════════════════════════════════════════════════════════════════

    public String cekBentrokJadwal(int idDosen, String hari, int idKelas, String jamMulai, String jamSelesai, int idRuang, String tahunAjaran, String semester, int idJadwalKecuali) {
        String sql = "SELECT * FROM jadwal WHERE hari = ? AND tahun_ajaran = ? AND semester = ? " +
                     "AND (jam_mulai < ? AND jam_selesai > ?) " +
                     "AND (id_dosen = ? OR ruang = ? OR kelas = ?) ";

        if (idJadwalKecuali > 0) {
            sql += "AND id_jadwal != " + idJadwalKecuali; 
        }

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, hari);
            ps.setString(2, tahunAjaran);
            ps.setString(3, semester);
            ps.setString(4, jamSelesai);
            ps.setString(5, jamMulai);
            ps.setInt(6, idDosen);
            ps.setInt(7, idRuang);  
            ps.setInt(8, idKelas);  

            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                if (rs.getInt("id_dosen") == idDosen) return "Gagal: Dosen sudah mengajar di jadwal lain pada jam tersebut!";
                if (rs.getInt("ruang") == idRuang) return "Gagal: Ruangan sudah dipakai kelas lain pada jam tersebut!"; 
                if (rs.getInt("kelas") == idKelas) return "Gagal: Kelas mahasiswa sudah memiliki mata kuliah lain di jam tersebut!"; 
                return "Gagal: Terjadi bentrok jadwal!";
            }
        } catch (SQLException e) {
            System.out.println("Error cek bentrok: " + e.getMessage());
            return "Error database saat mengecek bentrok.";
        }
        return null; 
    }

    public boolean tambahJadwal(int idMatkul, int idDosen, int idProdi,
                                 String hari, int idKelas, String jamMulai,
                                 String jamSelesai, int idRuang,
                                 String tahunAjaran, String semester) {
                                 
        String pesanBentrok = cekBentrokJadwal(idDosen, hari, idKelas, jamMulai, jamSelesai, idRuang, tahunAjaran, semester, -1);
        if (pesanBentrok != null) {
            System.out.println(pesanBentrok);
            return false; 
        }

        String sql = "INSERT INTO jadwal (id_matkul, id_dosen, id_prodi, hari, kelas, " +
                     "jam_mulai, jam_selesai, ruang, tahun_ajaran, semester) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idMatkul);
            ps.setInt(2, idDosen);
            ps.setInt(3, idProdi);
            ps.setString(4, hari);
            ps.setInt(5, idKelas);       
            ps.setString(6, jamMulai);   
            ps.setString(7, jamSelesai);
            ps.setInt(8, idRuang);       
            ps.setString(9, tahunAjaran);
            ps.setString(10, semester);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.out.println("Gagal menambah jadwal: " + e.getMessage());
            return false;
        }
    }
  
    // PERBAIKAN: Join ke tabel kelas dan ruang agar memunculkan NAMA, bukan ANGKA
    public ResultSet getAllJadwal() {
        String sql = "SELECT j.*, mk.nama_matkul, mk.sks, d.nama_dosen, p.nama_prodi, kl.nama_kelas, r.nama_ruang " +
                     "FROM jadwal j " +
                     "JOIN matkul mk ON j.id_matkul = mk.id_matkul " +
                     "JOIN dosen d   ON j.id_dosen   = d.id_dosen " +
                     "JOIN prodi p   ON j.id_prodi   = p.id_prodi " +
                     "JOIN kelas kl  ON j.kelas      = kl.id_kelas " +
                     "JOIN ruang r   ON j.ruang      = r.id_ruang";
        try {
            Statement st = conn.createStatement();
            return st.executeQuery(sql);
        } catch (SQLException e) {
            System.out.println("Gagal mengambil data jadwal: " + e.getMessage());
            return null;
        }
    }

    // PERBAIKAN: Join ke tabel kelas dan ruang agar memunculkan NAMA, bukan ANGKA
    public ResultSet getJadwalByProdi(int idProdi, String tahunAjaran, String semester) {
        String sql = "SELECT j.*, mk.nama_matkul, mk.sks, d.nama_dosen, kl.nama_kelas, r.nama_ruang " +
                     "FROM jadwal j " +
                     "JOIN matkul mk ON j.id_matkul = mk.id_matkul " +
                     "JOIN dosen d   ON j.id_dosen   = d.id_dosen " +
                     "JOIN kelas kl  ON j.kelas      = kl.id_kelas " +
                     "JOIN ruang r   ON j.ruang      = r.id_ruang " +
                     "WHERE j.id_prodi = ? AND j.tahun_ajaran = ? AND j.semester = ?";
        try {
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, idProdi);
            ps.setString(2, tahunAjaran);
            ps.setString(3, semester);
            return ps.executeQuery();
        } catch (SQLException e) {
            System.out.println("Gagal mengambil jadwal by prodi: " + e.getMessage());
            return null;
        }
    }

    public boolean updateJadwal(int idJadwal, int idMatkul, int idDosen, int idProdi,
                                 String hari, int idKelas, String jamMulai,
                                 String jamSelesai, int idRuang,
                                 String tahunAjaran, String semester) {
                                 
        String pesanBentrok = cekBentrokJadwal(idDosen, hari, idKelas, jamMulai, jamSelesai, idRuang, tahunAjaran, semester, idJadwal);
        if (pesanBentrok != null) {
            System.out.println(pesanBentrok);
            return false; 
        }

        String sql = "UPDATE jadwal SET id_matkul=?, id_dosen=?, id_prodi=?, hari=?, kelas=?, " +
                     "jam_mulai=?, jam_selesai=?, ruang=?, tahun_ajaran=?, semester=? WHERE id_jadwal=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idMatkul);
            ps.setInt(2, idDosen);
            ps.setInt(3, idProdi);
            ps.setString(4, hari);
            ps.setInt(5, idKelas); 
            ps.setString(6, jamMulai);
            ps.setString(7, jamSelesai);
            ps.setInt(8, idRuang); 
            ps.setString(9, tahunAjaran);
            ps.setString(10, semester);
            ps.setInt(11, idJadwal);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Gagal update jadwal: " + e.getMessage());
            return false;
        }
    }

    public boolean deleteJadwal(int idJadwal) {
        String sql = "DELETE FROM jadwal WHERE id_jadwal = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idJadwal);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Gagal menghapus jadwal: " + e.getMessage());
            return false;
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  CRUD - KRS (Header)
    // ══════════════════════════════════════════════════════════════════════════

    public int buatKRS(int idMahasiswa, int idDosenPA, String semester,
                        String tahunAjaran, String tanggalPengajuan) {
        String sql = "INSERT INTO krs (id_mahasiswa, id_dosen_pa, semester, tahun_ajaran, " +
                     "status_krs, tanggal_pengajuan) VALUES (?, ?, ?, ?, 'Menunggu', ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, idMahasiswa);
            ps.setInt(2, idDosenPA);
            ps.setString(3, semester);
            ps.setString(4, tahunAjaran);
            ps.setString(5, tanggalPengajuan); 
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) return rs.getInt(1); 
        } catch (SQLException e) {
            System.out.println("Gagal membuat KRS: " + e.getMessage());
        }
        return -1;
    }

    public ResultSet getKRSByMahasiswa(int idMahasiswa) {
        String sql = "SELECT k.*, m.nama_mahasiswa, " +
                     "dp.nama_dosen AS nama_dosen_pa, " +
                     "da.nama_dosen AS nama_dosen_acc " +
                     "FROM krs k " +
                     "JOIN mahasiswa m ON k.id_mahasiswa = m.id_mahasiswa " +
                     "JOIN dosen dp  ON k.id_dosen_pa  = dp.id_dosen " +
                     "LEFT JOIN dosen da ON k.id_dosen_acc = da.id_dosen " +
                     "WHERE k.id_mahasiswa = ?";
        try {
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, idMahasiswa);
            return ps.executeQuery();
        } catch (SQLException e) {
            return null;
        }
    }

    public ResultSet getAllKRS() {
        String sql = "SELECT k.*, m.nama_mahasiswa, " +
                     "dp.nama_dosen AS nama_dosen_pa, " +
                     "da.nama_dosen AS nama_dosen_acc " +
                     "FROM krs k " +
                     "JOIN mahasiswa m ON k.id_mahasiswa = m.id_mahasiswa " +
                     "JOIN dosen dp  ON k.id_dosen_pa  = dp.id_dosen " +
                     "LEFT JOIN dosen da ON k.id_dosen_acc = da.id_dosen";
        try {
            return conn.createStatement().executeQuery(sql);
        } catch (SQLException e) {
            return null;
        }
    }

    public ResultSet getKRSByDosenPA(int idDosenPA) {
        String sql = "SELECT k.*, m.nama_mahasiswa " +
                     "FROM krs k " +
                     "JOIN mahasiswa m ON k.id_mahasiswa = m.id_mahasiswa " +
                     "WHERE k.id_dosen_pa = ?";
        try {
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, idDosenPA);
            return ps.executeQuery();
        } catch (SQLException e) {
            return null;
        }
    }

    public boolean updateStatusKRS(int idKRS, int idDosenAcc, String statusKRS) {
        String sql = "UPDATE krs SET id_dosen_acc = ?, status_krs = ? WHERE id_krs = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idDosenAcc);
            ps.setString(2, statusKRS); 
            ps.setInt(3, idKRS);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            return false;
        }
    }

    public boolean deleteKRS(int idKRS) {
        String sql = "DELETE FROM krs WHERE id_krs = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idKRS);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            return false;
        }
    }


    // ══════════════════════════════════════════════════════════════════════════
    //  CRUD - KRS DETAIL
    // ══════════════════════════════════════════════════════════════════════════

    public boolean tambahKRSDetail(int idKRS, int idJadwal) {
        String sql = "INSERT INTO krs_detail (id_krs, id_jadwal) VALUES (?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idKRS);
            ps.setInt(2, idJadwal);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.out.println("Gagal menambah detail KRS: " + e.getMessage());
            return false;
        }
    }

    // PERBAIKAN: Menambahkan Join untuk kelas dan ruang agar bisa dipanggil namanya
    public ResultSet getKRSDetailByKRS(int idKRS) {
        String sql = "SELECT kd.*, mk.nama_matkul, mk.sks, d.nama_dosen, " +
                     "j.hari, j.jam_mulai, j.jam_selesai, kl.nama_kelas, r.nama_ruang " +
                     "FROM krs_detail kd " +
                     "JOIN jadwal j  ON kd.id_jadwal = j.id_jadwal " +
                     "JOIN matkul mk ON j.id_matkul  = mk.id_matkul " +
                     "JOIN dosen d   ON j.id_dosen   = d.id_dosen " +
                     "JOIN kelas kl  ON j.kelas      = kl.id_kelas " +
                     "JOIN ruang r   ON j.ruang      = r.id_ruang " +
                     "WHERE kd.id_krs = ?";
        try {
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, idKRS);
            return ps.executeQuery();
        } catch (SQLException e) {
            System.out.println("Gagal mengambil detail KRS: " + e.getMessage());
            return null;
        }
    }

    public boolean updateNilai(int idDetail, double nilai) {
        String sql = "UPDATE krs_detail SET nilai = ? WHERE id_detail = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, nilai);
            ps.setInt(2, idDetail);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            return false;
        }
    }

    public boolean deleteKRSDetail(int idDetail) {
        String sql = "DELETE FROM krs_detail WHERE id_detail = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idDetail);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            return false;
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  CRUD - ADMIN
    // ══════════════════════════════════════════════════════════════════════════

    public boolean tambahAdmin(String namaAdmin, String password) {
        String sql = "INSERT INTO admin (nama_admin, password) VALUES (?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, namaAdmin);
            ps.setString(2, password);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    public boolean updateAdmin(int idAdmin, String namaAdmin, String password) {
        String sql = "UPDATE admin SET nama_admin = ?, password = ? WHERE id_admin = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, namaAdmin);
            ps.setString(2, password);
            ps.setInt(3, idAdmin);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            return false;
        }
    }

    public boolean deleteAdmin(int idAdmin) {
        String sql = "DELETE FROM admin WHERE id_admin = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idAdmin);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            return false;
        }
    }

  public int getJumlahBimbingan(String idDosen) {
    int count = 0;
    try {
        // PERBAIKAN: Gunakan id_dosen_pa karena di buatKRS Anda menggunakan id_dosen_pa
        String sql = "SELECT COUNT(*) AS total FROM krs WHERE id_dosen_pa = ? AND status_krs IN ('Ditinjau', 'Disetujui')";
        
        PreparedStatement pst = getConnection().prepareStatement(sql);
        pst.setString(1, idDosen);
        ResultSet rs = pst.executeQuery();
        
        if (rs.next()) {
            count = rs.getInt("total");
        }
    } catch (Exception e) {
        System.out.println("Error count bimbingan: " + e.getMessage());
    }
    return count;
}
    
    // Tambahkan method ini di database.java
public String getCatatanRevisi(String idPengajuan) {
    String catatan = "";
    try {
        java.sql.Connection conn = getConnection();
        String sql = "SELECT catatan_revisi FROM pengajuan_krs WHERE id_pengajuan = ?";
        java.sql.PreparedStatement ps = conn.prepareStatement(sql);
        ps.setString(1, idPengajuan);
        java.sql.ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            catatan = rs.getString("catatan_revisi");
        }
    } catch (Exception e) {
        System.out.println("Error get catatan: " + e.getMessage());
    }
    return catatan;
}
    
public int getIdProdiByKaprodi(int idDosen) {
    String sql = "SELECT id_prodi FROM prodi WHERE id_kaprodi = ?";
    try (PreparedStatement ps = conn.prepareStatement(sql)) {
        ps.setInt(1, idDosen);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            return rs.getInt("id_prodi");
        }
    } catch (Exception e) {
        e.printStackTrace();
    }
    return -1; // Jika tidak ditemukan
}

public String getSqlFilter(String tabel, String idUser, String role) {
    if (role.equalsIgnoreCase("Admin")) {
        return "SELECT * FROM " + tabel; // Admin lihat semua
    } else {
        // Mahasiswa/Dosen lihat punya sendiri
        // ID Primary Key tiap tabel biasanya id_mahasiswa / id_dosen
        String kolomId = (tabel.equals("mahasiswa")) ? "id_mahasiswa" : "id_dosen";
        return "SELECT * FROM " + tabel + " WHERE " + kolomId + " = '" + idUser + "'";
    }
}

public void debugCekKolom(String namaTabel) {
    try {
        DatabaseMetaData meta = conn.getMetaData();
        ResultSet columns = meta.getColumns(null, null, namaTabel, null);
        System.out.println("--- DAFTAR KOLOM TABEL " + namaTabel + " ---");
        while (columns.next()) {
            System.out.println("Kolom: " + columns.getString("COLUMN_NAME"));
        }
    } catch (Exception e) {
        e.printStackTrace();
    }
}
    // ══════════════════════════════════════════════════════════════════════════
    //  MAIN - Testing (Disesuaikan parameternya)
    // ══════════════════════════════════════════════════════════════════════════

    public void disconnect() {
        try { if (conn != null) conn.close(); } catch (SQLException e) { System.out.println(e.getMessage()); }
    }
    
    public boolean isConnected() {
        try { return conn != null && !conn.isClosed(); } catch (SQLException e) { return false; }
    }

    // HANYA SATU MAIN METHOD DI SINI
    public static void main(String[] args) {
        database db = new database();

        if (db.isConnected()) {
         db.debugCekKolom("krs");        // Jalankan ini
        db.debugCekKolom("krs_detail"); // Jalankan ini
        db.disconnect();
            System.out.println("Status: Terhubung ke " + DB_NAME);
            
            // 7. Buat KRS
            int idKRS = db.buatKRS(1, 1, "Genap", "2024/2025", "2025-01-10");
            System.out.println("ID KRS dibuat: " + idKRS);

            // 8. Tambah detail KRS (id_jadwal=1)
            db.tambahKRSDetail(idKRS, 1);

            // 9. Tampilkan detail KRS
            try {
                ResultSet rs = db.getKRSDetailByKRS(idKRS);
                System.out.println("\n=== Detail KRS ===");
                while (rs != null && rs.next()) {
                    System.out.println(
                        rs.getString("nama_matkul") + " | " +
                        rs.getInt("sks") + " SKS | " +
                        rs.getString("nama_dosen") + " | " +
                        "Kelas: " + rs.getString("nama_kelas") + " | " + 
                        "Ruang: " + rs.getString("nama_ruang") + " | " + 
                        rs.getString("hari") + " " +
                        rs.getString("jam_mulai") + "-" +
                        rs.getString("jam_selesai")
                    );
                }
            } catch (SQLException e) {
                System.out.println("Error: " + e.getMessage());
            }

            // 10. Dosen ACC KRS
            db.updateStatusKRS(idKRS, 1, "Disetujui");
            
            // PENTING: disconnect dipanggil DI DALAM main, sebelum penutup if
            db.disconnect(); 
        }
    }
}