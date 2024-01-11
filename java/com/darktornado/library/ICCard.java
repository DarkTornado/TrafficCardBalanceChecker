package com.darktornado.library;

import android.nfc.tech.IsoDep;

public class ICCard {
    public boolean isPostPaid = false;
    public String type, cardType = "일반", //카드회사 종류 (티머니, 캐시비 등), 카트 종류 (일반, 청소년 등)
            number, //카드 번호
            due, //카드 유효기간이라는데, 교통카드는 유효기간 없지 않나?
            aid, aidLength, //잔액 조회하기 전에 SELECT FILE 명령어 실행해야 하는데, 그 때 AID 필요함. 길이 계산하기 귀찮으니 그냥 따로 저장
            balanceReadCommand = "905C000004", //905C000004가 표준인데 종종 904C000004인 경우가 있음. 잔액조회 명령어는 카드 안에 저장되어 있을 수도 있고 없을 수도 있음
            balance;
    int balance_int;

    public ICCard(IsoDep id) throws Exception {
        id.connect();
        byte[] transceive = id.transceive(str2bytes("00A40400" + "07" + "A0000004520001" + "00")); //마지막 00은 예상되는 최대 바이트의 수이며, 00으로 적으면 256바이트를 의미

        //data는 레코드. Tag, size, value 순서, value나 size는 없을 수도 있음?
        String data_str = bytes2hex(transceive);
        char[] data = data_str.toCharArray();

        //9000으로 끝나는 경우 = 정상적으로 읽었음을 의미. SW1, SW2가 각각 1바이트씩 뒤에 붙고, 99 00이 정상 처리된 경우를 의미
        if (!data_str.endsWith("9000")) return;

        //1바이트 = 2칸씩 읽음, 중간에 n값 수정해서 이미 읽거나 안읽어도 되는 부분 넘김
        for (int n = 0; n < data.length; n += 2) {

            //현재 바이트
            String s = data[n] + "" + data[n + 1];

            // FCI Template
            if (s.equals("6F")) {
                n += 2;  //어디에 쓰는건지 몰?루
            }

            // DF Name (전용파일 이름)
            else if (s.equals("84")) {
                int size = Integer.parseInt("" + data[n + 2] + data[n + 3], 16); //7
                //어차피 A0000004520001로 동일, 애초에 저 값이 아니라면 전국호환교통카드로 안읽힐 듯?
                n += 2 * size + 2;
            }

            //FCI Proprietary Template
            else if (s.equals("A5")) {
                n += 2;  //어디에 쓰는건지 몰?루
            }

            //카드 규격 및 선/후불 구분
            else if (s.equals("50")) {
                int size = Integer.parseInt("" + data[n + 2] + data[n + 3]); //2
                //선불은 01 00, 후불은 11 00
                if (data_str.substring(n + 4, n + 4 + 2 * size).equals("1100")) {
                    isPostPaid = true;
                }
                n += 2 * size + 2;
            }

            //지원 항목
            else if (s.equals("47")) {
                int size = Integer.parseInt("" + data[n + 2] + data[n + 3], 16); //2
                //ISO 14443-3, 하이패스 등 지원여부가 저장되어 있는데, 난 안쓸거임
                n += 2 * size + 2;
            }

            //ID CENTER - 사업자 구분
            else if (s.equals("43")) {
                int size = Integer.parseInt("" + data[n + 2] + data[n + 3], 16); //1
                //티머니, 캐시비 등 구분
                type = getCardType(Integer.parseInt(data_str.substring(n + 4, n + 4 + 2 * size), 16));
                n += 2 * size + 2;
            }

            //잔액조회명령 - 카드에 정보가 없을 수도 있음
            else if (s.equals("11")) {
                int size = Integer.parseInt("" + data[n + 2] + data[n + 3], 16); //7
                balanceReadCommand = data_str.substring(n + 4, n + 4 + 2 * size);
                //금융 IC 카드 표준안에 따르면 905C000004로 다 같아야 하고, 캐시비도 905C000004
                //티머니랑 레일플러스는 904C000004가 저장되어 있음
                n += 2 * size + 2;
            }

            //교통 호환 ADF AID
            else if (s.equals("4F")) {
                int size = Integer.parseInt("" + data[n + 2] + data[n + 3], 16); //7이여야 하는데, 레일플러스는 8임

                //따로 계산하기 귀찮으니 그냥 길이 저장함
                aidLength = data_str.substring(n + 2, n + 2 + 2);
                //금융 IC 카드 표준안에 따르면 K-CASH는 D4106509900020이고, 저 값이여야 한다는데,
                //K-CASH는 2020년 12월 15일에 서비스 종료했는데? 왜 2021년 표준안에서도 등장하는가?
                aid = data_str.substring(n + 4, n + 4 + 2 * size);

                n += 2 * size + 2;
            }

            //부가 데이터 파일 정보
            else if (s.equals("9F") && data[n + 2] == '1' && data[n + 3] == '0') {
                int size = Integer.parseInt("" + data[n + 4] + data[n + 5], 16); //3
                //어차피 EA 00 34로 고정, 뭔지는 몰?루
                n += 2 * size + 4;
            }

            //카트 타입 정보 (청소년 등)
            else if (s.equals("45")) {
                int size = Integer.parseInt("" + data[n + 2] + data[n + 3]); //1
                String[] types = {"?", "일반", "어린이", "청소년", "경로", "장애인",
                        "?", "?", "?", "?", "?", "버스", "화물차"};
                cardType = types[Integer.parseInt(data_str.substring(n + 4, n + 4 + 2 * size), 16)];
                n += 2 * size + 2;
            }

            //카드 유효기간 - YYMM 형식, 근데 이제 유효기간 없어지지 않았나?
            else if (s.equals("5F") && data[n + 2] == '2' && data[n + 3] == '4') {
                int size = Integer.parseInt("" + data[n + 4] + data[n + 5], 16); //2
                due = "20" + data_str.substring(n + 6, n + 6 + 2 * size);
                n += 2 * size + 4;
            }

            //카드 일련번호 - 카드에 정보가 없을 수도 있음
            else if (s.equals("12")) {
                int size = Integer.parseInt("" + data[n + 2] + data[n + 3], 16); //8
                number = data_str.substring(n + 4, n + 4 + 2 * size);
                n += 2 * size + 2;
            }

            //카드 관리번호 - 카드에 정보가 없을 수도 있음
            else if (s.equals("13")) {
                int size = Integer.parseInt("" + data[n + 2] + data[n + 3], 16); //8
                //어차피 사용하지 않을 것이니 몰?루
                n += 2 * size + 2;
            }

            //기타 카드 사업자 임의의 정보 - 카드에 정보가 없을 수도 있음
            else if (s.equals("BF") && data[n + 2] == '0' && data[n + 3] == 'C') {
                int size = Integer.parseInt("" + data[n + 4] + data[n + 5], 16); //?
                //몰?루
                n += 2 * size + 4;
            }
        }

        //잔액조회
        //CLA INS P1 P2 Lc Data Le 전송 후 balance cmd 전송
        //Lc는 AID의 길이, Data에는 AID가 들어감
        //00 A4 04 00 + 07 + D4100000030001 + 00
        id.transceive(str2bytes("00A40400" + aidLength + aid + "00"));

        //잔액조회 : 904C000004, 표준은 905C000004인데, 캐시비만 표준 지키고 티머니랑 레일플러스는 4C임
        //어차피 카드 안에 잔액조회 명령어 담김, 꼭 담기는 것은 아님
        String balance = bytes2hex(id.transceive(str2bytes(balanceReadCommand)));
        balance_int = Integer.parseInt(balance.substring(0, 8), 16);
        this.balance = String.format("%,d", balance_int);

        id.close();
    }

    private String getCardType(int type) {
        switch (type) {  //switch문에는 실제로 카드를 구해와서 확인해본 것들만 넣음
            case 0x08:
                return "티머니";
            case 0x0B:
                return "캐시비";
            case 0x21:
                return "레일플러스";
            default:
                return "(알 수 없음)";
        }
        /*
         * 아래 정보는 금융 IC 카드 표준안에 나온 내용
         * 0x00 Reserved
         * 0x01 금융결제원
         * 0x02 에이캐시
         * 0x03 마이비
         * 0x04 Reserved
         * 0x05 브이캐시
         * 0x06 몬덱스코리아
         * 0x07 한국도로공사 = 하이패스?
         * 0x08 한국스마트카드 = 티머니
         * 0x09 코레일네트웍스 = 레일플러스?
         * 0x0A Reserved
         * 0x0B 이비 = 캐시비로 통합
         * 0x0C 서울특별시버스운송사업조합
         * 0x0D 카드넷
         */
    }

    @Override
    public String toString() {
        return "type: " + type + "\ncardType: " + cardType +
                "\ndue: " + due + "\nnumber: " + number +
                "\naid: " + aid + "\naidLength: " + aidLength +
                "\nisPostPaid: " + isPostPaid +
                "\ncmd: " + balanceReadCommand;
    }

    private final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();

    public String bytes2hex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int n = 0; n < bytes.length; n++) {
            int v = bytes[n] & 0xFF;
            hexChars[n * 2] = HEX_ARRAY[v >>> 4];
            hexChars[n * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }

    private byte[] str2bytes(String str) {
        int length = str.length();
        byte[] arr = new byte[length / 2];
        for (int n = 0; n < length; n += 2) {
            arr[n / 2] = (byte) ((Character.digit(str.charAt(n), 16) << 4) + Character.digit(str.charAt(n + 1), 16));
        }
        return arr;
    }

}
