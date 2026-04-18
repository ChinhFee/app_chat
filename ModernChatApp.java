import com.formdev.flatlaf.FlatLightLaf;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;

public class ModernChatApp {
    private JFrame frame;
    private JPanel cardPanel;
    private CardLayout cardLayout;

    // UI - Đăng nhập / Đăng ký 
    private JTextField txtUserLogin, txtUserReg, txtEmailReg;
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
    private String currentCard = "AUTH";
    private String pendingUsername = ""; 

    // QUẢN LÝ ĐA HỘP THOẠI VÀ TRẠNG THÁI
    private HashMap<String, JPanel> chatPanelsMap = new HashMap<>();
    private HashSet<String> activeChats = new HashSet<>();
    private HashSet<String> onlineUsers = new HashSet<>(); 
    private HashMap<String, JLabel> statusLabelsMap = new HashMap<>(); 
    
    private HashMap<String, String> lastDateMap = new HashMap<>(); 
    
    // Mạng UDP Multicast (Dành riêng cho Group Chat)
    private String pendingMulticastIp = null;
    private int pendingMulticastPort = 8888;
    private HashMap<String, MulticastSocket> groupSockets = new HashMap<>();
    private HashMap<String, InetAddress> groupAddresses = new HashMap<>();
    private HashMap<String, Integer> groupPorts = new HashMap<>();

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

    private JLabel createLogoLabel() {
        JLabel lblLogo = new JLabel();
        try {
            ImageIcon icon = new ImageIcon("avt.png"); 
            if (icon.getIconWidth() > -1) {
                Image img = icon.getImage().getScaledInstance(200, 200, Image.SCALE_SMOOTH);
                lblLogo.setIcon(new ImageIcon(img));
            } else {
                lblLogo.setText("Z-CHATS");
                lblLogo.setFont(new Font("Segoe UI", Font.BOLD, 36));
                lblLogo.setForeground(new Color(79, 70, 229));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        lblLogo.setAlignmentX(Component.CENTER_ALIGNMENT);
        return lblLogo;
    }

    private JPanel createAuthScreen() {
        JPanel wrapper = new JPanel(new GridBagLayout());
        wrapper.setBackground(new Color(243, 246, 253));

        JPanel authBox = new JPanel(new CardLayout());
        authBox.setPreferredSize(new Dimension(420, 560)); 
        authBox.putClientProperty("FlatLaf.style", "arc: 35");
        authBox.setBackground(Color.WHITE);
        authBox.setBorder(new EmptyBorder(20, 40, 20, 40));

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

        loginPanel.add(Box.createVerticalStrut(40));
        loginPanel.add(lblLoginTitle);
        loginPanel.add(Box.createVerticalStrut(45));
        loginPanel.add(txtUserLogin);
        loginPanel.add(Box.createVerticalStrut(20));
        loginPanel.add(txtPassLogin);
        loginPanel.add(Box.createVerticalStrut(30));
        loginPanel.add(btnLogin);
        loginPanel.add(Box.createVerticalStrut(10));
        loginPanel.add(btnGoToReg);

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

        txtEmailReg = new JTextField();
        txtEmailReg.putClientProperty("JTextField.placeholderText", "Email cá nhân");
        txtEmailReg.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        txtEmailReg.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        txtEmailReg.setAlignmentX(Component.CENTER_ALIGNMENT); 
        
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

        regPanel.add(Box.createVerticalStrut(20));
        regPanel.add(lblRegTitle);
        regPanel.add(Box.createVerticalStrut(35));
        regPanel.add(txtUserReg);
        regPanel.add(Box.createVerticalStrut(15));
        regPanel.add(txtEmailReg); 
        regPanel.add(Box.createVerticalStrut(15));
        regPanel.add(txtPassReg);
        regPanel.add(Box.createVerticalStrut(25));
        regPanel.add(btnRegister);
        regPanel.add(Box.createVerticalStrut(5));
        regPanel.add(btnBackToLogin);

        authBox.add(loginPanel, "LOGIN_SCREEN");
        authBox.add(regPanel, "REG_SCREEN");

        CardLayout cl = (CardLayout) authBox.getLayout();
        btnGoToReg.addActionListener(e -> cl.show(authBox, "REG_SCREEN"));
        btnBackToLogin.addActionListener(e -> cl.show(authBox, "LOGIN_SCREEN"));

        btnLogin.addActionListener(e -> {
            String u = txtUserLogin.getText().trim();
            String p = new String(txtPassLogin.getPassword());
            if(!u.isEmpty() && !p.isEmpty()) {
                connectToServer("LOGIN " + u + "|" + p, u);
            } else {
                JOptionPane.showMessageDialog(frame, "Vui lòng nhập tên đăng nhập và mật khẩu!");
            }
        });

        btnRegister.addActionListener(e -> {
            String u = txtUserReg.getText().trim();
            String mail = txtEmailReg.getText().trim();
            String p = new String(txtPassReg.getPassword());
            
            if(!u.isEmpty() && !p.isEmpty() && !mail.isEmpty()) {
                connectToServer("REGISTER " + u + "|" + p + "|" + mail, u);
            } else {
                JOptionPane.showMessageDialog(frame, "Vui lòng điền đầy đủ Tên, Email và Mật khẩu!");
            }
        });

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        
        gbc.gridy = 0;
        gbc.insets = new Insets(0, 0, 20, 0); 
        wrapper.add(createLogoLabel(), gbc);
        
        gbc.gridy = 1;
        gbc.insets = new Insets(0, 0, 0, 0);
        wrapper.add(authBox, gbc);

        return wrapper;
    }

    private JPanel createMainWorkspace() {
        JPanel mainPanel = new JPanel(new BorderLayout(15, 15));
        mainPanel.setBackground(new Color(243, 246, 253));
        mainPanel.setBorder(new EmptyBorder(15, 15, 15, 15));

        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setPreferredSize(new Dimension(320, 0));
        sidebar.setBackground(new Color(250, 250, 252));
        sidebar.putClientProperty("FlatLaf.style", "arc: 20");
        sidebar.setBorder(new EmptyBorder(20, 15, 20, 15));

        JPanel sidebarHeader = new JPanel(new BorderLayout(0, 15));
        sidebarHeader.setOpaque(false);
        
        JPanel myProfilePanel = new JPanel(new BorderLayout(10, 0));
        myProfilePanel.setOpaque(false);
        
        lblMyProfile = new JLabel(); 
        lblMyProfile.setCursor(new Cursor(Cursor.HAND_CURSOR));
        lblMyProfile.setToolTipText("Nhấn vào đây để xem/sửa thông tin cá nhân");
        lblMyProfile.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                showEditProfilePopup();
            }
        });
        
        JLabel lblChats = new JLabel("Z-Chats");
        lblChats.setFont(new Font("Segoe UI", Font.BOLD, 22));
        
        JButton btnCreateGroup = new JButton("+");
        btnCreateGroup.setToolTipText("Tạo nhóm chat mới");
        btnCreateGroup.setFont(new Font("Segoe UI", Font.PLAIN, 32));
        btnCreateGroup.setForeground(new Color(79, 70, 229));
        btnCreateGroup.setContentAreaFilled(false);
        btnCreateGroup.setBorderPainted(false);
        btnCreateGroup.setFocusPainted(false);
        btnCreateGroup.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnCreateGroup.addActionListener(e -> showCreateGroupPopup());

        myProfilePanel.add(lblMyProfile, BorderLayout.WEST);
        myProfilePanel.add(lblChats, BorderLayout.CENTER);
        myProfilePanel.add(btnCreateGroup, BorderLayout.EAST);
        
        sidebarHeader.add(myProfilePanel, BorderLayout.NORTH);

        JTextField txtSearch = new JTextField();
        txtSearch.putClientProperty("JTextField.placeholderText", "Tìm kiếm bạn bè...");
        txtSearch.setPreferredSize(new Dimension(0, 42));
        sidebarHeader.add(txtSearch, BorderLayout.CENTER);
        
        sidebar.add(sidebarHeader, BorderLayout.NORTH);

        JPanel sidebarContent = new JPanel(new BorderLayout());
        sidebarContent.setOpaque(false);

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

        JPanel chatArea = new JPanel(new BorderLayout());
        chatArea.setBackground(Color.WHITE);
        chatArea.putClientProperty("FlatLaf.style", "arc: 20");

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

        chatScrollPane = new JScrollPane(); 
        chatScrollPane.setBorder(null);
        chatScrollPane.getVerticalScrollBar().setUnitIncrement(16);
        chatScrollPane.setViewportView(getOrCreateChatPanel("ALL")); 
        chatArea.add(chatScrollPane, BorderLayout.CENTER);

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
                    String uniqueName = System.currentTimeMillis() + "_" + file.getName();
                    fileClient.sendFile("localhost", 1988, file.getAbsolutePath(), uniqueName);
                    
                    String fileMsg = "[FILE]: " + uniqueName;

                    if (groupSockets.containsKey(currentChatTarget)) {
                        String formattedMsg = currentUsername + ": " + fileMsg;
                        byte[] sendData = formattedMsg.getBytes("UTF-8");
                        DatagramPacket sendPacket = new DatagramPacket(
                            sendData, sendData.length, 
                            groupAddresses.get(currentChatTarget), 
                            groupPorts.get(currentChatTarget)
                        );
                        groupSockets.get(currentChatTarget).send(sendPacket);
                    } else if (currentChatTarget.equals("ALL")) {
                        outToServer.println("CHATALL " + fileMsg);
                    } else {
                        outToServer.println("CHATPRIVATE " + currentChatTarget + "|" + fileMsg);
                    }

                    appendMessageBubble(currentChatTarget, currentUsername, fileMsg, true, true, null);
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(frame, "Lỗi gửi file: Hãy chắc chắn File Server đang chạy!");
                    ex.printStackTrace();
                }
            }
        });

        btnSend.addActionListener(e -> {
            String msg = txtMessage.getText().trim();
            if (!msg.isEmpty()) {
                
                if (msg.startsWith("JOINGROUP ")) {
                    outToServer.println(msg);
                    txtMessage.setText("");
                    return;
                }

                if (groupSockets.containsKey(currentChatTarget)) {
                    try {
                        String formattedMsg = currentUsername + ": " + msg;
                        byte[] sendData = formattedMsg.getBytes("UTF-8");
                        DatagramPacket sendPacket = new DatagramPacket(
                            sendData, sendData.length, 
                            groupAddresses.get(currentChatTarget), 
                            groupPorts.get(currentChatTarget)
                        );
                        groupSockets.get(currentChatTarget).send(sendPacket);
                        
                        appendMessageBubble(currentChatTarget, currentUsername, msg, true, true, null);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                } else if (currentChatTarget.equals("ALL")) {
                    outToServer.println("CHATALL " + msg);
                    appendMessageBubble(currentChatTarget, currentUsername, msg, true, true, null);
                } else {
                    outToServer.println("CHATPRIVATE " + currentChatTarget + "|" + msg);
                    appendMessageBubble(currentChatTarget, currentUsername, msg, true, true, null);
                }
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

    private void showCreateGroupPopup() {
        JDialog dialog = new JDialog(frame, "Tạo Nhóm Chat", true);
        dialog.setSize(400, 500);
        dialog.setLocationRelativeTo(frame);
        dialog.getContentPane().setBackground(Color.WHITE);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(Color.WHITE);
        content.setBorder(new EmptyBorder(25, 25, 25, 25));

        JLabel lblTitle = new JLabel("Nhập Tên Nhóm");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lblTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        JTextField txtGroupName = new JTextField();
        txtGroupName.putClientProperty("JTextField.placeholderText", "Ví dụ: Team Backend");
        txtGroupName.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        txtGroupName.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblMembers = new JLabel("Mời thành viên (Đang hoạt động):");
        lblMembers.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lblMembers.setAlignmentX(Component.LEFT_ALIGNMENT);
        lblMembers.setBorder(new EmptyBorder(20, 0, 10, 0));

        JPanel usersPanel = new JPanel();
        usersPanel.setLayout(new BoxLayout(usersPanel, BoxLayout.Y_AXIS));
        usersPanel.setBackground(Color.WHITE);
        
        HashMap<String, JCheckBox> checkboxes = new HashMap<>();
        for (String user : onlineUsers) {
            if (!user.equals(currentUsername)) {
                JCheckBox cb = new JCheckBox(user);
                cb.setBackground(Color.WHITE);
                cb.setFont(new Font("Segoe UI", Font.PLAIN, 15));
                cb.setCursor(new Cursor(Cursor.HAND_CURSOR));
                checkboxes.put(user, cb);
                usersPanel.add(cb);
                usersPanel.add(Box.createVerticalStrut(5));
            }
        }
        
        if (checkboxes.isEmpty()) {
            JLabel lblEmpty = new JLabel("Chưa có ai online để mời.");
            lblEmpty.setFont(new Font("Segoe UI", Font.ITALIC, 13));
            lblEmpty.setForeground(Color.GRAY);
            usersPanel.add(lblEmpty);
        }

        JScrollPane scroll = new JScrollPane(usersPanel);
        scroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(230, 230, 230)));

        JButton btnCreate = new JButton("Bắt Đầu Trò Chuyện");
        btnCreate.setBackground(new Color(79, 70, 229));
        btnCreate.setForeground(Color.WHITE);
        btnCreate.setFont(new Font("Segoe UI", Font.BOLD, 15));
        btnCreate.putClientProperty("FlatLaf.style", "arc: 999");
        btnCreate.setMaximumSize(new Dimension(Integer.MAX_VALUE, 45));
        btnCreate.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnCreate.setCursor(new Cursor(Cursor.HAND_CURSOR));

        btnCreate.addActionListener(e -> {
            String gName = txtGroupName.getText().trim();
            if (gName.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Vui lòng nhập tên nhóm!");
                return;
            }
            
            outToServer.println("JOINGROUP " + gName);
            
            for (String u : checkboxes.keySet()) {
                if (checkboxes.get(u).isSelected()) {
                    outToServer.println("CHATPRIVATE " + u + "|[INVITE]:" + gName);
                }
            }
            dialog.dispose();
        });

        content.add(lblTitle);
        content.add(Box.createVerticalStrut(8));
        content.add(txtGroupName);
        content.add(lblMembers);
        content.add(scroll);
        content.add(Box.createVerticalStrut(25));
        content.add(btnCreate);

        dialog.add(content);
        dialog.setVisible(true);
    }

    private void showEditProfilePopup() {
        JDialog dialog = new JDialog(frame, "Cài Đặt Tài Khoản", true);
        dialog.setSize(380, 500); 
        dialog.setLocationRelativeTo(frame);
        dialog.getContentPane().setBackground(Color.WHITE);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(Color.WHITE);
        content.setBorder(new EmptyBorder(25, 40, 25, 40));

        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        headerPanel.setBackground(Color.WHITE);
        headerPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel lblBigAvt = new JLabel(new AvatarIcon(currentUsername, 75, getColorForName(currentUsername)));
        headerPanel.add(lblBigAvt);
        
        JPanel pnlName = new JPanel(new BorderLayout());
        pnlName.setOpaque(false);
        pnlName.setMaximumSize(new Dimension(Integer.MAX_VALUE, 25)); 
        JLabel lblName = new JLabel("Tên đăng nhập (Chỉ xem):");
        lblName.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblName.setForeground(new Color(100, 100, 100));
        pnlName.add(lblName, BorderLayout.WEST); 
        pnlName.setAlignmentX(Component.CENTER_ALIGNMENT); 
        
        JTextField txtName = new JTextField(currentUsername);
        txtName.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        txtName.setAlignmentX(Component.CENTER_ALIGNMENT);
        txtName.setEditable(false);
        txtName.setBackground(new Color(245, 245, 245));
        txtName.setToolTipText("Tên đăng nhập được sử dụng làm mã định danh trên hệ thống nên không thể thay đổi.");

        JPanel pnlPass = new JPanel(new BorderLayout());
        pnlPass.setOpaque(false);
        pnlPass.setMaximumSize(new Dimension(Integer.MAX_VALUE, 25));
        JLabel lblPass = new JLabel("Mật khẩu mới:");
        lblPass.setFont(new Font("Segoe UI", Font.BOLD, 13));
        pnlPass.add(lblPass, BorderLayout.WEST);
        pnlPass.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        JPasswordField txtPass = new JPasswordField();
        txtPass.putClientProperty("JTextField.placeholderText", "Bỏ trống nếu không đổi...");
        txtPass.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        txtPass.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel pnlEmail = new JPanel(new BorderLayout());
        pnlEmail.setOpaque(false);
        pnlEmail.setMaximumSize(new Dimension(Integer.MAX_VALUE, 25));
        JLabel lblEmail = new JLabel("Email mới:");
        lblEmail.setFont(new Font("Segoe UI", Font.BOLD, 13));
        pnlEmail.add(lblEmail, BorderLayout.WEST);
        pnlEmail.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        JTextField txtEmail = new JTextField();
        txtEmail.putClientProperty("JTextField.placeholderText", "Nhập email mới để thay đổi...");
        txtEmail.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        txtEmail.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton btnSave = new JButton("Lưu Thay Đổi");
        btnSave.setBackground(new Color(16, 185, 129));
        btnSave.setForeground(Color.WHITE);
        btnSave.setFont(new Font("Segoe UI", Font.BOLD, 15));
        btnSave.putClientProperty("FlatLaf.style", "arc: 999");
        btnSave.setMaximumSize(new Dimension(Integer.MAX_VALUE, 45));
        btnSave.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnSave.setCursor(new Cursor(Cursor.HAND_CURSOR));

        btnSave.addActionListener(e -> {
            String newPass = new String(txtPass.getPassword()).trim();
            String newEmail = txtEmail.getText().trim();
            
            if(newPass.isEmpty() && newEmail.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Bạn chưa nhập thông tin mới nào để cập nhật!");
                return;
            }

            outToServer.println("UPDATE_PROFILE " + newPass + "|" + newEmail);
            
            JOptionPane.showMessageDialog(dialog, 
                "Đã chỉnh sửa thông tin!", 
                "Thông báo", JOptionPane.INFORMATION_MESSAGE);
            dialog.dispose();
        });

        content.add(headerPanel);
        content.add(Box.createVerticalStrut(15));
        content.add(pnlName); 
        content.add(Box.createVerticalStrut(5));
        content.add(txtName);
        content.add(Box.createVerticalStrut(15));
        content.add(pnlPass);
        content.add(Box.createVerticalStrut(5));
        content.add(txtPass);
        content.add(Box.createVerticalStrut(15));
        content.add(pnlEmail);
        content.add(Box.createVerticalStrut(5));
        content.add(txtEmail);
        content.add(Box.createVerticalStrut(25));
        content.add(btnSave);

        dialog.add(content);
        dialog.setVisible(true);
    }

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

    // ==========================================
    // HÀM [CẬP NHẬT] KHÔI PHỤC TẠO MÀU AVATAR 
    // ==========================================
    private Color getColorForName(String name) {
        if(name == null) return Color.GRAY;
        if(name.equals("ALL")) return new Color(79, 70, 229); 
        int hash = Math.abs(name.hashCode());
        return AVATAR_COLORS[hash % AVATAR_COLORS.length];
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
                
                if (targetId.equals("ALL")) {
                    lblChatStatus.setText("Public Channel");
                    lblChatStatus.setForeground(new Color(16, 163, 127));
                } else if (groupSockets.containsKey(targetId)) {
                    lblChatStatus.setText("Nhóm Chat (Multicast)");
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

    private void saveMessageToHistory(String targetId, String sender, String message, boolean isMe, String timeStr) {
        try {
            File f = new File("history_" + currentUsername + ".txt");
            FileWriter fw = new FileWriter(f, true);
            String cleanMsg = message.replace("\n", "\\n"); 
            fw.write(targetId + "|::|" + sender + "|::|" + isMe + "|::|" + timeStr + "|::|" + cleanMsg + "\n");
            fw.close();
        } catch (IOException e) {
            System.err.println("Error saving chat history for " + currentUsername + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadHistory() {
        lastDateMap.clear(); 
        try {
            File f = new File("history_" + currentUsername + ".txt");
            if (!f.exists()) return;
            BufferedReader br = new BufferedReader(new FileReader(f));
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split("\\|\\:\\:\\|");
                if (parts.length >= 5) { 
                    String targetId = parts[0];
                    String sender = parts[1];
                    boolean isMe = Boolean.parseBoolean(parts[2]);
                    String timeStr = parts[3];
                    String message = parts[4].replace("\\n", "\n");
                    appendMessageBubble(targetId, sender, message, isMe, false, timeStr);
                } else if (parts.length == 4) { 
                    String targetId = parts[0];
                    String sender = parts[1];
                    boolean isMe = Boolean.parseBoolean(parts[2]);
                    String message = parts[3].replace("\\n", "\n");
                    appendMessageBubble(targetId, sender, message, isMe, false, new SimpleDateFormat("dd/MM/yyyy HH:mm").format(new Date()));
                }
            }
            br.close();
        } catch (IOException e) {
            System.err.println("Error loading chat history for " + currentUsername + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void downloadOrOpenFile(File sourceFile, String fileName) {
        if (!sourceFile.exists()) {
            JOptionPane.showMessageDialog(frame, "Lỗi: File không còn tồn tại trên Server!");
            return;
        }
        
        String realName = fileName.contains("_") ? fileName.substring(fileName.indexOf("_") + 1) : fileName;
        
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File(realName));
        chooser.setDialogTitle("Lưu tệp đính kèm");
        
        if (chooser.showSaveDialog(frame) == JFileChooser.APPROVE_OPTION) {
            File destFile = chooser.getSelectedFile();
            try {
                Files.copy(sourceFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                int open = JOptionPane.showConfirmDialog(frame, "Đã tải xong! Bạn có muốn mở file lên xem không?", "Tải thành công", JOptionPane.YES_NO_OPTION);
                if (open == JOptionPane.YES_OPTION) {
                    Desktop.getDesktop().open(destFile);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(frame, "Lỗi khi lưu file: " + ex.getMessage());
            }
        }
    }

    @SuppressWarnings("deprecation")
    private void joinMulticastGroup(String ip, int port, String groupName) {
        try {
            if (groupSockets.containsKey(groupName)) return;

            InetAddress address = InetAddress.getByName(ip);
            MulticastSocket mSocket = new MulticastSocket(port);
            
            mSocket.joinGroup(address);
            
            groupSockets.put(groupName, mSocket);
            groupAddresses.put(groupName, address);
            groupPorts.put(groupName, port);
            
            addActiveChat(groupName, groupName, "Nhóm Chat (Multicast)");
            
            currentChatTarget = groupName;
            lblChatName.setText(groupName);
            lblChatAvatar.setIcon(new AvatarIcon(groupName, 45, getColorForName(groupName)));
            lblChatStatus.setText("Nhóm Chat (Multicast)");
            lblChatStatus.setForeground(new Color(16, 163, 127));
            chatScrollPane.setViewportView(getOrCreateChatPanel(groupName));
            
            Thread receiver = new Thread(() -> {
                byte[] buf = new byte[4096];
                while (true) {
                    try {
                        DatagramPacket packet = new DatagramPacket(buf, buf.length);
                        mSocket.receive(packet);
                        String msg = new String(buf, 0, packet.getLength(), "UTF-8");
                        
                        String[] parts = msg.split(": ", 2);
                        if (parts.length == 2) {
                            String sender = parts[0];
                            String content = parts[1];
                            if (!sender.equals(currentUsername)) {
                                SwingUtilities.invokeLater(() -> {
                                    appendMessageBubble(groupName, sender, content, false, true, null);
                                });
                            }
                        }
                    } catch (IOException e) {
                        break;
                    }
                }
            });
            receiver.start();
            
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(frame, "Lỗi tham gia nhóm UDP: " + e.getMessage());
        }
    }

    private void appendMessageBubble(String targetId, String sender, String message, boolean isMe, boolean saveToFile, String timeStr) {
        JPanel targetPanel = getOrCreateChatPanel(targetId);
        
        if (!targetId.equals("ALL")) {
            addActiveChat(targetId, targetId, null);
        }

        if (timeStr == null || timeStr.isEmpty()) {
            timeStr = new SimpleDateFormat("dd/MM/yyyy HH:mm").format(new Date());
        }

        String datePart = "";
        String timePart = timeStr;

        if (timeStr.contains(" ")) {
            String[] parts = timeStr.split(" ");
            if (parts.length >= 2) {
                datePart = parts[0];
                timePart = parts[1]; 
            }
        } else {
            datePart = new SimpleDateFormat("dd/MM/yyyy").format(new Date());
        }

        String lastDate = lastDateMap.getOrDefault(targetId, "");
        if (!datePart.equals(lastDate)) {
            String todayStr = new SimpleDateFormat("dd/MM/yyyy").format(new Date());
            String displayText = datePart.equals(todayStr) ? "Hôm nay" : datePart;

            JPanel separatorPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
            separatorPanel.setOpaque(false);
            separatorPanel.setBorder(new EmptyBorder(10, 0, 10, 0));

            JLabel lblDate = new JLabel("--- " + displayText + " ---");
            lblDate.setFont(new Font("Segoe UI", Font.BOLD, 12));
            lblDate.setForeground(new Color(150, 150, 150)); 

            separatorPanel.add(lblDate);
            targetPanel.add(separatorPanel);

            lastDateMap.put(targetId, datePart);
        }

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.setBorder(new EmptyBorder(5, 10, 5, 10));

        JLabel lblAvt = new JLabel(new AvatarIcon(sender, 40, getColorForName(sender)));
        lblAvt.setVerticalAlignment(SwingConstants.BOTTOM);

        Component messageContentComp;
        boolean isFile = message.startsWith("[FILE]: ");

        if (isFile) {
            String fileName = message.substring(8);
            File f = new File("server_files/" + fileName);
            String lowerName = fileName.toLowerCase();

            if (lowerName.endsWith(".jpg") || lowerName.endsWith(".png") || lowerName.endsWith(".jpeg") || lowerName.endsWith(".gif")) {
                JLabel imgLabel = new JLabel();
                if (f.exists()) {
                    ImageIcon icon = new ImageIcon(f.getAbsolutePath());
                    int width = icon.getIconWidth();
                    int height = icon.getIconHeight();
                    
                    if (width > 200) {
                        height = (int) (height * (200.0 / width));
                        width = 200;
                    } else if (width <= 0) { 
                        width = 150; height = 150;
                    }
                    
                    Image newImg = icon.getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH);
                    imgLabel.setIcon(new ImageIcon(newImg));
                } else {
                    imgLabel.setText("[Hình ảnh đang tải hoặc bị lỗi]");
                    imgLabel.setForeground(isMe ? Color.WHITE : Color.BLACK);
                }
                
                imgLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
                imgLabel.setToolTipText("Nhấn để xem / Tải về");
                imgLabel.addMouseListener(new MouseAdapter() {
                    public void mouseClicked(MouseEvent e) {
                        downloadOrOpenFile(f, fileName);
                    }
                });
                messageContentComp = imgLabel;
                
            } else {
                String realName = fileName.contains("_") ? fileName.substring(fileName.indexOf("_") + 1) : fileName;
                JButton fileBtn = new JButton("📄 " + realName);
                fileBtn.setFont(new Font("Segoe UI", Font.BOLD, 13));
                fileBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
                fileBtn.setFocusPainted(false);
                
                if (isMe) {
                    fileBtn.setBackground(new Color(255, 255, 255, 80));
                    fileBtn.setForeground(Color.WHITE);
                } else {
                    fileBtn.setBackground(Color.WHITE);
                    fileBtn.setForeground(new Color(50, 50, 50));
                }
                
                fileBtn.setToolTipText("Nhấn để tải tệp này về");
                fileBtn.addActionListener(e -> downloadOrOpenFile(f, fileName));
                messageContentComp = fileBtn;
            }
            
        } else {
            String textToShow = message.startsWith("[Đã đính kèm tệp]: ") ? "File: " + message.substring(20) : message;
            JTextArea txtMsg = new JTextArea(textToShow);
            txtMsg.setWrapStyleWord(true);
            txtMsg.setLineWrap(true);
            txtMsg.setEditable(false);
            txtMsg.setOpaque(false);
            txtMsg.setFont(new Font("Segoe UI", Font.PLAIN, 15));
            
            int textWidth = Math.min(textToShow.length() * 8, 400); 
            txtMsg.setSize(new Dimension(textWidth, Short.MAX_VALUE));
            
            if (isMe) txtMsg.setForeground(Color.WHITE);
            else txtMsg.setForeground(Color.BLACK);
            
            messageContentComp = txtMsg;
        }

        BubblePanel bubble = new BubblePanel(isMe ? new Color(0, 132, 255) : new Color(228, 230, 235));
        bubble.setLayout(new BorderLayout());
        bubble.setBorder(new EmptyBorder(10, 15, 10, 15));
        bubble.add(messageContentComp, BorderLayout.CENTER);

        if (isMe) {
            JLabel lblTime = new JLabel(timePart, SwingConstants.RIGHT);
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
            JLabel lblName = new JLabel(sender);
            lblName.setFont(new Font("Segoe UI", Font.BOLD, 12));
            lblName.setForeground(new Color(100, 100, 100));
            lblName.setBorder(new EmptyBorder(0, 5, 3, 0));

            JLabel lblTime = new JLabel(timePart, SwingConstants.LEFT);
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
            saveMessageToHistory(targetId, sender, message, isMe, timeStr);
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

            currentChatTarget = username;
            lblChatName.setText(username);
            lblChatAvatar.setIcon(new AvatarIcon(username, 45, getColorForName(username)));
            
            lblChatStatus.setText("Đang hoạt động");
            lblChatStatus.setForeground(new Color(16, 163, 127));

            JPanel targetPanel = getOrCreateChatPanel(username);
            chatScrollPane.setViewportView(targetPanel);

            SwingUtilities.invokeLater(() -> {
                JScrollBar vertical = chatScrollPane.getVerticalScrollBar();
                vertical.setValue(vertical.getMaximum());
            });
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
            pendingUsername = user;
            outToServer.println(command);
        } catch (IOException e) {
            System.err.println("Unable to connect to server for user " + user + ": " + e.getMessage());
            e.printStackTrace();
            JOptionPane.showMessageDialog(frame, "Lỗi kết nối Server: " + e.getMessage());
        }
    }

    // ==========================================
    // ĐÃ SỬA TRẬT TỰ CÁC LỆNH IF...ELSE ĐỂ KHÔNG BỊ "NUỐT" TIN NHẮN SERVER NỮA
    // ==========================================
    private void startListenerThread() {
        Thread listener = new Thread(() -> {
            try {
                String response;
                while ((response = inFromServer.readLine()) != null) {
                    final String res = response;
                    
                    if (res.startsWith("LOGIN_SUCCESS ")) {
                        SwingUtilities.invokeLater(() -> {
                            if (pendingUsername != null) {
                                currentUsername = pendingUsername;
                                pendingUsername = null;
                                lblMyProfile.setIcon(new AvatarIcon(currentUsername, 45, getColorForName(currentUsername)));
                            }
                            currentCard = "CHAT";
                            cardLayout.show(cardPanel, "CHAT");
                            outToServer.println("GET_ONLINE_USERS"); 
                            loadHistory();
                        });
                    } 
                    else if (res.startsWith("MESSAGE Login successful!")) {
                        SwingUtilities.invokeLater(() -> {
                            if (pendingUsername != null) {
                                currentUsername = pendingUsername;
                                pendingUsername = null;
                                lblMyProfile.setIcon(new AvatarIcon(currentUsername, 45, getColorForName(currentUsername)));
                            }
                            currentCard = "CHAT";
                            cardLayout.show(cardPanel, "CHAT");
                            outToServer.println("GET_ONLINE_USERS"); 
                            loadHistory(); 
                        });
                    } 
                    else if (res.startsWith("MESSAGE Registration successful!")) {
                        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(frame, "Đăng ký thành công! Hãy đăng nhập."));
                    }
                    else if (res.startsWith("MESSAGE Successfully joined group [")) {
                        try {
                            String gName = res.substring(35, res.length() - 2);
                            if (pendingMulticastIp != null) {
                                String ip = pendingMulticastIp;
                                int port = pendingMulticastPort;
                                pendingMulticastIp = null;
                                SwingUtilities.invokeLater(() -> {
                                    joinMulticastGroup(ip, port, gName);
                                });
                            }
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                    // THẰNG NÀY PHẢI NẰM SAU CÙNG CỦA CÁC ĐIỀU KIỆN "MESSAGE..." BÊN TRÊN
                    else if (res.startsWith("MESSAGE ")) {
                        String text = res.substring(8).trim();
                        SwingUtilities.invokeLater(() -> {
                            if (!"CHAT".equals(currentCard)) {
                                JOptionPane.showMessageDialog(frame, text, "Thông báo từ Server", JOptionPane.INFORMATION_MESSAGE);
                            } else {
                                appendMessageBubble("ALL", "Server", text, false, true, null);
                            }
                        });
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
                                    appendMessageBubble("ALL", rawSender, splitMsg[1].trim(), false, true, null);
                                }
                            }
                        });
                    }
                    else if (res.startsWith("PRIVATE")) {
                        SwingUtilities.invokeLater(() -> {
                            String[] splitMsg = res.split(":", 2);
                            if (splitMsg.length == 2) {
                                String rawSender = splitMsg[0].replace("PRIVATE từ ", "").trim();
                                String content = splitMsg[1].trim();
                                
                                if (content.startsWith("[INVITE]:")) {
                                    String groupName = content.substring(9).trim();
                                    outToServer.println("JOINGROUP " + groupName);
                                    System.out.println("Hệ thống: Bạn đã được tự động thêm vào nhóm " + groupName + " do " + rawSender + " mời.");
                                } else {
                                    appendMessageBubble(rawSender, rawSender, content, false, true, null);
                                }
                            }
                        });
                    }
                    else if (res.startsWith("MULTICAST_JOIN ")) {
                        String[] parts = res.split(" ");
                        if (parts.length >= 3) {
                            pendingMulticastIp = parts[1];
                            pendingMulticastPort = Integer.parseInt(parts[2]);
                        }
                    }
                }
            } catch (IOException e) {
                System.err.println("Error reading from server socket:");
                e.printStackTrace();
            }
        });
        listener.start();
    }

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
        } catch (Exception ex) {
            System.err.println("Failed to initialize UI look and feel:");
            ex.printStackTrace();
        }
        SwingUtilities.invokeLater(() -> new ModernChatApp());
    }
}