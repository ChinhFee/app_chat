import java.io.IOException;

public class FileTransferTest {
    public static void main(String[] args) {
        // Test 1: File hợp lệ
        try {
            System.out.println("Test 1: Valid file...");
            fileClient.sendFile("localhost", 1988, "test_file.txt", "valid_test.txt");
            System.out.println("✓ Valid file sent successfully!");
        } catch (Exception e) {
            System.err.println("✗ Valid file failed: " + e.getMessage());
        }

        // Test 2: File không tồn tại
        try {
            System.out.println("\nTest 2: Non-existent file...");
            fileClient.sendFile("localhost", 1988, "nonexistent.txt", "test.txt");
            System.out.println("✗ Should have failed!");
        } catch (Exception e) {
            System.out.println("✓ Correctly rejected: " + e.getMessage());
        }

        // Test 3: Tên file có path traversal
        try {
            System.out.println("\nTest 3: Path traversal attack...");
            fileClient.sendFile("localhost", 1988, "test_file.txt", "../../../evil.txt");
            System.out.println("✗ Should have failed!");
        } catch (Exception e) {
            System.out.println("✓ Correctly rejected: " + e.getMessage());
        }

        System.out.println("\nAll tests completed!");
    }
}