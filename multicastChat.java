/*
 * MulticastChat.java
 *
 * Lớp này triển khai một ứng dụng trò chuyện nhóm dựa trên multicast sử dụng các socket multicast UDP.
 * Nhiều người dùng có thể tham gia cùng một nhóm multicast và trao đổi tin nhắn theo thời gian thực.
 * Các tin nhắn được gửi đến tất cả các thành viên nhóm cùng một lúc bằng cách sử dụng địa chỉ multicast IP.
 * Lớp này có thể được sử dụng cùng với ChatServer chính cho các tính năng trò chuyện nhóm.
 *
 * Các Tính Năng Chính:
 * - Tham gia một nhóm multicast được chỉ định trên một cổng nhất định
 * - Hỗ trợ phát sóng tin nhắn theo thời gian thực cho tất cả các thành viên nhóm
 * - Chạy người gửi và người nhận trong các luồng riêng biệt để hoạt động đồng thời
 * - Cho phép người dùng đặt tên hiển thị để nhận dạng tin nhắn
 * - Cung cấp cơ chế thoát sạch sẽ với thông báo rời khỏi nhóm
 * - Xử lý nhiều trò chuyện nhóm với các địa chỉ multicast khác nhau
 *
 * Chi Tiết Kỹ Thuật:
 * - Sử dụng MulticastSocket cho giao tiếp multicast UDP
 * - Sử dụng DatagramPacket cho gói tin nhắn
 * - Chạy người nhận trong luồng nền để tránh chặn đầu vào
 * - Hỗ trợ kích thước bộ đệm linh hoạt cho nội dung tin nhắn
 *
 * Tác Giả: [Tên của bạn]
 * Ngày: [Ngày Hiện Tại]
 */

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.Scanner;

/**
 * MulticastChat cung cấp chức năng cho giao tiếp trò chuyện nhóm sử dụng
 * multicast IP.
 * Người dùng có thể tham gia các nhóm multicast và trao đổi tin nhắn với tất cả
 * các thành viên khác theo thời gian thực.
 */
public class multicastChat {

  /**
   * Bắt đầu một phiên trò chuyện multicast cho một nhóm.
   * Phương thức này xử lý cả tin nhắn đến và tin nhắn đi cho một nhóm multicast.
   *
   * Các Khối Chức Năng:
   * 1. Thiết Lập Người Dùng: Khởi tạo bộ quét cho đầu vào người dùng
   * 2. Thiết Lập Mạng: Tạo socket multicast và tham gia nhóm được chỉ định
   * 3. Bắt Đầu Phiên: Ghi lại kết nối thành công tới nhóm multicast
   * 4. Luồng Nhận: Bắt đầu luồng nền để nhận tin nhắn từ nhóm
   * 5. Vòng Lặp Gửi: Vòng lặp chính để đọc đầu vào người dùng và truyền tin nhắn
   * 6. Xử Lý Thoát: Xử lý lệnh QUIT và tắt máy sạch sẽ
   *
   * @param ipAddress Địa chỉ IP nhóm multicast (ví dụ: 224.0.0.3)
   * @param port      Số cổng cho giao tiếp multicast
   * @param username  Tên hiển thị của người dùng hiện tại trong nhóm
   */
  public static void startChat(String ipAddress, int port, String username) {
    try {
      // Khối 1: Khởi tạo bộ quét đầu vào của người dùng
      Scanner scanner = new Scanner(System.in);

      // Khối 2: Thiết lập mạng multicast
      // Lấy địa chỉ nhóm multicast và tạo socket
      InetAddress address = InetAddress.getByName(ipAddress);
      MulticastSocket socket = new MulticastSocket(port);

      // Tham gia nhóm multicast để nhận tin nhắn từ các thành viên nhóm khác
      socket.joinGroup(address);
      // Khối 3: Ghi lại kết nối thành công
      System.out.println("====== JOINED GROUP CHAT (" + ipAddress + ") ======");
      System.out.println("Group port: " + port + " | Your username: " + username);
      System.out.println("Type 'QUIT' to exit the group.");

      // Khối 4: Bắt đầu luồng nhận cho tin nhắn đến
      // Đây chạy ở nền để nhận tin nhắn từ các người dùng khác
      Thread receiverThread = new Thread(() -> {
        byte[] buf = new byte[1048]; // Bộ đệm để nhận dữ liệu
        while (true) {
          try {
            // Tạo gói tin để nhận dữ liệu từ nhóm multicast
            DatagramPacket msgPacket = new DatagramPacket(buf, buf.length);
            socket.receive(msgPacket);

            // Chuyển đổi các byte nhận được thành chuỗi
            String msg = new String(buf, 0, msgPacket.getLength());

            // Hiển thị tin nhắn nếu không được gửi bởi người dùng hiện tại (tránh lặp lại
            // tin nhắn của chính mình)
            if (!msg.startsWith(username + ": ")) {
              System.out.println("\n" + msg.trim());
            }
          } catch (IOException e) {
            // Lỗi kết nối hoặc socket đã đóng
            break;
          }
        }
      });
      receiverThread.start();

      // Khối 5: Vòng lặp gửi chính
      // Liên tục đọc đầu vào người dùng và gửi tin nhắn cho nhóm
      while (true) {
        String message = scanner.nextLine();

        // Khối 6: Xử lý lệnh thoát
        if (message.equalsIgnoreCase("QUIT")) {
          // Rời khỏi nhóm multicast
          socket.leaveGroup(address);
          socket.close();
          System.out.println("You have left the group chat.");
          break;
        }

        // Định dạng tin nhắn với tiền tố tên người dùng để nhận dạng
        String formattedMessage = username + ": " + message;
        byte[] sendData = formattedMessage.getBytes();

        // Tạo và gửi gói datagram cho nhóm multicast
        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, address, port);
        socket.send(sendPacket);
      }
    } catch (IOException ex) {
      // Xử lý lỗi mạng hoặc I/O
      System.out.println("Error in multicast chat: " + ex.getMessage());
      ex.printStackTrace();
    }
  }
}