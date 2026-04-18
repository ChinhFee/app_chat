public class TestImageSend {
    public static void main(String[] args) {
        try {
            System.out.println("Testing gửi file .png...");
            fileClient.sendFile("localhost", 1988, "test_image.png", "test_image.png");
            System.out.println("✅ File .png gửi thành công!");
        } catch (Exception e) {
            System.err.println("❌ Lỗi gửi file: " + e.getMessage());
        }
    }
}