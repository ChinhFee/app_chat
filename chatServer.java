/*
 * ChatServer.java
 *
 * Lớp máy chủ chat đa luồng xử lí nhiều kết nối client cùng lúc.
 * Hỗ trợ gửi tin nhắn phát sóng đến tất cả client kết nối và gửi tin nhắn riêng tư cho người dùng cụ thể.
 * Tích hợp với máy chủ file để chia sẻ tệp tin.
 *
 * Tính Năng Chính:
 * - Lắng nghe các kết nối client mới trên cổng 8888
 * - Quản lý danh sách các trình xử lí client đang hoạt động
 * - Hỗ trợ gửi tin nhắn phát sóng và tin nhắn riêng tư
 * - Tích hợp với máy chủ file chạy trên cổng 1988
 *
 * Tác Giả: [Tên của bạn]
 * Ngày: [Ngày hiện tại]
 */

import java.io.*;
import java.net.*;
import java.util.*;

/**
 * Lớp máy chủ chat chính chịu trách nhiệm quản lý kết nối client và định tuyến tin nhắn.
 */
public class chatServer {
  /**
   * Số cổng mà máy chủ chat lắng nghe các kết nối đến.
   */
  public static final int PORT = 8888;

  /**
   * Tập hợp dùng bộ nhớ chung chứa tất cả các luồng trình xử lí client đang hoạt động.
   * Tập hợp này cho phép phát sóng tin nhắn đến tất cả client kết nối
   * và định tuyến tin nhắn riêng tư.
   * Nó là an toàn với luồng vì các hoạt động trên HashSet được đồng bộ hóa bên ngoài.
   */
  public static Set<clientHandler> clientHandlers = new HashSet<>();

  /**
   * Phương thức main - khởi động máy chủ chat và bắt đầu lắng nghe kết nối client.
   *
   * Các Khối Chức Năng:
   * 1. Khởi Tạo Server: Khởi động máy chủ file trên luồng riêng và khởi tạo máy chủ chat.
   * 2. Xử Lí Kết Nối: Liên tục chấp nhận kết nối client mới và tạo luồng xử lí.
   * 3. Xử Lí Lỗi: Bắt và ghi lại bất kì ngoại lệ IO nào xảy ra trong quá trình hoạt động của server.
   *
   * @param args Tham số dòng lệnh (không được sử dụng trong việc triển khai này)
   */
  public static void main(String[] args) {
    System.out.println("====== CHAT SERVER ======");

    // Khối 1: Khởi tạo và khởi động máy chủ file
    // Máy chủ file chạy trên cổng 1988 và xử lí các hoạt động chia sẻ tệp
    // Nó chạy trên luồng riêng để tránh chặn luồng chính của máy chủ chat
    fileServer fs = new fileServer(1988);
    fs.start();

    // Khối 2: Khởi tạo máy chủ chat chính
    // Tạo ServerSocket để lắng nghe kết nối client mới trên cổng được chỉ định
    try (ServerSocket serverSocket = new ServerSocket(PORT)) {
      System.out.println("Server is running on port " + PORT + " and waiting for connections...");

      // Khối 3: Vòng lặp kết nối chính
      // Liên tục chấp nhận kết nối client mới
      while (true) {
        // Chờ kết nối client mới
        Socket clientSocket = serverSocket.accept();
        System.out.println("New client connected.");

        // Tạo trình xử lí client mới cho client vừa kết nối
        clientHandler handler = new clientHandler(clientSocket);

        // Thêm trình xử lí vào danh sách các trình xử lí đang hoạt động
        clientHandlers.add(handler);

        // Khởi động trình xử lí trên luồng mới để xử lí giao tiếp với client
        handler.start();
      }
    } catch (IOException e) {
      // Khối 4: Xử lí lỗi
      // Ghi lại bất kì lỗi nào xảy ra trong quá trình khởi tạo hoặc hoạt động của server
      System.err.println("Server initialization error: " + e.getMessage());
      e.printStackTrace();
    }
  }

  /**
   * Phát sóng tin nhắn đến tất cả client kết nối ngoại trừ người gửi.
   *
   * Phương thức này lặp qua tất cả trình xử lí client đang hoạt động và gửi tin nhắn
   * cho mỗi client ngoại trừ client của người gửi (để tránh lặp lại tin nhắn).
   *
   * @param message     Tin nhắn cần phát sóng
   * @param excludeUser Trình xử lí client của người gửi (bị loại khỏi phát sóng)
   */
  public static void broadcast(String message, clientHandler excludeUser) {
    for (clientHandler handler : clientHandlers) {
      if (handler != excludeUser) {
        handler.sendMessage(message);
      }
    }
  }

  /**
   * Gửi tin nhắn riêng tư cho một người dùng kết nối cụ thể.
   *
   * Phương thức này tìm kiếm qua tất cả trình xử lí client đang hoạt động để tìm người nhận
   * theo tên người dùng và gửi tin nhắn nếu tìm thấy.
   *
   * Các Khối Chức Năng:
   * 1. Tìm Kiếm Người Nhận: Lặp qua các client kết nối để tìm người dùng target
   * 2. Gửi Tin Nhắn: Gửi tin nhắn đến người nhận được tìm thấy
   * 3. Trả Về Trạng Thái: Trả về giá trị boolean chỉ thành công hay thất bại
   *
   * @param message      Tin nhắn riêng tư cần gửi
   * @param receiverName Tên người dùng của người nhận
   * @param sender       Trình xử lí client của người gửi (để sử dụng trong tương lai)
   * @return true nếu tin nhắn được gửi thành công, false nếu người nhận không online
   */
  public static boolean sendPrivateMessage(String message, String receiverName, clientHandler sender) {
    // Khối 1: Tìm kiếm người nhận
    for (clientHandler client : clientHandlers) {
      // Kiểm tra xem tên người dùng của client hiện tại có khớp với tên người nhận không
      if (client.getUsername().equals(receiverName)) {
        // Khối 2: Gửi tin nhắn cho người nhận
        client.sendMessage(message);
        // Khối 3: Trả về trạng thái thành công
        return true;
      }
    }
    // Khối 3: Trả về trạng thái thất bại nếu không tìm thấy người nhận
    return false;
  }

  /**
   * Xóa client bị ngắt kết nối khỏi danh sách trình xử lí đang hoạt động.
   *
   * Phương thức này được gọi khi một client ngắt kết nối để dọn dẹp danh sách
   * và ghi lại số lượng người dùng đang online.
   *
   * @param handler Trình xử lí client cần xóa
   */
  public static void removeClient(clientHandler handler) {
    clientHandlers.remove(handler);
    System.out.println("User disconnected. Online users: " + clientHandlers.size());
  }

  /**
   * Lưu trữ ánh xạ các tên nhóm với địa chỉ IP multicast được cấp phát.
   * Điều này cho phép xác định nhóm liên tục qua nhiều phiên chat.
   * Ví dụ: \"StudyGroup\" -> \"224.0.0.10\"
   */
  public static java.util.HashMap<String, String> groupMap = new java.util.HashMap<>();

  /**
   * Bộ đếm để cấp phát các địa chỉ IP multicast mới.
   * Bắt đầu từ 10 và tăng lên cho mỗi nhóm mới được tạo.
   * Phạm vi multicast lớp D hợp lệ: 224.0.0.0 đến 239.255.255.255
   */
  public static int nextIpSuffix = 10;

  /**
   * Cấp phát hoặc truy xuất địa chỉ IP multicast cho một nhóm.
   *
   * Các Khối Chức Năng:
   * 1. Tìm Kiếm Nhóm: Kiểm tra xem nhóm đã tồn tại hay chưa và trả về IP hiện tại
   * 2. Tạo Địa Chỉ: Tạo địa chỉ multicast mới nếu nhóm chưa tồn tại
   * 3. Đăng Ký: Lưu trữ ánh xạ và cập nhật bộ đếm
   * 4. Trả Về Kết Quả: Trả về địa chỉ IP multicast được cấp phát
   *
   * Phương thức an toàn với luồng để đảm bảo nhiều luồng có thể truy cập an toàn các hoạt động nhóm.
   *
   * @param groupName Tên của nhóm (ví dụ: \"StudyGroup\", \"ProjectTeam\")
   * @return Địa chỉ IP multicast được cấp phát định dạng \"224.0.0.X\"
   */
  public static synchronized String getOrCreateGroup(String groupName) {
    // Khối 1: Kiểm tra xem nhóm đã tồn tại hay chưa
    if (groupMap.containsKey(groupName)) {
      return groupMap.get(groupName);
    }
    // Khối 2: Tạo địa chỉ multicast mới
    // Sử dụng định dạng địa chỉ lớp D (224.0.0.0 đến 239.255.255.255)
    String newIp = "224.0.0." + nextIpSuffix;
    // Khối 3: Tăng bộ đếm cho nhóm tiếp theo và lưu trữ ánh xạ
    nextIpSuffix++; // Tăng lên 11, 12, 13, v.v. cho các nhóm tiếp theo
    groupMap.put(groupName, newIp);
    // Khối 4: Trả về địa chỉ IP được cấp phát
    return newIp;
  }
}