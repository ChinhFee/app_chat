
/*
 * FileClient.java
 *
 * Lớp này cung cấp chức năng gửi file qua socket mạng.
 * Nó thiết lập kết nối đến máy chủ file và truyền tải dữ liệu file theo từng khúc.
 * Được sử dụng cho khả năng chia sẻ file trong ứng dụng chat.
 *
 * Tính Năng Chính:
 * - Thiết lập kết nối socket đến máy chủ file
 * - Đọc file từ hệ thống tệp cục bộ
 * - Truyền tải dữ liệu file theo từng khúc 1024 byte
 * - Xử lí I/O file và I/O mạng
 *
 * Tác Giả: [Tên của bạn]
 * Ngày: [Ngày hiện tại]
 */

import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.Socket;

/**
 * FileClient xử lí các hoạt động truyền tải file phía client.
 * Nó cung cấp một phương thức tĩnh để gửi file đến máy chủ file từ xa.
 */
public class fileClient {

  /**
   * Gửi file đến máy chủ file từ xa qua kết nối socket TCP.
   *
   * Các Khối Chức Năng:
   * 1. Thiết Lập Kết Nối: Tạo kết nối socket đến máy chủ
   * 2. Thiết Lập Luồng: Khởi tạo luồng output cho truyền tải dữ liệu
   * 3. Đọc File: Mở và đọc file cục bộ cần gửi
   * 4. Truyền Tải Dữ Liệu: Gửi dữ liệu file theo từng khúc qua mạng
   * 5. Dọn Dẹp Tài Nguyên: Đóng tất cả các luồng và kết nối socket
   *
   * @param host     Tên host hoặc địa chỉ IP của máy chủ file
   * @param port     Số cổng mà máy chủ file đang lắng nghe
   * @param filePath Đường dẫn tuyệt đối hoặc tương đối đến file cần gửi
   * @throws IOException Nếu xảy ra lỗi I/O trong quá trình đọc file hoặc truyền tải mạng
   */
  public static void sendFile(String host, int port, String filePath) throws IOException {
    // Khối 1: Thiết lập kết nối socket đến máy chủ file
    Socket s = new Socket(host, port);

    // Khối 2: Thiết lập luồng output dữ liệu cho truyền tải binary hiệu quả
    DataOutputStream dos = new DataOutputStream(s.getOutputStream());

    // Khối 3: Mở file để đọc từ hệ thống tệp cục bộ
    FileInputStream fis = new FileInputStream(filePath);

    // Khối 4: Truyền tải dữ liệu file theo từng khúc
    // Sử dụng bộ đệm 1024 byte cho việc sử dụng bộ nhớ hiệu quả và truyền tải
    byte[] buffer = new byte[1024];
    int read;
    while ((read = fis.read(buffer)) > 0) {
      // Ghi các byte đã đọc vào luồng output mạng
      dos.write(buffer, 0, read);
    }

    // Khối 5: Dọn dẹp tài nguyên
    // Đóng các luồng theo thứ tự ngược lại với thứ tự tạo
    fis.close();
    dos.close();
    s.close();
  }
}
