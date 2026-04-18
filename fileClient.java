import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.Socket;

/**
 * FileClient xử lí các hoạt động truyền tải file phía client.
 * Nó cung cấp một phương thức tĩnh để gửi file đến máy chủ file từ xa.
 */
public class fileClient {

  /**
   * @param host       Tên host hoặc địa chỉ IP của máy chủ file
   * @param port       Số cổng mà máy chủ file đang lắng nghe
   * @param filePath   Đường dẫn tuyệt đối hoặc tương đối đến file cần gửi
   * @param uniqueName Tên duy nhất của file để lưu trên Server
   * @throws IOException Nếu xảy ra lỗi I/O trong quá trình đọc file hoặc truyền tải mạng
   */
  public static void sendFile(String host, int port, String filePath, String uniqueName) throws IOException {
    File file = new File(filePath);

    // 1. Kiểm tra file tồn tại và có thể đọc
    if (!file.exists()) {
      throw new IOException("File không tồn tại: " + filePath);
    }
    if (!file.canRead()) {
      throw new IOException("Không có quyền đọc file: " + filePath);
    }
    if (!file.isFile()) {
      throw new IOException("Đường dẫn không phải là file: " + filePath);
    }

    // 2. Giới hạn kích thước file (tối đa 100MB)
    final long MAX_FILE_SIZE = 100 * 1024 * 1024; // 100MB
    if (file.length() > MAX_FILE_SIZE) {
      throw new IOException("File quá lớn (tối đa 100MB): " + file.length() + " bytes");
    }
    if (file.length() == 0) {
      throw new IOException("File rỗng: " + filePath);
    }

    // 3. Validate tên file (chống path traversal)
    if (uniqueName.contains("..") || uniqueName.contains("/") || uniqueName.contains("\\") ||
        uniqueName.startsWith(".") || uniqueName.length() > 255) {
      throw new IOException("Tên file không hợp lệ: " + uniqueName);
    }

    // 4. Thử gửi với retry (tối đa 3 lần)
    int maxRetries = 3;
    IOException lastException = null;

    for (int attempt = 1; attempt <= maxRetries; attempt++) {
      Socket s = null;
      DataOutputStream dos = null;
      FileInputStream fis = null;

      try {
        // Khối 1: Thiết lập kết nối socket với timeout
        s = new Socket();
        s.connect(new java.net.InetSocketAddress(host, port), 10000); // 10s timeout
        s.setSoTimeout(30000); // 30s read timeout

        // Khối 2: Thiết lập luồng output dữ liệu
        dos = new DataOutputStream(s.getOutputStream());

        // Gửi Tên file và Dung lượng file
        dos.writeUTF(uniqueName);
        dos.writeLong(file.length());

        // Khối 3: Mở file để đọc
        fis = new FileInputStream(file);

        // Khối 4: Truyền tải dữ liệu file theo từng khúc
        byte[] buffer = new byte[4096];
        int read;
        long totalSent = 0;

        while ((read = fis.read(buffer)) > 0) {
          dos.write(buffer, 0, read);
          totalSent += read;

          // Log progress cho file lớn
          if (file.length() > 10 * 1024 * 1024 && totalSent % (1024 * 1024) == 0) { // Mỗi 1MB
            System.out.println("Đã gửi: " + (totalSent / (1024 * 1024)) + "MB / " + (file.length() / (1024 * 1024)) + "MB");
          }
        }

        // Đảm bảo tất cả dữ liệu được gửi
        dos.flush();

        // Thành công
        System.out.println("File gửi thành công: " + uniqueName + " (" + file.length() + " bytes)");
        return;

      } catch (IOException e) {
        lastException = e;
        System.err.println("Lần thử " + attempt + " thất bại: " + e.getMessage());
      } finally {
        // Cleanup tài nguyên
        try {
          if (fis != null) fis.close();
          if (dos != null) dos.close();
          if (s != null && !s.isClosed()) s.close();
        } catch (IOException ex) {
          System.err.println("Error closing resources: " + ex.getMessage());
        }
      }
    }

    // Tất cả lần thử đều thất bại
    throw lastException;
  }
}