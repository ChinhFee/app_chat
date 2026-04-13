/*
 * FileServer.java
 *
 * Lớp này triển khai một máy chủ tập tin đơn giản nhận các tệp từ khách hàng qua kết nối TCP.
 * Nó chạy như một luồng riêng biệt và lắng nghe các yêu cầu truyển tệp đến.
 * Máy chủ chấp nhận dữ liệu tệp và lưu nó vào một tệp cục bộ trên máy chủ.
 *
 * Các Tính Năng Chính:
 * - Lắng nghe trên một cổng được chỉ định cho các kết nối truyền tệp
 * - Nhận dữ liệu tệp theo từng khúc từ các khách hàng
 * - Lưu dữ liệu đã nhận vào một tệp cục bộ
 * - Chạy như một luồng nền bên cạnh máy chủ trò chuyện chính
 *
 * Tác Giả: [Tên của bạn]
 * Ngày: [Ngày Hiện Tại]
 */

import java.io.DataInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * FileServer xử lý các lần truyền tệp đến từ các khách hàng.
 * Nó mở rộng Thread để chạy cùng với máy chủ trò chuyện chính.
 */
public class fileServer extends Thread {
  /**
   * Socket máy chủ lắng nghe các kết nối truyền tệp đến.
   */
  private ServerSocket ss;

  /**
   * Hàm tạo cho FileServer.
   * Khởi tạo socket máy chủ trên cổng được chỉ định.
   *
   * @param port Số cổng để lắng nghe các lần truyền tệp
   */
  public fileServer(int port) {
    try {
      // Khối 1: Tạo socket máy chủ và ràng buộc với cổng được chỉ định
      ss = new ServerSocket(port);
      System.out.println("File Server is running on port: " + port);
    } catch (Exception e) {
      // Khối 2: In theo dõi ngăn xếp cho bất kỳ lỗi khởi tạo nào
      e.printStackTrace();
    }
  }

  /**
   * Phương thức run chính được thực thi bởi luồng.
   * Liên tục lắng nghe và xử lý các kết nối truyền tệp đến.
   *
   * Các Khối Chức Năng:
   * 1. Chấp Nhận Kết Nối: Chờ các kết nối của khách hàng
   * 2. Xử Lý Tệp: Xử lý từng yêu cầu chuyển tệp
   * 3. Xử Lý Lỗi: Bắt và ghi lại bất kỳ ngoại lệ nào trong quá trình hoạt động
   */
  public void run() {
    // Khối 1: Vòng lặp máy chủ chính - liên tục chấp nhận các kết nối
    while (true) {
      try {
        // Khối 2: Chấp nhận kết nối khách hàng đến
        Socket clientSock = ss.accept();

        // Khối 3: Xử lý việc chuyển tệp từ khách hàng đã kết nối
        saveFile(clientSock);
      } catch (Exception e) {
        // Khối 4: Xử lý bất kỳ ngoại lệ nào trong quá trình kết nối hoặc xử lý tệp
        e.printStackTrace();
      }
    }
  }

  /**
   * Lưu một tệp nhận được từ kết nối socket khách hàng.
   * Đọc dữ liệu từ khách hàng theo từng khúc và ghi nó vào một tệp cục bộ.
   *
   * Các Khối Chức Năng:
   * 1. Thiết Lập Luồng: Khởi tạo luồng đầu vào và đầu ra
   * 2. Nhận Dữ Liệu: Đọc dữ liệu tệp theo từng khúc từ mạng
   * 3. Ghi Tệp: Ghi dữ liệu nhận được vào tệp cục bộ
   * 4. Dọn Dẹp: Đóng tất cả các luồng
   * 5. Xác Nhận: Ghi lại việc nhận tệp thành công
   *
   * @param clientSock Socket kết nối với khách hàng gửi tệp
   * @throws IOException Nếu xảy ra lỗi I/O trong quá trình truyền dữ liệu
   */
  private void saveFile(Socket clientSock) throws IOException {
    // Khối 1: Thiết lập luồng đầu vào dữ liệu để đọc dữ liệu nhị phân từ khách hàng
    DataInputStream dis = new DataInputStream(clientSock.getInputStream());

    // Khối 2: Thiết lập luồng đầu ra tệp để ghi dữ liệu vào tệp cục bộ
    // Lưu ý: Tên tệp hiện được mã hóa cứng là "received_file.jpg"
    // Trong một hệ thống sản xuất, tên tệp có thể được gửi bởi khách hàng hoặc được xác định
    // bởi dấu thời gian hoặc quy ước đặt tên khác
    FileOutputStream fos = new FileOutputStream("received_file.jpg");

    // Khối 3: Nhận và lưu dữ liệu tệp
    // Sử dụng bộ đệm 1024 byte để truyền dữ liệu hiệu quả
    byte[] buffer = new byte[1024];
    int read = 0;

    // Đọc các khúc dữ liệu cho đến khi kết thúc luồng
    while ((read = dis.read(buffer)) > 0) {
      // Ghi các byte nhận được vào tệp
      fos.write(buffer, 0, read);
    }

    // Khối 4: Dọn dẹp tài nguyên
    fos.close();
    dis.close();

    // Khối 5: Ghi lại việc nhận tệp thành công
    System.out.println("File received successfully!");
  }

  /**
   * Phương thức main để kiểm test độc lập máy chủ tập tin.
   * Tạo và khởi động một thể hiện máy chủ tập tin trên cổng 1988.
   * Đây được sử dụng để kiểm test máy chủ tập tin một cách độc lập.
   *
   * @param args Các đối số dòng lệnh (không được sử dụng)
   */
  public static void main(String[] args) {
    // Tạo và khởi động máy chủ tập tin cho mục đích kiểm test
    new fileServer(1988).start();
  }
}
