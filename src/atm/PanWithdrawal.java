package atm;

import common.CommandDTO;
import common.RequestType;
import common.ResponseType;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.ByteBuffer;
import java.nio.channels.CompletionHandler;
import java.util.List; // (추가됨)

//*******************************************************************
// Name : PanWithdrawal
// Type : Class
// Description :  출금 화면 패널을 구현한 Class 이다.
//*******************************************************************
public class PanWithdrawal extends JPanel implements ActionListener
{
    private JLabel Label_Title;

    private JLabel Label_Account; // (추가됨)
    private JComboBox<String> Combo_Account; // (추가됨)
    private JButton Btn_LoadAccount; // (추가됨)

    private JLabel Label_Amount;
    private JTextField Text_Amount;


    private JButton Btn_Transfer;
    private JButton Btn_Close;

    ATMMain MainFrame;

    //*******************************************************************
    // Name : PanWithdrawal()
    // Type : 생성자
    // Description :  PanDeposite Class의 생성자 구현
    //*******************************************************************
    public PanWithdrawal(ATMMain parent)
    {
        MainFrame = parent;
        InitGUI();
    }

    //*******************************************************************
    // Name : InitGUI
    // Type : Method
    // Description :  출금 화면 패널의 GUI를 초기화 하는 메소드 구현
    //*******************************************************************
    private void InitGUI()
    {
        setLayout(null);
        setBounds(0,0,480,320);


        Label_Title = new JLabel("출금");
        Label_Title.setBounds(0,0,480,40);
        Label_Title.setHorizontalAlignment(JLabel.CENTER);
        add(Label_Title);

        // (추가됨) 계좌 선택 GUI
        Label_Account = new JLabel("출금 계좌");
        Label_Account.setBounds(0, 80, 100, 20);
        Label_Account.setHorizontalAlignment(JLabel.CENTER);
        add(Label_Account);

        Combo_Account = new JComboBox<>();
        Combo_Account.setBounds(100, 80, 240, 20);
        add(Combo_Account);

        Btn_LoadAccount = new JButton("조회");
        Btn_LoadAccount.setBounds(350, 80, 70, 20);
        Btn_LoadAccount.addActionListener(this);
        add(Btn_LoadAccount);


        Label_Amount = new JLabel("금액");
        Label_Amount.setBounds(0,120,100,20);
        Label_Amount.setHorizontalAlignment(JLabel.CENTER);
        add(Label_Amount);

        Text_Amount = new JTextField();
        Text_Amount.setBounds(100,120,350,20);
        Text_Amount.setEditable(true);
        Text_Amount.setToolTipText("숫자만 입력");
        add(Text_Amount);

        Btn_Transfer = new JButton("출금");
        Btn_Transfer.setBounds(100,250,70,20);
        Btn_Transfer.addActionListener(this);
        add(Btn_Transfer);

        Btn_Close = new JButton("취소");
        Btn_Close.setBounds(250,250,70,20);
        Btn_Close.addActionListener(this);
        add(Btn_Close);
    }

    //*******************************************************************
    // Name : actionPerformed
    // Type : Listner
    // Description :  입금 버튼, 취소 버튼의 동작을 구현
    //                입금, 취소 동작 후 메인 화면으로 변경되도록 구현
    //*******************************************************************
    public void actionPerformed(ActionEvent e)
    {
        if (e.getSource() == Btn_Transfer)
        {
            Withdrawal();
        }
        if (e.getSource() == Btn_Close)
        {
            this.setVisible(false);
            MainFrame.display("Main");
        }
        if (e.getSource() == Btn_LoadAccount) // (추가됨)
        {
            loadAccountList();
        }
    }

    // (추가됨) 계좌 목록 로드
    private void loadAccountList() {
        MainFrame.send(new CommandDTO(RequestType.VIEW), new CompletionHandler<Integer, ByteBuffer>() {
            @Override
            public void completed(Integer result, ByteBuffer attachment) {
                if (result == -1) return;
                attachment.flip();
                try {
                    ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(attachment.array());
                    ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream);
                    CommandDTO command = (CommandDTO) objectInputStream.readObject();

                    SwingUtilities.invokeLater(() -> {
                        if (command.getResponseType() == ResponseType.SUCCESS) {
                            Combo_Account.removeAllItems();
                            List<String> list = command.getAccountList();
                            if (list != null) {
                                for (String info : list) {
                                    String[] parts = info.split("/");
                                    if (parts.length >= 2) {
                                        Combo_Account.addItem(parts[1]); // 계좌번호
                                    }
                                }
                            }
                            JOptionPane.showMessageDialog(null, "계좌 목록을 불러왔습니다.", "알림", JOptionPane.PLAIN_MESSAGE);
                        } else {
                            JOptionPane.showMessageDialog(null, "계좌 목록 불러오기 실패", "ERROR", JOptionPane.ERROR_MESSAGE);
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            @Override
            public void failed(Throwable exc, ByteBuffer attachment) {}
        });
    }

    //*******************************************************************
    // Name : Withdrawal()
    // Type : Method
    // Description :  출금 화면의 데이터를 가지고 있는 CommandDTO를 생성하고,
    //                ATMMain의 Send 기능을 호출하여 서버에 출금 요청 메시지를 전달 하는 기능.
    //*******************************************************************
    public void Withdrawal() {
        long amount;
        String selectedAccount = (String) Combo_Account.getSelectedItem(); // (추가됨)

        if (selectedAccount == null || selectedAccount.isEmpty()) { // (추가됨)
            JOptionPane.showMessageDialog(null, "출금할 계좌를 먼저 조회하고 선택해주세요.", "입력 오류", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            amount = Long.parseLong(Text_Amount.getText());
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(null, "금액은 숫자만 입력해주세요.", "입력 오류", JOptionPane.ERROR_MESSAGE);
            return; // 입력값이 유효하지 않으면 메소드 종료
        }

        CommandDTO commandDTO = new CommandDTO(RequestType.WITHDRAW, MainFrame.userId, amount);
        commandDTO.setReceivedAccountNo(selectedAccount); // (추가됨) 타겟 계좌 지정

        MainFrame.send(commandDTO, new CompletionHandler<Integer, ByteBuffer>()
        {
            @Override
            public void completed(Integer result, ByteBuffer attachment)
            {
                if (result == -1)
                {
                    return;
                }
                attachment.flip();
                try
                {
                    ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(attachment.array());
                    ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream);
                    CommandDTO command = (CommandDTO) objectInputStream.readObject();
                    SwingUtilities.invokeLater(() ->
                    {
                        String contentText = null;
                        if (command.getResponseType() == ResponseType.SUCCESS)
                        {
                            contentText = "출금 되었습니다.";
                            JOptionPane.showMessageDialog(null, contentText, "SUCCESS_MESSAGE", JOptionPane.PLAIN_MESSAGE);
                            // 성공 시에만 메인으로 화면 전환
                            setVisible(false);
                            MainFrame.display("Main");
                        }
                        else if (command.getResponseType() == ResponseType.INSUFFICIENT)
                        {

                            contentText = "잔액이 부족합니다";
                            JOptionPane.showMessageDialog(null, contentText, "ERROR_MESSAGE", JOptionPane.ERROR_MESSAGE);
                        }
                        else
                        {
                            contentText = "ERROR";
                            JOptionPane.showMessageDialog(null, contentText, "ERROR_MESSAGE", JOptionPane.ERROR_MESSAGE);
                        }
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
            @Override
            public void failed(Throwable exc, ByteBuffer attachment) {
                SwingUtilities.invokeLater(() ->
                        JOptionPane.showMessageDialog(null, "서버 통신 실패: " + exc.getMessage(), "ERROR_MESSAGE", JOptionPane.ERROR_MESSAGE)
                );
            }
        });

    }

}