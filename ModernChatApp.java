import com.formdev.flatlaf.FlatLightLaf;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;

public class ModernChatApp {
    private JFrame frame;
    private JPanel cardPanel;
    private CardLayout cardLayout;

    // UI - Đăng nhập / Đăng ký
    private JTextField txtUserLogin, txtUserReg;
    private JPasswordField txtPassLogin, txtPassReg;

    // UI - Vùng Chat
    private JScrollPane chatScrollPane;
    private JTextField txtMessage;
    private JLabel lblChatAvatar, lblChatName, lblChatStatus;
    private JLabel lblMyProfile; 
    
    // UI - Vùng Sidebar
    private JPanel chatListPanel; 
    private JPanel onlineUsersPanel; 

    // Mạng & Dữ liệu
    private Socket socket;
    private PrintWriter outToServer;
    private BufferedReader inFromServer;
    private String currentUsername = "Unknown";
    private String currentChatTarget = "ALL"; 

    // QUẢN LÝ ĐA HỘP THOẠI VÀ TRẠNG THÁI
    private HashMap<String, JPanel> chatPanelsMap = new HashMap<>();
    private HashSet<String> activeChats = new HashSet<>();
    private HashSet<String> onlineUsers = new HashSet<>(); 
    private HashMap<String, JLabel> statusLabelsMap = new HashMap<>(); 

    private static final Color[] AVATAR_COLORS = {
        new Color(239, 68, 68), new Color(249, 115, 22), new Color(16, 185, 129), 
        new Color(59, 130, 246), new Color(99, 102, 241), new Color(139, 92, 246), 
        new Color(236, 72, 153), new Color(14, 165, 233)
    };

    public ModernChatApp() {
        initUI();
    }

    private void initUI() {
        frame = new JFrame("Z-Chat: Enterprise Edition");
        frame.setSize(1150, 750);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.getContentPane().setBackground(new Color(243, 246, 253)); 

        cardLayout = new CardLayout();
        cardPanel = new JPanel(cardLayout);
        cardPanel.setOpaque(false);

        cardPanel.add(createAuthScreen(), "AUTH");
        cardPanel.add(createMainWorkspace(), "CHAT");

        frame.add(cardPanel);
        frame.setVisible(true);
    }

    // ==========================================
    // 1. MÀN HÌNH XÁC THỰC
    // ==========================================
    private JPanel createAuthScreen() {
        JPanel wrapper = new JPanel(new GridBagLayout());
        wrapper.setBackground(new Color(243, 246, 253));

        JPanel authBox = new JPanel(new CardLayout());
        authBox.setPreferredSize(new Dimension(420, 520)); 
        authBox.putClientProperty("FlatLaf.style", "arc: 35");
        authBox.setBackground(Color.WHITE);
        authBox.setBorder(new EmptyBorder(40, 40, 40, 40));

        // --- PANEL ĐĂNG NHẬP ---
        JPanel loginPanel = new JPanel();
        loginPanel.setLayout(new BoxLayout(loginPanel, BoxLayout.Y_AXIS));
        loginPanel.setOpaque(false);

        JLabel lblLoginTitle = new JLabel("Đăng Nhập");
        lblLoginTitle.setFont(new Font("Segoe UI", Font.BOLD, 28));
        lblLoginTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        txtUserLogin = new JTextField();
        txtUserLogin.putClientProperty("JTextField.placeholderText", "Tên đăng nhập");
        txtUserLogin.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        txtUserLogin.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        txtUserLogin.setAlignmentX(Component.CENTER_ALIGNMENT); 

        txtPassLogin = new JPasswordField();
        txtPassLogin.putClientProperty("JTextField.placeholderText", "Mật khẩu");
        txtPassLogin.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        txtPassLogin.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        txtPassLogin.setAlignmentX(Component.CENTER_ALIGNMENT); 
        
        JButton btnLogin = new JButton("Vào Chat");
        btnLogin.setBackground(new Color(79, 70, 229));
        btnLogin.setForeground(Color.WHITE);
        btnLogin.setFont(new Font("Segoe UI", Font.BOLD, 16));
        btnLogin.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        btnLogin.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnLogin.setAlignmentX(Component.CENTER_ALIGNMENT); 
        
        JButton btnGoToReg = new JButton("Chưa có tài khoản? Đăng ký ngay");
        btnGoToReg.setContentAreaFilled(false);
        btnGoToReg.setForeground(new Color(79, 70, 229));
        btnGoToReg.setBorderPainted(false);
        btnGoToReg.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnGoToReg.setAlignmentX(Component.CENTER_ALIGNMENT);

        loginPanel.add(Box.createVerticalStrut(30));
        loginPanel.add(lblLoginTitle);
        loginPanel.add(Box.createVerticalStrut(45));
        loginPanel.add(txtUserLogin);
        loginPanel.add(Box.createVerticalStrut(20));
        loginPanel.add(txtPassLogin);
        loginPanel.add(Box.createVerticalStrut(30));
        loginPanel.add(btnLogin);
        loginPanel.add(Box.createVerticalStrut(10));
        loginPanel.add(btnGoToReg);

        // --- PANEL ĐĂNG KÝ ---
        JPanel regPanel = new JPanel();
        regPanel.setLayout(new BoxLayout(regPanel, BoxLayout.Y_AXIS));
        regPanel.setOpaque(false);

        JLabel lblRegTitle = new JLabel("Tạo Tài Khoản");
        lblRegTitle.setFont(new Font("Segoe UI", Font.BOLD, 28));
        lblRegTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        txtUserReg = new JTextField();
        txtUserReg.putClientProperty("JTextField.placeholderText", "Tên đăng nhập mới");
        txtUserReg.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        txtUserReg.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        txtUserReg.setAlignmentX(Component.CENTER_ALIGNMENT); 
        
        txtPassReg = new JPasswordField();
        txtPassReg.putClientProperty("JTextField.placeholderText", "Mật khẩu mới");
        txtPassReg.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        txtPassReg.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        txtPassReg.setAlignmentX(Component.CENTER_ALIGNMENT); 

        JButton btnRegister = new JButton("Đăng Ký");
        btnRegister.setBackground(new Color(16, 185, 129));
        btnRegister.setForeground(Color.WHITE);
        btnRegister.setFont(new Font("Segoe UI", Font.BOLD, 16));
        btnRegister.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        btnRegister.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnRegister.setAlignmentX(Component.CENTER_ALIGNMENT); 

        JButton btnBackToLogin = new JButton("Quay lại Đăng nhập");
        btnBackToLogin.setContentAreaFilled(false);
        btnBackToLogin.setBorderPainted(false);
        btnBackToLogin.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnBackToLogin.setAlignmentX(Component.CENTER_ALIGNMENT);

        regPanel.add(Box.createVerticalStrut(30));
        regPanel.add(lblRegTitle);
        regPanel.add(Box.createVerticalStrut(40));
        regPanel.add(txtUserReg);
        regPanel.add(Box.createVerticalStrut(20));
        regPanel.add(txtPassReg);
        regPanel.add(Box.createVerticalStrut(30));
        regPanel.add(btnRegister);
        regPanel.add(Box.createVerticalStrut(10));
        regPanel.add(btnBackToLogin);

        authBox.add(loginPanel, "LOGIN_SCREEN");
        authBox.add(regPanel, "REG_SCREEN");

        CardLayout cl = (CardLayout) authBox.getLayout();
        btnGoToReg.addActionListener(e -> cl.show(authBox, "REG_SCREEN"));
        btnBackToLogin.addActionListener(e -> cl.show(authBox, "LOGIN_SCREEN"));

        btnLogin.addActionListener(e -> {
            if(!txtUserLogin.getText().isEmpty()) {
                connectToServer("LOGIN " + txtUserLogin.getText() + "|" + new String(txtPassLogin.getPassword()), txtUserLogin.getText());
            }
        });

        btnRegister.addActionListener(e -> {
            String u = txtUserReg.getText();
            String p = new String(txtPassReg.getPassword());
            if(!u.isEmpty() && !p.isEmpty()) {
                connectToServer("REGISTER " + u + "|" + p + "|email@gmail.com", u);
            }
        });

        wrapper.add(authBox);
        return wrapper;
    }

    // ==========================================
    // 2. GIAO DIỆN CHAT CHÍNH
    // ==========================================
    private JPanel createMainWorkspace() {
        JPanel mainPanel = new JPanel(new BorderLayout(15, 15));
        mainPanel.setBackground(new Color(243, 246, 253));
        mainPanel.setBorder(new EmptyBorder(15, 15, 15, 15));

        // --- CỘT TRÁI (SIDEBAR) ---
        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setPreferredSize(new Dimension(320, 0));
        sidebar.setBackground(new Color(250, 250, 252));
        sidebar.putClientProperty("FlatLaf.style", "arc: 20");
        sidebar.setBorder(new EmptyBorder(20, 15, 20, 15));

        // 2.1 Header Sidebar (Đã dọn dẹp và gom nút + sang phải)
        JPanel sidebarHeader = new JPanel(new BorderLayout(0, 15));
        sidebarHeader.setOpaque(false);
        
        JPanel myProfilePanel = new JPanel(new BorderLayout(10, 0));
        myProfilePanel.setOpaque(false);
        lblMyProfile = new JLabel(); 
        
        JLabel lblChats = new JLabel("Chats HCD");
        lblChats.setFont(new Font("Segoe UI", Font.BOLD, 22));
        
        // Nút Tạo Nhóm (+)
        JButton btnCreateGroup = new JButton("+");
        btnCreateGroup.setToolTipText("Tạo nhóm chat mới");
        btnCreateGroup.setFont(new Font("Segoe UI", Font.BOLD, 18));
        btnCreateGroup.setBackground(new Color(79, 70, 229));
        btnCreateGroup.setForeground(Color.WHITE);
        btnCreateGroup.setPreferredSize(new Dimension(36, 36));
        btnCreateGroup.putClientProperty("FlatLaf.style", "arc: 999"); // Tròn xoe
        btnCreateGroup.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnCreateGroup.addActionListener(e -> {
            String groupName = JOptionPane.showInputDialog(frame, "Nhập tên nhóm:");
            if (groupName != null && !groupName.isEmpty()) outToServer.println("JOINGROUP " + groupName);
        });

        myProfilePanel.add(lblMyProfile, BorderLayout.WEST);
        myProfilePanel.add(lblChats, BorderLayout.CENTER);
        myProfilePanel.add(btnCreateGroup, BorderLayout.EAST);
        
        sidebarHeader.add(myProfilePanel, BorderLayout.NORTH);

        JTextField txtSearch = new JTextField();
        txtSearch.putClientProperty("JTextField.placeholderText", "Tìm kiếm bạn bè...");
        txtSearch.setPreferredSize(new Dimension(0, 42));
        sidebarHeader.add(txtSearch, BorderLayout.CENTER);
        
        sidebar.add(sidebarHeader, BorderLayout.NORTH);

        // 2.2 Khu vực hiển thị danh sách
        JPanel sidebarContent = new JPanel(new BorderLayout());
        sidebarContent.setOpaque(false);

        // Hàng Avatar Online 
        JPanel onlineWrapper = new JPanel(new BorderLayout());
        onlineWrapper.setOpaque(false);
        onlineWrapper.setBorder(new EmptyBorder(15, 0, 10, 0));
        JLabel lblOnlineTitle = new JLabel("Đang hoạt động");
        lblOnlineTitle.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblOnlineTitle.setForeground(new Color(150, 150, 150));
        onlineWrapper.add(lblOnlineTitle, BorderLayout.NORTH);

        onlineUsersPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        onlineUsersPanel.setOpaque(false);
        JScrollPane onlineScroll = new JScrollPane(onlineUsersPanel);
        onlineScroll.setBorder(null);
        onlineScroll.setOpaque(false);
        onlineScroll.getViewport().setOpaque(false);
        onlineScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        onlineScroll.setPreferredSize(new Dimension(300, 85));
        onlineWrapper.add(onlineScroll, BorderLayout.CENTER);
        sidebarContent.add(onlineWrapper, BorderLayout.NORTH);

        // Danh sách Chat Dọc 
        JPanel chatListWrapper = new JPanel(new BorderLayout());
        chatListWrapper.setOpaque(false);
        chatListPanel = new JPanel();
        chatListPanel.setLayout(new BoxLayout(chatListPanel, BoxLayout.Y_AXIS));
        chatListPanel.setOpaque(false);
        
        addActiveChat("ALL", "Kênh Server Tổng", "Public Channel");
        
        chatListWrapper.add(chatListPanel, BorderLayout.NORTH); 
        JScrollPane chatListScroll = new JScrollPane(chatListWrapper);
        chatListScroll.setBorder(null);
        chatListScroll.setOpaque(false);
        chatListScroll.getViewport().setOpaque(false);
        sidebarContent.add(chatListScroll, BorderLayout.CENTER);
        sidebar.add(sidebarContent, BorderLayout.CENTER);

        // --- CỘT PHẢI (KHU VỰC NHẮN TIN CHÍNH) ---
        JPanel chatArea = new JPanel(new BorderLayout());
        chatArea.setBackground(Color.WHITE);
        chatArea.putClientProperty("FlatLaf.style", "arc: 20");

        // 2.3 Header Khung Chat
        JPanel chatHeader = new JPanel(new BorderLayout());
        chatHeader.setOpaque(false);
        chatHeader.setBorder(new EmptyBorder(15, 20, 15, 20));
        chatHeader.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(240, 240, 240))); 
        
        JPanel headerInfo = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
        headerInfo.setOpaque(false);
        lblChatAvatar = new JLabel(new AvatarIcon("A", 45, getColorForName("ALL"))); 
        
        JPanel nameStatusPanel = new JPanel(new GridLayout(2, 1));
        nameStatusPanel.setOpaque(false);
        lblChatName = new JLabel("Kênh Server Tổng");
        lblChatName.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblChatStatus = new JLabel("Hoạt động");
        lblChatStatus.setForeground(new Color(16, 163, 127));
        nameStatusPanel.add(lblChatName);
        nameStatusPanel.add(lblChatStatus);
        
        headerInfo.add(lblChatAvatar);
        headerInfo.add(nameStatusPanel);
        chatHeader.add(headerInfo, BorderLayout.WEST);
        chatArea.add(chatHeader, BorderLayout.NORTH);

        // 2.4 Nội dung Chat
        chatScrollPane = new JScrollPane(); 
        chatScrollPane.setBorder(null);
        chatScrollPane.getVerticalScrollBar().setUnitIncrement(16);
        chatScrollPane.setViewportView(getOrCreateChatPanel("ALL")); 
        chatArea.add(chatScrollPane, BorderLayout.CENTER);

        // 2.5 Thanh Nhập Liệu
        JPanel inputWrapper = new JPanel(new BorderLayout());
        inputWrapper.setOpaque(false);
        inputWrapper.setBorder(new EmptyBorder(15, 20, 20, 20));

        JPanel inputBar = new JPanel(new BorderLayout(10, 0));
        inputBar.setBackground(new Color(243, 246, 253));
        inputBar.putClientProperty("FlatLaf.style", "arc: 999"); 
        inputBar.setBorder(new EmptyBorder(8, 15, 8, 10));

        JButton btnAttach = new JButton("+ Tệp");
        btnAttach.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnAttach.setForeground(new Color(100, 100, 100));
        btnAttach.setContentAreaFilled(false);
        btnAttach.setBorderPainted(false);
        btnAttach.setCursor(new Cursor(Cursor.HAND_CURSOR));

        txtMessage = new JTextField();
        txtMessage.setBorder(null);
        txtMessage.setBackground(new Color(243, 246, 253));
        txtMessage.putClientProperty("JTextField.placeholderText", "Nhập tin nhắn...");
        txtMessage.setFont(new Font("Segoe UI", Font.PLAIN, 15));

        JButton btnSend = new JButton("Gửi");
        btnSend.setBackground(new Color(79, 70, 229));
        btnSend.setForeground(Color.WHITE);
        btnSend.setFont(new Font("Segoe UI", Font.BOLD, 14));

        btnAttach.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            if (fileChooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();
                try {
                    fileClient.sendFile("localhost", 1988, file.getAbsolutePath());
                    appendMessageBubble(currentChatTarget, currentUsername, "Đã gửi tệp: " + file.getName(), true, true);
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(frame, "Lỗi gửi file: " + ex.getMessage());
                }
            }
        });

        btnSend.addActionListener(e -> {
            String msg = txtMessage.getText().trim();
            if (!msg.isEmpty()) {
                if (currentChatTarget.equals("ALL")) {
                    outToServer.println("CHATALL " + msg);
                } else {
                    outToServer.println("CHATPRIVATE " + currentChatTarget + "|" + msg);
                }
                appendMessageBubble(currentChatTarget, currentUsername, msg, true, true);
                txtMessage.setText("");
            }
        });
        txtMessage.addActionListener(e -> btnSend.doClick());

        inputBar.add(btnAttach, BorderLayout.WEST);
        inputBar.add(txtMessage, BorderLayout.CENTER);
        inputBar.add(btnSend, BorderLayout.EAST);
        inputWrapper.add(inputBar, BorderLayout.CENTER);
        chatArea.add(inputWrapper, BorderLayout.SOUTH);

        mainPanel.add(sidebar, BorderLayout.WEST);
        mainPanel.add(chatArea, BorderLayout.CENTER);

        return mainPanel;
    }

    // ==========================================
    // QUẢN LÝ TRẠNG THÁI ONLINE VÀ ĐỊNH TUYẾN CHAT
    // ==========================================
    private void updateUserStatus(String username, boolean isOnline) {
        String statusText = isOnline ? "Đang hoạt động" : "Ngoại tuyến";
        Color statusColor = isOnline ? new Color(16, 163, 127) : new Color(150, 150, 150);

        if (statusLabelsMap.containsKey(username)) {
            JLabel lbl = statusLabelsMap.get(username);
            lbl.setText(statusText);
        }

        if (currentChatTarget.equals(username)) {
            lblChatStatus.setText(statusText);
            lblChatStatus.setForeground(statusColor);
        }
    }

    private JPanel getOrCreateChatPanel(String targetId) {
        if (!chatPanelsMap.containsKey(targetId)) {
            JPanel newPanel = new JPanel();
            newPanel.setLayout(new BoxLayout(newPanel, BoxLayout.Y_AXIS));
            newPanel.setBackground(Color.WHITE);
            newPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
            chatPanelsMap.put(targetId, newPanel);
        }
        return chatPanelsMap.get(targetId);
    }

    private void addActiveChat(String targetId, String name, String forceStatus) {
        if (activeChats.contains(targetId)) return;
        activeChats.add(targetId);
        
        String statusText = forceStatus != null ? forceStatus : (onlineUsers.contains(targetId) ? "Đang hoạt động" : "Ngoại tuyến");
        
        JPanel item = new JPanel(new BorderLayout(15, 0));
        item.setPreferredSize(new Dimension(280, 65));
        item.setMaximumSize(new Dimension(Integer.MAX_VALUE, 65)); 
        item.setBorder(new EmptyBorder(10, 15, 10, 15));
        item.setBackground(new Color(250, 250, 252));
        item.putClientProperty("FlatLaf.style", "arc: 15");
        item.setCursor(new Cursor(Cursor.HAND_CURSOR));

        JLabel lblIcon = new JLabel(new AvatarIcon(targetId, 45, getColorForName(targetId)));
        item.add(lblIcon, BorderLayout.WEST);

        JPanel textPanel = new JPanel(new GridLayout(2, 1));
        textPanel.setOpaque(false);
        JLabel lblName = new JLabel(name);
        lblName.setFont(new Font("Segoe UI", Font.BOLD, 15));
        
        JLabel lblSub = new JLabel(statusText);
        lblSub.setForeground(new Color(130, 130, 130));
        lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        statusLabelsMap.put(targetId, lblSub); 
        
        textPanel.add(lblName);
        textPanel.add(lblSub);
        item.add(textPanel, BorderLayout.CENTER);

        item.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                currentChatTarget = targetId;
                lblChatName.setText(name);
                lblChatAvatar.setIcon(new AvatarIcon(targetId, 45, getColorForName(targetId)));
                
                if (targetId.equals("ALL") || targetId.equals("GROUP_ACTION")) {
                    lblChatStatus.setText("Public Channel");
                    lblChatStatus.setForeground(new Color(16, 163, 127));
                } else {
                    boolean currentlyOnline = onlineUsers.contains(targetId);
                    lblChatStatus.setText(currentlyOnline ? "Đang hoạt động" : "Ngoại tuyến");
                    lblChatStatus.setForeground(currentlyOnline ? new Color(16, 163, 127) : Color.GRAY);
                }
                
                JPanel targetPanel = getOrCreateChatPanel(targetId);
                chatScrollPane.setViewportView(targetPanel);
                
                SwingUtilities.invokeLater(() -> {
                    JScrollBar vertical = chatScrollPane.getVerticalScrollBar();
                    vertical.setValue(vertical.getMaximum());
                });
            }
        });
        
        chatListPanel.add(item);
        chatListPanel.revalidate();
        chatListPanel.repaint();
    }

    private void saveMessageToHistory(String targetId, String sender, String message, boolean isMe) {
        try {
            File f = new File("history_" + currentUsername + ".txt");
            FileWriter fw = new FileWriter(f, true);
            String cleanMsg = message.replace("\n", "\\n"); 
            fw.write(targetId + "|::|" + sender + "|::|" + isMe + "|::|" + cleanMsg + "\n");
            fw.close();
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void loadHistory() {
        try {
            File f = new File("history_" + currentUsername + ".txt");
            if (!f.exists()) return;
            BufferedReader br = new BufferedReader(new FileReader(f));
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split("\\|\\:\\:\\|");
                if (parts.length == 4) {
                    String targetId = parts[0];
                    String sender = parts[1];
                    boolean isMe = Boolean.parseBoolean(parts[2]);
                    String message = parts[3].replace("\\n", "\n");
                    appendMessageBubble(targetId, sender, message, isMe, false);
                }
            }
            br.close();
        } catch (IOException e) { e.printStackTrace(); }
    }

    // ==========================================
    // KHỐI VẼ BONG BÓNG CHAT VÀ ĐỊNH TUYẾN
    // ==========================================
    private void appendMessageBubble(String targetId, String sender, String message, boolean isMe, boolean saveToFile) {
        JPanel targetPanel = getOrCreateChatPanel(targetId);
        
        if (!targetId.equals("ALL") && !targetId.equals("GROUP_ACTION")) {
            addActiveChat(targetId, targetId, null);
        }

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.setBorder(new EmptyBorder(5, 10, 5, 10));

        JLabel lblAvt = new JLabel(new AvatarIcon(sender, 40, getColorForName(sender)));
        lblAvt.setVerticalAlignment(SwingConstants.BOTTOM);

        JTextArea txtMsg = new JTextArea(message);
        txtMsg.setWrapStyleWord(true);
        txtMsg.setLineWrap(true);
        txtMsg.setEditable(false);
        txtMsg.setOpaque(false);
        txtMsg.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        
        int textWidth = Math.min(message.length() * 8, 400); 
        txtMsg.setSize(new Dimension(textWidth, Short.MAX_VALUE));
        String timeStr = new SimpleDateFormat("HH:mm").format(new Date());

        if (isMe) {
            txtMsg.setForeground(Color.WHITE);
            BubblePanel bubble = new BubblePanel(new Color(0, 132, 255));
            bubble.setLayout(new BorderLayout());
            bubble.setBorder(new EmptyBorder(10, 15, 10, 15));
            bubble.add(txtMsg, BorderLayout.CENTER);

            JLabel lblTime = new JLabel(timeStr, SwingConstants.RIGHT);
            lblTime.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            lblTime.setForeground(Color.GRAY);
            lblTime.setBorder(new EmptyBorder(2, 0, 0, 0));

            JPanel messageStack = new JPanel(new BorderLayout());
            messageStack.setOpaque(false);
            messageStack.add(bubble, BorderLayout.CENTER);
            messageStack.add(lblTime, BorderLayout.SOUTH);

            JPanel alignPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
            alignPanel.setOpaque(false);
            alignPanel.add(messageStack); 
            alignPanel.add(lblAvt);      
            wrapper.add(alignPanel, BorderLayout.EAST);
        } else {
            txtMsg.setForeground(Color.BLACK);
            BubblePanel bubble = new BubblePanel(new Color(228, 230, 235));
            bubble.setLayout(new BorderLayout());
            bubble.setBorder(new EmptyBorder(10, 15, 10, 15));
            bubble.add(txtMsg, BorderLayout.CENTER);

            JLabel lblName = new JLabel(sender);
            lblName.setFont(new Font("Segoe UI", Font.BOLD, 12));
            lblName.setForeground(new Color(100, 100, 100));
            lblName.setBorder(new EmptyBorder(0, 5, 3, 0));

            JLabel lblTime = new JLabel(timeStr, SwingConstants.LEFT);
            lblTime.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            lblTime.setForeground(Color.GRAY);
            lblTime.setBorder(new EmptyBorder(2, 5, 0, 0));

            JPanel messageStack = new JPanel(new BorderLayout());
            messageStack.setOpaque(false);
            messageStack.add(lblName, BorderLayout.NORTH);
            messageStack.add(bubble, BorderLayout.CENTER);
            messageStack.add(lblTime, BorderLayout.SOUTH);

            JPanel alignPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
            alignPanel.setOpaque(false);
            alignPanel.add(lblAvt);       
            alignPanel.add(messageStack);  
            wrapper.add(alignPanel, BorderLayout.WEST);
        }

        targetPanel.add(wrapper);

        if (saveToFile) {
            saveMessageToHistory(targetId, sender, message, isMe);
        }

        if (currentChatTarget.equals(targetId)) {
            targetPanel.revalidate();
            targetPanel.repaint();
            SwingUtilities.invokeLater(() -> {
                JScrollBar vertical = chatScrollPane.getVerticalScrollBar();
                vertical.setValue(vertical.getMaximum());
            });
        }
    }

    // ==========================================
    // CÁC HÀM PHỤ TRỢ MẠNG LƯỚI
    // ==========================================
    private Color getColorForName(String name) {
        if(name.equals("ALL")) return new Color(79, 70, 229); 
        int hash = Math.abs(name.hashCode());
        return AVATAR_COLORS[hash % AVATAR_COLORS.length];
    }

    private void addOnlineUser(String username) {
        for (Component c : onlineUsersPanel.getComponents()) {
            if (c.getName() != null && c.getName().equals(username)) return;
        }

        JPanel userPanel = new JPanel(new BorderLayout());
        userPanel.setOpaque(false);
        userPanel.setName(username);

        JButton btnAvatar = new JButton(new AvatarIcon(username, 50, getColorForName(username)));
        btnAvatar.setContentAreaFilled(false);
        btnAvatar.setBorderPainted(false);
        btnAvatar.setCursor(new Cursor(Cursor.HAND_CURSOR));

        JLabel lblName = new JLabel(username, SwingConstants.CENTER);
        lblName.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblName.setBorder(new EmptyBorder(5, 0, 0, 0));

        userPanel.add(btnAvatar, BorderLayout.CENTER);
        userPanel.add(lblName, BorderLayout.SOUTH);

        btnAvatar.addActionListener(e -> {
            addActiveChat(username, username, null);
        });

        onlineUsersPanel.add(userPanel);
        onlineUsersPanel.revalidate();
    }

    private void removeOnlineUser(String username) {
        for (Component c : onlineUsersPanel.getComponents()) {
            if (c.getName() != null && c.getName().equals(username)) {
                onlineUsersPanel.remove(c);
                onlineUsersPanel.revalidate();
                onlineUsersPanel.repaint();
                break;
            }
        }
    }

    private void connectToServer(String command, String user) {
        try {
            if (socket == null || socket.isClosed()) {
                socket = new Socket("localhost", 8888);
                outToServer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);
                inFromServer = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
                startListenerThread();
            }
            currentUsername = user;
            lblMyProfile.setIcon(new AvatarIcon(user, 45, getColorForName(user)));
            outToServer.println(command);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(frame, "Lỗi kết nối Server!");
        }
    }

    private void startListenerThread() {
        Thread listener = new Thread(() -> {
            try {
                String response;
                while ((response = inFromServer.readLine()) != null) {
                    final String res = response;
                    
                    if (res.startsWith("MESSAGE Login successful!")) {
                        SwingUtilities.invokeLater(() -> {
                            cardLayout.show(cardPanel, "CHAT");
                            outToServer.println("GET_ONLINE_USERS"); 
                            loadHistory(); 
                        });
                    } 
                    else if (res.startsWith("MESSAGE Registration successful!")) {
                        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(frame, "Đăng ký thành công! Hãy đăng nhập."));
                    }
                    else if (res.startsWith("ONLINE_LIST ")) {
                        SwingUtilities.invokeLater(() -> {
                            String[] users = res.substring(12).split(",");
                            for (String u : users) {
                                if (!u.isEmpty() && !u.equals(currentUsername)) {
                                    onlineUsers.add(u);
                                    addOnlineUser(u);
                                    updateUserStatus(u, true); 
                                }
                            }
                        });
                    }
                    else if (res.startsWith("USER_JOINED ")) {
                        SwingUtilities.invokeLater(() -> {
                            String u = res.substring(12).trim();
                            onlineUsers.add(u);
                            addOnlineUser(u);
                            updateUserStatus(u, true);
                        });
                    }
                    else if (res.startsWith("USER_LEFT ")) {
                        SwingUtilities.invokeLater(() -> {
                            String u = res.substring(10).trim();
                            onlineUsers.remove(u);
                            removeOnlineUser(u);
                            updateUserStatus(u, false); 
                        });
                    }
                    else if (res.startsWith("BROADCAST")) {
                        SwingUtilities.invokeLater(() -> {
                            String[] splitMsg = res.split(":", 2);
                            if (splitMsg.length == 2) {
                                String rawSender = splitMsg[0].replace("BROADCAST ", "").trim();
                                if (!rawSender.equals(currentUsername)) {
                                    appendMessageBubble("ALL", rawSender, splitMsg[1].trim(), false, true);
                                }
                            }
                        });
                    }
                    else if (res.startsWith("PRIVATE")) {
                        SwingUtilities.invokeLater(() -> {
                            String[] splitMsg = res.split(":", 2);
                            if (splitMsg.length == 2) {
                                String rawSender = splitMsg[0].replace("PRIVATE từ ", "").trim();
                                appendMessageBubble(rawSender, rawSender, splitMsg[1].trim(), false, true);
                            }
                        });
                    }
                }
            } catch (IOException e) {}
        });
        listener.start();
    }

    // ==========================================
    // CÁC COMPONENT TỰ VẼ ĐỒ HỌA 
    // ==========================================
    class BubblePanel extends JPanel {
        private Color bgColor;
        public BubblePanel(Color bgColor) {
            this.bgColor = bgColor;
            setOpaque(false);
        }
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(bgColor);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20); 
            g2.dispose();
        }
    }

    class AvatarIcon implements Icon {
        private int size;
        private String letter;
        private Color bgColor;

        public AvatarIcon(String name, int size, Color bgColor) {
            this.size = size;
            this.bgColor = bgColor;
            if (name == null || name.isEmpty()) this.letter = "?";
            else if (name.equals("ALL")) this.letter = "#";
            else this.letter = name.substring(0, 1).toUpperCase();
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(bgColor);
            g2.fillOval(x, y, size, size);
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Segoe UI", Font.BOLD, size / 2));
            FontMetrics fm = g2.getFontMetrics();
            int textX = x + (size - fm.stringWidth(letter)) / 2;
            int textY = y + ((size - fm.getHeight()) / 2) + fm.getAscent();
            g2.drawString(letter, textX, textY);
            g2.dispose();
        }

        @Override
        public int getIconWidth() { return size; }
        @Override
        public int getIconHeight() { return size; }
    }

    public static void main(String[] args) {
        try {
            UIManager.put("defaultFont", new Font("Segoe UI", Font.PLAIN, 15));
            UIManager.put("Component.arc", 25); 
            UIManager.put("TextComponent.arc", 25);
            UIManager.put("Button.arc", 999); 
            UIManager.put("TextComponent.margin", new Insets(8, 15, 8, 15));
            UIManager.put("Component.innerFocusWidth", 0);
            UIManager.put("Component.focusWidth", 2);
            UIManager.put("ScrollBar.thumbArc", 999);
            UIManager.put("ScrollBar.thumbInsets", new Insets(2, 2, 2, 2));
            UIManager.setLookAndFeel(new FlatLightLaf());
        } catch (Exception ex) {}
        SwingUtilities.invokeLater(() -> new ModernChatApp());
    }
}