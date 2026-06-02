package PBO_Project_2;

public class Session {
    private static String id, nama, role, posisi;
    private static int idProdi; // Tambahkan variabel ini di kelas Session
    
    public static void setSession(String i, String n, String r, String p) {
        id = i; nama = n; role = r; posisi = p;
    }

    public static String getId() { return id; }
    public static String getNama() { return nama; }
    public static String getRole() { return role; }
    public static String getPosisi() { return posisi; }
    public static int getIdProdi() { return idProdi; }
    public static void setIdProdi(int id) { idProdi = id; }
    
    public static void clearSession() {
        id = null; nama = null; role = null; posisi = null;
    }
}