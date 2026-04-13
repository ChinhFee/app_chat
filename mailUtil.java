/*
 * MailUtil.java
 *
 * Lớp tiện ích này cung cấp chức năng email cho ứng dụng trò chuyện.
 * Nó xử lý việc gửi email thông báo cho người dùng khi họ nhận được tin nhắn riêng tư khi ngoại tuyến.
 * Sử dụng JavaMail API với mã hóa SSL/TLS để truyền email an toàn qua Gmail SMTP.
 *
 * Các Tính Năng Chính:
 * - Định cấu hình cài đặt máy chủ SMTP Gmail
 * - Xác thực sử dụng mật khẩu dành riêng cho ứng dụng
 * - Gửi email HTML/văn bản với nội dung tùy chỉnh
 * - Xử lý các lỗi gửi email một cách nhẹ nhàng
 *
 * Lưu Ý Về Bảo Mật: Mật khẩu ứng dụng phải được lưu trữ an toàn, không được mã hóa cứng trong sản xuất.
 *
 * Tác Giả: [Tên của bạn]
 * Ngày: [Ngày Hiện Tại]
 */

import java.util.Properties;
import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

/**
 * MailUtil cung cấp các phương thức tĩnh để gửi thông báo email.
 * Nó được sử dụng để thông báo cho người dùng về tin nhắn riêng tư mới khi họ ngoại tuyến.
 */
public class mailUtil {
  /**
   * Tên máy chủ SMTP cho Gmail.
   */
  public static final String HOST_NAME = "smtp.gmail.com";

  /**
   * Số cổng SSL cho Gmail SMTP.
   */
  public static final int SSL_PORT = 465;

  /**
   * Địa chỉ email ứng dụng được sử dụng làm người gửi.
   * Đây nên là một tài khoản email chuyên dụng cho hệ thống gửi tin nhắn.
   */
  public static final String APP_EMAIL = "phichinh08012004@gmail.com"; // Thay thế bằng email kiểm test thực tế

  /**
   * Mật khẩu ứng dụng cho tài khoản email người gửi.
   * Đây là mật khẩu dành riêng cho ứng dụng gồm 16 ký tự được tạo bởi Gmail để truy cập an toàn.
   * Lưu Ý: Trong sản xuất, điều này phải được lưu trữ an toàn, không được mã hóa cứng.
   */
  public static final String APP_PASSWORD = "kiaoftgmzuswbotz"; // Thay thế bằng mật khẩu ứng dụng thực tế

  /**
   * Gửi email thông báo ngoại tuyến cho người dùng về tin nhắn riêng tư mới.
   *
   * Các Khối Chức Năng:
   * 1. Định Cấu Hình SMTP: Thiết lập thuộc tính máy chủ thư cho kết nối SSL
   * 2. Thiết Lập Xác Thực: Định cấu hình phiên với thông tin xác thực ứng dụng
   * 3. Tạo Tin Nhắn: Xây dựng tin nhắn email với người nhận, chủ đề và nội dung
   * 4. Truyền Email: Gửi tin nhắn qua SMTP
   * 5. Xử Lý Lỗi: Ghi lại thành công hoặc xử lý các lỗi gửi
   *
   * @param toEmail Địa chỉ email của người nhận
   * @param content Nội dung tin nhắn để đưa vào phần thân email
   */
  public static void sendOfflineNotification(String toEmail, String content) {
    // Khối 1: Định cấu hình thuộc tính SMTP cho kết nối an toàn
    Properties props = new Properties();
    props.put("mail.smtp.auth", "true");
    props.put("mail.smtp.host", HOST_NAME);
    props.put("mail.smtp.socketFactory.port", SSL_PORT);
    props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
    props.put("mail.smtp.port", SSL_PORT);

    // Khối 2: Thiết lập xác thực cho phiên thư
    Session session = Session.getDefaultInstance(props, new javax.mail.Authenticator() {
      protected PasswordAuthentication getPasswordAuthentication() {
        return new PasswordAuthentication(APP_EMAIL, APP_PASSWORD);
      }
    });

    try {
      // Khối 3: Tạo tin nhắn email
      MimeMessage message = new MimeMessage(session);
      message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
      message.setSubject("Notification: You have a new offline message!");
      message.setText(content);

      // Khối 4: Gửi email
      Transport.send(message);

      // Khối 5: Ghi lại việc truyền thành công
      System.out.println("Offline notification email sent successfully to: " + toEmail);
    } catch (MessagingException e) {
      // Khối 5: Xử lý và ghi lại các lỗi gửi email
      System.out.println("Error sending email: " + e.getMessage());
    }
  }
}