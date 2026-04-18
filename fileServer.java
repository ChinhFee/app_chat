/*
 * FileServer.java
 *
 * Lớp này triển khai một máy chủ tập tin đơn giản nhận các tệp từ khách hàng qua kết nối TCP.
 * Nó chạy như một luồng riêng biệt và lắng nghe các yêu cầu truyền tệp đến.
 */

import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class fileServer extends Thread {
  private ServerSocket ss;

  public fileServer(int port) {
    try {
      ss = new ServerSocket(port);
      System.out.println("File Server is running on port: " + port);
    } catch (Exception e) {
      System.err.println("FileServer initialization error on port " + port + ": " + e.getMessage());
      e.printStackTrace();
    }
  }

  public void run() {
    while (true) {
      try {
        Socket clientSock = ss.accept();

        // [CẬP NHẬT]: Mở luồng mới cho mỗi file để không làm treo Server khi nhận file nặng
        new Thread(() -> {
            try {
                saveFile(clientSock);
            } catch (IOException e) {
                System.err.println("File transfer error: " + e.getMessage());
            }
        }).start();

      } catch (Exception e) {
        System.err.println("FileServer accept error: " + e.getMessage());
        e.printStackTrace();
      }
    }
  }

  private void saveFile(Socket clientSock) throws IOException {
    DataInputStream dis = new DataInputStream(clientSock.getInputStream());

    // 1. Đọc chính xác Tên và Size do Client quyết định
    String fileName = dis.readUTF();
    long fileSize = dis.readLong();

    // 2. Tạo thư mục lưu trữ riêng trên Server
    File directory = new File("server_files");
    if (!directory.exists()) {
        directory.mkdirs();
    }

    // 3. Sử dụng đúng tên duy nhất mà Client đã cấp
    File outputFile = new File(directory, fileName);
    FileOutputStream fos = new FileOutputStream(outputFile);

    // Sử dụng bộ đệm 4096 byte
    byte[] buffer = new byte[4096];
    int read = 0;
    long remaining = fileSize;

    // Đọc chính xác dung lượng file tránh dính rác
    while ((read = dis.read(buffer, 0, (int) Math.min(buffer.length, remaining))) > 0) {
        fos.write(buffer, 0, read);
        remaining -= read;
    }

    fos.close();
    dis.close();
    clientSock.close();

    System.out.println("Đã nhận và lưu file thành công: " + outputFile.getAbsolutePath());
  }

  public static void main(String[] args) {
    new fileServer(1988).start();
  }
}