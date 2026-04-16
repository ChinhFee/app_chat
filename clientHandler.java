/*
 * ClientHandler.java
 *
 * Lớp này xử lý giao tiếp với một client kết nối duy nhất trong máy chủ chat đa luồng.
 * Mỗi kết nối client được quản lí bởi luồng của nó riêng, cho phép xử lí đồng thời nhiều client.
 * Trình xử lí xử lí các lệnh khác nhau như đăng ký, đăng nhập, gửi tin nhắn phát sóng và tin nhắn riêng tư.
 * Nó cũng tích hợp với thông báo email cho người dùng offline.
 *
 * Tính Năng Chính:
 * - Quản lí kết nối socket client và các luồng I/O
 * - Xử lí các lệnh client (REGISTER, LOGIN, CHATALL, CHATPRIVATE, JOINGROUP)
 * - Xử lí xác thực người dùng và quản lí tài khoản
 * - Hỗ trợ tin nhắn phát sóng và tin nhắn riêng tư
 * - Gửi thông báo email cho người dùng offline về tin nhắn riêng
 * - Quản lí ngắt kết nối và dọn dẹp tài nguyên của client
 *
 * Tác Giả: [Tên của bạn]
 * Ngày: [Ngày hiện tại]
 */

import java.io.*;
import java.net.*;
import java.util.Scanner;

/**
 * ClientHandler quản lí giao tiếp cho một client kết nối duy nhất.
 * Mỗi đối tượng chạy trong luồng của riêng nó để xử lí các yêu cầu client đồng
 * thời.
 */
class clientHandler extends Thread {
  /**
   * Socket đại diện cho kết nối đến client.
   */
  private Socket socket;

  /**
   * PrintWriter cho gửi tin nhắn đến client.
   */
  private PrintWriter out;

  /**
   * BufferedReader để nhận tin nhắn từ client.
   */
  private BufferedReader in;

  /**
   * Tên người dùng của client kết nối. Mặc định là "Anonymous" cho đến khi đăng
   * nhập.
   */
  private String username = "Anonymous";

  /**
   * Cờ cho biết liệu client đã đăng nhập thành công hay chưa.
   */
  private boolean isLoggedIn = false;

  /**
   * Constructor cho ClientHandler.
   *
   * @param socket Socket kết nối đến client
   */
  public clientHandler(Socket socket) {
    this.socket = socket;
  }

  /**
   * Phương thức run chính được thực thi bởi luồng.
   * Phương thức này xử lí toàn bộ vòng đời phiên client.
   *
   * Các Khối Chức Năng:
   * 1. Khởi Tạo Luồng: Thiết lập các luồng input/output với mã hóa UTF-8
   * 2. Chào Mừng Client: Gửi tin nhắn chào mừng và cú pháp lệnh cho client mới
   * 3. Vòng Lặp Xử Lí Lệnh: Liên tục đọc và xử lí các lệnh client
   * 4. Xử Lí Ngắt Kết Nối: Xử lí việc ngắt kết nối có chủ đích (QUIT, LOGOUT,
   * LOOOUT)
   * 5. Xử Lí Lỗi: Bắt ngoại lệ IO cho ngắt kết nối bất ngờ
   * 6. Dọn Dẹp: Xóa client khỏi danh sách hoạt động, thông báo cho người khác,
   * đóng socket
   */
  @Override
  public void run() {
    try {
      // Khối 1: Khởi tạo các luồng input và output
      // Sử dụng mã hóa UTF-8 để hỗ trợ các ký tự quốc tế
      in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
      out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);

      // Khối 2: Gửi tin nhắn chào mừng và hướng dẫn sử dụng
      out.println("MESSAGE Welcome! There are currently " + chatServer.clientHandlers.size() + " users in the room.");
      out.println("MESSAGE REGISTER syntax: REGISTER <username>|<password>|<email>");
      out.println("MESSAGE LOGIN syntax: LOGIN <username>|<password>");

      String clientMessage;

      // Khối 3: Vòng lặp xử lí lệnh chính
      while ((clientMessage = in.readLine()) != null) {
        // Khối 4: Kiểm tra các lệnh ngắt kết nối
        // Xử lí các dạng khác nhau của lệnh thoát như được chỉ định trong giao thức
        if (clientMessage.equalsIgnoreCase("QUIT") ||
            clientMessage.equalsIgnoreCase("LOGOUT") ||
            clientMessage.equalsIgnoreCase("LOOUT")) {
          System.out.println(username + " intentionally sent exit command.");
          break; // Thoát vòng lặp để tiếp tục với dọn dẹp
        }

        System.out.println("Received command: " + clientMessage);

        // Khối 5: Xử lí các lệnh client
        if (clientMessage.startsWith("REGISTER ")) {
          handleRegister(clientMessage);
        } else if (clientMessage.startsWith("LOGIN ")) {
          handleLogin(clientMessage);
        } else if (clientMessage.startsWith("CHATALL ")) {
          handleBroadcast(clientMessage);
        } else if (clientMessage.startsWith("CHATPRIVATE ")) {
          handlePrivateChat(clientMessage);
        } 
        // [CẬP NHẬT]: Trả về danh sách người dùng đang online
        else if (clientMessage.equals("GET_ONLINE_USERS")) {
          StringBuilder onlineList = new StringBuilder("ONLINE_LIST ");
          for (clientHandler client : chatServer.clientHandlers) {
            if (client.isLoggedIn) {
              onlineList.append(client.getUsername()).append(",");
            }
          }
          out.println(onlineList.toString());
        } 
        else if (clientMessage.startsWith("CREATEGROUP ") || clientMessage.startsWith("JOINGROUP ")) {
          String[] parts = clientMessage.split(" ");

          if (parts.length >= 2) {
            String groupName = parts[1];

            // Yêu cầu cấp phát địa chỉ IP multicast từ trình quản lí nhóm server
            String multicastIp = chatServer.getOrCreateGroup(groupName);

            // Gửi lệnh đặc biệt cho client để client tự động join nhóm multicast
            out.println("MULTICAST_JOIN " + multicastIp + " 8888");
            out.println("MESSAGE Successfully joined group [" + groupName + "].");
          } else {
            out.println("MESSAGE Invalid syntax! Example: JOINGROUP StudyGroup");
          }
        } else {
          out.println("MESSAGE Unknown command. Use REGISTER, LOGIN, CHATALL, or CHATPRIVATE.");
        }
      }
    } catch (IOException e) {
      // Khối 6: Xử lí lỗi kết nối
      // Bắt các lỗi mạng hoặc ngắt kết nối bất ngờ của client
      System.err.println("Connection error from client " + username + ": " + e.getMessage());
      e.printStackTrace();
    } finally {
      // Khối 7: Các hoạt động dọn dẹp (luôn được thực thi)
      // 1. Xóa người dùng khỏi danh sách client đang hoạt động
      chatServer.removeClient(this);

      // 2. Thông báo cho người dùng còn lại nếu client đã đăng nhập
      if (isLoggedIn) {
        chatServer.broadcast("MESSAGE " + username + " has left the chat room.", this);
        // [CẬP NHẬT]: Thông báo cho toàn Server biết có người vừa thoát
        chatServer.broadcast("USER_LEFT " + username, this);
      }

      // 3. Đóng socket để giải phóng tài nguyên mạng
      try {
        if (socket != null && !socket.isClosed()) {
          socket.close();
          System.out.println("Service thread closed for client: " + username);
        }
      } catch (IOException e) {
        System.err.println("Error closing socket for client " + username + ": " + e.getMessage());
        e.printStackTrace();
      }
    }
  } // End of run() method

  /**
   * Gửi tin nhắn thô đến client này.
   *
   * @param message Tin nhắn cần gửi
   */
  public void sendMessage(String message) {
    out.println(message);
  }

  // =======================
  // CÁC PHƯƠNG THỨC XỬ LÍ LỆNH
  // =======================

  /**
   * Xử lí lệnh REGISTER để tạo tài khoản người dùng mới.
   *
   * Các Khối Chức Năng:
   * 1. Phân Tích Lệnh: Trích xuất tên người dùng, mật khẩu và email từ lệnh
   * 2. Xác Thực: Kiểm tra cú pháp lệnh và các tham số
   * 3. Đăng Ký: Cố gắng đăng ký người dùng vào hệ thống
   * 4. Phản Hồi: Gửi tin nhắn thành công hoặc lỗi cho client
   *
   * @param clientMessage Chuỗi lệnh REGISTER đầy đủ
   */
  private void handleRegister(String clientMessage) {
    // Khối 1: Phân tích các tham số lệnh
    String[] parts = clientMessage.substring(9).split("\\|");

    if (parts.length == 3) {
      String user = parts[0];
      String pass = parts[1];
      String email = parts[2];

      // Khối 2: Cố gắng đăng ký người dùng
      if (registerUser(user, pass, email)) {
        // Khối 3: Gửi phản hồi thành công
        out.println("MESSAGE Registration successful! Please type LOGIN to log in.");
      } else {
        // Khối 4: Gửi phản hồi lỗi nếu tên người dùng đã tồn tại
        out.println("MESSAGE Error: Username already exists.");
      }
    } else {
      // Khối 4: Gửi phản hồi lỗi nếu cú pháp sai
      out.println("MESSAGE Invalid syntax! Example: REGISTER JohnDoe|123|john@example.com");
    }
  }

  /**
   * Xử lí lệnh LOGIN để xác thực người dùng.
   *
   * Các Khối Chức Năng:
   * 1. Phân Tích Lệnh: Trích xuất tên người dùng và mật khẩu từ lệnh
   * 2. Xác Thực: Kiểm tra cú pháp lệnh và các tham số
   * 3. Xác thực Tài Khoản: Xác minh thông tin đăng nhập so với dữ liệu được lưu
   * trữ
   * 4. Đăng Nhập Thành Công: Cập nhật trạng thái client và thông báo cho người
   * dùng khác
   * 5. Phản Hồi: Gửi tin nhắn thành công hoặc lỗi cho client
   *
   * @param clientMessage Chuỗi lệnh LOGIN đầy đủ
   */
  private void handleLogin(String clientMessage) {
    // Khối 1: Phân tích các tham số lệnh
    String[] parts = clientMessage.substring(6).split("\\|");

    if (parts.length == 2) {
      String user = parts[0];
      String pass = parts[1];

      // Khối 2: Cố gắng xác xác thực người dùng
      if (checkLogin(user, pass)) {
        // Khối 3: Cập nhật trạng thái client khi đăng nhập thành công
        username = user;
        isLoggedIn = true;

        // Khối 4: Gửi phản hồi thành công và thông báo cho người dùng khác
        out.println("MESSAGE Login successful! Welcome " + username + ".");
        chatServer.broadcast("MESSAGE " + username + " has joined the chat room.", this);
        
        // [CẬP NHẬT]: Thông báo cho toàn Server biết có người mới đăng nhập
        chatServer.broadcast("USER_JOINED " + username, this);

      } else {
        // Khối 5: Gửi phản hồi lỗi nếu thông tin đăng nhập sai
        out.println("MESSAGE Username or password is incorrect.");
      }
    } else {
      // Khối 5: Gửi phản hồi lỗi nếu cú pháp sai
      out.println("MESSAGE Invalid syntax! Example: LOGIN JohnDoe|123");
    }
  }

  /**
   * Xử lí lệnh CHATALL để phát sóng tin nhắn đến tất cả người dùng.
   *
   * Các Khối Chức Năng:
   * 1. Kiểm Tra Xác Thực: Đảm bảo người dùng đã đăng nhập
   * 2. Trích Xuất Tin Nhắn: Phân tích nội dung tin nhắn từ lệnh
   * 3. Phát Sóng: Gửi tin nhắn đến tất cả client kết nối ngoại trừ người gửi
   *
   * @param clientMessage Chuỗi lệnh CHATALL đầy đủ
   */
  private void handleBroadcast(String clientMessage) {
    // Khối 1: Kiểm tra xem người dùng đã xác thực
    if (!isLoggedIn) {
      out.println("MESSAGE You must LOGIN before chatting.");
      return;
    }

    // Khối 2: Trích xuất nội dung tin nhắn
    String msg = clientMessage.substring(8);

    // Khối 3: Phát sóng tin nhắn
    chatServer.broadcast("BROADCAST " + username + ": " + msg, this);
  }

  /**
   * Xử lí lệnh CHATPRIVATE để gửi tin nhắn riêng tư.
   *
   * Các Khối Chức Năng:
   * 1. Kiểm Tra Xác Thực: Đảm bảo người dùng đã đăng nhập
   * 2. Phân Tích Tin Nhắn: Trích xuất người nhận và nội dung tin nhắn
   * 3. Gửi Online: Cố gắng gửi tin nhắn qua TCP nếu người nhận đang online
   * 4. Thông Báo Offline: Gửi thông báo email nếu người nhận đang offline
   * 5. Phản Hồi: Thông báo cho người gửi về trạng thái gửi
   *
   * @param clientMessage Chuỗi lệnh CHATPRIVATE đầy đủ
   */
  private void handlePrivateChat(String clientMessage) {
    // Khối 1: Kiểm tra xem người dùng đã xác thực
    if (!isLoggedIn) {
      out.println("MESSAGE You must LOGIN before chatting.");
      return;
    }

    // Khối 2: Phân tích người nhận và tin nhắn
    String data = clientMessage.substring(12);
    int separatorIndex = data.indexOf("|");
    if (separatorIndex != -1) {
      String receiver = data.substring(0, separatorIndex);
      String msg = data.substring(separatorIndex + 1);

      // Khối 3: Cố gắng gửi tin nhắn riêng tư
      boolean messageSent = chatServer.sendPrivateMessage("PRIVATE từ " + username + ": " + msg, receiver, this);

      if (messageSent) {
        // Khối 4: Người nhận đang online - tin nhắn được gửi thành công
        out.println("MESSAGE Private message sent to " + receiver + ".");
      } else {
        // Khối 5: Người nhận đang offline - gửi thông báo email
        out.println("MESSAGE User " + receiver + " is offline. Sending email notification...");

        String emailDich = getEmailFromUsersFile(receiver);

        if (emailDich != null) {
          // Gửi thông báo email bằng tiện ích mail
          mailUtil.sendOfflineNotification(emailDich, "You have a new message from " + username + ": " + msg);
          out.println("MESSAGE Email notification sent successfully to " + receiver + "!");
        } else {
          // Xử lí trường hợp tên người nhận không tồn tại
          out.println("MESSAGE Error: User " + receiver + " does not exist.");
        }
      }
    } else {
      // Khối 5: Gửi lỗi cho cú pháp sai
      out.println("MESSAGE Invalid syntax! Example: CHATPRIVATE Bob|Hello there");
    }
  }

  // =======================
  // CÁC PHƯƠNG THỨC QUẢN LÍ TÀI KHOẢN NGƯỜI DÙNG
  // =======================

  /**
   * Đăng ký tài khoản người dùng mới vào hệ thống.
   *
   * Các Khối Chức Năng:
   * 1. Truy Cập File: Mở hoặc tạo file users.txt
   * 2. Kiểm Tra Trùng Lặp: Quét người dùng hiện có để ngăn tên người dùng trùng
   * 3. Thêm Người Dùng: Nối dữ liệu người dùng mới vào file
   * 4. Xử Lí Lỗi: Xử lý ngoại lệ I/O của file
   *
   * @param user  Tên người dùng mong muốn
   * @param pass  Mật khẩu
   * @param email Địa chỉ email
   * @return true nếu đăng ký thành công, false nếu tên người dùng đã tồn tại hoặc
   * xảy ra lỗi
   */
  private boolean registerUser(String user, String pass, String email) {
    try {
      // Khối 1: Chuẩn bị file người dùng
      File file = new File("users.txt");
      file.createNewFile();

      // Khối 2: Kiểm tra tên người dùng đã tồn tại
      Scanner scanner = new Scanner(file);
      while (scanner.hasNextLine()) {
        String line = scanner.nextLine();
        if (line.startsWith(user + "|")) {
          scanner.close();
          return false; // Tên người dùng đã tồn tại
        }
      }
      scanner.close();

      // Khối 3: Thêm người dùng mới vào file
      FileWriter fw = new FileWriter(file, true);
      fw.write(user + "|" + pass + "|" + email + "\n");
      fw.close();

      return true; // Đăng ký thành công
    } catch (IOException e) {
      // Khối 4: Xử lí lỗi I/O của file
      System.err.println("File write error in registerUser: " + e.getMessage());
      e.printStackTrace();
      return false;
    }
  }

  /**
   * Xác minh thông tin đăng nhập của người dùng so với dữ liệu được lưu trữ.
   *
   * Các Khối Chức Năng:
   * 1. Truy Cập File: Mở file users.txt
   * 2. Kiểm Tra Thông Tin Xác Thực: Quét file tìm tên người dùng và mật khẩu khớp
   * 3. Trả Về Kết Quả: Trả về kết quả xác thực
   * 4. Xử Lí Lỗi: Xử lý ngoại lệ I/O của file
   *
   * @param user Tên người dùng để kiểm tra
   * @param pass Mật khẩu để xác minh
   * @return true nếu thông tin xác thực hợp lệ, false nếu không
   */
  private boolean checkLogin(String user, String pass) {
    try {
      // Khối 1: Truy cập file người dùng
      File file = new File("users.txt");
      if (!file.exists()) {
        return false; // File người dùng không tồn tại
      }

      // Khối 2: Tìm kiếm thông tin xác thực khớp
      Scanner scanner = new Scanner(file);
      String target = user + "|" + pass + "|";

      while (scanner.hasNextLine()) {
        String line = scanner.nextLine();
        if (line.startsWith(target)) {
          scanner.close();
          return true; // Thông tin xác thực khớp
        }
      }
      scanner.close();
    } catch (IOException e) {
      // Khối 4: Xử lí lỗi I/O của file
      System.err.println("File read error in checkLogin: " + e.getMessage());
      e.printStackTrace();
    }
    return false; // Thông tin xác thực không được tìm thấy hoặc xảy ra lỗi
  }

  /**
   * Lấy tên người dùng của trình xử lí client này.
   *
   * @return Chuỗi tên người dùng
   */
  public String getUsername() {
    return this.username;
  }

  /**
   * Truy xuất địa chỉ email của một người dùng từ file người dùng.
   *
   * Các Khối Chức Năng:
   * 1. Truy Cập File: Mở file users.txt
   * 2. Tìm Kiếm Người Dùng: Quét file tìm tên người dùng target
   * 3. Trích Xuất Email: Trả về email nếu tìm thấy người dùng
   * 4. Xử Lí Lỗi: Xử lý ngoại lệ I/O của file
   *
   * @param targetUser Tên người dùng để tìm kiếm
   * @return Địa chỉ email nếu tìm thấy, null nếu không
   */
  private String getEmailFromUsersFile(String targetUser) {
    try {
      // Khối 1: Truy cập file người dùng
      File file = new File("users.txt");
      if (!file.exists())
        return null; // File không tồn tại

      // Khối 2: Tìm kiếm người dùng target
      Scanner scanner = new Scanner(file);
      while (scanner.hasNextLine()) {
        String line = scanner.nextLine();
        // Phân tích định dạng dòng: User|Pass|Email
        String[] parts = line.split("\\|");

        // Kiểm tra xem dòng này có khớp với tên người dùng target không
        if (parts.length == 3 && parts[0].equals(targetUser)) {
          scanner.close();
          return parts[2]; // Trả về email (phần tử thứ 3, chỉ mục 0-based)
        }
      }
      scanner.close();
    } catch (IOException e) {
      // Khối 4: Xử lí lỗi I/O của file
      System.err.println("File read error in getEmailFromUsersFile: " + e.getMessage());
      e.printStackTrace();
    }
    return null; // Người dùng không được tìm thấy hoặc xảy ra lỗi
  }
}