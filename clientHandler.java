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
 * - Xử lí các lệnh client (REGISTER, LOGIN, CHATALL, CHATPRIVATE, JOINGROUP, UPDATE_PROFILE)
 * - Xử lí xác thực người dùng và quản lí tài khoản (Đã áp dụng đồng bộ hóa Thread-Safe)
 * - Hỗ trợ tin nhắn phát sóng và tin nhắn riêng tư
 * - Gửi thông báo email chuyên nghiệp cho người dùng offline về tin nhắn riêng
 * - Quản lí ngắt kết nối và dọn dẹp tài nguyên của client
 * - Tích hợp Hàng đợi tin nhắn Offline: Lưu tin nhắn khi ngoại tuyến và tự động đẩy khi đăng nhập
 *
 * Tác Giả: [Tên của bạn]
 * Ngày: [Ngày hiện tại]
 */

import java.io.*;
import java.net.*;
import java.util.Scanner;
import java.util.ArrayList;
import java.util.List;

class clientHandler extends Thread {
  // Khóa đồng bộ hóa an toàn đa luồng cho tệp users.txt
  private static final Object FILE_LOCK = new Object();

  // Khóa đồng bộ riêng cho tệp lưu tin nhắn offline
  private static final Object OFFLINE_LOCK = new Object();

  private Socket socket;
  private PrintWriter out;
  private BufferedReader in;
  private String username = "Anonymous";
  private boolean isLoggedIn = false;

  public clientHandler(Socket socket) {
    this.socket = socket;
  }

  @Override
  public void run() {
    try {
      in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
      out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);

      out.println("MESSAGE Welcome! There are currently " + chatServer.clientHandlers.size() + " users in the room.");
      out.println("MESSAGE REGISTER syntax: REGISTER <username>|<password>|<email>");
      out.println("MESSAGE LOGIN syntax: LOGIN <username>|<password>");

      String clientMessage;

      while ((clientMessage = in.readLine()) != null) {
        if (clientMessage.equalsIgnoreCase("QUIT") ||
            clientMessage.equalsIgnoreCase("LOGOUT") ||
            clientMessage.equalsIgnoreCase("LOOUT")) {
          System.out.println(username + " intentionally sent exit command.");
          break; 
        }

        System.out.println("Received command: " + clientMessage);

        if (clientMessage.startsWith("REGISTER ")) {
          handleRegister(clientMessage);
        } else if (clientMessage.startsWith("LOGIN ")) {
          handleLogin(clientMessage);
        } else if (clientMessage.startsWith("CHATALL ")) {
          handleBroadcast(clientMessage);
        } else if (clientMessage.startsWith("CHATPRIVATE ")) {
          handlePrivateChat(clientMessage);
        } 
        // [MỚI]: Bắt lệnh cập nhật thông tin cá nhân
        else if (clientMessage.startsWith("UPDATE_PROFILE ")) {
          handleUpdateProfile(clientMessage);
        }
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
            String multicastIp = chatServer.getOrCreateGroup(groupName);
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
      System.err.println("Connection error from client " + username + ": " + e.getMessage());
      e.printStackTrace();
    } finally {
      chatServer.removeClient(this);
      if (isLoggedIn) {
        chatServer.broadcast("MESSAGE " + username + " has left the chat room.", this);
        chatServer.broadcast("USER_LEFT " + username, this);
      }
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
  }

  public void sendMessage(String message) {
    out.println(message);
  }

  // =======================
  // CÁC PHƯƠNG THỨC XỬ LÍ LỆNH
  // =======================

  private void handleRegister(String clientMessage) {
    String[] parts = clientMessage.substring(9).split("\\|");

    if (parts.length == 3) {
      String user = parts[0];
      String pass = parts[1];
      String email = parts[2];

      if (registerUser(user, pass, email)) {
        out.println("MESSAGE Registration successful! Please type LOGIN to log in.");
      } else {
        out.println("MESSAGE Error: Username already exists.");
      }
    } else {
      out.println("MESSAGE Invalid syntax! Example: REGISTER JohnDoe|123|john@example.com");
    }
  }

  private void handleLogin(String clientMessage) {
    String[] parts = clientMessage.substring(6).split("\\|");

    if (parts.length == 2) {
      String user = parts[0];
      String pass = parts[1];

      if (checkLogin(user, pass)) {
        username = user;
        isLoggedIn = true;

        out.println("MESSAGE Login successful! Welcome " + username + ".");
        chatServer.broadcast("MESSAGE " + username + " has joined the chat room.", this);
        chatServer.broadcast("USER_JOINED " + username, this);

        deliverOfflineMessages(username);

      } else {
        out.println("MESSAGE Username or password is incorrect.");
      }
    } else {
      out.println("MESSAGE Invalid syntax! Example: LOGIN JohnDoe|123");
    }
  }

  private void handleBroadcast(String clientMessage) {
    if (!isLoggedIn) {
      out.println("MESSAGE You must LOGIN before chatting.");
      return;
    }

    String msg = clientMessage.substring(8);
    chatServer.broadcast("BROADCAST " + username + ": " + msg, this);
  }

  private void handlePrivateChat(String clientMessage) {
    if (!isLoggedIn) {
      out.println("MESSAGE You must LOGIN before chatting.");
      return;
    }

    String data = clientMessage.substring(12);
    int separatorIndex = data.indexOf("|");
    if (separatorIndex != -1) {
      String receiver = data.substring(0, separatorIndex);
      String msg = data.substring(separatorIndex + 1);

      boolean messageSent = chatServer.sendPrivateMessage("PRIVATE từ " + username + ": " + msg, receiver, this);

      if (messageSent) {
        out.println("MESSAGE Private message sent to " + receiver + ".");
      } else {
        out.println("MESSAGE User " + receiver + " is offline. Sending email notification...");

        String emailDich = getEmailFromUsersFile(receiver);

        if (emailDich != null) {
          mailUtil.sendOfflineNotification(emailDich, username, msg);
          saveOfflineMessage(receiver, username, msg);
          out.println("MESSAGE Professional email notification sent successfully to " + receiver + "!");
        } else {
          out.println("MESSAGE Error: User " + receiver + " does not exist.");
        }
      }
    } else {
      out.println("MESSAGE Invalid syntax! Example: CHATPRIVATE Bob|Hello there");
    }
  }

  // [MỚI]: Phương thức xử lý lệnh UPDATE_PROFILE từ Client
  private void handleUpdateProfile(String clientMessage) {
    if (!isLoggedIn) {
      out.println("MESSAGE Bạn phải đăng nhập để cập nhật thông tin.");
      return;
    }

    // Cắt bỏ phần "UPDATE_PROFILE " (độ dài 15 ký tự)
    String data = clientMessage.substring(15); 
    int separatorIndex = data.indexOf("|");
    
    if (separatorIndex != -1) {
      String newPass = data.substring(0, separatorIndex);
      String newEmail = data.substring(separatorIndex + 1);

      if (updateUserProfileInFile(username, newPass, newEmail)) {
        out.println("MESSAGE Cập nhật thông tin tài khoản thành công!");
        System.out.println("User [" + username + "] has updated their profile.");
      } else {
        out.println("MESSAGE Lỗi hệ thống: Không thể cập nhật thông tin lúc này.");
      }
    } else {
      out.println("MESSAGE Sai cú pháp cập nhật!");
    }
  }


  // =======================
  // CÁC PHƯƠNG THỨC XỬ LÝ OFFLINE MESSAGES
  // =======================

  private void saveOfflineMessage(String receiver, String sender, String msg) {
      synchronized (OFFLINE_LOCK) {
          try {
              File file = new File("offline_messages.txt");
              if (!file.exists()) file.createNewFile();
              
              FileWriter fw = new FileWriter(file, true);
              fw.write(receiver + "|::|" + sender + "|::|" + msg + "\n");
              fw.close();
          } catch (IOException e) {
              System.err.println("Lỗi ghi tin nhắn offline: " + e.getMessage());
          }
      }
  }

  private void deliverOfflineMessages(String targetUser) {
      synchronized (OFFLINE_LOCK) {
          try {
              File file = new File("offline_messages.txt");
              if (!file.exists()) return;

              List<String> remainingLines = new ArrayList<>();
              Scanner scanner = new Scanner(file);
              
              while (scanner.hasNextLine()) {
                  String line = scanner.nextLine();
                  String[] parts = line.split("\\|\\:\\:\\|");
                  
                  if (parts.length == 3) {
                      if (parts[0].equals(targetUser)) {
                          out.println("PRIVATE từ " + parts[1] + ": " + parts[2]);
                      } else {
                          remainingLines.add(line);
                      }
                  }
              }
              scanner.close();

              FileWriter fw = new FileWriter(file, false); // false để ghi đè
              for (String remLine : remainingLines) {
                  fw.write(remLine + "\n");
              }
              fw.close();

          } catch (IOException e) {
              System.err.println("Lỗi đọc tin nhắn offline: " + e.getMessage());
          }
      }
  }

  // =======================
  // CÁC PHƯƠNG THỨC QUẢN LÍ TÀI KHOẢN NGƯỜI DÙNG (users.txt)
  // =======================

  private boolean registerUser(String user, String pass, String email) {
    synchronized (FILE_LOCK) {
      try {
        File file = new File("users.txt");
        file.createNewFile();

        Scanner scanner = new Scanner(file);
        while (scanner.hasNextLine()) {
          String line = scanner.nextLine();
          if (line.startsWith(user + "|")) {
            scanner.close();
            return false; 
          }
        }
        scanner.close();

        FileWriter fw = new FileWriter(file, true);
        fw.write(user + "|" + pass + "|" + email + "\n");
        fw.close();

        return true; 
      } catch (IOException e) {
        System.err.println("File write error in registerUser: " + e.getMessage());
        e.printStackTrace();
        return false;
      }
    }
  }

  private boolean checkLogin(String user, String pass) {
    synchronized (FILE_LOCK) {
      try {
        File file = new File("users.txt");
        if (!file.exists()) return false;

        Scanner scanner = new Scanner(file);
        String target = user + "|" + pass + "|";

        while (scanner.hasNextLine()) {
          String line = scanner.nextLine();
          if (line.startsWith(target)) {
            scanner.close();
            return true; 
          }
        }
        scanner.close();
      } catch (IOException e) {
        System.err.println("File read error in checkLogin: " + e.getMessage());
        e.printStackTrace();
      }
      return false; 
    }
  }

  public String getUsername() {
    return this.username;
  }

  private String getEmailFromUsersFile(String targetUser) {
    synchronized (FILE_LOCK) {
      try {
        File file = new File("users.txt");
        if (!file.exists()) return null;

        Scanner scanner = new Scanner(file);
        while (scanner.hasNextLine()) {
          String line = scanner.nextLine();
          String[] parts = line.split("\\|");

          if (parts.length == 3 && parts[0].equals(targetUser)) {
            scanner.close();
            return parts[2]; 
          }
        }
        scanner.close();
      } catch (IOException e) {
        System.err.println("File read error in getEmailFromUsersFile: " + e.getMessage());
        e.printStackTrace();
      }
      return null; 
    }
  }

  // [MỚI]: Phương thức ghi đè thông tin người dùng vào file
  private boolean updateUserProfileInFile(String targetUser, String newPass, String newEmail) {
    synchronized (FILE_LOCK) {
      try {
        File file = new File("users.txt");
        if (!file.exists()) return false;

        List<String> lines = new ArrayList<>();
        Scanner scanner = new Scanner(file);
        boolean isUpdated = false;

        // Đọc từng dòng và tìm người dùng cần sửa
        while (scanner.hasNextLine()) {
          String line = scanner.nextLine();
          String[] parts = line.split("\\|");

          if (parts.length == 3 && parts[0].equals(targetUser)) {
            // Nếu người dùng để trống ô Mật khẩu -> Giữ nguyên mật khẩu cũ (parts[1])
            String finalPass = newPass.isEmpty() ? parts[1] : newPass;
            
            // Nếu người dùng để trống ô Email -> Giữ nguyên email cũ (parts[2])
            String finalEmail = newEmail.isEmpty() ? parts[2] : newEmail;
            
            // Nạp dòng mới đã được cập nhật
            lines.add(targetUser + "|" + finalPass + "|" + finalEmail);
            isUpdated = true;
          } else {
            // Nạp lại các dòng không liên quan
            lines.add(line);
          }
        }
        scanner.close();

        // Ghi đè lại toàn bộ tệp nếu có sự thay đổi
        if (isUpdated) {
          FileWriter fw = new FileWriter(file, false); // Tham số false = Ghi đè toàn bộ tệp
          for (String l : lines) {
            fw.write(l + "\n");
          }
          fw.close();
          return true;
        }

      } catch (IOException e) {
        System.err.println("Lỗi cập nhật file users.txt: " + e.getMessage());
        e.printStackTrace();
      }
      return false;
    }
  }
}