package PBO_Project_2;

import java.sql.*;

public class database {

    // ─── Konfigurasi Koneksi ───────────────────────────────────────────────────
    private static final String HOST     = "localhost";
    private static final String PORT     = "3306";
    private static final String DB_NAME  = "krs_db";
    private static final String USER     = "root";
    private static final String PASSWORD = "";

    private static final String URL =
        "jdbc:mysql://" + HOST + ":" + PORT + "/" + DB_NAME +
        "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";

    private Connection conn;

    // ─── Konstruktor ───────────────────────────────────────────────────────────
    public database() {
        connect();
    }

    // ─── Koneksi ke Database ───────────────────────────────────────────────────
    public void connect() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            conn = DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println("Koneksi berhasil ke database: " + DB_NAME);
        } catch (ClassNotFoundException e) {
            System.out.println("Driver MySQL tidak ditemukan: " + e.getMessage());
        } catch (SQLException e) {
            System.out.println("Gagal koneksi ke database: " + e.getMessage());
        }
    }
    // ─── Fungsi Login untuk Mahasiswa ──────────────────────────────────────────
    public ResultSet loginMahasiswa(int idMahasiswa, String password) {
        try {
            // Gunakan PreparedStatement untuk keamanan (SQL Injection protection)
            String sql = "SELECT * FROM mahasiswa WHERE id_mahasiswa = ? AND password = ?";
            PreparedStatement pst = conn.prepareStatement(sql);
            pst.setString(1, String.valueOf(idMahasiswa));
            pst.setString(2, password);
            return pst.executeQuery();
        } catch (SQLException e) {
            System.out.println("Error login mahasiswa: " + e.getMessage());
            return null;
        }
    }

    // ─── Fungsi Login untuk Dosen ──────────────────────────────────────────────
    public ResultSet loginDosen(String username, String password) {
        try {
            // Berdasarkan struktur SQL anda, dosen menggunakan nama_dosen atau id_dosen
            String sql = "SELECT * FROM dosen WHERE nama_dosen = ? AND password = ?";
            PreparedStatement pst = conn.prepareStatement(sql);
            pst.setString(1, username);
            pst.setString(2, password);
            return pst.executeQuery();
        } catch (SQLException e) {
            System.out.println("Error login dosen: " + e.getMessage());
            return null;
        }
    }

    // ─── Fungsi Login untuk Admin ──────────────────────────────────────────────
    public ResultSet loginAdmin(String username, String password) {
        try {
            String sql = "SELECT * FROM admin WHERE nama_admin = ? AND password = ?";
            PreparedStatement pst = conn.prepareStatement(sql);
            pst.setString(1, username);
            pst.setString(2, password);
            return pst.executeQuery();
        } catch (SQLException e) {
            System.out.println("Error login admin: " + e.getMessage());
            return null;
        }
    }

    // ─── Tutup Koneksi ─────────────────────────────────────────────────────────
    public void disconnect() {
        try {
            if (conn != null && !conn.isClosed()) {
                conn.close();
                System.out.println("Koneksi database ditutup.");
            }
        } catch (SQLException e) {
            System.out.println("Gagal menutup koneksi: " + e.getMessage());
        }
    }

    public boolean isConnected() {
        try {
            return conn != null && !conn.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }

    public Connection getConnection() {
        return conn;
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

    // LOGIN DOSEN
    


    // ══════════════════════════════════════════════════════════════════════════
    //  CRUD - MAHASISWA
    // ══════════════════════════════════════════════════════════════════════════

    public boolean tambahMahasiswa(int idProdi, int idDosenPA, String namaMahasiswa, int angkatan, String password) {
        String sql = "INSERT INTO mahasiswa (id_prodi, id_dosen_pa, nama_mahasiswa, angkatan, password) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idProdi);
            ps.setInt(2, idDosenPA);
            ps.setString(3, namaMahasiswa);
            ps.setInt(4, angkatan);
            ps.setString(5, password);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.out.println("Gagal menambah mahasiswa: " + e.getMessage());
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

    // READ - mahasiswa berdasarkan dosen PA
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
    //  CRUD - JADWAL
    // ══════════════════════════════════════════════════════════════════════════

    public boolean tambahJadwal(int idMatkul, int idDosen, int idProdi,
                                 String hari, String kelas, String jamMulai,
                                 String jamSelesai, String ruang,
                                 String tahunAjaran, String semester) {
        String sql = "INSERT INTO jadwal (id_matkul, id_dosen, id_prodi, hari, kelas, " +
                     "jam_mulai, jam_selesai, ruang, tahun_ajaran, semester) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idMatkul);
            ps.setInt(2, idDosen);
            ps.setInt(3, idProdi);
            ps.setString(4, hari);
            ps.setString(5, kelas);
            ps.setString(6, jamMulai);   // format "HH:MM:SS"
            ps.setString(7, jamSelesai);
            ps.setString(8, ruang);
            ps.setString(9, tahunAjaran);
            ps.setString(10, semester);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.out.println("Gagal menambah jadwal: " + e.getMessage());
            return false;
        }
    }

    public ResultSet getAllJadwal() {
        String sql = "SELECT j.*, mk.nama_matkul, mk.sks, d.nama_dosen, p.nama_prodi " +
                     "FROM jadwal j " +
                     "JOIN matkul mk ON j.id_matkul = mk.id_matkul " +
                     "JOIN dosen d  ON j.id_dosen  = d.id_dosen " +
                     "JOIN prodi p  ON j.id_prodi  = p.id_prodi";
        try {
            Statement st = conn.createStatement();
            return st.executeQuery(sql);
        } catch (SQLException e) {
            System.out.println("Gagal mengambil data jadwal: " + e.getMessage());
            return null;
        }
    }

    // READ - jadwal berdasarkan prodi (untuk mahasiswa memilih matkul)
    public ResultSet getJadwalByProdi(int idProdi, String tahunAjaran, String semester) {
        String sql = "SELECT j.*, mk.nama_matkul, mk.sks, d.nama_dosen " +
                     "FROM jadwal j " +
                     "JOIN matkul mk ON j.id_matkul = mk.id_matkul " +
                     "JOIN dosen d  ON j.id_dosen  = d.id_dosen " +
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
                                 String hari, String kelas, String jamMulai,
                                 String jamSelesai, String ruang,
                                 String tahunAjaran, String semester) {
        String sql = "UPDATE jadwal SET id_matkul=?, id_dosen=?, id_prodi=?, hari=?, kelas=?, " +
                     "jam_mulai=?, jam_selesai=?, ruang=?, tahun_ajaran=?, semester=? WHERE id_jadwal=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idMatkul);
            ps.setInt(2, idDosen);
            ps.setInt(3, idProdi);
            ps.setString(4, hari);
            ps.setString(5, kelas);
            ps.setString(6, jamMulai);
            ps.setString(7, jamSelesai);
            ps.setString(8, ruang);
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

    // CREATE - mahasiswa mengajukan KRS baru
    public int buatKRS(int idMahasiswa, int idDosenPA, String semester,
                        String tahunAjaran, String tanggalPengajuan) {
        String sql = "INSERT INTO krs (id_mahasiswa, id_dosen_pa, semester, tahun_ajaran, " +
                     "status_krs, tanggal_pengajuan) VALUES (?, ?, ?, ?, 'Menunggu', ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, idMahasiswa);
            ps.setInt(2, idDosenPA);
            ps.setString(3, semester);
            ps.setString(4, tahunAjaran);
            ps.setString(5, tanggalPengajuan); // format "YYYY-MM-DD"
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) return rs.getInt(1); // kembalikan id_krs
        } catch (SQLException e) {
            System.out.println("Gagal membuat KRS: " + e.getMessage());
        }
        return -1;
    }

    // READ - KRS berdasarkan mahasiswa
    public ResultSet getKRSByMahasiswa(int idMahasiswa) {
        String sql = "SELECT k.*, m.nama_mahasiswa, " +
                     "dp.nama_dosen AS nama_dosen_pa, " +
                     "da.nama_dosen AS nama_dosen_acc " +
                     "FROM krs k " +
                     "JOIN mahasiswa m ON k.id_mahasiswa = m.id_mahasiswa " +
                     "JOIN dosen dp   ON k.id_dosen_pa  = dp.id_dosen " +
                     "LEFT JOIN dosen da ON k.id_dosen_acc = da.id_dosen " +
                     "WHERE k.id_mahasiswa = ?";
        try {
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, idMahasiswa);
            return ps.executeQuery();
        } catch (SQLException e) {
            System.out.println("Gagal mengambil KRS: " + e.getMessage());
            return null;
        }
    }

    // READ - semua KRS (untuk dosen PA / admin)
    public ResultSet getAllKRS() {
        String sql = "SELECT k.*, m.nama_mahasiswa, " +
                     "dp.nama_dosen AS nama_dosen_pa, " +
                     "da.nama_dosen AS nama_dosen_acc " +
                     "FROM krs k " +
                     "JOIN mahasiswa m ON k.id_mahasiswa = m.id_mahasiswa " +
                     "JOIN dosen dp   ON k.id_dosen_pa  = dp.id_dosen " +
                     "LEFT JOIN dosen da ON k.id_dosen_acc = da.id_dosen";
        try {
            Statement st = conn.createStatement();
            return st.executeQuery(sql);
        } catch (SQLException e) {
            System.out.println("Gagal mengambil semua KRS: " + e.getMessage());
            return null;
        }
    }

    // READ - KRS yang perlu di-acc oleh dosen PA tertentu
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
            System.out.println("Gagal mengambil KRS by dosen PA: " + e.getMessage());
            return null;
        }
    }

    // UPDATE - dosen ACC / tolak KRS
    public boolean updateStatusKRS(int idKRS, int idDosenAcc, String statusKRS) {
        String sql = "UPDATE krs SET id_dosen_acc = ?, status_krs = ? WHERE id_krs = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idDosenAcc);
            ps.setString(2, statusKRS); // "Disetujui" / "Ditolak"
            ps.setInt(3, idKRS);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Gagal update status KRS: " + e.getMessage());
            return false;
        }
    }

    public boolean deleteKRS(int idKRS) {
        String sql = "DELETE FROM krs WHERE id_krs = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idKRS);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Gagal menghapus KRS: " + e.getMessage());
            return false;
        }
    }


    // ══════════════════════════════════════════════════════════════════════════
    //  CRUD - KRS DETAIL
    // ══════════════════════════════════════════════════════════════════════════

    // CREATE - tambah matkul ke KRS
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

    // READ - detail KRS berdasarkan id_krs
    public ResultSet getKRSDetailByKRS(int idKRS) {
        String sql = "SELECT kd.*, mk.nama_matkul, mk.sks, d.nama_dosen, " +
                     "j.hari, j.kelas, j.jam_mulai, j.jam_selesai, j.ruang " +
                     "FROM krs_detail kd " +
                     "JOIN jadwal j  ON kd.id_jadwal = j.id_jadwal " +
                     "JOIN matkul mk ON j.id_matkul  = mk.id_matkul " +
                     "JOIN dosen d   ON j.id_dosen   = d.id_dosen " +
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

    // UPDATE - input nilai ke krs_detail
    public boolean updateNilai(int idDetail, double nilai) {
        String sql = "UPDATE krs_detail SET nilai = ? WHERE id_detail = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, nilai);
            ps.setInt(2, idDetail);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Gagal update nilai: " + e.getMessage());
            return false;
        }
    }

    // DELETE - hapus satu matkul dari KRS
    public boolean deleteKRSDetail(int idDetail) {
        String sql = "DELETE FROM krs_detail WHERE id_detail = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idDetail);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Gagal menghapus detail KRS: " + e.getMessage());
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
            System.out.println("Gagal menambah admin: " + e.getMessage());
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
            System.out.println("Gagal update admin: " + e.getMessage());
            return false;
        }
    }

    public boolean deleteAdmin(int idAdmin) {
        String sql = "DELETE FROM admin WHERE id_admin = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idAdmin);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Gagal menghapus admin: " + e.getMessage());
            return false;
        }
    }


    // ══════════════════════════════════════════════════════════════════════════
    //  MAIN - Testing
    // ══════════════════════════════════════════════════════════════════════════
    public static void main(String[] args) {
        database db = new database();

        if (db.isConnected()) {
            System.out.println("Status: Terhubung ke " + DB_NAME);

            // Contoh alur lengkap:
            // 1. Tambah prodi
            db.tambahProdi("Teknik Informatika");

            // 2. Tambah dosen
            db.tambahDosen("Dr. Andi", "Dosen PA", "password123");

            // 3. Tambah mahasiswa (id_prodi=1, id_dosen_pa=1)
            db.tambahMahasiswa(1, 1, "Budi Santoso", 2023, "pass456");

            // 4. Tambah matkul
            db.tambahMatkul("Pemrograman Berorientasi Objek", 3);

            // 5. Tambah jadwal
            db.tambahJadwal(1, 1, 1, "Senin", "A", "08:00:00", "10:00:00",
                            "R.101", "2024/2025", "Genap");

            // 6. Buat KRS
            int idKRS = db.buatKRS(1, 1, "Genap", "2024/2025", "2025-01-10");
            System.out.println("ID KRS dibuat: " + idKRS);

            // 7. Tambah detail KRS (id_jadwal=1)
            db.tambahKRSDetail(idKRS, 1);

            // 8. Tampilkan detail KRS
            try {
                ResultSet rs = db.getKRSDetailByKRS(idKRS);
                System.out.println("\n=== Detail KRS ===");
                while (rs != null && rs.next()) {
                    System.out.println(
                        rs.getString("nama_matkul") + " | " +
                        rs.getInt("sks") + " SKS | " +
                        rs.getString("nama_dosen") + " | " +
                        rs.getString("hari") + " " +
                        rs.getString("jam_mulai") + "-" +
                        rs.getString("jam_selesai") + " | " +
                        rs.getString("ruang")
                    );
                }
            } catch (SQLException e) {
                System.out.println("Error: " + e.getMessage());
            }

            // 9. Dosen ACC KRS
            db.updateStatusKRS(idKRS, 1, "Disetujui");
        }

        db.disconnect();
    }
}
