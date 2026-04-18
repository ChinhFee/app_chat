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
  private static final int MAX_CONCURRENT_TRANSFERS = 10;
  private static final long MAX_FILE_SIZE = 100 * 1024 * 1024;
  private static final long MIN_DISK_SPACE = 100 * 1024 * 1024;
  private static java.util.concurrent.Semaphore transferSemaphore = new java.util.concurrent.Semaphore(MAX_CONCURRENT_TRANSFERS);

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

        if (transferSemaphore.tryAcquire()) {
          new Thread(() -> {
              try {
                  saveFile(clientSock);
              } catch (IOException e) {
                  System.err.println("File transfer error: " + e.getMessage());
              } finally {
                  transferSemaphore.release();
              }
          }).start();
        } else {
          System.err.println("Server quá tải, từ chối kết nối từ: " + clientSock.getRemoteSocketAddress());
          clientSock.close();
        }

      } catch (Exception e) {
        System.err.println("FileServer accept error: " + e.getMessage());
        e.printStackTrace();
      }
    }
  }

  private void saveFile(Socket clientSock) throws IOException {
    DataInputStream dis = new DataInputStream(clientSock.getInputStream());
    String fileName = dis.readUTF();
    long fileSize = dis.readLong();

    if (fileName.contains("..") || fileName.contains("/") || fileName.contains("\\") || fileName.startsWith(".")) {
        dis.close();
        clientSock.close();
        return;
    }
    if (fileSize > MAX_FILE_SIZE) {
        dis.close();
        clientSock.close();
        return;
    }

    File directory = new File("server_files");
    if (!directory.exists()) {
        directory.mkdirs();
    }

    if (directory.getUsableSpace() < fileSize + MIN_DISK_SPACE) {
        dis.close();
        clientSock.close();
        return;
    }

    File outputFile = new File(directory, fileName);
    if (outputFile.exists()) {
        String baseName = fileName.substring(0, fileName.lastIndexOf('.'));
        String ext = fileName.substring(fileName.lastIndexOf('.'));
        String newFileName = baseName + "_" + System.currentTimeMillis() + ext;
        outputFile = new File(directory, newFileName);
    }

    FileOutputStream fos = new FileOutputStream(outputFile);
    byte[] buffer = new byte[4096];
    int read = 0;
    long totalReceived = 0;
    long remaining = fileSize;

    try {
        while ((read = dis.read(buffer, 0, (int) Math.min(buffer.length, remaining))) > 0) {
            fos.write(buffer, 0, read);
            totalReceived += read;
            remaining -= read;
        }
        fos.close();
        dis.close();
        clientSock.close();
    } catch (IOException e) {
        fos.close();
        if (outputFile.exists()) outputFile.delete();
        throw e;
    }
  }

  public static void main(String[] args) {
    new fileServer(1988).start();
  }
}