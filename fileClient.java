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
    
    // Khối 1: Thiết lập kết nối socket đến máy chủ file
    Socket s = new Socket(host, port);

    // Khối 2: Thiết lập luồng output dữ liệu cho truyền tải binary hiệu quả
    DataOutputStream dos = new DataOutputStream(s.getOutputStream());

    // [CẬP NHẬT MỚI]: Gửi Tên file đặc biệt và Dung lượng file trước
    dos.writeUTF(uniqueName);
    dos.writeLong(file.length());

    // Khối 3: Mở file để đọc từ hệ thống tệp cục bộ
    FileInputStream fis = new FileInputStream(file);

    // Khối 4: Truyền tải dữ liệu file theo từng khúc
    // Nâng cấp bộ đệm lên 4096 byte cho việc truyền tải nhanh hơn
    byte[] buffer = new byte[4096];
    int read;
    while ((read = fis.read(buffer)) > 0) {
      // Ghi các byte đã đọc vào luồng output mạng
      dos.write(buffer, 0, read);
    }

    // Khối 5: Dọn dẹp tài nguyên
    fis.close();
    dos.close();
    s.close();
  }
}