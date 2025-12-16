package atm;

import common.CommandDTO;
import common.RequestType;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.ByteBuffer;
import java.nio.channels.CompletionHandler;
import java.util.List;

//*******************************************************************
// Name : PanViewAccount
// Type : Class
// Description :  계좌조회 화면 패널 (JTable 적용됨)
//*******************************************************************
public class PanViewAccount extends JPanel implements ActionListener
{
    // [수정] 기존 Label, TextArea 제거하고 Table 컴포넌트 추가
    private JTable Table_Account;
    private DefaultTableModel Model_Account;
    private JScrollPane Scroll_Account;

    private JButton Btn_Close;

    ATMMain MainFrame;

    public PanViewAccount(ATMMain parent)
    {
        MainFrame = parent;
        InitGUI();
    }

    //*******************************************************************
    // Name : InitGUI
    // Type : Method
    // Description :  GUI 초기화 (Table 구성)
    //*******************************************************************
    private void InitGUI()
    {
        setLayout(null);
        setBounds(0,0,480,320);

        // 1. 타이틀 라벨 (선택 사항, 깔끔하게 보이게 추가)
        JLabel title = new JLabel("보유 계좌 목록");
        title.setBounds(20, 10, 200, 30);
        add(title);

        // 2. 테이블 모델 생성 (컬럼: 계좌 종류, 계좌 번호, 잔액)
        String[] header = {"계좌 종류", "계좌 번호", "잔액"};

        // [수정됨] 익명 클래스를 사용하여 셀 수정 불가능하도록 오버라이드
        Model_Account = new DefaultTableModel(header, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // 모든 셀에 대해 수정 불가 반환
            }
        };

        // 3. JTable 생성 및 설정
        Table_Account = new JTable(Model_Account);

        // [추가됨] 테이블 헤더가 마우스 드래그로 이동되지 않도록 설정 (선택사항)
        Table_Account.getTableHeader().setReorderingAllowed(false);
        // [추가됨] 컬럼 크기 조절 불가 설정 (선택사항)
        Table_Account.getTableHeader().setResizingAllowed(false);

        // 4. 스크롤 페인에 테이블 담기
        Scroll_Account = new JScrollPane(Table_Account);
        Scroll_Account.setBounds(20, 50, 420, 180); // 화면 중앙 배치
        add(Scroll_Account);

        // 5. 닫기 버튼
        Btn_Close = new JButton("닫기");
        Btn_Close.setBounds(200, 250, 70, 20);
        Btn_Close.addActionListener(this);
        add(Btn_Close);
    }

    public void actionPerformed(ActionEvent e)
    {
        if (e.getSource() == Btn_Close)
        {
            this.setVisible(false);
            MainFrame.display("Main");
        }
    }

    //*******************************************************************
    // Name : GetBalance()
    // Type : Method
    // Description :  서버로부터 계좌 리스트를 받아 테이블에 출력
    //*******************************************************************
    public void GetBalance()
    {
        MainFrame.send(new CommandDTO(RequestType.VIEW), new CompletionHandler<Integer, ByteBuffer>() {
            @Override
            public void completed(Integer result, ByteBuffer attachment) {
                if (result == -1) {
                    return;
                }
                attachment.flip();
                try {
                    ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(attachment.array());
                    ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream);
                    CommandDTO command = (CommandDTO) objectInputStream.readObject();

                    SwingUtilities.invokeLater(() -> {
                        // 1. 기존 테이블 데이터 초기화
                        Model_Account.setRowCount(0);

                        // 2. 리스트 가져오기
                        List<String> list = command.getAccountList();

                        // 3. 데이터 파싱 및 행 추가
                        if (list != null && !list.isEmpty()) {
                            for (String info : list) {
                                // "종류/번호/잔액" 형식 split
                                String[] parts = info.split("/");
                                if (parts.length >= 3) {
                                    String type = parts[0];
                                    // 계좌번호 포맷팅 (BankUtils 사용)
                                    String accountNo = BankUtils.displayAccountNo(parts[1]);
                                    // 잔액 포맷팅
                                    long balanceVal = Long.parseLong(parts[2]);
                                    String balanceStr = BankUtils.displayBalance(balanceVal) + "원";

                                    // 테이블에 행 추가
                                    Model_Account.addRow(new Object[]{type, accountNo, balanceStr});
                                }
                            }
                        } else {
                            // 계좌가 없을 경우 처리 (선택)
                            // Model_Account.addRow(new Object[]{"없음", "-", "0원"});
                        }
                    });
                } catch (IOException | ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
            @Override
            public void failed(Throwable exc, ByteBuffer attachment) {
            }
        });
    }
}