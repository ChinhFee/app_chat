/*
 * MailUtil.java
 *
 * Lớp tiện ích cung cấp chức năng gửi thông báo email HTML chuyên nghiệp.
 * Sử dụng JavaMail API với mã hóa SSL/TLS qua Gmail SMTP.
 */

import java.util.Properties;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.io.UnsupportedEncodingException; // Hỗ trợ hiển thị tên tiếng Việt có dấu
import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class mailUtil {
    public static final String HOST_NAME = "smtp.gmail.com";
    public static final int SSL_PORT = 465;
    
    // Thông tin tài khoản gửi
    public static final String APP_EMAIL = "henryvo2k4@gmail.com";
    public static final String APP_PASSWORD = "padl oyrs cene skdg"; // Mật khẩu ứng dụng của bạn

    /**
     * Gửi email thông báo HTML chuyên nghiệp khi người dùng offline.
     * @param toEmail       Địa chỉ email người nhận.
     * @param senderName    Tên người gửi tin nhắn.
     * @param messageContent Nội dung tin nhắn.
     */
    public static void sendOfflineNotification(String toEmail, String senderName, String messageContent) {
        // Cấu hình SMTP
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.host", HOST_NAME);
        props.put("mail.smtp.socketFactory.port", SSL_PORT);
        props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        props.put("mail.smtp.port", SSL_PORT);

        // Xác thực tài khoản
        Session session = Session.getDefaultInstance(props, new javax.mail.Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                // JavaMail sẽ tự động bỏ qua khoảng trắng trong mật khẩu ứng dụng
                return new PasswordAuthentication(APP_EMAIL, APP_PASSWORD);
            }
        });

        try {
            // 1. Lấy thời gian hiện tại
            SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm");
            String timeStr = formatter.format(new Date());

            // 2. Thiết kế Template HTML
            String htmlContent = 
                "<div style='font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; border: 1px solid #e0e0e0; border-radius: 10px; overflow: hidden;'>" +
                    "<div style='background-color: #4f46e5; color: white; padding: 20px; text-align: center;'>" +
                        "<h2 style='margin: 0;'>Thông báo Z-Chat</h2>" +
                    "</div>" +
                    "<div style='padding: 30px; color: #333; background-color: #ffffff;'>" +
                        "<p style='font-size: 16px;'>Chào bạn,</p>" +
                        "<p style='font-size: 16px;'>Bạn có một tin nhắn riêng tư mới từ <strong>" + senderName + "</strong>:</p>" +
                        
                        // Khung nội dung tin nhắn
                        "<div style='background-color: #f3f6fd; padding: 15px; border-left: 4px solid #4f46e5; border-radius: 4px; margin: 20px 0; font-style: italic; color: #555;'>" +
                            "\"" + messageContent + "\"" +
                        "</div>" +
                        
                        "<p style='font-size: 14px; color: #888;'>Thời gian nhận: " + timeStr + "</p>" +
                        "<p style='font-size: 16px;'>Vui lòng đăng nhập vào ứng dụng để phản hồi.</p>" +
                    "</div>" +
                    "<div style='background-color: #f9f9f9; padding: 15px; text-align: center; font-size: 12px; color: #aaa; border-top: 1px solid #eee;'>" +
                        "© 2026 Hệ thống Z-Chat Enterprise. Vui lòng không trả lời email này." +
                    "</div>" +
                "</div>";

            // 3. Tạo và cấu hình tin nhắn
            MimeMessage message = new MimeMessage(session);
            
            // THIẾT LẬP TÊN HIỂN THỊ (Sẽ hiện "Hệ thống Z-Chat" thay vì henryvo2k4)
            message.setFrom(new InternetAddress(APP_EMAIL, "Z-Chats", "UTF-8"));
            
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject("Tin nhắn mới từ " + senderName + " - Z-Chat");
            
            // Gửi dưới dạng HTML
            message.setContent(htmlContent, "text/html; charset=UTF-8");

            // 4. Thực thi gửi email
            Transport.send(message);
            System.out.println("Email chuyên nghiệp đã được gửi tới: " + toEmail);

        } catch (MessagingException | UnsupportedEncodingException e) {
            // Bắt chung 2 ngoại lệ: Lỗi cấu hình thư (MessagingException) và Lỗi bảng mã chữ (UnsupportedEncodingException)
            System.err.println("Lỗi gửi email: " + e.getMessage());
            e.printStackTrace();
        }
    }
}