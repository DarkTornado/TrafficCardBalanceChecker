package com.darktornado.library;

import android.nfc.tech.NfcF;
import android.util.SparseArray;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;

public class FeliCa {

    private final SparseArray<String> deviceList = new SparseArray<>(),
            actionList = new SparseArray<>();
    public String id, balance, diff;
    public History[] history;

    public String result, manu;

    public FeliCa(NfcF nf, byte[] id) throws Exception {
        nf.connect();
        initList();
        ArrayList<History> histories = new ArrayList<>();
        int count = 10;
        byte[] data = nf.transceive(createCommand(id, 0, count));
        String data_str = bytes2hex(data);
//        응답길이 응답코드 id(제조자코드+카드식별번호) 상태flag1.0 상태flag2.0 블록수 사용내역
        result = data_str;
        manu = bytes2hex(nf.getManufacturer());

        this.id = data_str.substring(4, 4 + 16);
        parseHistory(data, count, histories);
        parseHistory(nf.transceive(createCommand(id, 10, count)), count, histories);
        history = histories.toArray(new History[0]);
        nf.close();

        if (history.length == 0) balance = "null";
        else balance = history[0].balance+ "";
        if (history.length > 2) {
            int now = history[0].balance - history[1].balance;
            diff = (now > 0 ? "+" : "") + now;
        } else {
            diff = "null";
        }
    }


    private void initList() { //https://osdn.net/projects/felicalib/wiki/suica
        deviceList.put(3, "정산기");
        deviceList.put(4, "휴대용 단말기");
        deviceList.put(5, "자동차 단말기");
        deviceList.put(7, "발매기");
        deviceList.put(8, "발매기");
        deviceList.put(9, "입금기");
        deviceList.put(18, "발매기");
        deviceList.put(20, "발매기 등");
        deviceList.put(21, "발매기 등");
        deviceList.put(22, "개찰기");
        deviceList.put(23, "간이 개찰기");
        deviceList.put(24, "창구 단말기");
        deviceList.put(25, "창구 단말기");
        deviceList.put(26, "개찰 단말기");
        deviceList.put(27, "휴대폰");
        deviceList.put(28, "승계 정산기");
        deviceList.put(29, "연락 개찰기");
        deviceList.put(31, "간이 입금기");
        deviceList.put(70, "VIEW ALTTE"); //ATM 이름, VIEW CARD(JR동일본 자회사)로만 충전 가능
        deviceList.put(72, "VIEW ALTTE");
        deviceList.put(199, "물판 단말기");
        deviceList.put(200, "자판기");

        actionList.put(1, "운임 지불 (개찰구 퇴장)");
        actionList.put(2, "충전");
        actionList.put(3, "표 구매");
        actionList.put(4, "정산");
        actionList.put(5, "입장 정산");
        actionList.put(6, "퇴장 (개찰 창구 처리)");
        actionList.put(7, "신규");
        actionList.put(8, "창구공제");
        actionList.put(13, "버스 (PiTaPa)");
        actionList.put(15, "버스 (IruCa)");
        actionList.put(17, "재발행 처리");
        actionList.put(19, "신칸센 이용");
        actionList.put(20, "자동 충전 (개찰구 입장)");
        actionList.put(21, "자동 충전 (개찰구 퇴장)");
        actionList.put(31, "입금 (버스 충전)");
        actionList.put(35, "버스 노면전차 기획권 구매");
        actionList.put(70, "물건 구매");
        actionList.put(72, "특전 (특전 요금)");
        actionList.put(73, "입금 (계산대 입금)");
        actionList.put(74, "물건 구매 취소");
        actionList.put(75, "입장 물판");
        actionList.put(198, "현금 병용 물판");
        actionList.put(203, "입장 현금 병용 물판");
        actionList.put(132, "타사 정산");
        actionList.put(133, "타사 입장 정산");
    }

    private byte[] createCommand(byte[] id, int offset, int count) throws Exception {
//        22 06 id(제조자코드+카드식별번호)8바이트 01 0F 09 10 8000 8001 8002 8003 8004 8005 8006 8007 8008 8009
//        22 06 id(제조자코드+카드식별번호)8바이트 01 0F 09 10 800A 800B 800C 800D 800E 800F 8010 8011 8012 8013
        ByteArrayOutputStream bout = new ByteArrayOutputStream(128); //일단 넉넉하게 잡음

        bout.write(0x00);   //데이터 길이, 일단 0 넣어두고 다 채운 뒤에 변경 예정
        bout.write(0x06);   //06은 암호화 없이 읽는 것을 의미하는 FeliCa 명령어
        bout.write(id);        //카드를 태그했을 때 읽어온 ID
        bout.write(0x01);   //서비스 코드 길이
        bout.write(0x0f);   // low byte of service code for pasmo history (리틀 엔디안 방식)
        bout.write(0x09);   // high byte of service code for pasmo history (리틀 엔디안 방식)
        bout.write(count);     //읽을 블록 수
        for (int n = offset; n < offset + count; n++) {
            bout.write(0x80);  //블록 엘리먼트 상위 바이트 「Felica 사용자 메뉴얼 발췌」의 4.3항 참조
            bout.write(n);        //블록 번호
        }

        byte[] cmd = bout.toByteArray();
        cmd[0] = (byte) cmd.length; //데이터 길이 넣음

        return cmd;
    }

    private void parseHistory(byte[] data, int count, ArrayList<History> histories) {
        for (int n = 0; n < count; n++) {
            History datum = new History(data, 13 + n * 16);
            if (datum.index > 0) histories.add(datum);
        }
    }


    private final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();
    private String bytes2hex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int n = 0; n < bytes.length; n++) {
            int v = bytes[n] & 0xFF;
            hexChars[n * 2] = HEX_ARRAY[v >>> 4];
            hexChars[n * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }


    public class History {

        public static final int TYPE_JR = 0;
        public static final int TYPE_METRO = 1;
        public static final int TYPE_BUS = 2;
        public static final int TYPE_GOODS = 3;

        public final String date, device, action;
        public final int regionId, inLineId, inStationId, outLineId, outStationId;
        public final int index, balance, type;

        public History(byte[] data, int offset) {

            //0 - 단말 종류
            int deviceId = data[offset + 0];
            this.device = deviceList.get(deviceId);

            //1 - 처리 내역
            int procId = data[offset + 1];
            this.action = actionList.get(procId);

            //2, 3 - 몰?루

            //4, 5 - 날짜
            int mixInt = toInt(data, offset, 4, 5);
            int year = ((mixInt >> 9) & 0x07f) + 2000;
            int month = (mixInt >> 5) & 0x00f;
            int date = mixInt & 0x01f;
            this.date = year + ". " + month + ". " + date + ".";

            //10 ~ 11 - 잔액 (리틀 엔디안 방식으로 저장됨)
            this.balance = toInt(data, offset, 11, 10);

            //12 ~ 14 - 일련번호, index 같음
            this.index = toInt(data, offset, 12, 13, 14);

            //15 - 지역 id
            this.regionId = data[offset + 15];

            //6, 7 - 승차역 노선명, 승차역
            //물건 구매의 경우 시간 (시/분/??)
            //버스인 경우 출선구?
            this.inLineId = toInt(data, offset, 6);
            this.inStationId = toInt(data, offset, 7);

            //8, 9 - 하차역 노선명, 하차역
            //버스인 경우 출역순?
            this.outStationId = toInt(data, offset, 8);
            this.outLineId = toInt(data, offset, 9);

            if (isShopping(procId)) {   //물건 구매 등
                this.type = TYPE_GOODS;
            } else if (isBus(procId)) { //버스 탑승
                this.type = TYPE_BUS;
            } else {                    //JR 또는 사철 탑승
                this.type = inLineId < 0x80 ? TYPE_JR : TYPE_METRO; //JR or 사철
            }

        }

        @Override
        public String toString() {
            return "index: " + index +
                    "\ndata: " + date +
                    "\ntype: " + type +
                    "\ndevice: " + device +
                    "\naction: " + action +
                    "\nbalance: " + balance + "円" +
                    "\nregionId: " + regionId;
        }

        private int toInt(byte[] res, int offset, int... index) {
            int num = 0;
            for (int i : index) {
                num = num << 8;
                num += ((int) res[offset + i]) & 0x0ff;
            }
            return num;
        }

        private boolean isShopping(int id) {
            return id == 70 || id == 73 || id == 74 || id == 75 || id == 198 || id == 203;
        }

        private boolean isBus(int id) {
            return id == 13 || id == 15 || id == 31 || id == 35;
        }

    }

}